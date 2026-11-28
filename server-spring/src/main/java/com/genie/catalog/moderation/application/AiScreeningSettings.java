// Bật / tắt kiểm duyệt tự động bằng Gemini — UC-CAT-12 (plan C1 P06; c1_auto_product.md A1, A10)
package com.genie.catalog.moderation.application;

import com.genie.common.audit.AuditAction;
import com.genie.common.audit.AuditLogger;
import com.genie.common.security.ClientInfo;
import com.genie.common.settings.PlatformSetting;
import com.genie.common.settings.PlatformSettingRepository;
import com.genie.common.settings.PlatformSettings;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Trạng thái và cấu hình kiểm duyệt sản phẩm tự động bằng Gemini. Mặc định <b>TẮT</b> khi chưa có dòng trong
 * {@code platform_settings} (A1); chỉ ADMIN được đổi qua API (4.12).
 */
@Service
public class AiScreeningSettings {

    /** Trạng thái hiển thị cho màn Cấu hình (API 11.12 — GET /admin/product-moderation/ai-settings). */
    public record View(boolean enabled, String mode, String model, int rejectThreshold, boolean configured,
                       OffsetDateTime updatedAt, String updatedBy) {
    }

    private final PlatformSettings settings;
    private final PlatformSettingRepository repo;
    private final AuditLogger audit;
    private final JdbcTemplate jdbc;
    private final String mode;
    private final String model;
    private final int threshold;
    private final String apiKey;

    public AiScreeningSettings(PlatformSettings settings, PlatformSettingRepository repo, AuditLogger audit,
                               JdbcTemplate jdbc,
                               @Value("${GEMINI_MODE:stub}") String mode,
                               @Value("${GEMINI_MODEL:gemini-3.8-flash}") String model,
                               @Value("${GEMINI_REJECT_THRESHOLD:85}") int threshold,
                               @Value("${genie.gemini.api-key:}") String apiKey) {
        this.settings = settings;
        this.repo = repo;
        this.audit = audit;
        this.jdbc = jdbc;
        String m = mode == null ? "stub" : mode.trim().toLowerCase(Locale.ROOT);
        this.mode = "http".equals(m) ? "http" : "stub";
        this.model = model == null || model.isBlank() ? "gemini-3.8-flash" : model.trim();
        this.threshold = Math.max(1, Math.min(threshold, 100));
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    public boolean enabled() {
        return settings.bool(PlatformSettings.AI_SCREENING_ENABLED, false);
    }

    public int rejectThreshold() {
        return threshold;
    }

    /** Tên mô hình ghi vào {@code product_moderation_results.model_name} ("stub" khi chạy giả lập). */
    public String model() {
        return "stub".equals(mode) ? "stub" : model;
    }

    @Transactional(readOnly = true)
    public View view() {
        PlatformSetting row = repo.findById(PlatformSettings.AI_SCREENING_ENABLED).orElse(null);
        boolean on = row != null && Boolean.parseBoolean(row.getValueJson().trim());
        boolean configured = "stub".equals(mode) || !apiKey.isBlank();
        OffsetDateTime updatedAt = row == null ? null : row.getUpdatedAt();
        String updatedBy = row == null || row.getUpdatedByAdminId() == null ? null : adminName(row.getUpdatedByAdminId());
        return new View(on, mode, model(), threshold, configured, updatedAt, updatedBy);
    }

    @Transactional
    public View setEnabled(long adminId, boolean on, ClientInfo client) {
        Boolean old = settings.putBool(PlatformSettings.AI_SCREENING_ENABLED, on, adminId);
        if (!Objects.equals(old, on)) {
            audit.record(AuditAction.PRODUCT_MODERATED, adminId, "ai_screening",
                    Map.of("action", on ? "AI_SCREENING_ON" : "AI_SCREENING_OFF"), client, true);
        }
        return view();
    }

    private String adminName(long userId) {
        List<String> names = jdbc.query("""
                SELECT COALESCE(ap.full_name, cp.full_name, u.email)
                  FROM users u
                  LEFT JOIN admin_profiles ap ON ap.user_id = u.id
                  LEFT JOIN customer_profiles cp ON cp.user_id = u.id
                 WHERE u.id = ?""", (rs, i) -> rs.getString(1), userId);
        return names.isEmpty() ? null : names.get(0);
    }
}
