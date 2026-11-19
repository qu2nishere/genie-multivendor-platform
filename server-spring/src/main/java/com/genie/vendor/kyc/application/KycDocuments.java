package com.genie.vendor.kyc.application;

import com.genie.vendor.kyc.domain.KycDocumentType;
import com.genie.vendor.shop.domain.VendorProfile;

/** Ánh xạ loại giấy tờ ↔ cột ảnh của {@link VendorProfile} (khóa đối tượng {@code .enc}). */
final class KycDocuments {

    private KycDocuments() {
    }

    static String keyOf(VendorProfile p, KycDocumentType type) {
        return switch (type) {
            case ID_FRONT -> p.getIdFrontUrl();
            case ID_BACK -> p.getIdBackUrl();
            case BUSINESS_LICENSE -> p.getLicenseUrl();
            case SELFIE -> p.getSelfieUrl();
        };
    }
}
