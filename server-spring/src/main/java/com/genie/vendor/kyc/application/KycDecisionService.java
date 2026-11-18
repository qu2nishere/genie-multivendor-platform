package com.genie.vendor.kyc.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.common.audit.AuditAction;
import com.genie.common.audit.AuditLogger;
import com.genie.common.domain.NameNormalizer;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.vendor.kyc.domain.KycOverallResult;
import com.genie.vendor.kyc.domain.KycResult;
import com.genie.vendor.kyc.domain.KycResultRepository;
import com.genie.vendor.kyc.event.KycDecidedEvent;
import com.genie.vendor.shop.domain.KycStatus;
import com.genie.vendor.shop.domain.KycVerificationMethod;
import com.genie.vendor.shop.domain.VendorProfile;
import com.genie.vendor.shop.domain.VendorProfileRepository;
import com.genie.vendor.shop.event.ShopVerifiedEvent;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Áp kết quả thẩm định AI vào hồ sơ — UC-VND-03 (plan Mở shop & KYC K05.07–K05.08; ADR 0011 mục 5):
 * <ol>
 *   <li>khóa dòng hồ sơ; không còn {@code AI_PROCESSING} → bỏ qua (callback lặp / đến muộn — idempotent, N8);</li>
 *   <li>bổ sung tín hiệu phía backend: {@code DUPLICATE_ID} (CCCD đã gắn shop khác đang VERIFIED),
 *       {@code DECLARED_DATA_MISMATCH} (số CCCD / họ tên kê khai khác dữ liệu QR sau chuẩn hóa); không đồng ý sinh trắc → bỏ
 *       tín hiệu khuôn mặt và LUÔN chuyển duyệt tay;</li>
 *   <li>tính điểm ({@link KycRiskPolicy}), lưu {@code kyc_results} (1 dòng / shop), chốt PASS / PARTIAL / FAIL;</li>
 *   <li>PASS → {@code VERIFIED (OCR_AUTO)}; PARTIAL → {@code MANUAL_REVIEW}; FAIL → {@code REJECTED} kèm lý do theo tín hiệu —
 *       trừ hồ sơ đã từng yêu cầu nhân viên xem xét thì vào {@code MANUAL_REVIEW} (D8, N6).</li>
 * </ol>
 */
@Service
public class KycDecisionService {

    private final VendorProfileRepository profiles;
    private final KycResultRepository results;
    private final KycRiskPolicy policy;
    private final AuditLogger audit;
    private final ApplicationEventPublisher events;
    private final ObjectMapper json;
    private final Clock clock;

    public KycDecisionService(VendorProfileRepository profiles, KycResultRepository results, KycRiskPolicy policy,
                              AuditLogger audit, ApplicationEventPublisher events, ObjectMapper json, Clock clock) {
        this.profiles = profiles;
        this.results = results;
        this.policy = policy;
        this.audit = audit;
        this.events = events;
        this.json = json;
        this.clock = clock;
    }

