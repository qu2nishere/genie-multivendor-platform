// Bộ thuộc tính chuẩn theo ngành — API 11.11 (UC-CAT-15; plan B2 mục 3.4, C06, D8–D12)
package com.genie.catalog.attribute.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.catalog.attribute.domain.AttributeDataType;
import com.genie.catalog.attribute.domain.CategoryAttribute;
import com.genie.catalog.attribute.domain.CategoryAttributeRepository;
import com.genie.catalog.category.application.CategoryCatalog;
import com.genie.catalog.category.application.CategoryIndex;
import com.genie.catalog.category.application.CategoryRow;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.error.InvalidRequestException;
import com.genie.common.error.InvalidRequestException.Violation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Thuộc tính khai ở ngành cha được ngành con kế thừa, nên {@code code} không được trùng trong ngành, các ngành tổ tiên
 * và con cháu → {@code 409 GNE-CAT-4095} ({@code details.category_id}, {@code category_name} nơi đã có — D8). Không đổi
 * {@code data_type} sau khi tạo (D9). Ràng buộc trường theo D10. Thêm / đổi thành "bắt buộc" khi nhánh đã có sản phẩm →
 * phản hồi kèm {@code grace_period_days = 14} (D11 — không lưu hạn). Ngừng dùng thì bỏ luôn "bắt buộc"; dùng lại không
 * tự bật lại (D12). Mọi thay đổi xóa cache thuộc tính của cả cây sau commit.
 */
@Service
public class CategoryAttributeService {

    static final Pattern CODE = Pattern.compile("^[a-z][a-z0-9_]*$");
    static final int GRACE_PERIOD_DAYS = 14;
    private static final TypeReference<List<String>> STRINGS = new TypeReference<>() { };

    private final CategoryAttributeRepository attributes;
    private final CategoryCatalog catalog;
    private final ObjectMapper objectMapper;

    public CategoryAttributeService(CategoryAttributeRepository attributes, CategoryCatalog catalog,
                                    ObjectMapper objectMapper) {
        this.attributes = attributes;
        this.catalog = catalog;
        this.objectMapper = objectMapper;
    }

    /** Thuộc tính của chính ngành + danh sách kế thừa từ tổ tiên. */
    public record AttributeList(List<AttributeView> own, List<InheritedAttribute> inherited) {
    }

    @Transactional(readOnly = true)
    public AttributeList list(long categoryId) {
        CategoryIndex idx = catalog.freshIndex();
        requireCategory(idx, categoryId);
        List<AttributeView> own = attributes.findByCategoryIdOrderByDisplayOrderAscIdAsc(categoryId).stream()
                .map(this::view).toList();
        Map<Long, String> names = new HashMap<>();
        List<Long> ancestorIds = idx.ancestors(categoryId).stream().peek(r -> names.put(r.id(), r.name()))
                .map(CategoryRow::id).toList();
        List<InheritedAttribute> inherited = ancestorIds.isEmpty() ? List.of()
                : attributes.findByCategoryIdInOrderByDisplayOrderAscIdAsc(ancestorIds).stream()
                .map(a -> new InheritedAttribute(a.getCode(), a.getName(), a.getDataType().name(),
                        Boolean.TRUE.equals(a.getIsRequired()), Boolean.TRUE.equals(a.getIsActive()),
                        a.getCategoryId(), names.get(a.getCategoryId())))
                .toList();
        return new AttributeList(own, inherited);
    }

