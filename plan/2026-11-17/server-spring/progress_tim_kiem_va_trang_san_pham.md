# PROGRESS — Tìm kiếm, lọc, trang sản phẩm, trang shop & chia sẻ (UC-CAT-02, 04, 05, 06, 07, 16, 18)

| | |
|---|---|
| **Plan** | [plan_tim_kiem_va_trang_san_pham.md](./plan_tim_kiem_va_trang_san_pham.md) — chi tiết từng task tra theo mã `Cxx.yy`; stack ở plan mục 1, thuật toán A1–A13 ở plan mục 2 |
| **Ngày lập** | 2026-11-17 |
| **Tổng** | 54 task + 12 commit, 9 giai đoạn |
| **Nhánh** | `feature/catalog-search` · `feature/catalog-product-detail` · `feature/catalog-shop-page` · `feature/web-catalog-real-api` |
| **Phụ trách** | Nghiêm Phú Đăng Quân — lập plan và thực hiện (phân công 2026-11-18; xem bảng Thành viên trong `docs/Document/CHECKLIST_TIEN_DO_UC.md`) |
| **Người commit** | danh tính git đã cấu hình trong repo — không `--author`, không `Co-Authored-By` |
| **Cập nhật gần nhất** | 2026-11-18 — xong C00–C03 (32 / 66), nhánh `feature/catalog-search` |

**Cách dùng**

- Xong task nào đổi `[ ]` thành `[x]`. Task bỏ qua có lý do: đổi thành `[-]` và ghi lý do ở mục Ghi chú.
- Task `COMMIT` chỉ tick sau khi qua Cổng kiểm tra (plan mục 8); điền mã commit 7 ký tự vào chỗ `hash`.
- Thông điệp commit ghi sẵn bên dưới, thêm ` [#GNE-<số>]` vào cuối khi đã có mã Jira.
- Cập nhật cột "Xong" của bảng Tổng quan và dòng "Cập nhật gần nhất" sau mỗi buổi làm; ghi một dòng vào Nhật ký cuối trang.

---

## Stack tóm tắt (plan mục 1)

| Việc | Công nghệ / thuật toán |
|---|---|
| Tìm theo từ khóa | PostgreSQL FTS (`tsvector` có trọng số + `unaccent`, `websearch_to_tsquery`, prefix `:*`, `ts_rank_cd`, GIN) → bù gõ sai bằng `pg_trgm` `word_similarity` ≥ 0,3 khi < 5 kết quả |
| Lọc & sắp xếp | SQL builder động (`NamedParameterJdbcTemplate`), CTE đệ quy danh mục con, JSONB `@>`, disjunctive faceting, phân trang `page/size` chặn trang sâu |
| Gợi ý khi gõ | Tiền tố + trigram trên tên bỏ dấu (danh mục / shop / sản phẩm), debounce 250 ms + `AbortController` |
| Xu hướng | Redis Sorted Set `ZINCRBY` + `ZUNIONSTORE` (top-K cửa sổ trượt), chống spam `SET NX`, job 5 phút UPSERT `search_keyword_stats` |
| Chi tiết & SKU | JPA nạp theo lô, ma trận `option_groups` → bảng tra `options.join('|')`, tồn = `stock − reserved`, cache phần tĩnh 5 phút |
| Lượt xem theo nguồn | Redis `HINCRBY` + HyperLogLog `PFADD` (khách duy nhất), `sendBeacon`, job 5 phút → `product_view_stats` |
| Sản phẩm tương tự | Content-based: ứng viên cùng danh mục lá ±50% giá → điểm `0.5·similarity + 0.3·log(sold) + 0.2·rating`, cache 30 phút |
| Gợi ý hôm nay | Random có seed theo ngày `md5(id ‖ ngày)` + trộn 60/40 theo danh mục xem gần đây (Redis LIST) |
| Trang shop | Cột tổng hợp `vendor_profiles` + cache Redis 10 phút, lưới dùng lại tìm kiếm với `shop_id` |
| Deeplink | Slug sinh từ tên (Java + TS cùng thuật toán), 301 khi slug lệch, Open Graph + JSON-LD (Astro SSR), Web Share API → Clipboard API |
| Hạ tầng chung | Spring Cache + Redis, ETag `304`, `RateLimiter` 60/phút/IP, Flyway `V18`, Testcontainers PG + Redis |

