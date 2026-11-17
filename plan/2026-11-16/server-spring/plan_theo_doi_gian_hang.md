# PLAN — Khách hàng theo dõi gian hàng (UC-SOC-06)

| | |
|---|---|
| **Ngày lập** | 2026-11-16 |
| **Thể loại** | server-spring (module `social`, feature `follow` + cổng nhỏ ở module `vendor`) + nối `web/` (feature `shop`, `account`) |
| **Use case** | Khách hàng theo dõi gian hàng — Shop Follow (UC-SOC-06) — Sprint 8 theo implement 1.2, làm sớm vì độc lập |
| **Tài liệu chuẩn** | UC phân hệ 08 (UC-SOC-06) · API 3.11, 0.3, 0.4, 1 (`$ShopMini`), 15, 17.5 b · ENTITY 9.2 (`ShopFollow`), 1.4 (`VendorProfile.followerCount`) · implement 8.4 · TS 4.4, 8, 9, 10.3, 14 — xem [README](../README.md) |
| **Phụ thuộc** | Shop đã KYC để thử tay: plan [seed](../db/plan_seed_du_lieu_mau.md) S03 (test tự động **không** phụ thuộc seed) |
| **Theo dõi tiến độ** | [progress_theo_doi_gian_hang.md](./progress_theo_doi_gian_hang.md) — cùng mã task `Fxx.yy` |
| **Người commit** | danh tính git đã cấu hình trong repo — không `--author`, không `Co-Authored-By` |

---

## 0. Hiện trạng đã khảo sát

| Phần | Có gì | Thiếu |
|---|---|---|
| `social/follow/domain` | `ShopFollow` (`customerId`, `vendorId` — đều là `users.id`; `UNIQUE (customer_id, vendor_id)`; factory `of(customerId, vendorId, now)`), `ShopFollowRepository` (rỗng) | `api`, `application`; truy vấn |
| `vendor/shop/domain` | `VendorProfile` (`userId`, `shopName`, `logoUrl`, `isMall`, `ratingAvg`, `followerCount`, `kycStatus`…), `VendorProfileRepository` (rỗng) | Module `vendor` chưa có tầng `application` nào — chưa có cách cho module khác tra shop / cập nhật `follower_count` |
| CSDL | `shop_follows` (`V9__review.sql`), `vendor_profiles.follower_count` (`V1`) | — (không cần migration) |
| `common/error/ErrorCode` | Chưa có mã cho "tự theo dõi shop mình" | Thêm 1 mã mới (D3) |
| `web/features/shop` | `shop.api.ts`: `listFollowedShops` gọi `GET /me/shop-follows`, `setFollow` gọi `POST /me/shop-follows` / `DELETE /me/shop-follows/{id}` — **lệch spec** (API 17.5 b: `GET /me/following`, `PUT / DELETE /me/following/{shop_id}`, `GET /me/following/shop-ids`); nút Theo dõi ở trang Shop + trang chi tiết sản phẩm; hộp "Hủy theo dõi Shop?" | Sửa đường dẫn / phương thức; nối thật |
| `web/features/account` | Mục Tài khoản › Shop đang theo dõi (`/account/following`) | Gọi `GET /me/following` (có `summary.total`) |

---

## 1. Nguyên tắc bắt buộc

