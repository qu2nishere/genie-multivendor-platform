# Kịch bản kiểm thử thủ công — Plan 2026-11-16

> Phạm vi: 8 plan trong `plan/2026-11-16` — dữ liệu seed, Nhật ký hệ thống (UC-ADM-07), Banner (UC-ADM-12),
> Điểm Genie (UC-PRO-08/09), Sản phẩm yêu thích (UC-CAT-08), Theo dõi gian hàng (UC-SOC-06),
> Mở shop & KYC (UC-VND-01/02/03/04/07/21), AI Worker KYC.
>
> Chuẩn bị môi trường theo [huong-dan-chay-local.md](huong-dan-chay-local.md) (Docker + backend + web, Mailpit, `web/.env` có đủ cờ `PUBLIC_REAL_API`).
> Mật khẩu mọi tài khoản seed: `Genie@123`. OTP xem ở Mailpit http://localhost:8025.

## Cách dùng file này

- Mỗi kịch bản có mã `TC-xx-nn`, **Bước làm** và **Kết quả mong đợi**. Làm đúng thứ tự trong từng nhóm vì kịch bản sau có thể dùng dữ liệu kịch bản trước.
- Đạt → ghi `☑` vào bảng tổng hợp cuối file; sai → ghi `☒` kèm mô tả + ảnh chụp / log backend.
- Mỗi lần thao tác, nên mở sẵn **DevTools → Network** để xem request có gọi `localhost:8080` thật (không phải mock) và mã lỗi `GNE-*` trả về.
- Phần nào giao diện chưa nối thật (ghi rõ ở từng mục), test bằng **Swagger** http://localhost:8080/swagger-ui.html hoặc file `.http` trong [docs/api-tests/](api-tests/) (VS Code extension *REST Client*).
- Muốn test lại từ đầu với dữ liệu sạch: `docker compose --env-file .env -f deploy/docker-compose.local.yml down -v` → `up -d` → chạy lại backend → `upload-assets.ps1`.

---

## 0. Dữ liệu seed & khởi động

| Mã | Bước làm | Kết quả mong đợi |
|---|---|---|
| TC-00-01 | Chạy backend lần đầu trên DB trống | Log Flyway chạy tới `V20__vendor_kyc_additions`, sau đó các `R__seed_*`; không lỗi; `Started GenieApplication` |
| TC-00-02 | Mở http://localhost:8080/actuator/health | `{"status":"UP"}` |
| TC-00-03 | Chạy `upload-assets.ps1`, mở Silo console http://localhost:9001 | Bucket ảnh sản phẩm / banner có tệp |
| TC-00-04 | Đăng nhập web lần lượt 5 tài khoản seed (mục 5 hướng dẫn chạy) | Đăng nhập được; Admin/Moderator/Finance/CS vào được `/admin`, menu hiện theo quyền |
| TC-00-05 | Dừng backend, chạy lại lần 2 | Seed chạy lại không lỗi, không nhân đôi dữ liệu (số banner / shop giữ nguyên) |

---

## 1. UC-ADM-07 — Nhật ký hệ thống (Admin)

Tài khoản: `yen.dh@genie.example` (có quyền `VIEW_AUDIT_LOG`). Màn: `/admin/audit-logs`.

**Tạo dữ liệu trước:** ở cửa sổ ẩn danh đăng nhập sai mật khẩu `mai.tran@gmail.com` 1–2 lần, rồi đăng nhập đúng 1 lần.

