// Cổng tra thẻ đơn hàng cho chat (plan 2026-11-26 chat M02.02, N2, D9; UC-SOC-02; API_SPEC 8.6)
package com.genie.social.chat.application;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Cổng đảo ngược — module {@code order} cài ({@code ChatOrderLookupImpl}). */
public interface ChatOrderLookup {

    /** Đơn theo id (một truy vấn cho cả trang tin); id lạ → không có trong kết quả. */
    Map<Long, OrderCard> cards(Collection<Long> orderIds);

    /** Đơn gần nhất giữa đúng một khách và một shop, mới nhất trước (ô "Gửi đơn"). */
    List<OrderCard> recentBetween(long customerId, long vendorUserId, int limit);

    /** Đơn có đúng cặp khách – shop của hội thoại. */
    default boolean belongs(long orderId, long customerId, long vendorUserId) {
        OrderCard c = cards(List.of(orderId)).get(orderId);
        return c != null && c.customerId() == customerId && c.vendorUserId() == vendorUserId;
    }

    /** @param total tổng tiền đơn (VND nguyên); {@code thumbnailUrl} ảnh dòng đầu (có thể {@code null}). */
    record OrderCard(long id, String code, String status, long customerId, long vendorUserId, long total,
                     OffsetDateTime createdAt, int itemCount, String thumbnailUrl) {
    }
}
