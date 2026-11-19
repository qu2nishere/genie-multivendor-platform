package com.genie.auth.address.infra;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.auth.address.application.RegionDirectory;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

/**
 * Danh mục hành chính 2 cấp đọc từ {@code classpath:regions/regions.json} — dùng khi chưa cấu hình GHN (máy dev, test).
 * Tệp trong repo là danh mục chính thức sau sáp nhập: 34 tỉnh / thành, 3.321 phường / xã / đặc khu, mã theo
 * Quyết định 19/2025/QĐ-TTg (tạo bằng {@code scripts/fetch-ghn-regions.py --official}). Khi chuyển sang GHN, chạy lại
 * script với token để mã khớp mã GHN.
 * <p>Định dạng: {@code [{id, name, wards: [{code, name}]}]}.
 */
public class StaticRegionDirectory implements RegionDirectory {

    private static final Logger log = LoggerFactory.getLogger(StaticRegionDirectory.class);
    static final String RESOURCE = "regions/regions.json";

    private final List<Region> provinces;
    private final Map<Integer, List<Ward>> wardsByProvince = new LinkedHashMap<>();

    public StaticRegionDirectory(ObjectMapper objectMapper) {
        List<ProvinceNode> tree = load(objectMapper);
        this.provinces = tree.stream().map(p -> new Region(p.id(), p.name())).toList();
        for (ProvinceNode p : tree) {
            wardsByProvince.put(p.id(), p.wards() == null ? List.of() : List.copyOf(p.wards()));
        }
        log.info("Danh mục hành chính: dùng {} ({} tỉnh / thành, 2 cấp) — chưa cấu hình GHN.", RESOURCE,
                provinces.size());
    }

    @Override
    public List<Region> provinces() {
        return provinces;
    }

    @Override
    public List<Ward> wards(int provinceId) {
        return wardsByProvince.getOrDefault(provinceId, List.of());
    }

    private static List<ProvinceNode> load(ObjectMapper objectMapper) {
        try (InputStream in = new ClassPathResource(RESOURCE).getInputStream()) {
            return objectMapper.readValue(in, new TypeReference<List<ProvinceNode>>() {
            });
        } catch (IOException e) {
            throw new IllegalStateException("Không đọc được " + RESOURCE, e);
        }
    }

    record ProvinceNode(int id, String name, List<Ward> wards) {
    }
}