| Mã | Bước làm | Kết quả mong đợi |
|---|---|---|
| TC-01-01 | Vào `/admin/audit-logs` | Bảng nhật ký hiện bản ghi mới nhất ở trên: có `LOGIN_FAILED`, `LOGIN_SUCCESS` vừa tạo, kèm email, IP, thời gian giờ VN |
| TC-01-02 | Chọn khoảng ngày *từ – đến* = hôm nay | Chỉ còn bản ghi hôm nay |
| TC-01-03 | Chọn ngày *đến* nhỏ hơn *từ* | Báo lỗi khoảng ngày không hợp lệ, không gọi được dữ liệu |
| TC-01-04 | Lọc nhóm **AUTH** | Chỉ còn LOGIN_*, ACCOUNT_* |
| TC-01-05 | Lọc nhóm **KYC** (sau khi đã làm phần 6) | Có KYC_SUBMITTED / KYC_APPROVED / KYC_REJECTED |
| TC-01-06 | Ô tìm kiếm gõ `mai.tran`; sau đó gõ IP đang hiện ở cột IP (máy dev thường là `0:0:0:0:0:0:0:1` — địa chỉ IPv6 của localhost, tương đương `127.0.0.1`); sau đó gõ một mã tham chiếu ở cột Chi tiết (nếu có) | Mỗi lần chỉ còn bản ghi khớp email / IP / mã tham chiếu |
| TC-01-07 | Phân trang: cần **trên 50** bản ghi (mỗi lần tải 50). DB dev ít dữ liệu thì tạo 60 dòng giả bằng SQL ở cuối mục này → tải lại trang → bấm **Tải thêm** ở cuối bảng | Lần đầu 50 dòng, có nút **Tải thêm**; bấm thì nối thêm dòng xuống dưới, không trùng; hết dữ liệu thì nút biến mất. Ít hơn 50 bản ghi thì **không có nút** — đúng |
| TC-01-08 | Bấm **Xuất CSV** | Tải về file `.csv`; mở Excel thấy tiếng Việt không lỗi font, đúng bộ lọc đang chọn |
| TC-01-09 | Xem cột **Chi tiết** của các dòng: dòng đăng nhập (`LOGIN_*`) và dòng có đối tượng (vd `KYC_APPROVED`, `COMMISSION_UPDATED` sau khi làm phần 6) | Dòng `LOGIN_*` để **trống** cột Chi tiết (sự kiện đăng nhập không có đối tượng — đúng thiết kế). Dòng có đối tượng hiện tên đối tượng (`details.target_label`, vd tên shop) hoặc mã tham chiếu (`reference_id`). Màn **không** có trang chi tiết khi bấm vào dòng; `user_agent` chỉ lưu trong DB (`system_logs.user_agent`), không hiển thị |
| TC-01-10 | Đăng nhập `phat.nt@genie.example` (Moderator) vào `/admin/audit-logs` | Menu không có mục Nhật ký; gõ URL trực tiếp → màn "không có quyền"; API trả 403 và **sinh thêm 1 bản ghi `ACCESS_DENIED`** (kiểm lại bằng Admin) |

Tạo 60 dòng giả để thử phân trang (TC-01-07), chạy trong DB `genie` (DataGrip / psql):

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
| TC-02-01 | Vào `/admin/banners` | Danh sách banner seed, lọc được theo vị trí (HOME_TOP_CAROUSEL, HOME_TOP_SIDE, HOME_MIDDLE, CATEGORY_TOP, FLASH_SALE_TOP, POPUP) và trạng thái; có số lượt xem / click |
| TC-02-02 | **Tạo banner**: vị trí HOME_TOP_CAROUSEL, tải ảnh hợp lệ, link `/flash-sale`, thời gian bắt đầu = bây giờ, kết thúc = +7 ngày, bật hoạt động → Lưu | Tạo thành công, banner hiện trong danh sách trạng thái đang chạy |
| TC-02-03 | Mở trang chủ http://localhost:4321/ (tab mới) | Banner mới xuất hiện trong carousel đầu trang (cache Redis có thể trễ vài giây — tải lại trang) |
| TC-02-04 | Ghi lại cột **Hiển thị / Bấm** của banner → mở trang chủ 1 lần, bấm vào banner đó → quay lại admin sau **tối đa 5 phút**, tải lại danh sách | **Hiển thị** của *mọi* banner trong carousel +1 (mỗi lần tải trang, carousel dựng sẵn tất cả slide nên đều được tính), **Bấm** của banner vừa bấm +1, CTR = Bấm ÷ Hiển thị. Số liệu không tăng ngay vì đi qua bộ đếm Redis rồi `BannerStatsFlushJob` mới ghi vào DB (5 phút / lần) |
| TC-02-05 | Tạo banner với link ngoài `https://google.com` | Bị chặn: lỗi **GNE-BNR-4222** (link phải là đường dẫn nội bộ) |
| TC-02-06 | Tải ảnh > 5MB hoặc sai định dạng | Bị chặn ngay khi tải, báo lỗi dung lượng / định dạng |
| TC-02-07 | Thời gian kết thúc trước thời gian bắt đầu | Báo lỗi ở trường thời gian, không lưu được |
| TC-02-08 | Tạo liên tục banner đang hoạt động cùng vị trí cho tới khi vượt **8** | Banner thứ 9 bị chặn: **GNE-BNR-4221** |
| TC-02-09 | Sửa banner TC-02-02: đổi ảnh / đổi link / tắt hoạt động | Lưu được; tắt hoạt động → trang chủ không còn hiện (sau khi tải lại) |
| TC-02-10 | Đổi thứ tự (sort order) 2 banner cùng vị trí | Thứ tự carousel trang chủ đổi theo |
| TC-02-11 | Xóa banner **đang chạy / đã lên lịch** | Bị chặn: **GNE-BNR-4091** |
| TC-02-12 | Tắt hoạt động (hoặc tạo banner đã hết hạn) rồi xóa | Xóa được, biến mất khỏi danh sách |
| TC-02-13 | Tạo banner bắt đầu từ ngày mai | Trạng thái "đã lên lịch"; trang chủ hôm nay **không** hiện |
| TC-02-14 | Đăng nhập `truc.ht@genie.example` (CS) vào `/admin/banners` | Không thấy menu; vào URL trực tiếp → không có quyền / 403 |
| TC-02-15 | Vào `/admin/audit-logs` bằng Admin, lọc nhóm CONFIG | Có bản ghi tạo / sửa / xóa banner vừa làm |

