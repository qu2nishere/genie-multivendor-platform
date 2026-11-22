package com.genie.catalog.category.application;

import java.math.BigDecimal;

/**
 * Ảnh chụp một dòng {@code categories} để dựng cây trong bộ nhớ (lưu cache JSON — plan B2 N5). {@code commissionRate}
 * là phân số của riêng ngành ({@code null} = dùng ngành cha / mặc định).
 */
public record CategoryRow(long id, String name, String slug, Long parentId, Integer displayOrder, String icon,
                          String description, BigDecimal commissionRate, boolean active,
                          boolean requiresBusinessLicense) {
}
