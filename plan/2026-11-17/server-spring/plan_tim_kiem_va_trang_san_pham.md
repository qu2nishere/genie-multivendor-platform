# PLAN — Tìm kiếm, lọc, trang sản phẩm, trang shop & chia sẻ (cụm khám phá sản phẩm)

| | |
|---|---|
| **Ngày lập** | 2026-11-17 |
| **Thể loại** | server-spring (module `catalog`: feature `search`, `product`, `stats`; module `vendor`: trang shop công khai) + nối `web/` (feature `search`, `product`, `catalog`, `shop`) |
| **Use case** | Tìm kiếm sản phẩm theo từ khóa (UC-CAT-02) · Gợi ý từ khóa tìm kiếm & Xu hướng (UC-CAT-04) · Lọc nâng cao & Sắp xếp sản phẩm đa tiêu chí (UC-CAT-05) · Xem chi tiết sản phẩm, ma trận biến thể SKU & Tồn kho (UC-CAT-06) · Xem gợi ý sản phẩm tương tự (UC-CAT-07) · Xem trang gian hàng (UC-CAT-16) · Chia sẻ sản phẩm bằng liên kết — Deeplink (UC-CAT-18) |
| **Tài liệu chuẩn** | UC phân hệ 02 (UC-CAT-02/04/05/06/07/16/18) · API 0.4, 0.11, 0.12, `$ProductCard`, 5.3–5.11 · ENTITY 2.2 (`Product`), 2.3 (`ProductVariant`), 2.4 (`ProductImage`), 2.7 (`Brand`), 2.11 (`SearchKeywordStat`), 2.12 (`ProductViewStat`), 1.4 (`VendorProfile`), 7.3 (`VendorShippingConfig`), 13.1 (index) · LLD 5 (index), 10 (cron), 11.2 (rate limit) · TS 4, 8, 10, 14 — viết tắt xem [README 2026-11-16](../../2026-11-16/README.md) |
| **Phụ thuộc** | Seed sản phẩm / shop / SKU: plan [seed](../../2026-11-16/db/plan_seed_du_lieu_mau.md) S02–S04 (test tự động **không** phụ thuộc seed). `ShopFacade` của plan [Mở shop & KYC](../../2026-11-16/server-spring/plan_mo_shop_kyc.md) — chưa có thì dùng cổng đảo ngược (D6). Flash Sale (UC-PRO-06) chưa có → `flash_sale = null` |
| **Theo dõi tiến độ** | [progress_tim_kiem_va_trang_san_pham.md](./progress_tim_kiem_va_trang_san_pham.md) — cùng mã task `Cxx.yy` |
| **Phụ trách** | Nghiêm Phú Đăng Quân — lập plan và thực hiện (phân công 2026-11-18; xem bảng Thành viên trong `docs/Document/CHECKLIST_TIEN_DO_UC.md`) |
| **Người commit** | danh tính git đã cấu hình trong repo — không `--author`, không `Co-Authored-By` |

---

## 0. Hiện trạng đã khảo sát

| Phần | Có gì | Thiếu |
|---|---|---|
| CSDL `V1`, `V2` | Extension `unaccent`, `pg_trgm`; hàm `immutable_unaccent(text)`; cột sinh `products.search_vector` = `to_tsvector('simple', immutable_unaccent(name ‖ brand ‖ description))` STORED; index `GIN(search_vector)`, `GIN(name gin_trgm_ops)`, `(category_id, status, created_at DESC)`, `(vendor_id, status)`, `GIN(specs_json jsonb_path_ops)`; bảng `search_keyword_stats` (`UNIQUE(normalized_keyword, stat_date)`), `product_view_stats` (`UNIQUE(product_id, stat_date, source)`) | Trọng số trong `search_vector` (tên quan trọng hơn mô tả); index trigram trên tên **đã bỏ dấu**; index cho tiền tố từ khóa; index giá theo SKU (D3, D4) |
| `products` | `sold`, `rating`, `view_count`, `base_price`, `brand_id`, `is_pre_order`, `ships_from`, `specs_json` | **Không có cột `slug`** (D2), không có `original_price` (D8) |
| `vendor_profiles` | `is_mall`, `rating_avg/count`, `follower_count`, `active_product_count`, `chat_response_rate`, `avg_response_minutes`, `is_favorite`, `vacation_*`, `pickup_province_id` | — (đủ cho trang shop) |
| `catalog/search`, `catalog/stats` | Entity + repository rỗng (`SearchKeywordStat`, `ProductViewStat`, `TrafficSource`) | Toàn bộ `api`, `application` |
| `common` | `RateLimiter` (Redis `INCR`+`EXPIRE`, Redis lỗi thì cho qua) **đã có thật**; `spring-boot-starter-cache`, `data-redis` trong `pom.xml` | `RedisCacheManager` cấu hình TTL theo tên cache; `ShallowEtagHeaderFilter` cho `/public/**` |
| `web/` | Trang `/search`, `/p/[slug]-[id]`, `/shop/[id]`, `/discover` chạy mock; `WishShare.tsx` (nút chia sẻ); `shared/lib/seo.ts` có `og:image` | **5 endpoint lệch spec** (handoff mục 2): `/public/search/suggest`, `/public/feed`, `/public/products/{id}/related`, `/public/products/facets`, trả `string[]` cho trending; thiếu `og:price:amount`; chưa gửi `POST /views` |

---

## 1. STACK CÔNG NGHỆ CHỐT CHO CỤM NÀY

> Không thêm hạ tầng mới. Toàn bộ chạy trên PostgreSQL 16 + Redis 7 đã có (TS mục 12; UC-CAT-04 ghi rõ TLCN **không** dùng Elasticsearch / Redis Trie / Qdrant).

### 1.1 Backend (`server-spring/`)