| # | Nguyên tắc | Nguồn |
|---|---|---|
| N1 | Package `com.genie.social.follow.{api,application,domain}`; một thao tác = một method `@Transactional` ở `application` | TS 4.1–4.2 |
| N2 | **Giữa module:** `social` không import `vendor..domain`. Tra shop và đổi `follower_count` đi qua **cổng** khai ở `social.follow.application`, module `vendor` cài đặt; chỉ truyền ID + record | TS 4.4 (ArchUnit) |
| N3 | Theo dõi / hủy **idempotent** và **không đếm sai**: chỉ cộng `follower_count` khi dòng `shop_follows` thật sự được chèn, chỉ trừ khi thật sự xóa — cả hai trong **cùng transaction** | UC-SOC-06 ngoại lệ; API 3.11 |
| N4 | `follower_count` cập nhật bằng `UPDATE … SET follower_count = follower_count ± 1` nguyên tử (không đọc-rồi-ghi), không âm | TS 10.3 |
| N5 | Chỉ thao tác trên danh sách của chính người gọi (`userId` từ `@CurrentUser`) | TS 9 |
| N6 | `shop_id` trên API = `vendor_profiles.id`; `shop_follows.vendor_id` = `users.id` của chủ shop — chuyển đổi nằm trong cài đặt cổng phía `vendor` | README mục 3; ENTITY 9.2 |
| N7 | JSON snake_case, vỏ `ApiResponse`, phân trang cursor; `@Operation(summary = "UC-SOC-06 …")` | TS 8 |
| N8 | Test: unit + `*IT` Testcontainers (PG) có ca đồng thời; `ModuleBoundaryTest` xanh | TS 14 |

---

## 2. Quyết định cần chốt trước khi làm

| # | Vấn đề | Nguồn lệch | Đề xuất | Chốt |
|---|---|---|---|---|
| D1 | `shop_id` là id nào | API không ghi rõ; ENTITY lưu `vendor_id = users.id` | `shop_id` = `vendor_profiles.id` (cùng giá trị API 9.2 trả sau khi mở shop). Ghi rõ vào API 0 / 3.11 | ☐ |
| D2 | UC-SOC-06 bước 1: `POST /api/v1/shops/{vendorId}/follow` (bật / tắt); API 3.11: `PUT` / `DELETE /me/following/{shop_id}` + `GET /me/following`, `GET /me/following/shop-ids` | UC vs API | Theo **API**; sửa câu trong Usecase.md (F05.03) | ☐ |
| D3 | "Không theo dõi chính shop của mình" chưa có mã lỗi | UC tiền điều kiện vs API 15 | Mã mới **`GNE-SOC-4095`** (409) "Không thể theo dõi shop của chính mình"; thêm vào API 15, `ErrorCode`, `error-codes.ts`, `vi.json` / `en.json`. Giao diện vốn ẩn nút ở shop của mình | ☐ |
| D4 | Shop nào theo dõi được | UC-CAT-16 tiền điều kiện | Chỉ shop `kyc_status = VERIFIED` và tài khoản chủ shop `ACTIVE`; không thỏa → `404 GNE-SYS-4040`. Shop đã theo dõi rồi mới bị khóa vẫn hủy theo dõi được và **không** hiện trong danh sách | ☐ |
| D5 | Module `vendor` chưa có tầng application; plan Mở shop & KYC sở hữu `ShopFacade` | README mục 3 | Plan này tạo **một lớp cài đặt cổng** trong `vendor`: `vendor/shop/application/ShopFollowDirectoryImpl` + thêm truy vấn vào `VendorProfileRepository` — không tạo `ShopFacade`. Khi `ShopFacade` có, lớp này gọi lại nó (ghi chú trong Javadoc) | ☐ |
| D6 | Bước 5 UC: gửi `NEW_VENDOR_VOUCHER` / `FLASH_SALE_REMINDER` tới người theo dõi | UC-SOC-06 | **Ngoài phạm vi** (cần phân hệ thông báo + voucher / Flash Sale); cung cấp `ShopFollowFacade.followerIdsOf(vendorId)` để phân hệ sau dùng | ☐ |
| D7 | `GET /me` → `counters.following_shops` | API 3.1 | Ngoài phạm vi (`GET /me` chưa hiện thực — plan AUTH A11.02); cung cấp `ShopFollowFacade.countFollowing(userId)` để cài `AccountCounterContributor` sau | ☐ |

---

## 3. Cây file sẽ tạo / sửa

