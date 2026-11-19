package com.genie.vendor.kyc.application;

import java.time.Duration;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Hằng số nghiệp vụ KYC {@code genie.kyc.*} (application.yml — UC-VND-03; ENTITY 1.6 c–d; plan Mở shop & KYC N5, K05.01).
 *
 * @param risk           ngưỡng điểm + tỷ lệ lấy mẫu + bảng điểm tín hiệu
 * @param aiTimeout      AI Worker không gọi lại sau thời gian này → chuyển duyệt tay (30 giây)
 * @param supplementDays hạn bổ sung hồ sơ (7 ngày)
 */
@ConfigurationProperties(prefix = "genie.kyc")
public record KycProperties(Risk risk, @DefaultValue("30s") Duration aiTimeout, @DefaultValue("7") int supplementDays) {

    /**
     * @param autoApproveBelow điểm dưới ngưỡng này → tự duyệt (30)
     * @param rejectFrom       điểm từ ngưỡng này → từ chối (70)
     * @param sampleRate       tỷ lệ hồ sơ điểm thấp bị lấy mẫu hậu kiểm (0.05)
     * @param points           mã tín hiệu → điểm cộng
     */
    public record Risk(@DefaultValue("30") int autoApproveBelow, @DefaultValue("70") int rejectFrom,
                       @DefaultValue("0.05") double sampleRate, Map<String, Integer> points) {
    }
}
