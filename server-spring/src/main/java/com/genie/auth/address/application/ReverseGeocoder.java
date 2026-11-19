package com.genie.auth.address.application;

import java.util.List;
import java.util.Optional;

/**
 * Cổng đổi tọa độ → địa chỉ chữ (plan AUTH A12.06): cài đặt {@code infra.NominatimGeocoder} (OpenStreetMap, miễn phí).
 * Chỉ trả TÊN; việc khớp tên sang mã GHN do {@link GeocodeService} làm với {@link RegionDirectory}.
 */
public interface ReverseGeocoder {

    /** Không có địa chỉ ở tọa độ này (biển, ngoài lãnh thổ…) → rỗng. Lỗi mạng → {@code 503}. */
    Optional<GeoPlace> lookup(double lat, double lng);

    /**
     * @param names       mọi tên đơn vị hành chính tìm thấy, từ nhỏ đến lớn (phường, quận, thành phố, tỉnh…) — nhà
     *                    cung cấp đặt tên trường khác nhau và địa giới mới (2025) bỏ cấp huyện, nên không gán cứng
     *                    trường nào là tỉnh / quận / phường
     * @param addressLine số nhà + tên đường (có thể rỗng)
     */
    record GeoPlace(List<String> names, String addressLine) {
    }
}
