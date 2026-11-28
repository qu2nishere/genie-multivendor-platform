// Sản phẩm vừa vào hàng chờ duyệt — phát trong transaction lưu sản phẩm (UC-CAT-09, 10 → UC-CAT-12; plan C1 D12)
package com.genie.catalog.product.event;

import java.time.OffsetDateTime;

/**
 * Listener sàng lọc AI (UC-CAT-12) nghe {@code AFTER_COMMIT}; {@code submittedAt} để bỏ kết quả về muộn khi sản phẩm đã
 * được gửi lại / sửa tiếp. Chỉ mang ID + giá trị (CLAUDE.md mục 6).
 */
public record ProductSubmittedEvent(long productId, long vendorUserId, OffsetDateTime submittedAt, boolean requeued) {
}
