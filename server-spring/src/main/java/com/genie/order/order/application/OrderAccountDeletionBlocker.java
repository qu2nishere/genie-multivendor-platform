package com.genie.order.order.application;

import com.genie.auth.deletion.application.AccountDeletionBlocker;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Module order cài cổng {@link AccountDeletionBlocker} (UC-AUTH-10; plan C2 D14, O07.06): còn đơn chưa kết thúc (người
 * mua hoặc chủ shop) → {@code 409 GNE-USR-4091 {active_order_codes}}.
 */
@Component
public class OrderAccountDeletionBlocker implements AccountDeletionBlocker {

    private final OpenOrders open;

    public OrderAccountDeletionBlocker(OpenOrders open) {
        this.open = open;
    }

    @Override
    public Map<String, Object> blockingDetails(long userId) {
        List<String> codes = open.codesOf(userId);
        return codes.isEmpty() ? Map.of() : Map.of("active_order_codes", codes);
    }
}