---

## Quyết định (plan mục 4)

| # | Vấn đề | Đã chốt | Kết quả chốt / ngày |
|---|---|:---:|---|
| D1 | Đường dẫn theo UC hay API | ☑ | Theo API (`/public/...`); sửa Usecase.md ở C08.03 — chốt theo đề xuất plan, 2026-11-18 |
| D2 | `products` không có cột `slug` | ☑ | Sinh slug từ `name` khi đọc, không thêm cột; 301 khi slug lệch — chốt theo đề xuất plan, 2026-11-18 |
| D3 | Trọng số `search_vector` + index trigram bỏ dấu (`V18`) | ☑ | `V18`: `search_vector` có trọng số A/B/D + index GIN trigram trên `immutable_unaccent(lower(name))` — chốt theo đề xuất plan, 2026-11-18 |
| D4 | Index / cột giá thấp nhất cho lọc & sắp theo giá | ☑ | `V18`: index `product_variants (product_id, price) WHERE deleted_at IS NULL`; cột `min_price` để sau nếu đo chậm — chốt theo đề xuất plan, 2026-11-18 |
| D5 | Trending 24 giờ (API) hay 7 ngày (UC); thời gian cache | ☑ | Theo API: 24 giờ (hôm nay trọng số 1 + hôm qua 0,5), cache 10 phút — chốt theo đề xuất plan, 2026-11-18 |
| D6 | Đọc bảng module `vendor` / `shipping` trong SQL tìm kiếm; cổng `CatalogShopDirectory` | ☑ | SQL đọc JOIN thẳng bảng `vendor_profiles`, `vendor_shipping_config`, `users` (read-model); không import class module khác — chốt theo đề xuất plan, 2026-11-18 |
| D7 | Vị trí `GET /public/shops/{id}`, mã lỗi shop, `is_online` | ☑ | `vendor/shop/api/PublicShopController`; thêm `GNE-VND-4041`; `is_online = false` tới khi có presence — chốt theo đề xuất plan, 2026-11-18 |
| D8 | Nguồn `original_price`, `discount_percent`, `discount_min` | ☑ | `original_price = base_price` khi `base_price > price`, ngược lại `null`; `discount_min` lọc theo công thức này — chốt theo đề xuất plan, 2026-11-18 |
| D9 | "Phổ biến nhất" không có trong enum `sort` | ☑ | Không thêm enum; "Phổ biến nhất" = `RELEVANCE` (có `q`) / `BEST_SELLING` — chốt theo đề xuất plan, 2026-11-18 |
| D10 | Bộ lọc `has_shop_voucher` cần module `promotion` | ☑ | Cổng `ShopVoucherAvailability`, mặc định không hỗ trợ → bỏ qua bộ lọc, trả `facets.has_shop_voucher_supported = false` — chốt theo đề xuất plan, 2026-11-18 |
| D11 | Cache chi tiết sản phẩm vs tồn kho thời gian thực | ☑ | Cache phần tĩnh 5 phút, tồn đọc riêng mỗi request — chốt theo đề xuất plan, 2026-11-18 |
| D12 | Số sản phẩm tương tự: 9 (UC) hay 6 (API) | ☑ | Mặc định 6, `limit` 1–12 — chốt theo đề xuất plan, 2026-11-18 |

## Tổng quan

