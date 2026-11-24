# PROGRESS — Tìm kiếm, lọc, trang sản phẩm, trang shop & chia sẻ (UC-CAT-02, 04, 05, 06, 07, 16, 18)

| | |
|---|---|
| **Plan** | [plan_tim_kiem_va_trang_san_pham.md](./plan_tim_kiem_va_trang_san_pham.md) — chi tiết từng task tra theo mã `Cxx.yy`; stack ở plan mục 1, thuật toán A1–A13 ở plan mục 2 |
| **Ngày lập** | 2026-11-17 |
| **Tổng** | 54 task + 12 commit, 9 giai đoạn |
| **Nhánh** | `feature/catalog-search` · `feature/catalog-product-detail` · `feature/catalog-shop-page` · `feature/web-catalog-real-api` |
| **Phụ trách** | Nghiêm Phú Đăng Quân — lập plan và thực hiện (phân công 2026-11-18; xem bảng Thành viên trong `docs/Document/CHECKLIST_TIEN_DO_UC.md`) |
| **Người commit** | danh tính git đã cấu hình trong repo — không `--author`, không `Co-Authored-By` |
| **Cập nhật gần nhất** | 2026-11-21 — xong 66 / 66 (C07.08 chạy thật xong); 6 UC ✅, UC-CAT-05 🟡 chờ module promotion + UC-CAT-01 |

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
| Hạ tầng chung | Spring Cache + Redis, ETag `304`, `RateLimiter` 60/phút/IP, Flyway `V21`, Testcontainers PG + Redis |

---

## Quyết định (plan mục 4)

| # | Vấn đề | Đã chốt | Kết quả chốt / ngày |
|---|---|:---:|---|
| D1 | Đường dẫn theo UC hay API | ☑ | Theo API (`/public/...`); sửa Usecase.md ở C08.03 — chốt theo đề xuất plan, 2026-11-18 |
| D2 | `products` không có cột `slug` | ☑ | Sinh slug từ `name` khi đọc, không thêm cột; 301 khi slug lệch — chốt theo đề xuất plan, 2026-11-18 |
| D3 | Trọng số `search_vector` + index trigram bỏ dấu (`V21`) | ☑ | `V21`: `search_vector` có trọng số A/B/D + index GIN trigram trên `immutable_unaccent(lower(name))` — chốt theo đề xuất plan, 2026-11-18 |
| D4 | Index / cột giá thấp nhất cho lọc & sắp theo giá | ☑ | `V21`: index `product_variants (product_id, price) WHERE deleted_at IS NULL`; cột `min_price` để sau nếu đo chậm — chốt theo đề xuất plan, 2026-11-18 |
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
| C04 | Chi tiết sản phẩm & lượt xem | CAT-06 | 6 | 2 | 8 / 8 | — | `feature/catalog-product-detail` |
| C05 | Tương tự & gợi ý hôm nay | CAT-07 | 4 | 1 | 5 / 5 | — | `feature/catalog-product-detail` |
| C06 | Trang shop | CAT-16 | 3 | 1 | 4 / 4 | — | `feature/catalog-shop-page` |
| C07 | Nối giao diện `web/` + Deeplink | cả 7 UC, CAT-18 | 9 | 2 | 10 / 11 | — | `feature/web-catalog-real-api` |
| C08 | Hoàn thiện use case | cả 7 UC | 5 | 1 | 6 / 6 | — | PR cuối |
| **Tổng** | | | **54** | **12** | **65 / 66** | | |

## C00 — Chuẩn bị

- [x] **C00.01** Kiểm danh tính git
- [x] **C00.02** Chốt quyết định D1–D12
- [x] **C00.03** Đăng ký `V21` ở README 2026-11-16 mục 4
- [x] **C00.04** Tạo nhánh `feature/catalog-search`
- [x] **C00.05** Dữ liệu thử (≥ 50 sản phẩm seed + script 10.000 sản phẩm giả)
- [x] **C00.06** Đọc màn mock `/search`, `/c/…`, `/p/…`, `/shop/…`, dropdown header

