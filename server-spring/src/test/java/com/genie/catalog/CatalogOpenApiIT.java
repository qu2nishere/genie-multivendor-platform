package com.genie.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** OpenAPI có đủ endpoint của cụm tìm kiếm & trang sản phẩm, summary mang mã UC (plan 2026-11-17 C08.02; TS 13.7). */
class CatalogOpenApiIT extends CatalogITSupport {

    @Test
    void apiDocsListEveryEndpointWithUseCaseCode() throws Exception {
        String docs = call("/v3/api-docs").getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(docs).contains(
                "/api/v1/public/products\"", "/api/v1/public/products/{id}\"", "/api/v1/public/products/{id}/variants",
                "/api/v1/public/products/{id}/views", "/api/v1/public/products/{id}/similar",
                "/api/v1/public/recommendations", "/api/v1/public/search/suggestions", "/api/v1/public/search/trending",
                "/api/v1/public/shops/{id}", "/api/v1/public/shops\"");
        assertThat(docs).contains("UC-CAT-02", "UC-CAT-04", "UC-CAT-05", "UC-CAT-06", "UC-CAT-07", "UC-CAT-16", "UC-CAT-18");
    }
}
