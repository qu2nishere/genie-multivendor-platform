# PLAN — Hoàn thiện phần còn thiếu của cụm B1, B2, B3 (UC-ADM-03 · UC-CAT-01, 14, 15 · UC-CAT-05)

| Mục | Nội dung |
|---|---|
| **Ngày lập** | 2026-11-24 |
| **Người rà soát / làm bù** | Nguyễn Ngọc Thiện — chỉ rà soát và bổ sung phần còn thiếu; **phân công không đổi**, người phụ trách gốc xem lại từng PR trước khi gộp |
| **Phụ trách** | B1, B2: Nguyễn Quốc Khoa (`plan/2026-11-19/b1.md`, `b2.md`) · B3: Nghiêm Phú Đăng Quân (`plan/2026-11-17/server-spring/plan_tim_kiem_va_trang_san_pham.md`) |
| **Tiến độ** | [progress_hoan_thien_b1_b2_b3.md](progress_hoan_thien_b1_b2_b3.md) |
| **Mục tiêu** | 5 UC đang 🟡 → ✅ (đủ 5 lớp GD / BE / Test / Nối / Chạy): **UC-ADM-03**, **UC-CAT-01**, **UC-CAT-14**, **UC-CAT-15**, **UC-CAT-05**. Tổng quan dự kiến: Đã triển khai 12 → **17** / 87 |
| **Người commit** | danh tính git đã cấu hình trong repo — không `--author`, không `--no-verify`, không `Co-Authored-By` / dấu vết AI (CLAUDE.md mục 4) |
| **Nguồn quy định (thứ tự ưu tiên khi mâu thuẫn)** | TECH_STACK mục 12 → Usecase.md / ENTITY.md → Document_Vi → implement (CLAUDE.md mục 8). Cụ thể: `Usecase.md` UC-ADM-03 (dòng 2590), UC-CAT-01 / 05 / 14 / 15 · `API_SPEC.md` 0.12, 5.3, 11.8, 11.11, 11.13, 15, 16.4 · `ENTITY.md` 1.1 `User`, 4.1 `Voucher`, 2.1 `Category`, 2.6 `CategoryAttribute`, 12.2 `PlatformSetting` · `LLD.md` 10 (Cron), 11.1 (ma trận quyền) · `TECH_STACK_VA_QUY_UOC.md` 4.4, 10.3, 13.3, 14 |

---

## 0. Hiện trạng đã khảo sát (`develop` @ `1d0c849` + nhánh `chore/minor-fixes`, 2026-11-24)

| Cụm / UC | Đã có | Còn thiếu (lý do UC chưa ✅) |
|---|---|---|
| **B1 · UC-ADM-03** Khóa / Mở khóa | `User.suspend / reactivate / reactivateIfSuspensionExpired` (đã có unit test), `UserFacade.suspend / reactivate`, `/admin/users` + `/admin/staff` (đã chạy thật 2026-11-21), WebSocket đăng xuất ngay. Khi **đăng nhập**, `LoginService` / `OAuthLoginService` đã tự mở khóa nếu hết hạn (ghi `ACCOUNT_REACTIVATED` với `reason = SUSPENSION_EXPIRED`) | **Cron `SuspensionExpiryJob`** (Usecase UC-ADM-03, luồng thay thế, 5 phút / lần): chưa có → người bị khóa 7 ngày mà không đăng nhập lại thì vẫn hiện "Đang khóa" ở `/admin/users`, không có nhật ký, không được thông báo. LLD 10 chưa có dòng của cron này. Câu *Hiện trạng* của UC-ADM-03 trong `Usecase.md` còn ghi "Chưa triển khai backend" (lệch checklist) |
| **B2 · UC-CAT-01 / 14 / 15** | Đủ GD / BE / Nối; `./mvnw verify`, `npm run verify` xanh; 26 bước `catalog-category.http` đúng trên backend thật (2026-11-24) | (1) **Chạy thật trên giao diện** (storefront, Admin, Seller) — chưa ai bấm. (2) Test còn hở: **`CatalogSecurityIT`** (ma trận vai trò — TECH_STACK 14 "ma trận 13 mã quyền"), xóa ngành bị voucher tham chiếu → `IN_USE` (API 11.11) chưa có test, "Redis lỗi → đọc thẳng CSDL" (UC-CAT-01 bước 3) chưa có test |
| **B3 · UC-CAT-05** Lọc & sắp xếp | Backend đã nhận `has_shop_voucher`, SQL đã lọc theo danh sách shop qua cổng `catalog.search.application.ShopVoucherAvailability`; cài đặt mặc định trả `Optional.empty()` → bộ lọc bị bỏ qua, `has_shop_voucher_supported = false` | (1) **Module `promotion` chưa cài cổng** (bảng `vouchers` đã có, seed có 2 voucher SHOP của TechZone). (2) Web **không đọc** `has_shop_voucher_supported` → ô "Shop có voucher" luôn hiện dù chưa lọc được (trái plan 2026-11-17 D10: "client ẩn ô lọc"). (3) **Bộ lọc thuộc tính ngành vẫn là mock**: `listAttributeFilters` gọi `GET /public/categories/{id}/attribute-filters` — endpoint **không có trong API_SPEC** (handoff dòng 41); spec đã trả `summary.facets.attribute` ở 5.3 → web phải dùng nguồn này (UC-CAT-05 bước 5, UC-CAT-15 bước 4). (4) Trang danh mục `/c/{slug}` không có ô "Shop có voucher" trong khi Usecase UC-CAT-05 ghi tiền điều kiện "trang danh mục **hoặc** trang kết quả tìm kiếm" → cần chốt (D8) |

**Việc đang dở trên nhánh `chore/minor-fixes` (chưa commit):** sửa ETag `/public/**` (`ContentEtagHeaderFilter`, test hồi quy trong `ProductSearchIT`) + cập nhật tiến độ B2 / B3 trong CHECKLIST, BAO_CAO, Usecase.md, Excel, `b2.md`, progress B3. Phải commit trước khi mở nhánh mới (H00).

**Ngoài phạm vi plan này (ghi lại để không quên):**
- `CategoryFacade.attributesFor` + kiểm thuộc tính bắt buộc khi đăng bán → làm cùng UC-CAT-09 (plan sản phẩm người bán).
- Duyệt đề xuất danh mục (UC-CAT-17), sản phẩm theo danh mục trong Admin (UC-CAT-12 / 13) → còn mock, thuộc plan khác.
- Thông báo **trong app** khi tự mở khóa / ân hạn 14 ngày thuộc tính → module Notification (UC-NOT-*). Plan này chỉ gửi **email** (D2).
- Dán deeplink vào Zalo / Facebook (UC-CAT-18) → chờ tên miền công khai.
- Tạo / sửa voucher shop (UC-PRO-01), kho voucher (UC-PRO-03) → cụm B5. Plan này **chỉ đọc** bảng `vouchers`.

---

## 1. Nguyên tắc bắt buộc (bám tài liệu)

1. **Tài liệu sửa trước, code sau** (TECH_STACK 7.3, cách làm của plan B1 / B2): mỗi giai đoạn có thay đổi hành vi thì sửa `Usecase.md` / `API_SPEC.md` / `LLD.md` trước, commit `docs(...)` riêng.
2. **Ranh giới module** (TS 4.4, `ModuleBoundaryTest`): `promotion` chỉ cài **cổng đảo ngược** nằm ở `catalog.search.application` (giống `vendor` cài `WishlistShopAvailability`); không import `domain` / `api` / `infra` của module khác; không tạo vòng phụ thuộc (đã kiểm: hiện `catalog` không import `promotion`).
3. **Cron** (TS 10.3, LLD 10): `@Scheduled` + `SELECT … FOR UPDATE SKIP LOCKED`, xử lý theo lô, mỗi lô một transaction, lỗi một tài khoản không dừng các tài khoản khác, ghi log kết quả. Mẫu có sẵn: `AnonymizeAccountsJob`, `KycSupplementExpiryJob`.
4. **Email / thông báo / cache chỉ chạy sau commit** (`@TransactionalEventListener(AFTER_COMMIT)` — CLAUDE.md mục 6). Không gửi mail trong transaction mở khóa.
5. **Không đổi tên bảng / cột / enum / mã UC** (NFR-12). Plan này **không cần migration** (V25 chưa dùng) — nếu phát sinh thì `V25__…` và sửa ENTITY.md trước.
6. **Mã lỗi** chỉ dùng mã đã có trong API_SPEC 15. Không thêm mã mới.
7. **Web** (CLAUDE.md mục 5): sửa theo 4 tầng `domain → infrastructure → application → ui`; mọi chuỗi qua `t()`, thêm khóa vi thì thêm en; màn có đổi giao diện kiểm **9 theme**; sửa logic lọc thì sửa cả `*.mock.ts` cho khớp.
8. **Cổng kiểm tra trước mỗi task COMMIT**: `cd server-spring && ./mvnw verify` bằng **JDK 21** (`JAVA_HOME=C:\Users\MSII\.jdks\ms-21.0.10`; JDK 24 làm Mockito lỗi ở `CoinLedgerServiceTest` / `BannerCacheTest`); có đụng `web/` thì `cd web && npm run verify`.
9. **Chỉ tick cột Chạy khi người dùng đã tự thao tác trên giao diện với hệ thống thật** (CLAUDE.md mục 4.4). Claude chạy API / SSR thì ghi "API đúng", không tick Chạy.
10. **Commit** theo TECH_STACK 13.3: `<type>(<scope>): <subject>` tiếng Anh, chữ thường, ≤ 100 ký tự, body tiếng Anh; một commit = một thay đổi logic, build được, test xanh; `git add` đúng từng file.

