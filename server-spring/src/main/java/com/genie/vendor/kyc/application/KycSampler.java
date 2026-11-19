package com.genie.vendor.kyc.application;

import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Nguồn ngẫu nhiên cho lấy mẫu hậu kiểm 5% (UC-VND-03; plan Mở shop & KYC K05.02) — tách thành bean để test thay bằng bản
 * cố định.
 */
@Component
public class KycSampler {

    /** {@code true} với xác suất {@code rate}. */
    public boolean sample(double rate) {
        return rate > 0 && ThreadLocalRandom.current().nextDouble() < rate;
    }
}
