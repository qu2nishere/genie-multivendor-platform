package com.genie.catalog.attribute.application;

/** Thuộc tính ngành con được kế thừa từ ngành tổ tiên (chỉ đọc — plan B2 D8): hiện "Kế thừa từ …" trên màn hình. */
public record InheritedAttribute(String code, String name, String dataType, boolean isRequired, boolean isActive,
                                 long categoryId, String categoryName) {
}
