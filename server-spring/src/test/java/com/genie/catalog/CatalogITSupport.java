package com.genie.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.genie.support.IntegrationTestBase;
import com.genie.support.TestDataBuilder;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Nền chung cho IT của cụm tìm kiếm & trang sản phẩm (plan 2026-11-17). */
public abstract class CatalogITSupport extends IntegrationTestBase {

    protected TestDataBuilder data;

    @BeforeEach
    void initData() {
        data = new TestDataBuilder(jdbc);
    }

    /** GET với các cặp tham số {@code "q", "ao", "size", "5"}. */
    protected MvcResult call(String path, String... params) throws Exception {
        MockHttpServletRequestBuilder req = get(path);
        for (int i = 0; i + 1 < params.length; i += 2) {
            req.param(params[i], params[i + 1]);
        }
        return mvc.perform(req).andReturn();
    }

    protected static int status(MvcResult r) {
        return r.getResponse().getStatus();
    }

    protected static <T> T read(MvcResult r, String path) throws Exception {
        return JsonPath.read(r.getResponse().getContentAsString(StandardCharsets.UTF_8), path);
    }

    /** {@code $.data[*].id} dạng long. */
    protected static List<Long> ids(MvcResult r) throws Exception {
        List<Number> raw = read(r, "$.data[*].id");
        return raw.stream().map(Number::longValue).toList();
    }
}
