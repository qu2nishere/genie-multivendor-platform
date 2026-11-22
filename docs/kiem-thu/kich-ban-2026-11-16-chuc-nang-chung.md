# Kịch bản kiểm thử — Plan 2026-11-16 · Phần 1: chức năng chung

> Phạm vi: dữ liệu seed, Nhật ký hệ thống (UC-ADM-07), Banner (UC-ADM-12), Điểm Genie (UC-PRO-08/09),
> Sản phẩm yêu thích (UC-CAT-08), Theo dõi gian hàng (UC-SOC-06).
> Luồng Mở shop & KYC ở file riêng: [kich-ban-2026-11-16-mo-shop-kyc.md](kich-ban-2026-11-16-mo-shop-kyc.md).
>
> Chuẩn bị môi trường: [../huong-dan-chay-local.md](../huong-dan-chay-local.md) (Docker + backend + web, Mailpit, `web/.env` đủ cờ `PUBLIC_REAL_API`).
> Mật khẩu mọi tài khoản seed: `Genie@123`. OTP xem ở Mailpit http://localhost:8025.

## Cách dùng file này

- Mỗi kịch bản có mã `TC-xx-nn`, **Bước làm** và **Kết quả mong đợi**. Làm đúng thứ tự trong từng nhóm vì kịch bản sau có thể dùng dữ liệu kịch bản trước.
- Đạt → ghi `☑` vào bảng tổng hợp cuối file; sai → ghi `☒` kèm mô tả + ảnh chụp / log backend.
- Mỗi lần thao tác, mở sẵn **DevTools → Network** để xem request có gọi `localhost:8080` thật (không phải mock) và mã lỗi `GNE-*` trả về.
- Phần giao diện chưa nối thật (ghi rõ ở từng mục) → test bằng **Swagger** http://localhost:8080/swagger-ui.html hoặc file `.http` trong [../api-tests/](../api-tests/) (VS Code extension *REST Client*).
- Test lại từ đầu với dữ liệu sạch: `docker compose --env-file .env -f deploy/docker-compose.local.yml down -v` → `up -d` → chạy lại backend → `upload-assets.ps1`.
- Cột **API (2026-11-22)** ở bảng tổng hợp: kết quả đã chạy bằng API thật + kiểm DB. Cột **Chạy** chỉ tick khi chính bạn bấm trên giao diện thấy đúng.

---

## 0. Dữ liệu seed & khởi động

| Mã | Bước làm | Kết quả mong đợi |
|---|---|---|
| TC-00-01 | Chạy backend lần đầu trên DB trống | Log Flyway chạy hết `V…` (develop: tới `V20`), sau đó các `R__seed_*`; không lỗi; `Started GenieApplication` |
| TC-00-02 | Mở http://localhost:8080/actuator/health | `{"status":"UP"}` |
| TC-00-03 | Chạy `upload-assets.ps1`, mở Silo console http://localhost:9001 (`siloadmin` / `changeme`) | Bucket `product-media` (22 tệp), `content-assets` (15 tệp) có ảnh |
| TC-00-04 | Đăng nhập web lần lượt 5 tài khoản seed | Đăng nhập được; Admin / Moderator / Finance / CS vào thẳng `/admin`, menu hiện theo quyền; có nút **Đăng xuất** cuối sidebar |
| TC-00-05 | Dừng backend, chạy lại lần 2 | Seed không chạy lại (checksum không đổi), không lỗi, không nhân đôi dữ liệu |

---

## 1. UC-ADM-07 — Nhật ký hệ thống (Admin)

Tài khoản: `yen.dh@genie.example` (có quyền `VIEW_AUDIT_LOG`). Màn: `/admin/audit-logs`.

**Tạo dữ liệu trước:** ở cửa sổ ẩn danh đăng nhập sai mật khẩu `mai.tran@gmail.com` 1–2 lần, rồi đăng nhập đúng 1 lần.

