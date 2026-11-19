package com.genie.vendor;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint {@code /seller/**} chỉ có trong test — đại diện các màn bị cổng KYC khóa (plan Mở shop & KYC K02.09). */
@RestController
class KycGateProbeController {

    @GetMapping("/api/v1/seller/__kyc-gate-probe")
    Map<String, Object> probe() {
        return Map.of("ok", true);
    }
}
