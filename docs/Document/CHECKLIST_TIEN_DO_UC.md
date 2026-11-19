# CHECKLIST TIẾN ĐỘ USE CASE — GENIE (BẢN TLCN)

> **Cập nhật:** 2026-11-17 · **Nguồn đặc tả:** [Usecase.md](diagrams/UseCaseDiagram/Usecase.md) (TLCN v1.4) · **Nguồn trạng thái:** mã nguồn `genie-multivendor-platform` (plan `plan/2026-11-11/web/` G00–G13, `plan/2026-11-14/auth.md` A00–A16, `docs/web-backend-handoff.md`).
> **Bảng quản lý đầy đủ (tác nhân, quan hệ, lọc):** [Quản Lý UseCase.xlsx](diagrams/UseCaseDiagram/Quản%20Lý%20UseCase.xlsx) — cột *Hiện trạng mã nguồn* đã đồng bộ với file này.
> **Cách dùng:** mỗi khi xong một lớp của UC thì đổi ô `☐` → `☑` (một phần: `◐`), cập nhật cột Trạng thái và ghi mã commit / PR vào Nhật ký cuối file. Một UC chỉ **Đã triển khai** khi đủ 5 lớp (Definition of Done — implement.md 15.3).

## Thành viên & phân công

> Cập nhật 2026-11-17. Cột **Phụ trách** ở mọi bảng use case bên dưới ghi người nhận làm; `—` = chưa phân công. Đổi / nhận thêm use case thì sửa cột đó, bảng này, cột *Người phụ trách* trong `Quản Lý UseCase.xlsx` và ghi một dòng Nhật ký.

| Thành viên | Phụ trách | Hiện trạng |
|---|---|---|
| **Nguyễn Quốc Khoa** | Toàn bộ phân hệ 01 — Xác thực & Tài khoản (9 use case `UC-AUTH-*`) | Đang triển khai — 4 đã xong, 5 đang làm (UC-AUTH-09 xong 2026-11-18); plan `plan/2026-11-14/auth.md` |
| **Nguyễn Ngọc Thiện** | 13 use case: Tra cứu nhật ký kiểm toán hệ thống bất biến (UC-ADM-07) · Quản lý banner trang chủ & chiến dịch tĩnh (UC-ADM-12) · Điểm danh nhận Điểm hàng ngày & Duy trì chuỗi điểm danh (UC-PRO-08) + Tra cứu số dư Điểm Genie & Lịch sử biến động Điểm (UC-PRO-09) · Quản lý danh sách sản phẩm yêu thích (UC-CAT-08) · Khách hàng theo dõi gian hàng (UC-SOC-06) · cụm B4 Mở shop & KYC: UC-VND-01, 02, 03, 04, 05, 07, 21 | Đang triển khai — seed dữ liệu mẫu xong (nhánh `feature/db-seed-demo`); UC-ADM-07 chờ chạy thật (nhánh `feature/admin-audit-log`); UC-ADM-12 chờ chạy thật (nhánh `feature/admin-home-banner`); UC-PRO-08, PRO-09 chờ chạy thật (nhánh `feature/promotion-coin-checkin`); UC-CAT-08 chờ chạy thật (nhánh `feature/catalog-wishlist`); UC-SOC-06 chờ chạy thật (nhánh `feature/social-shop-follow`); UC-VND-01, 02, 03 (backend), 04, 07, 21 chờ chạy thật (nhánh `feature/vendor-shop-kyc`), UC-VND-05 chờ danh mục hành chính GHN (A12) |
| **Nghiêm Phú Đăng Quân** | Chưa phân công | — |

## 1. Tổng quan

| Chỉ số | Số lượng |
|---|---:|
| Tổng use case | **87** |
| Đã triển khai (backend + test + web nối API thật) | **4** |
| Đang triển khai | **17** |
| Chưa có backend — giao diện đã dựng trên lớp mock | **55** |
| Chưa có backend — xử lý phía máy chủ (cron / webhook / AI) | **11** |
| Giao diện `web/` (Storefront + Seller + Admin, 34 feature, chạy mock) | **429 / 429 task — xong** |
| Backend `server-spring` | 65 entity + `V1…V14`; module **AUTH** đang làm (83 test xanh — 2026-11-17); 10 module còn lại chưa có service |
| AI Worker `server-python` | Khung (`kyc`, `sentiment` còn TODO) |

