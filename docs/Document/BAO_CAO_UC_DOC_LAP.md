# BÁO CÁO PHÂN NHÓM USE CASE THEO MỨC ĐỘ ĐỘC LẬP — GENIE (BẢN TLCN)

> **Ngày lập:** 2026-11-16 (phân công cập nhật 2026-11-26, tiến độ B1 / B2 / B3 cập nhật 2026-11-24, B4 cập nhật 2026-11-25) · **Phạm vi:** 87 use case của [Usecase.md](diagrams/UseCaseDiagram/Usecase.md) (TLCN v1.4)
> **Mục đích:** chỉ ra use case nào làm riêng được (một plan riêng, chạy song song) mà không chờ hay làm ảnh hưởng use case khác, use case nào phải làm theo cụm hoặc theo chuỗi — làm đầu vào để lập plan trong `plan/<ngày>/<thể loại>/`.
> **Nguồn đã quét:** quan hệ «include» / «extend», *Tiền điều kiện* và *Thực thể CSDL* của từng use case trong Usecase.md; bảng ánh xạ use case ↔ API ở [API_SPEC.md](document/API_SPEC.md); hiện trạng code `genie-multivendor-platform` (AUTH đã hiện thực; 65 bảng + `V1…V14` có sẵn; `common` đã có Silo, email, nhật ký kiểm toán, phân quyền). Tiến độ từng use case: [CHECKLIST_TIEN_DO_UC.md](CHECKLIST_TIEN_DO_UC.md).
> **Cách ghi:** mỗi use case ghi **tên đầy đủ** theo Usecase.md, mã đặt trong ngoặc để tra cứu.

---

## Thành viên & phân công

> Cập nhật 2026-11-26. Cột **Phụ trách** ở bảng mục 3, 4 và dòng *Phụ trách* của từng cụm / chuỗi ghi người nhận làm; `—` = chưa phân công. Đổi / nhận thêm use case thì sửa cột đó, bảng này, cột *Người phụ trách* trong `Quản Lý UseCase.xlsx` và ghi một dòng Nhật ký.

| Thành viên | Phụ trách | Hiện trạng |
|---|---|---|
| **Nguyễn Quốc Khoa** | Toàn bộ phân hệ 01 — Xác thực & Tài khoản (9 use case `UC-AUTH-*`) · cụm **B1 Nhân viên & phân quyền** (UC-ADM-01, 02, 03) · cụm **B2 Danh mục sàn** (UC-CAT-01, 14, 15) | AUTH: đang triển khai — xem mục 3 (UC-AUTH-09 xong 2026-11-18; còn phần còn lại của UC-AUTH-03, 08), plan `plan/2026-11-14/auth.md`. B1: **3 / 3 ✅** (UC-ADM-03 xong 2026-11-24). B2: **3 / 3 ✅** (2026-11-24). Phần còn thiếu của B1 / B2 do Nguyễn Ngọc Thiện rà soát làm bù — plan `plan/2026-11-24/server-spring/plan_hoan_thien_b1_b2_b3.md` |
| **Nguyễn Ngọc Thiện** | 21 use case: Tra cứu nhật ký kiểm toán hệ thống bất biến (UC-ADM-07) · Quản lý banner trang chủ & chiến dịch tĩnh (UC-ADM-12) · Điểm danh nhận Điểm hàng ngày & Duy trì chuỗi điểm danh (UC-PRO-08) + Tra cứu số dư Điểm Genie & Lịch sử biến động Điểm (UC-PRO-09) · Quản lý danh sách sản phẩm yêu thích (UC-CAT-08) · Khách hàng theo dõi gian hàng (UC-SOC-06) · cụm B4 Mở shop & KYC: UC-VND-01, 02, 03, 04, 05, 07, 21 · **nhận 2026-11-26:** Admin phát thông báo toàn sàn (UC-NOT-05) · cụm **B5 Voucher** (UC-PRO-01, 02, 03) · cụm **B6 Chat & realtime** (UC-SOC-01, 02, UC-VND-06, UC-NOT-01) | Đang triển khai — seed dữ liệu mẫu xong (nhánh `feature/db-seed-demo`); UC-ADM-07 chờ chạy thật (nhánh `feature/admin-audit-log`); UC-ADM-12 chờ chạy thật (nhánh `feature/admin-home-banner`); UC-PRO-08, PRO-09 chờ chạy thật (nhánh `feature/promotion-coin-checkin`); UC-CAT-08 chờ chạy thật (nhánh `feature/catalog-wishlist`); UC-SOC-06 chờ chạy thật (nhánh `feature/social-shop-follow`); B4: **7 / 7 ✅** (2026-11-25 — UC-VND-05 làm xong ở nhánh `feature/vendor-k07-pickup`; 6 UC còn lại chạy thật bằng Playwright + AI Worker thật, sửa lỗi ở nhánh `bugfix/b4-real-run-fixes`) — xem cụm B4 mục 5. UC-NOT-05, B5, B6: **đã lập plan 2026-11-26** (`plan/2026-11-26/server-spring/` — 3 plan + progress), chưa bắt đầu code |
| **Nghiêm Phú Đăng Quân** | Cụm **B3 Storefront đọc công khai** (7 use case): Tìm kiếm sản phẩm theo từ khóa (UC-CAT-02) · Gợi ý từ khóa tìm kiếm & Xu hướng (UC-CAT-04) · Lọc nâng cao & Sắp xếp sản phẩm đa tiêu chí (UC-CAT-05) · Xem chi tiết sản phẩm, ma trận biến thể SKU & Tồn kho (UC-CAT-06) · Xem gợi ý sản phẩm tương tự (UC-CAT-07) · Xem trang gian hàng (UC-CAT-16) · Chia sẻ sản phẩm bằng liên kết — Deeplink (UC-CAT-18) | **7 / 7 ✅ Đã triển khai** (6 UC chạy thật 2026-11-21; *Lọc nâng cao & Sắp xếp* (UC-CAT-05) xong 2026-11-24 — lọc "Shop có voucher" + bộ lọc thuộc tính, làm bù ở plan rà soát 2026-11-24); plan `plan/2026-11-17/server-spring/plan_tim_kiem_va_trang_san_pham.md` |

