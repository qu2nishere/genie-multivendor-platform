package com.genie.auth.address;

import static org.assertj.core.api.Assertions.assertThat;

import com.genie.auth.address.application.RegionNames;
import org.junit.jupiter.api.Test;

class RegionNamesTest {

    @Test
    void stripsAccentsAndAdministrativePrefixes() {
        assertThat(RegionNames.normalize("Thành phố Hồ Chí Minh")).isEqualTo("ho chi minh");
        assertThat(RegionNames.normalize("TP. Hồ Chí Minh")).isEqualTo("ho chi minh");
        assertThat(RegionNames.normalize("Ho Chi Minh City")).isEqualTo("ho chi minh");
        assertThat(RegionNames.normalize("Phường Bến Nghé")).isEqualTo("ben nghe");
        assertThat(RegionNames.normalize("P. Bến Nghé")).isEqualTo("ben nghe");
        assertThat(RegionNames.normalize("Quận Đống Đa")).isEqualTo("dong da");
        assertThat(RegionNames.normalize("Tỉnh Bình Dương")).isEqualTo("binh duong");
        assertThat(RegionNames.normalize("Thành phố Thủ Đức")).isEqualTo("thu duc");
    }

    @Test
    void numberedUnitsMatchExactlyOnly() {
        assertThat(RegionNames.same("Phường 1", "P. 1")).isTrue();
        assertThat(RegionNames.same("Phường 1", "P. 13")).isFalse();
        assertThat(RegionNames.same("Quận 1", "Quận 1")).isTrue();
        assertThat(RegionNames.same("", "")).isFalse();
        assertThat(RegionNames.same("Phường 5", "Quận 5")).isFalse();             // cùng số, khác cấp
        assertThat(RegionNames.same("Thành phố Thủ Đức", "TP. Thủ Đức")).isTrue();
    }

    @Test
    void newTwoLevelNamesMatch() {
        assertThat(RegionNames.same("Đặc khu Phú Quốc", "Phú Quốc")).isTrue();
        assertThat(RegionNames.same("Tỉnh Đồng Nai", "Thành phố Đồng Nai")).isTrue();     // OSM cũ ↔ tên mới
        assertThat(RegionNames.same("Phường Trấn Biên", "Xã Trấn Biên")).isTrue();       // cùng cấp xã
        assertThat(RegionNames.same("Thanh Hoá", "Tỉnh Thanh Hóa")).isTrue();            // "oá" ↔ "óa"
    }
}
