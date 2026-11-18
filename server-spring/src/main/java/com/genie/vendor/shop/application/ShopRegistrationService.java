package com.genie.vendor.shop.application;

import com.genie.auth.otp.application.IssuedOtp;
import com.genie.catalog.category.application.CategoryFacade;
import com.genie.auth.otp.application.OtpFacade;
import com.genie.auth.otp.application.OtpPurpose;
import com.genie.auth.profile.application.UserFacade;
import com.genie.auth.profile.application.UserSummary;
import com.genie.common.domain.UserRole;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.vendor.shop.domain.BusinessType;
import com.genie.vendor.shop.domain.VendorProfile;
import com.genie.vendor.shop.domain.VendorProfileRepository;
import com.genie.vendor.shop.event.ShopRegisteredEvent;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đăng ký mở gian hàng — UC-VND-01 (API_SPEC 9.1–9.2; plan Mở shop & KYC K03.02–K03.03):
 * <ol>
 *   <li>{@link #register}: kiểm chưa có shop ({@code 409 GNE-VND-4093}), tên hợp lệ / chưa dùng ({@code 409 GNE-VND-4094}),
 *       ngành hàng ({@code CategoryFacade} của catalog); gửi OTP {@code SHOP_REGISTRATION} tới email liên hệ của shop, giữ dữ liệu form trong challenge;</li>
 *   <li>{@link #confirm}: mã đúng → tạo {@code vendor_profiles} ({@code NOT_SUBMITTED}) + nâng tài khoản lên {@code VENDOR}
 *       (giữ phiên — D11) trong một transaction. Hai request song song chỉ tạo 1 shop nhờ {@code UNIQUE user_id} và
 *       {@code uk_vendor_profiles_shop_name_lower}.</li>
 * </ol>
 */
@Service
public class ShopRegistrationService {

    static final String SHOP_NAME = "shop_name";
    static final String OWNER_NAME = "owner_name";
    static final String PHONE = "phone_number";
    static final String EMAIL = "email";
    static final String CATEGORY = "main_category_id";
    static final String BUSINESS_TYPE = "business_type";

    private final VendorProfileRepository profiles;
    private final ShopNamePolicy names;
    private final OtpFacade otps;
    private final UserFacade users;
    private final CategoryFacade categories;
    private final ApplicationEventPublisher events;

    public ShopRegistrationService(VendorProfileRepository profiles, ShopNamePolicy names, OtpFacade otps, UserFacade users,
                                   CategoryFacade categories, ApplicationEventPublisher events) {
        this.profiles = profiles;
        this.names = names;
        this.otps = otps;
        this.users = users;
        this.categories = categories;
        this.events = events;
    }

    /** Dữ liệu form "Đăng ký mở Shop" (API 9.1). */
    public record Registration(String shopName, String ownerName, String phoneNumber, String email, long mainCategoryId,
                               BusinessType businessType) {
    }

    /** Kết quả 9.2: {@code {shop_id, kyc_status}}. */
    public record Registered(long shopId, String kycStatus) {
    }

    @Transactional(readOnly = true)
    public IssuedOtp register(long userId, Registration r) {
        requireBuyerAccount(userId);
        if (profiles.findByUserId(userId).isPresent()) {
            throw new BusinessException(ErrorCode.SHOP_ALREADY_EXISTS);
        }
        String shopName = names.validate(r.shopName(), null);
        if (!categories.isActive(r.mainCategoryId())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Ngành hàng không tồn tại.",
                    Map.of("field", "main_category_id"));
        }
        String email = r.email().trim().toLowerCase(Locale.ROOT);
        Map<String, String> payload = new HashMap<>();
        payload.put(SHOP_NAME, shopName);
        payload.put(OWNER_NAME, r.ownerName().trim().replaceAll("\\s+", " "));
        payload.put(PHONE, r.phoneNumber().replaceAll("[\\s.-]", ""));
        payload.put(EMAIL, email);
        payload.put(CATEGORY, String.valueOf(r.mainCategoryId()));
        payload.put(BUSINESS_TYPE, r.businessType().name());
        return otps.issue(OtpPurpose.SHOP_REGISTRATION, email, userId, payload);
    }

    @Transactional
    public Registered confirm(long userId, String challengeId, String code) {
        requireBuyerAccount(userId);
        Map<String, String> p = otps.verify(challengeId, code, OtpPurpose.SHOP_REGISTRATION, userId);
        if (profiles.findByUserId(userId).isPresent()) {
            throw new BusinessException(ErrorCode.SHOP_ALREADY_EXISTS);
        }
        String shopName = names.validate(p.get(SHOP_NAME), null);   // tên có thể vừa bị shop khác lấy trong 5 phút chờ mã
        VendorProfile shop = VendorProfile.register(userId, shopName, Long.valueOf(p.get(CATEGORY)),
                BusinessType.valueOf(p.get(BUSINESS_TYPE)), p.get(OWNER_NAME), p.get(PHONE), p.get(EMAIL));
        try {
            profiles.saveAndFlush(shop);
        } catch (DataIntegrityViolationException e) {
            String msg = String.valueOf(e.getMostSpecificCause().getMessage());
            throw new BusinessException(msg.contains("shop_name") ? ErrorCode.SHOP_NAME_TAKEN : ErrorCode.SHOP_ALREADY_EXISTS);
        }
        users.promoteToVendor(userId);
        events.publishEvent(new ShopRegisteredEvent(shop.getId(), userId));
        return new Registered(shop.getId(), shop.getKycStatus().name());
    }

    /** Chỉ tài khoản người mua (hoặc VENDOR chưa có shop) mới mở shop; nhân viên / Admin → 403. */
    private void requireBuyerAccount(long userId) {
        UserSummary user = users.findSummary(userId).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        if (user.role() != UserRole.CUSTOMER && user.role() != UserRole.VENDOR) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Tài khoản nhân viên không mở shop được.");
        }
    }
}
