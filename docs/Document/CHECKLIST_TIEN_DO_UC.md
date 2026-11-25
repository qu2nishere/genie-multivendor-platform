# CHECKLIST TIẾN ĐỘ USE CASE — GENIE (BẢN TLCN)

> **Cập nhật:** 2026-11-24 · **Nguồn đặc tả:** [Usecase.md](diagrams/UseCaseDiagram/Usecase.md) (TLCN v1.4) · **Nguồn trạng thái:** mã nguồn `genie-multivendor-platform` (plan `plan/2026-11-11/web/` G00–G13, `plan/2026-11-14/auth.md` A00–A16, `plan/2026-11-16/`, `plan/2026-11-17/` C00–C08, `plan/2026-11-19/b1.md` R00–R10, `plan/2026-11-19/b2.md` C00–C09, `docs/web-backend-handoff.md`).
> **Bảng quản lý đầy đủ (tác nhân, quan hệ, lọc):** [Quản Lý UseCase.xlsx](diagrams/UseCaseDiagram/Quản%20Lý%20UseCase.xlsx) — cột *Hiện trạng mã nguồn* đã đồng bộ với file này.
> **Cách dùng:** mỗi khi xong một lớp của UC thì đổi ô `☐` → `☑` (một phần: `◐`), cập nhật cột Trạng thái và ghi mã commit / PR vào Nhật ký cuối file. Một UC chỉ **Đã triển khai** khi đủ 5 lớp (Definition of Done — implement.md 15.3).

## Thành viên & phân công

> Cập nhật 2026-11-18. Cột **Phụ trách** ở mọi bảng use case bên dưới ghi người nhận làm; `—` = chưa phân công. Đổi / nhận thêm use case thì sửa cột đó, bảng này, cột *Người phụ trách* trong `Quản Lý UseCase.xlsx` và ghi một dòng Nhật ký.