| Lớp | Công nghệ | Dùng cho |
|---|---|---|
| Tìm kiếm toàn văn | **PostgreSQL Full-Text Search**: `tsvector` / `tsquery`, cấu hình `simple` + `unaccent`, `ts_rank_cd`, index **GIN** | UC-CAT-02 (từ khóa có dấu / không dấu) |
| Tìm gần đúng / gõ sai | **`pg_trgm`**: toán tử `%`, `<%` (`word_similarity`), hàm `similarity()`, index **GIN `gin_trgm_ops`** | UC-CAT-02 bù kết quả, UC-CAT-04 gợi ý, UC-CAT-07 độ giống tên |
| Lọc thuộc tính | **JSONB** + toán tử `@>` trên `specs_json` (index `jsonb_path_ops`) | UC-CAT-05 `attr.{code}` |
| Truy vấn động | **Spring `NamedParameterJdbcTemplate` + SQL builder tự viết** (`ProductSearchSqlBuilder`) — không dùng JPA Criteria vì cần `ts_rank`, `similarity`, CTE, `GROUP BY` facet | UC-CAT-02/05/16 |
| Chi tiết sản phẩm | **Spring Data JPA** (đọc entity) + nạp theo lô (`findAllByProductIdIn`) chống N+1 | UC-CAT-06 |
| Cache dữ liệu | **Spring Cache + Redis (`RedisCacheManager`)**, TTL theo tên cache, khóa có tiền tố `cache:` | 5.6 (5 phút), tương tự (30 phút), trending (10 phút), shop stats (10 phút) |
| Cache HTTP | **`Cache-Control: public, max-age`** + **ETag** (`ShallowEtagHeaderFilter`) → `304` cho `GET /public/**` | API 0.12 |
| Đếm thời gian thực | **Redis**: `ZINCRBY` (Sorted Set) đếm từ khóa, `HINCRBY` (Hash) đếm lượt xem, **HyperLogLog** `PFADD/PFCOUNT` đếm khách duy nhất, `SET NX EX` chống đếm trùng | UC-CAT-04, UC-CAT-06 bước 9, UC-CAT-18 |
| Ghi gộp định kỳ | **`@Scheduled` (5 phút)** + khóa Redis `SET NX` (một instance chạy) + `UPSERT … ON CONFLICT DO UPDATE` | `search_keyword_stats`, `product_view_stats`, `products.view_count` |
| Giới hạn tần suất | **`RateLimiter` có sẵn** (Redis fixed window `INCR`+`EXPIRE`) | 60 / phút / IP cho `GET /public/products`, `/public/search/suggestions` |
| Migration | **Flyway** `V18__catalog_search_tuning.sql` (D3, D4) | trọng số FTS, index trigram bỏ dấu, index giá |
| Tài liệu API | **springdoc-openapi** `@Operation(summary = "UC-CAT-0x …")` | |
| Test | **JUnit 5 + Testcontainers** (`postgres:16` — có sẵn `unaccent`, `pg_trgm`; Redis qua `GenericContainer("redis:7-alpine")`), **ArchUnit** | `*IT`, đo số truy vấn (chống N+1) |

### 1.2 Frontend (`web/`) — giữ nguyên stack hiện có

| Lớp | Công nghệ | Dùng cho |
|---|---|---|
| Trang công khai | **Astro 5 SSR** (frontmatter gọi `server-http.ts`) — HTML có sẵn nội dung cho SEO | `/search`, `/c/[slug]`, `/p/[slug]-[id]`, `/shop/[id]` |
| Tương tác | **React 19 islands** + **TanStack Query** (`keepPreviousData` khi đổi bộ lọc) | lưới kết quả, bộ lọc, chọn SKU, dropdown gợi ý |
| Trạng thái bộ lọc | **URL search params** là nguồn sự thật (`history.replaceState`) — chia sẻ / F5 giữ bộ lọc | UC-CAT-05 bước 3 |
| Gõ gợi ý | **Debounce 250 ms** + **`AbortController`** hủy request cũ + cache theo chuỗi | UC-CAT-04 |
| Chia sẻ | **Web Share API** (`navigator.share`) → dự phòng **Clipboard API** (`navigator.clipboard.writeText`) | UC-CAT-18 |
| Xem trước liên kết | **Open Graph** (`og:title`, `og:image`, `og:price:amount`, `og:price:currency`) + **JSON-LD Product / BreadcrumbList** + `<link rel="canonical">` | UC-CAT-18 bước 4, R14–R19 |
| Ghi lượt xem | **`navigator.sendBeacon`** (dự phòng `fetch keepalive`) gọi `POST /public/products/{id}/views` | UC-CAT-06 bước 9 |

---

## 2. THUẬT TOÁN & KỸ THUẬT TÌM KIẾM (chi tiết)

### A1. Chuẩn hóa từ khóa (dùng chung UC-CAT-02, 04)

```
normalize(q):
  1. trim, cắt tối đa 100 ký tự
  2. Unicode NFC → lowercase (Locale.ROOT)
  3. thay ký tự không phải chữ / số / khoảng trắng bằng ' '   (giữ chữ có dấu tiếng Việt)
  4. gộp nhiều khoảng trắng thành 1
  5. bản "bỏ dấu" để lưu thống kê = unaccent(chuỗi) — đ/Đ → d
```
- Cài trong `catalog/search/domain/KeywordNormalizer` (hàm thuần, có test). Bỏ dấu phía Java bằng `java.text.Normalizer` (NFD + bỏ `\p{M}` + thay `đ`) — **phải cho kết quả giống** `unaccent()` của PostgreSQL (test so khớp 30 chuỗi mẫu).
- Rỗng sau chuẩn hóa → luồng 2a (trả sản phẩm mới nhất, không đếm thống kê).

### A2. Tìm kiếm toàn văn + bù gõ sai (UC-CAT-02) — thuật toán 2 tầng

**Tầng 1 — Full-text (chính xác, nhanh):**
```sql
WITH q AS (SELECT websearch_to_tsquery('simple', immutable_unaccent(:q)) AS tsq,
                  to_tsquery('simple', :prefixQuery) AS tsq_prefix)   -- vd 'tai & nghe & chong:*'
SELECT p.id, ts_rank_cd(p.search_vector, q.tsq) AS rank
FROM products p, q
WHERE p.status = 'PUBLISHED'
  AND (p.search_vector @@ q.tsq OR p.search_vector @@ q.tsq_prefix)
  AND <bộ lọc A4>
ORDER BY rank DESC, p.sold DESC NULLS LAST, p.id DESC
LIMIT :size OFFSET :offset;
```
- `websearch_to_tsquery` chịu được cú pháp người dùng (dấu ngoặc kép, `-` loại trừ) không ném lỗi như `to_tsquery`.
- **Từ cuối gắn `:*` (prefix match)** để "tai ngh" vẫn ra "tai nghe" — `prefixQuery` dựng ở Java, mỗi token đã escape ký tự đặc biệt của tsquery.
- **Xếp hạng:** `ts_rank_cd` (cover density — ưu tiên các từ đứng gần nhau) trên vector có **trọng số** (D3): tên `A`, thương hiệu `B`, mô tả `D`.

**Tầng 2 — Trigram fallback (chịu gõ sai):** chỉ chạy khi tầng 1 trả **< 5** kết quả ở trang đầu:
```sql
SELECT p.id, word_similarity(:qUnaccent, immutable_unaccent(lower(p.name))) AS sim
FROM products p
WHERE p.status = 'PUBLISHED'
  AND :qUnaccent <% immutable_unaccent(lower(p.name))     -- dùng index GIN trigram bỏ dấu (D3)
  AND p.id NOT IN (:idsTầng1)  AND <bộ lọc A4>
ORDER BY sim DESC LIMIT :need;
```
- Ngưỡng: `SET LOCAL pg_trgm.word_similarity_threshold = 0.3` (khớp UC "`similarity > 0.3`"; dùng `word_similarity` vì so một cụm ngắn với tên dài cho điểm hợp lý hơn `similarity`).
- Gộp: kết quả tầng 1 đứng trước, tầng 2 nối sau; `total_items` = tầng 1 + tầng 2 (chỉ khi có fallback).
- Không có kết quả cả hai tầng → `[]` + client hiện "Không tìm thấy" + gọi 5.5 làm "Có thể bạn quan tâm" (luồng 3a).

