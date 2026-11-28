// Job quét bù sản phẩm chưa được AI sàng lọc (mỗi 5 phút) — UC-CAT-12 (plan C1 P06; c1_auto_product.md mục 4.9)
package com.genie.catalog.moderation.application;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Quét sản phẩm {@code PENDING_APPROVAL} gửi duyệt trong khoảng {@code [now - 24h, now - 3m]} mà chưa có bản ghi kết quả
 * AI tạo sau {@code submitted_at}. Dùng {@link TransactionTemplate} cho bước {@code claim()} để giữ khóa
 * {@code FOR UPDATE OF p SKIP LOCKED} chỉ trong bước lấy lô, không giữ khóa lúc gọi mạng sang Gemini (mục 4.9).
 */
@Component
public class ProductScreeningSweepJob {

    static final int BATCH = 20;

    record Candidate(long productId, OffsetDateTime submittedAt) {
    }

    private final AiScreeningSettings settings;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final ProductScreeningListener screening;

    public ProductScreeningSweepJob(AiScreeningSettings settings, JdbcTemplate jdbc,
                                    TransactionTemplate tx, ProductScreeningListener screening) {
        this.settings = settings;
        this.jdbc = jdbc;
        this.tx = tx;
        this.screening = screening;
    }

    @Scheduled(cron = "0 */5 * * * *", zone = "Asia/Ho_Chi_Minh")
    public void sweep() {
        if (!settings.enabled()) return;
        List<Candidate> batch = claim();
        // giao từng sản phẩm cho luồng @Async — luồng lịch trả về ngay (sửa 2026-11-28)
        for (Candidate c : batch) {
            screening.screenLater(c.productId(), c.submittedAt());
        }
    }

    private List<Candidate> claim() {
        List<Candidate> found = tx.execute(status -> jdbc.query("""
                SELECT p.id, p.submitted_at FROM products p
                 WHERE p.status = 'PENDING_APPROVAL'
                   AND p.submitted_at BETWEEN now() - interval '24 hours' AND now() - interval '3 minutes'
                   AND NOT EXISTS (SELECT 1 FROM product_moderation_results r
                                    WHERE r.product_id = p.id AND r.created_at >= p.submitted_at)
                 ORDER BY p.submitted_at
                 LIMIT ?
                 FOR UPDATE OF p SKIP LOCKED""",
                (rs, i) -> {
                    Timestamp ts = rs.getTimestamp("submitted_at");
                    OffsetDateTime at = ts == null ? null : ts.toInstant().atOffset(ZoneOffset.UTC);
                    return new Candidate(rs.getLong("id"), at);
                }, BATCH));
        return found == null ? List.of() : found;
    }
}
