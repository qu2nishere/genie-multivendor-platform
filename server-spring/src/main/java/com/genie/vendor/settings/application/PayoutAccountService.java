package com.genie.vendor.settings.application;

import com.genie.auth.otp.application.OtpPurpose;
import com.genie.auth.otp.application.StepUpPrecondition;
import com.genie.auth.profile.application.UserFacade;
import com.genie.common.audit.AuditAction;
import com.genie.common.audit.AuditLogger;
import com.genie.common.domain.NameNormalizer;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.mail.EmailSender;
import com.genie.common.mail.MailTemplate;
import com.genie.common.security.ClientInfo;
import com.genie.common.security.RateLimiter;
import com.genie.vendor.kyc.domain.KycResult;
import com.genie.vendor.kyc.domain.KycResultRepository;
import com.genie.vendor.shop.application.SellerContext;
import com.genie.vendor.shop.domain.VendorProfile;
import com.genie.vendor.shop.domain.VendorProfileRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Tài khoản nhận giải ngân của shop — UC-VND-07 (API 10.11; plan Mở shop & KYC K08.03–K08.07). Chỉ shop {@code VERIFIED}
 * ({@code 403 GNE-VND-4031}). Chính chủ = tên ngân hàng trả về khớp họ tên KYC ({@code kyc_results.extracted_full_name},
 * không có thì người đại diện) sau {@link NameNormalizer}. Lưu / xóa cần OTP bước 2 (controller {@code @RequireStepUp}); lưu
 * luôn xác minh lại phía server. Cài thêm {@link StepUpPrecondition} để chặn TRƯỚC khi gửi mã xóa khi còn lệnh rút chờ.
 */
@Service
public class PayoutAccountService implements StepUpPrecondition {

    private static final Logger log = LoggerFactory.getLogger(PayoutAccountService.class);
    static final DateTimeFormatter VN_TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private final VendorProfileRepository profiles;
    private final KycResultRepository kycResults;
    private final SellerContext sellers;
    private final BankCatalog banks;
    private final BankAccountLookup lookup;
    private final List<PayoutAccountGuard> guards;
    private final RateLimiter rateLimiter;
    private final AuditLogger audit;
    private final EmailSender mail;
    private final UserFacade users;
    private final Clock clock;

    public PayoutAccountService(VendorProfileRepository profiles, KycResultRepository kycResults, SellerContext sellers,
                                BankCatalog banks, BankAccountLookup lookup, List<PayoutAccountGuard> guards,
                                RateLimiter rateLimiter, AuditLogger audit, EmailSender mail, UserFacade users, Clock clock) {
        this.profiles = profiles;
        this.kycResults = kycResults;
        this.sellers = sellers;
        this.banks = banks;
        this.lookup = lookup;
        this.guards = guards;
        this.rateLimiter = rateLimiter;
        this.audit = audit;
        this.mail = mail;
        this.users = users;
        this.clock = clock;
    }

    public record AccountInput(String bankCode, String accountNumber, String accountHolder) {
    }

    public record Verification(boolean matched, String normalizedHolder) {
    }

    /** {@code payout_account} (API 10.11) — số tài khoản luôn che. */
    public record PayoutAccountView(String bankName, String accountHolder, String accountNumberMasked, boolean isVerified) {
    }

    @Transactional(readOnly = true)
    public Verification verify(long userId, AccountInput in) {
        SellerContext.Seller seller = sellers.requireVerified(userId);
        rateLimiter.check("rl:payout-verify:" + seller.vendorProfileId(), 5, Duration.ofMinutes(1));
        VendorProfile p = profiles.findById(seller.vendorProfileId()).orElseThrow();
        String returned = checkHolder(p, in);
        return new Verification(true, returned);
    }

    @Transactional
    public PayoutAccountView save(long userId, AccountInput in, ClientInfo client) {
        SellerContext.Seller seller = sellers.requireVerified(userId);
        VendorProfile p = profiles.findByIdForUpdate(seller.vendorProfileId()).orElseThrow();
        BankCatalog.Bank bank = bank(in.bankCode());
        String returned = checkHolder(p, in);   // xác minh lại phía server — không tin bước verification của client
        OffsetDateTime now = OffsetDateTime.now(clock);
        if (!p.setPayoutAccount(bank.name(), in.accountNumber().trim(), returned, returned, kycName(p), now)) {
            throw mismatch(p);
        }
        record(p, "UPDATED", bank.name(), mask(in.accountNumber()), userId, client, now);
        return view(p);
    }