---

## 2. Quyết định cần chốt trước khi làm

| Mã | Câu hỏi | Căn cứ | Đề xuất | Chốt |
|---|---|---|---|---|
| D1 | **Nhật ký khi cron mở khóa** ghi gì? | UC-ADM-03 luồng thay thế; plan B1 D13 ghi `details.reason = "EXPIRED"`, nhưng code đăng nhập đang ghi `"SUSPENSION_EXPIRED"` | `ACCOUNT_REACTIVATED`, `actor = null` (hệ thống), `reference_id = userId`, `details = { "reason": "SUSPENSION_EXPIRED", "source": "CRON", "suspended_until": "<hạn cũ>" }` — **cùng mã lý do** với luồng đăng nhập để lọc / thống kê một kiểu; `source` phân biệt cron với đăng nhập. Sửa plan B1 D13 cho khớp | ☑ 2026-11-24 theo đề xuất (Thiện rà soát — chờ Khoa / Quân xem lại) |
| D2 | **"Thông báo cho người dùng"** khi tự mở khóa — kênh nào? | UC-ADM-03 luồng thay thế; module Notification chưa có | **Email** mẫu mới `account-reactivated` (vi / en) gửi sau commit qua sự kiện `AccountReactivatedEvent` → `AccountMailListener` (giống `PASSWORD_CHANGED`). Thông báo trong app để plan Notification. Mở khóa **thủ công** (Admin) gửi cùng mẫu? → đề xuất **có** (cùng sự kiện, biến `reason` khác) để người dùng luôn biết tài khoản đã mở | ☑ 2026-11-24 theo đề xuất (Thiện rà soát — chờ Khoa / Quân xem lại) |
| D3 | **Chu kỳ, lô, giới hạn** | Usecase "5 phút/lần"; LLD 10 | `@Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT1M")`; lô 100 (`LIMIT 100 FOR UPDATE SKIP LOCKED`), mỗi lô một transaction, tối đa 1.000 tài khoản / lượt; log `"Đã mở khóa {} tài khoản hết hạn đình chỉ"` khi > 0 | ☑ 2026-11-24 theo đề xuất (Thiện rà soát — chờ Khoa / Quân xem lại) |
| D4 | **Giữ việc tự mở khi đăng nhập** (`LoginService`, `OAuthLoginService`)? | Code hiện có | **Giữ** — là lưới an toàn khi cron chưa kịp chạy (người dùng không phải chờ tới 5 phút). Hai luồng không trùng nhờ khóa dòng: luồng nào mở trước thì luồng kia thấy `ACTIVE` và bỏ qua (kiểm bằng IT) | ☑ 2026-11-24 theo đề xuất (Thiện rà soát — chờ Khoa / Quân xem lại) |
| D5 | **Định nghĩa "shop đang có voucher"** cho bộ lọc | API 5.3 `has_shop_voucher`; ENTITY 4.1 (`isApplicable`, vòng đời ACTIVE: `isActive && validFrom ≤ now ≤ validTo`) | Voucher `type = 'SHOP'` **và** `is_active` **và** `valid_from ≤ now < valid_to` **và** `used_count < usage_limit` **và** (`budget IS NULL` **hoặc** `total_discount_used < budget`). **Không** tính `PLATFORM` / `FREESHIP` (không phải của shop), **không** xét `min_order_value` (điều kiện lúc thanh toán). Trả `users.id` chủ shop (`vouchers.vendor_id`) — đúng hợp đồng cổng | ☑ 2026-11-24 theo đề xuất (Thiện rà soát — chờ Khoa / Quân xem lại) |
| D6 | **Hiệu năng** — cổng bị gọi mỗi lần tìm (2 lần nếu có `include_facets`) | API 0.12, CLAUDE.md (cache Redis `JsonCache`) | Cache Redis bằng `common/cache/JsonCache`, khóa `promotion:shop_voucher_vendors`, TTL **60 giây**; Redis lỗi → đọc thẳng CSDL. Voucher mới / hết hạn được phản ánh trong ≤ 60 giây (chấp nhận cho TLCN). Cụm B5 (tạo / sửa voucher) xóa khóa sau commit. **Không** thêm index (bảng `vouchers` nhỏ) | ☑ 2026-11-24 theo đề xuất (Thiện rà soát — chờ Khoa / Quân xem lại) |
| D7 | **Web khi API chưa hỗ trợ lọc voucher** | Plan 2026-11-17 D10 ("client ẩn ô lọc") | Ánh xạ `has_shop_voucher_supported` sang domain (`voucherFilterSupported`); `FilterPanel` chỉ hiện ô "Shop có voucher" khi `true`; mock trả `true` (mock đã lọc theo `SHOPS_WITH_VOUCHER`) | ☑ 2026-11-24 theo đề xuất (Thiện rà soát — chờ Khoa / Quân xem lại) |
| D8 | **Trang danh mục `/c/{slug}` có ô "Shop có voucher" không?** | Usecase UC-CAT-05: tiền điều kiện "trang danh mục hoặc trang kết quả tìm kiếm", bước 1 liệt kê "Tình trạng khuyến mãi (shop đang có voucher)"; giao diện hiện chỉ có ở `/search` (nhóm Dịch vụ & khuyến mãi chỉ ở `variant="search"`) | **Có** — bám Usecase: thêm ô vào nhóm lọc của `variant="category"` (giữ thứ tự / kiểu như `/search`), `pages/c/[slug].astro` truyền `hasVoucher`. Nếu nhóm muốn giữ đúng prototype thì chốt **Không** và ghi lệch vào `BAO_CAO_DOI_CHIEU_GIAO_DIEN.md` | ☑ 2026-11-24 theo đề xuất (Thiện rà soát — chờ Khoa / Quân xem lại) |
| D9 | **Nguồn bộ lọc thuộc tính ngành** (`attribute-filters` đang mock) | API 5.3 `summary.facets.attribute` (thuộc tính `is_filterable` đầu tiên của danh mục đang chọn); `web-backend-handoff.md` dòng 41 ("bổ sung spec **hoặc** trả trong `include_facets`") | **Dùng `facets.attribute` của 5.3** — không thêm endpoint mới (spec không có). Bỏ hàm `listAttributeFilters` gọi đường dẫn không có; lấy thuộc tính + giá trị + số lượng từ facets; giá trị lọc gửi `attr.{code}` (mã thật, không phải tên). Gạch dòng 41 + 274 của handoff | ☑ 2026-11-24 theo đề xuất (Thiện rà soát — chờ Khoa / Quân xem lại) |
| D10 | **Seed thêm voucher** để chạy thật có cả shop có / không có voucher và voucher hết hạn | `R__seed_demo_50_vouchers.sql` (chỉ TechZone có voucher SHOP) | Thêm 3 dòng vào file 50 (cùng kiểu `VALUES`, `ON CONFLICT DO NOTHING`): 1 voucher SHOP còn hạn cho shop thứ hai (`owner.shoe@genie.example`), 1 voucher SHOP **đã tắt** (`is_active = false`) cho `owner.home@genie.example`, 1 voucher SHOP **hết lượt** cho `owner.beauty@genie.example`. Báo Thiện (chủ plan seed) — chính người làm plan này | ☑ 2026-11-24 theo đề xuất (Thiện rà soát — chờ Khoa / Quân xem lại) |
| D11 | **Nhánh** | CLAUDE.md mục 4 (`feature/…`, `bugfix/…`; không có `chore/`) | Đổi tên nhánh hiện tại `chore/minor-fixes` → `bugfix/public-etag-304` (H00). Ba nhánh mới từ `develop` mới nhất: `feature/b1-suspension-expiry-job`, `feature/b2-catalog-acceptance`, `feature/b3-shop-voucher-filter`. Mỗi PR ≤ ~400 dòng, vào `develop` | ☑ 2026-11-24 theo đề xuất (Thiện rà soát — chờ Khoa / Quân xem lại) |