| Giai đoạn | Nội dung | UC | Task | Commit | Xong | Mã Jira | Nhánh |
|---|---|---|---:|---:|---:|---|---|
| C00 | Chuẩn bị | — | 6 | 0 | 6 / 6 | — | `feature/catalog-search` |
| C01 | Nền: migration, cache, chuẩn hóa | — | 5 | 2 | 7 / 7 | — | `feature/catalog-search` |
| C02 | Tìm kiếm & lọc | CAT-02, CAT-05 | 10 | 2 | 12 / 12 | — | `feature/catalog-search` |
| C03 | Gợi ý & xu hướng | CAT-04 | 6 | 1 | 7 / 7 | — | `feature/catalog-search` |
| C04 | Chi tiết sản phẩm & lượt xem | CAT-06 | 6 | 2 | 0 / 8 | — | `feature/catalog-product-detail` |
| C05 | Tương tự & gợi ý hôm nay | CAT-07 | 4 | 1 | 0 / 5 | — | `feature/catalog-product-detail` |
| C06 | Trang shop | CAT-16 | 3 | 1 | 0 / 4 | — | `feature/catalog-shop-page` |
| C07 | Nối giao diện `web/` + Deeplink | cả 7 UC, CAT-18 | 9 | 2 | 0 / 11 | — | `feature/web-catalog-real-api` |
| C08 | Hoàn thiện use case | cả 7 UC | 5 | 1 | 0 / 6 | — | PR cuối |
| **Tổng** | | | **54** | **12** | **32 / 66** | | |

## C00 — Chuẩn bị

- [x] **C00.01** Kiểm danh tính git
- [x] **C00.02** Chốt quyết định D1–D12
- [x] **C00.03** Đăng ký `V18` ở README 2026-11-16 mục 4
- [x] **C00.04** Tạo nhánh `feature/catalog-search`
- [x] **C00.05** Dữ liệu thử (≥ 50 sản phẩm seed + script 10.000 sản phẩm giả)
- [x] **C00.06** Đọc màn mock `/search`, `/c/…`, `/p/…`, `/shop/…`, dropdown header

## C01 — Nền: migration, cache, chuẩn hóa

- [x] **C01.01** `V18__catalog_search_tuning.sql` (trọng số FTS, trigram bỏ dấu, index giá SKU) + ENTITY 13.1
- [x] **C01.02** `CacheConfig` — `RedisCacheManager` + TTL theo tên cache
- [x] **C01.03** `EtagConfig` — ETag + `Cache-Control` cho `/api/v1/public/**`
- [x] **C01.04** `KeywordNormalizer` + `SlugUtil` + test khớp `unaccent()`
- [x] **C01.05** `TestDataBuilder` — danh mục, brand, shop, sản phẩm, SKU, ảnh
- [x] **C01.C1** 🔖 COMMIT `feat(db): weight product search vector and add unaccent trigram index` · hash: `4f808a9`
- [x] **C01.C2** 🔖 COMMIT `feat(common): configure redis cache and etag for public api` · hash: `86e5ddc`

## C02 — Tìm kiếm & lọc (UC-CAT-02, UC-CAT-05)

- [x] **C02.01** `ProductSort` + `ProductSearchRequest` (validate)
- [x] **C02.02** `ProductSearchSqlBuilder` + test
- [x] **C02.03** `ProductSearchService` — FTS tầng 1 + trigram tầng 2
- [x] **C02.04** Dựng `$ProductCard` theo lô
- [x] **C02.05** `FacetService` — disjunctive facets
- [x] **C02.06** Cổng `CatalogShopDirectory`, `ShopVoucherAvailability`
- [x] **C02.07** `PublicProductSearchController` + rate limit
- [x] **C02.08** `ProductSearchIT`
- [x] **C02.09** `FacetIT`
- [x] **C02.10** `SearchPerformanceIT` (p95 < 300 ms / 10.000 sản phẩm)
- [x] **C02.C1** 🔖 COMMIT `feat(catalog): search products with full text and trigram fallback` · hash: `a45aedb`
- [x] **C02.C2** 🔖 COMMIT `feat(catalog): add multi criteria filters sorting and facets` · hash: `fa33542`

