package com.genie.catalog.product.domain;

/**
 * Enum ProductStatus — ENTITY.md Phụ lục A. ← Product.status (FLAGGED: kiểm duyệt tự động đánh dấu nghi vấn — UC-CAT-12)
 */
public enum ProductStatus {                                                                    // ← Product.status (FLAGGED: kiểm duyệt tự động đánh dấu nghi vấn — UC-CAT-12)
    DRAFT, PENDING_APPROVAL, PUBLISHED, HIDDEN, REJECTED, FLAGGED, DELETED
}
