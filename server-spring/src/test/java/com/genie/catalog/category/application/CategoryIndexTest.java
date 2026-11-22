package com.genie.catalog.category.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.genie.common.error.InvalidRequestException.Violation;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Cây trong bộ nhớ (plan B2 D4, D5) + đổi phần trăm ⇄ phân số (N4). */
class CategoryIndexTest {

    private static CategoryRow row(long id, Long parent, Integer order, String name, boolean active) {
        return new CategoryRow(id, name, "s" + id, parent, order, null, null, null, active, false);
    }

    private final CategoryIndex idx = new CategoryIndex(List.of(
            row(1, null, 2, "Thời trang", true), row(2, null, 1, "Điện tử", true), row(3, null, null, "Sách", true),
            row(11, 2, 1, "Điện thoại", true), row(111, 11, 1, "Ốp lưng", true),
            row(12, 2, 2, "Laptop", false), row(121, 12, 1, "Chuột", true)));

    @Test
    void ordersSiblingsAndWalksTheTree() {
        assertThat(idx.roots()).extracting(CategoryRow::id).containsExactly(2L, 1L, 3L);   // thứ tự rỗng xuống cuối
        assertThat(idx.level(111)).isEqualTo(3);
        assertThat(idx.height(2)).isEqualTo(2);
        assertThat(idx.ancestors(111)).extracting(CategoryRow::id).containsExactly(2L, 11L);
        assertThat(idx.descendants(2)).extracting(CategoryRow::id).containsExactlyInAnyOrder(11L, 111L, 12L, 121L);
    }

    @Test
    void hiddenParentHidesTheBranch() {
        assertThat(idx.visible(111)).isTrue();
        assertThat(idx.visible(12)).isFalse();
        assertThat(idx.visible(121)).isFalse();   // tự nó bật nhưng cha ẩn
    }

    @Test
    void moveRules() {
        List<Violation> errors = new ArrayList<>();
        CategoryAdminService.checkMove(idx, 2, 111L, errors);
        assertThat(errors).extracting(Violation::code).containsExactly("CYCLE");
        errors.clear();
        CategoryAdminService.checkMove(idx, 11, 12L, errors);   // 11 (cao 1) dưới 12 (cấp 2) → 4 cấp
        assertThat(errors).extracting(Violation::code).containsExactly("TOO_DEEP");
        errors.clear();
        CategoryAdminService.checkMove(idx, 11, 1L, errors);
        CategoryAdminService.checkMove(idx, 111, null, errors);
        assertThat(errors).isEmpty();
    }

    @Test
    void percentAndFraction() {
        assertThat(CommissionRates.toFraction(new BigDecimal("5"))).isEqualByComparingTo("0.05");
        assertThat(CommissionRates.toPercent(new BigDecimal("0.0350"))).isEqualByComparingTo("3.5");
        assertThat(CommissionRates.inRange(new BigDecimal("0.0350"))).isTrue();
        assertThat(CommissionRates.inRange(new BigDecimal("0.0800"))).isTrue();
        assertThat(CommissionRates.inRange(new BigDecimal("0.0349"))).isFalse();
        assertThat(CommissionRates.inRange(new BigDecimal("0.0801"))).isFalse();
        List<Violation> errors = new ArrayList<>();
        assertThat(CategoryAdminService.slugOf(null, "Điện thoại & Phụ kiện", errors)).isEqualTo("dien-thoai-phu-kien");
        CategoryAdminService.slugOf("Có Dấu", "x", errors);
        assertThat(errors).hasSize(1);
    }
}
