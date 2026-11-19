package com.genie.vendor.kyc.application;

import com.genie.auth.profile.application.UserFacade;
import com.genie.auth.profile.application.UserSummary;
import com.genie.common.mail.EmailSender;
import com.genie.common.mail.MailTemplate;
import com.genie.vendor.kyc.domain.KycDocumentType;
import com.genie.vendor.kyc.event.KycDecidedEvent;
import com.genie.vendor.shop.domain.VendorProfile;
import com.genie.vendor.shop.domain.VendorProfileRepository;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Email kết quả KYC cho chủ shop sau khi quyết định đã commit (UC-NOT-03; plan Mở shop & KYC K05.11, K06.11): duyệt →
 * {@code kyc-approved}, từ chối → {@code kyc-rejected} (kèm nút "Yêu cầu nhân viên xem xét" nếu chưa dùng), yêu cầu bổ sung
 * → {@code kyc-supplement}. Gửi tới email tài khoản chủ shop. Thông báo in-app: module notification khi hiện thực.
 */
@Component
public class KycNotifier {

    private static final Logger log = LoggerFactory.getLogger(KycNotifier.class);
    static final DateTimeFormatter VN_TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final Map<String, String> DOC_LABELS = Map.of(
            KycDocumentType.ID_FRONT.name(), "CCCD mặt trước",
            KycDocumentType.ID_BACK.name(), "CCCD mặt sau",
            KycDocumentType.BUSINESS_LICENSE.name(), "Giấy phép kinh doanh",
            KycDocumentType.SELFIE.name(), "Ảnh chân dung");

    private final EmailSender mail;
    private final UserFacade users;
    private final VendorProfileRepository profiles;

    public KycNotifier(EmailSender mail, UserFacade users, VendorProfileRepository profiles) {
        this.mail = mail;
        this.users = users;
        this.profiles = profiles;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDecided(KycDecidedEvent e) {
        String email = users.emailOf(e.vendorUserId()).orElse(null);
        VendorProfile p = profiles.findById(e.vendorProfileId()).orElse(null);
        if (email == null || p == null) {
            return;
        }
        Map<String, Object> vars = new HashMap<>();
        vars.put("name", users.findSummary(e.vendorUserId()).map(UserSummary::fullName)
                .filter(n -> n != null && !n.isBlank()).orElse(p.getRepresentative() == null ? p.getShopName() : p.getRepresentative()));
        vars.put("storeName", p.getShopName());
        MailTemplate template;
        switch (e.status()) {
            case "VERIFIED" -> template = MailTemplate.KYC_APPROVED;
            case "REJECTED" -> {
                template = MailTemplate.KYC_REJECTED;
                vars.put("reason", e.reason());
                vars.put("canRequestReview", !p.isManualReviewRequested());
            }
            case "SUPPLEMENT_REQUESTED" -> {
                template = MailTemplate.KYC_SUPPLEMENT;
                vars.put("documents", e.supplementDocuments().stream().map(d -> DOC_LABELS.getOrDefault(d, d)).toList());
                vars.put("note", e.reason());
                vars.put("deadline", e.deadlineAt() == null ? "" : VN_TIME.format(e.deadlineAt()));
            }
            default -> {
                return;
            }
        }
        try {
            mail.send(email, template, vars, Locale.forLanguageTag("vi"));
        } catch (RuntimeException ex) {
            log.warn("Không gửi được email KYC {} cho shop {}: {}", e.status(), e.vendorProfileId(), ex.getMessage());
        }
    }
}
