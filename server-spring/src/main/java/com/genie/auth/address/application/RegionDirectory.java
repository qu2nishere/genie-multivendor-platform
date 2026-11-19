package com.genie.auth.address.application;

import java.util.List;
import java.util.Optional;

/**
 * Danh mục đơn vị hành chính <b>2 cấp</b> theo địa giới mới từ 01/07/2025 (API_SPEC 3.9, UC-AUTH-09; plan AUTH
 * A12.01, D15, D25): Tỉnh / thành → Phường / xã / đặc khu — không còn cấp quận / huyện.
 * Cài đặt: {@code infra.GhnRegionDirectory} (GHN master-data v3, cache Redis 24 giờ) khi có {@code GHN_TOKEN} +
 * {@code GHN_SHOP_ID}; thiếu → {@code infra.StaticRegionDirectory} đọc {@code resources/regions/regions.json}
 * (34 tỉnh / 3.321 xã theo mã ĐVHC của Quyết định 19/2025/QĐ-TTg).
 * Dùng chung cho sổ địa chỉ khách và địa chỉ lấy hàng của shop (module vendor).
 */
public interface RegionDirectory {

    List<Region> provinces();

    /** Phường / xã / đặc khu của tỉnh; tỉnh không tồn tại → rỗng. */
    List<Ward> wards(int provinceId);

    /**
     * Kiểm phường có thuộc tỉnh không và trả tên chuẩn để lưu (server tự điền tên, không tin client).
     * Sai → rỗng.
     */
    default Optional<ResolvedRegion> resolve(int provinceId, String wardCode) {
        if (wardCode == null || wardCode.isBlank()) {
            return Optional.empty();
        }
        Optional<Region> province = provinces().stream().filter(p -> p.id() == provinceId).findFirst();
        if (province.isEmpty()) {
            return Optional.empty();
        }
        return wards(provinceId).stream().filter(w -> w.code().equals(wardCode.trim())).findFirst()
                .map(w -> new ResolvedRegion(province.get(), w));
    }

    /** {@code $Region} — tỉnh / thành theo mã số. */
    record Region(int id, String name) {
    }

    /** Phường / xã / đặc khu theo mã chuỗi. */
    record Ward(String code, String name) {
    }

    record ResolvedRegion(Region province, Ward ward) {
    }
}
