package com.genie.admin.users.application;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Cổng đảo ngược (plan B1 D10): tên shop của chủ shop và tìm chủ shop theo tên shop. Module {@code vendor} cài
 * ({@code AdminShopNameLookup}); chưa có bean thì cột tên shop trống và không tìm được theo tên shop.
 */
public interface ShopNameLookup {

    /** {@code userId chủ shop → tên shop}. */
    Map<Long, String> shopNames(Collection<Long> vendorUserIds);

    /** {@code userId} các chủ shop có tên shop chứa {@code q} (không dấu, không phân biệt hoa thường). */
    Set<Long> vendorUserIdsByShopName(String q);
}