## C03 — Gợi ý & xu hướng (UC-CAT-04)

- [x] **C03.01** `SearchKeywordCounter` (dedupe + ZINCRBY + pending)
- [x] **C03.02** `SearchKeywordFlushJob` (5 phút, UPSERT)
- [x] **C03.03** `TrendingService` (ZUNIONSTORE + blocklist + dự phòng SQL)
- [x] **C03.04** `SuggestionService` (2 danh mục / 2 shop / 5 sản phẩm)
- [x] **C03.05** `PublicSearchController` — `/suggestions`, `/trending`
- [x] **C03.06** `SuggestionIT` + `TrendingIT`
- [x] **C03.C1** 🔖 COMMIT `feat(catalog): add search suggestions and trending keywords` · hash: `ac22d10`

## C04 — Chi tiết sản phẩm & lượt xem (UC-CAT-06)

- [ ] **C04.01** `VariantMatrixBuilder` + test
- [ ] **C04.02** `ProductDetailService` (cache tĩnh, tồn đọc riêng)
- [ ] **C04.03** `PublicProductController` — `/products/{id}`, `/products/{id}/variants`
- [ ] **C04.04** `ProductViewCounter` + `ProductViewFlushJob` + `RecentCategoryTracker`
- [ ] **C04.05** `PublicProductViewController` — `POST /products/{id}/views`
- [ ] **C04.06** `ProductDetailIT` + `ProductViewIT`
- [ ] **C04.C1** 🔖 COMMIT `feat(catalog): show product detail with sku matrix and available stock` · hash: `…`
- [ ] **C04.C2** 🔖 COMMIT `feat(catalog): count product views by traffic source` · hash: `…`

## C05 — Tương tự & gợi ý hôm nay (UC-CAT-07)

- [ ] **C05.01** `SimilarProductService` (candidate + scoring + bù danh mục cha)
- [ ] **C05.02** `RecommendationService` (seeded random + interleave)
- [ ] **C05.03** Endpoint `/products/{id}/similar`, `/recommendations`
- [ ] **C05.04** `SimilarProductIT` + `RecommendationIT`
- [ ] **C05.C1** 🔖 COMMIT `feat(catalog): recommend similar products and daily picks` · hash: `…`

## C06 — Trang shop (UC-CAT-16)

- [ ] **C06.01** Phối hợp chủ plan Mở shop & KYC (D7)
- [ ] **C06.02** `PublicShopController` — `/shops/{id}`, `/shops?featured=true`
- [ ] **C06.03** `PublicShopIT`
- [ ] **C06.C1** 🔖 COMMIT `feat(vendor): add public shop page api` · hash: `…`

## C07 — Nối giao diện `web/` + Deeplink (UC-CAT-18)

- [ ] **C07.01** `suggest.api.ts` — đường dẫn + DTO 3 nhóm + debounce / abort
- [ ] **C07.02** `product.api.ts` — facets gộp, similar, recommendations, `recordView`
- [ ] **C07.03** Trang `/search`, `/c/[slug]` — bộ lọc theo URL params
- [ ] **C07.04** Trang `/p/[slug]-[id]` — slug 301, 404, `?v=`, `src`, Open Graph giá
- [ ] **C07.05** `WishShare.tsx` — Web Share API / Clipboard
- [ ] **C07.06** `shop.api.ts` + `/shop/[id]`
- [ ] **C07.07** Vitest: mapper, `slug.ts`, khóa giá trị biến thể
- [ ] **C07.08** Chạy thật hai phía (`PUBLIC_REAL_API=catalog,search,shop`)
- [ ] **C07.09** `npm run verify` + Playwright `/search`, `/p/…`
- [ ] **C07.C1** 🔖 COMMIT `fix(web): call catalog endpoints defined in the api spec` · hash: `…`
- [ ] **C07.C2** 🔖 COMMIT `feat(web): share product deeplink with open graph preview` · hash: `…`

