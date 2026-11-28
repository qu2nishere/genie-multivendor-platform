// Thứ tự dòng thông số người bán sắp được giữ qua specs_json (plan C1, 2026-11-27 — SpecCodec.ORDER_KEY)
package com.genie.catalog.product.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SpecCodecTest {

    private final ObjectMapper json = new ObjectMapper();

    @Test
    void storesSellerOrderAndReadsItBack() throws Exception {
        Map<String, String> specs = new LinkedHashMap<>();
        specs.put("Tính năng màn hình", "120 Hz\nHDR");
        specs.put("os", "iOS 27");
        specs.put("chip", "A20 Pro");
        String stored = json.writeValueAsString(SpecCodec.toStored(specs));

        Map<String, String> back = SpecCodec.parse(json, stored);
        assertThat(List.copyOf(back.keySet())).containsExactly("Tính năng màn hình", "os", "chip");
        assertThat(back).doesNotContainKey(SpecCodec.ORDER_KEY).containsEntry("Tính năng màn hình", "120 Hz\nHDR");
    }

    @Test
    void singleSpecHasNoOrderKey() {
        assertThat(SpecCodec.toStored(Map.of("os", "iOS"))).doesNotContainKey(SpecCodec.ORDER_KEY);
    }

    @Test
    void legacyObjectWithoutOrderStillParses() {
        Map<String, String> back = SpecCodec.parse(json, "{\"os\": \"iOS\", \"chip\": \"A20\"}");
        assertThat(back).containsEntry("os", "iOS").containsEntry("chip", "A20").hasSize(2);
    }

    @Test
    void keysMissingFromOrderGoLast() throws Exception {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("a", "1");
        values.put("b", "2");
        values.put("c", "3");
        Map<String, String> out = SpecCodec.ordered(values, json.readTree("[\"c\", \"x\", \"a\"]"));
        assertThat(List.copyOf(out.keySet())).containsExactly("c", "a", "b");
    }
}
