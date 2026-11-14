package com.genie.promotion.voucher.domain;

import java.io.Serializable;
import java.util.Objects;
import lombok.*;

/**
 * Khóa gộp của VoucherCategory (@IdClass) — trường trùng tên & kiểu với các trường @Id của entity (implement-entity.md D4).
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class VoucherCategoryId implements Serializable {

    private Long voucherId;
    private Long categoryId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VoucherCategoryId that)) return false;
        return Objects.equals(voucherId, that.voucherId) && Objects.equals(categoryId, that.categoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(voucherId, categoryId);
    }
}