---

## 3. UC-PRO-08 / UC-PRO-09 — Điểm Genie: điểm danh & đổi voucher

Tài khoản: `mai.tran@gmail.com` (hoặc tài khoản mới đăng ký để thấy chuỗi từ ngày 1). Màn: `/account/points`, `/account/vouchers`.

| Mã | Bước làm | Kết quả mong đợi |
|---|---|---|
| TC-03-01 | Vào `/account/points` | Thấy số dư Điểm, lịch 7 ngày điểm danh (ngày 1–6: +100, ngày 7: +1000), lịch sử giao dịch, điểm sắp hết hạn |
| TC-03-02 | Bấm **Điểm danh** | Cộng **+100** (hoặc mức của ngày hiện tại trong chuỗi); số dư tăng; lịch sử có dòng mới loại CHECKIN; ngày hôm nay đánh dấu đã nhận |
| TC-03-03 | Bấm điểm danh lần 2 trong ngày (hoặc F5 rồi bấm) | Nút bị khóa; nếu gọi API → **GNE-PRO-4091** (đã điểm danh hôm nay) |
| TC-03-04 | Mở 2 tab, bấm điểm danh gần như cùng lúc ở cả hai | Chỉ 1 lần được cộng, tab kia báo đã điểm danh |
| TC-03-05 | Kiểm tra quy tắc chuỗi bằng DB (tùy chọn): lùi `created_at` bản ghi điểm danh 1 ngày rồi điểm danh lại | Chuỗi tăng lên ngày 2; nếu lùi 2 ngày (đứt chuỗi) → quay lại ngày 1 |
| TC-03-06 | Phần **Đổi Điểm lấy voucher**: xem 3 mức 500 / 1000 / 2000 Điểm | Hiện mệnh giá voucher tương ứng (COIN500 / COIN1000 / COIN2000) và nút đổi |
| TC-03-07 | Đổi mức **500** khi đủ điểm | Trừ 500 Điểm; lịch sử có dòng REDEEM; vào `/account/vouchers` thấy voucher mới |
| TC-03-08 | Đổi lại đúng mức 500 lần nữa khi voucher trước chưa dùng | Bị chặn: **GNE-PRO-4092** (đã có voucher này) |
| TC-03-09 | Đổi mức **2000** khi số dư < 2000 | Nút mờ hoặc báo **GNE-PRO-4222** (không đủ Điểm); số dư giữ nguyên |
| TC-03-10 | Trang công khai mức thưởng (đăng xuất, vào trang giới thiệu Điểm nếu có / Swagger `GET /public/coin-rewards`) | Trả bảng thưởng không cần đăng nhập |
| TC-03-11 | (Tùy chọn, DB) sửa `expires_at` một lô điểm về hôm qua, chờ job 00:15 hoặc gọi job qua test | Lô đó bị trừ, lịch sử có dòng EXPIRED; trừ điểm khi đổi voucher ăn lô **sắp hết hạn trước** (FEFO) |

---

## 4. UC-CAT-08 — Sản phẩm yêu thích

Tài khoản: `mai.tran@gmail.com`. Màn: `/account/wishlist`, nút ♥ trên thẻ / trang sản phẩm.

