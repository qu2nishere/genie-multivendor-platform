package com.genie.vendor.kyc.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.common.audit.AuditAction;
import com.genie.common.audit.AuditLogger;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.security.ClientInfo;
import com.genie.vendor.kyc.domain.KycDocumentType;
import com.genie.vendor.kyc.event.KycSubmittedEvent;
import com.genie.vendor.shop.application.SellerContext;
import com.genie.vendor.shop.application.ShopNamePolicy;
import com.genie.vendor.shop.domain.BusinessType;
import com.genie.vendor.shop.domain.KycStatus;
import com.genie.vendor.shop.domain.VendorProfile;
import com.genie.vendor.shop.domain.VendorProfileRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Hồ sơ KYC phía người bán — UC-VND-02 (API_SPEC 10.12; plan Mở shop & KYC K04.05–K04.08). Shop luôn lấy theo người gọi
 * (N10). Giấy tờ được niêm phong (mã hóa) ngay trong request; transaction rollback thì xóa tệp mới, commit thì xóa tệp cũ
 * bị thay.
 */
@Service
public class KycSubmissionService {

    static final Pattern IDENTITY = Pattern.compile("^\\d{12}$");
    static final Pattern IDENTITY_DRAFT = Pattern.compile("^\\d{0,12}$");
    static final Pattern TAX_CODE = Pattern.compile("^\\d{10}(-\\d{3})?$");

    private final VendorProfileRepository profiles;
    private final SellerContext sellers;
    private final KycDossierStorage dossiers;
    private final ShopNamePolicy names;
    private final AuditLogger audit;
    private final ApplicationEventPublisher events;
    private final ObjectMapper json;
    private final Clock clock;

    public KycSubmissionService(VendorProfileRepository profiles, SellerContext sellers, KycDossierStorage dossiers,
                                ShopNamePolicy names, AuditLogger audit, ApplicationEventPublisher events, ObjectMapper json,
                                Clock clock) {
        this.profiles = profiles;
        this.sellers = sellers;
        this.dossiers = dossiers;
        this.names = names;
        this.audit = audit;
        this.events = events;
        this.json = json;
        this.clock = clock;
    }

    /** Dữ liệu form KYC (lưu nháp: mọi trường tùy chọn). {@code documentFileNames} — tên tệp gốc để hiển thị (tùy chọn). */
    public record KycForm(String shopName, BusinessType businessType, String identityNumber, String taxCode,
                          String representative, Map<KycDocumentType, String> documentObjectKeys,
                          Map<KycDocumentType, String> documentFileNames, Boolean biometricConsent) {
    }

    public record DocumentInfo(String fileName, long sizeBytes) {
    }

    public record Supplement(List<String> documents, String note, OffsetDateTime deadlineAt) {
    }

    /** {@code GET /seller/kyc} — KHÔNG có URL ảnh (N4). */
    public record KycDossierView(String status, String shopName, String businessType, String identityNumber,
                                 String taxCode, String representative, Map<String, DocumentInfo> documents,
                                 boolean biometricConsent, String rejectionReason, Supplement supplement,
                                 boolean manualReviewRequested, OffsetDateTime submittedAt) {
    }

    public record SubmitResult(String status) {
    }

    @Transactional(readOnly = true)
    public KycDossierView get(long userId) {
        return view(load(userId, false));
    }

    @Transactional
    public KycDossierView saveDraft(long userId, KycForm form) {
        VendorProfile p = load(userId, true);
        requireEditable(p);
        if (form.identityNumber() != null && !IDENTITY_DRAFT.matcher(form.identityNumber()).matches()) {
            throw invalid("identity_number", "Số CCCD gồm 12 chữ số.");
        }
        String shopName = form.shopName() == null || form.shopName().isBlank() ? null : names.validate(form.shopName(), p.getId());
        attach(userId, p, form);
        p.saveKycDraft(form.identityNumber(), form.taxCode(), trimOrNull(form.representative()), shopName,
                form.businessType(), form.biometricConsent());
        return view(p);
    }

