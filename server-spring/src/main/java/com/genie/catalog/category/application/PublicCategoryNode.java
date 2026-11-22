package com.genie.catalog.category.application;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** Nút cây danh mục công khai — API 5.1 {@code [{ id, name, slug, icon, children[] }]}. */
public record PublicCategoryNode(long id, String name, String slug,
                                 @JsonInclude(JsonInclude.Include.ALWAYS) String icon,
                                 List<PublicCategoryNode> children) {
}