### A3. Sắp xếp (UC-CAT-05 bước 2)

| `sort` | Biểu thức `ORDER BY` (luôn kèm `p.id DESC` để ổn định phân trang) |
|---|---|
| `RELEVANCE` (mặc định khi có `q`) | `rank DESC, sold DESC` |
| `NEWEST` (mặc định khi không có `q`) | `created_at DESC` |
| `BEST_SELLING` | `sold DESC NULLS LAST` |
| `PRICE_ASC` / `PRICE_DESC` | `min_price ASC/DESC` — `min_price` = giá thấp nhất SKU còn bán (subquery `LATERAL` / CTE `MIN(price) … WHERE deleted_at IS NULL`) |
| "Phổ biến nhất" (UC) | ánh xạ vào `RELEVANCE` nếu có `q`, ngược lại `BEST_SELLING` — ghi vào D9 |

### A4. Lọc đa tiêu chí — SQL builder động (UC-CAT-05, UC-CAT-16)

Mỗi tham số thêm **một mệnh đề `AND`** + tham số đặt tên (không nối chuỗi giá trị — chống SQL injection):

| Tham số | Mệnh đề |
|---|---|
| `category_id` | `p.category_id IN (SELECT id FROM category_subtree(:cid))` — **CTE đệ quy** `WITH RECURSIVE` lấy cả danh mục con (cache cây danh mục trong Redis, đổi thành mảng ID trước khi truy vấn) |
| `shop_id` | `p.vendor_id = :vendorId` (đổi `shop_id → vendor_id` qua cổng D6) |
| `brand_ids` | `p.brand_id = ANY(:brandIds)` |
| `province_ids` | `vp.pickup_province_id = ANY(:provinceIds)` |
| `price_min/max` | `min_price BETWEEN :min AND :max` |
| `rating_min` | `p.rating >= :ratingMin` |
| `is_mall` | `vp.is_mall = TRUE` ("Chính hãng" = `vp.is_mall OR b.is_verified`) |
| `free_shipping` | `vsc.freeship_threshold IS NOT NULL` |
| `has_shop_voucher` | `EXISTS (…)` qua cổng `ShopVoucherAvailability` (module `promotion`) — chưa có → bỏ qua tham số (D10) |
| `attr.{code}` | `p.specs_json @> :jsonFragment` — chỉ khi đúng 1 danh mục; nhiều giá trị cùng mã → `OR` |
| `discount_min` | bỏ qua tới khi có Flash Sale / giá gốc (D8) |

**Facet (bộ đếm sidebar — `include_facets=true`):** kỹ thuật **disjunctive faceting** — đếm facet *thương hiệu* trên tập đã áp mọi bộ lọc **trừ** chính bộ lọc thương hiệu (để chọn Sony vẫn thấy số của Apple); tương tự cho *nơi bán*, *thuộc tính*. Mỗi facet một câu `GROUP BY` trên CTE gốc, chạy trong cùng request, giới hạn top 20.

**Phân trang:** `page/size` (API 5.3), `size ≤ 60`, **chặn trang sâu** `page ≤ 50` (OFFSET lớn rất chậm với `ORDER BY rank`). `total_items` bằng `COUNT(*)` trên CTE (bỏ khi > 10.000 → trả `10000+`).

**Đọc chéo module:** câu truy vấn đọc (read-model) `JOIN vendor_profiles vp`, `LEFT JOIN vendor_shipping_config vsc`, `LEFT JOIN brands b` bằng SQL thuần — xem D6.

### A5. Gợi ý khi gõ — Autocomplete (UC-CAT-04, API 5.4)

Ba nguồn chạy song song (`CompletableFuture` hoặc 1 câu `UNION ALL`), hợp nhất:
| Nguồn | Truy vấn | Số lượng |
|---|---|---|
| Danh mục | `immutable_unaccent(lower(name)) LIKE :prefix || '%'` trước, sau đó `%` trigram — danh mục ít, lọc trong bộ nhớ từ cây đã cache | 2 |
| Shop | `vendor_profiles` `VERIFIED`, tên bỏ dấu khớp tiền tố / trigram | 2 |
| Sản phẩm | `PUBLISHED`, `word_similarity` trên tên bỏ dấu (index D3), xếp `sim DESC, sold DESC` | 5 |
- Ngưỡng 2 ký tự (client chặn + server trả rỗng), rate limit 60/phút/IP, `Cache-Control: max-age=60`.
- Từ khóa phổ biến khớp tiền tố (UC bước 3): lấy từ **Redis ZSET trending** (A6) lọc `startsWith(prefix)` — không cần truy vấn DB.

### A6. Xu hướng tìm kiếm — Trending (UC-CAT-04, API 5.4b)

```
Mỗi lượt tìm hợp lệ (q không rỗng, có ≥ 1 kết quả):
  key chống spam  = "search:dedupe:{ipHash}:{kw}"  SET NX EX 600   → đã có thì không đếm
  ZINCRBY  "search:kw:{yyyyMMdd}"  1  {kw đã chuẩn hóa}            (EXPIRE 8 ngày)
  HINCRBY  "search:kw:pending"     {yyyyMMdd|kw}  1                 (chờ ghi DB)

GET /public/search/trending?limit=5   (cache 10 phút)
  ZUNIONSTORE tmp  N  search:kw:{hôm nay} … search:kw:{N-1 ngày trước}   (N theo D5)
  ZREVRANGE tmp 0 (limit*3)   → loại từ trong blocklist (Redis SET "search:kw:blocked",
                                đồng bộ từ search_keyword_stats.is_blocked) → lấy limit
  Redis trống (khởi động lại) → dự phòng SQL: SUM(search_count) GROUP BY normalized_keyword
                                 WHERE stat_date >= now()-N AND NOT is_blocked
```
- **Thuật toán xếp hạng:** đếm tần suất trong **cửa sổ trượt N ngày** (top-K bằng Sorted Set — O(log n) mỗi lần tăng). Có thể mở rộng thành điểm suy giảm theo thời gian `score = Σ count_d · 0.8^(tuổi ngày)` bằng `ZUNIONSTORE … WEIGHTS` — ghi là tùy chọn (D5).
- **Job ghi gộp 5 phút** (`SearchKeywordFlushJob`): `RENAME search:kw:pending → …:flushing` (nguyên tử) → `HGETALL` → `INSERT … ON CONFLICT (normalized_keyword, stat_date) DO UPDATE SET search_count = search_keyword_stats.search_count + EXCLUDED.search_count` (đúng `SearchKeywordStat.accumulate()`) → `DEL`. Lỗi DB thì giữ khóa `flushing` để lần sau ghi lại.