    @Transactional
    public SubmitResult submit(long userId, KycForm form, ClientInfo client) {
        VendorProfile p = load(userId, true);
        requireEditable(p);
        BusinessType type = form.businessType() != null ? form.businessType() : p.getBusinessType();
        if (type == null) {
            throw invalid("business_type", "Chọn loại hình kinh doanh.");
        }
        if (form.identityNumber() == null || !IDENTITY.matcher(form.identityNumber()).matches()) {
            throw invalid("identity_number", "Số CCCD gồm đúng 12 chữ số.");
        }
        if (form.representative() == null || form.representative().isBlank()) {
            throw invalid("representative", "Nhập họ tên người đại diện.");
        }
        boolean business = type != BusinessType.INDIVIDUAL;
        if (business && (form.taxCode() == null || !TAX_CODE.matcher(form.taxCode().trim()).matches())) {
            throw invalid("tax_code", "Mã số thuế gồm 10 hoặc 13 chữ số (dạng 0312345678-001).");
        }
        List<String> missing = missingDocuments(p, form, business);
        if (!missing.isEmpty()) {
            throw new BusinessException(ErrorCode.KYC_DOCUMENTS_MISSING, Map.of("missing", missing));
        }
        String shopName = form.shopName() == null || form.shopName().isBlank() ? null : names.validate(form.shopName(), p.getId());
        boolean consent = Boolean.TRUE.equals(form.biometricConsent());
        attach(userId, p, form);
        OffsetDateTime now = OffsetDateTime.now(clock);
        p.saveKycDraft(form.identityNumber(), business ? form.taxCode().trim() : "", form.representative().trim(), shopName,
                type, consent);
        p.submitKyc(now);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("biometric_consent", consent);
        details.put("consent_at", consent ? now.toString() : null);
        details.put("business_type", type.name());
        audit.record(AuditAction.KYC_SUBMITTED, userId, String.valueOf(p.getId()), details, client, true);
        events.publishEvent(new KycSubmittedEvent(p.getId(), userId));
        return new SubmitResult(p.getKycStatus().name());
    }

    @Transactional
    public void requestManualReview(long userId) {
        VendorProfile p = load(userId, true);
        if (p.isManualReviewRequested()) {
            throw new BusinessException(ErrorCode.KYC_REVIEW_ALREADY_REQUESTED);
        }
        if (p.getKycStatus() != KycStatus.REJECTED) {
            throw new BusinessException(ErrorCode.KYC_NOT_EDITABLE, Map.of("kyc_status", p.getKycStatus().name()));
        }
        p.requestManualReview(OffsetDateTime.now(clock));
    }

    // ===== nội bộ =====

    private VendorProfile load(long userId, boolean forUpdate) {
        SellerContext.Seller seller = sellers.current(userId);
        return (forUpdate ? profiles.findByIdForUpdate(seller.vendorProfileId()) : profiles.findById(seller.vendorProfileId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));
    }

    private static void requireEditable(VendorProfile p) {
        if (!p.isKycEditable()) {
            throw new BusinessException(ErrorCode.KYC_NOT_EDITABLE, Map.of("kyc_status", p.getKycStatus().name()));
        }
    }

    private List<String> missingDocuments(VendorProfile p, KycForm form, boolean business) {
        Map<KycDocumentType, String> sent = form.documentObjectKeys() == null ? Map.of() : form.documentObjectKeys();
        List<String> missing = new ArrayList<>();
        for (KycDocumentType t : KycDocumentType.values()) {
            boolean required = t != KycDocumentType.BUSINESS_LICENSE || business;
            boolean has = present(sent.get(t)) || present(KycDocuments.keyOf(p, t));
            if (required && !has) {
                missing.add(t.name());
            }
        }
        return missing;
    }