```
server-spring/src/main/java/com/genie/
├── common/error/ErrorCode.java                 (sửa) + CANNOT_FOLLOW_OWN_SHOP GNE-SOC-4095
├── social/follow/
│   ├── domain/       (đã có) ShopFollow · ShopFollowRepository (sửa: truy vấn)
│   ├── application/  ShopFollowService · ShopFollowQueryService · ShopFollowFacade (D6, D7)
│   │                 ShopFollowDirectory (cổng — N2) · FollowableShop / FollowedShopView (record)
│   └── api/          MeFollowingController · FollowedShopResponse (record)
└── vendor/shop/
    ├── domain/VendorProfileRepository.java      (sửa: tra theo id / userId, tăng giảm follower_count)
    └── application/ShopFollowDirectoryImpl.java (mới — cài cổng, D5)
server-spring/src/test/java/com/genie/social/
    ShopFollowIT · ShopFollowQueryIT · ShopFollowConcurrencyIT
web/src/features/shop/infrastructure/shop.api.ts     (sửa)
web/src/features/account/…                           (sửa — trang Shop đang theo dõi)
web/src/shared/api/error-codes.ts · web/src/i18n/{vi,en}.json   (sửa — GNE-SOC-4095)
docs/Document/…  Usecase.md · API_SPEC 3.11, 15, 17.5 · ENTITY Phụ lục E · CHECKLIST_TIEN_DO_UC.md · Quản Lý UseCase.xlsx
docs/web-backend-handoff.md · docs/api-tests/shop-follow.http
```

---

## 4. Bảng ánh xạ Use case → API → Dữ liệu

| Bước UC | Endpoint (API 3.11) | Bảng |
|---|---|---|
| Tô trạng thái nút Theo dõi | `GET /me/following/shop-ids` → `[15, 17, 12]` | `shop_follows`, `vendor_profiles` (đổi `vendor_id` → `shop_id`) |
| Bấm "Theo dõi" | `PUT /me/following/{shop_id}` → `204` | `shop_follows` (chèn), `vendor_profiles.follower_count` (+1) |
| Xác nhận "Hủy theo dõi Shop?" | `DELETE /me/following/{shop_id}` → `204` | `shop_follows` (xóa), `vendor_profiles.follower_count` (−1) |
| Tài khoản › Shop đang theo dõi | `GET /me/following?cursor&limit=20` (+ `summary.total`) | `shop_follows`, `vendor_profiles` (đọc) |

**Mã lỗi thêm vào `ErrorCode`:** **`GNE-SOC-4095`** (409 — mới, D3). Dùng lại: `GNE-SYS-4040` (shop không tồn tại / chưa `VERIFIED`), `GNE-SEC-4010`, `GNE-VAL-4220`.
**Không có migration, không thêm `UploadPurpose` / `AuditAction`.**

---

## 5. Nhánh, commit, PR

- Nhánh: `feature/social-shop-follow` tách từ `develop` (có mã Jira: `feature/GNE-<số>-social-shop-follow`). Một PR backend + một PR web.
- Scope commit: `common`, `vendor` (cổng + repository), `social`, `web`, `docs`.
- Quy ước chung: [README mục 5](../README.md).

## 6. Cổng kiểm tra trước mỗi task `COMMIT`

1. `cd server-spring && ./mvnw verify` — unit + IT + **ArchUnit `ModuleBoundaryTest`** (không vòng phụ thuộc `social` ↔ `vendor`).
2. Swagger UI hiện 4 endpoint, summary có `UC-SOC-06`.
3. Thử tay `docs/api-tests/shop-follow.http`; sau mỗi kịch bản: `follower_count` của shop = `SELECT count(*) FROM shop_follows WHERE vendor_id = …` (với shop tạo trong test; shop seed có số ban đầu lớn hơn — so chênh lệch).
4. Nếu đụng `web/`: `cd web && npm run verify`.
5. Tick task ở progress, ghi mã commit vào Nhật ký.