> ⚠ Catalog storefront **vẫn chạy mock** → id sản phẩm trên thẻ mock có thể không trùng id trong DB. Nếu bấm ♥ ở storefront mà báo **GNE-CAT-4042**, lấy id thật bằng SQL
> `SELECT id, name, status FROM products ORDER BY id LIMIT 10;` rồi test bằng [docs/api-tests/wishlist.http](api-tests/wishlist.http) / Swagger. Màn `/account/wishlist` thì hiển thị dữ liệu thật.

| Mã | Bước làm | Kết quả mong đợi |
|---|---|---|
| TC-04-01 | Chưa đăng nhập, bấm ♥ trên một sản phẩm | Chuyển sang đăng nhập (hoặc báo cần đăng nhập) |
| TC-04-02 | Đăng nhập, bấm ♥ (hoặc `PUT /me/wishlist/{productId}` id thật) | Icon chuyển đầy; `/account/wishlist` có sản phẩm với ảnh, giá, tên shop |
| TC-04-03 | Bấm ♥ lần nữa / gọi PUT lần 2 | Không lỗi, không nhân đôi (idempotent) |
| TC-04-04 | Thêm 3–4 sản phẩm, vào `/account/wishlist` | Hiện đủ, sắp xếp mới thêm trước; phân trang nếu nhiều |
| TC-04-05 | Bỏ thích 1 sản phẩm ở `/account/wishlist` | Biến mất khỏi danh sách; gọi DELETE lần 2 vẫn không lỗi |
| TC-04-06 | Thêm id sản phẩm không tồn tại / đã ẩn (vd `999999`) | **GNE-CAT-4042** |
| TC-04-07 | (DB) đổi `status` một sản phẩm đang trong wishlist thành ẩn / shop bị khóa | Sản phẩm trong wishlist hiện nhãn "không còn bán" (hoặc bị lọc), không bấm mua được |
| TC-04-08 | Đăng nhập tài khoản khác | Wishlist trống / khác, không thấy của mai.tran |

---

## 5. UC-SOC-06 — Theo dõi gian hàng

Tài khoản: tài khoản **không phải chủ TechZone** (vd đăng ký mới `buyer1@genie.test`, hoặc `owner.shoe@genie.example`). Màn: `/account/following`, nút **Theo dõi** ở `/shop/{id}`.

> ⚠ Trang shop storefront đang mock — id shop có thể lệch DB. Lấy `shop_id` thật: `SELECT id, shop_name FROM vendor_profiles;` (`shop_id` trên API = `vendor_profiles.id`), test bằng [docs/api-tests/shop-follow.http](api-tests/shop-follow.http) nếu nút trên trang shop báo lỗi.

| Mã | Bước làm | Kết quả mong đợi |
|---|---|---|
| TC-05-01 | Vào trang shop, bấm **Theo dõi** (hoặc `PUT /me/following/{shopId}`) | Nút đổi thành "Đang theo dõi"; số người theo dõi +1 |
| TC-05-02 | Bấm theo dõi lần 2 / gọi PUT lại | Không lỗi, số theo dõi không tăng thêm |
| TC-05-03 | Vào `/account/following` | Thấy shop với logo, tên, số người theo dõi, đánh giá |
| TC-05-04 | Bỏ theo dõi ở `/account/following` | Shop biến mất; `follower_count` giảm 1; gọi DELETE lần 2 không lỗi |
| TC-05-05 | Đăng nhập `mai.tran@gmail.com`, theo dõi **chính TechZone Official** | Bị chặn: **GNE-SOC-4095** |
| TC-05-06 | Theo dõi shop id không tồn tại | Lỗi không tìm thấy shop (404) |
| TC-05-07 | (DB) khóa tài khoản chủ một shop đang được theo dõi, tải lại `/account/following` | Shop đó bị ẩn khỏi danh sách |

---

## 6. UC-VND-01/02/03/04/07/21 — Mở shop & KYC

Chuẩn bị:
- 1 tài khoản người mua **chưa có shop**: đăng ký mới `seller1@genie.test` (OTP ở Mailpit).
- Ảnh CCCD mặt trước, mặt sau, ảnh selfie, (giấy phép kinh doanh) — **mỗi ảnh ≥ 20KB**, jpg/png. Có thể dùng ảnh bất kỳ khi AI ở chế độ **stub**.
- Nhớ quy tắc stub: **2 số cuối CCCD** `00–29` VERIFIED · `30–69` MANUAL_REVIEW · `70–99` REJECTED.