    @Transactional
    public AttributeView create(long categoryId, AttributeCommand cmd) {
        CategoryIndex idx = catalog.freshIndex();
        requireCategory(idx, categoryId);
        List<Violation> errors = new ArrayList<>();
        String code = cmd.code() == null ? "" : cmd.code().trim();
        if (code.isEmpty() || code.length() > 50 || !CODE.matcher(code).matches()) {
            errors.add(new Violation("code", "INVALID_FORMAT", cmd.code(),
                    "Mã thuộc tính theo mẫu ^[a-z][a-z0-9_]*$, tối đa 50 ký tự."));
        }
        AttributeDataType type = typeOf(cmd.dataType(), errors);
        Fields f = fields(cmd, type, errors);
        if (!errors.isEmpty()) throw new InvalidRequestException(errors);
        requireCodeFree(idx, categoryId, code);

        boolean required = Boolean.TRUE.equals(cmd.isRequired());
        CategoryAttribute a = CategoryAttribute.builder().categoryId(categoryId).code(code).name(f.name())
                .dataType(type).allowedValuesJson(f.valuesJson()).unit(f.unit()).isRequired(required)
                .isFilterable(Boolean.TRUE.equals(cmd.isFilterable())).isVariantAxis(f.variantAxis())
                .displayOrder(attributes.maxDisplayOrder(categoryId) + 1).isActive(true).build();
        try {
            a = attributes.saveAndFlush(a);
        } catch (DataIntegrityViolationException e) {   // uq_category_attribute_code — hai request cùng lúc
            throw new BusinessException(ErrorCode.ATTRIBUTE_CODE_DUPLICATE, Map.<String, Object>of("code", code));
        }
        catalog.evictAfterCommit();
        return view(a).withGracePeriod(required ? grace(categoryId) : null);
    }

    @Transactional
    public AttributeView update(long categoryId, String code, AttributeCommand cmd) {
        CategoryIndex idx = catalog.freshIndex();
        requireCategory(idx, categoryId);
        CategoryAttribute a = attributes.findByCategoryIdAndCode(categoryId, code)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        List<Violation> errors = new ArrayList<>();
        AttributeDataType type = a.getDataType();
        if (cmd.dataType() != null && !cmd.dataType().trim().equalsIgnoreCase(type.name())) {
            errors.add(new Violation("data_type", "IMMUTABLE", cmd.dataType(),
                    "Không đổi được kiểu dữ liệu — hãy ngừng dùng và tạo mã mới."));
        }
        Fields f = fields(cmd, type, errors);
        if (!errors.isEmpty()) throw new InvalidRequestException(errors);

        boolean wasRequired = Boolean.TRUE.equals(a.getIsRequired());
        a.update(f.name(), f.valuesJson(), f.unit(), Boolean.TRUE.equals(cmd.isRequired()),
                Boolean.TRUE.equals(cmd.isFilterable()), f.variantAxis());
        attributes.saveAndFlush(a);
        catalog.evictAfterCommit();
        boolean becameRequired = !wasRequired && Boolean.TRUE.equals(a.getIsRequired());
        return view(a).withGracePeriod(becameRequired ? grace(categoryId) : null);
    }

    @Transactional
    public AttributeView setActive(long categoryId, String code, boolean active) {
        requireCategory(catalog.freshIndex(), categoryId);
        CategoryAttribute a = attributes.findByCategoryIdAndCode(categoryId, code)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (active) a.activate();
        else a.deactivate();
        attributes.saveAndFlush(a);
        catalog.evictAfterCommit();
        return view(a);
    }

    // ===== Kiểm tra =====

    private record Fields(String name, String valuesJson, String unit, boolean variantAxis) {
    }