## C08 — Hoàn thiện use case

- [ ] **C08.01** `docs/api-tests/catalog-search.http`
- [ ] **C08.02** OpenAPI
- [ ] **C08.03** Tài liệu nguồn (Usecase, API 5.x / 17, ENTITY 13.1, LLD 9 + 10)
- [ ] **C08.04** Tiến độ (checklist, xlsx, handoff, CLAUDE.md)
- [ ] **C08.C1** 🔖 COMMIT `docs(catalog): record search and product page use cases as implemented` · hash: `…`
- [ ] **C08.05** Mở PR vào `develop`

---

## Chốt plan — tick tiến độ use case

Làm **ngay khi xong plan** (hoặc xong từng use case), trong file [`docs/Document/CHECKLIST_TIEN_DO_UC.md`](../../../docs/Document/CHECKLIST_TIEN_DO_UC.md) — cách tick xem mục 0 của file đó.

- [ ] `UC-CAT-02` Tìm kiếm sản phẩm theo từ khóa (sau C02 + C07)
- [ ] `UC-CAT-04` Gợi ý từ khóa tìm kiếm & Xu hướng (sau C03 + C07)
- [ ] `UC-CAT-05` Lọc nâng cao & Sắp xếp sản phẩm đa tiêu chí (sau C02 + C07; `◐` nếu `has_shop_voucher` / `discount_min` còn chờ module khác — D8, D10)
- [ ] `UC-CAT-06` Xem chi tiết sản phẩm, ma trận biến thể SKU & Tồn kho (sau C04 + C07)
- [ ] `UC-CAT-07` Xem gợi ý sản phẩm tương tự (sau C05 + C07)
- [ ] `UC-CAT-16` Xem trang gian hàng (sau C06 + C07)
- [ ] `UC-CAT-18` Chia sẻ sản phẩm bằng liên kết — Deeplink (sau C04 lượt xem + C07.04–C07.05)
- [ ] Cập nhật số ở mục 1 *Tổng quan* của checklist
- [ ] Thêm một dòng vào mục 5 *Nhật ký cập nhật* của checklist (ngày · use case · mã commit / PR)
- [ ] Đổi cột *Hiện trạng mã nguồn* của 7 use case trong `Quản Lý UseCase.xlsx`

## Ghi chú

Ghi ở đây: lý do bỏ qua task (`[-]`), chỗ làm khác plan, số đo hiệu năng (`EXPLAIN ANALYZE`, p95), việc tồn chuyển sang plan khác.