    @Transactional
    public void delete(long userId, ClientInfo client) {
        SellerContext.Seller seller = sellers.requireVerified(userId);
        VendorProfile p = profiles.findByIdForUpdate(seller.vendorProfileId()).orElseThrow();
        requireNoPendingWithdrawal(p.getUserId());
        String bank = p.getPayoutBankName();
        String masked = mask(p.getPayoutBankAccountNumber());
        if (bank == null) {
            return;
        }
        p.clearPayoutAccount();
        record(p, "DELETED", bank, masked, userId, client, OffsetDateTime.now(clock));
    }

    public PayoutAccountView view(VendorProfile p) {
        if (p.getPayoutBankAccountNumber() == null) {
            return null;
        }
        return new PayoutAccountView(p.getPayoutBankName(), p.getPayoutBankAccountName(), mask(p.getPayoutBankAccountNumber()),
                p.isPayoutAccountVerified());
    }

    // ===== StepUpPrecondition: chặn gửi mã xóa khi còn lệnh rút chờ (API 3.5) =====

    @Override
    public boolean supports(OtpPurpose purpose) {
        return purpose == OtpPurpose.PAYOUT_ACCOUNT_DELETE;
    }

    @Override
    public void check(long userId, OtpPurpose purpose) {
        requireNoPendingWithdrawal(userId);
    }

    // ===== nội bộ =====

    private void requireNoPendingWithdrawal(long vendorUserId) {
        for (PayoutAccountGuard g : guards) {
            if (g.hasPendingWithdrawal(vendorUserId)) {
                throw new BusinessException(ErrorCode.PAYOUT_PENDING_WITHDRAWAL);
            }
        }
    }

    private BankCatalog.Bank bank(String code) {
        return banks.find(code).orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED,
                "Ngân hàng không hợp lệ.", Map.of("field", "bank_code")));
    }

    /** Tra tên chủ tài khoản và so với họ tên KYC; không khớp → {@code 422 GNE-WAL-4222 {kyc_name}}. */
    private String checkHolder(VendorProfile p, AccountInput in) {
        BankCatalog.Bank bank = bank(in.bankCode());
        String returned = lookup.holderName(bank.code(), in.accountNumber().trim(), in.accountHolder());
        if (!NameNormalizer.normalize(returned).equals(NameNormalizer.normalize(kycName(p)))) {
            throw mismatch(p);
        }
        return NameNormalizer.normalize(returned);
    }

    private BusinessException mismatch(VendorProfile p) {
        return new BusinessException(ErrorCode.PAYOUT_HOLDER_MISMATCH, Map.of("kyc_name", NameNormalizer.normalize(kycName(p))));
    }

    private String kycName(VendorProfile p) {
        return kycResults.findByVendorProfileId(p.getId()).map(KycResult::getExtractedFullName)
                .filter(n -> n != null && !n.isBlank()).orElse(p.getRepresentative() == null ? "" : p.getRepresentative());
    }

    /** {@code 0071000006712} → {@code •••• •••• 6712}. */
    public static String mask(String number) {
        if (number == null) {
            return null;
        }
        String n = number.trim();
        return "•••• •••• " + (n.length() <= 4 ? n : n.substring(n.length() - 4));
    }

    private void record(VendorProfile p, String action, String bank, String masked, long userId, ClientInfo client,
                        OffsetDateTime now) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("action", action);
        details.put("bank_name", bank);
        details.put("account_number_masked", masked);
        audit.record(AuditAction.PAYOUT_ACCOUNT_CHANGED, userId, String.valueOf(p.getId()), details, client, true);
        Map<String, Object> vars = new HashMap<>();
        vars.put("name", p.getRepresentative() == null ? p.getShopName() : p.getRepresentative());
        vars.put("storeName", p.getShopName());
        vars.put("action", action);
        vars.put("bankName", bank);
        vars.put("accountMasked", masked);
        vars.put("changedAt", VN_TIME.format(now));
        String email = users.emailOf(p.getUserId()).orElse(null);
        if (email == null || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    mail.send(email, MailTemplate.PAYOUT_ACCOUNT_CHANGED, vars, Locale.forLanguageTag("vi"));
                } catch (RuntimeException e) {
                    log.warn("Không gửi được email đổi tài khoản nhận tiền: {}", e.getMessage());
                }
            }
        });
    }
}
