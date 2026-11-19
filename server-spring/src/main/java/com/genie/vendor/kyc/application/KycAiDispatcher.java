package com.genie.vendor.kyc.application;

import com.genie.common.ai.AiWorkerProperties;
import com.genie.vendor.kyc.domain.KycDocumentType;
import com.genie.vendor.kyc.event.KycSubmittedEvent;
import com.genie.vendor.shop.domain.KycStatus;
import com.genie.vendor.shop.domain.VendorProfile;
import com.genie.vendor.shop.domain.VendorProfileRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Gửi hồ sơ vừa nộp cho AI Worker — bất đồng bộ SAU commit (UC-VND-03; plan Mở shop & KYC N7, K05.06):
 * transaction mới chuyển {@code PENDING → AI_PROCESSING}, mở bản xem tạm 5 phút cho 3 ảnh rồi gọi
 * {@link KycVerificationClient}. Gọi lỗi (AI Worker tắt, 5xx, timeout) → chuyển duyệt tay ngay, không để hồ sơ kẹt.
 */
@Component
public class KycAiDispatcher {

    private static final Logger log = LoggerFactory.getLogger(KycAiDispatcher.class);

    private final VendorProfileRepository profiles;
    private final KycDossierStorage dossiers;
    private final KycVerificationClient client;
    private final KycDecisionService decisions;
    private final AiWorkerProperties aiWorker;
    private final ObjectMapper json;
    private final TransactionTemplate tx;

    public KycAiDispatcher(VendorProfileRepository profiles, KycDossierStorage dossiers, KycVerificationClient client,
                           KycDecisionService decisions, AiWorkerProperties aiWorker, ObjectMapper json,
                           PlatformTransactionManager txManager) {
        this.profiles = profiles;
        this.dossiers = dossiers;
        this.client = client;
        this.decisions = decisions;
        this.aiWorker = aiWorker;
        this.json = json;
        this.tx = new TransactionTemplate(txManager);
        this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubmitted(KycSubmittedEvent event) {
        dispatch(event.vendorProfileId());
    }

    /** Ảnh chụp các trường cần gửi, lấy trong transaction chuyển trạng thái. */
    private record Snapshot(long id, String identity, String fullName, boolean consent, Map<KycDocumentType, String> keys,
                            Map<String, Map<String, Object>> meta) {
    }

    public void dispatch(long vendorProfileId) {
        Snapshot s = tx.execute(status -> {
            VendorProfile p = profiles.findByIdForUpdate(vendorProfileId).orElse(null);
            if (p == null || p.getKycStatus() != KycStatus.PENDING) {
                return null;
            }
            p.startAiProcessing();
            Map<KycDocumentType, String> keys = new LinkedHashMap<>();
            keys.put(KycDocumentType.ID_FRONT, p.getIdFrontUrl());
            keys.put(KycDocumentType.ID_BACK, p.getIdBackUrl());
            keys.put(KycDocumentType.SELFIE, p.getSelfieUrl());
            return new Snapshot(p.getId(), p.getIdentityCode(), p.getRepresentative(),
                    Boolean.TRUE.equals(p.getKycBiometricConsent()), keys, meta(p.getKycDocumentsMetaJson()));
        });
        if (s == null) {
            return;
        }
        try {
            Map<String, String> urls = new LinkedHashMap<>();
            for (Map.Entry<KycDocumentType, String> e : s.keys().entrySet()) {
                Map<String, Object> m = s.meta().get(e.getKey().name());
                String type = m == null ? null : (String) m.get("content_type");
                urls.put(e.getKey().name().toLowerCase(), dossiers.openTemporaryView(s.id(), e.getKey(), e.getValue(), type));
            }
            String callback = aiWorker.callbackBaseUrl().replaceAll("/+$", "") + "/api/v1/internal/kyc/" + s.id() + "/result";
            client.requestVerification(new KycVerificationClient.Request(s.id(), urls, s.identity(), s.fullName(),
                    s.consent(), callback));
        } catch (RuntimeException e) {
            log.warn("Không gửi được hồ sơ KYC {} cho AI Worker → chuyển duyệt tay: {}", s.id(), e.getMessage());
            decisions.failToManual(s.id(), "AI_WORKER_UNAVAILABLE");
        }
    }

    private Map<String, Map<String, Object>> meta(String raw) {
        try {
            return raw == null ? Map.of() : json.readValue(raw, new TypeReference<Map<String, Map<String, Object>>>() { });
        } catch (Exception e) {
            return Map.of();
        }
    }
}