| Task | Ghi chú |
|---|---|
| C00.05 | Không phụ thuộc seed: mọi IT tự dựng dữ liệu bằng `TestDataBuilder`; 10.000 sản phẩm giả sinh bằng `generate_series` ngay trong `SearchPerformanceIT` |
| C01.02 | Làm khác plan: không dùng `RedisCacheManager` + `@Cacheable` mà viết `common/cache/JsonCache` (StringRedisTemplate + Jackson, chỉ rõ kiểu khi đọc record) — Redis lỗi thì đọc thẳng nguồn (N7). TTL đặt ở từng service |
| C01.C2 | Thêm `PageResponse` (vỏ danh sách có `pagination` + `summary`, API_SPEC 0.3) và `ApiResponseFactory.page()/body()` trong cùng commit |
| C01.04 | Thêm commit `08c76cd` `feat(catalog): add keyword normalizer slug util and catalog test fixtures` (normalizer, slug, `TestDataBuilder`, `KeywordNormalizerIT` so 30 mẫu với `unaccent()` của PG, bộ mẫu slug chung `docs/api-tests/slug-samples.json`) |
| C02.03 | Tầng 1 dùng `to_tsquery('simple', 'tai & nghe:*')` thay `websearch_to_tsquery`: từ khóa đã chuẩn hóa chỉ còn chữ / số nên cú pháp web không còn tác dụng; tsquery tiền tố đã bao luôn khớp nguyên từ. Trang tối đa 100 (API 0.4) thay vì 50 |
| C02.05 | Lọc thuộc tính dùng `(specs_json ->> code) IN (…)` thay `@>`: khớp cả giá trị số trong JSON; chưa dùng index `jsonb_path_ops` (đủ cho MVP). Chế độ bù gõ sai: facet đếm trên đúng tập kết quả (≤ 105 ID), không còn disjunctive — để giữ p95 |
| C02.06 | Bỏ cổng `CatalogShopDirectory`: SQL đọc JOIN thẳng `vendor_profiles` (D6) nên không cần đổi `shop_id ↔ vendor_id` qua cổng. Còn cổng `ShopVoucherAvailability` (mặc định chưa hỗ trợ) |
| C02.10 | Đo 2026-11-18 (10.000 sản phẩm, 36 request, máy dev + Testcontainers): p50 = 29 ms, **p95 = 50 ms**, max 256 ms (request đầu, JIT). Lần đầu p95 = 538 ms → 2 sửa: chỉ JOIN LATERAL giá khi lọc / sắp theo giá; facet chế độ trigram đếm trên tập kết quả. `EXPLAIN` (tắt seq scan) dùng `idx_products_search_vector` và `idx_products_name_unaccent_trgm`; với 10.000 dòng planner tự chọn seq scan (3,5 ms) là hợp lý. Thiếu `ANALYZE users / vendor_profiles` làm planner chọn nested loop ~500 ms — production có autovacuum |
| C03.01 | Đếm đồng bộ (không `@Async`): 3 lệnh Redis nhanh, bọc try/catch — test xác định được. Chỉ đếm lượt tìm toàn sàn, trang 1, có kết quả; từ khóa lưu bản chuẩn hóa còn dấu (hiển thị ở Xu hướng) |
| C03.02 | Xóa `search:kw:flushing` trong `afterCommit` — DB rollback thì giữ lại để lần sau ghi; `flush()` public để test gọi (cron tắt trong test) |
| C03.04 | API 5.4 không có trường "từ khóa phổ biến khớp tiền tố" (UC-CAT-04 bước 3) → không trả; client dùng `/trending` khi ô trống |

## Nhật ký

| Ngày | Việc đã làm | Commit |
|---|---|---|
| 2026-11-17 | Lập plan + progress (54 task, 12 commit, 9 giai đoạn; stack + thuật toán A1–A13) — chưa bắt đầu làm | — |
| 2026-11-18 | Phân công Nghiêm Phú Đăng Quân; ghi "đã lập plan `plan/2026-11-17`" + cột Phụ trách cho 7 UC trong `CHECKLIST_TIEN_DO_UC.md` (bảng Thành viên, mục 5 Nhật ký) và cột *Người phụ trách* / sheet *Thành viên* trong `Quản Lý UseCase.xlsx` — trạng thái vẫn ⬜ Chưa triển khai | — |
| 2026-11-18 | C00 chuẩn bị: nhánh `feature/catalog-search`, chốt D1–D12 theo đề xuất, đăng ký `V18`; commit plan + checklist | `04b866d` |
| 2026-11-18 | C01 nền: `V18` (trọng số FTS, trigram bỏ dấu, index giá), `JsonCache`, ETag `/public`, `PageResponse`, normalizer + slug + `TestDataBuilder` | `4f808a9` `86e5ddc` `08c76cd` |
| 2026-11-18 | C02 tìm kiếm & lọc: `GET /public/products` (FTS + trigram, lọc đa tiêu chí, facet, rate limit 60/phút); 7 unit + 16 IT xanh; đo hiệu năng p95 50 ms | `a45aedb` `fa33542` |
| 2026-11-18 | C03 gợi ý & xu hướng: `/public/search/suggestions`, `/public/search/trending`, bộ đếm Redis ZSET + job ghi gộp 5 phút; 9 IT xanh | `ac22d10` |