### A7. Ma trận biến thể SKU & tồn kho (UC-CAT-06, API 5.6)

- **Dựng `option_groups`** từ `product_variants.attributes_json` (vd `{"Màu sắc":"Đen","Phiên bản":"Pro"}`): giữ thứ tự nhóm theo SKU đầu tiên, thứ tự giá trị theo lần xuất hiện; `image_url` của giá trị lấy từ `products.variant_image_attribute` + ảnh SKU.
- **`variants[].options`** = mảng giá trị theo đúng thứ tự nhóm → client dựng **bảng tra `Map<options.join('|'), variant>`** (O(1) khi người dùng chọn tổ hợp).
- **Khóa nút không khả dụng phía client:** với mỗi giá trị chưa chọn, kiểm tra có tồn tại SKU khớp các lựa chọn hiện tại + giá trị đó và `stock > 0` không → không có thì làm mờ (thuật toán lọc chéo, n nhóm × m giá trị, dữ liệu nhỏ).
- **Tồn khả dụng** = `stock_count − reserved_count` (≥ 0), SKU `deleted_at IS NULL`. Nhãn: `0` "Hết hàng" · `≤ 5` "Sắp hết" · `≤ 10` "Còn N sản phẩm".
- **Cache 5 phút** (API 0.12) cho phần tĩnh; vì tồn đổi liên tục nên **tách tồn ra khỏi cache**: cache khối `ProductDetailView` (không có `stock`), mỗi request đọc `stock` của các SKU bằng 1 câu SQL nhẹ theo `product_id` (D11). Giỏ / đặt hàng luôn đọc DB.
- Xóa cache khi có `ProductUpdatedEvent` / ẩn sản phẩm (`@CacheEvict` trong listener `AFTER_COMMIT`).

### A8. Sản phẩm tương tự (UC-CAT-07, API 5.9) — content-based, không AI

**Bước 1 — Sinh ứng viên (candidate generation):**
```
cùng category_id lá · status = PUBLISHED · id ≠ :id · có SKU stock > 0
· min_price BETWEEN 0.5·giá_gốc AND 1.5·giá_gốc · shop không tạm nghỉ
· LIMIT 200 ứng viên (dùng index (category_id, status, created_at))
```
**Bước 2 — Chấm điểm (weighted scoring), tính trong SQL:**
```
score = 0.5 · similarity(unaccent(tên ứng viên), unaccent(tên gốc))          -- pg_trgm, 0..1
      + 0.3 · ln(1 + sold) / ln(1 + max_sold_trong_tập_ứng_viên)               -- chuẩn hóa log
      + 0.2 · coalesce(rating, 0) / 5
ORDER BY score DESC LIMIT :limit
```
**Bước 3 — Bù khi thiếu:** < `limit` → mở rộng sang **danh mục cha** (bỏ điều kiện giá), vẫn thiếu → trả ít hơn; rỗng → client ẩn khối.
**Cache:** Redis `similar:{productId}:{limit}` TTL 30 phút (Spring Cache `catalog.similar`).
Trọng số `0.5 / 0.3 / 0.2` để trong `application.yml` (`genie.catalog.similar.weights`) để chỉnh khi demo.

### A9. "Gợi ý cho bạn / Gợi ý hôm nay" (UC-CAT-07 bước 5, API 5.5)

- **Khách chưa đăng nhập:** trộn *bán chạy* và *mới* theo **thứ tự ngẫu nhiên có seed theo ngày** để phân trang ổn định trong ngày:
  `ORDER BY md5(p.id::text || :yyyyMMdd)` (cùng ngày → cùng thứ tự; sang ngày mới đổi). Tập nguồn: top 300 `sold` ∪ 300 mới nhất (CTE).
- **Khách đã đăng nhập:** danh mục xem gần đây lấy từ Redis `LIST recent:cat:{userId}` (ghi khi gọi `POST /views` có token, `LPUSH` + `LTRIM 0 19`, TTL 30 ngày). Trộn theo tỉ lệ **60% sản phẩm thuộc 3 danh mục xem nhiều nhất + 40% dòng chung**, xen kẽ (interleave) rồi sắp bằng seed ngày.
- `size` 36 (trang chủ) / 30 (trang Gợi ý hôm nay), tối đa 8 trang (240 sản phẩm).

### A10. Đếm lượt xem theo nguồn (UC-CAT-06 bước 9, UC-CAT-18 bước 5, API 5.7)

```
POST /public/products/{id}/views {source}      → 204, không chặn người dùng
  dedupe: SET NX EX 1800 "pv:seen:{visitorKey}:{id}"  → đã có thì chỉ bỏ qua
          (visitorKey = userId nếu có token, ngược lại SHA-256(ip + user-agent) cắt 16 ký tự)
  HINCRBY "pv:{yyyyMMdd}"  "{id}:{source}"  1
  PFADD   "pv:uv:{yyyyMMdd}:{id}:{source}"  visitorKey        (HyperLogLog ≈ 12KB, sai số ~0,81%)

ProductViewFlushJob (5 phút, khóa SET NX "job:pv-flush"):
  RENAME pv:{ngày} → pv:{ngày}:flushing
  mỗi field: UPSERT product_view_stats (view_count += n, unique_visitor_count = PFCOUNT)
             UPDATE products SET view_count = view_count + n   (gộp theo product_id, 1 câu UPDATE … FROM (VALUES …))
```
- `source` hợp lệ: `SEARCH, HOME_FEED, PROMOTION, SHOP_PAGE, EXTERNAL_LINK`; `src=share` → `EXTERNAL_LINK`; sai → `422`.
- `vendor_id` của dòng thống kê lấy từ `products.vendor_id` khi flush.

### A11. Deeplink & chuyển hướng slug (UC-CAT-18)

- **Slug không lưu DB** (D2): `slug = slugify(name)` — `KeywordNormalizer` bỏ dấu → chữ thường → ký tự khác `[a-z0-9]` thành `-` → gộp `-` → cắt 80 ký tự. Cài một lần ở Java (`SlugUtil`) **và** TS (`shared/lib/slug.ts`), test chéo cùng bộ mẫu.
- URL chuẩn: `https://genie.vn/p/{slug}-{id}?v={variantId}&src=share`. Astro tách `id` = phần sau dấu `-` cuối; **slug khác slug hiện tại → `301`** sang URL chuẩn (giữ `v`, `src`) — R17.
- `v` hợp lệ → island chọn sẵn SKU; không hợp lệ / đã xóa → bỏ qua im lặng.
- Sản phẩm ẩn / xóa → trang SSR mã `404` + nội dung "Sản phẩm không còn bán" + khối 5.9 (`noindex`).
- Không gắn mã giới thiệu / UTM cá nhân (ngoài phạm vi TLCN).