| Mã | Bước làm | Kết quả mong đợi |
|---|---|---|
| TC-01-01 | Vào `/admin/audit-logs` | Bản ghi mới nhất ở trên: có `LOGIN_FAILED`, `LOGIN_SUCCESS` vừa tạo, kèm email, IP, thời gian giờ VN |
| TC-01-02 | Chọn khoảng ngày *từ – đến* = hôm nay | Chỉ còn bản ghi hôm nay |
| TC-01-03 | Chọn ngày *đến* nhỏ hơn *từ* | Báo lỗi khoảng ngày không hợp lệ, không gọi được dữ liệu |
| TC-01-04 | Lọc nhóm **AUTH** | Chỉ còn LOGIN_*, PASSWORD_*, OAUTH_*… |
| TC-01-05 | Lọc nhóm **KYC** (sau khi làm file Mở shop & KYC) | Có KYC_SUBMITTED / KYC_APPROVED / KYC_REJECTED / KYC_SUPPLEMENT_REQUESTED / KYC_DOCUMENT_VIEWED |
| TC-01-06 | Ô tìm kiếm gõ `mai.tran`; sau đó gõ IP đang hiện ở cột IP (máy dev thường `0:0:0:0:0:0:0:1` — IPv6 của localhost, tương đương `127.0.0.1`); sau đó gõ một mã tham chiếu | Mỗi lần chỉ còn bản ghi khớp email / IP / mã tham chiếu |
| TC-01-07 | Phân trang: cần **trên 50** bản ghi (mỗi lần tải 50). Ít dữ liệu thì tạo 60 dòng giả bằng SQL ở cuối mục → tải lại → bấm **Tải thêm** | Lần đầu 50 dòng + nút **Tải thêm**; bấm thì nối thêm, không trùng; hết dữ liệu nút biến mất. Dưới 50 bản ghi → **không có nút** (đúng) |
| TC-01-08 | Bấm **Xuất CSV** | Tải `.csv`; mở Excel tiếng Việt không lỗi font, đúng bộ lọc |
| TC-01-09 | Xem cột **Chi tiết**: dòng `LOGIN_*` và dòng có đối tượng (`KYC_APPROVED`, `COMMISSION_UPDATED`, banner…) | `LOGIN_*` để **trống** (không có đối tượng — đúng thiết kế); dòng có đối tượng hiện tên (`details.target_label`) hoặc mã tham chiếu. Trên `develop` **không** có hộp chi tiết khi bấm dòng; nhánh `feature/audit-banner-granular-actions` có hộp chi tiết (mã tham chiếu, IP, user agent, bảng Trước → Sau) |
| TC-01-10 | Đăng nhập `phat.nt@genie.example` (Moderator) vào `/admin/audit-logs` | Menu không có mục Nhật ký; gõ URL trực tiếp → "không có quyền"; API 403 và **sinh thêm 1 bản ghi `ACCESS_DENIED`** (kiểm bằng Admin) |

Tạo 60 dòng giả cho TC-01-07 (DataGrip / psql, DB `genie`):

```sql
INSERT INTO system_logs (id, created_at, user_id, user_email, user_role, action_type, details, ip_address, status, user_agent)
SELECT gen_random_uuid()::text, now() - (g || ' minutes')::interval, NULL,
       'test.paging@genie.test', 'CUSTOMER', 'LOGIN_FAILED', 'Dòng thử phân trang #' || g,
       '127.0.0.1', 'FAILED', 'paging-test'
FROM generate_series(1, 60) g;
-- thử xong thì dọn:
DELETE FROM system_logs WHERE user_email = 'test.paging@genie.test';
```

---

## 2. UC-ADM-12 — Quản lý banner

Tài khoản: `linh.tm@genie.example` (Finance Officer, quyền `MANAGE_VOUCHER`) hoặc Admin. Màn: `/admin/banners`.
Chuẩn bị 1 ảnh jpg/png/webp < 5MB và 1 ảnh > 5MB hoặc 1 file `.gif`/`.pdf`.