    @Transactional
    public KycStatus apply(long vendorProfileId, KycAiResult r) {
        VendorProfile p = profiles.findByIdForUpdate(vendorProfileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (p.getKycStatus() != KycStatus.AI_PROCESSING) {
            return p.getKycStatus();
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        boolean consent = Boolean.TRUE.equals(p.getKycBiometricConsent());
        KycAiResult.Checks checks = r.checks() == null ? KycAiResult.Checks.empty() : r.checks();
        KycAiResult.Extracted ex = r.extracted();

        Set<String> signals = new LinkedHashSet<>(r.signals() == null ? List.of() : r.signals());
        boolean duplicate = p.getIdentityCode() != null && profiles.existsVerifiedWithIdentity(p.getIdentityCode(), p.getId());
        if (duplicate) {
            signals.add("DUPLICATE_ID");
        }
        if (declaredMismatch(p, ex)) {
            signals.add("DECLARED_DATA_MISMATCH");
        }
        if (!consent) {
            signals.removeAll(KycSignals.FACE);
        }
        KycRiskPolicy.Assessment a = policy.assess(signals);

        KycResult result = resultOf(p);
        fill(result, checks, ex, duplicate, consent ? r : null);
        result.setErrors(write(r.errors() == null ? List.of() : r.errors()));
        KycOverallResult overall = result.applyRiskScore(a.score(), write(KycRiskPolicy.toJsonList(a.signals())), a.sampled(),
                policy.autoApproveBelow(), policy.rejectFrom(), now);
        results.save(result);

        List<String> codes = a.signals().stream().map(KycRiskPolicy.ScoredSignal::code).toList();
        if (!consent || overall == KycOverallResult.PARTIAL
                || (overall == KycOverallResult.FAIL && p.isManualReviewRequested())) {
            p.routeToManualReview();
        } else if (overall == KycOverallResult.PASS) {
            p.approveKyc(null, KycVerificationMethod.OCR_AUTO, now);
            audit.record(AuditAction.KYC_APPROVED, (Long) null, String.valueOf(p.getId()),
                    details("OCR_AUTO", a.score(), codes), null, true);
            events.publishEvent(new ShopVerifiedEvent(p.getId(), p.getUserId()));
            events.publishEvent(new KycDecidedEvent(p.getId(), p.getUserId(), KycStatus.VERIFIED.name(), null, List.of(), null));
        } else {
            String reason = KycSignals.rejectionReason(codes);
            p.rejectKyc(reason);
            audit.record(AuditAction.KYC_REJECTED, (Long) null, String.valueOf(p.getId()),
                    details("OCR_AUTO", a.score(), codes), null, true);
            events.publishEvent(new KycDecidedEvent(p.getId(), p.getUserId(), KycStatus.REJECTED.name(), reason, List.of(),
                    null));
        }
        return p.getKycStatus();
    }

    /**
     * AI Worker lỗi / không gọi lại kịp → chuyển nhân viên duyệt, ghi lỗi vào {@code kyc_results.errors} (N7). Hồ sơ đã
     * rời {@code AI_PROCESSING} thì bỏ qua.
     */
    @Transactional
    public KycStatus failToManual(long vendorProfileId, String errorCode) {
        VendorProfile p = profiles.findByIdForUpdate(vendorProfileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return failToManual(p, errorCode);
    }

    /** Job quá hạn: mọi hồ sơ {@code AI_PROCESSING} nộp trước {@code now - aiTimeout} → duyệt tay; trả số hồ sơ đã chuyển. */
    @Transactional
    public int timeoutStuck(java.time.Duration aiTimeout) {
        List<VendorProfile> stuck = profiles.findStuckAiProcessing(OffsetDateTime.now(clock).minus(aiTimeout));
        stuck.forEach(p -> failToManual(p, "AI_TIMEOUT"));
        return stuck.size();
    }

    private KycStatus failToManual(VendorProfile p, String errorCode) {
        if (p.getKycStatus() != KycStatus.AI_PROCESSING) {
            return p.getKycStatus();
        }
        KycResult result = resultOf(p);
        List<String> errors = new ArrayList<>(read(result.getErrors()));
        errors.add(errorCode);
        result.setErrors(write(errors));
        result.setOverallStatus(KycOverallResult.PARTIAL);
        result.setProcessedAt(OffsetDateTime.now(clock));
        results.save(result);
        p.routeToManualReview();
        return p.getKycStatus();
    }

    private KycResult resultOf(VendorProfile p) {
        return results.findByVendorProfileId(p.getId())
                .orElseGet(() -> KycResult.builder().vendorProfileId(p.getId()).vendorProfile(p)
                        .overallStatus(KycOverallResult.PARTIAL).sampledForReview(false).build());
    }

    private static void fill(KycResult k, KycAiResult.Checks c, KycAiResult.Extracted ex, boolean duplicate, KycAiResult face) {
        k.setQrCodeDetected(c.qrCode());
        k.setMrzValid(c.mrz());
        k.setMrzQrMatch(c.mrzQrMatch());
        k.setCccdNumberValid(c.cccdNumberValid());
        k.setExpiryRuleValid(c.expiryRuleValid());
        k.setDuplicateIdFound(duplicate);
        k.setFaceMatchResult(face == null ? null : c.faceMatch());
        k.setFaceDuplicateFound(face == null ? null : c.faceDuplicate());
        k.setFaceMatchScore(face == null ? null : face.faceMatchScore());
        k.setLivenessScore(face == null ? null : face.livenessScore());
        if (ex != null) {
            k.setExtractedIdNumber(cut(ex.identityNumber(), 20));
            k.setExtractedFullName(cut(ex.fullName(), 255));
            k.setExtractedDob(cut(ex.dateOfBirth(), 20));
            k.setExtractedGender(cut(ex.gender(), 10));
            k.setExtractedAddress(ex.address());
            k.setExtractedIssueDate(cut(ex.issueDate(), 20));
            k.setExtractedExpiryDate(cut(ex.expiryDate(), 20));
            k.setExtractedNationality("VNM");
        }
    }

    private static boolean declaredMismatch(VendorProfile p, KycAiResult.Extracted ex) {
        if (ex == null) {
            return false;
        }
        boolean idDiff = ex.identityNumber() != null && p.getIdentityCode() != null
                && !Objects.equals(ex.identityNumber().trim(), p.getIdentityCode().trim());
        boolean nameDiff = ex.fullName() != null && p.getRepresentative() != null
                && !NameNormalizer.normalize(ex.fullName()).equals(NameNormalizer.normalize(p.getRepresentative()));
        return idDiff || nameDiff;
    }

    private static Map<String, Object> details(String method, int score, List<String> signals) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("method", method);
        d.put("risk_score", score);
        d.put("signals", signals);
        return d;
    }

    private static String cut(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }

    private String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private List<String> read(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            return json.readValue(raw, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() { });
        } catch (JsonProcessingException e) {
            return List.of(raw);
        }
    }
}