Cụm / chuỗi ở mục 5, 6 không ghi *Phụ trách* = chưa phân công.

## 1. Tiêu chí "độc lập"

Một use case được xếp **độc lập** khi thỏa cả 3 điều kiện:

1. **Không «include» / «extend» use case nào chưa làm** (theo mục *Quan hệ Use Case* của Usecase.md).
2. **Tiền điều kiện đáp ứng được bằng dữ liệu seed** (`R__seed_demo.sql`) — không phải đợi use case khác chạy trước để sinh dữ liệu.
3. **Chỉ ghi vào bảng của chính nó**; làm xong không buộc use case khác phải sửa. Use case khác có thể *đọc* kết quả của nó sau này.

| Nhóm | Ý nghĩa | Cách lập plan |
|---|---|---|
| 🟢 **A** | Độc lập hoàn toàn | Mỗi use case một plan, làm song song được |
| 🟡 **B** | Độc lập theo cụm — các use case trong cụm dính nhau, nhưng cụm không đụng cụm khác | Mỗi cụm một plan |
| 🔴 **C** | Chuỗi đơn hàng → tiền, phụ thuộc lẫn nhau | Làm tuần tự theo chuỗi |

## 2. Tổng kết

| Nhóm | Số use case |
|---|---:|
| ✅ Đã / gần xong (phân hệ Xác thực & Tài khoản) | 6 |
| 🟢 A — Độc lập hoàn toàn | 10 |
| 🟡 B — Độc lập theo cụm (6 cụm) | 27 |
| 🔴 C — Chuỗi đơn hàng → tiền (11 chuỗi) | 44 |
| **Tổng** | **87** |

## 3. ✅ Use case đã / gần xong (không cần plan mới)

| Use case | Trạng thái | Còn lại | Phụ trách |
|---|---|---|---|
| Đăng ký tài khoản Khách hàng (UC-AUTH-01) | ✅ Đã triển khai | — | Nguyễn Quốc Khoa |
| Đăng nhập hệ thống (UC-AUTH-02) | ✅ Đã triển khai | — | Nguyễn Quốc Khoa |
| Khôi phục mật khẩu qua Email OTP (UC-AUTH-04) | ✅ Đã triển khai | — | Nguyễn Quốc Khoa |
| Đổi / Thiết lập mật khẩu (UC-AUTH-06) | 🟡 Code + test xong | Chạy thử thật trên giao diện | Nguyễn Quốc Khoa |
| Đăng xuất & Quản lý thiết bị đăng nhập (UC-AUTH-11) | 🟡 Code + test xong | Chạy thử thật | Nguyễn Quốc Khoa |
| Yêu cầu ẩn danh hóa & Xóa dữ liệu cá nhân — Xóa tài khoản (UC-AUTH-10) | 🟡 Code + test xong | Chạy thử thật; khi làm nhóm C, module đơn hàng / ví cài cổng `AccountDeletionBlocker` | Nguyễn Quốc Khoa |

## 4. 🟢 Nhóm A — Độc lập hoàn toàn (mỗi use case một plan)

