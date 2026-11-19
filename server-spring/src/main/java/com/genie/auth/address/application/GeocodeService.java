package com.genie.auth.address.application;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.stereotype.Service;

/**
 * "Dùng vị trí hiện tại (GPS)" — {@code GET /public/regions/reverse-geocode} (API_SPEC 3.9; plan AUTH A12.06, D15,
 * D25). Tọa độ → tên (OpenStreetMap) → khớp Tỉnh rồi Phường trong {@link RegionDirectory} (địa giới 2 cấp) bằng
 * {@link RegionNames}. Khớp được tới đâu trả tới đó; cấp không khớp để {@code null} cho người dùng tự chọn (ví dụ dữ
 * liệu OpenStreetMap nơi đó chưa cập nhật tên phường mới).
 */
@Service
public class GeocodeService {

    private final ReverseGeocoder geocoder;
    private final RegionDirectory regions;

    public GeocodeService(ReverseGeocoder geocoder, RegionDirectory regions) {
        this.geocoder = geocoder;
        this.regions = regions;
    }

    /**
     * @param province    {@code null} nếu không khớp được tỉnh nào
     * @param ward        {@code null} nếu không khớp được phường / xã
     * @param addressLine số nhà + đường, rỗng nếu không có
     */
    public record GeocodeView(RegionDirectory.Region province, RegionDirectory.Ward ward, String addressLine) {
    }

    public GeocodeView reverse(double lat, double lng) {
        Optional<ReverseGeocoder.GeoPlace> place = geocoder.lookup(lat, lng);
        if (place.isEmpty()) {
            return new GeocodeView(null, null, "");
        }
        List<String> names = place.get().names();
        String line = place.get().addressLine() == null ? "" : place.get().addressLine();

        // names xếp từ nhỏ đến lớn: phường chỉ được khớp với các tên NHỎ hơn tên đã khớp tỉnh
        Match<RegionDirectory.Region> province = match(regions.provinces(), names, names.size(),
                RegionDirectory.Region::name);
        if (province == null) {
            return new GeocodeView(null, null, line);
        }
        Match<RegionDirectory.Ward> ward = match(regions.wards(province.value().id()), names, province.index(),
                RegionDirectory.Ward::name);
        return new GeocodeView(province.value(), ward == null ? null : ward.value(), line);
    }

    private record Match<T>(T value, int index) {
    }

    /** Tên đầu tiên (từ nhỏ đến lớn, chỉ xét {@code names[0, limit)}) trùng một ứng viên. */
    private static <T> Match<T> match(List<T> candidates, List<String> names, int limit,
                                      Function<T, String> nameOf) {
        for (int i = 0; i < Math.min(limit, names.size()); i++) {
            String n = names.get(i);
            for (T c : candidates) {
                if (RegionNames.same(n, nameOf.apply(c))) {
                    return new Match<>(c, i);
                }
            }
        }
        return null;
    }
}