| Mã | Bước làm | Kết quả mong đợi |
|---|---|---|
| TC-02-01 | Vào `/admin/banners` | Danh sách banner seed, lọc theo vị trí (HOME_TOP_CAROUSEL đầu trang chủ, HOME_TOP_SIDE ô nhỏ bên phải, HOME_MIDDLE giữa trang, CATEGORY_TOP trang danh mục, FLASH_SALE_TOP, POPUP) và trạng thái; cột **Hiển thị / Bấm** + **CTR** (= Bấm ÷ Hiển thị) |
| TC-02-02 | **Tạo banner**: HOME_TOP_CAROUSEL, ảnh hợp lệ, link `/flash-sale`, bắt đầu = bây giờ, kết thúc = +7 ngày, bật → Lưu. Ô **Thứ tự** tự điền = lớn nhất trong vị trí + 1 | Tạo thành công, trạng thái **Đang hiển thị** |
| TC-02-03 | Mở trang chủ (tab mới) | Banner mới trong carousel đầu trang (cache tối đa 60 giây — tải lại) |
| TC-02-04 | Ghi lại **Hiển thị / Bấm** → mở trang chủ 1 lần, bấm banner đó → quay lại admin sau **tối đa 5 phút**, tải lại | **Hiển thị** của *mọi* banner trong carousel +1 (carousel dựng sẵn mọi slide), **Bấm** của banner vừa bấm +1. Không tăng ngay vì đi qua bộ đếm Redis (`banner:<id>:imp/clk`) rồi `BannerStatsFlushJob` 5 phút / lần mới ghi DB |
| TC-02-05 | Tạo banner link ngoài `https://google.com` | **GNE-BNR-4222** (link phải nội bộ) |
| TC-02-06 | Ảnh > 5MB / sai định dạng | Bị chặn khi tải, báo dung lượng / định dạng |
| TC-02-07 | Kết thúc trước bắt đầu | Báo lỗi trường thời gian, không lưu |
| TC-02-08 | Tạo banner đang hoạt động cùng vị trí, **thời gian chồng nhau**, tới khi vượt **8** | Banner thứ 9 bị chặn **GNE-BNR-4221** (banner đặt lịch không chồng thời gian thì vẫn được) |
| TC-02-09 | Sửa banner TC-02-02: đổi ảnh / link / tắt | Lưu được; tắt → trang chủ không còn hiện (sau khi tải lại) |
| TC-02-10 | Đổi **Thứ tự** 2 banner cùng vị trí (số nhỏ hiện trước) | Thứ tự carousel đổi theo |
| TC-02-11 | Xóa banner **đang chạy / đã lên lịch** | **GNE-BNR-4091** |
| TC-02-12 | Tắt (hoặc banner đã hết hạn) rồi xóa | Xóa được |
| TC-02-13 | Tạo banner bắt đầu từ ngày mai | Trạng thái **Đã lên lịch**; trang chủ hôm nay **không** hiện (API công khai không trả) |
| TC-02-14 | `truc.ht@genie.example` (CS) vào `/admin/banners` | Không thấy menu; URL trực tiếp → không có quyền / 403 |
| TC-02-15 | Admin vào `/admin/audit-logs`, lọc **CONFIG** | Có bản ghi thao tác banner. `develop`: mã `BANNER_CHANGED` (loại thao tác nằm trong `details.op`); nhánh `feature/audit-banner-granular-actions`: mã riêng **Tạo / Sửa / Bật / Tắt / Xóa banner** |
| TC-02-16 | Ô "Danh mục" khi chọn liên kết tới danh mục | Danh mục con (có "—") nằm **ngay dưới** danh mục cha |

---

## 3. UC-PRO-08 / UC-PRO-09 — Điểm Genie: điểm danh & đổi voucher

Tài khoản: `mai.tran@gmail.com` (user id 1). Màn: `/account/points`, `/account/vouchers`.
Mốc điểm danh lưu ở bảng `daily_checkins` (`last_checkin_at`, `streak_count`), lịch sử Điểm ở `coin_transactions`.

