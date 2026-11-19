package com.genie.auth.address.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.auth.address.application.RegionDirectory;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Danh mục 2 cấp trong repo đúng quy mô sau sáp nhập (plan AUTH D25): 34 tỉnh, 3.321 phường / xã, mã không trùng. */
class StaticRegionDirectoryTest {

    private final StaticRegionDirectory directory = new StaticRegionDirectory(new ObjectMapper());

    @Test
    void catalogHas34ProvincesAnd3321UniqueWards() {
        assertThat(directory.provinces()).hasSize(34);
        Set<String> codes = new HashSet<>();
        int total = 0;
        for (RegionDirectory.Region p : directory.provinces()) {
            for (RegionDirectory.Ward w : directory.wards(p.id())) {
                codes.add(w.code());
                total++;
            }
        }
        assertThat(total).isEqualTo(3321);
        assertThat(codes).hasSize(3321);
    }

    @Test
    void resolveChecksWardBelongsToProvince() {
        assertThat(directory.resolve(79, "26740")).hasValueSatisfying(r -> {
            assertThat(r.province().name()).isEqualTo("Thành phố Hồ Chí Minh");
            assertThat(r.ward().name()).isEqualTo("Phường Sài Gòn");
        });
        assertThat(directory.resolve(1, "26740")).isEmpty();       // phường của TP. HCM, tỉnh Hà Nội
        assertThat(directory.resolve(79, " ")).isEmpty();
    }
}
