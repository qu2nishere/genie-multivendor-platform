package com.genie.promotion.coin.application;

/**
 * Cổng đảo ngược báo lệch số dư Điểm (băm không khớp — có thể bị sửa thẳng CSDL; plan Điểm Genie D8). Module
 * {@code notification} cài đặt sau (tạo {@code AdminNotification} loại {@code SYSTEM_ALERT}); chưa có bean thì
 * {@link CoinLedgerService} chỉ ghi log lỗi.
 */
public interface CoinAlertPort {

    void balanceTampered(long userId, int storedBalance, long ledgerSum);
}