| Use case | Vì sao độc lập | Cần chuẩn bị | Phụ trách |
|---|---|---|---|
| **Quản lý sổ địa chỉ giao hàng chuẩn GHN** (UC-AUTH-09) | Chỉ ghi `addresses`, `customer_profiles.default_address_id`; không quan hệ use case nào. *Tính phí vận chuyển qua GHN API* sau này chỉ *đọc* địa chỉ | `GHN_TOKEN` hoặc ảnh chụp JSON danh mục hành chính (plan AUTH A12) | Nguyễn Quốc Khoa |
| **Quản lý thông tin hồ sơ cá nhân** — phần còn lại: `GET /me` + đổi email (UC-AUTH-08) | Bộ đếm / shop lấy qua cổng đảo ngược, mặc định `0` / `null` khi module khác chưa có | Không | Nguyễn Quốc Khoa |
| **Đăng nhập & Liên kết tài khoản Google / Apple** — phần Apple (UC-AUTH-03) | Chỉ thêm một nhà cung cấp, đã có cờ `genie.oauth.apple.enabled` | Tài khoản Apple Developer (trả phí) — **xem phương án thay thế miễn phí ở mục 4.1** | Nguyễn Quốc Khoa |
| **Tra cứu nhật ký kiểm toán hệ thống bất biến** (UC-ADM-07) | Chỉ *đọc* `system_logs` — bảng phân hệ AUTH đang ghi thật | Không | Nguyễn Ngọc Thiện |
| **Quản lý banner trang chủ & chiến dịch tĩnh** (UC-ADM-12) | Bảng `home_banners` riêng; storefront chỉ đọc `GET /public/banners` | Thêm purpose `BANNER_IMAGE` ở `UploadPurpose` | Nguyễn Ngọc Thiện |
| **Admin phát thông báo toàn sàn** (UC-NOT-05) | Bảng `platform_notifications`, `user_platform_notification_reads` riêng | Đẩy realtime cần *Thông báo trong ứng dụng thời gian thực* (cụm B6); không có thì vẫn xem được qua danh sách thông báo — gộp chung plan với UC-NOT-01: `plan/2026-11-26/server-spring/plan_thong_bao_realtime.md` | Nguyễn Ngọc Thiện |
| **Điểm danh nhận Điểm hàng ngày & Duy trì chuỗi điểm danh** (UC-PRO-08) + **Tra cứu số dư Điểm Genie & Lịch sử biến động Điểm** (UC-PRO-09) | Chỉ cần user; ghi `daily_checkins`, `user_coins`, `coin_transactions` | Không — nguồn Điểm từ đơn / đánh giá cộng thêm sau | Nguyễn Ngọc Thiện |
| **Quản lý danh sách sản phẩm yêu thích** (UC-CAT-08) | Bảng `user_wishlists` riêng | Seed vài sản phẩm | Nguyễn Ngọc Thiện |
| **Khách hàng theo dõi gian hàng** (UC-SOC-06) | Bảng `shop_follows` riêng | Seed shop đã KYC; thông báo "shop có hàng mới" để sau | Nguyễn Ngọc Thiện |

> *Điểm danh nhận Điểm* và *Tra cứu số dư Điểm* dùng chung bảng `user_coins` nên đếm là 2 use case nhưng nên gộp một plan.

### 4.1 Ghi chú: đổi Apple sang nhà cung cấp miễn phí (Facebook / Zalo)

"Đăng nhập bằng Apple" bắt buộc có **Apple Developer Program (99 USD / năm)** và tên miền HTTPS thật. Hiện giao diện đã có nút "Tiếp tục với Apple" (`web/src/features/auth/ui/SocialButtons.tsx`) nhưng backend chỉ có Google (`OAuthProvider`) → khi chạy API thật bấm nút Apple sẽ lỗi. Có thể **đổi Apple thành Facebook hoặc Zalo** (đều miễn phí):

| Phương án | Chi phí | Công làm | Lưu ý khi đổi |
|---|---|---|---|
| **Giữ Google, bỏ Apple** (để DATN) | 0đ | Rất ít — ẩn nút Apple, sửa tài liệu | Nhanh nhất; "Đăng nhập & Liên kết tài khoản" chuyển sang Đã triển khai ngay |
| **Facebook Login** | 0đ | Trung bình — thêm một nhà cung cấp OAuth2 | Phổ biến ở Việt Nam. Chế độ phát triển chỉ cho tài khoản thử nghiệm (đủ để demo); mở cho mọi người phải qua duyệt của Meta. Không phải OpenID Connect → không có cờ `email_verified`; có tài khoản không trả email (đăng ký bằng số điện thoại) → phải viết lại luật tự liên kết theo email |
| **Zalo Login** | 0đ | Nhiều | Rất "Việt Nam" nhưng **không trả email** → không tự liên kết được với tài khoản email, chỉ tạo tài khoản mới hoặc liên kết thủ công; lệch nhiều với mô hình hiện tại (email là định danh chính) |