| Mã | Bước làm | Kết quả mong đợi |
|---|---|---|
| TC-03-01 | Vào `/account/points` | Số dư Điểm, lịch 7 ngày (ngày 1–6: +100, ngày 7: +1000), lịch sử, Điểm sắp hết hạn (30 ngày tới) |
| TC-03-02 | Bấm **Điểm danh** | +100 (hoặc mức của ngày hiện tại trong chuỗi); số dư tăng; lịch sử có dòng `CHECK_IN`; hôm nay đánh dấu đã nhận |
| TC-03-03 | Bấm điểm danh lần 2 trong ngày | Nút khóa; gọi API → **409 GNE-PRO-4091** "Hôm nay bạn đã điểm danh rồi." |
| TC-03-04 | Mở 2 tab, bấm gần như cùng lúc | Chỉ 1 lần được cộng (khóa Redis `lock:checkin:<id>` 5 giây + ràng buộc DB) |
| TC-03-05 | Chuỗi (dùng SQL bên dưới): (a) lùi mốc về hôm qua → điểm danh; (b) lùi mốc về 2 ngày trước → điểm danh; (c) đặt chuỗi = 6 + mốc hôm qua → điểm danh | (a) chuỗi +1, +100 · (b) **đứt chuỗi**, về ngày 1, +100 · (c) ngày **7**, **+1000** |
| TC-03-06 | Phần **Đổi Điểm lấy voucher** (`GET /public/coin-rewards`) | 3 mức: 500 Điểm → giảm 5.000₫ (đơn từ 50k) · 1000 → 10.000₫ (từ 100k) · 2000 → 20.000₫ (từ 200k) |
| TC-03-07 | Đổi mức **500** khi đủ Điểm | Trừ 500; lịch sử dòng `VOUCHER_REDEEM` −500; `/account/vouchers` có **COIN500** |
| TC-03-08 | Đổi lại mức 500 khi voucher chưa dùng | **409 GNE-PRO-4092** "Bạn đang có voucher này trong Kho Voucher." |
| TC-03-09 | Đổi mức **2000** khi số dư < 2000 | Nút mờ hoặc **422 GNE-PRO-4222** "Bạn không đủ Điểm…"; số dư giữ nguyên |
| TC-03-10 | Đăng xuất, gọi `GET /public/coin-rewards` | Trả bảng thưởng, không cần đăng nhập |
| TC-03-11 | **FEFO**: ghi lại "Điểm sắp hết hạn" → đổi 500 → xem lại | Lô hết hạn **sớm nhất** bị trừ trước (seed: lô 150 Điểm hạn 11/12 → "sắp hết hạn" về 0) |
| TC-03-12 | **Hết hạn**: SQL đặt `expires_at` một lô về quá khứ (bên dưới), giữ backend chạy qua **00:15** | Sáng hôm sau: dòng `EXPIRED` âm đúng phần còn lại của lô, số dư giảm tương ứng. Kiểm nhanh không cần chờ: chạy `CoinExpiryJobIT` (`mvnw verify -Dit.test=CoinExpiryJobIT`) |

SQL hỗ trợ TC-03-05 / TC-03-12:

```sql
-- (a) cho điểm danh lại hôm nay, chuỗi +1
UPDATE daily_checkins SET last_checkin_at = now() - interval '1 day' WHERE customer_id = 1;
-- (b) đứt chuỗi
UPDATE daily_checkins SET last_checkin_at = now() - interval '2 days' WHERE customer_id = 1;
-- (c) lần bấm tới là ngày 7
UPDATE daily_checkins SET streak_count = 6, last_checkin_at = now() - interval '1 day' WHERE customer_id = 1;
-- trả lại voucher đổi Điểm để thử TC-03-07/08 lần nữa
DELETE FROM user_vouchers WHERE user_id = 1
  AND voucher_id IN (SELECT id FROM vouchers WHERE code IN ('COIN500','COIN1000','COIN2000'));
-- TC-03-12: cho lô cộng hết hạn (xem id ở: SELECT id, amount, type, expires_at FROM coin_transactions WHERE user_id = 1;)
UPDATE coin_transactions SET expires_at = now() - interval '1 hour' WHERE id = <id lô dương>;
```

---

## 4. UC-CAT-08 — Sản phẩm yêu thích

Tài khoản: `mai.tran@gmail.com`. Màn: `/account/wishlist`, nút ♥ trên thẻ / trang sản phẩm.

> ⚠ Catalog storefront **vẫn chạy mock** → id sản phẩm trên thẻ mock có thể không trùng DB. Bấm ♥ ở storefront mà báo **GNE-CAT-4042** thì lấy id thật:
> `SELECT id, name, status FROM products ORDER BY id LIMIT 10;` rồi test bằng [../api-tests/wishlist.http](../api-tests/wishlist.http) / Swagger. Màn `/account/wishlist` hiển thị dữ liệu thật.

