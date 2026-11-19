package com.genie.vendor.shop.application;

import java.util.Map;

/**
 * Cổng đảo ngược: số đếm trên khung Seller Center ({@code GET /seller/me → badges} — API_SPEC 10.1; plan Mở shop & KYC
 * K03.07). Module {@code order} trả {@code orders_to_confirm}, {@code social} (chat) trả {@code unread_chats} khi được hiện
 * thực; chưa có cài đặt → 0.
 */
public interface SellerBadgeContributor {

    /** Khóa snake_case → số đếm cho shop của chủ {@code vendorUserId}. */
    Map<String, Long> badgesOf(long vendorUserId);
}