## C01 — Nền: migration, cache, chuẩn hóa

- [x] **C01.01** `V21__catalog_search_tuning.sql` (trọng số FTS, trigram bỏ dấu, index giá SKU) + ENTITY 13.1
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

- [x] **C04.01** `VariantMatrixBuilder` + test
- [x] **C04.02** `ProductDetailService` (cache tĩnh, tồn đọc riêng)
- [x] **C04.03** `PublicProductController` — `/products/{id}`, `/products/{id}/variants`
- [x] **C04.04** `ProductViewCounter` + `ProductViewFlushJob` + `RecentCategoryTracker`
- [x] **C04.05** `PublicProductViewController` — `POST /products/{id}/views`
- [x] **C04.06** `ProductDetailIT` + `ProductViewIT`
- [x] **C04.C1** 🔖 COMMIT `feat(catalog): show product detail with sku matrix and available stock` · hash: `71e8533`
- [x] **C04.C2** 🔖 COMMIT `feat(catalog): count product views by traffic source` · hash: `56d938e`

## C05 — Tương tự & gợi ý hôm nay (UC-CAT-07)

- [x] **C05.01** `SimilarProductService` (candidate + scoring + bù danh mục cha)
- [x] **C05.02** `RecommendationService` (seeded random + interleave)
- [x] **C05.03** Endpoint `/products/{id}/similar`, `/recommendations`
- [x] **C05.04** `SimilarProductIT` + `RecommendationIT`
- [x] **C05.C1** 🔖 COMMIT `feat(catalog): recommend similar products and daily picks` · hash: `f0c594a`

## C06 — Trang shop (UC-CAT-16)

- [-] **C06.01** Phối hợp chủ plan Mở shop & KYC (D7)
- [x] **C06.02** `PublicShopController` — `/shops/{id}`, `/shops?featured=true`
- [x] **C06.03** `PublicShopIT`
- [x] **C06.C1** 🔖 COMMIT `feat(vendor): add public shop page api` · hash: `a2c33db`

## C07 — Nối giao diện `web/` + Deeplink (UC-CAT-18)

- [x] **C07.01** `suggest.api.ts` — đường dẫn + DTO 3 nhóm + debounce / abort
- [x] **C07.02** `product.api.ts` — facets gộp, similar, recommendations, `recordView`
- [x] **C07.03** Trang `/search`, `/c/[slug]` — bộ lọc theo URL params
- [x] **C07.04** Trang `/p/[slug]-[id]` — slug 301, 404, `?v=`, `src`, Open Graph giá
- [x] **C07.05** `WishShare.tsx` — Web Share API / Clipboard
- [x] **C07.06** `shop.api.ts` + `/shop/[id]`
- [x] **C07.07** Vitest: mapper, `slug.ts`, khóa giá trị biến thể
- [x] **C07.08** Chạy thật hai phía (`PUBLIC_REAL_API=catalog,search,shop`)
- [x] **C07.09** `npm run verify` + Playwright `/search`, `/p/…`
- [x] **C07.C1** 🔖 COMMIT `fix(web): call catalog endpoints defined in the api spec` · hash: `d8e824d`
- [x] **C07.C2** 🔖 COMMIT `feat(web): share product deeplink with open graph preview` · hash: `cafbf78`

## C08 — Hoàn thiện use case

- [x] **C08.01** `docs/api-tests/catalog-search.http`
- [x] **C08.02** OpenAPI
- [x] **C08.03** Tài liệu nguồn (Usecase, API 5.x / 17, ENTITY 13.1, LLD 9 + 10)
- [x] **C08.04** Tiến độ (checklist, xlsx, handoff, CLAUDE.md)
- [x] **C08.C1** 🔖 COMMIT `docs(catalog): record search and product page use cases as implemented` · hash: `a8f1c15`
- [x] **C08.05** Mở PR vào `develop` — PR #86

---

## Chốt plan — tick tiến độ use case