    /** Niêm phong các tệp mới gửi và gắn vào hồ sơ; dọn tệp theo kết quả transaction. */
    private void attach(long userId, VendorProfile p, KycForm form) {
        Map<KycDocumentType, String> sent = form.documentObjectKeys() == null ? Map.of() : form.documentObjectKeys();
        if (sent.values().stream().noneMatch(KycSubmissionService::present)) {
            return;
        }
        Map<String, Map<String, Object>> meta = readMeta(p.getKycDocumentsMetaJson());
        Map<KycDocumentType, String> newKeys = new EnumMap<>(KycDocumentType.class);
        List<String> created = new ArrayList<>();
        List<String> replaced = new ArrayList<>();
        afterTransaction(created, replaced);
        for (Map.Entry<KycDocumentType, String> e : sent.entrySet()) {
            if (!present(e.getValue())) {
                continue;
            }
            String fileName = form.documentFileNames() == null ? null : form.documentFileNames().get(e.getKey());
            KycDossierStorage.SealedDocument d = dossiers.seal(userId, p.getId(), e.getKey(), e.getValue(), fileName);
            created.add(d.objectKey());
            String old = KycDocuments.keyOf(p, e.getKey());
            if (present(old)) {
                replaced.add(old);
            }
            newKeys.put(e.getKey(), d.objectKey());
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("file_name", d.fileName());
            info.put("size_bytes", d.sizeBytes());
            info.put("content_type", d.contentType());
            meta.put(e.getKey().name(), info);
        }
        p.attachKycDocuments(newKeys.get(KycDocumentType.ID_FRONT), newKeys.get(KycDocumentType.ID_BACK),
                newKeys.get(KycDocumentType.BUSINESS_LICENSE), newKeys.get(KycDocumentType.SELFIE), writeMeta(meta));
    }

    private void afterTransaction(List<String> created, List<String> replaced) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                List<String> garbage = status == STATUS_COMMITTED ? replaced : created;
                garbage.forEach(dossiers::deleteQuietly);
            }
        });
    }

    KycDossierView view(VendorProfile p) {
        Map<String, Map<String, Object>> meta = readMeta(p.getKycDocumentsMetaJson());
        Map<String, DocumentInfo> docs = new LinkedHashMap<>();
        for (KycDocumentType t : KycDocumentType.values()) {
            Map<String, Object> m = present(KycDocuments.keyOf(p, t)) ? meta.get(t.name()) : null;
            docs.put(t.name(), m == null ? null
                    : new DocumentInfo(String.valueOf(m.get("file_name")), ((Number) m.get("size_bytes")).longValue()));
        }
        KycStatus s = p.getKycStatus();
        Supplement supplement = s == KycStatus.SUPPLEMENT_REQUESTED
                ? new Supplement(readList(p.getKycSupplementDocumentsJson()), p.getKycRejectionReason(),
                p.getKycSupplementDeadlineAt())
                : null;
        return new KycDossierView(s.name(), p.getShopName(), p.getBusinessType() == null ? null : p.getBusinessType().name(),
                p.getIdentityCode(), p.getTaxCode(), p.getRepresentative(), docs,
                Boolean.TRUE.equals(p.getKycBiometricConsent()),
                s == KycStatus.REJECTED ? p.getKycRejectionReason() : null, supplement, p.isManualReviewRequested(),
                p.getKycSubmittedAt());
    }

    private Map<String, Map<String, Object>> readMeta(String raw) {
        if (raw == null || raw.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return json.readValue(raw, new TypeReference<LinkedHashMap<String, Map<String, Object>>>() { });
        } catch (JsonProcessingException e) {
            return new LinkedHashMap<>();
        }
    }

    private String writeMeta(Map<String, Map<String, Object>> meta) {
        try {
            return json.writeValueAsString(meta);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    List<String> readList(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            return json.readValue(raw, new TypeReference<List<String>>() { });
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private static boolean present(String s) {
        return s != null && !s.isBlank();
    }

    private static String trimOrNull(String s) {
        return s == null ? null : s.trim();
    }

    private static BusinessException invalid(String field, String message) {
        return new BusinessException(ErrorCode.VALIDATION_FAILED, message, Map.of("field", field));
    }
}
