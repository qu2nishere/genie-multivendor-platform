package com.genie.vendor.kyc.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.common.audit.AuditAction;
import com.genie.common.audit.AuditLogger;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.security.ClientInfo;
import com.genie.vendor.kyc.domain.KycDocumentType;
import com.genie.vendor.kyc.domain.KycResult;
import com.genie.vendor.kyc.domain.KycResultRepository;
import com.genie.vendor.shop.application.ShopFacade;
import com.genie.vendor.shop.domain.KycStatus;
import com.genie.vendor.shop.domain.VendorProfile;
import com.genie.vendor.shop.domain.VendorProfileRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hàng chờ & chi tiết hồ sơ KYC cho nhân viên — UC-VND-04 (API_SPEC 11.3; plan Mở shop & KYC K06.03–K06.04).
 * Nhóm lọc {@code PENDING} = {@code PENDING + AI_PROCESSING + MANUAL_REVIEW}; hàng chờ sắp rủi ro cao trước rồi nộp sớm trước.
 * Mỗi lần mở chi tiết: sinh bản xem tạm 5 phút cho từng giấy tờ và ghi {@code KYC_DOCUMENT_VIEWED} (NFR-07).
 */
@Service
public class KycQueryService {

    static final List<String> WAITING = List.of("PENDING", "AI_PROCESSING", "MANUAL_REVIEW");
    static final List<String> DECIDED_OR_WAITING = List.of("PENDING", "AI_PROCESSING", "MANUAL_REVIEW",
            "SUPPLEMENT_REQUESTED", "VERIFIED", "REJECTED");

    private final VendorProfileRepository profiles;
    private final KycResultRepository results;
    private final KycDossierStorage dossiers;
    private final ShopFacade shops;
    private final AuditLogger audit;
    private final ObjectMapper json;

    public KycQueryService(VendorProfileRepository profiles, KycResultRepository results, KycDossierStorage dossiers,
                           ShopFacade shops, AuditLogger audit, ObjectMapper json) {
        this.profiles = profiles;
        this.results = results;
        this.dossiers = dossiers;
        this.shops = shops;
        this.audit = audit;
        this.json = json;
    }

    public record ShopRef(long id, String name, String logoUrl) {
    }

    public record SubmissionRow(long id, String code, ShopRef shop, String ownerName, String businessType, String status,
                                OffsetDateTime submittedAt) {
    }

    public record Document(String type, String url, String contentType) {
    }

    public record AiChecks(Boolean qrCode, Boolean nationalEmblem, Boolean mrz, Boolean fingerprint, Boolean faceMatch) {
    }

    public record AiResultView(String overall, AiChecks checks, BigDecimal faceMatchScore, List<String> errors) {
    }

    public record Declared(String identityNumberMasked, String representative, String taxCode) {
    }

    public record Supplement(List<String> documents, String note, OffsetDateTime deadlineAt) {
    }

    /** {@code GET /admin/kyc-submissions/{id}} (API 11.3). {@code shop_commission_rate} theo phần trăm (3.0 = 3%). */
    public record SubmissionDetail(long id, String code, ShopRef shop, String ownerName, String businessType, String status,
                                   OffsetDateTime submittedAt, String riskLevel, Integer riskScore, boolean isRandomAudit,
                                   boolean isMall, List<Document> documents, AiResultView aiResult, Declared declared,
                                   BigDecimal shopCommissionRate, String rejectionReason, Supplement supplement,
                                   boolean manualReviewRequested) {
    }