### 6.1. UC-VND-01 — Đăng ký mở shop (`/become-seller`)

| Mã | Bước làm | Kết quả mong đợi |
|---|---|---|
| TC-06-01 | Đăng nhập `seller1@genie.test`, vào `/become-seller` | Form đăng ký: tên shop, email shop, SĐT, ngành, loại hình (INDIVIDUAL / HOUSEHOLD / BUSINESS) |
| TC-06-02 | Nhập tên shop `TechZone Official` (đã có) | Báo **GNE-VND-4094** (tên shop đã được dùng) |
| TC-06-03 | Nhập tên mới `Shop Thử Nghiệm 1`, email shop `shop1@genie.test` → Gửi | Báo đã gửi OTP; Mailpit có mail OTP tới `shop1@genie.test` |
| TC-06-04 | Nhập sai OTP | Báo OTP sai, đếm số lần còn lại |
| TC-06-05 | Nhập đúng OTP | Tạo shop thành công, chuyển sang Seller Center, trạng thái KYC = NOT_SUBMITTED, banner nhắc nộp KYC |
| TC-06-06 | Quay lại `/become-seller` đăng ký shop thứ 2 | **GNE-VND-4093** (tài khoản đã có shop) |

### 6.2. UC-VND-02 — Nộp hồ sơ KYC (`/seller/kyc`)

| Mã | Bước làm | Kết quả mong đợi |
|---|---|---|
| TC-06-07 | Ở Seller Center, bấm các menu bán hàng (sản phẩm, đơn hàng…) | Bị `KycGate` chặn, dẫn về `/seller/kyc` |
| TC-06-08 | Điền CCCD 11 số / có chữ | Lỗi trường: CCCD phải đúng 12 chữ số |
| TC-06-09 | Bỏ trống ảnh selfie → Gửi | Lỗi **GNE-VND-4223** (thiếu giấy tờ bắt buộc) |
| TC-06-10 | Tải ảnh < 20KB | Bị chặn: ảnh quá nhỏ / chất lượng thấp |
| TC-06-11 | Loại hình HOUSEHOLD/BUSINESS mà không có giấy phép kinh doanh / mã số thuế | Báo thiếu BUSINESS_LICENSE / mã số thuế |
| TC-06-12 | Nhập CCCD đuôi **45** (vd `079203000145`), đủ ảnh, tick đồng ý sinh trắc → Gửi | Trạng thái chuyển **AI_PROCESSING** (đang xác minh); sau ~2 giây tải lại → **MANUAL_REVIEW** (chờ người duyệt) |
| TC-06-13 | Trong lúc AI_PROCESSING/MANUAL_REVIEW, thử nộp lại hồ sơ | **GNE-VND-4095** (hồ sơ đang xử lý) |
| TC-06-14 | Kiểm tra Silo console bucket hồ sơ KYC | Tệp lưu dạng **đã mã hóa** (tải về không mở được như ảnh thường) |

### 6.3. UC-VND-21 — Admin duyệt KYC (`/admin/kyc`)

| Mã | Bước làm | Kết quả mong đợi |
|---|---|---|
| TC-06-15 | Đăng nhập `phat.nt@genie.example` (Moderator) → `/admin/kyc` | Hàng đợi có shop seed (Nhà Sách Mộc, Gốm Bát Tràng Xưa, Mẹ & Bé Happy, TravelGear Outdoor…) và **Shop Thử Nghiệm 1**; lọc theo trạng thái |
| TC-06-16 | Mở hồ sơ Shop Thử Nghiệm 1 | Xem được ảnh CCCD/selfie (link tạm, hết hạn sau 5 phút), điểm rủi ro, các tín hiệu AI, thông tin khai báo |
| TC-06-17 | Moderator thử sửa **hoa hồng** | Bị chặn 403 (thiếu `UPDATE_COMMISSION`) |
| TC-06-18 | Bấm **Yêu cầu bổ sung**, không chọn giấy tờ nào | Báo phải chọn ≥ 1 giấy tờ |
| TC-06-19 | Yêu cầu bổ sung ảnh SELFIE, ghi chú "ảnh mờ" | Trạng thái SUPPLEMENT_REQUESTED, hạn 7 ngày; người bán (`seller1`) vào `/seller/kyc` thấy yêu cầu và chỉ cần tải lại selfie |
| TC-06-20 | `seller1` tải lại selfie (CCCD giữ đuôi 45) → gửi | Quay lại AI_PROCESSING → MANUAL_REVIEW |
| TC-06-21 | Moderator **Từ chối** với lý do OTHER mà không ghi chú | Báo phải nhập ghi chú |
| TC-06-22 | Đăng nhập Admin `yen.dh` → **Duyệt** hồ sơ, đặt hoa hồng 6% | Trạng thái VERIFIED; `seller1` vào Seller Center hết bị chặn; audit log có KYC_APPROVED + COMMISSION_UPDATED |
| TC-06-23 | Admin thử đặt hoa hồng 60% | Bị chặn (chỉ 0–50%) |
| TC-06-24 | Đăng nhập `linh.tm` / `truc.ht` vào `/admin/kyc` | Không có quyền (403) |
| TC-06-25 | Với một hồ sơ seed MANUAL_REVIEW (vd *Gốm Bát Tràng Xưa*) → **Từ chối** lý do hợp lệ | Trạng thái REJECTED; audit có KYC_REJECTED |
| TC-06-26 | Dùng nút **Đưa lại hàng đợi / chạy lại AI** (requeue) cho một hồ sơ | Hồ sơ chuyển lại trạng thái xử lý |

