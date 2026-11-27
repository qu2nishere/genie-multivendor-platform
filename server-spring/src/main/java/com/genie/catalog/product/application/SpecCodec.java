// Thông số kỹ thuật ⇄ products.specs_json — plan C1 D8 (khớp cách đọc của ProductDetailService, plan 2026-11-17)
package com.genie.catalog.product.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.catalog.attribute.application.AttributeView;
import com.genie.common.domain.NameNormalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * API dùng {@code [{name, value}]} (theo <b>tên</b>), CSDL lưu {@code {"<mã thuộc tính>": "<giá trị>"}} để bộ lọc / trang chi
 * tiết dùng chung khóa máy. Khớp tên không dấu, không phân biệt hoa thường; dòng không khớp thuộc tính nào giữ khóa = tên
 * người bán nhập (trang chi tiết hiện nguyên tên).
 * <p><b>Thứ tự dòng:</b> JSONB tự sắp khóa nên thứ tự người bán kéo thả bị mất → lưu thêm khóa {@link #ORDER_KEY} là mảng
 * các khóa theo đúng thứ tự (chỉ khi ≥ 2 dòng). Bộ lọc tìm kiếm đọc theo từng mã ({@code specs_json ->> 'code'}) nên không
 * bị ảnh hưởng; mọi chỗ đọc toàn bộ đối tượng phải bỏ khóa này ({@link #parse}, {@code ProductDetailService.specs}).
 */
public final class SpecCodec {

    /** Khóa phụ giữ thứ tự dòng thông số trong {@code specs_json} (mã thuộc tính không bắt đầu bằng {@code _}). */
    public static final String ORDER_KEY = "__order";

    /** Dòng thông số đã gom: {@code key} = mã thuộc tính hoặc tên tự do. */
    record Entry(String key, String name, String value, AttributeView attribute) {
    }

    private SpecCodec() {
    }

    static List<Entry> resolve(List<SellerProductViews.Spec> specs, List<AttributeView> attributes) {
        Map<String, AttributeView> byName = new HashMap<>();
        for (AttributeView a : attributes) {
            byName.putIfAbsent(NameNormalizer.normalize(a.name()), a);
            byName.putIfAbsent(NameNormalizer.normalize(a.code()), a);
        }
        List<Entry> out = new ArrayList<>();
        if (specs == null) return out;
        for (SellerProductViews.Spec s : specs) {
            if (s == null || s.name() == null || s.value() == null) continue;
            String name = s.name().trim();
            // giữ xuống dòng của người bán (chuẩn hóa \r\n → \n, bỏ dòng trống thừa liên tiếp)
            String value = s.value().replace("\r\n", "\n").replace('\r', '\n').replaceAll("\n{3,}", "\n\n").trim();
            if (name.isEmpty() || value.isEmpty()) continue;
            AttributeView attr = byName.get(NameNormalizer.normalize(name));
            out.add(new Entry(attr == null ? name : attr.code(), attr == null ? name : attr.name(), value, attr));
        }
        return out;
    }

    static Map<String, String> toMap(List<Entry> entries) {
        Map<String, String> map = new LinkedHashMap<>();
        entries.forEach(e -> map.putIfAbsent(e.key(), e.value()));
        return map;
    }

    /** Giá trị ghi vào {@code specs_json}: các khóa → giá trị + {@link #ORDER_KEY} (thứ tự người bán sắp). */
    static Map<String, Object> toStored(Map<String, String> specs) {
        Map<String, Object> out = new LinkedHashMap<>(specs);
        if (specs.size() > 1) out.put(ORDER_KEY, new ArrayList<>(specs.keySet()));
        return out;
    }

    /** Xếp lại {@code values} theo mảng {@link #ORDER_KEY} (khóa không có trong mảng giữ cuối, theo thứ tự cũ). */
    public static Map<String, String> ordered(Map<String, String> values, JsonNode order) {
        if (order == null || !order.isArray() || values.size() < 2) return values;
        Map<String, String> out = new LinkedHashMap<>();
        for (JsonNode k : order) {
            String key = k.asText();
            if (values.containsKey(key)) out.put(key, values.get(key));
        }
        values.forEach(out::putIfAbsent);
        return out;
    }

    /** {@code specs_json} (đối tượng theo mã, hoặc dạng cũ mảng {@code [{name, value}]}) → map khóa → giá trị. */
    public static Map<String, String> parse(ObjectMapper json, String raw) {
        Map<String, String> values = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) return values;
        try {
            JsonNode node = json.readTree(raw);
            if (node.isObject()) {
                for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
                    Map.Entry<String, JsonNode> e = it.next();
                    if (!e.getValue().isNull() && !ORDER_KEY.equals(e.getKey())) {
                        values.put(e.getKey(), e.getValue().isTextual() ? e.getValue().asText() : e.getValue().toString());
                    }
                }
                return ordered(values, node.get(ORDER_KEY));
            } else if (node.isArray()) {
                for (JsonNode n : node) {
                    if (n.hasNonNull("name") && n.hasNonNull("value")) {
                        values.put(n.get("name").asText(), n.get("value").asText());
                    }
                }
            }
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
        return values;
    }

    /** Map khóa → giá trị thành {@code [{name, value}]}: mã thuộc tính đổi ra tên hiển thị. */
    public static List<SellerProductViews.Spec> toViews(Map<String, String> values, List<AttributeView> attributes) {
        Map<String, String> names = new HashMap<>();
        attributes.forEach(a -> names.put(a.code(), a.name()));
        List<SellerProductViews.Spec> out = new ArrayList<>();
        values.forEach((k, v) -> out.add(new SellerProductViews.Spec(names.getOrDefault(k, k), v)));
        return out;
    }
}