---

## 7. Danh sách task theo giai đoạn

Ký hiệu: `Fxx.yy` = task; `Fxx.Cn` = task commit.

### F00 — Chuẩn bị

- **[F00.01]** Kiểm danh tính git — `git config user.name` / `user.email`.
- **[F00.02]** Chốt quyết định D1–D7 — ghi vào đầu file progress.
- **[F00.03]** Tạo nhánh — `git switch develop && git pull && git switch -c feature/social-shop-follow`.
- **[F00.04]** Kiểm trạng thái plan Mở shop & KYC — nếu `ShopFacade` đã có trên `develop` thì `ShopFollowDirectoryImpl` gọi nó thay vì truy vấn trực tiếp (D5); ghi lựa chọn vào progress.
- **[F00.05]** Đọc màn mock — nút Theo dõi / Đang theo dõi ở `/shop/{id}` và trang chi tiết sản phẩm, hộp xác nhận hủy, `/account/following` (ảnh, nhãn Chính hãng, số người theo dõi, nút "Vào shop") — ghi trường giao diện cần.

### F01 — Cổng tra shop & truy vấn

- **[F01.01]** `ErrorCode` — thêm `GNE-SOC-4095` (409); cập nhật API 15 trong `docs/Document` (cùng commit tài liệu ở F05).
- **[F01.02]** `ShopFollowDirectory` (cổng ở `social.follow.application`) — `Optional<FollowableShop> findFollowable(long shopId)` (chỉ shop `VERIFIED` + chủ `ACTIVE` — D4), `Optional<Long> vendorUserIdOf(long shopId)` (không lọc trạng thái — dùng khi hủy), `Map<Long, FollowedShopView> findByVendorUserIds(Collection<Long>)`, `void adjustFollowerCount(long vendorUserId, int delta)`. Record `FollowableShop(shopId, vendorUserId)`, `FollowedShopView(shopId, name, logoUrl, isMall, ratingAvg, followerCount)`.
- **[F01.03]** `VendorProfileRepository` (scope `vendor`) — `findById`, `findByUserIdIn`, `@Modifying` `adjustFollowerCount(userId, delta)`: `UPDATE vendor_profiles SET follower_count = GREATEST(follower_count + :delta, 0) WHERE user_id = :userId` (N4).
- **[F01.04]** `ShopFollowDirectoryImpl` (ở `vendor.shop.application`) — cài cổng; kiểm trạng thái chủ shop qua `UserFacade.isActive` (module `auth`, đã có); ghép `logoUrl` thành URL công khai nếu cột lưu khóa đối tượng.
- **[F01.05]** `ShopFollowRepository` — `insertIfAbsent(customerId, vendorId, now)` (native `ON CONFLICT (customer_id, vendor_id) DO NOTHING`, trả số dòng — N3); `deleteByCustomerIdAndVendorId` (trả số dòng); `findVendorIdsByCustomerId`; trang keyset `(created_at DESC, id DESC)`; `countByCustomerId`; `findCustomerIdsByVendorId` (D6).
- **[F01.06]** `ModuleBoundaryTest` — chạy lại, xác nhận `social` chỉ phụ thuộc cổng của chính nó, `vendor` phụ thuộc `social..application` (cổng) — nếu luật hiện tại cấm chiều này thì khai cổng theo đúng mẫu ADR 0008 (cổng ở module dùng, module sở hữu dữ liệu cài) và ghi vào Ghi chú.
- **[F01.C1]** COMMIT — `feat(vendor): expose shop lookup and follower counter for shop follow`

### F02 — Theo dõi / hủy theo dõi