---

## 3. Luồng nghiệp vụ chi tiết

### 3.1 Cron hết hạn đình chỉ (UC-ADM-03 luồng thay thế — D1–D4)

```
SuspensionExpiryJob.scheduled()            (mỗi 5 phút, chạy sau 1 phút kể từ lúc khởi động)
  └─ run(): lặp tới khi hết hoặc đủ 1.000
       tx.execute:
         rows = users.findSuspensionExpired(now, 100)      -- SELECT * FROM users
                                                            --  WHERE status = 'SUSPENDED'
                                                            --    AND suspended_until IS NOT NULL
                                                            --    AND suspended_until <= :now
                                                            --  ORDER BY suspended_until, id
                                                            --  LIMIT :limit FOR UPDATE SKIP LOCKED
         với từng user:
           until = user.getSuspendedUntil()
           if user.reactivateIfSuspensionExpired(now):    -- domain method có sẵn (ENTITY 1.1)
              audit.record(ACCOUNT_REACTIVATED, actor = null, ref = userId,
                           {reason: SUSPENSION_EXPIRED, source: CRON, suspended_until: until})
              events.publish(AccountReactivatedEvent(userId, email, lang, reason = SUSPENSION_EXPIRED))
       (lỗi một lô → log warn, sang lô sau; lô rỗng → dừng)
AFTER_COMMIT: AccountMailListener.onReactivated → email "account-reactivated"
```

- **Không đụng**: `status = 'LOCKED'` (khóa nhân viên ở `/admin/staff`, khóa vô thời hạn của API 11.13), `SUSPENDED` vô thời hạn (`suspended_until IS NULL`), `ANONYMIZED`, `DELETED`, khóa tạm vì sai mật khẩu (`locked_until` — tự hết, không phải đình chỉ).
- **Không** khôi phục phiên đã thu hồi lúc khóa — người dùng đăng nhập lại như thường.
- Đồng hồ lấy từ `Clock` (IT dùng `MutableClock`).

### 3.2 Cổng "shop có voucher" (UC-CAT-05 — D5, D6)

```
catalog.search ProductSearchService / FacetService
   └─ ShopVoucherAvailability.vendorIdsWithActiveVoucher()      (cổng có sẵn ở catalog.search.application)
        └─ promotion.voucher.application.PromotionSearchPorts (mới, @Component — SearchPortsConfig tự tắt bản mặc định)
             ├─ JsonCache.get("promotion:shop_voucher_vendors") → có thì trả
             └─ không có / Redis lỗi → VoucherRepository.findVendorIdsWithActiveShopVoucher(now)  (native SQL theo D5)
                                        → lưu cache 60 giây → Optional.of(list)   (list rỗng = hỗ trợ nhưng không shop nào có)
```

- Kết quả `Optional.of(List.of())` khác `Optional.empty()`: lọc `has_shop_voucher=true` khi không shop nào có voucher → **0 sản phẩm**, `has_shop_voucher_supported = true`.

### 3.3 Web — bộ lọc (D7, D8, D9)

- `product.dto.ts` đã có `has_shop_voucher_supported`, `attribute` trong facets → thêm vào mapper / domain `SearchFacets` (`voucherFilterSupported: boolean`, `attribute: { code, name, values: {value, count}[] } | null`).
- `FilterPanel`: ô "Shop có voucher" hiện khi `voucherFilterSupported && !inShop`; ở `variant="category"` theo D8.
- Thuộc tính: `FilterPanel` nhận `attribute` từ facets (có `code`) thay cho `attrs[0]` lấy từ mock; URL giữ tham số `attr=<giá trị>` như hiện tại, `product.api.ts` gửi `attr.<code>=<giá trị>` — **kiểm lại hàm dựng query đang gửi tên hay mã** (H11.02).

---

## 4. Cây file sẽ tạo / sửa

```
server-spring/src/main/java/com/genie/
├── auth/profile/application/SuspensionExpiryJob.java                 (mới — H03.02)
├── auth/profile/domain/UserRepository.java                           (sửa — findSuspensionExpired, H03.01)
├── auth/profile/event/AccountReactivatedEvent.java                   (mới — H03.03, D2)
├── auth/profile/application/UserFacade.java                          (sửa — phát sự kiện khi mở thủ công, D2)
├── auth/profile/application/AccountMailListener.java                 (sửa — onReactivated, D2)
├── common/mail/MailTemplate.java                                     (sửa — ACCOUNT_REACTIVATED, D2)
├── promotion/voucher/domain/VoucherRepository.java                   (sửa — findVendorIdsWithActiveShopVoucher, H08.01)
└── promotion/voucher/application/PromotionSearchPorts.java           (mới — H08.02)
server-spring/src/main/resources/
├── templates/email/account-reactivated.html                          (mới — D2; vi + en theo cách các mẫu khác chọn ngôn ngữ)
└── db/seed/R__seed_demo_50_vouchers.sql                              (sửa — D10)
server-spring/src/test/java/com/genie/
├── auth/profile/SuspensionExpiryJobIT.java                           (mới — H03.05)
├── auth/profile/domain/UserTest.java                                 (sửa — biên "đúng bằng hạn", H03.04)
├── common/mail/AccountReactivatedEmailTemplateTest.java              (mới — H03.06)
├── catalog/category/CatalogSecurityIT.java                           (mới — H06.01)
├── catalog/category/AdminCategoryIT.java                             (sửa — IN_USE, H06.02)
├── catalog/category/application/CategoryCatalogTest.java             (mới hoặc sửa — Redis lỗi, H06.03)
├── promotion/voucher/PromotionSearchPortsIT.java                     (mới — H08.04)
└── catalog/search/ProductSearchIT.java, FacetIT.java                 (sửa — has_shop_voucher, H08.05)
web/src/
├── features/product/{domain/product.types.ts, infrastructure/product.mapper.ts, product.mock.ts}   (H11)
├── features/search/ui/FilterPanel.tsx (+ labels / test)              (H11)
├── features/catalog/infrastructure/catalog.api.ts (+ mock, index)    (H11 — bỏ listAttributeFilters)
├── pages/search.astro, pages/c/[slug].astro                          (H11)
└── locales vi.json / en.json                                         (chỉ khi cần khóa mới)
docs/ … (H02, H07, H10, H14 — liệt kê ở từng task)
```

---

## 5. Danh sách task

> Ký hiệu: `[Hxx.yy]` — mã khớp với progress. Task `COMMIT` chỉ chạy sau khi cổng kiểm tra (nguyên tắc 8) xanh.

### H00 — Chuẩn bị (làm trước tiên)

