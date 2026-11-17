package com.genie.auth.profile.application;

import java.util.Map;

/**
 * Cổng bộ đếm trên header / sidebar cho {@code GET /me} (API_SPEC 3.1, plan AUTH D13, A13.01). Mỗi module cài phần của
 * mình: {@code order} → {@code cart_items}; {@code notification} → {@code unread_notifications};
 * {@code social} → {@code unread_chats}, {@code following_shops}; {@code promotion} → {@code available_vouchers}.
 * Module chưa làm → không có cài đặt → bộ đếm = 0.
 */
public interface AccountCounterContributor {

    /** Khóa nằm trong {@link MeView.Counters#KEYS}; khóa lạ bị bỏ qua, nhiều cài đặt cùng khóa thì cộng dồn. */
    Map<String, Long> countersOf(long userId);
}
