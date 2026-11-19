package com.genie.vendor.kyc.application;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quy đổi tín hiệu rủi ro → điểm (UC-VND-03 "cộng điểm thay vì chặn cứng"; ENTITY 1.6 d; plan Mở shop & KYC N5, K05.02).
 * Hàm thuần theo cấu hình {@code genie.kyc.risk.*}: mỗi mã tín hiệu tính một lần; mã lạ bỏ qua + cảnh báo. Lấy mẫu hậu kiểm
 * dùng nguồn ngẫu nhiên tiêm được.
 */
@Component
public class KycRiskPolicy {

    private static final Logger log = LoggerFactory.getLogger(KycRiskPolicy.class);

    private final KycProperties.Risk risk;
    private final KycSampler sampler;

    @Autowired
    public KycRiskPolicy(KycProperties props, KycSampler sampler) {
        this.risk = props.risk();
        this.sampler = sampler;
    }

    /** Cho test: nguồn ngẫu nhiên cố định. */
    KycRiskPolicy(KycProperties.Risk risk, DoubleSupplier random) {
        this.risk = risk;
        this.sampler = new KycSampler() {
            @Override
            public boolean sample(double rate) {
                return rate > 0 && random.getAsDouble() < rate;
            }
        };
    }

    /** Một tín hiệu đã tính điểm — phần tử của {@code risk_signals_json}. */
    public record ScoredSignal(String code, int points) {
    }

    /**
     * @param score   tổng điểm
     * @param signals tín hiệu đã tính (thứ tự xuất hiện, không trùng)
     * @param sampled hồ sơ bị lấy mẫu hậu kiểm (chỉ có nghĩa khi điểm dưới ngưỡng tự duyệt)
     */
    public record Assessment(int score, List<ScoredSignal> signals, boolean sampled) {
    }

    public Assessment assess(Collection<String> signalCodes) {
        Map<String, Integer> points = risk.points() == null ? Map.of() : risk.points();
        List<ScoredSignal> scored = new ArrayList<>();
        int total = 0;
        for (String code : new LinkedHashSet<>(signalCodes)) {
            Integer p = points.get(code);
            if (p == null) {
                log.warn("Bỏ qua tín hiệu KYC không có trong bảng điểm: {}", code);
                continue;
            }
            scored.add(new ScoredSignal(code, p));
            total += p;
        }
        boolean sampled = total < risk.autoApproveBelow() && sampler.sample(risk.sampleRate());
        return new Assessment(total, scored, sampled);
    }

    public int autoApproveBelow() {
        return risk.autoApproveBelow();
    }

    public int rejectFrom() {
        return risk.rejectFrom();
    }

    /** {@code risk_signals_json}: {@code [{"code":"FACE_SCORE_BORDERLINE","points":25}]}. */
    public static List<Map<String, Object>> toJsonList(List<ScoredSignal> signals) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (ScoredSignal s : signals) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("code", s.code());
            m.put("points", s.points());
            out.add(m);
        }
        return out;
    }
}