**Khi đổi cần sửa:** đặc tả *Đăng nhập & Liên kết tài khoản* trong Usecase.md, tác nhân `ACT-IDP` (sơ đồ PlantUML, XMI), API_SPEC §2.9–2.10 và §3.3 (giá trị `provider`), Excel, hợp đồng Swagger `genie-api-spec`, nút ở `SocialButtons.tsx`, enum `OAuthProvider`. Cơ sở dữ liệu **không đổi**: cột `users.oauth_provider` là chuỗi (`"google"`, `"facebook"`, `"zalo"`) và mỗi tài khoản chỉ gắn một nhà cung cấp.

## 5. 🟡 Nhóm B — Độc lập theo cụm (mỗi cụm một plan)

### B1. Nhân viên & phân quyền (3 use case)
- Quản lý danh sách nhân viên nội bộ & Lời mời kích hoạt (UC-ADM-01)
- Phân cấp vai trò & Cấp phát / Thu hồi mã quyền chi tiết — RBAC (UC-ADM-02)
- Khóa / Mở khóa tài khoản nhân viên và người dùng vi phạm (UC-ADM-03)

*Phụ trách:* **Nguyễn Quốc Khoa** — plan `plan/2026-11-19/b1.md` (R00–R10), nhánh `feature/b1`.

| Use case | Trạng thái | Còn lại |
|---|---|---|
| Quản lý danh sách nhân viên nội bộ & Lời mời kích hoạt (UC-ADM-01) | ✅ Đã triển khai | — |
| Phân cấp vai trò & Cấp phát / Thu hồi mã quyền chi tiết — RBAC (UC-ADM-02) | ✅ Đã triển khai | — |
| Khóa / Mở khóa tài khoản nhân viên và người dùng vi phạm (UC-ADM-03) | ✅ Đã triển khai | — (cron `SuspensionExpiryJob` + email mở lại tài khoản xong 2026-11-24) |

*Ghi chú:* *Quản lý danh sách nhân viên* «include» *Phân cấp vai trò & cấp quyền* nên phải làm chung. *Khóa / Mở khóa tài khoản* dùng `UserFacade.suspend` đã có. Nối phân hệ AUTH qua `POST /auth/staff-activations`; đổi quyền gọi `StaffPermissionAuthorityProvider.evict`.

### B2. Danh mục sàn (3 use case)
- Quản lý danh mục sàn toàn cục & Cấu hình tỷ lệ hoa hồng (UC-CAT-14)
- Cấu hình bộ thuộc tính chuẩn theo ngành hàng (UC-CAT-15)
- Duyệt cây danh mục đa cấp (UC-CAT-01)

