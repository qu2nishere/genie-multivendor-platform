// Cài cổng CoinAlertPort: báo đội vận hành số dư Điểm bị lệch băm (plan Điểm Genie D8; plan 2026-11-26 thông báo N03.08)
package com.genie.promotion.coin.application;

import com.genie.notification.admin.application.AdminInboxFacade;
import com.genie.notification.admin.application.StaffAlertKind;
import org.springframework.stereotype.Component;

/**
 * {@code CoinLedgerService} gọi cổng rồi ném lỗi (transaction nghiệp vụ rollback) → phải ghi <b>ngay</b> trong
 * transaction riêng ({@link AdminInboxFacade#raiseNow}), nếu chờ commit thì cảnh báo mất. Cạnh {@code promotion → notification}.
 */
@Component
public class CoinAlertNotifier implements CoinAlertPort {

    private final AdminInboxFacade adminInbox;

    public CoinAlertNotifier(AdminInboxFacade adminInbox) {
        this.adminInbox = adminInbox;
    }

    @Override
    public void balanceTampered(long userId, int storedBalance, long ledgerSum) {
        adminInbox.raiseNow(StaffAlertKind.SYSTEM_ALERT, "Lệch số dư Điểm của người dùng #" + userId,
                "Số dư Điểm đang lưu " + storedBalance + " không khớp sổ biến động (" + ledgerSum
                        + ") — có thể bị sửa trực tiếp CSDL. Ghi Điểm của tài khoản này đã bị chặn.",
                "/admin/users?q=" + userId);
    }
}