### 6.4. Luồng tự duyệt / tự từ chối & yêu cầu xem xét lại

| Mã | Bước làm | Kết quả mong đợi |
|---|---|---|
| TC-06-27 | Tạo `seller2@genie.test` → mở shop → nộp KYC CCCD đuôi **12** | Sau ~2 giây → **VERIFIED** tự động, không cần admin |
| TC-06-28 | Tạo `seller3@genie.test` → mở shop → nộp KYC CCCD đuôi **88** | Sau ~2 giây → **REJECTED** |
| TC-06-29 | `seller3` bấm **Yêu cầu xem xét thủ công** | Chuyển MANUAL_REVIEW, vào hàng đợi admin |
| TC-06-30 | `seller3` (sau khi bị từ chối lần nữa) yêu cầu xem xét lần 2 | **GNE-VND-4096** (chỉ được yêu cầu 1 lần) |
| TC-06-31 | Đăng nhập `kyc.*@genie.example` của TravelGear Outdoor | `/seller/kyc` hiện yêu cầu bổ sung từ seed |

### 6.5. UC-VND-07 — Tài khoản nhận tiền (Seller Center → Tài khoản ngân hàng / payout)

Tài khoản: `mai.tran@gmail.com` (TechZone đã VERIFIED) hoặc `seller1` sau TC-06-22.

| Mã | Bước làm | Kết quả mong đợi |
|---|---|---|
| TC-06-32 | Mở màn tài khoản nhận tiền | Chọn ngân hàng từ danh sách (`/public/banks`), nhập số tài khoản, tên chủ tài khoản |
| TC-06-33 | Nhập số tài khoản kết thúc **000** | Báo tên không khớp: **GNE-WAL-4222** |
| TC-06-34 | Nhập số hợp lệ, tên trùng tên trên KYC → Lưu | Yêu cầu OTP xác thực bước 2 → Mailpit có OTP (mục đích PAYOUT_ACCOUNT_UPDATE) → nhập đúng → lưu thành công, số tài khoản hiển thị dạng che `****1234` |
| TC-06-35 | Nhập sai OTP / để OTP hết hạn | Không lưu; báo OTP sai / hết hạn |
| TC-06-36 | Xóa tài khoản nhận tiền | Yêu cầu OTP (PAYOUT_ACCOUNT_DELETE) → xác nhận → xóa |
| TC-06-37 | Shop chưa VERIFIED (vd `seller3`) vào màn này | Bị chặn **GNE-VND-4031** |

### 6.6. UC-VND-04 — Cài đặt shop (`/seller/settings`)

Tài khoản: `seller1` (shop mới, chưa đổi tên lần nào) hoặc `mai.tran`.