### A12. Trang shop (UC-CAT-16, API 5.11)

- Chỉ hiện shop `kyc_status = VERIFIED` và chủ shop `ACTIVE`; ngược lại `404 GNE-VND-404x` (D7).
- Chỉ số đọc thẳng cột tổng hợp của `vendor_profiles` (đã do sự kiện / job khác cập nhật — không tính mỗi lần mở trang); `categories` của shop = `SELECT DISTINCT category_id` (gốc cấp 1) từ sản phẩm `PUBLISHED`.
- Cache Redis `shop:stats:{id}` TTL 10 phút; `is_online` = có phiên WebSocket / hoạt động trong 5 phút (Redis key `presence:{userId}` — chưa có thì `false`, D7).
- Lưới sản phẩm & "Tìm trong Shop" **dùng lại 5.3** với `shop_id` (A4) — không viết truy vấn riêng.

### A13. Giới hạn tần suất & bảo vệ

- `RateLimiter.check("rl:search:{ip}", 60, 1 phút)` trên 5.3 và 5.4 (LLD 11.2) → `429 GNE-RAT-4290` + `Retry-After`.
- Timeout truy vấn tìm kiếm: `SET LOCAL statement_timeout = '2s'` trong transaction chỉ-đọc → quá thì `503` thay vì treo pool.
- `@Transactional(readOnly = true)` cho mọi service đọc.

---

## 3. Nguyên tắc bắt buộc

| # | Nguyên tắc | Nguồn |
|---|---|---|
| N1 | Package `com.genie.catalog.{search,product,stats}.{api,application,domain}`; trang shop công khai ở `com.genie.vendor.shop.api` (D7) | TS 4.1–4.2 |
| N2 | Mọi endpoint `/api/v1/public/**` không cần token; có token thì dùng thêm (A9) nhưng **không** trả dữ liệu cá nhân trong body có cache | API 0.1, 0.12 |
| N3 | Không nối chuỗi giá trị người dùng vào SQL — chỉ tham số đặt tên; tên cột sắp xếp lấy từ `enum` trắng | TS 9 |
| N4 | Module khác chỉ qua `*Facade` / cổng đảo ngược; ngoại lệ duy nhất: **câu SQL đọc** của tìm kiếm được `JOIN` bảng `vendor_profiles`, `vendor_shipping_config` (D6) | TS 4.4 |
| N5 | Giá là số nguyên VND; ảnh = `PUBLIC_S3_URL` + khóa; JSON snake_case, vỏ `ApiResponse` | API 0.5, 0.6 |
| N6 | Đếm thống kê (từ khóa, lượt xem) **không** ghi DB trong request — chỉ Redis, job gộp 5 phút | UC-CAT-04 b5, UC-CAT-06 b9 |
| N7 | Redis lỗi: đếm thống kê / cache → bỏ qua + log WARN, API vẫn trả dữ liệu từ DB | TS 10 |
| N8 | Test: unit (normalizer, slug, scoring, SQL builder) + `*IT` Testcontainers PG + Redis; có test hiệu năng trên 10.000 sản phẩm seed | TS 14 |

---

## 4. Quyết định cần chốt trước khi làm

| # | Vấn đề | Nguồn lệch | Đề xuất | Chốt |
|---|---|---|---|---|
| D1 | Đường dẫn: UC ghi `GET /api/v1/products/{id}`, `/search/suggest`, `/shops/{vendorId}`; API ghi `/public/products/{id}`, `/public/search/suggestions`, `/public/shops/{id}` | UC vs API | Theo **API** (README mục "đường dẫn lấy theo API"); sửa câu trong Usecase.md ở C09 | ☐ |
| D2 | `products` không có cột `slug` nhưng URL `/p/{slug}-{id}` | ENTITY vs API | **Sinh slug từ `name` khi đọc** (A11), không thêm cột; 301 khi slug đổi. Phương án khác: thêm cột `slug` (V18) — chỉ khi cần slug cố định | ☐ |
| D3 | `search_vector` hiện không có trọng số; trigram chỉ trên `name` có dấu | V2 | `V18`: thay cột sinh bằng `setweight(…name,'A') ‖ setweight(…brand,'B') ‖ setweight(…description,'D')`; thêm `CREATE INDEX idx_products_name_unaccent_trgm ON products USING GIN (immutable_unaccent(lower(name)) gin_trgm_ops)` | ☐ |
| D4 | Lọc / sắp theo giá cần giá thấp nhất SKU | V2 | `V18`: index `product_variants (product_id, price) WHERE deleted_at IS NULL`. Nếu đo > 300 ms trên 10.000 sản phẩm → thêm cột chuẩn hóa `products.min_price` cập nhật khi lưu SKU (để sau, ghi vào Ghi chú) | ☐ |
| D5 | Trending: UC ghi "7 ngày gần nhất, cache 1 giờ"; API ghi "24 giờ qua, cache 10 phút" | UC vs API | Theo **API** (24 giờ = `N = 1` ngày hôm nay + hôm qua có trọng số 0.5); cache 10 phút. Ghi lại Usecase.md | ☐ |
| D6 | Tìm kiếm cần `is_mall`, tỉnh kho, freeship, shop nghỉ (module `vendor`, `shipping`) | TS 4.4 | Câu SQL đọc `JOIN` thẳng bảng (read-model, không import class Java của module khác → ArchUnit vẫn xanh). Đổi `shop_id ↔ vendor_id` + thông tin shop ở chi tiết sản phẩm qua cổng `CatalogShopDirectory` (khai trong `catalog`, mặc định đọc bằng SQL; module `vendor` cài lại khi có `ShopFacade`) | ☐ |
| D7 | Đặt `GET /public/shops/{id}` ở đâu; mã lỗi shop không tồn tại | API 5.11, 15 | Ở `vendor/shop/api/PublicShopController` (phối hợp chủ plan Mở shop & KYC); mã lỗi tra API 15 (`GNE-VND-404x`) — chưa có thì thêm; `is_online = false` tới khi có presence của Chat | ☐ |
| D8 | `original_price`, `discount_percent`, `discount_min` không có nguồn dữ liệu | ENTITY | `original_price = base_price` khi `base_price > price`, ngược lại `null`; khi có Flash Sale thì giá gốc = giá SKU. `discount_min` lọc theo công thức này | ☐ |
| D9 | "Phổ biến nhất" (UC) không có trong enum `sort` của API | UC vs API | Không thêm enum; ánh xạ như A3 | ☐ |
| D10 | `has_shop_voucher` cần module `promotion` | API 5.3 | Cổng `ShopVoucherAvailability.vendorIdsWithActiveVoucher()`; chưa có cài đặt → bộ lọc bị bỏ qua và client ẩn ô lọc (cờ `facets.has_shop_voucher_supported`) | ☐ |
| D11 | Cache 5 phút cho chi tiết sản phẩm làm tồn kho cũ | API 0.12 | Cache phần tĩnh, đọc `stock` riêng mỗi request (A7) | ☐ |
| D12 | Sản phẩm tương tự: UC `limit=9`, API `limit=6` | UC vs API | Mặc định 6, cho phép `limit` 1–12; trang chi tiết gọi `limit=6` theo giao diện | ☐ |

