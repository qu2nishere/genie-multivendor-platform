// Cổng tra shop cho chat (plan 2026-11-26 chat M02.02, N2, D5, D11, D12, D16)
package com.genie.social.chat.application;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Cổng đảo ngược: dữ liệu shop thuộc module {@code vendor}; {@code social} chỉ biết cổng này — {@code vendor} cài đặt
 * ({@code ChatShopDirectoryImpl}) bằng file mới, nên không có vòng {@code order → vendor → social}.
 * <p>{@code shopId} = {@code vendor_profiles.id} (trên API); {@code vendorUserId} = {@code users.id} chủ shop (lưu ở
 * {@code conversations.vendor_id}) — đừng trộn hai id.
 */
public interface ChatShopDirectory {

    /** Shop theo {@code vendor_profiles.id} — mọi trạng thái (xem {@link ChatShop#active()}). */
    Optional<ChatShop> find(long shopId);

    /** Shop theo chủ shop, nạp theo lô cho danh sách hội thoại (id không có shop → không có trong kết quả). */
    Map<Long, ChatShop> byVendorUsers(Collection<Long> vendorUserIds);

    default Optional<ChatShop> byVendorUser(long vendorUserId) {
        return Optional.ofNullable(byVendorUsers(List.of(vendorUserId)).get(vendorUserId));
    }

    /**
     * Thông tin chat của một shop.
     *
     * @param active           {@code VERIFIED} và chủ shop {@code ACTIVE} — chỉ shop này mở / nhận tin mới được (D5)
     * @param greeting         lời chào khi mở hội thoại (UC-VND-06) — chỉ dùng khi {@code autoReplyEnabled}
     * @param awayMessage      tin vắng mặt — chỉ dùng khi {@code autoReplyEnabled}
     * @param chatResponseRate % hội thoại được trả lời trong 24 giờ (job hằng ngày — D12); {@code null} khi chưa đủ dữ liệu
     */
    record ChatShop(long shopId, long vendorUserId, String name, String logoUrl, boolean isMall, boolean active,
                    boolean autoReplyEnabled, String greeting, String awayMessage,
                    OffsetDateTime vacationStartsAt, OffsetDateTime vacationEndsAt, String vacationMessage,
                    BigDecimal chatResponseRate) {

        /** Đang tạm nghỉ: {@code vacation_starts_at <= now < vacation_ends_at} (UC-VND-19 — chỉ đọc cột). */
        public boolean onVacation(OffsetDateTime now) {
            return vacationStartsAt != null && !now.isBefore(vacationStartsAt)
                    && (vacationEndsAt == null || now.isBefore(vacationEndsAt));
        }
    }
}