    /** D10: tên, danh sách giá trị (chỉ ENUM / MULTI_ENUM), đơn vị (chỉ NUMBER), trục biến thể (chỉ ENUM / MULTI_ENUM). */
    private Fields fields(AttributeCommand cmd, AttributeDataType type, List<Violation> errors) {
        String name = cmd.name() == null ? "" : cmd.name().trim();
        if (name.isEmpty() || name.length() > 100) {
            errors.add(new Violation("name", "LENGTH", cmd.name(), "Tên thuộc tính 1–100 ký tự."));
        }
        boolean enumLike = type == AttributeDataType.ENUM || type == AttributeDataType.MULTI_ENUM;
        List<String> values = cmd.values() == null ? List.of()
                : cmd.values().stream().map(v -> v == null ? "" : v.trim()).toList();
        String valuesJson = null;
        if (enumLike) {
            Set<String> seen = new HashSet<>();
            boolean bad = values.isEmpty() || values.size() > 100;
            for (String v : values) {
                if (v.isEmpty() || v.length() > 100 || !seen.add(v.toLowerCase(Locale.ROOT))) bad = true;
            }
            if (bad) {
                errors.add(new Violation("values", "INVALID", values.size(),
                        "Kiểu danh sách cần 1–100 giá trị, không trùng, mỗi giá trị 1–100 ký tự."));
            } else {
                valuesJson = json(values);
            }
        } else if (!values.isEmpty()) {
            errors.add(new Violation("values", "NOT_ALLOWED", values.size(),
                    "Chỉ kiểu ENUM / MULTI_ENUM có danh sách giá trị."));
        }
        String unit = cmd.unit() == null || cmd.unit().isBlank() ? null : cmd.unit().trim();
        if (unit != null && (type != AttributeDataType.NUMBER || unit.length() > 20)) {
            errors.add(new Violation("unit", "NOT_ALLOWED", cmd.unit(), "Đơn vị chỉ dùng cho kiểu NUMBER, tối đa 20 ký tự."));
        }
        boolean variantAxis = Boolean.TRUE.equals(cmd.isVariantAxis());
        if (variantAxis && !enumLike) {
            errors.add(new Violation("is_variant_axis", "NOT_ALLOWED", true,
                    "Chỉ thuộc tính kiểu danh sách mới làm trục biến thể."));
        }
        return new Fields(name, valuesJson, unit, variantAxis);
    }

    private static AttributeDataType typeOf(String raw, List<Violation> errors) {
        try {
            return AttributeDataType.valueOf(raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            errors.add(new Violation("data_type", "INVALID", raw,
                    "Kiểu dữ liệu phải là TEXT, NUMBER, BOOLEAN, ENUM, MULTI_ENUM hoặc DATE."));
            return AttributeDataType.TEXT;
        }
    }

    /** D8: mã không trùng trong ngành, tổ tiên, con cháu. */
    private void requireCodeFree(CategoryIndex idx, long categoryId, String code) {
        List<Long> family = new ArrayList<>();
        family.add(categoryId);
        idx.ancestors(categoryId).forEach(r -> family.add(r.id()));
        idx.descendants(categoryId).forEach(r -> family.add(r.id()));
        attributes.findByCodeAndCategoryIdIn(code, family).stream().findFirst().ifPresent(hit -> {
            throw new BusinessException(ErrorCode.ATTRIBUTE_CODE_DUPLICATE, Map.<String, Object>of(
                    "code", code, "category_id", hit.getCategoryId(),
                    "category_name", idx.find(hit.getCategoryId()).map(CategoryRow::name).orElse("")));
        });
    }

    private static void requireCategory(CategoryIndex idx, long categoryId) {
        if (idx.find(categoryId).isEmpty()) throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
    }

    private Integer grace(long categoryId) {
        return catalog.liveProductsInBranch(categoryId) > 0 ? GRACE_PERIOD_DAYS : null;
    }

    // ===== Ánh xạ =====

    AttributeView view(CategoryAttribute a) {
        return new AttributeView(a.getCode(), a.getName(), a.getDataType().name(), values(a.getAllowedValuesJson()),
                a.getUnit(), Boolean.TRUE.equals(a.getIsRequired()), Boolean.TRUE.equals(a.getIsFilterable()),
                Boolean.TRUE.equals(a.getIsVariantAxis()), Boolean.TRUE.equals(a.getIsActive()),
                a.getDisplayOrder() == null ? 0 : a.getDisplayOrder(), null);
    }

    List<String> values(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, STRINGS);
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private String json(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
