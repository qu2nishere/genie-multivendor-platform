package com.genie.vendor.kyc.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.common.audit.AuditAction;
import com.genie.common.audit.AuditLogger;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.security.ClientInfo;
import com.genie.vendor.kyc.domain.KycRejectReason;
import com.genie.vendor.kyc.event.KycDecidedEvent;
import com.genie.vendor.shop.domain.KycStatus;
import com.genie.vendor.shop.domain.KycVerificationMethod;
import com.genie.vendor.shop.domain.VendorProfile;
import com.genie.vendor.shop.domain.VendorProfileRepository;
import com.genie.vendor.shop.event.ShopVerifiedEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nhân viên duyệt tay hồ sơ KYC — UC-VND-04 (API_SPEC 11.3; plan Mở shop & KYC K06.05–K06.08). Duyệt / từ chối / yêu cầu
 * bổ sung chỉ từ {@code MANUAL_REVIEW} (khác → {@code 409 GNE-VND-4095}); đưa lại hàng chờ từ hồ sơ đã quyết định (D9).
 * Mọi hành động ghi {@code system_logs}; quyết định gửi email cho chủ shop sau commit ({@link KycNotifier}).
 */
@Service
public class KycReviewService {

    /** Giấy tờ được yêu cầu bổ sung — {@code KycDocumentType} của API 16.1. */
    static final Set<String> SUPPLEMENT_DOCUMENTS = Set.of("ID_FRONT", "ID_BACK", "BUSINESS_LICENSE",
            "BUSINESS_REGISTRATION", "SELFIE", "TAX_NOTICE");
    static final BigDecimal MAX_COMMISSION_PERCENT = new BigDecimal("50");

    private final VendorProfileRepository profiles;
    private final AuditLogger audit;
    private final ApplicationEventPublisher events;
    private final KycProperties props;
    private final ObjectMapper json;
    private final Clock clock;

    public KycReviewService(VendorProfileRepository profiles, AuditLogger audit, ApplicationEventPublisher events,
                            KycProperties props, ObjectMapper json, Clock clock) {
        this.profiles = profiles;
        this.audit = audit;
        this.events = events;
        this.props = props;
        this.json = json;
        this.clock = clock;
    }

    /**
     * Duyệt. {@code commissionPercent} (vd 3.0 = 3%) chỉ nhận khi {@code canUpdateCommission} (quyền {@code UPDATE_COMMISSION}
     * — D10), không có quyền → {@code 403}; {@code isMall} gắn huy hiệu Chính hãng.
     */
    @Transactional
    public void approve(long id, long staffId, BigDecimal commissionPercent, Boolean isMall, boolean canUpdateCommission,
                        ClientInfo client) {
        VendorProfile p = load(id);
        requireManualReview(p);
        if (commissionPercent != null) {
            if (!canUpdateCommission) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "Cần quyền UPDATE_COMMISSION để đặt hoa hồng riêng.");
            }
            if (commissionPercent.signum() < 0 || commissionPercent.compareTo(MAX_COMMISSION_PERCENT) > 0) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Hoa hồng riêng phải trong khoảng 0–50%.",
                        Map.of("field", "shop_commission_rate"));
            }
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        p.approveKyc(staffId, KycVerificationMethod.MANUAL_STAFF, now);
        if (isMall != null) {
            p.markAsMall(isMall);
        }
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("method", "MANUAL_STAFF");
        details.put("is_mall", Boolean.TRUE.equals(p.getIsMall()));
        if (commissionPercent != null) {
            BigDecimal rate = commissionPercent.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("shop_id", p.getId());
            c.put("from", p.getCommissionRate());
            c.put("to", rate);
            p.setNegotiatedCommission(rate);
            audit.record(AuditAction.COMMISSION_UPDATED, staffId, String.valueOf(p.getId()), c, client, true);
            details.put("shop_commission_rate", commissionPercent);
        }
        audit.record(AuditAction.KYC_APPROVED, staffId, String.valueOf(p.getId()), details, client, true);
        events.publishEvent(new ShopVerifiedEvent(p.getId(), p.getUserId()));
        events.publishEvent(new KycDecidedEvent(p.getId(), p.getUserId(), KycStatus.VERIFIED.name(), null, List.of(), null));
    }

    @Transactional
    public void reject(long id, long staffId, KycRejectReason reason, String note, ClientInfo client) {
        if (reason == KycRejectReason.OTHER && (note == null || note.isBlank())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Nhập ghi chú cho lý do khác.", Map.of("field", "note"));
        }
        VendorProfile p = load(id);
        requireManualReview(p);
        String text = reason.reasonWith(note);
        p.rejectKyc(text);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("reason_code", reason.name());
        details.put("note", note);
        audit.record(AuditAction.KYC_REJECTED, staffId, String.valueOf(p.getId()), details, client, true);
        events.publishEvent(new KycDecidedEvent(p.getId(), p.getUserId(), KycStatus.REJECTED.name(), text, List.of(), null));
    }

    @Transactional
    public void requestSupplement(long id, long staffId, List<String> documents, String note, ClientInfo client) {
        Set<String> docs = new LinkedHashSet<>(documents == null ? List.of() : documents);
        if (docs.isEmpty() || !SUPPLEMENT_DOCUMENTS.containsAll(docs)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Chọn ít nhất 1 giấy tờ cần bổ sung.",
                    Map.of("field", "documents"));
        }
        VendorProfile p = load(id);
        requireManualReview(p);
        OffsetDateTime deadline = OffsetDateTime.now(clock).plusDays(props.supplementDays());
        String trimmed = note == null ? null : note.trim();
        p.requestSupplement(write(List.copyOf(docs)), trimmed, deadline);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("documents", List.copyOf(docs));
        details.put("note", trimmed);
        details.put("deadline_at", deadline.toString());
        audit.record(AuditAction.KYC_SUPPLEMENT_REQUESTED, staffId, String.valueOf(p.getId()), details, client, true);
        events.publishEvent(new KycDecidedEvent(p.getId(), p.getUserId(), KycStatus.SUPPLEMENT_REQUESTED.name(), trimmed,
                List.copyOf(docs), deadline));
    }

    @Transactional
    public void requeue(long id, long staffId, ClientInfo client) {
        VendorProfile p = load(id);
        String from = p.getKycStatus().name();
        try {
            p.requeueForReview();
        } catch (IllegalStateException e) {
            throw notEditable(p);
        }
        audit.record(AuditAction.KYC_REQUEUED, staffId, String.valueOf(p.getId()), Map.of("from", from), client, true);
    }

    private VendorProfile load(long id) {
        VendorProfile p = profiles.findByIdForUpdate(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (p.getKycStatus() == KycStatus.NOT_SUBMITTED) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return p;
    }

    private static void requireManualReview(VendorProfile p) {
        if (p.getKycStatus() != KycStatus.MANUAL_REVIEW) {
            throw notEditable(p);
        }
    }

    private static BusinessException notEditable(VendorProfile p) {
        return new BusinessException(ErrorCode.KYC_NOT_EDITABLE, "Hồ sơ không ở trạng thái cho phép thao tác này.",
                Map.of("kyc_status", p.getKycStatus().name()));
    }

    private String write(Object v) {
        try {
            return json.writeValueAsString(v);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
