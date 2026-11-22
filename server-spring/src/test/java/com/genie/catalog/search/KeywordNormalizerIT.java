package com.genie.catalog.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.genie.catalog.search.domain.KeywordNormalizer;
import com.genie.support.IntegrationTestBase;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Bỏ dấu phía Java phải khớp {@code immutable_unaccent(lower(…))} của PostgreSQL — nếu lệch, từ khóa thống kê
 * và gợi ý tiền tố sẽ không khớp index (plan 2026-11-17 C01.04, rủi ro "unaccent Java ≠ PostgreSQL").
 */
class KeywordNormalizerIT extends IntegrationTestBase {

    static final List<String> SAMPLES = List.of(
            "Áo thun nam", "Đồng hồ ĐẸP", "tai nghe chống ồn", "Sữa rửa mặt hữu cơ", "Ốp lưng iPhone",
            "bàn phím cơ", "nồi chiên không dầu", "giày thể thao nữ", "Ưu đãi Ứng dụng", "Quần jean ống rộng",
            "kem chống nắng", "máy lọc không khí", "điện thoại", "Ấm siêu tốc", "Ỹ ỳ ỷ ỵ ý", "Chuột không dây",
            "sạc dự phòng", "Bình giữ nhiệt", "đèn ngủ", "Tủ lạnh mini", "váy hoa nhí", "son môi lì",
            "nước hoa", "mũ bảo hiểm", "ổ cứng di động", "loa bluetooth", "vợt cầu lông", "bút bi", "gối tựa lưng",
            "thớt gỗ");

    @Test
    void javaUnaccentMatchesPostgres() {
        for (String s : SAMPLES) {
            String pg = jdbc.queryForObject("SELECT immutable_unaccent(lower(?))", String.class, s);
            assertThat(KeywordNormalizer.normalizeUnaccented(s)).as(s).isEqualTo(pg);
        }
    }

    @Test
    void v18IndexesExist() {
        List<String> indexes = jdbc.queryForList("SELECT indexname FROM pg_indexes WHERE tablename IN ('products', 'product_variants')",
                String.class);
        assertThat(indexes).contains("idx_products_search_vector", "idx_products_name_unaccent_trgm",
                "idx_product_variants_product_price", "idx_products_status_sold");
    }

    @Test
    void searchVectorIsWeightedByName() {
        String vector = jdbc.queryForObject("""
                SELECT setweight(to_tsvector('simple', immutable_unaccent(lower('Tai nghe'))), 'A')::text""", String.class);
        assertThat(vector).contains("'tai':1A");
    }
}
