package com.genie.admin.users.application;

import java.util.Collection;
import java.util.Map;

/**
 * Cổng đảo ngược (plan B1 D10): số đơn đã đặt của từng người dùng cho cột "Số đơn" ở {@code /admin/users}. Module
 * {@code order} cài sau; chưa có bean thì màn hình hiện 0.
 */
public interface UserOrderStats {

    /** {@code userId → số đơn}; người không có trong map coi như 0. */
    Map<Long, Integer> orderCounts(Collection<Long> userIds);
}