Làm **ngay khi xong plan** (hoặc xong từng use case),
> 2026-11-18: 7 UC đã chuyển 🟡 Đang triển khai trong checklist (BE / Test / Nối); 7 dòng UC dưới đây tick khi **Chạy** xong (C07.08) → ✅.
> trong file [`docs/Document/CHECKLIST_TIEN_DO_UC.md`](../../../docs/Document/CHECKLIST_TIEN_DO_UC.md) — cách tick xem mục 0 của file đó.

- [x] `UC-CAT-02` Tìm kiếm sản phẩm theo từ khóa (sau C02 + C07)
- [x] `UC-CAT-04` Gợi ý từ khóa tìm kiếm & Xu hướng (sau C03 + C07)
- [ ] `UC-CAT-05` Lọc nâng cao & Sắp xếp sản phẩm đa tiêu chí — Chạy `◐` 2026-11-21 (sau C02 + C07; `◐` nếu `has_shop_voucher` / `discount_min` còn chờ module khác — D8, D10)
- [x] `UC-CAT-06` Xem chi tiết sản phẩm, ma trận biến thể SKU & Tồn kho (sau C04 + C07)
- [x] `UC-CAT-07` Xem gợi ý sản phẩm tương tự (sau C05 + C07)
- [x] `UC-CAT-16` Xem trang gian hàng (sau C06 + C07)
- [x] `UC-CAT-18` Chia sẻ sản phẩm bằng liên kết — Deeplink (sau C04 lượt xem + C07.04–C07.05)
- [x] Cập nhật số ở mục 1 *Tổng quan* của checklist
- [x] Thêm một dòng vào mục 5 *Nhật ký cập nhật* của checklist (ngày · use case · mã commit / PR)
- [x] Đổi cột *Hiện trạng mã nguồn* của 7 use case trong `Quản Lý UseCase.xlsx`

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
| C04.01 | `VariantMatrix` đặt ở `catalog/product/domain` (hàm thuần). Thứ tự nhóm theo khóa của `attributes_json` — JSONB tự sắp khóa (ngắn trước) nên có thể khác thứ tự người bán nhập; khi có UC-CAT-09/15 nên lấy theo `category_attributes.is_variant_axis` + `display_order` |
| C04.02 | Đọc bằng `JdbcTemplate` (không JPA) cho đồng bộ với tìm kiếm và tránh N+1. Shop tạm nghỉ tính lúc trả (không theo cache). `description_html` trả nguyên văn — **phải lọc HTML khi lưu** ở UC-CAT-09 (XSS), web hiển thị cũng cần lọc. `evict(id)` sẵn cho plan sản phẩm người bán gọi khi sửa / ẩn |
| C04.03 | HTTP `Cache-Control: no-cache` + ETag thay vì `max-age=300` (API 0.12) — trình duyệt / CDN luôn hỏi lại để tồn không cũ (D11); cache 5 phút nằm phía máy chủ |
| C04.05 | `source` sai → 422; sản phẩm không còn bán → 404. Web gửi bằng `fetch(..., {keepalive: true})` (JSON) thay `sendBeacon` vì `sendBeacon` kiểu `application/json` bị chặn CORS preflight khi khác origin |
| C05.01 | Bước bù lấy danh mục cha + anh em **trừ danh mục lá** (nếu gồm cả lá thì sản phẩm cùng lá ngoài ±50% giá lọt vào — test bắt được). `/similar` chạy cả khi sản phẩm gốc đã ẩn (trang "không còn bán" cần — UC-CAT-18); sản phẩm không tồn tại → `[]`. Trọng số qua `genie.catalog.similar.weight-*` (mặc định 0,5 / 0,3 / 0,2) |
| C05.02 | Tập nguồn dòng chung tối đa 240 (= 8 trang × 30, khớp giao diện "Trang 2/8"), cache 30 phút theo ngày. Có token → `Cache-Control: private, no-cache` |
| C06.01 | **Chưa phối hợp** — cần báo Nguyễn Ngọc Thiện (chủ plan Mở shop & KYC): đã thêm `vendor/shop/api/PublicShopController` + `vendor/shop/application/PublicShopService` (chỉ đọc). Khi plan đó có `ShopFacade` / sửa thông tin shop (UC-VND-21) thì gọi `PublicShopService.evict(shopId)` |
| C06.02 | `product_count` đếm sản phẩm `PUBLISHED` trực tiếp (cột `active_product_count` chưa có sự kiện nào cập nhật). `GET /public/shops?featured=true` trả thêm `logo_url` (API 5.12 chưa có trường này). Cache 10 phút, tạm nghỉ tính lúc trả. Thêm mã `GNE-VND-4041` (API 15 + web + i18n, commit `4fc1468`) |
| C07.02 | Cờ nối thật theo nhóm: `PUBLIC_REAL_API=product,search,shop` (plan ghi `catalog,search,shop` — đổi vì nhóm `catalog` là cây danh mục UC-CAT-01, chưa có backend). URL / UI vẫn lọc theo **tên** thương hiệu, nơi bán; tầng `infrastructure` gọi `include_facets=true` của cùng phạm vi để đổi tên → id (tên không có → id `-1` = rỗng). `listProductsByIds` gọi chi tiết song song (API không lọc theo id). Thêm commit backend `ff452e1`: `shop.is_mall` trên thẻ, `province_name` ở trang shop |
| C07.03 | Trang `/search`, `/c/[slug]` không đổi (bộ lọc đã theo URL params từ G08.17). Giới hạn: API 5.3 chỉ nhận **một** `category_id` → chọn nhiều danh mục gốc thì lấy danh mục đầu. Danh mục trên web vẫn là mock (UC-CAT-01 chưa làm) nên `category_id` từ slug mock chưa khớp CSDL thật — lọc danh mục chỉ đúng khi UC-CAT-01 nối thật |
| C07.04 | BuyBox giá / tồn / khóa lựa chọn theo SKU thật (`stockFor`, `isOptionOut`, `priceOf` theo SKU) — mock giữ luật cũ. `?v=` đọc `variant_id` (UC-CAT-18), vẫn đọc định dạng cũ `0-1`. `src` đọc lúc SSR (BuyBox xóa khỏi URL khi hydrate) → `trackProductView` gửi `fetch keepalive` kèm token nếu có. Trang "không còn bán" gọi `/similar` của chính sản phẩm đó |
| C07.06 | Trang shop: tab danh mục lấy `shop.categories` (API 5.11), nhãn thời gian phản hồi từ `avg_response_minutes` qua khóa i18n `shop.head.response_minutes/hours`. Mock "không tìm thấy shop" đổi `GNE-CAT-4041` → `GNE-VND-4041`. Phí ship / ngưỡng freeship không có ở API trang shop (tính ở checkout) |
| C07.07 | Vitest mới: `slug.test.ts` (chung bộ mẫu với `SlugUtilTest`), `product.sku.rules.test.ts` (giá / tồn / khóa lựa chọn / deeplink / nguồn lượt xem — cùng ca với `VariantMatrixTest`), `suggest.mapper.test.ts`, OG giá trong `seo.test.ts` |
| C07.08 | **Chưa chạy — để người phụ trách tự chạy** (CLAUDE.md: chỉ tick Chạy khi người dùng tự thao tác). Máy dev chưa có `.env` / container Genie. Cách chạy: `cp .env.example .env` (điền khóa) → `docker compose --env-file .env -f deploy/docker-compose.local.yml up -d` → `cd server-spring && ./mvnw spring-boot:run` → chèn vài shop VERIFIED + sản phẩm PUBLISHED (plan seed S02–S04 hoặc tay) → `cd web && PUBLIC_REAL_API=product,search,shop npm run dev` → thử tìm có dấu / không dấu / gõ sai, lọc + sắp, dropdown gợi ý, chi tiết chọn SKU hết hàng, tương tự, trang shop, dán link chia sẻ vào Zalo / Facebook Sharing Debugger |
| C07.09 | `npm ci` lại (node_modules thiếu `@types/node` dù đã khai trong lock — làm `astro check` báo 7 lỗi cũ). Sau đó: eslint 0 lỗi · `astro check` + `tsc` 0 lỗi · Vitest 68 file / 328 test xanh · build OK · i18n 3267 khóa OK · Playwright `guest` + `seo` 22/22 xanh (2 khổ màn hình) |
| C08.02 | Thêm `CatalogOpenApiIT` (commit `a545337`): `/v3/api-docs` có đủ 10 endpoint, summary chứa đủ 7 mã UC |
| C08.04 | Checklist: 7 UC → 🟡 Đang triển khai (BE / Test / Nối ☑, Chạy ☐; UC-CAT-05 BE / Nối `◐` — chờ voucher shop + UC-CAT-01), Tổng quan 5 → 12 đang triển khai, 67 → 60 chưa có backend, phân hệ 02 = 0 / 7 / 10, mục 3 thêm việc còn lại của cụm. Excel: cột S = "Đang triển khai" + ghi chú U, sheet Thành viên. CLAUDE.md mục 2 + 9 |
| C08.05 | **Chưa làm** — theo quy trình repo chỉ push / viết nội dung PR khi người phụ trách yêu cầu. 4 nhánh xếp chồng: `feature/catalog-search` → `feature/catalog-product-detail` → `feature/catalog-shop-page` → `feature/web-catalog-real-api` (mỗi nhánh tách từ nhánh trước) — mở PR vào `develop` theo đúng thứ tự, hoặc gộp một PR từ nhánh cuối |
| C07.08 (kiểm 2026-11-19) | Đã chạy **tự động** hai phía trên máy dev (docker local + backend `local` + web `PUBLIC_REAL_API=auth,profile,security,upload,product,search,shop`, dữ liệu mẫu 2 shop / 7 sản phẩm): API 37/37 và trang SSR 13/13 đạt cho đủ 7 UC; job ghi gộp lượt xem / từ khóa ghi đúng xuống CSDL. Tìm ra và sửa 3 lỗi: ngưỡng trigram 0,3 khớp nhầm "ao thun" ↔ "…thể thao…" → 0,4; `sort` bị bỏ qua khi < 5 kết quả (`c1fc0dd`); xu hướng hiện 2 dòng "áo thun" / "ao thun" → gộp (`0e6277e`); `<img src="">` khi sản phẩm chưa có ảnh (`cfe2b87`). Backend 218 test xanh, web 328 test + build xanh. **Ô Chạy vẫn chờ người phụ trách tự thao tác trên giao diện** |