- **[F02.01]** `ShopFollowService.follow(userId, shopId)` — `findFollowable` không có → `404 GNE-SYS-4040` (D4); `vendorUserId == userId` → `409 GNE-SOC-4095` (D3); `insertIfAbsent` trả 1 → `adjustFollowerCount(+1)`; trả 0 (đã theo dõi) → không làm gì (N3).
- **[F02.02]** `ShopFollowService.unfollow(userId, shopId)` — `vendorUserIdOf` không có → coi như đã hủy (`204`); xóa trả 1 → `adjustFollowerCount(−1)`; trả 0 → không làm gì.
- **[F02.03]** `ShopFollowQueryService.shopIds(userId)` — đổi `vendor_id` → `shop_id` qua cổng; chỉ trả shop còn theo dõi được (D4).
- **[F02.04]** `MeFollowingController` — `GET /api/v1/me/following/shop-ids`, `PUT /api/v1/me/following/{shop_id}` (`204`), `DELETE /api/v1/me/following/{shop_id}` (`204`); `@Operation(summary = "UC-SOC-06 …")`; `Cache-Control: no-store`.
- **[F02.05]** `ShopFollowIT` — theo dõi → có dòng + `follower_count` +1; `PUT` lần 2 không cộng thêm; hủy → −1; `DELETE` lần 2 không trừ thêm; không âm khi số đang 0; theo dõi shop của chính mình → `409 GNE-SOC-4095`; shop chưa `VERIFIED` / không tồn tại → `404`; shop bị khóa sau khi đã theo dõi vẫn hủy được; chưa đăng nhập → `401`.
- **[F02.06]** `ShopFollowConcurrencyIT` — 20 luồng của **cùng** một user `PUT` cùng shop → `follower_count` +1 đúng một lần; 20 user khác nhau cùng `PUT` → +20; xen kẽ `PUT` / `DELETE` kết thúc ở trạng thái nhất quán (`follower_count` = số dòng thực).
- **[F02.C1]** COMMIT — `feat(social): follow and unfollow shops without miscounting followers`

### F03 — Danh sách shop đang theo dõi

- **[F03.01]** `ShopFollowQueryService.list(userId, cursor, limit)` — trang `shop_follows` theo keyset; nạp thông tin shop **một lần** qua `findByVendorUserIds` (tránh N+1); mỗi phần tử `{shop: $ShopMini, rating_avg, follower_count}` (API 3.11); bỏ shop không còn hiển thị được (D4).
- **[F03.02]** `summary.total` = số shop đang theo dõi còn hiển thị được.
- **[F03.03]** `MeFollowingController` — `GET /api/v1/me/following?cursor&limit=20` (`limit` 1–50).
- **[F03.04]** `ShopFollowFacade` — `followerIdsOf(vendorUserId)` (D6), `countFollowing(userId)` (D7); Javadoc ghi module nào sẽ dùng.
- **[F03.05]** `ShopFollowQueryIT` — thứ tự mới theo dõi trước; `total` đúng; đủ trường `$ShopMini` (`is_mall`, `logo_url`); phân trang 2 trang không trùng; shop bị đình chỉ không xuất hiện; danh sách của user A không lẫn user B.
- **[F03.C1]** COMMIT — `feat(social): list followed shops with rating and follower count`

### F04 — Nối giao diện `web/`

