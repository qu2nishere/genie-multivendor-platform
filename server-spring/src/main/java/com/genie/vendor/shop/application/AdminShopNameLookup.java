package com.genie.vendor.shop.application;

import com.genie.admin.users.application.ShopNameLookup;
import com.genie.vendor.shop.domain.VendorProfile;
import com.genie.vendor.shop.domain.VendorProfileRepository;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cài cổng {@link ShopNameLookup} của module {@code admin} (plan B1 D10): tên shop cho màn {@code /admin/users} và tìm
 * chủ shop theo tên shop (không dấu, tối đa 200 kết quả — đủ cho một trang danh sách quản trị).
 */
@Component
public class AdminShopNameLookup implements ShopNameLookup {

    static final int MAX_MATCHES = 200;

    private final VendorProfileRepository profiles;
    private final NamedParameterJdbcTemplate jdbc;

    public AdminShopNameLookup(VendorProfileRepository profiles, NamedParameterJdbcTemplate jdbc) {
        this.profiles = profiles;
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, String> shopNames(Collection<Long> vendorUserIds) {
        if (vendorUserIds.isEmpty()) return Map.of();
        return profiles.findByUserIdIn(vendorUserIds).stream()
                .collect(Collectors.toMap(VendorProfile::getUserId, VendorProfile::getShopName, (a, b) -> a));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> vendorUserIdsByShopName(String q) {
        if (q == null || q.isBlank()) return Set.of();
        List<Long> ids = jdbc.queryForList("SELECT user_id FROM vendor_profiles"
                        + " WHERE position(immutable_unaccent(lower(:q)) in immutable_unaccent(lower(shop_name))) > 0"
                        + " ORDER BY user_id LIMIT " + MAX_MATCHES,
                new MapSqlParameterSource("q", q.trim()), Long.class);
        return new HashSet<>(ids);
    }
}