---

## 5. Cây file sẽ tạo / sửa

```
server-spring/src/main/resources/db/migration/
    V18__catalog_search_tuning.sql             (D3, D4 — đăng ký số ở README 2026-11-16 mục 4)
server-spring/src/main/java/com/genie/
├── common/config/CacheConfig.java             RedisCacheManager + TTL theo tên cache
├── common/web/EtagConfig.java                 ShallowEtagHeaderFilter cho /api/v1/public/**
├── common/error/ErrorCode.java                (sửa) + GNE-CAT-4041, 4042, GNE-VND-404x (nếu chưa có)
├── catalog/search/
│   ├── domain/       KeywordNormalizer · SlugUtil · ProductSort (enum) · SearchKeywordStat (sửa: accumulate)
│   ├── application/  ProductSearchService · ProductSearchSqlBuilder · FacetService · SuggestionService
│   │                 TrendingService · SearchKeywordCounter (Redis) · SearchKeywordFlushJob
│   │                 CatalogShopDirectory (cổng D6) · ShopVoucherAvailability (cổng D10)
│   └── api/          PublicProductSearchController (5.3) · PublicSearchController (5.4, 5.4b)
│                     ProductCardResponse · ProductSearchRequest · FacetsResponse · SuggestionsResponse
├── catalog/product/
│   ├── application/  ProductDetailService · VariantMatrixBuilder · SimilarProductService · RecommendationService
│   └── api/          PublicProductController (5.6, 5.9, 5.10) · PublicRecommendationController (5.5)
│                     ProductDetailResponse · VariantResponse
├── catalog/stats/
│   ├── application/  ProductViewCounter (Redis) · ProductViewFlushJob · RecentCategoryTracker
│   └── api/          PublicProductViewController (5.7)
└── vendor/shop/api/  PublicShopController (5.11, 5.12) · PublicShopResponse      (D7)
server-spring/src/test/java/com/genie/catalog/
    KeywordNormalizerTest · SlugUtilTest · VariantMatrixBuilderTest · ProductSearchSqlBuilderTest
    ProductSearchIT · FacetIT · SuggestionIT · TrendingIT · ProductDetailIT · SimilarProductIT
    RecommendationIT · ProductViewIT · PublicShopIT · SearchPerformanceIT (@Tag("perf"))
web/src/
    features/search/infrastructure/suggest.api.ts      (sửa đường dẫn + DTO 3 nhóm)
    features/product/infrastructure/product.api.ts     (sửa: facets gộp, related→similar, feed→recommendations, views)
    features/product/ui/WishShare.tsx                  (Web Share API + clipboard + v/src)
    features/shop/infrastructure/shop.api.ts           (nối thật)
    shared/lib/slug.ts · shared/lib/seo.ts              (slug dùng chung; og:price:*)
    pages/p/[slug]-[id].astro · pages/search.astro · pages/shop/[id].astro   (301 slug, 404 thật)
docs/Document/…  Usecase.md · API_SPEC 5.x, 17 · ENTITY 13.1 · CHECKLIST_TIEN_DO_UC.md · Quản Lý UseCase.xlsx
docs/web-backend-handoff.md · docs/api-tests/catalog-search.http
```

---

## 6. Bảng ánh xạ Use case → API → Dữ liệu → Kỹ thuật

| UC | Endpoint | Bảng / Redis | Kỹ thuật chính |
|---|---|---|---|
| CAT-02 | `GET /public/products?q&sort&page&size` | `products`, `product_variants`, `product_images`, `vendor_profiles` | A1, A2 (FTS + trigram), A3, A13 |
| CAT-04 | `GET /public/search/suggestions?q` · `GET /public/search/trending?limit` | `products`, `categories`, `vendor_profiles`, `search_keyword_stats`; Redis `search:kw:*` | A1, A5 (prefix + trigram), A6 (ZSET top-K + flush) |
| CAT-05 | `GET /public/products?…&include_facets=true` | + `brands`, `vendor_shipping_config` | A3, A4 (SQL builder, CTE đệ quy, JSONB `@>`, disjunctive facets) |
| CAT-06 | `GET /public/products/{id}` · `POST /public/products/{id}/views` | `products`, `product_variants`, `product_images`, `brands`, `vendor_profiles`, `product_view_stats`; Redis `pv:*` | A7 (ma trận SKU), A10 (HINCRBY + HyperLogLog + flush) |
| CAT-07 | `GET /public/products/{id}/similar?limit` · `GET /public/recommendations` | `products`, `categories`; Redis `similar:*`, `recent:cat:*` | A8 (candidate + weighted score), A9 (seeded random + interleave) |
| CAT-16 | `GET /public/shops/{id}` · `GET /public/products?shop_id=` | `vendor_profiles`, `products`; Redis `shop:stats:*` | A12, dùng lại A4 |
| CAT-18 | (không API riêng) SSR `/p/{slug}-{id}?v&src=share` + `POST /views {EXTERNAL_LINK}` | `products`, `product_view_stats` | A11 (slug 301, OG, Web Share API) |

**Mã lỗi dùng:** `GNE-CAT-4041` (danh mục), `GNE-CAT-4042` (sản phẩm không còn bán), `GNE-VND-404x` (shop — D7), `GNE-VAL-4220`, `GNE-RAT-4290`.
**Migration:** `V18__catalog_search_tuning.sql`. **Không** thêm `UploadPurpose` / `AuditAction`.

---

## 7. Nhánh, commit, PR

- Nhánh: `feature/catalog-search` (C00–C03), `feature/catalog-product-detail` (C04–C05), `feature/catalog-shop-page` (C06), nối web chung `feature/web-catalog-real-api` (C07), tài liệu đi cùng PR cuối (C08) — mỗi nhánh một PR ≤ ~400 dòng.
- Scope commit: `db`, `common`, `catalog`, `vendor`, `web`, `docs`.
- Quy ước chung: [README 2026-11-16 mục 5](../../2026-11-16/README.md).

## 8. Cổng kiểm tra trước mỗi task `COMMIT`

1. `cd server-spring && ./mvnw verify` — unit + IT + ArchUnit.
2. Swagger UI có endpoint mới, summary chứa mã UC.
3. Thử tay `docs/api-tests/catalog-search.http` trên CSDL đã seed (có cả từ có dấu / không dấu / gõ sai).
4. `EXPLAIN (ANALYZE, BUFFERS)` câu tìm kiếm chính dùng index GIN (không `Seq Scan` trên `products`) — dán kết quả vào Ghi chú progress.
5. Nếu đụng `web/`: `cd web && npm run verify`.
6. Tick task ở progress, ghi mã commit vào Nhật ký.

