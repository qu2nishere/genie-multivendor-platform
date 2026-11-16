package com.genie.support;

import com.genie.auth.deletion.application.AccountDeletionBlocker;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thay cho module {@code order} (chưa làm) trong test xóa tài khoản: {@link #blockWithOrders} giả lập user còn đơn
 * đang xử lý → {@code 409 GNE-USR-4091 {active_order_codes}}.
 */
public class FakeAccountDeletionBlocker implements AccountDeletionBlocker {

    private final Map<Long, List<String>> activeOrders = new ConcurrentHashMap<>();

    public void blockWithOrders(long userId, String... orderCodes) {
        activeOrders.put(userId, List.of(orderCodes));
    }

    public void clear() {
        activeOrders.clear();
    }

    @Override
    public Map<String, Object> blockingDetails(long userId) {
        List<String> codes = activeOrders.get(userId);
        return codes == null || codes.isEmpty() ? Map.of() : Map.of("active_order_codes", codes);
    }
}
