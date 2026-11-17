package com.genie.auth.profile.application;

import com.genie.common.domain.UserRole;
import java.util.List;
import java.util.Map;

/**
 * {@code GET /me} — ngữ cảnh phiên cho header, menu người dùng, sidebar Tài khoản (API_SPEC 3.1).
 *
 * @param shop     {@code null} khi chưa mở shop (hoặc module vendor chưa cài {@link AccountShopProvider})
 * @param counters luôn đủ 5 khóa, mặc định 0
 */
public record MeView(long id, UserRole role, String fullName, String avatarUrl, String email,
                     LoginMethods loginMethods, AccountShopProvider.ShopSummary shop, Counters counters) {

    /** Cách đăng nhập đang bật (API_SPEC: {@code password}, {@code google}; thêm {@code zalo} — plan D23). */
    public record LoginMethods(boolean password, boolean google, boolean zalo) {
    }

    public record Counters(long cartItems, long unreadNotifications, long unreadChats, long availableVouchers,
                           long followingShops) {

        public static final String CART_ITEMS = "cart_items";
        public static final String UNREAD_NOTIFICATIONS = "unread_notifications";
        public static final String UNREAD_CHATS = "unread_chats";
        public static final String AVAILABLE_VOUCHERS = "available_vouchers";
        public static final String FOLLOWING_SHOPS = "following_shops";
        public static final List<String> KEYS = List.of(CART_ITEMS, UNREAD_NOTIFICATIONS, UNREAD_CHATS,
                AVAILABLE_VOUCHERS, FOLLOWING_SHOPS);

        static Counters of(Map<String, Long> totals) {
            return new Counters(totals.getOrDefault(CART_ITEMS, 0L), totals.getOrDefault(UNREAD_NOTIFICATIONS, 0L),
                    totals.getOrDefault(UNREAD_CHATS, 0L), totals.getOrDefault(AVAILABLE_VOUCHERS, 0L),
                    totals.getOrDefault(FOLLOWING_SHOPS, 0L));
        }
    }
}