- **[F04.01]** `shop/infrastructure/shop.api.ts` — `listFollowedShops`: `GET /me/shop-follows` → `GET /me/following` (DTO mới `{shop, rating_avg, follower_count}` + mapper); `setFollow`: `PUT /me/following/{shop_id}` / `DELETE /me/following/{shop_id}`; thêm `fetchFollowedShopIds` (`GET /me/following/shop-ids`) nếu đang suy ra từ danh sách; thay `USE_MOCK` bằng `isMocked('follow')` cho nhóm hàm theo dõi (hàm `getShop` / `listShops` công khai **giữ mock** tới khi có API trang shop).
- **[F04.02]** `error-codes.ts` + `vi.json` / `en.json` — thêm `GNE-SOC-4095`; `check-i18n` xanh.
- **[F04.03]** Trạng thái & lỗi — cập nhật lạc quan nút Theo dõi + số người theo dõi; lỗi thì trả lại + toast; `404` → "Shop không còn hoạt động"; chưa đăng nhập → lời nhắc đăng nhập.
- **[F04.04]** Trang `/account/following` dùng `summary.total` cho tiêu đề "Shop đang theo dõi (n)".
- **[F04.05]** Chạy thật hai phía — `PUBLIC_REAL_API=auth,follow`: đăng nhập `mai.tran@gmail.com` (seed đang theo dõi 3 shop) → danh sách đúng; theo dõi / hủy ở trang shop (trang shop còn mock thì kiểm nút + gọi API thật qua tab Network); kiểm `follower_count` trong CSDL.
- **[F04.06]** `cd web && npm run verify`.
- **[F04.C1]** COMMIT — `fix(web): call shop follow endpoints defined in the api spec`

### F05 — Hoàn thiện use case

- **[F05.01]** `docs/api-tests/shop-follow.http` — kịch bản đúng (theo dõi → ids → danh sách → hủy) + lỗi (`409 4095`, `404`, `401`).
- **[F05.02]** OpenAPI — kiểm `/v3/api-docs`.
- **[F05.03]** Tài liệu nguồn — Usecase.md UC-SOC-06: bước 1 theo API (D2), dòng *Hiện trạng*; API 3.11 ghi `shop_id` = `vendor_profiles.id` (D1), điều kiện shop theo dõi được (D4); API 15 thêm `GNE-SOC-4095`; 17.5 b đánh dấu dòng `/me/shop-follows` đã sửa; lịch sử phiên bản.
- **[F05.04]** Tiến độ — `CHECKLIST_TIEN_DO_UC.md` (tick 5 lớp, Nhật ký), `Quản Lý UseCase.xlsx` → "Đã triển khai"; `docs/web-backend-handoff.md`; `CLAUDE.md` mục 2 + mục 9 (cổng `ShopFollowDirectory` do `vendor` cài).
- **[F05.C1]** COMMIT — `docs(social): record shop follow use case as implemented`
- **[F05.05]** Mở PR vào `develop` — mẫu PR (Ảnh hưởng CSDL: không có).

---

## 8. Ngoài phạm vi

- Gửi thông báo voucher / Flash Sale mới tới người theo dõi (D6); bộ đếm `following_shops` trong `GET /me` (D7).
- Trang shop công khai `GET /public/shops/{id}` (Xem trang gian hàng — UC-CAT-16, cụm B3) — nút Theo dõi dùng API thật nhưng dữ liệu trang shop vẫn mock.
- Job đối soát `follower_count` với `shop_follows` — không cần vì cập nhật cùng transaction; nếu sau này lệch thì thêm job đêm (DATN).

## 9. Rủi ro & cách xử lý

| Rủi ro | Xử lý |
|---|---|
| Bấm liên tục làm `follower_count` lệch | Chỉ cộng / trừ khi dòng thật sự chèn / xóa, cùng transaction, `UPDATE` nguyên tử (N3, N4); test đồng thời (F02.06) |
| Vòng phụ thuộc `social` ↔ `vendor` | Cổng khai ở `social`, `vendor` cài (F01.06); ArchUnit chặn |
| Trùng việc với plan Mở shop & KYC (cùng sửa `VendorProfileRepository`) | Chỉ **thêm** phương thức; rebase trước khi mở PR; ghi ở F00.04 |
| Shop seed có `follower_count` lớn (128.000) khác số dòng `shop_follows` | Đúng thiết kế seed (số liệu hiển thị); test dùng shop tự tạo với số 0 |
| Lẫn `shop_id` và `vendor_id` | Quy ước D1 + record `FollowableShop` tách rõ hai giá trị; test có shop mà `vendor_profiles.id ≠ users.id` |