    @Transactional(readOnly = true)
    public Page<SubmissionRow> search(String status, int page, int size) {
        List<String> statuses = status == null || status.isBlank() ? DECIDED_OR_WAITING
                : "PENDING".equals(status) ? WAITING : List.of(status);
        return profiles.findKycQueue(statuses, PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(size, 1), 100)))
                .map(this::row);
    }

    /** {@code summary.counts}: PENDING (gộp 3 trạng thái chờ), SUPPLEMENT_REQUESTED, VERIFIED, REJECTED, ALL. */
    @Transactional(readOnly = true)
    public Map<String, Long> counts() {
        Map<KycStatus, Long> raw = new EnumMap<>(KycStatus.class);
        for (Object[] r : profiles.countByKycStatus()) {
            raw.put((KycStatus) r[0], (Long) r[1]);
        }
        long pending = raw.getOrDefault(KycStatus.PENDING, 0L) + raw.getOrDefault(KycStatus.AI_PROCESSING, 0L)
                + raw.getOrDefault(KycStatus.MANUAL_REVIEW, 0L);
        Map<String, Long> out = new LinkedHashMap<>();
        out.put("PENDING", pending);
        out.put("SUPPLEMENT_REQUESTED", raw.getOrDefault(KycStatus.SUPPLEMENT_REQUESTED, 0L));
        out.put("VERIFIED", raw.getOrDefault(KycStatus.VERIFIED, 0L));
        out.put("REJECTED", raw.getOrDefault(KycStatus.REJECTED, 0L));
        out.put("ALL", out.values().stream().mapToLong(Long::longValue).sum());
        return out;
    }

    /** Chi tiết + URL ảnh 5 phút; ghi nhật ký mỗi lần nhân viên mở (không ghi khi gọi lại sau thao tác duyệt). */
    @Transactional
    public SubmissionDetail detail(long id, Long reviewerId, ClientInfo client, boolean logView) {
        VendorProfile p = profiles.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (p.getKycStatus() == KycStatus.NOT_SUBMITTED) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        KycResult r = results.findByVendorProfileId(id).orElse(null);
        Map<String, Map<String, Object>> meta = readMeta(p.getKycDocumentsMetaJson());
        List<Document> docs = new ArrayList<>();
        for (KycDocumentType t : KycDocumentType.values()) {
            String key = KycDocuments.keyOf(p, t);
            if (key == null || key.isBlank() || !key.endsWith(".enc")) {
                continue;   // dữ liệu mẫu chưa có ảnh thật
            }
            Map<String, Object> m = meta.get(t.name());
            String type = m == null ? "application/octet-stream" : String.valueOf(m.get("content_type"));
            docs.add(new Document(t.name(), dossiers.openTemporaryView(p.getId(), t, key, type), type));
        }
        if (logView) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("documents", docs.stream().map(Document::type).toList());
            audit.record(AuditAction.KYC_DOCUMENT_VIEWED, reviewerId, String.valueOf(id), details, client, true);
        }
        Integer score = r == null ? null : r.getRiskScore();
        boolean aiDone = r != null && r.getProcessedAt() != null && r.getRiskScore() != null;
        AiResultView ai = aiDone ? new AiResultView(r.getOverallStatus().name(),
                new AiChecks(r.getQrCodeDetected(), null, r.getMrzValid(), null, r.getFaceMatchResult()),
                r.getFaceMatchScore(), readList(r.getErrors())) : null;
        KycStatus s = p.getKycStatus();
        Supplement supplement = s == KycStatus.SUPPLEMENT_REQUESTED
                ? new Supplement(readList(p.getKycSupplementDocumentsJson()), p.getKycRejectionReason(),
                p.getKycSupplementDeadlineAt())
                : null;
        BigDecimal commission = p.getCommissionRate() == null ? null
                : p.getCommissionRate().multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
        SubmissionRow row = row(p);
        return new SubmissionDetail(row.id(), row.code(), row.shop(), row.ownerName(), row.businessType(), row.status(),
                row.submittedAt(), riskLevel(score), score, r != null && Boolean.TRUE.equals(r.getSampledForReview()),
                Boolean.TRUE.equals(p.getIsMall()), docs, ai,
                new Declared(maskIdentity(p.getIdentityCode()), p.getRepresentative(), p.getTaxCode()), commission,
                s == KycStatus.REJECTED ? p.getKycRejectionReason() : null, supplement, p.isManualReviewRequested());
    }

    private SubmissionRow row(VendorProfile p) {
        return new SubmissionRow(p.getId(), "KYC-" + p.getId(),
                new ShopRef(p.getId(), p.getShopName(), shops.mediaUrl(p.getLogoUrl())), p.getRepresentative(),
                p.getBusinessType() == null ? null : p.getBusinessType().name(), p.getKycStatus().name(),
                p.getKycSubmittedAt());
    }

    /** {@code LOW < 30 ≤ MEDIUM < 70 ≤ HIGH} (API 11.3); chưa có điểm → null. */
    static String riskLevel(Integer score) {
        if (score == null) {
            return null;
        }
        return score >= 70 ? "HIGH" : score >= 30 ? "MEDIUM" : "LOW";
    }

    /** {@code 079096001041} → {@code 0790•••••041} (giữ 4 đầu, 3 cuối). */
    static String maskIdentity(String id) {
        if (id == null || id.length() < 8) {
            return id == null ? null : "•".repeat(id.length());
        }
        return id.substring(0, 4) + "•".repeat(id.length() - 7) + id.substring(id.length() - 3);
    }

    private Map<String, Map<String, Object>> readMeta(String raw) {
        try {
            return raw == null ? Map.of() : json.readValue(raw, new TypeReference<Map<String, Map<String, Object>>>() { });
        } catch (Exception e) {
            return Map.of();
        }
    }

    List<String> readList(String raw) {
        try {
            return raw == null || raw.isBlank() ? List.of() : json.readValue(raw, new TypeReference<List<String>>() { });
        } catch (Exception e) {
            return List.of(raw);
        }
    }
}
