package com.genie.admin.users.application;

import java.util.Collection;
import java.util.Map;

/**
 * Cổng đảo ngược (plan B1 D10): số lần bị cảnh báo vi phạm (khiếu nại / đánh giá bị gỡ…) cho cột "Cảnh báo". Module
 * {@code support} / {@code social} cài sau; chưa có bean thì hiện 0.
 */
public interface UserWarningStats {

    /** {@code userId → số cảnh báo}; người không có trong map coi như 0. */
    Map<Long, Integer> warningCounts(Collection<Long> userIds);
}
