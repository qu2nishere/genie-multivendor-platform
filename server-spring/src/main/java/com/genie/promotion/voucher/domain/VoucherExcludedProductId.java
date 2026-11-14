package com.genie.promotion.voucher.domain;

import java.io.Serializable;
import java.util.Objects;
import lombok.*;

/**
 * Khóa gộp của VoucherExcludedProduct (@IdClass) — trường trùng tên & kiểu với các trường @Id của entity (implement-entity.md D4).
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class VoucherExcludedProductId implements Serializable {

    private Long voucherId;
    private Long productId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VoucherExcludedProductId that)) return false;
        return Objects.equals(voucherId, that.voucherId) && Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(voucherId, productId);
    }
}
