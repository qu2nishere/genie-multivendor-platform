package com.genie.auth.deletion.application;

import java.util.Map;

/**
 * Cổng "chưa được xóa tài khoản" — UC-AUTH-10, FR-AUTH-07 (chặn khi còn đơn chưa hoàn tất / số dư ví shop).
 * Module sở hữu dữ liệu cài đặt: {@code order} trả {@code active_order_codes}, {@code payment} / {@code vendor} trả
 * số dư ví chưa rút… Không có cài đặt nào (module chưa làm) → luôn cho xóa.
 */
public interface AccountDeletionBlocker {

    /**
     * Lý do chặn dưới dạng {@code details} của lỗi {@code 409 GNE-USR-4091}, ví dụ
     * {@code {"active_order_codes": ["TR-20260608-000044"]}}; rỗng = không chặn.
     */
    Map<String, Object> blockingDetails(long userId);
}
