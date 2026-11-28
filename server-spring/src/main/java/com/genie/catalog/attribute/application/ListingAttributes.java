// Bộ thuộc tính áp dụng cho một ngành khi đăng bán (kế thừa ngành cha) — UC-CAT-09 bước 13, UC-CAT-15 (plan C1 D8; plan B2 C06.02)
package com.genie.catalog.attribute.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.catalog.attribute.domain.CategoryAttribute;
import com.genie.catalog.attribute.domain.CategoryAttributeRepository;
import com.genie.catalog.category.application.CategoryCatalog;
import com.genie.catalog.category.application.CategoryIndex;
import com.genie.catalog.category.application.CategoryRow;
import com.genie.common.cache.JsonCache;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Thuộc tính <b>đang dùng</b> của ngành + mọi ngành tổ tiên, xếp từ gốc xuống (trong mỗi ngành theo {@code display_order}).
 * Cache Redis {@code catalog:attrs:{id}} 1 giờ — B2 đã xóa cả nhóm {@code catalog:attrs:*} sau mỗi lần sửa danh mục /
 * thuộc tính ({@link CategoryCatalog#evictAfterCommit()}); Redis lỗi thì đọc thẳng CSDL ({@link JsonCache}).
 */
@Component
public class ListingAttributes {

    static final Duration TTL = Duration.ofHours(1);

    private final CategoryAttributeRepository attributes;
    private final CategoryCatalog catalog;
    private final ObjectMapper objectMapper;
    private final JsonCache cache;

    public ListingAttributes(CategoryAttributeRepository attributes, CategoryCatalog catalog,
                             ObjectMapper objectMapper, JsonCache cache) {
        this.attributes = attributes;
        this.catalog = catalog;
        this.objectMapper = objectMapper;
        this.cache = cache;
    }

    public List<AttributeView> effectiveFor(long categoryId) {
        List<AttributeView> list = cache.getOrLoad(CategoryCatalog.ATTRIBUTE_PREFIX + categoryId,
                cache.listOf(AttributeView.class), TTL, () -> load(categoryId));
        return list == null ? List.of() : list;
    }

    List<AttributeView> load(long categoryId) {
        CategoryIndex idx = catalog.index();
        if (idx.find(categoryId).isEmpty()) {
            return List.of();
        }
        List<Long> chain = new ArrayList<>();
        idx.ancestors(categoryId).stream().map(CategoryRow::id).forEach(chain::add);
        chain.add(categoryId);
        Map<Long, Integer> depth = new HashMap<>();
        for (int i = 0; i < chain.size(); i++) {
            depth.put(chain.get(i), i);
        }
        return attributes.findByCategoryIdInOrderByDisplayOrderAscIdAsc(chain).stream()
                .filter(a -> Boolean.TRUE.equals(a.getIsActive()))
                .sorted(Comparator.comparing((CategoryAttribute a) -> depth.getOrDefault(a.getCategoryId(), 0))
                        .thenComparing(a -> a.getDisplayOrder() == null ? 0 : a.getDisplayOrder())
                        .thenComparing(CategoryAttribute::getId))
                .map(this::view)
                .toList();
    }

    // Cùng cách đổi như CategoryAttributeService.view (không gọi qua proxy giao dịch của service đó)
    private AttributeView view(CategoryAttribute a) {
        return new AttributeView(a.getCode(), a.getName(), a.getDataType().name(), values(a.getAllowedValuesJson()),
                a.getUnit(), Boolean.TRUE.equals(a.getIsRequired()), Boolean.TRUE.equals(a.getIsFilterable()),
                Boolean.TRUE.equals(a.getIsVariantAxis()), true, a.getDisplayOrder() == null ? 0 : a.getDisplayOrder(),
                null);
    }

    private List<String> values(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() { });
        } catch (Exception e) {
            return List.of();
        }
    }
}
