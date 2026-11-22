package com.genie.common.settings;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đọc / ghi cấu hình chung toàn sàn ({@code platform_settings}, plan B2 D2). Giá trị số lưu dạng số JSON
 * ({@code 0.0500}); khóa chưa có thì dùng giá trị mặc định do nơi gọi truyền vào.
 */
@Component
public class PlatformSettings {

    /** Hoa hồng mặc định toàn sàn — phân số (0.0500 = 5 %). */
    public static final String DEFAULT_COMMISSION_RATE = "commission.default_rate";

    private final PlatformSettingRepository settings;
    private final Clock clock;

    public PlatformSettings(PlatformSettingRepository settings, Clock clock) {
        this.settings = settings;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public BigDecimal decimal(String key, BigDecimal fallback) {
        return settings.findById(key).map(s -> new BigDecimal(s.getValueJson().trim())).orElse(fallback);
    }

    /** Ghi số (khóa dòng — hai Admin đổi cùng lúc thì lần sau chờ lần trước); trả giá trị cũ ({@code null} nếu chưa có). */
    @Transactional
    public BigDecimal putDecimal(String key, BigDecimal value, Long adminId) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        String json = value.toPlainString();
        return settings.findByKeyForUpdate(key).map(s -> {
            BigDecimal old = new BigDecimal(s.getValueJson().trim());
            s.change(json, adminId, now);
            return old;
        }).orElseGet(() -> {
            settings.save(PlatformSetting.builder().settingKey(key).valueJson(json).updatedByAdminId(adminId)
                    .updatedAt(now).build());
            return null;
        });
    }
}
