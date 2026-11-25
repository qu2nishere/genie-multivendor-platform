package com.genie.vendor.shop.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

/** Máy trạng thái KYC + luật cài đặt trong entity (LLD 6.3; plan Mở shop & KYC K03.04, K04.02, K06.02, K08.04, K09.03). */
class VendorProfileTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-08T10:00:00+07:00");

    private static VendorProfile shop() {
        return VendorProfile.register(7L, "TechZone", 1L, BusinessType.BUSINESS, "Đặng Quốc Huy", "0912345678",
                "shop@techzone.vn");
    }

    private static VendorProfile in(KycStatus status) {
        VendorProfile p = shop();
        p.setKycStatus(status);
        return p;
    }

    @Test
    void registerStartsNotSubmittedWithoutCommissionPickupOrGhn() {
        VendorProfile p = shop();
        assertThat(p.getKycStatus()).isEqualTo(KycStatus.NOT_SUBMITTED);
        assertThat(p.getCommissionRate()).isNull();
        assertThat(p.getGhnShopId()).isNull();
        assertThat(p.isPickupConfigured()).isFalse();
        assertThat(p.getContactEmail()).isEqualTo("shop@techzone.vn");
        assertThat(p.getRepresentative()).isEqualTo("Đặng Quốc Huy");
        assertThat(p.getKycBiometricConsent()).isFalse();
        assertThat(p.getIsMall()).isFalse();
        assertThat(p.getFollowerCount()).isZero();
    }

    @Test
    void submitOnlyFromEditableStatesAndClearsPreviousDecision() {
        for (KycStatus s : new KycStatus[] {KycStatus.NOT_SUBMITTED, KycStatus.REJECTED, KycStatus.SUPPLEMENT_REQUESTED}) {
            VendorProfile p = in(s);
            p.setKycRejectionReason("cũ");
            p.setKycSupplementDocumentsJson("[\"ID_FRONT\"]");
            p.submitKyc(NOW);
            assertThat(p.getKycStatus()).isEqualTo(KycStatus.PENDING);
            assertThat(p.getKycSubmittedAt()).isEqualTo(NOW);
            assertThat(p.getKycRejectionReason()).isNull();
            assertThat(p.getKycSupplementDocumentsJson()).isNull();
        }
        for (KycStatus s : new KycStatus[] {KycStatus.PENDING, KycStatus.AI_PROCESSING, KycStatus.MANUAL_REVIEW,
                KycStatus.VERIFIED}) {
            assertThatThrownBy(() -> in(s).submitKyc(NOW)).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> in(s).saveKycDraft("079096001234", null, null, null, null, true))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void draftKeepsUnsentFieldsAndAttachKeepsOldDocuments() {
        VendorProfile p = shop();
        p.saveKycDraft("079096001234", "0312345678", null, null, BusinessType.HOUSEHOLD, true);
        p.saveKycDraft(null, null, "Người Khác", null, null, null);
        assertThat(p.getIdentityCode()).isEqualTo("079096001234");
        assertThat(p.getTaxCode()).isEqualTo("0312345678");
        assertThat(p.getRepresentative()).isEqualTo("Người Khác");
        assertThat(p.getBusinessType()).isEqualTo(BusinessType.HOUSEHOLD);
        assertThat(p.getKycBiometricConsent()).isTrue();
        p.attachKycDocuments("front.enc", "back.enc", null, "selfie.enc", "{}");
        p.attachKycDocuments(null, "back2.enc", null, null, "{\"x\":1}");
        assertThat(p.getIdFrontUrl()).isEqualTo("front.enc");
        assertThat(p.getIdBackUrl()).isEqualTo("back2.enc");
        assertThat(p.getSelfieUrl()).isEqualTo("selfie.enc");
        assertThat(p.getKycDocumentsMetaJson()).isEqualTo("{\"x\":1}");
    }

    @Test
    void aiPipelineTransitions() {
        VendorProfile p = in(KycStatus.PENDING);
        p.startAiProcessing();
        assertThat(p.getKycStatus()).isEqualTo(KycStatus.AI_PROCESSING);
        p.approveKyc(null, KycVerificationMethod.OCR_AUTO, NOW);
        assertThat(p.getKycStatus()).isEqualTo(KycStatus.VERIFIED);
        assertThat(p.getKycVerificationMethod()).isEqualTo(KycVerificationMethod.OCR_AUTO);

        assertThatThrownBy(() -> in(KycStatus.NOT_SUBMITTED).startAiProcessing()).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> in(KycStatus.PENDING).routeToManualReview()).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> in(KycStatus.PENDING).approveKyc(1L, KycVerificationMethod.MANUAL_STAFF, NOW))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> in(KycStatus.VERIFIED).rejectKyc("x")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void manualReviewRequestOnlyOnceAndOnlyWhenRejected() {
        VendorProfile p = in(KycStatus.REJECTED);
        p.requestManualReview(NOW);
        assertThat(p.getKycStatus()).isEqualTo(KycStatus.MANUAL_REVIEW);
        assertThat(p.isManualReviewRequested()).isTrue();
        p.rejectKyc("vẫn sai");
        assertThatThrownBy(() -> p.requestManualReview(NOW)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> in(KycStatus.PENDING).requestManualReview(NOW)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void supplementExpiryAndRequeue() {
        VendorProfile p = in(KycStatus.MANUAL_REVIEW);
        p.requestSupplement("[\"ID_FRONT\"]", "Ảnh mờ", NOW.plusDays(7));
        assertThat(p.getKycStatus()).isEqualTo(KycStatus.SUPPLEMENT_REQUESTED);
        assertThat(p.getKycSupplementDeadlineAt()).isEqualTo(NOW.plusDays(7));
        p.expireSupplement("Quá hạn bổ sung hồ sơ");
        assertThat(p.getKycStatus()).isEqualTo(KycStatus.REJECTED);
        assertThat(p.getKycRejectionReason()).isEqualTo("Quá hạn bổ sung hồ sơ");
        assertThatThrownBy(() -> in(KycStatus.PENDING).requestSupplement("[]", "x", NOW))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> in(KycStatus.REJECTED).expireSupplement("x")).isInstanceOf(IllegalStateException.class);

        VendorProfile v = in(KycStatus.MANUAL_REVIEW);
        v.approveKyc(3L, KycVerificationMethod.MANUAL_STAFF, NOW);
        v.requeueForReview();
        assertThat(v.getKycStatus()).isEqualTo(KycStatus.MANUAL_REVIEW);
        assertThat(v.getKycVerifiedBy()).isNull();
        assertThatThrownBy(() -> in(KycStatus.MANUAL_REVIEW).requeueForReview()).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> in(KycStatus.NOT_SUBMITTED).requeueForReview()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void renameAtMostOncePerThirtyDays() {
        VendorProfile p = shop();
        assertThat(p.nextRenameAllowedAt(NOW)).isNull();
        p.renameShop("TechZone VN", NOW);
        assertThat(p.getShopNameChangedAt()).isEqualTo(NOW);
        assertThat(p.nextRenameAllowedAt(NOW.plusDays(1))).isEqualTo(NOW.plusDays(30));
        p.renameShop("TechZone VN", NOW.plusDays(1));                      // cùng tên: không tính là đổi
        assertThatThrownBy(() -> p.renameShop("Khác", NOW.plusDays(29))).isInstanceOf(IllegalStateException.class);
        p.renameShop("Khác", NOW.plusDays(30));
        assertThat(p.getShopName()).isEqualTo("Khác");
    }

    @Test
    void autoReplyNeedsContentWhenEnabled() {
        VendorProfile p = shop();
        assertThatThrownBy(() -> p.configureAutoReply(true, " ", null)).isInstanceOf(IllegalArgumentException.class);
        p.configureAutoReply(true, null, "Shop đang bận");
        assertThat(p.getAutoReplyEnabled()).isTrue();
        p.configureAutoReply(false, null, null);
        assertThat(p.getAutoReplyEnabled()).isFalse();
    }

    @Test
    void pickupAndCommission() {
        VendorProfile p = shop();
        p.updatePickup("0912345678", 79, "Thành phố Hồ Chí Minh", "26740", "Phường Sài Gòn", "88 NVL", 884120);
        assertThat(p.isPickupConfigured()).isTrue();
        assertThat(p.getPickupDistrictId()).isNull();      // địa giới 2 cấp — không còn quận (D16)
        p.updatePickup("0912345678", 79, "Thành phố Hồ Chí Minh", "26740", "Phường Sài Gòn", "88 NVL", null);
        assertThat(p.isPickupConfigured()).isFalse();      // chưa có GHN Shop ID
        p.setNegotiatedCommission(new BigDecimal("0.0300"));
        assertThat(p.effectiveCommissionRate(new BigDecimal("0.08"))).isEqualByComparingTo("0.03");
        assertThatThrownBy(() -> p.setNegotiatedCommission(new BigDecimal("1.5"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void payoutAccountSavedOnlyWhenHolderMatchesKycName() {
        VendorProfile p = in(KycStatus.VERIFIED);
        assertThat(p.setPayoutAccount("Vietcombank", "0071000006712", "Đặng Quốc Huy", "DANG QUOC HUY", "Đặng  Quốc Huy", NOW))
                .isTrue();
        assertThat(p.isPayoutAccountVerified()).isTrue();
        assertThat(p.setPayoutAccount("ACB", "999", "Người Khác", "NGUOI KHAC", "Đặng Quốc Huy", NOW)).isFalse();
        assertThat(p.getPayoutBankAccountNumber()).isEqualTo("0071000006712");   // tài khoản cũ giữ nguyên
        assertThat(p.isPayoutAccountVerified()).isTrue();
        p.clearPayoutAccount();
        assertThat(p.getPayoutBankName()).isNull();
        assertThat(p.isPayoutAccountVerified()).isFalse();
    }
}