---

## 9. Danh sách task theo giai đoạn

Ký hiệu: `Cxx.yy` = task; `Cxx.Cn` = task commit.

### C00 — Chuẩn bị
- **[C00.01]** Kiểm danh tính git — `git config user.name` / `user.email`.
- **[C00.02]** Chốt quyết định D1–D12 — ghi vào đầu file progress.
- **[C00.03]** Đăng ký `V18` ở README 2026-11-16 mục 4 (kiểm số chưa bị chiếm).
- **[C00.04]** Tạo nhánh `feature/catalog-search` từ `develop`.
- **[C00.05]** Kiểm dữ liệu thử: seed ≥ 50 sản phẩm nhiều danh mục / thương hiệu / tỉnh; viết script sinh **10.000 sản phẩm** giả cho `SearchPerformanceIT`.
- **[C00.06]** Đọc màn mock `/search`, `/c/…`, `/p/…`, `/shop/…`, dropdown header — ghi trường giao diện cần.

### C01 — Nền: migration, cache, chuẩn hóa
- **[C01.01]** `V18__catalog_search_tuning.sql` — `search_vector` có trọng số (D3), index trigram bỏ dấu (D3), index giá SKU (D4); cập nhật ENTITY 13.1 trước.
- **[C01.02]** `CacheConfig` — `RedisCacheManager`, cache `catalog.product-detail` 5′, `catalog.similar` 30′, `catalog.trending` 10′, `catalog.category-tree` 1h, `vendor.shop-stats` 10′; serializer JSON.
- **[C01.03]** `EtagConfig` — `ShallowEtagHeaderFilter` cho `/api/v1/public/**` + `Cache-Control` theo API 0.12.
- **[C01.04]** `KeywordNormalizer` + `SlugUtil` + test (A1, A11) — bộ mẫu 30 chuỗi khớp `unaccent()` của PG (test IT so sánh).
- **[C01.05]** `TestDataBuilder` — dựng danh mục cây, brand, shop (`vendor_profiles` + `vendor_shipping_config`), sản phẩm + SKU + ảnh (dùng lại của plan wishlist nếu đã có).
- **[C01.C1]** COMMIT — `feat(db): weight product search vector and add unaccent trigram index`
- **[C01.C2]** COMMIT — `feat(common): configure redis cache and etag for public api`

### C02 — Tìm kiếm & lọc (UC-CAT-02, UC-CAT-05)
- **[C02.01]** `ProductSort` enum + `ProductSearchRequest` (validate `size ≤ 60`, `page ≤ 50`, `price_min ≤ price_max`).
- **[C02.02]** `ProductSearchSqlBuilder` — dựng WHERE / ORDER BY động (A3, A4), CTE đệ quy danh mục; test đơn vị so chuỗi SQL + tham số.
- **[C02.03]** `ProductSearchService` — tầng 1 FTS + tầng 2 trigram (A2), gộp kết quả, `total_items`, `statement_timeout`.
- **[C02.04]** Dựng `$ProductCard` theo lô (giá thấp nhất, ảnh chính, shop mini, `is_free_shipping`, `is_shop_on_vacation`) — D8.
- **[C02.05]** `FacetService` — disjunctive facets thương hiệu / nơi bán / thuộc tính (A4).
- **[C02.06]** Cổng `CatalogShopDirectory` + `ShopVoucherAvailability` (D6, D10) với cài đặt mặc định.
- **[C02.07]** `PublicProductSearchController` — `GET /api/v1/public/products`; `RateLimiter` 60/phút/IP; `@Operation(summary = "UC-CAT-02, UC-CAT-05 …")`.
- **[C02.08]** `ProductSearchIT` — có dấu / không dấu ("ao thun" ↔ "áo thun"), gõ sai ("tai nge") ra qua trigram, prefix ("tai ngh"), từ rỗng → mới nhất, `DRAFT/HIDDEN` không xuất hiện, sắp xếp 5 kiểu ổn định, phân trang không trùng.
- **[C02.09]** `FacetIT` — mọi bộ lọc A4; facet thương hiệu không bị thu hẹp bởi chính nó; `attr.*` chỉ khi 1 danh mục.
- **[C02.10]** `SearchPerformanceIT` (`@Tag("perf")`) — 10.000 sản phẩm: p95 < 300 ms cho tìm kiếm + facet; ghi số đo vào Ghi chú.
- **[C02.C1]** COMMIT — `feat(catalog): search products with full text and trigram fallback`
- **[C02.C2]** COMMIT — `feat(catalog): add multi criteria filters sorting and facets`

### C03 — Gợi ý & xu hướng (UC-CAT-04)
- **[C03.01]** `SearchKeywordCounter` — dedupe `SET NX`, `ZINCRBY`, `HINCRBY pending` (A6); gọi sau khi tìm kiếm có kết quả (`@Async`).
- **[C03.02]** `SearchKeywordFlushJob` — `@Scheduled(fixedDelay = 5m)`, khóa job, `RENAME` nguyên tử, UPSERT `accumulate()`.
- **[C03.03]** `TrendingService` — `ZUNIONSTORE` + blocklist + dự phòng SQL; cache 10 phút.
- **[C03.04]** `SuggestionService` — 2 danh mục / 2 shop / 5 sản phẩm (A5).
- **[C03.05]** `PublicSearchController` — `GET /public/search/suggestions`, `GET /public/search/trending`.
- **[C03.06]** `SuggestionIT` + `TrendingIT` — gợi ý không dấu, ngưỡng 2 ký tự, từ bị chặn không hiện, flush cộng dồn đúng khi chạy 2 lần, Redis tắt vẫn trả trending từ DB.
- **[C03.C1]** COMMIT — `feat(catalog): add search suggestions and trending keywords`

### C04 — Chi tiết sản phẩm & lượt xem (UC-CAT-06)
- **[C04.01]** `VariantMatrixBuilder` + test (A7) — `option_groups`, `variants[].options`, ảnh theo giá trị.
- **[C04.02]** `ProductDetailService` — nạp theo lô, cache phần tĩnh, đọc tồn riêng (D11); `404 GNE-CAT-4042` khi không `PUBLISHED`.
- **[C04.03]** `PublicProductController` — `GET /public/products/{id}`, `GET /public/products/{id}/variants`.
- **[C04.04]** `ProductViewCounter` + `ProductViewFlushJob` + `RecentCategoryTracker` (A10, A9).
- **[C04.05]** `PublicProductViewController` — `POST /public/products/{id}/views` (`204`).
- **[C04.06]** `ProductDetailIT` + `ProductViewIT` — tồn = `stock − reserved`, SKU xóa mềm bị loại, cache bị xóa khi sản phẩm đổi, dedupe 30 phút, flush đúng `product_view_stats` + `products.view_count`, nguồn sai → `422`.
- **[C04.C1]** COMMIT — `feat(catalog): show product detail with sku matrix and available stock`
- **[C04.C2]** COMMIT — `feat(catalog): count product views by traffic source`

