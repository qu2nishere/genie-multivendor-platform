package com.genie.vendor.settings.application;

/**
 * Cổng đảo ngược: shop còn lệnh rút tiền đang chờ không (UC-VND-07 xóa tài khoản → {@code 409 GNE-WAL-4091}; plan Mở shop &
 * KYC K08.06). Module {@code payment} cài khi hiện thực; chưa có cài đặt → không chặn.
 */
public interface PayoutAccountGuard {

    boolean hasPendingWithdrawal(long vendorUserId);
}