| Mã | Bước làm | Kết quả mong đợi |
|---|---|---|
| TC-06-38 | Đổi logo (ảnh < 1MB) và ảnh bìa (< 2MB), mô tả shop → Lưu | Lưu thành công, tải lại trang vẫn giữ |
| TC-06-39 | Logo > 1MB / ảnh bìa > 2MB | Bị chặn dung lượng |
| TC-06-40 | Đổi tên shop | Lưu thành công; audit/thông tin shop dùng tên mới |
| TC-06-41 | Đổi tên shop lần 2 ngay sau đó | **GNE-VND-4091** (chỉ đổi 1 lần / 30 ngày) |
| TC-06-42 | Tắt **tất cả** đơn vị vận chuyển → Lưu | **GNE-VND-4221** (phải còn ≥ 1) |
| TC-06-43 | Bật tự động trả lời chat nhưng để trống nội dung | **GNE-VND-4222** |
| TC-06-44 | Bật tự trả lời với nội dung hợp lệ | Lưu được |

### 6.7. UC-VND-03 — Hồ sơ shop / thông tin `/seller/me`

| Mã | Bước làm | Kết quả mong đợi |
|---|---|---|
| TC-06-45 | Đăng nhập chủ shop, vào trang tổng quan Seller Center | Hiện tên shop, trạng thái KYC, loại hình, hoa hồng đúng như DB |
| TC-06-46 | Admin xem danh sách shop (`/admin/shops` nếu có màn / Swagger) | Thấy shop mới với trạng thái KYC tương ứng |

> **Không test trong đợt này:** UC-VND-05 (địa chỉ lấy hàng / kho GHN) và chế độ tạm nghỉ (vacation) — giao diện vẫn mock, backend chờ danh mục hành chính.

---

## 7. AI Worker KYC (tùy chọn — chế độ `http`)

Chỉ làm khi đã bật AI worker thật theo mục 6.2 hướng dẫn chạy.

| Mã | Bước làm | Kết quả mong đợi |
|---|---|---|
| TC-07-01 | Mở http://localhost:8000/health | `status: UP`, `models_loaded: true`, có `pipeline_version` |
| TC-07-02 | Gọi `POST http://localhost:8000/kyc/verify` **không** kèm header token | 401/403 |
| TC-07-03 | Nộp KYC trên web với ảnh CCCD thật (ảnh của chính mình) | Log worker nhận yêu cầu, callback về backend; admin thấy tín hiệu `checks` (QR, MRZ, face_match…) và điểm khớp mặt |
| TC-07-04 | Nộp KYC với ảnh không phải CCCD (ảnh phong cảnh) | Tín hiệu `UNREADABLE_IMAGE` → hồ sơ vào MANUAL_REVIEW hoặc REJECTED, không treo |
| TC-07-05 | Selfie là người khác | `face_match = false`, điểm rủi ro cao |
| TC-07-06 | Tắt worker rồi nộp KYC | Backend không lỗi 500; hồ sơ chuyển MANUAL_REVIEW (dự phòng) hoặc giữ chờ, có log cảnh báo |

---

## 8. Bảng tổng hợp kết quả

| Nhóm | Use case | Mã kịch bản | Kết quả | Ghi chú |
|---|---|---|---|---|
| 0 | Seed & khởi động | TC-00-01 → 05 | ☐ | |
| 1 | UC-ADM-07 Nhật ký hệ thống | TC-01-01 → 10 | ☐ | |
| 2 | UC-ADM-12 Banner | TC-02-01 → 15 | ☐ | |
| 3 | UC-PRO-08 Điểm danh | TC-03-01 → 05 | ☐ | |
| 3 | UC-PRO-09 Đổi Điểm lấy voucher | TC-03-06 → 11 | ☐ | |
| 4 | UC-CAT-08 Yêu thích | TC-04-01 → 08 | ☐ | |
| 5 | UC-SOC-06 Theo dõi shop | TC-05-01 → 07 | ☐ | |
| 6.1 | UC-VND-01 Đăng ký shop | TC-06-01 → 06 | ☐ | |
| 6.2 | UC-VND-02 Nộp KYC | TC-06-07 → 14 | ☐ | |
| 6.3–6.4 | UC-VND-21 Duyệt KYC | TC-06-15 → 31 | ☐ | |
| 6.5 | UC-VND-07 Tài khoản nhận tiền | TC-06-32 → 37 | ☐ | |
| 6.6 | UC-VND-04 Cài đặt shop | TC-06-38 → 44 | ☐ | |
| 6.7 | UC-VND-03 Hồ sơ shop | TC-06-45 → 46 | ☐ | |
| 7 | AI Worker (tùy chọn) | TC-07-01 → 06 | ☐ | |

Khi một UC đạt toàn bộ, báo lại để cập nhật cột **Chạy** trong `docs/document/CHECKLIST_TIEN_DO_UC.md`.