*Phụ trách:* **Nguyễn Quốc Khoa** — plan `plan/2026-11-19/b2.md` (C00–C09), nhánh `feature/b2` (PR #92 vào `develop`). Kiểm 2026-11-24: `./mvnw verify` + `npm run verify` xanh, 26 bước `docs/api-tests/catalog-category.http` đúng trên backend thật.

| Use case | Trạng thái | Còn lại |
|---|---|---|
| Duyệt cây danh mục đa cấp (UC-CAT-01) | ✅ Đã triển khai | — |
| Quản lý danh mục sàn toàn cục & Cấu hình tỷ lệ hoa hồng (UC-CAT-14) | ✅ Đã triển khai | Duyệt đề xuất danh mục thuộc UC-CAT-17 (còn mock) |
| Cấu hình bộ thuộc tính chuẩn theo ngành hàng (UC-CAT-15) | ✅ Đã triển khai | `CategoryFacade.attributesFor` + kiểm thuộc tính bắt buộc làm cùng *Thêm mới sản phẩm* (UC-CAT-09) |

*Ghi chú:* *Cấu hình bộ thuộc tính* cần ngành hàng tạo ở *Quản lý danh mục sàn*; *Duyệt cây danh mục* chỉ đọc. Là **gốc** của mọi cụm sản phẩm — nên làm sớm.

### B3. Storefront đọc công khai (7 use case)

*Phụ trách:* **Nghiêm Phú Đăng Quân** — plan `plan/2026-11-17/server-spring/plan_tim_kiem_va_trang_san_pham.md` (+ progress, 66 / 66 task), nhánh `feature/web-catalog-real-api` (PR #86 vào `develop`). Chạy thật hai phía 2026-11-21 (web `PUBLIC_REAL_API=product,search,shop` + backend `local`, seed 200 sản phẩm / 11 shop). Kiểm lại 2026-11-24: test của cụm xanh, `catalog-search.http` + SSR đúng; sửa ETag `/public/**` để nhận `304`.

| Use case | Trạng thái | Còn lại |
|---|---|---|
| Tìm kiếm sản phẩm theo từ khóa (UC-CAT-02) | ✅ Đã triển khai | — |
| Gợi ý từ khóa tìm kiếm & Xu hướng (UC-CAT-04) | ✅ Đã triển khai | — |
| Lọc nâng cao & Sắp xếp sản phẩm đa tiêu chí (UC-CAT-05) | ✅ Đã triển khai | — (lọc "Shop có voucher" do module promotion cài 2026-11-24; cụm B5 tạo / sửa voucher shop phải xóa cache `promotion:shop_voucher_vendors`) |
| Xem chi tiết sản phẩm, ma trận biến thể SKU & Tồn kho (UC-CAT-06) | ✅ Đã triển khai | — |
| Xem gợi ý sản phẩm tương tự (UC-CAT-07) | ✅ Đã triển khai | — |
| Xem trang gian hàng (UC-CAT-16) | ✅ Đã triển khai | — |
| Chia sẻ sản phẩm bằng liên kết — Deeplink (UC-CAT-18) | ✅ Đã triển khai | Thử dán link vào Zalo / Facebook khi có tên miền công khai (thẻ OG đã kiểm trên HTML) |

*Ghi chú:* chỉ đọc `products` + ghi bộ đếm riêng (lượt xem, từ khóa). Chạy trên **seed sản phẩm**, không chờ người bán đăng bán. *Xem chi tiết sản phẩm* «include» *Xem gợi ý sản phẩm tương tự*; *Gợi ý từ khóa* «extend» *Tìm kiếm sản phẩm*; *Xem trang gian hàng* và *Chia sẻ sản phẩm* «extend» *Xem chi tiết sản phẩm*. Cần cụm B2 có cây danh mục (hoặc seed).

### B4. Mở shop & KYC (7 use case)

*Phụ trách:* **Nguyễn Ngọc Thiện** — plan `plan/2026-11-16/server-spring/plan_mo_shop_kyc.md` (backend + nối web, K00–K12; K07 làm lại 2026-11-25 theo D16; K11.08 chạy thật xong 2026-11-25) và `plan/2026-11-16/server-python/plan_kyc_ai_worker.md` (AI Worker) + plan sửa `plan/2026-11-22/server-python/plan_sua_kyc_anh_that.md`. Đã merge vào `develop`: PR #83 (`feature/vendor-shop-kyc`), #84 (`feature/ai-worker-kyc`), #96 (sửa KYC với ảnh điện thoại thật). Rà 2026-11-25: đủ endpoint mở shop / KYC / duyệt tay / callback AI / tài khoản nhận tiền / cài đặt shop + 12 lớp IT vendor + `pytest` AI Worker; kịch bản kiểm thử `docs/kiem-thu/kich-ban-2026-11-16-mo-shop-kyc.md`, việc chưa làm `docs/kiem-thu/kyc-viec-chua-lam.md`. **Chạy thật 2026-11-25:** Chromium (Playwright) trên hệ thống thật + AI Worker thật với ảnh CCCD thật của người phụ trách — 80 ca đạt; 4 lỗi phát hiện đã sửa (tín hiệu rủi ro trên màn duyệt, menu Seller Center không tự mở khóa, hộp thoại xóa tài khoản nhận tiền treo, gợi ý "mã thử" khi chạy thật).

| Use case | Trạng thái | Còn lại |
|---|---|---|
| Đăng ký mở gian hàng — Become a Seller (UC-VND-01) | ✅ Đã triển khai (2026-11-25) | — |
| Tải hồ sơ pháp lý & Giấy tờ định danh KYC (UC-VND-02) | ✅ Đã triển khai (2026-11-25) | — |
| Thẩm định KYC tự động qua AI — QR, MRZ & Face Match (UC-VND-03) | ✅ Đã triển khai (2026-11-25) | — (chạy thật qua web với AI Worker thật + ảnh CCCD thật); `FACE_DUPLICATE`, quốc huy / vân tay để DATN |
| Kiểm duyệt thủ công hồ sơ KYC (UC-VND-04) | ✅ Đã triển khai (2026-11-25) | Báo nhân viên khi có hồ sơ mới chờ module `notification` |
| Cấu hình kho lấy hàng & Đồng bộ GHN Shop ID (UC-VND-05) | ✅ Đã triển khai (2026-11-25) | — (địa giới 2 cấp; GHN thật `GHN_PICKUP_MODE=http` chưa kiểm vì `shop/register` của GHN còn đòi mã quận cũ — mặc định dùng giả lập) |
| Đăng ký tài khoản nhận giải ngân (UC-VND-07) | ✅ Đã triển khai (2026-11-25) | Tra tên chủ tài khoản đang giả lập (`MockBankAccountLookup`); chặn đổi khi còn lệnh rút chờ module `payment` |
| Cập nhật thông tin gian hàng & cài đặt vận hành (UC-VND-21) | ✅ Đã triển khai (2026-11-25) | — |

*Ghi chú:* *Tải hồ sơ KYC* «include» *Thẩm định KYC tự động qua AI*; *Kiểm duyệt thủ công hồ sơ KYC* «extend» *Thẩm định KYC tự động*. Thẩm định AI cần AI Worker (`server-python` mới có khung) — có thể làm *Kiểm duyệt thủ công* trước, AI sau. *Cấu hình kho lấy hàng*, *Đăng ký tài khoản nhận giải ngân*, *Cập nhật thông tin gian hàng* chỉ cần shop `VERIFIED`.

### B5. Voucher — quản lý, chưa áp vào giá (3 use case)
- Tạo & Quản lý Voucher Toàn sàn (UC-PRO-02)
- Khách hàng thu thập Voucher vào Kho Voucher cá nhân (UC-PRO-03)
- Tạo & Quản lý Voucher Gian hàng (UC-PRO-01)

*Phụ trách:* **Nguyễn Ngọc Thiện** (nhận 2026-11-26) — plan `plan/2026-11-26/server-spring/plan_voucher_b5.md` (+ progress, V00–V10), nhánh `feature/voucher-management` → `feature/voucher-wallet`. Chưa bắt đầu code.

*Ghi chú:* chỉ tạo / sửa / lưu voucher. *Voucher Gian hàng* cần shop `VERIFIED` (seed được). Việc **áp** voucher khi mua (*Thuật toán Stacking Voucher 3 Tầng & Điểm*) thuộc nhóm C.

### B6. Chat & realtime (4 use case)
- Nhắn tin trò chuyện thời gian thực Khách – Shop (UC-SOC-01)
- Gửi thẻ sản phẩm, hình ảnh và trạng thái đơn hàng trong chat (UC-SOC-02)
- Cấu hình tin nhắn trả lời tự động (UC-VND-06)
- Thông báo trong ứng dụng thời gian thực — WebSocket (UC-NOT-01)

*Phụ trách:* **Nguyễn Ngọc Thiện** (nhận 2026-11-26) — 2 plan: `plan/2026-11-26/server-spring/plan_thong_bao_realtime.md` (UC-NOT-01 + UC-NOT-05, N00–N10) và `plan_chat_khach_shop.md` (UC-SOC-01, 02, UC-VND-06, M00–M10); migration chung `V27` (tạm) cho cả B5 / B6. Chưa bắt đầu code. Lưu ý: phần *cấu hình* tự trả lời (UC-VND-06 bước 1–3) đã có từ B4 (`PATCH /seller/settings` khối `auto_reply`) — plan chat làm phần gửi tự động.

*Ghi chú:* cùng hạ tầng WebSocket / STOMP. *Gửi thẻ sản phẩm, hình ảnh trong chat* «extend» *Nhắn tin trò chuyện*; *Tin nhắn trả lời tự động* cần chat. *Thông báo thời gian thực* là nền mà nhiều use case khác đẩy thông báo vào → làm cụm này *sớm* thì các nhóm khác đỡ chờ.

## 6. 🔴 Nhóm C — Chuỗi đơn hàng → tiền (làm tuần tự)

### C1. Đăng bán (6 use case)
- Thêm mới sản phẩm & Biến thể SKU — Người bán (UC-CAT-09)
- Cập nhật thông tin sản phẩm, giá bán & hình ảnh (UC-CAT-10)
- Ẩn / Khóa / Xóa mềm sản phẩm (UC-CAT-11)
- Kiểm duyệt sản phẩm tự động qua AI — Google Gemini API (UC-CAT-12)
- Kiểm duyệt sản phẩm thủ công theo hàng đợi — Staff Moderator (UC-CAT-13)
- Người bán đề xuất danh mục mới & Duyệt đề xuất (UC-CAT-17)

*Phụ thuộc:* cần cụm B2 (danh mục) + B4 (shop `VERIFIED`). *Thêm mới* và *Cập nhật sản phẩm* «include» *Kiểm duyệt tự động qua AI* (Gemini); *Kiểm duyệt thủ công* «extend» *Kiểm duyệt tự động*; *Đề xuất danh mục mới* «extend» *Thêm mới sản phẩm*.

### C2. Giỏ → Checkout (5 use case)
- Quản lý giỏ hàng mua sắm cá nhân (UC-ORD-01)
- Xem trước đơn hàng & Tách giỏ hàng đa nhà bán (UC-ORD-04)
- Tính phí vận chuyển qua GHN API (UC-ORD-06)
- Tạo phiên Checkout & Các Đơn hàng con theo Shop (UC-ORD-07)
- Tự động hủy đơn quá hạn thanh toán & Hoàn kho — Sweeper Cron (UC-ORD-08)

*Phụ thuộc:* *Tính phí vận chuyển* cần *Quản lý sổ địa chỉ* (nhóm A) + *Cấu hình kho lấy hàng* (cụm B4); *Tạo phiên Checkout* «include» *Xem trước đơn hàng* «include» *Tính phí vận chuyển*.

*Tiến độ (2026-11-26, plan `plan/2026-11-25/server-spring/plan_gio_hang_checkout_c2.md`):* 5 / 5 UC **đang triển khai** — backend + test xong cả 5, web nối API thật (cờ `cart`, `checkout`) cho UC-ORD-01 / 04 / 06 / 07; còn chạy thật trên giao diện (kịch bản `docs/kiem-thu/kich-ban-2026-11-26-c2.md`). Chờ chuỗi sau: voucher / Điểm (C6 — cổng `CheckoutDiscountPolicy`, `CheckoutCompensation`), cổng thanh toán thật (C3 — cổng `PaymentInitiator`), spike GHN địa giới 2 cấp (cần `GHN_TOKEN`). Phụ trách: chưa phân công.

### C3. Thanh toán & ký quỹ (5 use case)
- Khởi tạo thanh toán trực tuyến — VNPay / chuyển khoản VietQR qua SePay (UC-PAY-01)
- Xử lý IPN VNPay & webhook SePay idempotent (UC-PAY-03)
- Ghi nhận ký quỹ (Escrow) & Khấu trừ hoa hồng (UC-PAY-05)
- Ghi sổ cái tài chính kép bất biến (UC-PAY-06)
- Tự động giải phóng tiền ký quỹ về ví người bán (UC-PAY-07)

*Phụ thuộc:* cần *Tạo phiên Checkout*; *Xử lý IPN & webhook SePay* «include» *Ghi nhận ký quỹ* và *Ghi sổ cái*; cần VNPay sandbox + webhook SePay.

### C4. Vận chuyển (5 use case)
- Người bán xác nhận / từ chối đơn hàng mới (UC-LOG-16)
- Tiếp nhận đơn hàng & Đóng gói tại kho shop (UC-LOG-01)
- Đẩy vận đơn sang GHN & In phiếu gửi hàng PDF (UC-LOG-02)
- Điều phối đa hãng vận chuyển (UC-LOG-15)
- Đồng bộ hành trình giao nhận qua Webhook GHN (UC-LOG-03)

*Phụ thuộc:* cần đơn đã thanh toán; *Đẩy vận đơn sang GHN* «include» *Điều phối đa hãng vận chuyển*; webhook GHN.

### C5. Sau giao hàng (4 use case)
- Khách hàng theo dõi chi tiết hành trình đơn hàng (UC-ORD-09)
- Khách hàng yêu cầu hủy đơn hàng (UC-ORD-10)
- Khách hàng xác nhận "Đã nhận hàng thành công" (UC-ORD-11)
- Hệ thống tự động hoàn tất đơn sau 7 ngày từ khi Delivered (UC-ORD-12)

*Phụ thuộc:* cần đơn + vận đơn + ký quỹ; *Tự động hoàn tất đơn* «include» *Tự động giải phóng tiền ký quỹ*.

### C6. Khuyến mãi khi mua (4 use case)
- Thuật toán Stacking Voucher 3 Tầng & Điểm tự động khi Checkout (UC-PRO-04)
- Sử dụng Điểm Genie để giảm trừ thanh toán đơn hàng (UC-PRO-10)
- Tạo chiến dịch Flash Sale & Cấu hình khung giờ vàng (UC-PRO-05)
- Mua hàng Flash Sale chống bán vượt suất (UC-PRO-06)

*Phụ thuộc:* *Stacking Voucher* «extend» *Xem trước đơn hàng*; *Sử dụng Điểm Genie* «extend» *Stacking Voucher*; *Mua hàng Flash Sale* «extend» *Tạo phiên Checkout*; cần cụm B5 có voucher.

### C7. Đánh giá (4 use case)
- Đánh giá sản phẩm đã mua & Tải ảnh thực tế (UC-SOC-03)
- Người bán phản hồi đánh giá của khách hàng (UC-SOC-04)
- Báo cáo đánh giá vi phạm / spam & Kiểm duyệt đánh giá (UC-SOC-05)
- Phân tích cảm xúc đánh giá bằng PhoBERT (UC-AI-07)

*Phụ thuộc:* *Đánh giá sản phẩm đã mua* yêu cầu đơn `COMPLETED`; *Phân tích cảm xúc* cần PhoBERT trong AI Worker.

### C8. Trả hàng & hoàn tiền (4 use case)
- Tạo yêu cầu Trả hàng / Hoàn tiền (UC-SUP-01)
- Người bán tiếp nhận và xử lý yêu cầu trả hàng (UC-SUP-02)
- Nhân viên sàn phân xử tranh chấp khiếu nại (UC-SUP-03)
- Thực hiện hoàn tiền — VNPay / VietQR / COD (UC-SUP-04)

*Phụ thuộc:* *Tạo yêu cầu Trả hàng* «extend» *Khách hàng xác nhận "Đã nhận hàng thành công"*; dùng ký quỹ + sổ cái.

### C9. Ví & rút tiền (3 use case)
- Xem ví & sổ giao dịch người bán (UC-VND-20)
- Người bán gửi yêu cầu rút tiền từ Ví về Ngân hàng (UC-ADM-04)
- Nhân viên sàn phê duyệt & Thực hiện lệnh chuyển khoản — Payout (UC-ADM-05)

*Phụ thuộc:* cần ví có tiền từ *Tự động giải phóng tiền ký quỹ*; *Gửi yêu cầu rút tiền* dùng tài khoản do *Đăng ký tài khoản nhận giải ngân* (cụm B4) tạo.

### C10. Báo cáo (3 use case)
- Xem báo cáo doanh thu & Phân tích bán hàng (UC-VND-08)
- Dashboard doanh thu & ký quỹ toàn sàn (UC-ADM-06)
- Bật chế độ tạm nghỉ bán hàng của shop (UC-VND-19)

*Phụ thuộc:* đọc đơn / ký quỹ — viết code sớm được nhưng không có số liệu để kiểm; *Chế độ tạm nghỉ* chặn khi shop còn đơn đang xử lý.

### C11. Email giao dịch (1 use case)
- Gửi email giao dịch tự động (UC-NOT-03)

*Phụ thuộc:* hạ tầng `EmailSender` đã có; nội dung email gắn với từng use case phát sự kiện (đặt hàng, thanh toán, hoàn tiền…).

## 7. ⚠️ Điểm chung dễ xung đột khi chạy nhiều plan song song

Use case độc lập về nghiệp vụ nhưng các nhánh vẫn sửa chung vài file — dễ **conflict khi merge**:

| File / chỗ | Vì sao đụng nhau | Cách tránh |
|---|---|---|
| `common/error/ErrorCode.java` + `web/src/shared/api/error-codes.ts` + khóa `errors.GNE-*` (`vi.json`, `en.json`) | Mỗi use case thêm mã lỗi | Ghi trước trong plan các mã sẽ thêm; merge `develop` thường xuyên |
| `common/storage/UploadPurpose.java` | Mỗi use case có ảnh thêm một purpose | Ghi rõ purpose trong plan |
| `common/audit/AuditAction.java` + `CHECK` của `system_logs` | Giá trị mới cần migration sửa `CHECK` | Chốt **số migration** (`V15`, `V16`…) theo thứ tự merge trước khi làm — hai nhánh cùng `V15` sẽ hỏng |
| `R__seed_demo.sql` | Nhiều cụm cần seed shop / sản phẩm | Một người giữ file seed, các plan gửi phần cần thêm |
| `docs/web-backend-handoff.md` | Nối web theo từng nhóm | Mỗi plan chỉ sửa dòng của nhóm mình |

## 8. Thứ tự đề xuất

1. **Song song ngay:** nhóm A (bắt đầu *Quản lý sổ địa chỉ giao hàng chuẩn GHN* và *Tra cứu nhật ký kiểm toán hệ thống*) + cụm **B1 Nhân viên & phân quyền** + cụm **B2 Danh mục sàn**.
2. **Tiếp theo:** **B3 Storefront đọc công khai** (trên seed — 6 / 7 đã xong 2026-11-21), **B4 Mở shop & KYC** (**7 / 7 ✅** 2026-11-25), **B6 Chat & realtime** (WebSocket làm nền), **B5 Voucher**.
3. **Sau cùng:** nhóm C theo chuỗi C1 Đăng bán → C2 Giỏ → Checkout → C3 Thanh toán & ký quỹ → C4 Vận chuyển → C5 Sau giao hàng → C6 Khuyến mãi khi mua / C7 Đánh giá / C8 Trả hàng & hoàn tiền / C9 Ví & rút tiền → C10 Báo cáo, C11 Email giao dịch.