| Mã | Bước làm | Kết quả mong đợi |
|---|---|---|
| TC-04-01 | Chưa đăng nhập, bấm ♥ | Mở hộp đăng nhập (`requireLogin`); API trả **401 GNE-SEC-4010** |
| TC-04-02 | Đăng nhập, bấm ♥ (hoặc `PUT /me/wishlist/{id}`) | Icon đầy; `/account/wishlist` có sản phẩm: ảnh, tên, giá, trạng thái mua được (API_SPEC 3.10 **không** trả tên shop) |
| TC-04-03 | Bấm ♥ lần nữa / PUT lần 2 | `204`, DB vẫn 1 dòng (idempotent) |
| TC-04-04 | Thêm 3–4 sản phẩm | Đủ, **mới thích trước**; phân trang cursor (`limit`) không trùng |
| TC-04-05 | Bỏ thích ở `/account/wishlist`; DELETE lần 2; DELETE id không có trong danh sách | Biến mất; các lần sau vẫn `204` |
| TC-04-06 | Thích id `999999` / sản phẩm `HIDDEN` | **404 GNE-CAT-4042** "Sản phẩm không còn bán." |
| TC-04-07 | (DB) một sản phẩm trong wishlist: (a) `status = 'HIDDEN'`; (b) tồn kho = 0; (c) shop đang **tạm nghỉ** | Vẫn nằm trong danh sách nhưng **không mua được** (`is_purchasable = false`). Nhớ khôi phục dữ liệu |
| TC-04-08 | Đăng nhập tài khoản khác | Wishlist riêng, không thấy / không xóa được của `mai.tran` |

> Ghi chú: sản phẩm của shop **TravelGear** (vd Balo, id 2) luôn "không mua được" vì shop đang tạm nghỉ theo seed (19/11 → 21/12) — đúng.

---

## 5. UC-SOC-06 — Theo dõi gian hàng

Tài khoản: không phải chủ shop được theo dõi (vd `owner.shoe@genie.example` — chủ Shoe Republic, hoặc tài khoản mới).
Màn: `/account/following`, nút **Theo dõi** ở `/shop/{id}`.

> ⚠ Trang shop storefront đang mock — id có thể lệch DB. `shop_id` thật: `SELECT id, shop_name FROM vendor_profiles;` (`shop_id` trên API = `vendor_profiles.id`); test bằng [../api-tests/shop-follow.http](../api-tests/shop-follow.http) nếu nút báo lỗi.

| Mã | Bước làm | Kết quả mong đợi |
|---|---|---|
| TC-05-01 | Theo dõi Home & Living (shop 4) | Nút "Đang theo dõi"; `follower_count` +1 |
| TC-05-02 | Theo dõi lần 2 | `204`, không tăng thêm, DB 1 dòng |
| TC-05-03 | `/account/following` | Shop: logo, tên, nhãn Chính hãng, đánh giá, số người theo dõi |
| TC-05-04 | Bỏ theo dõi; bỏ lần 2 | Biến mất, `follower_count` −1 đúng 1 lần; lần 2 vẫn `204` |
| TC-05-05 | `mai.tran` theo dõi **TechZone** (shop của mình) | **409 GNE-SOC-4095** |
| TC-05-06 | Shop không tồn tại / **chưa xác minh KYC** (Nhà Sách Mộc) / **KYC bị từ chối** (Cheap Electronics) | **404 GNE-SYS-4040** |
| TC-05-07 | (DB) khóa chủ một shop đang được theo dõi (`UPDATE users SET status='SUSPENDED' WHERE id=…`), tải lại `/account/following` | Shop đó **ẩn**; theo dõi shop đó → 404; mở khóa (`status='ACTIVE'`) thì hiện lại |

---

## 6. Bảng tổng hợp kết quả

| Nhóm | Use case | Mã | API (2026-11-22) | Chạy (bạn tick) | Ghi chú |
|---|---|---|---|---|---|
| 0 | Seed & khởi động | TC-00-01 → 05 | — | ☐ | TC-00-03 đạt (Silo có ảnh) |
| 1 | UC-ADM-07 Nhật ký hệ thống | TC-01-01 → 10 | — | ☐ | |
| 2 | UC-ADM-12 Banner | TC-02-01 → 16 | — | ☐ | TC-02-04 / TC-02-13 đã thấy đúng khi thao tác |
| 3 | UC-PRO-08 Điểm danh | TC-03-01 → 05 | ☑ (03-03, 03-05) | ☐ | |
| 3 | UC-PRO-09 Đổi Điểm | TC-03-06 → 12 | ☑ (03-06 → 03-11; 03-12 qua `CoinExpiryJobIT`) | ☐ | |
| 4 | UC-CAT-08 Yêu thích | TC-04-01 → 08 | ☑ | ☐ | Shop chủ bị khóa vẫn `is_purchasable=true` — đúng spec, nên bàn thêm |
| 5 | UC-SOC-06 Theo dõi shop | TC-05-01 → 07 | ☑ | ☐ | |

Khi một UC đạt toàn bộ trên giao diện, cập nhật cột **Chạy** trong `docs/Document/CHECKLIST_TIEN_DO_UC.md`.