**Chú thích ngắn:** **GD** = giao diện · **BE** = backend · **Test** = kiểm thử · **Nối** = nối API thật · **Chạy** = chạy thật. `☑` xong · `◐` một phần · `☐` chưa · `—` không áp dụng. Giải thích đầy đủ ở [mục 0](#0-chu-thich).

<a id="0-chu-thich"></a>
### 0. Chú thích chi tiết — đọc trước khi tick

Mỗi use case đi qua **5 lớp theo thứ tự trái → phải**. Lớp sau dựa trên lớp trước: chưa có backend thì không thể "Nối", chưa "Nối" thì không thể "Chạy".

```
GD ──► BE ──► Test ──► Nối ──► Chạy   ⇒  ✅ Đã triển khai
```

| Cột | Tên đầy đủ | Nghĩa là gì | Làm ở đâu | Tick `☑` khi | Ví dụ hiện tại |
|---|---|---|---|---|---|
| **GD** | Giao diện | Màn hình của UC đã dựng trong `web/` và **chạy được bằng dữ liệu giả** (lớp mock, `PUBLIC_USE_MOCK=true`). Bấm được, hiển thị đúng bố cục, đủ trạng thái (đang tải, rỗng, lỗi) nhưng dữ liệu chưa phải thật | `web/src/features/<x>/ui`, trang trong `web/src/pages`, `/seller/*`, `/admin/*` | Mở đúng route ở cột *Màn hình (web)* thấy màn hình hoạt động trên mock | Mọi UC có màn hình đều `☑` (plan giao diện G00–G13 xong 429/429 task) |
| **BE** | Backend | API và nghiệp vụ của UC đã viết trong `server-spring`: controller (đường dẫn đúng API_SPEC), service (luật nghiệp vụ, transaction), lưu PostgreSQL / Redis / Silo, mã lỗi `GNE-*` | `server-spring/src/main/java/com/genie/<module>/<feature>/{api,application,domain,infra}` | Gọi được endpoint thật (Swagger `http://localhost:8080/swagger-ui.html` hoặc file `.http`) và trả đúng dữ liệu | UC-AUTH-01, 02, 04, 06, 10, 11 `☑`; UC-AUTH-03, 08 `◐` |
| **Test** | Kiểm thử tự động | Có test cho UC và **đang xanh**: unit test (`*Test`) cho luật trong entity / service, test tích hợp (`*IT`) chạy với PostgreSQL + Redis + Silo thật qua Testcontainers, phủ cả ca lỗi | `server-spring/src/test/java/com/genie/<module>/…` | `./mvnw verify` pass và có test ứng với luồng chính + luồng ngoại lệ của UC | `RegisterFlowIT`, `LoginFlowIT`, `PasswordFlowIT`, `AccountDeletionIT`… |
| **Nối** | Nối API thật | Giao diện **bỏ dữ liệu giả**, gọi backend thật: xóa nhánh `if (USE_MOCK)` trong `*.api.ts` (hoặc bật cờ `PUBLIC_REAL_API=<nhóm>`), sửa đường dẫn lệch spec, ánh xạ mã lỗi `GNE-*` sang thông báo | `web/src/features/<x>/infrastructure/*.api.ts`, `docs/web-backend-handoff.md` | Màn hình lấy / ghi dữ liệu qua `/api/v1/...` thật, không còn đọc mock | Nhóm `auth`, `upload`, `profile`, `security` đã nối |
| **Chạy** | Chạy thật end-to-end | Một người **tự thao tác trên giao diện** với cả hệ thống thật chạy cùng lúc (web 4321 + backend 8080 + PostgreSQL + Redis + Silo + Mailpit / Google / VNPay sandbox…) và luồng đúng từ đầu tới cuối, kể cả email / OTP thật | Máy dev (`deploy/docker-compose.local.yml`) hoặc môi trường demo | Đi hết luồng chính trong Usecase.md không lỗi, dữ liệu lưu đúng trong CSDL | Đăng ký, đăng nhập, Google, quên mật khẩu đã chạy thật |

**Ký hiệu trong ô**

| Ký hiệu | Nghĩa | Khi nào dùng |
|:---:|---|---|
| `☑` | Xong | Lớp đó hoàn chỉnh cho **toàn bộ** UC |
| `◐` | Một phần | Mới xong một phần của UC — ví dụ UC-AUTH-03: Google xong nhưng Zalo chưa chạy thật; UC-AUTH-08: hồ sơ + ảnh xong nhưng đổi email chưa. Ghi phần còn thiếu ở dòng chữ nhỏ dưới cột *Trạng thái* |
| `☐` | Chưa làm | Chưa bắt đầu lớp này |
| `—` | Không áp dụng | UC chạy nền (cron, webhook, AI — vd UC-ORD-08 hủy đơn quá hạn, UC-PAY-03 IPN / webhook SePay) không có màn hình riêng nên không có **GD** / **Nối**; vẫn phải có **BE**, **Test**, **Chạy** |

**Cột Trạng thái** (cũng là giá trị cột *Hiện trạng mã nguồn* trong Excel)

| Trạng thái | Điều kiện |
|---|---|
| ⬜ **Chưa triển khai** | Chưa có backend — dù GD đã `☑` |
| 🟡 **Đang triển khai** | Có ít nhất một ô BE / Test / Nối / Chạy là `☑` hoặc `◐`, nhưng chưa đủ 5 ô |
| ✅ **Đã triển khai** | Đủ 5 ô `☑` (UC chạy nền: đủ các ô không phải `—`) — đúng *Definition of Done* ở implement.md 15.3 |

**Cột Phụ trách** — người nhận làm use case (làm đủ 5 lớp, kể cả nối web và chạy thật). `—` = chưa ai nhận. Nhận use case mới: ghi tên ở cột này, ở bảng *Thành viên & phân công* đầu file và cột *Người phụ trách* trong Excel. Chuyển người: sửa cả ba nơi và ghi Nhật ký.

**Ví dụ đọc một dòng:** `UC-AUTH-06 | … | ☑ ☑ ☑ ☑ ☐ | 🟡 Đang triển khai` → màn hình có, backend có, test xanh, web đã gọi API thật; **chỉ còn** chạy thử trên giao diện. Chạy xong, đúng → đổi ô cuối thành `☑`, Trạng thái thành ✅.

**Quy trình cập nhật khi làm xong một lớp**
1. Đổi ô tương ứng `☐` → `☑` (hoặc `◐` nếu chưa đủ, và ghi phần thiếu ở dòng chữ nhỏ).
2. Cập nhật cột *Trạng thái* theo bảng trên; cập nhật số ở **mục 1. Tổng quan**.
3. Ghi một dòng vào **mục 5. Nhật ký** (ngày, UC, việc đã làm, mã commit / PR).
4. Đổi cùng giá trị ở cột *Hiện trạng mã nguồn* trong `Quản Lý UseCase.xlsx` (và câu *Hiện trạng* của UC trong Usecase.md) để các nơi khớp nhau.

**Tiến độ theo phân hệ**

| Phân hệ | Số UC | Đã triển khai | Đang triển khai | Chưa |
|---|---:|---:|---:|---:|
| 01. Xác thực & Tài khoản | 9 | 4 | 5 | 0 |
| 02. Danh mục, Sản phẩm & Tìm kiếm | 17 | 0 | 0 | 17 |
| 03. Giỏ hàng, Đặt hàng & Đơn hàng | 9 | 0 | 0 | 9 |
| 04. Thanh toán, Ký quỹ & Sổ cái | 5 | 0 | 0 | 5 |
| 05. Gian hàng & KYC người bán | 11 | 0 | 6 | 5 |
| 06. Vận chuyển 3PL | 5 | 0 | 0 | 5 |
| 07. Khuyến mãi, Voucher, Flash Sale & Điểm | 9 | 0 | 2 | 7 |
| 08. Chat, Đánh giá & Theo dõi shop | 7 | 0 | 1 | 6 |
| 09. Trả hàng, Hoàn tiền & Khiếu nại | 4 | 0 | 0 | 4 |
| 10. Thông báo | 3 | 0 | 0 | 3 |
| 11. Quản trị sàn, Nhân viên & Rút tiền | 8 | 0 | 2 | 6 |

## 2. Checklist từng use case

### Phân hệ 01 — XÁC THỰC & TÀI KHOẢN NGƯỜI DÙNG (AUTHENTICATION & ACCOUNT)

| Mã | Use case | Sprint | Màn hình (web) | GD | BE | Test | Nối | Chạy | Trạng thái | Phụ trách |
|---|---|---|---|:---:|:---:|:---:|:---:|:---:|---|---|
| `UC-AUTH-01` | Đăng ký tài khoản Khách hàng | S1 | `/auth/register` | ☑ | ☑ | ☑ | ☑ | ☑ | ✅ Đã triển khai<br><sub>backend + test, web nối API thật, đã chạy thật</sub> | Nguyễn Quốc Khoa |
| `UC-AUTH-02` | Đăng nhập hệ thống | S1 | `/auth/login` (mật khẩu / OTP), màn tạm khóa | ☑ | ☑ | ☑ | ☑ | ☑ | ✅ Đã triển khai<br><sub>backend + test, web nối API thật; GET /me dựng phiên đầy đủ (2026-11-17)</sub> | Nguyễn Quốc Khoa |
| `UC-AUTH-03` | Đăng nhập & Liên kết tài khoản Google / Zalo (OAuth2 / OpenID Connect) | S7 | `/auth/login`, `/auth/oauth-callback`, `/account/logins` | ☑ | ◐ | ☑ | ☑ | ◐ | 🟡 Đang triển khai<br><sub>Google xong (đăng nhập, liên kết / gỡ liên kết). Zalo thay Apple (2026-11-17): code + test xong, build xanh; chạy thật tạm dừng — Zalo bắt xác thực domain, chờ tên miền thật</sub> | Nguyễn Quốc Khoa |
| `UC-AUTH-04` | Khôi phục mật khẩu qua Email OTP (Forgot Password) | S1 | `/auth/forgot` | ☑ | ☑ | ☑ | ☑ | ☑ | ✅ Đã triển khai<br><sub>backend + test, web nối API thật, đã chạy thật</sub> | Nguyễn Quốc Khoa |
| `UC-AUTH-06` | Đổi / Thiết lập mật khẩu | S1 | `/account/password`, Seller › Cài đặt › Bảo mật, Admin › Tài khoản | ☑ | ☑ | ☑ | ☑ | ☐ | 🟡 Đang triển khai<br><sub>backend + test + web nối xong, chưa chạy thật</sub> | Nguyễn Quốc Khoa |
| `UC-AUTH-08` | Quản lý thông tin hồ sơ cá nhân (Customer Profile) | S2 | `/account/profile`, Admin › Tài khoản | ☑ | ◐ | ◐ | ◐ | ☐ | 🟡 Đang triển khai<br><sub>hồ sơ + ảnh đại diện (Silo) xong; GET /me xong; còn đổi email</sub> | Nguyễn Quốc Khoa |
| `UC-AUTH-09` | Quản lý sổ địa chỉ giao hàng chuẩn GHN | S2 | `/account/address`, modal địa chỉ ở `/checkout` | ☑ | ☑ | ☑ | ☑ | ☑ | ✅ Đã triển khai<br><sub>sổ địa chỉ địa giới 2 cấp (34 tỉnh / 3.321 xã), 5 loại địa chỉ, chặn trùng, ghi chú giao hàng, định vị GPS thật; build xanh + đã chạy thật 2026-11-18. Danh mục mã GHN cần `GHN_TOKEN` + `GHN_SHOP_ID`</sub> | Nguyễn Quốc Khoa |
| `UC-AUTH-10` | Yêu cầu ẩn danh hóa & Xóa dữ liệu cá nhân (Xóa tài khoản) | S7 | `/account/profile` › Xóa tài khoản | ☑ | ☑ | ☑ | ☑ | ☐ | 🟡 Đang triển khai<br><sub>backend + test + web nối xong, chưa chạy thật; cổng chặn của order / payment chưa cài</sub> | Nguyễn Quốc Khoa |
| `UC-AUTH-11` | Đăng xuất & Quản lý thiết bị đăng nhập | S1 | Menu Đăng xuất, `/account/devices` | ☑ | ☑ | ☑ | ☑ | ☐ | 🟡 Đang triển khai<br><sub>backend + test + web nối xong, chưa chạy thật</sub> | Nguyễn Quốc Khoa |

### Phân hệ 02 — DANH MỤC, SẢN PHẨM & TÌM KIẾM (CATALOG, PRODUCT & SEARCH)

| Mã | Use case | Sprint | Màn hình (web) | GD | BE | Test | Nối | Chạy | Trạng thái | Phụ trách |
|---|---|---|---|:---:|:---:|:---:|:---:|:---:|---|---|
| `UC-CAT-01` | Duyệt cây danh mục đa cấp (Category Hierarchy) | S2 | Header danh mục, `/c/{slug}` | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-CAT-02` | Tìm kiếm sản phẩm theo từ khóa | S2 | `/search` | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-CAT-04` | Gợi ý từ khóa tìm kiếm & Xu hướng (Autocomplete & Trending) | S7 | Ô tìm kiếm (gợi ý, xu hướng) | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-CAT-05` | Lọc nâng cao & Sắp xếp sản phẩm đa tiêu chí | S2 | `/c/{slug}`, `/search` (bộ lọc, sắp xếp) | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-CAT-06` | Xem chi tiết sản phẩm, ma trận biến thể SKU & Tồn kho | S2 | `/p/{slug}-{id}` | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-CAT-07` | Xem gợi ý sản phẩm tương tự | S7 | `/p/{slug}-{id}` (tương tự), `/discover` | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-CAT-08` | Quản lý danh sách sản phẩm yêu thích (Wishlist) | S6 | `/account/wishlist`, tim ở thẻ sản phẩm | ☑ | ☑ | ☑ | ☑ | ☐ | 🟡 Đang triển khai<br><sub>backend + test, web nối API thật (`PUBLIC_REAL_API=wishlist`); còn chạy thật trên giao diện</sub> | Nguyễn Ngọc Thiện |
| `UC-CAT-09` | Thêm mới sản phẩm & Biến thể SKU (Người bán) | S2 | `/seller/products/new` | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-CAT-10` | Cập nhật thông tin sản phẩm, giá bán & hình ảnh | S2 | `/seller/products/:id/edit` | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-CAT-11` | Ẩn / Khóa / Xóa mềm sản phẩm | S2 | `/seller/products`, Admin › Danh mục › sản phẩm | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-CAT-12` | Kiểm duyệt sản phẩm tự động qua AI (Google Gemini API) | S7 | (nền) kết quả ở `/admin/product-moderation` | — | ☐ | ☐ | — | ☐ | ⬜ Chưa triển khai<br><sub>xử lý phía máy chủ (cron / webhook / AI), giao diện chỉ hiển thị kết quả trên mock</sub> | — |
| `UC-CAT-13` | Kiểm duyệt sản phẩm thủ công theo hàng đợi (Staff Moderator) | S2 | `/admin/product-moderation` | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-CAT-14` | Quản lý danh mục sàn toàn cục & Cấu hình tỷ lệ hoa hồng | S2 | `/admin/catalog`, `/admin/commission` | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-CAT-15` | Cấu hình bộ thuộc tính chuẩn theo ngành hàng (Category Attributes) | S7 | `/admin/catalog/:id/attributes` | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-CAT-16` | Xem trang gian hàng (Shop Page) | S2 | `/shop/{id}` | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-CAT-17` | Người bán đề xuất danh mục mới & Duyệt đề xuất | S7 | `/seller/catalog`, `/admin/catalog` (đề xuất) | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-CAT-18` | Chia sẻ sản phẩm bằng liên kết (Deeplink) | S7 | Nút Chia sẻ ở `/p/…`, `/shop/…` | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |

### Phân hệ 03 — GIỎ HÀNG, ĐẶT HÀNG & ĐƠN HÀNG (CART, CHECKOUT & ORDERS)

| Mã | Use case | Sprint | Màn hình (web) | GD | BE | Test | Nối | Chạy | Trạng thái | Phụ trách |
|---|---|---|---|:---:|:---:|:---:|:---:|:---:|---|---|
| `UC-ORD-01` | Quản lý giỏ hàng mua sắm cá nhân (Shopping Cart) | S3 | `/cart`, mini-cart | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-ORD-04` | Xem trước đơn hàng & Tách giỏ hàng đa nhà bán (Cart Preview & Splitting) | S3 | `/cart`, `/checkout` | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-ORD-06` | Tính phí vận chuyển qua GHN API | S3 | `/checkout` (phí ship) | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-ORD-07` | Tạo phiên Checkout & Các Đơn hàng con theo Shop (PersistCheckout) | S3 | `/checkout` → `/checkout/result` | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-ORD-08` | Tự động hủy đơn quá hạn thanh toán & Hoàn kho (Sweeper Cron) | S3 | (nền) đơn tự hủy ở `/orders` | — | ☐ | ☐ | — | ☐ | ⬜ Chưa triển khai<br><sub>xử lý phía máy chủ (cron / webhook / AI), giao diện chỉ hiển thị kết quả trên mock</sub> | — |
| `UC-ORD-09` | Khách hàng theo dõi chi tiết hành trình đơn hàng | S4 | `/orders`, `/orders/{id}` | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-ORD-10` | Khách hàng yêu cầu hủy đơn hàng | S4 | `/orders/{id}` › Hủy đơn | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-ORD-11` | Khách hàng xác nhận "Đã nhận hàng thành công" | S4 | `/orders/{id}` › Đã nhận hàng | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-ORD-12` | Hệ thống tự động hoàn tất đơn sau 7 ngày từ khi Delivered | S4 | (nền) trạng thái Hoàn tất | — | ☐ | ☐ | — | ☐ | ⬜ Chưa triển khai<br><sub>xử lý phía máy chủ (cron / webhook / AI), giao diện chỉ hiển thị kết quả trên mock</sub> | — |

### Phân hệ 04 — THANH TOÁN, KÝ QUỸ (ESCROW) & SỔ CÁI (PAYMENTS, ESCROW & LEDGER)

| Mã | Use case | Sprint | Màn hình (web) | GD | BE | Test | Nối | Chạy | Trạng thái | Phụ trách |
|---|---|---|---|:---:|:---:|:---:|:---:|:---:|---|---|
| `UC-PAY-01` | Khởi tạo thanh toán trực tuyến (VNPay / chuyển khoản VietQR qua SePay) | S3 | `/checkout` (VNPay / VietQR), `/checkout/result` | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-PAY-03` | Xử lý IPN VNPay & webhook SePay idempotent | S3 | (nền) IPN VNPay / webhook SePay | — | ☐ | ☐ | — | ☐ | ⬜ Chưa triển khai<br><sub>xử lý phía máy chủ (cron / webhook / AI), giao diện chỉ hiển thị kết quả trên mock</sub> | — |
| `UC-PAY-05` | Ghi nhận ký quỹ (Escrow) & Khấu trừ hoa hồng | S3 | (nền) ký quỹ — hiển thị ở `/seller/wallet` | — | ☐ | ☐ | — | ☐ | ⬜ Chưa triển khai<br><sub>xử lý phía máy chủ (cron / webhook / AI), giao diện chỉ hiển thị kết quả trên mock</sub> | — |
| `UC-PAY-06` | Ghi sổ cái tài chính kép bất biến (Financial Ledger) | S3 | (nền) `/admin/ledger` | — | ☐ | ☐ | — | ☐ | ⬜ Chưa triển khai<br><sub>xử lý phía máy chủ (cron / webhook / AI), giao diện chỉ hiển thị kết quả trên mock</sub> | — |
| `UC-PAY-07` | Tự động giải phóng tiền ký quỹ về ví người bán | S4 | (nền) `/seller/wallet` | — | ☐ | ☐ | — | ☐ | ⬜ Chưa triển khai<br><sub>xử lý phía máy chủ (cron / webhook / AI), giao diện chỉ hiển thị kết quả trên mock</sub> | — |

### Phân hệ 05 — GIAN HÀNG & THẨM ĐỊNH KYC NGƯỜI BÁN (VENDOR MANAGEMENT & KYC)

| Mã | Use case | Sprint | Màn hình (web) | GD | BE | Test | Nối | Chạy | Trạng thái | Phụ trách |
|---|---|---|---|:---:|:---:|:---:|:---:|:---:|---|---|
| `UC-VND-01` | Đăng ký mở gian hàng (Become a Seller) | S2 | `/become-seller` | ☑ | ☑ | ☑ | ☑ | ☐ | 🟡 Đang triển khai<br><sub>backend + test, web nối API thật (`PUBLIC_REAL_API=vendor`); còn chạy thật trên giao diện</sub> | Nguyễn Ngọc Thiện |
| `UC-VND-02` | Tải hồ sơ pháp lý & Giấy tờ định danh KYC | S2 | `/seller/kyc` | ☑ | ☑ | ☑ | ☑ | ☐ | 🟡 Đang triển khai<br><sub>backend + test, web nối API thật (`PUBLIC_REAL_API=vendor`); còn chạy thật trên giao diện</sub> | Nguyễn Ngọc Thiện |
| `UC-VND-03` | Thẩm định KYC tự động qua AI (QR, MRZ & Face Match) | S2 | (nền) kết quả ở `/seller/kyc`, `/admin/kyc` | — | ◐ | ☑ | — | ☐ | 🟡 Đang triển khai<br><sub>phần backend + giả lập `AI_WORKER_MODE=stub` (đã chạy thử); AI Worker thật chờ plan server-python</sub> | Nguyễn Ngọc Thiện |
| `UC-VND-04` | Kiểm duyệt thủ công hồ sơ KYC (Staff / Admin) | S2 | `/admin/kyc` | ☑ | ☑ | ☑ | ☑ | ☐ | 🟡 Đang triển khai<br><sub>backend + test, web nối API thật (`PUBLIC_REAL_API=vendor`); còn chạy thật trên giao diện</sub> | Nguyễn Ngọc Thiện |
| `UC-VND-05` | Cấu hình kho lấy hàng & Đồng bộ GHN Shop ID | S2 | `/seller/settings` (kho lấy hàng) | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock · đã lập plan `plan/2026-11-16`</sub> | Nguyễn Ngọc Thiện |
| `UC-VND-06` | Cấu hình tin nhắn trả lời tự động (Auto-reply) | S7 | `/seller/settings` (tự trả lời) | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-VND-07` | Đăng ký tài khoản nhận giải ngân (Payout Bank Account) | S4 | `/seller/settings` (tài khoản nhận tiền) | ☑ | ☑ | ☑ | ☑ | ☐ | 🟡 Đang triển khai<br><sub>backend + test, web nối API thật (`PUBLIC_REAL_API=vendor`); còn chạy thật trên giao diện</sub> | Nguyễn Ngọc Thiện |
| `UC-VND-08` | Xem báo cáo doanh thu & Phân tích bán hàng (Vendor Analytics) | S6 | `/seller/analytics`, `/seller/` | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-VND-19` | Bật chế độ tạm nghỉ bán hàng của shop (Vacation Mode) | S7 | `/seller/settings` (tạm nghỉ) | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-VND-20` | Xem ví & sổ giao dịch người bán (Seller Wallet & Escrow Ledger) | S7 | `/seller/wallet` | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-VND-21` | Cập nhật thông tin gian hàng & cài đặt vận hành | S7 | `/seller/settings` | ☑ | ☑ | ☑ | ☑ | ☐ | 🟡 Đang triển khai<br><sub>backend + test, web nối API thật (`PUBLIC_REAL_API=vendor`); còn chạy thật trên giao diện</sub> | Nguyễn Ngọc Thiện |

### Phân hệ 06 — VẬN CHUYỂN QUA ĐỐI TÁC 3PL (SHIPPING VIA 3PL)

| Mã | Use case | Sprint | Màn hình (web) | GD | BE | Test | Nối | Chạy | Trạng thái | Phụ trách |
|---|---|---|---|:---:|:---:|:---:|:---:|:---:|---|---|
| `UC-LOG-01` | Tiếp nhận đơn hàng & Đóng gói tại kho shop (Fulfillment) | S4 | `/seller/orders` | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-LOG-02` | Đẩy vận đơn sang GHN & In phiếu gửi hàng PDF (Waybill Label) | S4 | `/seller/orders` (đẩy vận đơn, in phiếu) | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-LOG-03` | Đồng bộ hành trình giao nhận qua Webhook GHN | S4 | (nền) webhook 3PL — hành trình ở `/orders/{id}` | — | ☐ | ☐ | — | ☐ | ⬜ Chưa triển khai<br><sub>xử lý phía máy chủ (cron / webhook / AI), giao diện chỉ hiển thị kết quả trên mock</sub> | — |
| `UC-LOG-15` | Điều phối đa hãng vận chuyển (Dynamic Carrier Routing) | S7 | (nền) chọn hãng ở `/seller/orders` | — | ☐ | ☐ | — | ☐ | ⬜ Chưa triển khai<br><sub>xử lý phía máy chủ (cron / webhook / AI), giao diện chỉ hiển thị kết quả trên mock</sub> | — |
| `UC-LOG-16` | Người bán xác nhận / từ chối đơn hàng mới | S4 | `/seller/orders` (xác nhận / từ chối) | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |

### Phân hệ 07 — KHUYẾN MÃI, VOUCHER, FLASH SALE & ĐIỂM (PROMOTIONS, VOUCHERS, FLASH SALE & COINS)

| Mã | Use case | Sprint | Màn hình (web) | GD | BE | Test | Nối | Chạy | Trạng thái | Phụ trách |
|---|---|---|---|:---:|:---:|:---:|:---:|:---:|---|---|
| `UC-PRO-01` | Tạo & Quản lý Voucher Gian hàng (Vendor Shop Vouchers) | S5 | `/seller/promotions` | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-PRO-02` | Tạo & Quản lý Voucher Toàn sàn | S5 | `/admin/vouchers` | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-PRO-03` | Khách hàng thu thập Voucher vào Kho Voucher cá nhân | S5 | `/account/vouchers`, vé Lưu ở trang chủ / shop / chiến dịch | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-PRO-04` | Thuật toán Stacking Voucher 3 Tầng & Điểm tự động khi Checkout | S8 | `/checkout` (Voucher Picker, Điểm) | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-PRO-05` | Tạo chiến dịch Flash Sale & Cấu hình khung giờ vàng | S8 | `/admin/flash-sales`, `/seller/flash-sales` | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-PRO-06` | Mua hàng Flash Sale chống bán vượt suất | S8 | `/flash-sale`, `/p/…` | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-PRO-08` | Điểm danh nhận Điểm hàng ngày & Duy trì chuỗi điểm danh (Streak) | S8 | `/account/points` (điểm danh) | ☑ | ☑ | ☑ | ☑ | ☐ | 🟡 Đang triển khai<br><sub>backend + test, web nối API thật (`PUBLIC_REAL_API=coins`); còn chạy thật trên giao diện</sub> | Nguyễn Ngọc Thiện |
| `UC-PRO-09` | Tra cứu số dư Điểm Genie & Lịch sử biến động Điểm | S8 | `/account/points` | ☑ | ☑ | ☑ | ☑ | ☐ | 🟡 Đang triển khai<br><sub>backend + test, web nối API thật (`PUBLIC_REAL_API=coins`); còn chạy thật trên giao diện</sub> | Nguyễn Ngọc Thiện |
| `UC-PRO-10` | Sử dụng Điểm Genie để giảm trừ thanh toán đơn hàng | S8 | `/checkout` (dùng Điểm) | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |

### Phân hệ 08 — CHAT, ĐÁNH GIÁ & THEO DÕI SHOP (CHAT, REVIEWS & SHOP FOLLOW)

| Mã | Use case | Sprint | Màn hình (web) | GD | BE | Test | Nối | Chạy | Trạng thái | Phụ trách |
|---|---|---|---|:---:|:---:|:---:|:---:|:---:|---|---|
| `UC-SOC-01` | Nhắn tin trò chuyện thời gian thực Khách - Shop (Live Chat) | S6 | `/chat`, `/seller/chat` | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-SOC-02` | Gửi thẻ sản phẩm, hình ảnh và trạng thái đơn hàng trong chat | S8 | `/chat` (thẻ sản phẩm / đơn / ảnh) | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-SOC-03` | Đánh giá sản phẩm đã mua & Tải ảnh thực tế (Verified Buyer Review) | S5 | `/orders/{id}` › Đánh giá, `/p/…` | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-SOC-04` | Người bán phản hồi đánh giá của khách hàng (Vendor Review Reply) | S5 | `/seller/reviews` | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-SOC-05` | Báo cáo đánh giá vi phạm / spam & Kiểm duyệt đánh giá (Review Moderation & Report) | S8 | `/p/…` (báo cáo), `/admin/reviews` | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-SOC-06` | Khách hàng theo dõi gian hàng (Shop Follow) | S8 | `/shop/{id}`, `/account/following` | ☑ | ☑ | ☑ | ☑ | ☐ | 🟡 Đang triển khai<br><sub>backend + test, web nối API thật (`PUBLIC_REAL_API=follow`; trang shop công khai còn mock); còn chạy thật trên giao diện</sub> | Nguyễn Ngọc Thiện |
| `UC-AI-07` | Phân tích cảm xúc đánh giá bằng PhoBERT (Sentiment Analysis) | S6 | (nền) chỉ số cảm xúc ở `/seller/analytics`, `/seller/reviews` | — | ☐ | ☐ | — | ☐ | ⬜ Chưa triển khai<br><sub>xử lý phía máy chủ (cron / webhook / AI), giao diện chỉ hiển thị kết quả trên mock</sub> | — |

### Phân hệ 09 — TRẢ HÀNG, HOÀN TIỀN & KHIẾU NẠI (RETURNS, REFUNDS & DISPUTES)

| Mã | Use case | Sprint | Màn hình (web) | GD | BE | Test | Nối | Chạy | Trạng thái | Phụ trách |
|---|---|---|---|:---:|:---:|:---:|:---:|:---:|---|---|
| `UC-SUP-01` | Tạo yêu cầu Trả hàng / Hoàn tiền (Return & Refund Request) | S5 | `/orders/{id}` › Trả hàng / hoàn tiền | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-SUP-02` | Người bán tiếp nhận và xử lý yêu cầu trả hàng | S5 | `/seller/returns` | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-SUP-03` | Nhân viên sàn phân xử tranh chấp khiếu nại (Dispute Arbitration) | S5 | `/admin/refunds` | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-SUP-04` | Thực hiện hoàn tiền (VNPay / VietQR / COD) | S5 | `/admin/refunds` (hoàn tiền) | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |

### Phân hệ 10 — THÔNG BÁO (NOTIFICATIONS)

| Mã | Use case | Sprint | Màn hình (web) | GD | BE | Test | Nối | Chạy | Trạng thái | Phụ trách |
|---|---|---|---|:---:|:---:|:---:|:---:|:---:|---|---|
| `UC-NOT-01` | Thông báo trong ứng dụng thời gian thực (WebSocket) | S5 | Chuông + `/account/notifications` | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-NOT-03` | Gửi email giao dịch tự động | S5 | (nền) email giao dịch | — | ☐ | ☐ | — | ☐ | ⬜ Chưa triển khai<br><sub>xử lý phía máy chủ (cron / webhook / AI), giao diện chỉ hiển thị kết quả trên mock</sub> | — |
| `UC-NOT-05` | Admin phát thông báo toàn sàn (Broadcast Platform Notification) | S8 | `/admin/notifications` | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |

### Phân hệ 11 — QUẢN TRỊ SÀN, NHÂN VIÊN & RÚT TIỀN (PLATFORM ADMIN, STAFF & PAYOUTS)

| Mã | Use case | Sprint | Màn hình (web) | GD | BE | Test | Nối | Chạy | Trạng thái | Phụ trách |
|---|---|---|---|:---:|:---:|:---:|:---:|:---:|---|---|
| `UC-ADM-01` | Quản lý danh sách nhân viên nội bộ & Lời mời kích hoạt | S1 | `/admin/staff` | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-ADM-02` | Phân cấp vai trò & Cấp phát / Thu hồi mã quyền chi tiết (RBAC) | S1 | `/admin/staff` (phân quyền) | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-ADM-03` | Khóa / Mở khóa tài khoản nhân viên và người dùng vi phạm | S6 | `/admin/users`, `/admin/staff` | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-ADM-04` | Người bán gửi yêu cầu rút tiền từ Ví về Ngân hàng | S4 | `/seller/wallet` (rút tiền) | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-ADM-05` | Nhân viên sàn phê duyệt & Thực hiện lệnh chuyển khoản (Payout) | S4 | `/admin/withdrawals`, `/admin/cod-reconciliation` | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-ADM-06` | Dashboard doanh thu & ký quỹ toàn sàn | S6 | `/admin/` (Tổng quan) | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-ADM-07` | Tra cứu nhật ký kiểm toán hệ thống bất biến (System Audit Logs) | S7 | `/admin/audit-logs` | ☑ | ☑ | ☑ | ☑ | ☐ | 🟡 Đang triển khai<br><sub>backend + test, web nối API thật (`PUBLIC_REAL_API=audit`); còn chạy thật trên giao diện</sub> | Nguyễn Ngọc Thiện |
| `UC-ADM-12` | Quản lý banner trang chủ & chiến dịch tĩnh (CMS Banner) | S8 | `/admin/banners`, trang chủ, `/campaign/{slug}` | ☑ | ☑ | ☑ | ☑ | ☐ | 🟡 Đang triển khai<br><sub>backend + test, web nối API thật (`PUBLIC_REAL_API=banner`); còn chạy thật trên giao diện</sub> | Nguyễn Ngọc Thiện |

## 3. Việc còn lại của phân hệ đang làm (AUTH — phụ trách: Nguyễn Quốc Khoa)

- [x] `GET /me` — ngữ cảnh tài khoản (vai trò, shop, bộ đếm) để web dựng `SessionUser` đầy đủ (A11.02, A15.01) — xong 2026-11-17, BUILD SUCCESS
- [ ] Đổi email đăng nhập `POST /me/email-change-requests` (+ `/confirm`) (A11.04)
- [x] Sổ địa chỉ + danh mục hành chính 2 cấp + định vị GPS (A12 — UC-AUTH-09) — xong 2026-11-18, build xanh, đã chạy thật; danh mục mã GHN cần `GHN_TOKEN` + `GHN_SHOP_ID` (chạy `scripts/fetch-ghn-regions.py`)
  - [x] Chặn địa chỉ trùng (A12.07, plan AUTH D26) — trùng tỉnh + phường + SĐT + địa chỉ cụ thể → `409 GNE-USR-4093`
  - [x] Ghi chú cho người giao hàng (A12.08, plan AUTH D27, migration `V17`)
- [ ] ~~Apple Sign In~~ → Đăng nhập Zalo (A14.10, D23 — UC-AUTH-03): build `./mvnw verify` + `npm run verify`; app Zalo đã tạo, `ZALO_APP_ID` / `ZALO_APP_SECRET` đã có trong `.env`; **chờ tên miền thật** để xác thực domain trên Zalo rồi chạy thật
- [ ] Chạy thật: đăng xuất / thiết bị, đổi mật khẩu, xóa tài khoản (A10.03)
- [ ] E2E với API thật `tests/e2e/auth-real.spec.ts`, bộ kiểm thử Apidog / Postman, k6 đăng nhập (A15.06, A16)
- [ ] Phân hệ order / payment / vendor cài cổng `AccountDeletionBlocker` khi làm (UC-AUTH-10)
- [ ] Sửa 10 endpoint web gọi khác spec trước khi nối các phân hệ khác (API_SPEC 17.5 b)

## 4. Thứ tự phân hệ tiếp theo (gợi ý theo implement.md Phần 1.2)

| Sprint | Phân hệ / UC | Phụ thuộc |
|---|---|---|
| S2 | Hoàn tất AUTH-08/09; CAT-01, 02, 04, 05, 06, 07, 08, 16 (đọc công khai) | Seed danh mục / sản phẩm |
| S3 | CAT-09…15, 17, 18 (Seller đăng sản phẩm, kiểm duyệt Gemini); VND-01, 02, 05, 07, 21 | Silo `purpose` sản phẩm / KYC |
| S4 | ORD-01…08, PAY-01, 03, 05, 06; LOG-01, 02, 15, 16 | VNPay sandbox, webhook SePay, GHN |
| S5–S8 | Còn lại theo implement.md | — |

## 5. Nhật ký cập nhật

| Ngày | UC | Thay đổi | Commit / PR |
|---|---|---|---|
| 2026-11-14 | (nền) | 65 entity + `V1…V12`, `SchemaValidationIT` | PR #28–#51 |
| 2026-11-15 | AUTH-01, 02, 04, 06, 08, 11 | Đăng ký, OTP, đăng nhập, phiên, mật khẩu, hồ sơ + Silo | PR #52–#54 |
| 2026-11-15 | AUTH-03 | Đăng nhập Google, đổi MinIO → Silo | PR #55 |
| 2026-11-16 | AUTH-04, 02 | Quên mật khẩu, tài khoản chỉ Google (`GNE-AUTH-4012`) | PR #56, #57 |
| 2026-11-16 | AUTH-10 | Xóa tài khoản + cron ẩn danh | PR #58 |
| 2026-11-16 | AUTH-03 | Liên kết / gỡ Google ở trang tài khoản (`GNE-AUTH-4098`) | PR #59 |
| 2026-11-16 | (tài liệu) | Đồng bộ bộ tài liệu TLCN v1.4 với mã nguồn; tạo checklist này | — |
| 2026-11-17 | AUTH-03 | Zalo thay Apple: đăng nhập / đăng ký (nhập email + OTP) / liên kết Zalo — code + test viết xong, chưa build; không đổi database | (chưa commit) |
| 2026-11-17 | AUTH-03 | Tạo app Zalo; Zalo bắt Home URL `https` + xác thực domain (`localhost` bị từ chối) → tạm dừng chạy thật, chờ tên miền thật (bỏ phương án domain tạm ngrok) | — |
| 2026-11-17 | ADM-07 | Nhật ký hệ thống: `GET /admin/audit-logs` (lọc thời gian / nhóm / từ khóa, cursor, `denied_count`) + `…/export` CSV, quyền `VIEW_AUDIT_LOG`, 13 test tích hợp; web nối API thật — còn chạy thật trên giao diện | nhánh `feature/admin-audit-log` |
| 2026-11-18 | ADM-12 | Banner trang chủ: `/admin/banners` (tạo / sửa / tắt / bật / xóa, tối đa 8 banner / vị trí, liên kết trong sàn, ảnh `BANNER_IMAGE`), `/public/banners` (cache Redis 60 giây), `/public/banner-events` + job 5 phút, migration `V18` (`BANNER_CHANGED`); web nối API thật — còn chạy thật trên giao diện | nhánh `feature/admin-home-banner` |
| 2026-11-18 | PRO-08, PRO-09 | Điểm Genie: điểm danh 7 ngày (khóa Redis + khóa dòng, ngày theo giờ VN), số dư + Điểm sắp hết hạn (FEFO) + lịch sử cursor, đổi Điểm lấy voucher sàn, cron hết hạn lô (`V19`, `EXPIRED`), băm số dư `coin:` HMAC; web nối API thật — còn chạy thật trên giao diện | nhánh `feature/promotion-coin-checkin` |
| 2026-11-18 | CAT-08 | Sản phẩm yêu thích: `PUT` / `DELETE /me/wishlist/{id}` idempotent (`ON CONFLICT`), `/me/wishlist/product-ids`, `/me/wishlist` (giá thấp nhất, SKU mặc định, `is_purchasable`, `product_slug`, nạp theo lô chống N+1), cổng `WishlistShopAvailability`; web sửa 2 endpoint lệch spec + nối API thật — còn chạy thật trên giao diện | nhánh `feature/catalog-wishlist` |
| 2026-11-18 | SOC-06 | Theo dõi gian hàng: `PUT` / `DELETE /me/following/{shop_id}` idempotent, `follower_count` chỉ đổi khi dòng thật sự chèn / xóa (không đếm sai dưới tải đồng thời), `/me/following/shop-ids`, `/me/following` (`$ShopMini`, `summary.total`), cổng `ShopFollowDirectory` (module vendor cài), `GNE-SOC-4095`; web sửa endpoint lệch spec + nối API thật — còn chạy thật trên giao diện | nhánh `feature/social-shop-follow` |
| 2026-11-18 | VND-01…07, 21 | Mở shop & KYC: mở shop OTP email (giữ phiên, nâng VENDOR), cổng KYC `/seller/**`, hồ sơ KYC mã hóa AES-256-GCM (không trả URL ảnh), gửi AI Worker sau commit + callback nội bộ + chấm điểm tín hiệu cấu hình được + giả lập `AI_WORKER_MODE=stub`, duyệt tay 6 endpoint (REVIEW_KYC / UPDATE_COMMISSION), job hết hạn bổ sung, tài khoản nhận tiền OTP bước 2, cài đặt shop; migration `V20`; web nối API thật — còn chạy thật; UC-VND-05 chờ A12 | nhánh `feature/vendor-shop-kyc` |
| 2026-11-17 | AUTH-02, 08 | `GET /me` (ngữ cảnh phiên) + web dựng `SessionUser` từ `/me` sau đăng nhập / làm mới — code + test viết xong, chờ build | (chưa commit) |
| 2026-11-17 | AUTH-02, 03, 08 | `./mvnw verify` 83 test BUILD SUCCESS — `GET /me` (MeQueryIT 4/4), Zalo (ZaloLoginIT 5/5) | (chưa commit) |
| 2026-11-17 | (phân công) | Thêm cột **Phụ trách** + bảng *Thành viên & phân công*: Nguyễn Quốc Khoa — phân hệ AUTH (9 UC); Nguyễn Ngọc Thiện — 13 UC (ADM-07, ADM-12, PRO-08, PRO-09, CAT-08, SOC-06, VND-01/02/03/04/05/07/21), đang lập plan `plan/2026-11-16`; Nghiêm Phú Đăng Quân — chưa phân công. Đồng bộ cột *Người phụ trách* trong Excel | `feature/env-config-cleanup` |
| 2026-11-18 | AUTH-09 | Sổ địa chỉ + danh mục hành chính GHN (backend, test `AddressIT`, web nối qua cờ `address`) — viết xong, chờ build | (chưa commit) |
| 2026-11-18 | AUTH-09 | Build xanh (`AddressIT` 8 ca) — BE, Test ☑; Nối ◐ (GPS còn mô phỏng) | (chưa commit) |
| 2026-11-18 | AUTH-09 | Định vị GPS thật (Nominatim → khớp mã GHN) — viết xong, chờ build | (chưa commit) |
| 2026-11-18 | AUTH-09 | Thêm loại địa chỉ Trường học / Chung cư / Khác (migration `V15`) | (chưa commit) |
| 2026-11-18 | AUTH-09 | Chuyển sổ địa chỉ sang **địa giới 2 cấp** Tỉnh → Phường (plan AUTH D25, migration `V19`, danh mục 34 tỉnh / 3.321 xã; GHN v3); trạng thái giữ nguyên, chờ verify + kiểm thử giao diện | (chưa commit) |
| 2026-11-18 | AUTH-09 | Thêm việc **chặn địa chỉ trùng** (A12.07, D26) vào kế hoạch — chưa làm | — |
| 2026-11-18 | AUTH-09 | Chặn địa chỉ trùng (A12.07) — code + test viết xong, chờ build | (chưa commit) |
| 2026-11-18 | AUTH-09 | Ghi chú cho người giao hàng (A12.08, migration `V20`) — code + test viết xong, chờ build | (chưa commit) |
| 2026-11-18 | AUTH-09 | Build xanh + chạy thật trên giao diện (địa giới 2 cấp, loại địa chỉ, chặn trùng, ghi chú, GPS) → **✅ Đã triển khai**; giao diện: tìm nhanh không phân biệt dấu, thẻ địa chỉ cân lại, placeholder nhạt hơn; trang chủ: bảng đường tiền chồng 1/3 ảnh | (chưa commit) |