## Nhật ký

| Ngày | Việc đã làm | Commit |
|---|---|---|
| 2026-11-17 | Lập plan + progress (54 task, 12 commit, 9 giai đoạn; stack + thuật toán A1–A13) — chưa bắt đầu làm | — |
| 2026-11-18 | Phân công Nghiêm Phú Đăng Quân; ghi "đã lập plan `plan/2026-11-17`" + cột Phụ trách cho 7 UC trong `CHECKLIST_TIEN_DO_UC.md` (bảng Thành viên, mục 5 Nhật ký) và cột *Người phụ trách* / sheet *Thành viên* trong `Quản Lý UseCase.xlsx` — trạng thái vẫn ⬜ Chưa triển khai | — |
| 2026-11-18 | C00 chuẩn bị: nhánh `feature/catalog-search`, chốt D1–D12 theo đề xuất, đăng ký `V21`; commit plan + checklist | `04b866d` |
| 2026-11-18 | C01 nền: `V21` (trọng số FTS, trigram bỏ dấu, index giá), `JsonCache`, ETag `/public`, `PageResponse`, normalizer + slug + `TestDataBuilder` | `4f808a9` `86e5ddc` `08c76cd` |
| 2026-11-18 | C02 tìm kiếm & lọc: `GET /public/products` (FTS + trigram, lọc đa tiêu chí, facet, rate limit 60/phút); 7 unit + 16 IT xanh; đo hiệu năng p95 50 ms | `a45aedb` `fa33542` |
| 2026-11-18 | C03 gợi ý & xu hướng: `/public/search/suggestions`, `/public/search/trending`, bộ đếm Redis ZSET + job ghi gộp 5 phút; 9 IT xanh | `ac22d10` |
| 2026-11-18 | C04 chi tiết & lượt xem: `/public/products/{id}`, `/{id}/variants`, `POST /{id}/views` + job ghi gộp; 5 unit + 8 IT xanh | `71e8533` `56d938e` |
| 2026-11-18 | C05 tương tự & gợi ý hôm nay: `/{id}/similar` (ứng viên + chấm điểm + bù anh em), `/recommendations` (seed theo ngày, xen kẽ 3 : 2); 3 unit + 6 IT xanh | `f0c594a` |
| 2026-11-18 | C06 trang shop: `/public/shops/{id}`, `/public/shops?featured=true`, mã `GNE-VND-4041`; 6 IT xanh; `./mvnw verify` toàn bộ 211 test xanh (1 bỏ qua = test hiệu năng) | `a2c33db` `4fc1468` |
| 2026-11-18 | C07 nối web: sản phẩm / gợi ý / shop gọi API thật sau cờ `PUBLIC_REAL_API`, BuyBox theo SKU, deeplink `?v={variant_id}&src=share`, OG giá, ghi lượt xem; verify + Playwright 22/22 xanh. C07.08 chạy thật để người phụ trách làm | `ff452e1` `d8e824d` `cafbf78` |
| 2026-11-18 | C08 hoàn thiện: `catalog-search.http`, `CatalogOpenApiIT`, Usecase / API_SPEC / LLD / handoff, checklist + Excel + CLAUDE.md. Còn C07.08 (chạy thật) và C08.05 (PR) | `a545337` `a8f1c15` |
| 2026-11-19 | Kiểm lại 7 UC: chạy thật tự động hai phía (API 37/37, trang SSR 13/13), toàn bộ test backend 218 + web 328 xanh; sửa 3 lỗi phát hiện khi chạy thật (ngưỡng trigram + sort khi ít kết quả, gộp xu hướng có / không dấu, ảnh rỗng) | `c1fc0dd` `0e6277e` `cfe2b87` |
| 2026-11-19 | Gộp `develop` mới nhất (migration tìm kiếm `V18` → `V21`, bỏ mã lỗi trùng, gộp `TestDataBuilder` / `shop.*`, tính lại checklist), một quy tắc slug sản phẩm cho tìm kiếm + wishlist; backend 417 test + web 333 test + Playwright 22/22 xanh; mở **PR #86** vào `develop` — CI backend / web / commitlint xanh | `e8ad436` `477dccf` · PR #86 |
| 2026-11-21 | C07.08 chạy thật hai phía theo yêu cầu người phụ trách: API + trình duyệt Chromium (tìm có dấu / không dấu / gõ sai, xu hướng + gợi ý, sắp xếp 4 kiểu + lọc, SKU hết hàng, 301 / 404, tương tự, trang shop, deeplink `?v=&src=share` + OG, lượt xem xuống `product_view_stats`). Sửa lỗi ô giá "Từ" tự gửi form khi bấm sang ô "Đến" (`FilterPanel.tsx`). Phát hiện ETag `/public/**` đổi mỗi lần (thân có `meta.request_id`) → không bao giờ 304, chưa sửa. 6 UC ✅, UC-CAT-05 Chạy ◐ | (chưa commit) |
| 2026-11-24 | Kiểm lại (Nguyễn Ngọc Thiện): `./mvnw verify` xanh (toàn backend 177 unit + 292 IT, mọi test của cụm đạt); `SearchPerformanceIT -Dperf=true` p50 27 ms / p95 51 ms; chạy `catalog-search.http` + SSR trên backend thật 40 / 41 đúng — chỉ còn ETag `/public/**` không `304` (đã ghi nhận); lọc danh mục `/c/{slug}` + `?sub=` nay lấy dữ liệu thật (cây UC-CAT-01 đã nối) | — |