### C05 — Tương tự & gợi ý hôm nay (UC-CAT-07)
- **[C05.01]** `SimilarProductService` — candidate + scoring + bù danh mục cha (A8); trọng số trong `application.yml`.
- **[C05.02]** `RecommendationService` — seeded random + interleave theo danh mục xem gần đây (A9).
- **[C05.03]** Endpoint `GET /public/products/{id}/similar`, `GET /public/recommendations`.
- **[C05.04]** `SimilarProductIT` + `RecommendationIT` — không chứa chính nó, ngoài ±50% giá bị loại, shop nghỉ bị loại, cùng ngày 2 lần gọi trang 2 giống hệt, khác ngày khác thứ tự.
- **[C05.C1]** COMMIT — `feat(catalog): recommend similar products and daily picks`

### C06 — Trang shop (UC-CAT-16)
- **[C06.01]** Phối hợp chủ plan Mở shop & KYC về `PublicShopController` (D7).
- **[C06.02]** `PublicShopController` — `GET /public/shops/{id}`, `GET /public/shops?featured=true`; cache `shop:stats`.
- **[C06.03]** `PublicShopIT` — shop chưa `VERIFIED` / chủ bị khóa → `404`; lưới `GET /public/products?shop_id=` đúng shop; tìm trong shop.
- **[C06.C1]** COMMIT — `feat(vendor): add public shop page api`

### C07 — Nối giao diện `web/` (cả 7 UC, gồm UC-CAT-18)
- **[C07.01]** `suggest.api.ts` — `/public/search/suggestions` (3 nhóm), trending trả `{keyword}[]`; debounce 250 ms + `AbortController`.
- **[C07.02]** `product.api.ts` — gộp `getFacets` vào `listProducts(include_facets)`, `related → similar`, `feed → recommendations`, thêm `recordView(source)` (`sendBeacon`).
- **[C07.03]** Trang `/search`, `/c/[slug]` — bộ lọc đồng bộ URL params, `keepPreviousData`, trạng thái rỗng + "Có thể bạn quan tâm".
- **[C07.04]** Trang `/p/[slug]-[id]` — `shared/lib/slug.ts`, 301 khi slug sai, 404 thật, chọn sẵn `?v=`, `src` → `source` của lượt xem, `og:price:amount` / `og:price:currency` + canonical.
- **[C07.05]** `WishShare.tsx` — Web Share API → Clipboard → toast "Đã sao chép liên kết"; link có `v` và `src=share` (UC-CAT-18).
- **[C07.06]** `shop.api.ts` + `/shop/[id]` — nối thật, 404 shop.
- **[C07.07]** Vitest: mapper DTO, `slug.ts` (cùng bộ mẫu với `SlugUtilTest`), hàm khóa giá trị biến thể không khả dụng.
- **[C07.08]** Chạy thật hai phía (`PUBLIC_REAL_API=catalog,search,shop`): tìm có dấu / không dấu / gõ sai, lọc + sắp, gợi ý dropdown, chi tiết chọn SKU hết hàng, tương tự, trang shop, dán link chia sẻ vào Zalo / Messenger (hoặc Facebook Sharing Debugger) thấy ảnh xem trước.
- **[C07.09]** `cd web && npm run verify` + cập nhật Playwright cho `/search` và `/p/…`.
- **[C07.C1]** COMMIT — `fix(web): call catalog endpoints defined in the api spec`
- **[C07.C2]** COMMIT — `feat(web): share product deeplink with open graph preview`

### C08 — Hoàn thiện use case
- **[C08.01]** `docs/api-tests/catalog-search.http` — kịch bản đúng + lỗi (`404`, `422`, `429`).
- **[C08.02]** OpenAPI — kiểm `/v3/api-docs`.
- **[C08.03]** Tài liệu nguồn — Usecase.md (D1, D5, D9, D12 + dòng *Hiện trạng* 7 UC), API 5.x (D8, D10, D11) và 17 (đánh dấu 5 endpoint đã sửa), ENTITY 13.1 (D3, D4), LLD 9 thêm mục "Tìm kiếm & gợi ý" tóm tắt A2, A6, A8; LLD 10 thêm 2 job.
- **[C08.04]** Tiến độ — `CHECKLIST_TIEN_DO_UC.md`, `Quản Lý UseCase.xlsx`, `docs/web-backend-handoff.md`, `CLAUDE.md` mục 2 + mục 9 (cổng `CatalogShopDirectory`, `ShopVoucherAvailability` chờ module khác cài).
- **[C08.C1]** COMMIT — `docs(catalog): record search and product page use cases as implemented`
- **[C08.05]** Mở PR vào `develop` (Ảnh hưởng CSDL: `V18`).

---

## 10. Ngoài phạm vi

- Elasticsearch / OpenSearch, Redis Trie, tìm kiếm vector / AI (Qdrant, embedding), gợi ý cộng tác (collaborative filtering) — DATN.
- Từ đồng nghĩa / sửa chính tả theo từ điển ("đt" → "điện thoại") — ghi làm ý mở rộng (`synonym` dictionary của PG).
- Bộ lọc Flash Sale / `discount_min` thật — chờ UC-PRO-06.
- Màn Staff chặn từ khóa (`is_blocked`) — chỉ làm phần đọc cờ; giao diện Admin thuộc plan khác.
- Mã giới thiệu / affiliate trong deeplink.

## 11. Rủi ro & cách xử lý

| Rủi ro | Xử lý |
|---|---|
| `unaccent` Java ≠ PostgreSQL → thống kê / slug lệch | Bộ mẫu chung + IT so sánh (C01.04) |
| Truy vấn tìm kiếm chậm khi nhiều bộ lọc + facet | Index V18, chặn trang sâu, `statement_timeout`, `SearchPerformanceIT`; còn chậm → cột `min_price` chuẩn hóa (D4) |
| Đổi cột sinh `search_vector` khóa bảng `products` | Chạy ở `V18` khi bảng còn nhỏ (dev / seed); production chưa có dữ liệu thật |
| Đếm từ khóa / lượt xem bị spam | Dedupe `SET NX` theo IP / visitor + rate limit |
| Mất số đếm khi Redis khởi động lại giữa 2 lần flush | Chấp nhận mất ≤ 5 phút số liệu thống kê (không phải dữ liệu tiền) — ghi vào tài liệu |
| Cache chi tiết sản phẩm hiển thị giá cũ | `@CacheEvict` khi sản phẩm / SKU đổi; giỏ & đặt hàng luôn đọc DB |
| Đọc chéo bảng module `vendor` trong SQL | Gói gọn trong `ProductSearchSqlBuilder`, ghi rõ ở D6 + CLAUDE.md mục 9 |