- **[H00.01]** Chốt D1–D11 với người phụ trách (Khoa cho B1 / B2, Quân cho B3); ghi kết quả vào cột *Chốt* mục 2 + nhật ký progress.
- **[H00.02]** Đổi tên nhánh `chore/minor-fixes` → `bugfix/public-etag-304` (D11): `git branch -m bugfix/public-etag-304`.
- **[H00.03]** Chạy lại cổng: `./mvnw clean verify` (JDK 21) + `npm run verify` trên nhánh này (đã xanh 2026-11-24: 177 unit + 293 IT; ghi lại số).
- **[H00.04]** COMMIT `fix(common): ignore response meta when computing public etags` — chỉ `ContentEtagHeaderFilter.java`, `PublicHttpCacheConfig.java`, `ProductSearchIT.java`.
- **[H00.05]** COMMIT `docs: record b2 and b3 verification results` — CHECKLIST, BAO_CAO_UC_DOC_LAP, Usecase.md, `Quản Lý UseCase.xlsx`, `plan/2026-11-19/b2.md`, progress B3, plan + progress này.
- **[H00.06]** Push + mở PR vào `develop` (mẫu `.github/pull_request_template.md`); merge xong mới tạo 3 nhánh của D11 từ `develop` mới nhất.
- **[H00.07]** Môi trường chạy thật: `docker compose --env-file .env -f deploy/docker-compose.local.yml up -d` (postgres 5433, redis, silo, mailpit); `.env` có `SMTP_HOST=localhost`, `SMTP_PORT=1025` (Mailpit http://localhost:8025); `web/.env` `PUBLIC_REAL_API` có đủ `auth,…,rbac,users,catalog,product,search,shop,admin-catalog,commission` (bỏ mock `attribute-filters` sau H11); backend `JAVA_HOME` = JDK 21, profile `local`, `GENIE_SEED_DEMO=true`.

### H01 — B1: chuẩn bị dữ liệu thử

- **[H01.01]** Tạo 3 tài khoản khách thử bằng màn đăng ký `/auth/register` (OTP qua Mailpit): `b1.cron7@example.com`, `b1.forever@example.com`, `b1.manual@example.com` (mật khẩu `Genie@123`). Lý do: seed không có tài khoản `CUSTOMER` riêng (15 tài khoản seed là Admin / nhân viên / chủ shop) — không khóa shop thật.
- **[H01.02]** Ghi `users.id` của 3 tài khoản vào bảng *Dữ liệu thử* ở progress.

### H02 — B1: tài liệu trước

- **[H02.01]** `LLD.md` mục 10: thêm dòng `Hết hạn đình chỉ (SuspensionExpiryJob)` · 5 phút · `SUSPENDED` có `suspended_until <= now` → `ACTIVE`, xóa lý do / hạn, `ACCOUNT_REACTIVATED` (`SUSPENSION_EXPIRED`, `source CRON`), email; lô 100, `SKIP LOCKED` · UC ADM-03.
- **[H02.02]** `API_SPEC.md` 11.13: thêm đoạn *Tự mở khi hết hạn* (cron 5 phút + khi đăng nhập; `ACCOUNT_REACTIVATED` với `details.reason`, `details.source`; email `account-reactivated`); nhắc lại `LOCKED` / vô thời hạn không tự mở.
- **[H02.03]** `Usecase.md` UC-ADM-03: sửa câu *Hiện trạng* đang lệch ("Chưa triển khai backend") cho khớp checklist; bước 6 luồng thay thế ghi kênh thông báo = email (D2).
- **[H02.04]** `plan/2026-11-19/b1.md`: sửa D13 theo D1 (mã lý do `SUSPENSION_EXPIRED`), R06 trỏ sang plan này.
- **[H02.05]** COMMIT `docs(auth): specify suspension expiry job and reactivation email`.

### H03 — B1: backend + test

- **[H03.01]** `UserRepository.findSuspensionExpired(OffsetDateTime now, int limit)` — native SQL như mục 3.1 (`ORDER BY suspended_until, id`, `FOR UPDATE SKIP LOCKED`).
- **[H03.02]** `auth/profile/application/SuspensionExpiryJob` theo D3 (mẫu `AnonymizeAccountsJob`: `TransactionTemplate`, `Clock`, `scheduled()` + `run()` trả số đã mở để test gọi trực tiếp); comment tiếng Việt, dòng đầu ghi nguồn `UC-ADM-03 luồng thay thế; LLD 10; plan 2026-11-24 H03`.
- **[H03.03]** (D2) `AccountReactivatedEvent(userId, email, reason)` trong `auth/profile/event`; job và `UserFacade.reactivate` phát sự kiện; `MailTemplate.ACCOUNT_REACTIVATED("account-reactivated", "Tài khoản Genie của bạn đã được mở lại", "Your Genie account has been reactivated")`; `AccountMailListener.onReactivated` `@TransactionalEventListener(AFTER_COMMIT)`, lỗi gửi chỉ log warn; mẫu HTML theo khung `password-changed.html` (lời chào, câu "tài khoản đã hoạt động lại từ {thời điểm}", nút Đăng nhập, dòng "không phải bạn yêu cầu? liên hệ hỗ trợ").
- **[H03.04]** Unit `UserTest`: biên `suspendedUntil == now` → mở (hàm dùng `isAfter`); `LOCKED` → không mở; `SUSPENDED` vô thời hạn → không mở.
- **[H03.05]** `SuspensionExpiryJobIT` (Testcontainers, `MutableClock`, `FakeEmailSender`):
  1. Hết hạn 1 phút trước → `ACTIVE`, `suspension_reason` / `suspended_until` = NULL, **đúng 1** dòng `ACCOUNT_REACTIVATED` với `details.reason = SUSPENSION_EXPIRED`, `details.source = CRON`, `user_id` NULL, `reference_id = id`.
  2. Còn hạn 1 phút → giữ `SUSPENDED`, không log.
  3. Vô thời hạn (`suspended_until` NULL) → giữ nguyên.
  4. `LOCKED` (khóa nhân viên / khóa vô thời hạn) → giữ nguyên.
  5. `DELETED` / `ANONYMIZED` có `suspended_until` quá khứ (dựng bằng SQL) → không đụng.
  6. 150 tài khoản hết hạn → một lần `run()` mở đủ 150 qua 2 lô; lần `run()` thứ hai trả 0, không log thêm.
  7. **Chạy song song**: 2 luồng `run()` cùng lúc trên 40 tài khoản → tổng mở = 40, mỗi tài khoản đúng 1 dòng audit (SKIP LOCKED).
  8. **Không trùng với đăng nhập** (D4): tài khoản hết hạn → đăng nhập trước (luồng `LoginService` mở + log) → `run()` không log thêm.
  9. Đăng nhập được ngay sau khi cron mở (`POST /auth/sessions` → 201).
  10. Email (D2): `FakeEmailSender` nhận **1** thư `ACCOUNT_REACTIVATED` đúng địa chỉ, **sau commit**; transaction rollback (giả lập lỗi) → không có thư.
  11. Mở thủ công qua `POST /admin/users/{id}/reactivate` → cũng có 1 thư (D2).
- **[H03.06]** `AccountReactivatedEmailTemplateTest` (theo mẫu `PasswordChangedEmailTemplateTest`): render vi / en, có tên, thời điểm, liên kết đăng nhập; không lộ lý do nội bộ.
- **[H03.07]** Cổng: `./mvnw verify` (ArchUnit xanh — job ở `auth`, không import module khác).
- **[H03.08]** COMMIT `feat(auth): reactivate accounts when suspension expires` (repo + job + IT + unit).
- **[H03.09]** COMMIT `feat(auth): email users when their account is reactivated` (sự kiện + template + listener + test).

### H04 — B1: chạy thật (kịch bản TC-B1 ở mục 6.1)

- **[H04.01]** Chạy TC-B1-01 → TC-B1-12, ghi kết quả từng dòng vào progress (Đạt / Không đạt + ghi chú).
- **[H04.02]** Lỗi phát sinh → sửa, thêm test hồi quy, chạy lại đúng TC đó; commit `fix(auth): …` riêng.

### H05 — B2: chuẩn bị

- **[H05.01]** Ghi id các ngành seed dùng để thử (`dien-tu`, một ngành con có sản phẩm, một ngành lá rỗng) vào bảng *Dữ liệu thử* ở progress.
- **[H05.02]** Chuẩn bị SQL tạo voucher tham chiếu danh mục cho TC-B2-17 (mục 6.2) và SQL dọn sau khi thử.

### H06 — B2: test tự động còn thiếu

- **[H06.01]** `CatalogSecurityIT` — ma trận 7 vai trò × mọi endpoint của B2, đối chiếu LLD 11.1 + API 11.8 / 11.11:

  | Endpoint | Khách (không token) | CUSTOMER | VENDOR | CS (`truc.ht`) | Moderator (`phat.nt`, `MANAGE_CATALOG`) | Finance (`linh.tm`, `UPDATE_COMMISSION`) | ADMIN |
  |---|---|---|---|---|---|---|---|
  | `GET /public/categories`, `/{slug}` | 200 | 200 | 200 | 200 | 200 | 200 | 200 |
  | `GET /admin/categories` | 401 | 403 | 403 | 403 | 200 | 403 | 200 |
  | `POST /admin/categories` (không hoa hồng) | 401 | 403 | 403 | 403 | 201 | 403 | 201 |
  | `POST /admin/categories` (có `commission_rate`) | 401 | 403 | 403 | 403 | **403** `GNE-SEC-4030` | 403 | 201 |
  | `PATCH /admin/categories/{id}` đổi tên | 401 | 403 | 403 | 403 | 200 | 403 | 200 |
  | `PATCH …` đổi `commission_rate` | 401 | 403 | 403 | 403 | **403** | 403 | 200 |
  | `PATCH …` gửi lại `commission_rate` **không đổi** | — | — | — | — | **200** (API 11.11: chỉ kiểm khi giá trị thật sự đổi) | — | 200 |
  | `DELETE /admin/categories/{id}` | 401 | 403 | 403 | 403 | 204 | 403 | 204 |
  | `GET/POST/PUT/PATCH /admin/categories/{id}/attributes…` | 401 | 403 | 403 | 403 | 2xx | 403 | 2xx |
  | `GET /admin/commission-settings` | 401 | 403 | 403 | 403 | 403 | 200 | 200 |
  | `PATCH /admin/commission-settings` | 401 | 403 | 403 | 403 | 403 | 200 | 200 |

  Mã lỗi: 401 → `GNE-SEC-4010`, 403 → `GNE-SEC-4030`. Mỗi ô 403 kiểm thêm: có dòng `ACCESS_DENIED` (plan B1 D14, giới hạn 1 dòng / người / đường dẫn / phút). Nhân viên bị thu hồi `MANAGE_CATALOG` giữa chừng → request kế tiếp 403 (plan B1 D19).
- **[H06.02]** `AdminCategoryIT.deleteRejectedWhileAVoucherReferencesTheCategory`: chèn voucher + `voucher_categories` trỏ ngành rỗng → `DELETE` → `409 GNE-CAT-4092`, `details.reason = IN_USE`; ngành vẫn còn; xóa liên kết → xóa được `204`.
- **[H06.03]** Unit cây công khai khi **Redis lỗi** (mẫu `BannerCacheTest.redisFailureIsIgnored`): `StringRedisTemplate` ném lỗi khi đọc / ghi → vẫn trả cây từ CSDL, không ném ra ngoài (UC-CAT-01 bước 3).
- **[H06.04]** `PublicCategoryIT`: header `Cache-Control: public, max-age=300` (API 0.12) và `If-None-Match` → `304` (nhờ sửa ETag H00); đổi tên ngành → ETag mới.
- **[H06.05]** `CommissionIT`: lưu **hàng loạt** nhiều ngành trong một `PATCH` → mỗi ngành đổi đúng 1 dòng `COMMISSION_UPDATED` (từ → đến); ngành gửi lên nhưng không đổi → không ghi (UC-CAT-14 bước 8).
- **[H06.06]** Cập nhật `docs/api-tests/catalog-category.http`: thêm bước Moderator đổi hoa hồng → 403, Finance vào `/admin/categories` → 403, xóa ngành bị voucher tham chiếu → `IN_USE`.
- **[H06.07]** Cổng `./mvnw verify`.
- **[H06.08]** COMMIT `test(catalog): cover role matrix, voucher-referenced delete and redis fallback`.

### H07 — B2: tài liệu trước (chỉ khi H06 lộ lệch spec)

- **[H07.01]** Nếu ma trận H06.01 cho kết quả khác API_SPEC / LLD 11.1 → **dừng**, báo Khoa, chốt sửa code hay sửa tài liệu; ghi quyết định vào nhật ký. Không có lệch → bỏ qua giai đoạn này (tick "không cần").
- **[H07.02]** COMMIT `docs(catalog): …` (nếu có).

### H08 — B3: cổng "shop có voucher" (backend)

- **[H08.01]** `VoucherRepository.findVendorIdsWithActiveShopVoucher(OffsetDateTime now)` — native SQL `SELECT DISTINCT vendor_id FROM vouchers WHERE type = 'SHOP' AND is_active AND vendor_id IS NOT NULL AND valid_from <= :now AND valid_to > :now AND used_count < usage_limit AND (budget IS NULL OR total_discount_used < budget)` (D5).
- **[H08.02]** `promotion/voucher/application/PromotionSearchPorts implements ShopVoucherAvailability` (`@Component`, `@Transactional(readOnly = true)`), cache theo D6 (`JsonCache`, khóa `promotion:shop_voucher_vendors`, 60 giây, Redis lỗi → CSDL); comment ghi nguồn `API_SPEC 5.3; plan 2026-11-17 D10; plan 2026-11-24 D5–D6`.
- **[H08.03]** Kiểm `SearchPortsConfig` tự tắt bản mặc định (`@ConditionalOnMissingBean`) — IT khẳng định bean đang dùng là `PromotionSearchPorts`.
- **[H08.04]** `PromotionSearchPortsIT` — mỗi ca dựng voucher riêng cho shop riêng (`TestDataBuilder`), khẳng định "có / không có" shop đó (CSDL test dùng chung — CLAUDE.md mục 9):
  1. SHOP còn hạn, còn lượt → **có**.
  2. Chưa tới `valid_from` → không. 3. Đã qua `valid_to` → không. 4. `valid_to` đúng bằng now → không (`valid_to > now`).
  5. `is_active = false` → không. 6. `used_count = usage_limit` → không. 7. `total_discount_used >= budget` → không; `budget` NULL → có.
  8. Voucher `PLATFORM` / `FREESHIP` → không tính shop nào.
  9. Một shop có 1 voucher hỏng + 1 voucher tốt → **có** (DISTINCT, không lặp).
  10. Cache: lần 2 không truy vấn CSDL (đếm bằng spy / xóa dữ liệu rồi gọi lại vẫn ra kết quả cũ trong 60 giây); `MutableClock` +61 giây hoặc xóa khóa → nạp lại.
  11. Redis lỗi → vẫn trả đúng từ CSDL.
- **[H08.05]** `ProductSearchIT` / `FacetIT`:
  1. `has_shop_voucher=true&shop_id=<shop có voucher>` → có sản phẩm; `shop_id=<shop không voucher>` → 0.
  2. `has_shop_voucher=true` kết hợp `q`, `category_id`, `price_min/max`, `sort=PRICE_ASC` → mọi sản phẩm thuộc shop có voucher và vẫn đúng các điều kiện kia.
  3. `include_facets=true` → `summary.facets.has_shop_voucher_supported = true`; facet thương hiệu / nơi bán đếm trên tập đã lọc voucher.
  4. `has_shop_voucher=false` / không gửi → không lọc (cùng tổng với khi không gửi).
  5. Tầng trigram (gõ sai) + `has_shop_voucher=true` → vẫn chỉ ra sản phẩm của shop có voucher.
- **[H08.06]** Cổng `./mvnw verify` (ArchUnit: `promotion` → `catalog.search.application` hợp lệ, không vòng).
- **[H08.07]** COMMIT `feat(promotion): provide shops with active vouchers to product search`.

### H09 — B3: seed (D10)

- **[H09.01]** Thêm 3 dòng voucher vào `R__seed_demo_50_vouchers.sql` theo D10 (cột `is_active`, `used_count` đang ghi cứng trong `SELECT` → đổi thành cột của `VALUES` cho dòng tắt / hết lượt; giữ chạy lại được, không ghi cứng id).
- **[H09.02]** `SeedDataIT` xanh (sửa số đếm voucher nếu có khẳng định); `scripts/seed/seed.ps1 -Docker` chạy lại không nhân đôi.
- **[H09.03]** COMMIT `feat(db): seed shop vouchers for more shops including inactive and used up ones`.

### H10 — B3: tài liệu

- **[H10.01]** `API_SPEC.md` 5.3: ghi rõ quy tắc D5 cho `has_shop_voucher`; `has_shop_voucher_supported = true` từ khi module promotion cài cổng; độ trễ ≤ 60 giây (D6). Mục 14 (màn → API): trang danh mục dùng `summary.facets.attribute` cho bộ lọc thuộc tính (D9).
- **[H10.02]** `docs/web-backend-handoff.md`: gạch dòng 41 và 274 (`attribute-filters`) — dùng facets 5.3.
- **[H10.03]** `CLAUDE.md` mục 9: bỏ "Cổng chờ module khác cài: `ShopVoucherAvailability` (promotion)", thêm "`promotion` cài `ShopVoucherAvailability` (cache 60 giây khóa `promotion:shop_voucher_vendors` — B5 xóa khóa khi tạo / sửa voucher)".
- **[H10.04]** Nếu D8 = Không: ghi lệch vào `BAO_CAO_DOI_CHIEU_GIAO_DIEN.md`.
- **[H10.05]** COMMIT `docs(catalog): document shop voucher filter and attribute facets`.

### H11 — B3: web

- **[H11.01]** Domain / mapper (D7, D9): `SearchFacets` thêm `voucherFilterSupported`, `attribute` (có `code`); `product.mapper.ts` ánh xạ từ `summary.facets`; mock trả `has_shop_voucher_supported: true` và `attribute` theo dữ liệu mock (giữ hành vi mock cũ).
- **[H11.02]** Kiểm hàm dựng query trong `product.api.ts`: bộ lọc thuộc tính phải gửi `attr.<code>=<giá trị>` với **mã** thuộc tính từ facets; thêm test Vitest cho hàm dựng query.
- **[H11.03]** `FilterPanel.tsx`: ô "Shop có voucher" theo D7 (ẩn khi không hỗ trợ) và D8 (hiện ở `variant="category"` nếu chốt Có); nhóm thuộc tính dùng `attribute` từ facets (hiện số lượng từng giá trị nếu thiết kế có chỗ); bỏ chip "Shop có voucher" khỏi thanh chip khi ô bị ẩn.
- **[H11.04]** `pages/search.astro`, `pages/c/[slug].astro`: bỏ gọi `listAttributeFilters`, truyền `facets.attribute`, `facets.voucherFilterSupported`; `/c/{slug}` truyền `hasVoucher` (D8).
- **[H11.05]** `features/catalog`: xóa `listAttributeFilters`, `ATTR_FILTER_DTOS`, khóa mock `attribute-filters` và export ở `index.ts` (không còn nơi dùng); `docs/huong-dan-chay-local.md` bỏ nhóm `attribute-filters` khỏi danh sách mock (nếu có).
- **[H11.06]** Test Vitest: `filters.test.ts` (URL `voucher=1` ↔ state, chip), test mapper facets, test `FilterPanel` (ẩn / hiện ô theo cờ) nếu bộ test có test component.
- **[H11.07]** Kiểm **9 theme** ở `/search` và `/c/dien-tu` (ô mới + nhóm thuộc tính): `?theme=genie|classic|dongho|dongson|hoian|polymer|buudien|saigon|sleeve`, cả khổ hẹp (bộ lọc dạng ngăn kéo).
- **[H11.08]** `npm run verify`.
- **[H11.09]** COMMIT `feat(web): drive voucher and attribute filters from search facets` (nếu dài quá 400 dòng thì tách: `feat(web): hide unsupported shop voucher filter` · `feat(web): load category attribute filters from search facets` · `feat(web): add shop voucher filter to category pages`).

### H12 — B2 + B3: chạy thật trên giao diện (kịch bản TC-B2, TC-B3 ở mục 6.2, 6.3)

- **[H12.01]** Chạy TC-B2-01 → TC-B2-30 (người dùng tự bấm), ghi kết quả vào progress.
- **[H12.02]** Chạy TC-B3-01 → TC-B3-16, ghi kết quả vào progress.
- **[H12.03]** Lỗi phát sinh → sửa + test hồi quy + chạy lại TC; commit `fix(...)` riêng cho từng lỗi.

### H13 — Hồi quy toàn bộ

- **[H13.01]** `./mvnw clean verify` (JDK 21) — ghi số unit / IT.
- **[H13.02]** `SearchPerformanceIT` có bộ lọc voucher: `./mvnw verify -Dperf=true -Dit.test=SearchPerformanceIT -Dtest=NoSuchTest -Dsurefire.failIfNoSpecifiedTests=false -Dfailsafe.failIfNoSpecifiedTests=false` — p95 không tệ hơn mốc 2026-11-24 (p50 27 ms, p95 51 ms) quá 20 %.
- **[H13.03]** `npm run verify` + `npm run test:e2e` (Playwright trên bản build, mock) — 0 lỗi.
- **[H13.04]** Chạy lại toàn bộ `docs/api-tests/catalog-category.http`, `catalog-search.http` trên backend thật (thêm ca `has_shop_voucher` vào `catalog-search.http`).

### H14 — Đồng bộ tiến độ & đóng plan

- **[H14.01]** `CHECKLIST_TIEN_DO_UC.md`: UC-ADM-03 BE / Test / Chạy `◐` → `☑`, UC-CAT-01 / 14 / 15 Chạy `☐` → `☑`, UC-CAT-05 BE / Nối / Chạy `◐` → `☑` — **chỉ** khi TC tương ứng Đạt hết; trạng thái → ✅; mục 1 *Tổng quan* (Đã triển khai 12 → 17, Đang triển khai 22 → 17) + bảng *Tiến độ theo phân hệ* (02: 6 / 5 / 6 → 10 / 1 / 6; 11: 2 / 3 / 3 → 3 / 2 / 3); mục 3 gạch việc đã xong; mục 5 một dòng nhật ký.
- **[H14.02]** `Usecase.md`: câu *Hiện trạng* của 5 UC. `BAO_CAO_UC_DOC_LAP.md`: bảng B1 / B2 / B3 + dòng thành viên. `Quản Lý UseCase.xlsx`: cột *Hiện trạng mã nguồn* của 5 UC → "Đã triển khai", sheet *Thành viên* (sửa thẳng chuỗi trong XML như lần 2026-11-24 để không mất định dạng).
- **[H14.03]** `b1.md` R06 ☑, `b2.md` "Chạy thật" ☑ + C07 `CatalogSecurityIT` ☑, progress B3 (UC-CAT-05) — mỗi file một dòng nhật ký trỏ về plan này.
- **[H14.04]** Chép kịch bản mục 6 sang `docs/kiem-thu/kich-ban-2026-11-24-b1-b2-b3.md` (cùng khuôn `kich-ban-2026-11-16-chuc-nang-chung.md`: bảng *Mã · Thao tác · Kết quả mong đợi*, SQL hỗ trợ ở cuối mục).
- **[H14.05]** `CLAUDE.md` mục 2: một dòng *Plan 2026-11-24* (cron đình chỉ, cổng voucher, bộ lọc thuộc tính từ facets).
- **[H14.06]** COMMIT `docs: update progress after completing b1, b2 and b3 use cases`; mở PR từng nhánh vào `develop`.

---

## 6. Kịch bản kiểm thử thủ công (chạy thật)

> Môi trường H00.07. Mật khẩu mọi tài khoản seed: `Genie@123`. Mỗi dòng ghi Đạt / Không đạt vào progress. SQL chạy bằng `docker exec -it genie-local-postgres-1 psql -U genie -d genie`.

### 6.1 B1 · UC-ADM-03 — Cron hết hạn đình chỉ (Admin `yen.dh@genie.example`)

| Mã | Thao tác | Kết quả mong đợi |
|---|---|---|
| TC-B1-01 | `/admin/users` → tìm `b1.cron7@example.com` → Khóa, lý do `ABNORMAL_RETURNS`, thời hạn **7 ngày** | Tab *Đang khóa* có tài khoản, trạng thái "Đình chỉ" + lý do + hạn; số đếm của tab tăng 1; tab đang đăng nhập của tài khoản đó bị đăng xuất ngay (WebSocket) |
| TC-B1-02 | Đăng nhập `b1.cron7@example.com` | `403 GNE-AUTH-4031`, màn hình hiện lý do + hạn + kênh khiếu nại |
| TC-B1-03 | SQL (a) dời hạn về 1 phút trước; **không** đăng nhập; chờ tối đa 5 phút (xem log backend) | Log `Đã mở khóa 1 tài khoản hết hạn đình chỉ`; `/admin/users` (tải lại) → trạng thái "Hoạt động", rời tab *Đang khóa* |
| TC-B1-04 | `/admin/audit-logs`, lọc nhóm AUTH / tìm email | Có `ACCOUNT_REACTIVATED`, người làm = Hệ thống, chi tiết `SUSPENSION_EXPIRED` · `CRON` · hạn cũ |
| TC-B1-05 | Mailpit http://localhost:8025 | Có **1** thư "Tài khoản Genie của bạn đã được mở lại" gửi `b1.cron7@example.com`, đúng tiếng Việt, nút Đăng nhập trỏ `/auth/login` |
| TC-B1-06 | Đăng nhập `b1.cron7@example.com` | Đăng nhập được |
| TC-B1-07 | Khóa `b1.forever@example.com` **vô thời hạn** → SQL (b) kiểm `suspended_until` NULL → chờ ≥ 6 phút | Vẫn khóa ("Đang khóa", vô thời hạn); không có `ACCOUNT_REACTIVATED` mới; không có thư |
| TC-B1-08 | Khóa `b1.manual@example.com` 30 ngày → Mở khóa thủ công, **bỏ trống lý do** | Không cho lưu, lỗi tại ô lý do (`GNE-VAL-4220`) |
| TC-B1-09 | Mở khóa thủ công có lý do "Đã xác minh khiếu nại" | "Hoạt động"; log `ACCOUNT_REACTIVATED` lý do Admin nhập, người làm = Yến; 1 thư mở khóa (D2); sau ≥ 5 phút cron **không** log thêm |
| TC-B1-10 | Khóa lại `b1.cron7` 7 ngày → SQL (a) dời hạn → **đăng nhập ngay** (trước khi cron chạy) | Đăng nhập được (luồng D4); đúng **1** dòng `ACCOUNT_REACTIVATED` (không có `source CRON`); cron lần sau không log thêm |
| TC-B1-11 | Admin khóa một nhân viên thử ở `/admin/staff` (tạo nhân viên mời thử, không khóa nhân viên seed) → chờ ≥ 6 phút | Nhân viên vẫn khóa (`LOCKED` không thuộc cron) |
| TC-B1-12 | Đăng nhập `phat.nt@genie.example` (Moderator, không có `MANAGE_USER`) → `/admin/users` | Menu không có mục; gõ URL → màn "không có quyền"; API `403 GNE-SEC-4030` |

SQL hỗ trợ:
```sql
-- (a) dời hạn đình chỉ về quá khứ
UPDATE users SET suspended_until = now() - interval '1 minute' WHERE email = 'b1.cron7@example.com';
-- (b) xem trạng thái
SELECT id, email, status, suspension_reason, suspended_until FROM users WHERE email LIKE 'b1.%';
-- nhật ký mở khóa gần nhất
SELECT created_at, user_email, reference_id, details FROM system_logs
 WHERE action_type = 'ACCOUNT_REACTIVATED' ORDER BY created_at DESC LIMIT 5;
```

### 6.2 B2 · UC-CAT-01 / 14 / 15 — Danh mục, hoa hồng, bộ thuộc tính

**Storefront (khách chưa đăng nhập)**

| Mã | Thao tác | Kết quả mong đợi |
|---|---|---|
| TC-B2-01 | Trang chủ → menu **Danh mục** ở header | Cây 3 cấp, có icon, tên đúng CSDL; không có ngành đang ẩn (so `SELECT name FROM categories WHERE NOT is_active`) |
| TC-B2-02 | Bấm một ngành gốc (`/c/dien-tu`) | Tiêu đề, mô tả, breadcrumb "Trang chủ › Điện tử", chip ngành con, số sản phẩm (= `product_count` của API 5.2); DevTools → Network có `/api/v1/public/categories` |
| TC-B2-03 | Mở `/c/thiet-bi-am-thanh` (ngành con) | `301` về `/c/dien-tu?sub=thiet-bi-am-thanh`, lưới chỉ có sản phẩm của ngành con |
| TC-B2-04 | Mở `/c/khong-ton-tai` | Trang 404 thật (mã HTTP 404) |
| TC-B2-05 | View-source `/c/dien-tu` | Có JSON-LD `BreadcrumbList`, đúng 1 `<h1>` |

**Admin (`yen.dh@genie.example`) — `/admin/catalog`**

| Mã | Thao tác | Kết quả mong đợi |
|---|---|---|
| TC-B2-06 | Mở màn | 4 chỉ số: Danh mục gốc / Danh mục con khớp SQL (c); Tổng sản phẩm / Chờ duyệt đề xuất khớp `summary` của `GET /admin/categories` (Network) |
| TC-B2-07 | **Thêm danh mục** "Thú cưng thử", slug **để trống**, icon, mô tả, hoa hồng **6** → Lưu | Tạo được, slug tự sinh `thu-cung-thu`; tải lại header storefront → có ngay "Thú cưng thử" (cache xóa sau commit) |
| TC-B2-08 | Thêm con "Thức ăn cho chó thử" (cha = Thú cưng thử), **bỏ trống** hoa hồng | Tạo được; ô hoa hồng hiện "kế thừa 6 %" (`effective_commission_rate`) |
| TC-B2-09 | Thêm cấp 3 dưới "Thức ăn cho chó thử" → rồi thử thêm cấp **4** | Cấp 3 tạo được; cấp 4 bị chặn, lỗi hiện tại ô Danh mục cha (`GNE-VAL-4220`, cây tối đa 3 cấp) |
| TC-B2-10 | Thêm ngành slug `dien-tu` | Lỗi **tại ô slug**: đã tồn tại, nêu tên "Điện tử" (`GNE-CAT-4091`) |
| TC-B2-11 | Thêm ngành con tên "Thức ăn cho chó thử" lần 2 cùng cha / cùng tên nhưng khác cha | Cùng cha → lỗi tại ô tên (`4091`); khác cha → tạo được |
| TC-B2-12 | Sửa hoa hồng = **12**, rồi **3** | Lỗi tại ô: ngoài khung 3,5–8 % |
| TC-B2-13 | Sửa "Thú cưng thử": chọn cha = chính ngành con của nó | Lỗi tại ô cha (vòng lặp — `CYCLE`) |
| TC-B2-14 | Bật công tắc **"Ngành cần giấy tờ"** → Lưu → mở lại | Công tắc giữ trạng thái bật (`requires_business_license = true` trong CSDL) |
| TC-B2-15 | **Ẩn** "Thú cưng thử" → tải lại storefront | Cả nhánh (cấp 2, 3) biến mất khỏi menu; `/c/thu-cung-thu` → 404. **Hiện** lại → quay lại đầy đủ |
| TC-B2-16 | **Xóa** "Thú cưng thử" (còn con) | Hộp xác nhận "Hành động này không thể hoàn tác"; xác nhận → báo không xóa được vì còn danh mục con (`HAS_CHILDREN`) |
| TC-B2-17 | SQL (d) cho voucher tham chiếu ngành cấp 3 → xóa ngành đó | Báo không xóa được vì đang được voucher dùng (`IN_USE`); chạy SQL dọn (d') → xóa được |
| TC-B2-18 | Xóa "Điện tử" (có sản phẩm) — **chỉ bấm, ngành sẽ không bị xóa** | Báo còn sản phẩm (`HAS_PRODUCTS`); ngành vẫn còn |

**Bộ thuộc tính — `/admin/catalog/:id/attributes`**

| Mã | Thao tác | Kết quả mong đợi |
|---|---|---|
| TC-B2-19 | Ngành "Thú cưng thử": thêm `pet_size` "Kích cỡ thú cưng", ENUM [Nhỏ, Vừa, Lớn], lọc được, trục biến thể | Tạo được, hiện trong bảng |
| TC-B2-20 | Thêm `weight` NUMBER đơn vị `kg`; thêm `flavor` ENUM **không có giá trị**; thêm `note` TEXT **có đơn vị** | `weight` tạo được; 2 ca còn lại lỗi tại đúng ô (`GNE-VAL-4220`) |
| TC-B2-21 | Mở ngành con | Bảng **kế thừa (chỉ đọc)** có `pet_size`, ghi ngành gốc |
| TC-B2-22 | Ngành con thêm mã `pet_size` | Lỗi tại ô mã: trùng với ngành "Thú cưng thử" (`GNE-CAT-4095`) |
| TC-B2-23 | Sửa `pet_size`: thử đổi kiểu dữ liệu | Ô kiểu dữ liệu bị khóa (không đổi được sau khi tạo) |
| TC-B2-24 | Bật **bắt buộc** cho `pet_size` (ngành chưa có sản phẩm) | Lưu được, **không** có thông báo ân hạn |
| TC-B2-25 | Ngành `dien-tu` (có sản phẩm): thêm thuộc tính thử `b2_test` TEXT **bắt buộc** | Thông báo ân hạn **14 ngày** (`grace_period_days = 14`); sản phẩm cũ vẫn hiện ở `/c/dien-tu` |
| TC-B2-26 | **Ngừng dùng** `b2_test` | Thuộc tính về trạng thái ngừng, cờ bắt buộc tự tắt; dọn bằng SQL (e) |

**Hoa hồng — `/admin/commission`** và phân quyền

| Mã | Thao tác | Kết quả mong đợi |
|---|---|---|
| TC-B2-27 | Đổi mặc định toàn sàn 5 → 5,5; tắt hoa hồng riêng "Thú cưng thử"; đổi 2 ngành gốc khác → **Lưu một lần** | Lưu được; "Thú cưng thử" hiện 5,5 (mặc định); `/admin/audit-logs` lọc CONFIG có **đúng** số dòng `COMMISSION_UPDATED` đã đổi (từ → đến). Trả mặc định về 5 sau khi thử |
| TC-B2-28 | Đăng nhập **Moderator** `phat.nt` | Thấy menu Danh mục, **không** thấy Cấu hình hoa hồng; sửa tên ngành được; đổi ô hoa hồng → báo không có quyền (`GNE-SEC-4030`) |
| TC-B2-29 | Đăng nhập **Finance** `linh.tm` | Thấy Cấu hình hoa hồng, **không** thấy Danh mục; đổi được hoa hồng; gõ URL `/admin/catalog` → "không có quyền" |
| TC-B2-30 | Đăng nhập **CS** `truc.ht` → gõ cả 2 URL | Không có quyền ở cả hai |

**Seller (`mai.tran@gmail.com`)**

| Mã | Thao tác | Kết quả mong đợi |
|---|---|---|
| TC-B2-31 | `/seller/products/new` → ô chọn danh mục | 3 cấp lấy từ API (Network), có "Thú cưng thử" khi đang hiện, không có khi đang ẩn |
| TC-B2-32 | Dọn dữ liệu: xóa cấp 3 → cấp 2 → "Thú cưng thử" | Xóa được (`204`); storefront không còn |

**Giao diện:** TC-B2-06, 19, 27 kiểm thêm 9 theme (`?theme=…`) — chữ / nút / thông báo lỗi đọc được.

SQL hỗ trợ:
```sql
-- (c) số liệu 4 chỉ số
SELECT count(*) FILTER (WHERE parent_id IS NULL) roots, count(*) FILTER (WHERE parent_id IS NOT NULL) children FROM categories;
-- Tổng sản phẩm: so với summary.product_count của GET /admin/categories (API 11.11)
-- (d) cho voucher TECHZONE5 tham chiếu ngành cấp 3 (thay <id>)
INSERT INTO voucher_categories (voucher_id, category_id, created_at) SELECT id, <id>, now() FROM vouchers WHERE code = 'TECHZONE5';
-- (d') dọn
DELETE FROM voucher_categories WHERE category_id = <id>;
-- (e) dọn thuộc tính thử
DELETE FROM category_attributes WHERE code = 'b2_test';
```

### 6.3 B3 · UC-CAT-05 — Lọc "Shop có voucher" & bộ lọc thuộc tính (khách)

| Mã | Thao tác | Kết quả mong đợi |
|---|---|---|
| TC-B3-01 | `/search?q=tai nghe` → tick **Shop có voucher** | URL có `voucher=1`; mọi sản phẩm thuộc shop có voucher SHOP còn hiệu lực (seed: TechZone + shop D10); số "**N** sản phẩm" khớp SQL (f) |
| TC-B3-02 | Bỏ tick / bấm × trên chip | Danh sách trở lại như trước, URL bỏ `voucher` |
| TC-B3-03 | Tick voucher + khoảng giá + 4★ + sắp xếp Giá tăng dần | Thỏa **đồng thời** mọi điều kiện, giá tăng dần |
| TC-B3-04 | Tick voucher + chọn danh mục | Chỉ sản phẩm thuộc danh mục đó của shop có voucher |
| TC-B3-05 | Gõ sai "tai nge" + voucher | Vẫn ra kết quả (tầng trigram) và chỉ của shop có voucher |
| TC-B3-06 | Facet thương hiệu / nơi bán khi đang tick voucher | Số lượng từng thương hiệu / tỉnh đếm trên tập đã lọc |
| TC-B3-07 | SQL (g) tắt voucher của shop D10 → chờ ≤ 60 giây → tải lại TC-B3-01 | Sản phẩm của shop đó biến mất khỏi kết quả lọc (cache 60 giây — D6); SQL (g') bật lại |
| TC-B3-08 | Shop có voucher **đã tắt** / **hết lượt** (seed D10) | Không xuất hiện khi tick voucher |
| TC-B3-09 | Trang shop `/shop/<id>` → "Tìm trong shop" | **Không** có ô Shop có voucher (đang trong một shop) |
| TC-B3-10 | `/c/dien-tu` (D8 = Có) | Có ô Shop có voucher trong nhóm lọc, tick → lọc đúng như TC-B3-01 trong phạm vi danh mục; D8 = Không → không có ô (đúng ghi chú lệch giao diện) |
| TC-B3-11 | `/c/dien-tu` → nhóm **thuộc tính** (vd. Bảo hành) | Giá trị + số lượng lấy từ API (Network: `/public/products?…include_facets=true`, **không** có request `attribute-filters`); tick một giá trị → URL `attr=…`, request gửi `attr.<code>=…`, kết quả đúng thuộc tính |
| TC-B3-12 | Ngành không có thuộc tính `is_filterable` | Không hiện nhóm thuộc tính |
| TC-B3-13 | Thuộc tính lọc được vừa tạo ở TC-B2-19 cho ngành có sản phẩm (tạo tạm ở `dien-tu`, `is_filterable`, gán giá trị bằng SQL vào `specs_json` một sản phẩm) | Hiện trong nhóm thuộc tính của `/c/dien-tu` (UC-CAT-15 bước 4); dọn sau khi thử |
| TC-B3-14 | Khổ hẹp (≤ 768 px): mở ngăn kéo bộ lọc, tick voucher → Áp dụng | Như TC-B3-01; chip hiện trên thanh chip |
| TC-B3-15 | Tắt JS (DevTools) → tick voucher → bấm **Áp dụng** | Gửi form thường, SSR trả kết quả đã lọc (bộ lọc theo URL params) |
| TC-B3-16 | 9 theme tại `/search` và `/c/dien-tu` | Ô / chip voucher và nhóm thuộc tính đọc được ở mọi theme |

SQL hỗ trợ:
```sql
-- (f) số sản phẩm đang bán của shop có voucher SHOP còn hiệu lực (khớp D5)
SELECT count(*) FROM products p
 WHERE p.status = 'PUBLISHED' AND p.vendor_id IN (
   SELECT DISTINCT vendor_id FROM vouchers WHERE type = 'SHOP' AND is_active AND vendor_id IS NOT NULL
      AND valid_from <= now() AND valid_to > now() AND used_count < usage_limit
      AND (budget IS NULL OR total_discount_used < budget));
-- (g) tắt / (g') bật voucher thử của shop thứ hai (mã theo D10)
UPDATE vouchers SET is_active = false WHERE code = '<MÃ D10>';
UPDATE vouchers SET is_active = true  WHERE code = '<MÃ D10>';
```
*(Với `q`, số ở TC-B3-01 so với cùng truy vấn API `GET /public/products?q=…&has_shop_voucher=true` — `pagination.total_items`.)*

---

## 7. Điều kiện xong (Definition of Done)

| UC | Điều kiện để chuyển ✅ |
|---|---|
| UC-ADM-03 | H03 xanh (11 ca IT + unit), TC-B1-01 → 12 Đạt, LLD / API / Usecase đã sửa |
| UC-CAT-01 | TC-B2-01 → 05, 15, 31 Đạt; H06.03, H06.04 xanh |
| UC-CAT-14 | TC-B2-06 → 18, 27 → 30, 32 Đạt; `CatalogSecurityIT` xanh |
| UC-CAT-15 | TC-B2-19 → 26 Đạt (+ TC-B3-13) |
| UC-CAT-05 | H08 xanh, TC-B3-01 → 16 Đạt, web không còn mock `attribute-filters`, `has_shop_voucher_supported = true` |

Chung: `./mvnw clean verify` + `npm run verify` + Playwright xanh; không còn mục `☐` ở progress (trừ task được đánh "không cần" có lý do).

## 8. Rủi ro

| Rủi ro | Cách xử lý |
|---|---|
| Cron và đăng nhập cùng mở một tài khoản | Khóa dòng (`SKIP LOCKED` + `findByIdForUpdate` ở luồng đăng nhập) — IT ca 7, 8 |
| Email lỗi làm hỏng mở khóa | Gửi sau commit, lỗi chỉ log (ca 10) |
| Cache voucher làm kết quả lọc trễ | Chấp nhận ≤ 60 giây (D6); B5 xóa khóa khi tạo / sửa voucher — ghi vào CLAUDE.md mục 9 |
| Đổi seed làm `SeedDataIT` đỏ | Sửa seed + test cùng commit (CLAUDE.md mục 9) |
| Sửa code của Khoa / Quân khi họ đang làm cùng file | Báo trước trong nhóm (H00.01); mỗi cụm một nhánh, PR nhỏ |
| Ma trận quyền lộ lệch spec | Dừng ở H07, chốt với Khoa trước khi sửa |
| Máy dev chỉ có JDK 24 | Luôn đặt `JAVA_HOME` JDK 21 trước khi chạy Maven |