| Thành viên | Phụ trách | Hiện trạng |
|---|---|---|
| **Nguyễn Quốc Khoa** | Toàn bộ phân hệ 01 — Xác thực & Tài khoản (9 use case `UC-AUTH-*`) · cụm B1 Nhân viên & phân quyền: UC-ADM-01, 02, 03 (nhận 2026-11-19) · cụm B2 Danh mục & hoa hồng: UC-CAT-01, 14, 15 (nhận 2026-11-22) | Đang triển khai — AUTH: 4 đã xong, 5 đang làm (UC-AUTH-09 xong 2026-11-18); plan `plan/2026-11-14/auth.md`. B1: **3 / 3 ✅** (UC-ADM-01, 02 xong 2026-11-20; UC-ADM-03 xong 2026-11-24 — cron hết hạn đình chỉ làm bù ở plan rà soát 2026-11-24). B2: **3 / 3 ✅** (2026-11-24 — test bổ sung + sửa giao diện quản lý ngành con ở plan rà soát), plan `plan/2026-11-19/b2.md` |
| **Nguyễn Ngọc Thiện** | 13 use case: Tra cứu nhật ký kiểm toán hệ thống bất biến (UC-ADM-07) · Quản lý banner trang chủ & chiến dịch tĩnh (UC-ADM-12) · Điểm danh nhận Điểm hàng ngày & Duy trì chuỗi điểm danh (UC-PRO-08) + Tra cứu số dư Điểm Genie & Lịch sử biến động Điểm (UC-PRO-09) · Quản lý danh sách sản phẩm yêu thích (UC-CAT-08) · Khách hàng theo dõi gian hàng (UC-SOC-06) · cụm B4 Mở shop & KYC: UC-VND-01, 02, 03, 04, 05, 07, 21 | Đang triển khai — seed dữ liệu mẫu xong (nhánh `feature/db-seed-demo`); UC-ADM-07 chờ chạy thật (nhánh `feature/admin-audit-log`); UC-ADM-12 chờ chạy thật (nhánh `feature/admin-home-banner`); UC-PRO-08, PRO-09 chờ chạy thật (nhánh `feature/promotion-coin-checkin`); UC-CAT-08 chờ chạy thật (nhánh `feature/catalog-wishlist`); UC-SOC-06 chờ chạy thật (nhánh `feature/social-shop-follow`); UC-VND-01, 02, 03, 04, 07, 21 đã merge vào `develop` (PR #83 `feature/vendor-shop-kyc`, #84 `feature/ai-worker-kyc`, #96 sửa KYC ảnh thật) — chờ chạy thật trên giao diện; UC-VND-05 chưa có backend (A12 đã có, còn K07 `PUT /seller/pickup-address`) |
| **Nghiêm Phú Đăng Quân** | 7 use case cụm khám phá sản phẩm: Tìm kiếm sản phẩm theo từ khóa (UC-CAT-02) · Gợi ý từ khóa tìm kiếm & Xu hướng (UC-CAT-04) · Lọc nâng cao & Sắp xếp sản phẩm đa tiêu chí (UC-CAT-05) · Xem chi tiết sản phẩm, ma trận biến thể SKU & Tồn kho (UC-CAT-06) · Xem gợi ý sản phẩm tương tự (UC-CAT-07) · Xem trang gian hàng (UC-CAT-16) · Chia sẻ sản phẩm bằng liên kết — Deeplink (UC-CAT-18) | Cụm B3 Storefront đọc công khai — **7 / 7 UC ✅ Đã triển khai** (UC-CAT-02, 04, 06, 07, 16, 18 chạy thật 2026-11-21; UC-CAT-05 xong 2026-11-24 — lọc "Shop có voucher" + bộ lọc thuộc tính từ facets làm bù ở plan rà soát 2026-11-24); plan `plan/2026-11-17/server-spring/plan_tim_kiem_va_trang_san_pham.md` (66 / 66) |

## 1. Tổng quan

| Chỉ số | Số lượng |
|---|---:|
| Tổng use case | **87** |
| Đã triển khai (backend + test + web nối API thật) | **17** |
| Đang triển khai | **17** |
| Chưa có backend — giao diện đã dựng trên lớp mock | **42** |
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
| 02. Danh mục, Sản phẩm & Tìm kiếm | 17 | 10 | 1 | 6 |
| 03. Giỏ hàng, Đặt hàng & Đơn hàng | 9 | 0 | 0 | 9 |
| 04. Thanh toán, Ký quỹ & Sổ cái | 5 | 0 | 0 | 5 |
| 05. Gian hàng & KYC người bán | 11 | 0 | 6 | 5 |
| 06. Vận chuyển 3PL | 5 | 0 | 0 | 5 |
| 07. Khuyến mãi, Voucher, Flash Sale & Điểm | 9 | 0 | 2 | 7 |
| 08. Chat, Đánh giá & Theo dõi shop | 7 | 0 | 1 | 6 |
| 09. Trả hàng, Hoàn tiền & Khiếu nại | 4 | 0 | 0 | 4 |
| 10. Thông báo | 3 | 0 | 0 | 3 |
| 11. Quản trị sàn, Nhân viên & Rút tiền | 8 | 3 | 2 | 3 |

## 2. Checklist từng use case

### Phân hệ 01 — XÁC THỰC & TÀI KHOẢN NGƯỜI DÙNG (AUTHENTICATION & ACCOUNT)

| Mã | Use case | Sprint | Màn hình (web) | GD | BE | Test | Nối | Chạy | Trạng thái | Phụ trách |
|---|---|---|---|:---:|:---:|:---:|:---:|:---:|---|---|
| `UC-AUTH-01` | Đăng ký tài khoản Khách hàng | S1 | `/auth/register` | ☑ | ☑ | ☑ | ☑ | ☑ | ✅ Đã triển khai<br><sub>backend + test, web nối API thật, đã chạy thật</sub> | Nguyễn Quốc Khoa |
| `UC-AUTH-02` | Đăng nhập hệ thống | S1 | `/auth/login` (mật khẩu / OTP), màn tạm khóa | ☑ | ☑ | ☑ | ☑ | ☑ | ✅ Đã triển khai<br><sub>backend + test, web nối API thật; GET /me dựng phiên đầy đủ (2026-11-17)</sub> | Nguyễn Quốc Khoa |
| `UC-AUTH-03` | Đăng nhập & Liên kết tài khoản Google / Zalo (OAuth2 / OpenID Connect) | S7 | `/auth/login`, `/auth/oauth-callback`, `/account/logins` | ☑ | ◐ | ☑ | ☑ | ◐ | 🟡 Đang triển khai<br><sub>Google xong (đăng nhập, liên kết / gỡ liên kết). Zalo thay Apple (2026-11-17): code + test xong, build xanh; chạy thật tạm dừng — Zalo bắt xác thực domain, chờ tên miền thật; tài khoản nội bộ STAFF / ADMIN không đăng nhập / liên kết Google, Zalo (`GNE-AUTH-4033`, 2026-11-20)</sub> | Nguyễn Quốc Khoa |
| `UC-AUTH-04` | Khôi phục mật khẩu qua Email OTP (Forgot Password) | S1 | `/auth/forgot` | ☑ | ☑ | ☑ | ☑ | ☑ | ✅ Đã triển khai<br><sub>backend + test, web nối API thật, đã chạy thật</sub> | Nguyễn Quốc Khoa |
| `UC-AUTH-06` | Đổi / Thiết lập mật khẩu | S1 | `/account/password`, Seller › Cài đặt › Bảo mật, Admin › Tài khoản | ☑ | ☑ | ☑ | ☑ | ☐ | 🟡 Đang triển khai<br><sub>backend + test + web nối xong, chưa chạy thật</sub> | Nguyễn Quốc Khoa |
| `UC-AUTH-08` | Quản lý thông tin hồ sơ cá nhân (Customer Profile) | S2 | `/account/profile`, Admin › Tài khoản | ☑ | ◐ | ◐ | ◐ | ☐ | 🟡 Đang triển khai<br><sub>hồ sơ + ảnh đại diện (Silo) xong; GET /me xong; còn đổi email</sub> | Nguyễn Quốc Khoa |
| `UC-AUTH-09` | Quản lý sổ địa chỉ giao hàng chuẩn GHN | S2 | `/account/address`, modal địa chỉ ở `/checkout` | ☑ | ☑ | ☑ | ☑ | ☑ | ✅ Đã triển khai<br><sub>sổ địa chỉ địa giới 2 cấp (34 tỉnh / 3.321 xã), 5 loại địa chỉ, chặn trùng, ghi chú giao hàng, định vị GPS thật; build xanh + đã chạy thật 2026-11-18. Danh mục mã GHN cần `GHN_TOKEN` + `GHN_SHOP_ID`</sub> | Nguyễn Quốc Khoa |
| `UC-AUTH-10` | Yêu cầu ẩn danh hóa & Xóa dữ liệu cá nhân (Xóa tài khoản) | S7 | `/account/profile` › Xóa tài khoản | ☑ | ☑ | ☑ | ☑ | ☐ | 🟡 Đang triển khai<br><sub>backend + test + web nối xong, chưa chạy thật; cổng chặn của order / payment chưa cài</sub> | Nguyễn Quốc Khoa |
| `UC-AUTH-11` | Đăng xuất & Quản lý thiết bị đăng nhập | S1 | Menu Đăng xuất, `/account/devices` | ☑ | ☑ | ☑ | ☑ | ☐ | 🟡 Đang triển khai<br><sub>backend + test + web nối xong, chưa chạy thật</sub> | Nguyễn Quốc Khoa |

### Phân hệ 02 — DANH MỤC, SẢN PHẨM & TÌM KIẾM (CATALOG, PRODUCT & SEARCH)

| Mã | Use case | Sprint | Màn hình (web) | GD | BE | Test | Nối | Chạy | Trạng thái | Phụ trách |
|---|---|---|---|:---:|:---:|:---:|:---:|:---:|---|---|
| `UC-CAT-01` | Duyệt cây danh mục đa cấp (Category Hierarchy) | S2 | Header danh mục, `/c/{slug}` | ☑ | ☑ | ☑ | ☑ | ☑ | ✅ Đã triển khai<br><sub>cây công khai (cache Redis, Redis lỗi đọc CSDL, ETag 304), header + "Tất cả danh mục", `/c/{slug}` (breadcrumb, 301 ngành con, 404 thật) — TC-B2-01 → 05, 15, 31 đạt, chạy thật 2026-11-24 bằng Chromium tự động (Playwright) trên hệ thống thật theo yêu cầu người dùng — plan `plan/2026-11-24/server-spring/plan_hoan_thien_b1_b2_b3.md`</sub> | Nguyễn Quốc Khoa |
| `UC-CAT-02` | Tìm kiếm sản phẩm theo từ khóa | S2 | `/search` | ☑ | ☑ | ☑ | ☑ | ☑ | ✅ Đã triển khai<br><sub>backend + test + web nối xong (cờ `product`); chạy thật 2026-11-21: tìm có dấu / không dấu / gõ sai, từ lạ → trang rỗng · plan `plan/2026-11-17`</sub> | Nghiêm Phú Đăng Quân |
| `UC-CAT-04` | Gợi ý từ khóa tìm kiếm & Xu hướng (Autocomplete & Trending) | S7 | Ô tìm kiếm (gợi ý, xu hướng) | ☑ | ☑ | ☑ | ☑ | ☑ | ✅ Đã triển khai<br><sub>backend + test + web nối xong (cờ `search`); chạy thật 2026-11-21: ô trống hiện xu hướng, gõ "ao th" ra gợi ý từ CSDL · plan `plan/2026-11-17`</sub> | Nghiêm Phú Đăng Quân |
| `UC-CAT-05` | Lọc nâng cao & Sắp xếp sản phẩm đa tiêu chí | S2 | `/c/{slug}`, `/search` (bộ lọc, sắp xếp) | ☑ | ☑ | ☑ | ☑ | ☑ | ✅ Đã triển khai<br><sub>đủ tiêu chí của Usecase: giá, sao, nơi bán, thương hiệu, danh mục / con, Chính hãng, Freeship, **Shop có voucher** (module promotion cài cổng, cache 60 giây; có ở `/search` và `/c/{slug}`), thuộc tính ngành lấy từ `summary.facets.attribute`; 16 / 16 TC-B3 đạt (cả khổ hẹp, tắt JS, 9 theme), chạy thật 2026-11-24 bằng Chromium tự động (Playwright) trên hệ thống thật theo yêu cầu người dùng — plan `plan/2026-11-24/server-spring/plan_hoan_thien_b1_b2_b3.md`</sub> | Nghiêm Phú Đăng Quân |
| `UC-CAT-06` | Xem chi tiết sản phẩm, ma trận biến thể SKU & Tồn kho | S2 | `/p/{slug}-{id}` | ☑ | ☑ | ☑ | ☑ | ☑ | ✅ Đã triển khai<br><sub>backend + test + web nối xong (SKU, tồn, lượt xem); chạy thật 2026-11-21: SKU hết hàng bị khóa, chọn SKU, slug sai → 301, id lạ → 404, lượt xem ghi xuống CSDL theo nguồn · plan `plan/2026-11-17`</sub> | Nghiêm Phú Đăng Quân |
| `UC-CAT-07` | Xem gợi ý sản phẩm tương tự | S7 | `/p/{slug}-{id}` (tương tự), `/discover` | ☑ | ☑ | ☑ | ☑ | ☑ | ✅ Đã triển khai<br><sub>backend + test + web nối xong (tương tự + gợi ý hôm nay); chạy thật 2026-11-21: khối tương tự 6 sản phẩm cùng loại (không lặp chính nó), `/discover` · plan `plan/2026-11-17`</sub> | Nghiêm Phú Đăng Quân |
| `UC-CAT-08` | Quản lý danh sách sản phẩm yêu thích (Wishlist) | S6 | `/account/wishlist`, tim ở thẻ sản phẩm | ☑ | ☑ | ☑ | ☑ | ☐ | 🟡 Đang triển khai<br><sub>backend + test, web nối API thật (`PUBLIC_REAL_API=wishlist`); còn chạy thật trên giao diện</sub> | Nguyễn Ngọc Thiện |
| `UC-CAT-09` | Thêm mới sản phẩm & Biến thể SKU (Người bán) | S2 | `/seller/products/new` | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-CAT-10` | Cập nhật thông tin sản phẩm, giá bán & hình ảnh | S2 | `/seller/products/:id/edit` | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-CAT-11` | Ẩn / Khóa / Xóa mềm sản phẩm | S2 | `/seller/products`, Admin › Danh mục › sản phẩm | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-CAT-12` | Kiểm duyệt sản phẩm tự động qua AI (Google Gemini API) | S7 | (nền) kết quả ở `/admin/product-moderation` | — | ☐ | ☐ | — | ☐ | ⬜ Chưa triển khai<br><sub>xử lý phía máy chủ (cron / webhook / AI), giao diện chỉ hiển thị kết quả trên mock</sub> | — |
| `UC-CAT-13` | Kiểm duyệt sản phẩm thủ công theo hàng đợi (Staff Moderator) | S2 | `/admin/product-moderation` | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-CAT-14` | Quản lý danh mục sàn toàn cục & Cấu hình tỷ lệ hoa hồng | S2 | `/admin/catalog`, `/admin/commission` | ☑ | ☑ | ☑ | ☑ | ☑ | ✅ Đã triển khai<br><sub>quản lý cây 3 cấp — sửa / ẩn / xóa ngành **mọi cấp** (sửa giao diện H15), trùng slug / tên 4091 tại ô, xóa còn con / sản phẩm / voucher bị chặn; hoa hồng mặc định + theo ngành (3,5–8 %, lưu hàng loạt, `COMMISSION_UPDATED`); `CatalogSecurityIT` ma trận 7 vai trò — TC-B2-06 → 18, 27 → 30, 32 đạt, chạy thật 2026-11-24 bằng Chromium tự động (Playwright) trên hệ thống thật theo yêu cầu người dùng — plan `plan/2026-11-24/server-spring/plan_hoan_thien_b1_b2_b3.md`. Duyệt đề xuất thuộc UC-CAT-17 (còn mock)</sub> | Nguyễn Quốc Khoa |
| `UC-CAT-15` | Cấu hình bộ thuộc tính chuẩn theo ngành hàng (Category Attributes) | S7 | `/admin/catalog/:id/attributes` | ☑ | ☑ | ☑ | ☑ | ☑ | ✅ Đã triển khai<br><sub>bộ thuộc tính theo ngành **mọi cấp** + bảng kế thừa, mã duy nhất trong nhánh (4095), kiểu dữ liệu khóa khi sửa, ân hạn 14 ngày, ngừng dùng — TC-B2-19 → 26 (+ TC-B3-13 lọc theo thuộc tính) đạt, chạy thật 2026-11-24 bằng Chromium tự động (Playwright) trên hệ thống thật theo yêu cầu người dùng — plan `plan/2026-11-24/server-spring/plan_hoan_thien_b1_b2_b3.md`. Kiểm thuộc tính bắt buộc khi đăng bán làm ở UC-CAT-09</sub> | Nguyễn Quốc Khoa |
| `UC-CAT-16` | Xem trang gian hàng (Shop Page) | S2 | `/shop/{id}` | ☑ | ☑ | ☑ | ☑ | ☑ | ✅ Đã triển khai<br><sub>backend + test + web nối xong (cờ `shop`); chạy thật 2026-11-21: từ trang sản phẩm sang `/shop/3` đúng thông tin + sản phẩm của shop, shop lạ → 404 · plan `plan/2026-11-17`</sub> | Nghiêm Phú Đăng Quân |
| `UC-CAT-17` | Người bán đề xuất danh mục mới & Duyệt đề xuất | S7 | `/seller/catalog`, `/admin/catalog` (đề xuất) | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock</sub> | — |
| `UC-CAT-18` | Chia sẻ sản phẩm bằng liên kết (Deeplink) | S7 | Nút Chia sẻ ở `/p/…`, `/shop/…` | ☑ | ☑ | ☑ | ☑ | ☑ | ✅ Đã triển khai<br><sub>deeplink `?v={variant_id}&src=share` + OG giá + ghi lượt xem; chạy thật 2026-11-21: link `?v=25` chọn sẵn SKU, ghi lượt xem `EXTERNAL_LINK`, thẻ OG / `product:price` / canonical đúng. Dán vào Zalo / Facebook để xem ảnh xem trước cần tên miền công khai (localhost không dán được) · plan `plan/2026-11-17`</sub> | Nghiêm Phú Đăng Quân |

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
| `UC-VND-03` | Thẩm định KYC tự động qua AI (QR, MRZ & Face Match) | S2 | (nền) kết quả ở `/seller/kyc`, `/admin/kyc` | — | ☑ | ☑ | — | ☐ | 🟡 Đang triển khai<br><sub>backend + AI Worker (QR zxing-cpp, MRZ, luật, YuNet + SFace) + test, đã chạy hai phía bằng thẻ mẫu; pipeline chạy trên ảnh CCCD chụp điện thoại thật → 0 điểm, tự duyệt (PR #96, 2026-11-22); còn chạy thật trên giao diện</sub> | Nguyễn Ngọc Thiện |
| `UC-VND-04` | Kiểm duyệt thủ công hồ sơ KYC (Staff / Admin) | S2 | `/admin/kyc` | ☑ | ☑ | ☑ | ☑ | ☐ | 🟡 Đang triển khai<br><sub>backend + test, web nối API thật (`PUBLIC_REAL_API=vendor`); còn chạy thật trên giao diện</sub> | Nguyễn Ngọc Thiện |
| `UC-VND-05` | Cấu hình kho lấy hàng & Đồng bộ GHN Shop ID | S2 | `/seller/settings` (kho lấy hàng) | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai<br><sub>giao diện đã dựng trên lớp mock (cờ `pickup`) · plan `plan/2026-11-16` K07 chưa làm — danh mục hành chính GHN (A12, `RegionDirectory`) nay đã có nên hết bị chặn; còn `PickupShopRegistrar` + `PUT /seller/pickup-address` + IT</sub> | Nguyễn Ngọc Thiện |
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
| `UC-ADM-01` | Quản lý danh sách nhân viên nội bộ & Lời mời kích hoạt | S1 | `/admin/staff`, `/auth/staff-activation` (mới) | ☑ | ☑ | ☑ | ☑ | ☑ | ✅ Đã triển khai<br><sub>2026-11-20: mời (email mọi tên miền, giới hạn bằng `STAFF_EMAIL_DOMAINS`; cảnh báo tức thời khi email trùng tài khoản có sẵn), gửi lại, lời mời hết hạn `INVITATION_EXPIRED`, kích hoạt OTP + mật khẩu, khóa / xóa nhân viên; tài khoản nội bộ chỉ xem storefront (`GNE-ORD-4031`) và không đăng nhập Google / Zalo (`GNE-AUTH-4033`) — build + verify xanh, chạy thật xong</sub> | Nguyễn Quốc Khoa |
| `UC-ADM-02` | Phân cấp vai trò & Cấp phát / Thu hồi mã quyền chi tiết (RBAC) | S1 | `/admin/staff` (phân quyền) | ☑ | ☑ | ☑ | ☑ | ☑ | ✅ Đã triển khai<br><sub>2026-11-20: `PUT /admin/staff/{id}/permissions` (thay bộ quyền theo chênh lệch, audit từng mã, cảnh báo tách biệt nhiệm vụ, chặn `MANAGE_RBAC`, xóa cache quyền; 2026-11-21: không còn đăng xuất nhân viên — menu tự cập nhật realtime), `GET /admin/permission-catalog`, menu Admin lọc theo `/admin/me` — build + verify xanh, chạy thật xong</sub> | Nguyễn Quốc Khoa |
| `UC-ADM-03` | Khóa / Mở khóa tài khoản nhân viên và người dùng vi phạm | S6 | `/admin/users`, `/admin/staff` | ☑ | ☑ | ☑ | ☑ | ☑ | ✅ Đã triển khai<br><sub>khóa / mở khóa nhân viên (`/admin/staff`) và khách / shop vi phạm (`/admin/users`: 4 tab, 7 / 30 ngày / vô thời hạn, mở khóa có lý do, đăng xuất ngay qua WebSocket); **cron `SuspensionExpiryJob` 5 phút** tự mở khi hết hạn đình chỉ (`ACCOUNT_REACTIVATED` · `SUSPENSION_EXPIRED` · `CRON`) + email "Tài khoản đã được mở lại" (cả khi Admin mở thủ công) — 12 / 12 TC-B1 đạt, chạy thật 2026-11-24 bằng Chromium tự động (Playwright) trên hệ thống thật theo yêu cầu người dùng — plan `plan/2026-11-24/server-spring/plan_hoan_thien_b1_b2_b3.md`</sub> | Nguyễn Quốc Khoa |
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

### Cụm B1 — Nhân viên & phân quyền (UC-ADM-01, 02, 03 — phụ trách: Nguyễn Quốc Khoa, plan `plan/2026-11-19/b1.md`)

- [x] Lập plan, chốt D1–D15 (2026-11-19)
- [x] R01 — (phần backend + tài liệu; phần web làm ở R08) sửa API_SPEC 2.11 / 11.13 / 11.18 (OTP kích hoạt, `INVITATION_EXPIRED`, `invitation_expires_at`), mã lỗi `GNE-ADM-*`, `OtpPurpose.STAFF_ACTIVATION`, mẫu email mời
- [x] R02 — (build xanh, `StaffActivationIT` 5/5) `auth`: tạo tài khoản nhân viên, mã lời mời Redis `auth:staff_invite:*`, `POST /auth/staff-activations` (+ `/otp-challenges`)
- [x] R03 — (build xanh, chạy thật 2026-11-20) cấp / thu hồi 13 mã quyền, `GET /admin/permission-catalog`, `evict` sau commit
- [x] R04 — (build xanh, chạy thật 2026-11-20; kèm `GET`/`PATCH /admin/me`, `GET /admin/staff/email-check`) `/admin/staff/**`: danh sách (kể cả lời mời hết hạn), mời, gửi lại, sửa quyền, khóa / mở, xóa
- [x] R05 — (build + chạy thật 2026-11-21) `/admin/users/**`: danh sách 4 tab, đình chỉ 7 / 30 ngày / vô thời hạn, mở khóa; web nối nhóm `users`
- [x] R06 — cron `SuspensionExpiryJob` (5 phút) + email mở lại tài khoản — làm ở plan rà soát 2026-11-24 (H02–H04), 12 / 12 TC-B1 đạt
- [x] R08 — nối web `/admin/staff` (nhóm `rbac` ✔), khung Admin `/admin/me` (nhóm `auth` ✔), màn `/auth/staff-activation` ✔, cảnh báo email trùng ✔, `/admin/users` (nhóm `users`) ✔ — chạy thật xong 2026-11-21
- [x] Giao diện chuyển kênh (2026-11-20): menu tài khoản storefront có **Trang quản trị** (ADMIN / STAFF), header Admin Console có menu tài khoản (ảnh + tên + chức danh) với **Kênh người mua**, Tài khoản của tôi, Đăng xuất; đăng nhập ADMIN / STAFF không có `next` → `/admin`
- [x] Chốt D3 (2026-11-20): bỏ ràng buộc `@genie.example` — mời bằng mọi email hợp lệ; production giới hạn bằng `STAFF_EMAIL_DOMAINS`
- [x] Chốt lại D16 (2026-11-20, hướng B): tài khoản nhân viên / Admin **chỉ xem** storefront, không mua hàng
- [x] R10 — (build + chạy thật 2026-11-20) chặn mua hàng cho tài khoản nội bộ: `BuyerOnlyFilter` `/me/cart/**`, `/me/checkout/**` → `403 GNE-ORD-4031`; web ẩn giỏ hàng / mua ngay, khối mua hiện dòng giải thích, menu tài khoản rút gọn
- [x] Chốt D17 (2026-11-20): tài khoản nội bộ chỉ đăng nhập email + mật khẩu — chặn đăng nhập / liên kết Google, Zalo (`403 GNE-AUTH-4033`, câu báo chung không lộ tài khoản nội bộ)
- [x] Chạy thật (2026-11-20): Admin mời → email → kích hoạt → nhân viên thấy đúng menu → đổi quyền / khóa có hiệu lực ngay
- [ ] Commit toàn bộ B1 lên nhánh `feature/b1` (30 commit tách nhỏ, chờ duyệt kịch bản)

### Cụm B2 — Danh mục & hoa hồng (UC-CAT-01, 14, 15 — phụ trách: Nguyễn Quốc Khoa, plan `plan/2026-11-19/b2.md`)

> **Đã test (2026-11-24):** `./mvnw verify` xanh (177 unit + 292 IT; `AdminCategoryIT` 5, `CategoryAttributeIT` 3, `CommissionIT` 2, `PublicCategoryIT` 2, `CategoryIndexTest` 4), `npm run verify` xanh (74 file / 346 test, build, i18n). Backend thật chạy đủ 26 bước `docs/api-tests/catalog-category.http` + ân hạn 14 ngày trên `dien-tu`; `system_logs` có `COMMISSION_UPDATED` (DEFAULT + theo ngành); SSR `/c/dien-tu` 200 kèm BreadcrumbList, slug lạ 404. Cột Test → `☑`. **Còn:** chạy thật trên giao diện Admin (cột Chạy) rồi mới ✅.

- [x] Chốt D1–D17 (2026-11-22): D2 = bảng `platform_settings` (`V23`), D3 = UNIQUE slug (`V22`), còn lại theo đề xuất
- [x] C01–C06 — backend viết xong: cây công khai `GET /public/categories` (+ `/{slug}`), quản lý cây `/admin/categories` (`MANAGE_CATALOG`; đổi hoa hồng cần thêm `UPDATE_COMMISSION`), `/admin/commission-settings`, bộ thuộc tính `/admin/categories/{id}/attributes`; mã lỗi `GNE-CAT-4041 / 4091 / 4092 / 4095`; `CategoryFacade.effectiveCommissionRate`
- [x] C07 — (build xanh 2026-11-24) chạy `./mvnw verify` (5 lớp test mới: `AdminCategoryIT`, `PublicCategoryIT`, `CommissionIT`, `CategoryAttributeIT`, `CategoryIndexTest`); `CatalogSecurityIT` ma trận 7 vai trò đã viết 2026-11-24 (plan rà soát H06) — khớp spec
- [x] C08 — nối web nhóm `catalog`, `admin-catalog`, `commission` (tsc / prettier / i18n sạch; chờ `npm run verify` trên máy chủ dự án); vẫn mock: `attribute-filters` (UC-CAT-05), `catalog-proposals` (UC-CAT-17), `catalog-products` (UC-CAT-12, 13)
- [x] Chạy thật trên giao diện (2026-11-24, Chromium tự động — plan rà soát H12, H15): 32 / 32 TC-B2 đạt sau khi sửa giao diện quản lý ngành con + khóa kiểu dữ liệu thuộc tính (H15)
- [ ] `CategoryFacade.attributesFor` (bộ thuộc tính kế thừa cho form đăng bán) — để plan UC-CAT-09 làm cùng phần kiểm thuộc tính bắt buộc
- [x] Commit B2 lên nhánh `feature/b2` — đã gộp `develop` (PR #92)

### Cụm tìm kiếm & trang sản phẩm (UC-CAT-02, 04, 05, 06, 07, 16, 18 — phụ trách: Nghiêm Phú Đăng Quân)

- [x] Chạy thật hai phía (`PUBLIC_REAL_API=product,search,shop`) theo progress C07.08 (2026-11-21) — 6 UC ✅, UC-CAT-05 Chạy ◐
- [x] Sửa ETag `/public/**` (2026-11-24): `ContentEtagHeaderFilter` băm thân phản hồi bỏ khối `meta` (`request_id`, `timestamp`) → gửi lại `If-None-Match` nhận `304`; test hồi quy `ProductSearchIT.sameContentGives304AndChangedContentGivesNewEtag`
- [ ] Dán link chia sẻ vào Zalo / Facebook Sharing Debugger khi có tên miền công khai (UC-CAT-18 — thẻ OG đã kiểm trên HTML)
- [x] Lọc "Shop có voucher" (UC-CAT-05): module promotion cài cổng `ShopVoucherAvailability` (2026-11-24, plan rà soát H08–H11; cache 60 giây — cụm B5 phải xóa khóa khi tạo / sửa voucher shop)
- [x] Lọc danh mục trên web khớp CSDL thật khi UC-CAT-01 (cây danh mục) nối API — kiểm 2026-11-24: `/c/dien-tu` và `?sub=` lấy sản phẩm thật, URL ngành con → 301 về `?sub=`
- [ ] Plan sản phẩm người bán (UC-CAT-09/10/11/13) gọi `ProductDetailService.evict(id)` khi sửa / ẩn; lọc HTML `description` khi lưu
- [ ] Plan Mở shop & KYC (UC-VND-21) gọi `PublicShopService.evict(shopId)` khi sửa thông tin shop

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
| 2026-11-17 | (nền) | Bộ dữ liệu mẫu `db/seed` (23 danh mục, 6 shop KYC, 200 sản phẩm, voucher, banner, Điểm / yêu thích / theo dõi của `mai.tran`, hàng chờ KYC) + `scripts/seed/seed.{sh,ps1}`, `upload-assets` — plan `2026-11-16/db` | nhánh `feature/db-seed-demo` |
| 2026-11-17 | ADM-07 | Nhật ký hệ thống: `GET /admin/audit-logs` (lọc thời gian / nhóm / từ khóa, cursor, `denied_count`) + `…/export` CSV, quyền `VIEW_AUDIT_LOG`, 13 test tích hợp; web nối API thật — còn chạy thật trên giao diện | nhánh `feature/admin-audit-log` |
| 2026-11-18 | ADM-12 | Banner trang chủ: `/admin/banners` (tạo / sửa / tắt / bật / xóa, tối đa 8 banner / vị trí, liên kết trong sàn, ảnh `BANNER_IMAGE`), `/public/banners` (cache Redis 60 giây), `/public/banner-events` + job 5 phút, migration `V18` (`BANNER_CHANGED`); web nối API thật — còn chạy thật trên giao diện | nhánh `feature/admin-home-banner` |
| 2026-11-18 | PRO-08, PRO-09 | Điểm Genie: điểm danh 7 ngày (khóa Redis + khóa dòng, ngày theo giờ VN), số dư + Điểm sắp hết hạn (FEFO) + lịch sử cursor, đổi Điểm lấy voucher sàn, cron hết hạn lô (`V19`, `EXPIRED`), băm số dư `coin:` HMAC; web nối API thật — còn chạy thật trên giao diện | nhánh `feature/promotion-coin-checkin` |
| 2026-11-18 | CAT-08 | Sản phẩm yêu thích: `PUT` / `DELETE /me/wishlist/{id}` idempotent (`ON CONFLICT`), `/me/wishlist/product-ids`, `/me/wishlist` (giá thấp nhất, SKU mặc định, `is_purchasable`, `product_slug`, nạp theo lô chống N+1), cổng `WishlistShopAvailability`; web sửa 2 endpoint lệch spec + nối API thật — còn chạy thật trên giao diện | nhánh `feature/catalog-wishlist` |
| 2026-11-18 | SOC-06 | Theo dõi gian hàng: `PUT` / `DELETE /me/following/{shop_id}` idempotent, `follower_count` chỉ đổi khi dòng thật sự chèn / xóa (không đếm sai dưới tải đồng thời), `/me/following/shop-ids`, `/me/following` (`$ShopMini`, `summary.total`), cổng `ShopFollowDirectory` (module vendor cài), `GNE-SOC-4095`; web sửa endpoint lệch spec + nối API thật — còn chạy thật trên giao diện | nhánh `feature/social-shop-follow` |
| 2026-11-18 | VND-01…07, 21 | Mở shop & KYC: mở shop OTP email (giữ phiên, nâng VENDOR), cổng KYC `/seller/**`, hồ sơ KYC mã hóa AES-256-GCM (không trả URL ảnh), gửi AI Worker sau commit + callback nội bộ + chấm điểm tín hiệu cấu hình được + giả lập `AI_WORKER_MODE=stub`, duyệt tay 6 endpoint (REVIEW_KYC / UPDATE_COMMISSION), job hết hạn bổ sung, tài khoản nhận tiền OTP bước 2, cài đặt shop; migration `V20`; web nối API thật — còn chạy thật; UC-VND-05 chờ A12 | nhánh `feature/vendor-shop-kyc` |
| 2026-11-18 | VND-03 (AI) | AI Worker KYC: `POST /kyc/verify` 202 + xử lý nền, tải ảnh có danh sách host, QR / MRZ TD1 / luật số CCCD, hạn thẻ, QR↔MRZ, YuNet + SFace, liveness thụ động, callback thử lại; không ghi ảnh xuống đĩa, không log dữ liệu cá nhân; chạy hai phía với backend `AI_WORKER_MODE=http` | nhánh `feature/ai-worker-kyc` |
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
| 2026-11-18 | CAT-02, 04, 05, 06, 07, 16, 18 | Phân công Nghiêm Phú Đăng Quân; lập plan + progress `plan/2026-11-17/server-spring/plan_tim_kiem_va_trang_san_pham.md` (54 task, 12 commit, giai đoạn C00–C08; stack + thuật toán tìm kiếm A1–A13). Chưa đổi ô nào (vẫn ⬜ Chưa triển khai). Đồng bộ cột *Người phụ trách* trong Excel | — |
| 2026-11-18 | CAT-02, 04, 05, 06, 07, 16, 18 | Backend (`/public/products`, `/search/suggestions`, `/search/trending`, `/products/{id}`, `/variants`, `/views`, `/similar`, `/recommendations`, `/public/shops`) + 74 test mới xanh (24 unit + 50 IT; toàn backend 212) + web nối API sau cờ `PUBLIC_REAL_API=product,search,shop`, Playwright 22/22; 7 UC → 🟡 Đang triển khai (CAT-05 BE / Nối `◐`), chưa chạy thật | `feature/web-catalog-real-api` (`a45aedb`…`cafbf78`) |
| 2026-11-19 | ADM-01, 02, 03 | Nhận cụm B1 (Nguyễn Quốc Khoa); plan `plan/2026-11-19/b1.md` chốt D1–D15. Luồng lời mời mới: thông tin ở PostgreSQL, mã link ở Redis `auth:staff_invite:*` 48 giờ; Admin xem được lời mời hết hạn (`INVITATION_EXPIRED`, `invitation_expires_at` suy từ Redis + `system_logs`) và gửi lại. Trạng thái giữ ⬜ tới khi có backend | `b8ca54a`, nhánh `feature/admin-staff-rbac` |
| 2026-11-19 | ADM-01 | B1 R01–R02: API_SPEC 1.6 + mã lỗi `GNE-ADM-*` + mẫu email mời; `auth` tạo tài khoản nhân viên, mã lời mời Redis, `POST /auth/staff-activations` (+ `/otp-challenges`), `StaffActivationIT` — code + test viết xong, chờ build; trạng thái giữ ⬜ | (chưa commit) |
| 2026-11-19 | ADM-01 | B1 R01–R02 build xanh (`./mvnw verify`, `StaffActivationIT` 5/5) → BE ◐, Test ◐, trạng thái 🟡; đồng bộ Excel (Hiện trạng, Người phụ trách ADM-01/02/03) | (chưa commit) |
| 2026-11-19 | ADM-01 | Màn kích hoạt `/auth/staff-activation` (web, nối API thật qua nhóm `auth`) — code + test viết xong, chờ `npm run verify`; Nối giữ ☐ tới khi nối cả `/admin/staff` | (chưa commit) |
| 2026-11-19 | ADM-01, 02 | B1 R03–R04: API thật `/admin/staff/**` (7 endpoint), `/admin/permission-catalog`, `GET`/`PATCH /admin/me`, `StaffAdminIT`; web `/admin/staff` + khung Admin nối API thật (nhóm `rbac`, `auth`), nhãn lời mời hết hạn — code + test viết xong, chờ build | (chưa commit) |
| 2026-11-20 | ADM-01, 02 | Chạy được trên giao diện với API thật: `/admin/staff` lấy danh sách từ CSDL, khung Admin dùng `/admin/me` → ADM-01 Nối ◐, ADM-02 BE ◐ + Nối ◐ (🟡); số liệu mục 1 + phân hệ 11 cập nhật | (chưa commit) |
| 2026-11-20 | ADM-01 | Giao diện chuyển kênh: mục **Trang quản trị** trong menu tài khoản storefront; header Admin Console có menu tài khoản (ảnh + tên + chức danh, Kênh người mua, Đăng xuất) thay thẻ ở đáy thanh bên; sửa menu storefront rộng 280px + biểu tượng xoay chuyển | (chưa commit) |
| 2026-11-20 | ADM-01 | Chốt lại D3: bỏ ràng buộc email `@genie.example` (tên miền `.example` không có hộp thư thật) — mời bằng mọi email hợp lệ, biến `STAFF_EMAIL_DOMAINS` giới hạn ở production; API_SPEC 11.18 / 15, Usecase UC-ADM-01, `docs/cau-hinh-moi-truong.md` | (chưa commit) |
| 2026-11-20 | (quyết định) | D16: nhân viên / Admin vẫn mua hàng được như khách (giữ nguyên, không đổi code) | — |
| 2026-11-20 | ADM-01, ORD-01, ORD-07 | Chốt lại D16 (hướng B): tài khoản nội bộ không mua hàng — luật mới ở UC-ORD-01 / 07 (`403 GNE-ORD-4031`, chặn ở tầng bảo mật theo đường dẫn nên áp dụng sẵn khi module order hiện thực); web ẩn thao tác mua cho STAFF / ADMIN — code + test viết xong, chờ build. Trạng thái ORD-01 / 07 không đổi (chưa có backend giỏ hàng) | (chưa commit) |
| 2026-11-20 | ADM-01 | Form mời cảnh báo tức thời khi email đã thuộc khách hàng / người bán / nhân viên (`GET /admin/staff/email-check`, `GNE-ADM-4091` kèm `taken_by`) — viết xong, chờ build | (chưa commit) |
| 2026-11-20 | ADM-01, AUTH-03 | Nhân viên chờ kích hoạt không đăng nhập Google được (`403 GNE-AUTH-4033`, câu báo chung) — vá lỗ hổng bỏ qua bước kích hoạt; chờ build | (chưa commit) |
| 2026-11-20 | ADM-01, AUTH-03 | Chốt D17: mọi tài khoản nội bộ STAFF / ADMIN chỉ đăng nhập email + mật khẩu — chặn đăng nhập / liên kết Google, Zalo (`403 GNE-AUTH-4033`, câu báo chung); chờ build | (chưa commit) |
| 2026-11-20 | ADM-01, 02, 03 | Chủ dự án build + verify xanh và chạy thật xong → UC-ADM-01, UC-ADM-02 ✅ Đã triển khai (đủ 5 lớp); UC-ADM-03 🟡 (khóa / mở nhân viên xong, còn `/admin/users` + cron). Tổng quan (sau khi gộp `develop` — cụm tìm kiếm & trang sản phẩm): Đã triển khai 4 → 6, Đang triển khai **25**, Chưa (mock) **45** — đếm lại theo từng dòng UC (số ở `develop` lệch 1–2 so với bảng phân hệ); phân hệ 11: 2 / 3 / 3. Đồng bộ Excel | (chờ commit — `feature/b1`) |
| 2026-11-21 | ADM-03 | B1 R05 `/admin/users`: danh sách 4 tab + tìm kiếm, khóa 7 / 30 ngày / vô thời hạn (thu hồi phiên), mở khóa có lý do; nhân viên / Admin không phải đích (404); số đơn / cảnh báo tạm 0 qua cổng (D10); web nối nhóm `users` — code + test viết xong, chờ build; ô giữ ◐ | (chưa commit) |
| 2026-11-21 | ADM-01, 02, 03 | Realtime (D18): WebSocket STOMP `/ws` + kênh `/user/queue/account` — người bị khóa / đổi quyền / bị xóa đăng xuất ngay trên mọi tab, không phải tải lại; hạ tầng WebSocket dùng chung cho thông báo / chat sau này — code + test viết xong, chờ build | (chưa commit) |
| 2026-11-21 | CAT-02, 04, 05, 06, 07, 16, 18 | Chạy thật hai phía (Postgres + Redis + Silo + backend `local` + web `PUBLIC_REAL_API=…,product,search,shop`, seed 200 sản phẩm / 11 shop) bằng API + trình duyệt Chromium (Playwright): tìm có dấu / không dấu / gõ sai / từ lạ; xu hướng + gợi ý dropdown; sắp xếp 4 kiểu, lọc giá / đánh giá / Chính hãng / Freeship / facet; chi tiết SKU hết hàng bị khóa, 301 slug sai, 404; tương tự; trang shop; deeplink `?v=&src=share` chọn sẵn SKU + OG giá; lượt xem ghi xuống `product_view_stats` theo nguồn. **6 UC → ✅ Đã triển khai**; UC-CAT-05 Chạy ◐ (vẫn 🟡). Sửa lỗi ô giá "Từ" tự gửi form khi chuyển sang ô "Đến" (`FilterPanel.tsx`). Ghi nhận lỗi ETag `/public/**` không bao giờ 304 (mục 3). Tổng quan: Đã triển khai 6 → 12, Đang triển khai 25 → 19; phân hệ 02: 6 / 2 / 9 | (chưa commit) |
| 2026-11-21 | ADM-02 | D19: đổi quyền / chức danh nhân viên không còn đăng xuất họ — quyền có hiệu lực ở request kế tiếp, giao diện tải lại menu qua tin `PERMISSIONS_CHANGED` (kênh `/user/queue/account`), mất quyền màn đang mở thì về trang chính kèm thông báo; Usecase UC-ADM-02 bước 4, API_SPEC 11.18 / 13 — viết xong, chờ build | (chưa commit) |
| 2026-11-21 | ADM-02, 03 | Chủ dự án build + chạy thật xong R05 `/admin/users`, realtime D18, D19 → UC-ADM-03 Nối ☑ (BE / Test / Chạy ◐ — còn cron R06) | (chờ commit — `feature/b1`) |
| 2026-11-22 | CAT-01, 14, 15 | Plan B2: chốt D1–D17; `V22` UNIQUE slug, `V23` `platform_settings`; cây công khai có cache, quản lý cây 3 cấp (trùng slug / tên → 4091, xóa còn con / sản phẩm → 4092), hoa hồng mặc định + theo ngành (3,5–8 %, audit `COMMISSION_UPDATED`), bộ thuộc tính có kế thừa (trùng mã trong nhánh → 4095, ân hạn 14 ngày); web nối nhóm `catalog`, `admin-catalog`, `commission`; trang chính sách hoa hồng đổi về khung 3,5–8 % — code + test viết xong, chờ build. Tổng quan: Đang triển khai 19 → 22, mock 45 → 42; phân hệ 02: 6 / 5 / 6 | (chưa commit — `feature/b2`) |
| 2026-11-22 | CAT-01, 14, 15 | Ghi rõ B2 **chưa test**: chủ dự án chưa build / chưa chạy thật — giữ 🟡, cột Test `◐`; thêm danh sách 28 commit vào `b2.md` | (chưa commit — `feature/b2`) |
| 2026-11-24 | CAT-01, 14, 15 | Test B2 (Nguyễn Ngọc Thiện kiểm thay): `./mvnw verify` xanh — 177 unit + 292 IT, 4 IT của B2 + `CategoryIndexTest` đều đạt (máy chỉ có JDK 24 → chạy kèm `-Dnet.bytebuddy.experimental=true`, không thì Mockito lỗi ở `CoinLedgerServiceTest` / `BannerCacheTest`); `npm run verify` xanh (346 test); backend thật chạy đủ 26 bước `catalog-category.http` (4091 / 4092 / 4095 / 422 / 403 đúng, ẩn cha → cả nhánh mất khỏi cây công khai, ân hạn 14 ngày), `system_logs` có `COMMISSION_UPDATED`; SSR `/c/dien-tu` 200 + BreadcrumbList, slug lạ 404. Cột Test `◐` → `☑`; Chạy giữ `☐` (chưa thao tác giao diện Admin), trạng thái vẫn 🟡 — tổng quan không đổi | PR #92 (đã gộp `develop`) |
| 2026-11-24 | CAT-02, 04, 05, 06, 07, 16, 18 | Kiểm lại B3 (Nguyễn Ngọc Thiện kiểm thay): `./mvnw verify` xanh — 7 lớp unit + 13 lớp IT của cụm đều đạt; `SearchPerformanceIT` (`-Dperf=true`, JDK 21) p50 27 ms / p95 51 ms; `npm run verify` xanh. Chạy `catalog-search.http` + SSR trên backend thật: 40 / 41 kiểm đúng (tìm có dấu / không dấu / tiền tố / gõ sai / từ lạ, lọc giá / đánh giá / Chính hãng / sắp xếp, 422, gợi ý + xu hướng, chi tiết + SKU + 404, lượt xem theo nguồn, tương tự, gợi ý hôm nay ổn định, trang shop + 404, deeplink OG, JSON-LD, slug sai 301). Lọc danh mục trên web nay khớp CSDL thật (cây UC-CAT-01) → gạch việc chờ ở mục 3. Lỗi ETag `/public/**` không bao giờ `304` đã sửa cùng ngày (dòng dưới). Trạng thái không đổi: 6 UC ✅, UC-CAT-05 🟡 chỉ còn chờ lọc "Shop có voucher" | — |
| 2026-11-24 | CAT-01, 02, 06, 16 | Sửa ETag `/public/**`: `common/config/ContentEtagHeaderFilter` (thay `ShallowEtagHeaderFilter` trong `PublicHttpCacheConfig`) băm thân bỏ `meta` → `If-None-Match` trùng nhận `304`, nội dung đổi thì ETag đổi; thêm IT hồi quy. `./mvnw clean verify` xanh (JDK 21: 177 unit + 293 IT); chạy thật `products/{id}`, `products?q=`, `categories`, `shops/{id}`, `regions/provinces` đều `304`. Trạng thái UC không đổi | (chưa commit) |
| 2026-11-22 | ADM-07, ADM-12 | Nhật ký banner theo động từ cụ thể (`BANNER_CREATED / UPDATED / ACTIVATED / DEACTIVATED / DELETED`, migration `V24`); API nhật ký trả thêm `reference_id`, `user_agent`, `changes`; màn Nhật ký dịch tên hành động + hộp chi tiết trước → sau (plan `2026-11-22/server-spring/plan_nhat_ky_banner_chi_tiet.md`). Trạng thái UC không đổi — vẫn chờ chạy thật | nhánh `feature/audit-banner-granular-actions` |
| 2026-11-24 | ADM-03, CAT-01, 05, 14, 15 | Plan rà soát 2026-11-24 (Nguyễn Ngọc Thiện làm bù, phân công không đổi): cron `SuspensionExpiryJob` + email mở lại tài khoản (B1); test bổ sung `CatalogSecurityIT`, `IN_USE`, Redis lỗi, ETag, lưu hoa hồng hàng loạt + sửa giao diện Admin quản lý ngành cấp 2 / 3 và khóa kiểu dữ liệu thuộc tính (B2); module promotion cài cổng "Shop có voucher", bộ lọc thuộc tính từ facets, seed 3 voucher shop (B3). Chạy thật 60 / 60 TC bằng Chromium tự động (Playwright) trên hệ thống thật theo yêu cầu người dùng. **5 UC → ✅ Đã triển khai.** Tổng quan: Đã triển khai 12 → 17, Đang triển khai 22 → 17; phân hệ 02: 10 / 1 / 6, phân hệ 11: 3 / 2 / 3 | nhánh `bugfix/public-etag-304` → `feature/b1-suspension-expiry-job` → `feature/b2-catalog-acceptance` → `feature/b3-shop-voucher-filter` |
| 2026-11-25 | VND-01, 02, 03, 04, 05, 07, 21 | Rà tiến độ cụm B4 trên `develop` (`0e34dce`): code 6 / 7 UC đã merge — PR #83 (`feature/vendor-shop-kyc`), #84 (`feature/ai-worker-kyc`), #96 (sửa KYC ảnh điện thoại thật: QR zxing-cpp, MRZ, dò mặt / độ nét; `/seller/kyc` tự làm mới; email shop = email tài khoản). Có đủ endpoint `/me/shop-registrations`, `/seller/kyc`, `/admin/kyc-submissions`, `/internal/kyc`, `/seller/payout-account`, `/seller/settings` + 12 lớp IT vendor + `pytest` AI Worker. Cột *Chạy* vẫn ☐ — chưa ai thao tác thật trên giao diện (K11.08, F06.01 chưa xong). UC-VND-05: A12 đã có nhưng K07 chưa làm → vẫn ⬜. Trạng thái UC và số Tổng quan không đổi | nhánh `docs/update-b4-progress` |
