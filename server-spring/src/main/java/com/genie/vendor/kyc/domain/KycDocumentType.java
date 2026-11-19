package com.genie.vendor.kyc.domain;

/**
 * Loại giấy tờ KYC (API_SPEC 16 {@code KycDocumentType}; plan Mở shop & KYC K04.01) ↔ cột ảnh của {@code VendorProfile}.
 * {@code slug} là tiền tố tên tệp đã mã hóa: {@code kyc-dossiers/vendor/{vendorProfileId}/{slug}-{uuid}.enc}.
 */
public enum KycDocumentType {
    ID_FRONT("front"),            // ↔ VendorProfile.idFrontUrl
    ID_BACK("back"),              // ↔ VendorProfile.idBackUrl
    BUSINESS_LICENSE("license"),  // ↔ VendorProfile.licenseUrl
    SELFIE("selfie");             // ↔ VendorProfile.selfieUrl

    private final String slug;

    KycDocumentType(String slug) {
        this.slug = slug;
    }

    public String slug() {
        return slug;
    }

    /** AAD của AES-GCM: gắn bản mã với đúng shop + loại giấy tờ (ADR 0011 mục 4). */
    public String aad(long vendorProfileId) {
        return vendorProfileId + ":" + name();
    }
}
