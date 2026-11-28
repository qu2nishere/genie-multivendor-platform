// Staff vừa ra quyết định kiểm duyệt sản phẩm — UC-CAT-13 bước 5–6 (plan C1 D14). Listener gửi email sau commit.
package com.genie.catalog.product.event;

/**
 * @param decision   {@code APPROVED} | {@code REJECTED}
 * @param reasonCode mã lý do từ chối (API 16 — {@code null} khi duyệt)
 * @param note       ghi chú thêm của Staff (NULLABLE)
 */
public record ProductModeratedEvent(long productId, long vendorUserId, String productName, String decision,
                                    String reasonCode, String note) {
}
