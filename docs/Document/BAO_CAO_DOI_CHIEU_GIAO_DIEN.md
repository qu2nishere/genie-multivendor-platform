# BÁO CÁO ĐỐI CHIẾU GIAO DIỆN ↔ TÀI LIỆU (client + prototype)

> **Ngày lập:** 2026-10-28
> **Mục đích:** Liệt kê mọi màn hình / chức năng đang có trên giao diện (`prototype/` và `client/`) mà tài liệu **bản ngoài = `DATN/`** (`DATN/diagrams/`, `DATN/document/`, `DATN/kien-thuc/`, `DATN/implement.md`) và/hoặc **bản TLCN** (`TLCN/`) chưa đặc tả. Báo cáo dùng làm danh sách việc cần sửa sau.
> **Nguyên tắc phạm vi (bắt buộc):** TLCN phải đặc tả **toàn bộ** chức năng xuất hiện trên giao diện. Cụ thể là **hợp của client và prototype**:
> - Chức năng chỉ có ở prototype → **lấy**.
> - Chức năng chỉ có ở client (mục mới như *Tổng quan*, *Duyệt Sản phẩm*, *Thông báo sàn* của Admin, *Danh mục toàn sàn* của Seller) → **lấy**.
> - Mục nav client đang để `hidden` (Khuyến mãi, Đánh giá, Phân tích, Danh mục toàn sàn của Seller; Trả hàng & Hoàn tiền, Kiểm duyệt đánh giá, Danh mục toàn sàn của Admin) → **lấy**.
> - Màn hình gắn nhãn "(Đang phát triển)", "(Bảo trì)" hoặc chỉ có trong prototype mà client chưa kịp làm → **lấy**.
>
> **Vai trò hai bộ tài liệu:**
> - **Bản ngoài = thư mục `DATN/`** (200 UC; người dùng đã chuyển `diagrams/`, `document/`, `kien-thuc/`, `implement.md` từ gốc `fix-doc/` vào `DATN/`) là kế hoạch cho **đồ án tốt nghiệp (KLTN)**, triển khai toàn bộ sau này.
> - **TLCN** = toàn bộ chức năng của client + prototype (cộng các quyết định ở mục 0.3).
>
> Báo cáo này **không đề xuất bỏ chức năng nào khỏi giao diện**. Ngoại lệ duy nhất là thanh toán (mục 0.3). Khi giao diện và tài liệu lệch số liệu (mục 6), chỉ cần **chốt con số / quy tắc**, còn chức năng thì vẫn giữ.

> 🔁 **Cập nhật 2026-11-06:** đối chiếu lại với giao diện Genie (`genie/ui_kits/`) — xem [mục 15](#muc-15).
>
> 🔁 **Cập nhật 2026-11-16:** đối chiếu với mã nguồn `genie-multivendor-platform` (giao diện `web/` + backend AUTH + Silo) — xem [mục 18](#muc-18).
>
> ✅ **Trạng thái (2026-10-29): toàn bộ việc ở mục 4, 5, 9, 10, 11, 13 đã thực hiện** trên cả `TLCN/` và `DATN/` — xem [mục 14](#muc-14). Các mục 0–13 giữ nguyên làm hồ sơ phân tích.

**Ký hiệu**

| Ký hiệu | Nghĩa |
| :---: | :--- |
| ✅ | Đã có use case đặc tả đầy đủ |
| ⚠️ | Có một phần, hoặc có nhưng lệch với giao diện |
| ❌ | Chưa có |
| **P** | Có trên prototype |
| **C** | Có trên client |
| **C-ẩn** | Có trong client nhưng mục nav đang để `hidden` |

---

## 0. Tóm tắt nhanh

### 0.1. Số liệu

| Chỉ số | Giá trị |
| :--- | :---: |
| Dòng chức năng giao diện đã đối chiếu (mục 2) | **102** |
| TLCN chưa có (❌) | **35** |
| TLCN có một phần / lệch (⚠️) | **34** |
| Bản ngoài chưa có (❌) | **10** |
| Bản ngoài có một phần / lệch (⚠️) | **19** |
| Chức năng cần UC mới hoặc mở rộng ở **cả hai bản** | **17** (mục 5) |
| Mâu thuẫn số liệu / nghiệp vụ giữa giao diện và tài liệu | **14** (mục 6) |

### 0.2. Kết luận chính

1. **TLCN đang thiếu nhiều.** Bản MVP đã cắt đúng những phần mà client *đã làm thật* (có gọi API):
   - Điểm Genie
   - Theo dõi shop
   - Thông báo toàn sàn
   - Nhật ký hệ thống
   - Nhân viên & phân quyền
   - Sổ cái kép
   - Auto-reply
   - Kiểm duyệt đánh giá
   - Kiểm duyệt sản phẩm bằng AI
   - Đăng nhập Google
   - Xóa tài khoản

   Phần lớn đã có sẵn đặc tả ở bản ngoài, chỉ cần **đưa lại vào TLCN** với mã UC giữ nguyên (mục 4.1).
2. **Có 17 chức năng mà cả hai bản đều chưa có hoặc chưa đủ** (mục 5). Nổi bật:
   - Ví & sổ giao dịch người bán
   - Sửa thông tin gian hàng
   - Shop xác nhận đơn
   - Đề xuất danh mục
   - Giấy tờ chứng thực sản phẩm
   - Thương hiệu
   - Đăng nhập bằng OTP email
3. **Phương thức thanh toán đang lệch nặng** (đã chốt ở mục 0.3, dòng Q3).
   - Giao diện có: VNPay, MoMo, ZaloPay, thẻ quốc tế, COD (đang "bảo trì").
   - Tài liệu TLCN có: VNPay + **VietQR (SePay)** + COD.
   - Không màn hình nào có VietQR/SePay (mục 6).
4. **Nhiều nút trên giao diện chưa có chức năng thật.**
   - Nút **tim (Yêu thích)** và **Chia sẻ** trên trang SP chỉ là nút giao diện, chưa gọi API. Client không có endpoint wishlist.
   - Trang Shop có ghi cứng "Đánh giá shop 5★".
   - TLCN chưa có trường dữ liệu cho đánh giá shop, số người theo dõi, tỉ lệ phản hồi chat.
5. **`client/` không còn mã nguồn.** Thư mục chỉ có bản build `dist/`, không có `src/`, nên mọi phân tích client ở đây được trích từ bundle JS đã build. Muốn sửa giao diện phải khôi phục lại `src/` (từ git / máy khác) — mục 7.

### 0.3. Quyết định đã chốt (2026-10-28)

| # | Quyết định | Ảnh hưởng tới TLCN | Ghi chú kiểm tra |
| :-: | :--- | :--- | :--- |
| **Q1** | Phạm vi TLCN = **toàn bộ** chức năng client + prototype (kể cả mục ẩn, mục chỉ có ở client) | Mọi dòng ❌ / ⚠️ ở mục 2 đều phải bổ sung | — |
| **Q2** | **Kiểm duyệt sản phẩm tự động bằng Google Gemini API** (thay cho AI Worker tự host của bản ngoài) | Thêm UC-CAT-12 phiên bản TLCN: tác nhân chính "Hệ thống", tác nhân phụ **`ACT-GEM` Google Gemini API** (tác nhân hệ thống mới; tổng tác nhân TLCN 11 → 15, gồm cả 3 chức danh nhân viên ở mục 10). CAT-13 nhận kết quả từ CAT-12 | Chi tiết ở mục 4.3 |
| **Q3** | **Thanh toán: VNPay + SePay (VietQR) trước**, vì hoàn tiền được. MoMo / ZaloPay / thẻ quốc tế để sang KLTN | TLCN **giữ nguyên** PAY-01 (VNPay / VietQR), **không** đưa PAY-02 vào. Giao diện: **gỡ** MoMo, ZaloPay, thẻ quốc tế khỏi checkout; **thêm** màn hình VietQR (mã QR, số tiền, nội dung `TR{id}`, đếm ngược 15 phút, tự chuyển trang khi webhook SePay báo đã nhận) | ⚠️ **Lưu ý hoàn tiền:** chỉ **VNPay có API hoàn tiền tự động**. **SePay không chuyển tiền đi** — chỉ báo biến động số dư. Đơn trả bằng VietQR phải được **nhân viên chuyển khoản hoàn thủ công**, sau đó webhook SePay báo giao dịch ra (nội dung `TRRF{id}`) để hệ thống tự đánh dấu "đã hoàn". Luồng này đã có trong SUP-04 TLCN. Màn hình M5 (đồng bộ / thử lại hoàn tiền qua cổng) cần thêm trạng thái "chờ chuyển khoản thủ công" cho VietQR |
| **Q4** | **Giữ COD trong TLCN, kết hợp COD với escrow** giống hệt bản ngoài, để sang KLTN **không phải sửa lại** phần này | Phương thức TLCN = **VNPay + VietQR (SePay) + COD**. COD dùng chung mô hình escrow: đơn COD tạo `EscrowLedger` ở `PENDING_FUNDING` → GHN giao xong, đối soát tiền thu hộ (webhook SePay khi GHN chuyển tiền về) → `codFundedAmount` được ghi, chuyển `HELD` → giữ 7 ngày → `RELEASED` về ví shop. Giao diện **bật lại COD**, bỏ chữ "Bảo trì" / "Không khả dụng cho đơn bảo chứng" / "không hỗ trợ COD" | ✅ Đã kiểm: ENTITY TLCN dùng **cùng tên** với bản ngoài (`EscrowLedger`, trạng thái `PENDING_FUNDING`, trường `codFundedAmount`, `PaymentMethod.COD`, loại giao dịch `COD_CASH`). Khi đưa `FinancialLedger` (PAY-06) vào TLCN, dùng **cùng mã bút toán** với bản ngoài (`COD_REMIT`, `COD_OVERDUE`…). Phần COD do **tài xế Genie Express** thu (`ShipperCodLedger`, LOG-10) vẫn để KLTN, vì TLCN chỉ dùng GHN |
| **Q5** | Trang Shop phải đủ: **đánh giá shop, người theo dõi, số SP, tỉ lệ phản hồi chat**; trang SP có **"Hữu ích"** cho đánh giá, **chia sẻ deeplink**, nút **tim** | Thêm vào TLCN: SOC-03 (Hữu ích), SOC-06 (theo dõi), N15 (deeplink), N16 (chỉ số shop), CAT-08 (đã có, cần màn hình danh sách yêu thích) | Chi tiết ở mục 4.3 |

---

## 1. Nguồn đã đọc

### 1.1. Prototype (`prototype/lazadee/ui_kits/`)

| Bộ giao diện | File | Màn hình |
| :--- | :--- | :--- |
| Storefront | `Home, Discover, Search, Shop, ProductDetail, Cart, Checkout, OrderTracking, Account, AddressModal, ChatWidget, BecomeSeller, Header`, `index.html` | Trang chủ, khám phá, tìm kiếm, trang shop, chi tiết SP, giỏ, thanh toán, theo dõi đơn + trả hàng, tài khoản (9 tab), sổ địa chỉ, chat, mở shop, đặt hàng thành công / thất bại |
| Seller Center | `VendorShell, Overview, Products, Fulfillment, Wallet, Onboarding, Chat, VendorPages` | 10 mục nav: Tổng quan, Đơn hàng, Sản phẩm, Ví & Escrow, Hồ sơ KYC, Khuyến mãi, Chat, Đánh giá, Phân tích, Cài đặt; cổng khóa KYC |
| Admin Console | `AdminShell, KYCReview, Disputes, Finance, Catalog, AuditLog, Vouchers` | 11 mục nav: Duyệt KYC, Trả hàng & Hoàn tiền, Kiểm duyệt đánh giá, Xử lý rút tiền, Cấu hình hoa hồng, Sổ cái tài chính, Nhật ký hệ thống, Danh mục toàn sàn, Voucher sàn, Phân quyền nhân viên, Tài khoản; xem nav theo vai trò |
| Auth | `auth/index.html` | Đăng nhập, OTP, khóa tài khoản sau 5 lần sai, đăng ký, quên mật khẩu, đăng nhập Google/Apple |

### 1.2. Client (`client/dist/`)

Client chỉ có bản build. Các bundle đã phân tích:

| Bundle | Route |
| :--- | :--- |
| `StorefrontApp` | `/` |
| `VendorApp` | `/vendor/*` |
| `AdminApp` | `/admin` |
| `LazadeeAuth` | `/auth/login`, `/auth/register` |
| `SharedComponents` | — |

Riêng `/staff` chỉ là trang giữ chỗ ("đây là trang staff").

**Nav Seller trên client** (ẩn = `hidden: true`):

| Mục | Prototype | Client |
| :--- | :---: | :---: |
| Tổng quan | P | C |
| Đơn hàng | P | C |
| Sản phẩm | P | C |
| Ví & Escrow | P | C |
| Hồ sơ KYC | P | C |
| **Danh mục toàn sàn** | — | **C-ẩn** (mới, chỉ có ở client) |
| Khuyến mãi | P | C-ẩn |
| Chat | P | C |
| Đánh giá | P | C-ẩn |
| Phân tích | P | C-ẩn |
| Cài đặt | P | C |

**Nav Admin trên client:**

| Mục | Quyền | Prototype | Client |
| :--- | :--- | :---: | :---: |
| **Tổng quan** | `dashboard.view` | — | **C** (mới) |
| Duyệt KYC | `kyc.review` | P | C |
| Trả hàng & Hoàn tiền | `refund.arbitrate` | P | C-ẩn |
| Kiểm duyệt đánh giá | `review.moderate` | P | C-ẩn |
| Xử lý rút tiền | `payout.process` | P | C |
| Cấu hình hoa hồng | `commission.update` | P | C |
| Sổ cái tài chính | `finance.view` | P | C |
| Nhật ký hệ thống | `audit.view` | P | C |
| Danh mục toàn sàn | `catalog.manage` | P | C-ẩn |
| **Duyệt Sản phẩm** | `catalog.manage` | — | **C** (mới) |
| Voucher sàn | `voucher.manage` | P | C |
| **Thông báo sàn** | `notif.manage` | — | **C** (mới) |
| Phân quyền nhân viên | `rbac.manage` | P | C |
| Tài khoản | — | P | C |

**API client đang gọi** (bằng chứng chức năng đã làm thật):

- **Storefront**
  - `customer/coins`, `coins/checkin`, `coins/history`
  - `customer/followed-shops`, `shops/{id}/follow`
  - `customer/vouchers`, `vouchers/platform`
  - `customer/notifications`
  - `customer/orders/{id}/cancel`, `customer/orders/confirm`
  - `products/{id}/similar`, `products/{id}/reviews`, `reviews`
  - `auth/change-password-otp`, `auth/verify-password`
  - `vendor/register`, `vendor/send-otp`
  - `checkout/preview`, `checkout`, `payments`
  - `addresses/provinces|districts|wards`
- **Seller**
  - `vendor/dashboard`, `vendor/dashboard/recent-orders`
  - `vendor/wallet/overview`, `vendor/wallet/transactions`, `vendor/wallet/withdraw`
  - `vendor/vouchers`, `vendor/auto-reply`, `vendor/kyc`, `vendor/badges`
- **Admin**
  - `admin/finance/dashboard`, `admin/finance/chart`, `admin/finance/top-vendors`
  - `admin/ledger`, `admin/withdrawals/{id}/process`
  - `admin/kyc/{id}/decide`
  - `admin/products/moderation`, `admin/products/{id}/moderate`, `admin/products/{id}/reassign`
  - `admin/moderators/config`, `admin/moderators/redistribute`
  - `admin/categories`, `admin/commissions/bulk`
  - `admin/system-logs`, `admin/system-logs/export`
  - `admin/platform-notifications`
  - `admin/staffs`, `admin/staffs/{id}/permissions`, `admin/staffs/{id}/lock`
  - `admin/vouchers`, `admin/password/change-request`
- **Auth**
  - `auth/customer/register`, `auth/verify-otp`
  - **`auth/login-otp/send`, `auth/login-otp/verify`**

### 1.3. Tài liệu đối chiếu

| Bản | File |
| :--- | :--- |
| Bản ngoài | `DATN/diagrams/UseCaseDiagram/Usecase.md` (200 UC), `DATN/diagrams/ClassDiagram/ENTITY.md` |
| TLCN | `TLCN/diagrams/UseCaseDiagram/Usecase.md` (57 UC), `TLCN/diagrams/ClassDiagram/ENTITY.md` (38 entity) |

---

## 2. Ma trận đối chiếu theo phân hệ

### 2.1. Xác thực (Auth)

| # | Chức năng trên giao diện | Nguồn | Bản ngoài | TLCN | Ghi chú |
| :-: | :--- | :-: | :-: | :-: | :--- |
| A1 | Đăng ký người mua + xác thực OTP email | P, C | ✅ AUTH-01 | ✅ AUTH-01 | |
| A2 | Đăng nhập email + mật khẩu | P, C | ✅ AUTH-02 | ✅ AUTH-02 | |
| A3 | **Đăng nhập bằng mã OTP gửi email** (không mật khẩu) | P, C (`login-otp/*`) | ❌ | ❌ | Prototype ghi "Nhập Email để nhận mã OTP". Client có API riêng. Cần bổ sung luồng thay thế vào AUTH-02 hoặc tạo UC mới |
| A4 | Khóa tài khoản sau 5 lần nhập sai OTP / mật khẩu | P, C | ✅ AUTH-07 | ⚠️ | TLCN đã chuyển AUTH-07 sang yêu cầu phi chức năng. AUTH-02 chỉ có luồng 4a "đang bị khóa", chưa có bước đếm số lần sai và khóa. Còn thiếu luồng sai OTP ở AUTH-01 (client báo "sai OTP quá 5 lần, vui lòng đăng ký lại") |
| A5 | Đăng nhập / đăng ký bằng Google, Apple | P, C | ✅ AUTH-03 | ❌ | Client: tab Tài khoản phân biệt "Tài khoản Google / Apple / Email" và có "Thiết lập mật khẩu" cho người dùng SSO |
| A6 | Quên mật khẩu qua OTP email | P, C | ✅ AUTH-04 | ✅ AUTH-04 | |
| A7 | Đổi mật khẩu (có OTP) cho khách, shop và admin | P, C | ✅ AUTH-06 | ✅ AUTH-06 | Tên UC TLCN "tài khoản nội bộ" chưa khớp, vì tác nhân gồm cả Customer → nên đổi tên |
| A8 | **Thiết lập mật khẩu lần đầu cho tài khoản SSO** | P, C | ⚠️ | ❌ | Bản ngoài AUTH-03 nhắc SSO nhưng chưa có luồng "đặt mật khẩu để đăng nhập song song". Phụ thuộc A5 |
| A9 | Đăng xuất | P, C | ✅ AUTH-11 | ✅ AUTH-11 | |
| A10 | Danh sách thiết bị đăng nhập + "Đăng xuất tất cả thiết bị khác" | P | ✅ AUTH-11 | ⚠️ | TLCN có "đăng xuất tất cả thiết bị" nhưng thiếu bước **xem danh sách phiên** (thiết bị, IP, vị trí, "Phiên này"). Admin cũng có panel "Phiên đăng nhập" |
| A11 | Cổng đăng nhập riêng cho nhân viên (`/staff`) | C (giữ chỗ) | — | — | Chưa có nội dung. Cần quyết định: gộp vào `/admin` (phân quyền theo nav) hay tách cổng |

### 2.2. Khách hàng — tài khoản

| # | Chức năng | Nguồn | Bản ngoài | TLCN | Ghi chú |
| :-: | :--- | :-: | :-: | :-: | :--- |
| K1 | Hồ sơ: avatar ≤1MB, họ tên, email, SĐT, giới tính, ngày sinh, giới thiệu, ngôn ngữ | P, C | ✅ AUTH-08 | ⚠️ AUTH-08 | TLCN chưa có trường "giới thiệu (bio)" và "ngôn ngữ". Kiểm tra `CustomerProfile` |
| K2 | Đổi email (yêu cầu thiết lập mật khẩu trước nếu là tài khoản SSO) | P | ⚠️ | ❌ | |
| K3 | **Xóa tài khoản** | C | ✅ AUTH-10 | ❌ | Client có nút "Xóa tài khoản" + hộp xác nhận |
| K4 | Sổ địa chỉ GHN: thêm / sửa / xóa / đặt mặc định, loại địa chỉ Nhà riêng / Văn phòng | P, C | ✅ AUTH-09 | ✅ AUTH-09 | |
| K5 | **Lấy địa chỉ từ GPS** ("Dùng vị trí hiện tại") | P | ⚠️ | ❌ | Bản ngoài có GPS cho shipper, không có cho địa chỉ khách |
| K6 | Kho voucher: lưu, xóa, lịch sử (đã dùng / hết hạn), chi tiết voucher | P, C | ✅ PRO-03 | ⚠️ PRO-03 | TLCN gộp "lưu + áp dụng". Chưa có xóa voucher khỏi kho, tab lịch sử, xem chi tiết |
| K7 | **Điểm Genie**: số dư, điểm danh hằng ngày, lịch sử Điểm, đổi quà | P, C | ✅ PRO-08, PRO-09 (+GAM-08 đổi quà) | ❌ | Client đã gọi `customer/coins`, `checkin`, `history` |
| K8 | **Shop đang theo dõi** (danh sách, hủy theo dõi, vào shop) | P, C | ✅ SOC-06 | ❌ | Có cả nút Theo dõi ở trang SP và trang Shop |
| K9 | Trung tâm thông báo: tab Đơn hàng / Khuyến mãi / Hệ thống / Chat, đánh dấu đã đọc | P, C | ✅ NOT-01 | ⚠️ NOT-01 | TLCN chưa có phân loại tab và "đánh dấu tất cả đã đọc" |
| K10 | Thông báo **toàn sàn** hiển thị cho khách (`PLATFORM_NOTIFICATION`) | C | ✅ NOT-05 | ❌ | Đi kèm mục Admin "Thông báo sàn" (M13) |
| K11 | Chuyển ngôn ngữ (Tiếng Việt / …) | P, C | ⚠️ | ❌ | Đưa vào yêu cầu phi chức năng i18n trong Document_Vi, kèm trường `language` trong hồ sơ (K1). Không cần UC riêng |

### 2.3. Khách hàng — mua sắm

| # | Chức năng | Nguồn | Bản ngoài | TLCN | Ghi chú |
| :-: | :--- | :-: | :-: | :-: | :--- |
| S1 | Trang chủ: banner, ô danh mục, voucher nổi bật có nút "Lưu" | P, C | ⚠️ | ⚠️ | Bản ngoài ADM-12 có banner CMS. TLCN không có UC cho banner, banner đang hard-code |
| S2 | **Khu Flash Sale** trên trang chủ, nhãn FLASH SALE trên SP | P | ✅ PRO-05/06 | ❌ | TLCN đã cắt Flash Sale → đưa lại PRO-05/06 (mục 4.1) |
| S3 | **"Gợi ý hôm nay" / Khám phá** (lưới SP, phân trang) | P, C | ✅ REC-03 (AI) | ❌ | Client chỉ lấy `sortBy=random`. TLCN cần một UC đơn giản không dùng AI, hoặc ghép vào CAT-01/CAT-05 |
| S4 | Tìm kiếm + lọc (danh mục, khoảng giá, số sao, Mall, freeship, chính hãng) + sắp xếp | P, C | ✅ CAT-02, CAT-05 | ✅ CAT-02, CAT-05 | Kiểm lại: TLCN có lọc "Mall", "Freeship", "Chính hãng" chưa |
| S5 | Tìm **trong shop** ↔ mở rộng toàn sàn | P, C | ⚠️ | ⚠️ CAT-16 | |
| S6 | Duyệt danh mục theo cây, trang danh mục có mô tả | C | ✅ CAT-01 | ✅ CAT-01 | |
| S7 | Chi tiết SP: biến thể, tồn kho, thông số, mô tả rich-text, "Gửi từ" | P, C | ✅ CAT-06 | ✅ CAT-06 | |
| S8 | Nút **tim** — Yêu thích (Wishlist) | P, C | ✅ CAT-08 | ✅ CAT-08 | Tài liệu đủ. **Giao diện chưa đủ:** nút tim chưa gọi API, không có trạng thái đã thích / bỏ thích, chưa có trang "SP yêu thích" trong tab Tài khoản → thêm màn hình + API |
| S9 | **Chia sẻ SP bằng deeplink** | P, C | ❌ | ❌ | Theo Q5: cần UC mới N15. Nút trên client hiện chưa có chức năng |
| S10 | **Sản phẩm tương tự** | P, C (`/similar`) | ✅ CAT-07 / REC-02 (AI) | ❌ | TLCN cần bản đơn giản (cùng danh mục / cùng shop) |
| S11 | Đánh giá trên trang SP: lọc "Có hình ảnh", nút **"Hữu ích"**, phản hồi của shop | P, C | ⚠️ SOC-03 (`helpfulCount`) | ❌ | TLCN chưa có "Hữu ích" và bộ lọc ảnh. Bản ngoài chỉ tăng `helpfulCount`, chưa chặn một người bấm nhiều lần → cần bảng `ReviewHelpfulVote` |
| S12 | Trang Shop: **đánh giá shop**, **người theo dõi**, **số SP**, **tỉ lệ phản hồi chat**, "tham gia từ", voucher shop, tab SP | P, C | ⚠️ (chỉ có tỉ lệ phản hồi trong luật phạt VND-10, không có trường lưu) | ⚠️ CAT-16 | Cả hai bản **chưa có trường dữ liệu** cho các chỉ số này. Client đang ghi cứng "Đánh giá shop 5★" → UC mới N16 |
| S13 | Giỏ hàng nhiều shop, chọn từng món, xóa nhiều món, cảnh báo "Giá lúc thêm tăng / giảm" | P, C | ✅ ORD-01 | ✅ ORD-01 | |
| S14 | Voucher shop theo từng shop, Genie Voucher trong giỏ | P, C | ✅ PRO-03/04 | ✅ PRO-03 | |
| S15 | Checkout: địa chỉ, phí GHN, lời nhắn cho shop, voucher sàn + freeship + voucher shop, **dùng Điểm tối đa 50%** | P, C | ✅ (PRO-10 Điểm) | ⚠️ | TLCN chưa có Điểm |
| S16 | Phương thức thanh toán | P, C | ⚠️ | ⚠️ | **Đã chốt Q3:** VNPay + VietQR (SePay). Giao diện gỡ MoMo / ZaloPay / thẻ, thêm màn hình VietQR. **COD giữ (Q4)**, bật lại trên checkout |
| S17 | Đặt hàng thành công: hóa đơn theo phiên, tách theo shop | P, C | ✅ ORD-07 | ✅ ORD-07 | |
| S18 | Thanh toán thất bại → **thử lại / đổi phương thức** (giữ tồn kho thêm) | P, C | ⚠️ | ⚠️ | Chưa UC nào có luồng "thanh toán lại đơn `PENDING_PAYMENT`". Xem thêm `client/checkout_ux_plan.md` (giỏ hàng bị xóa khi thanh toán lỗi) |
| S19 | Đơn mua: tab trạng thái, **"Thanh toán ngay"** cho đơn chờ, **"Hủy nhóm đơn"** (cả phiên) | P, C | ⚠️ ORD-10 | ⚠️ ORD-10 | TLCN chỉ hủy từng đơn con. Client có hủy cả nhóm |
| S20 | **"Mua lại" / "Mua lại tất cả"** | P, C | ❌ | ❌ | Thêm luồng phụ vào ORD-01 |
| S21 | Theo dõi đơn: timeline GHN, mã vận đơn, giải thích escrow | P, C | ✅ ORD-09 | ✅ ORD-09 | |
| S22 | Đã nhận hàng | P, C | ✅ ORD-11 | ✅ ORD-11 | |
| S23 | Trả hàng / hoàn tiền: chọn món, lý do, bằng chứng ảnh / video ≤10MB | P, C | ✅ SUP-01 | ✅ SUP-01 | |
| S24 | Viết đánh giá + ảnh (≤5) | P, C | ✅ SOC-03 | ✅ SOC-03 | |
| S25 | Chat với shop: thẻ SP, gửi ảnh, trạng thái gửi / nhận / đã xem, câu hỏi gợi ý | P, C | ✅ SOC-01, SOC-02 | ⚠️ SOC-01 | TLCN chưa có SOC-02 (thẻ SP, ảnh) và **trạng thái đã xem** |
| S26 | Mở shop (Become Seller) + **OTP email** + đồng ý Điều khoản người bán | P, C | ✅ VND-01 | ⚠️ VND-01 | Client gọi `vendor/send-otp`. Kiểm lại VND-01 TLCN có bước OTP chưa |
| S27 | Trang chính sách tĩnh (khoảng 30 trang: Quy chế sàn, Điều khoản, Bảo mật, Hoa hồng…) | C | ⚠️ (văn bản pháp lý ở kien-thuc) | ❌ | Không phải UC. Liệt kê trong Document_Vi như "trang nội dung tĩnh" |

### 2.4. Seller Center

| # | Chức năng | Nguồn | Bản ngoài | TLCN | Ghi chú |
| :-: | :--- | :-: | :-: | :-: | :--- |
| V1 | Cổng khóa KYC: chưa `VERIFIED` thì khóa mọi mục trừ KYC và Cài đặt | P, C | ⚠️ | ⚠️ | Nên ghi rõ làm quy tắc nghiệp vụ trong VND-02/04 |
| V2 | Tổng quan: doanh thu tháng, số dư khả dụng, escrow, **đơn chờ xác nhận / chờ đóng gói / đang giao**, biểu đồ 7/30 ngày / năm, đơn gần đây, truy cập nhanh | P, C | ⚠️ VND-08 | ⚠️ VND-08 | VND-08 là phân tích, không phải dashboard vận hành |
| V3 | **Shop xác nhận / từ chối đơn** (PAID → CONFIRMED) | P (nút "Chuẩn bị"), C ("Đơn chờ xác nhận") | ❌ | ❌ | LOG-01 yêu cầu đơn đã `CONFIRMED` nhưng không UC nào chuyển sang trạng thái đó |
| V4 | Danh sách đơn: tab, tìm, sắp xếp, trạng thái GHN chi tiết (giao thất bại, đang trả hàng, đơn gặp sự cố…) | P, C | ✅ LOG-01/03 | ⚠️ LOG-01/03 | TLCN chưa liệt kê đủ trạng thái GHN (giao thất bại, hoàn hàng, sự cố) |
| V5 | Tạo vận đơn, **yêu cầu lấy hàng 3PL**, in / in lại vận đơn | P, C | ✅ LOG-02 | ✅ LOG-02 | |
| V6 | Danh sách SP: tab Tất cả / Đang bán / Nháp / Chờ duyệt / Không hợp lệ, sửa, **xóa** | P, C | ✅ CAT-09/10/11 | ✅ | |
| V7 | Form SP: danh mục, thương hiệu, tình trạng, xuất xứ, thông số, mô tả rich-text, 2 nhóm phân loại có ảnh, cân nặng / kích thước | P, C | ✅ CAT-09/15 | ⚠️ CAT-09 | TLCN chưa có "tình trạng (mới / cũ / refurbished)", "xuất xứ", bảng thông số gợi ý theo ngành (CAT-15) |
| V8 | **Thêm thương hiệu mới** ngay trong form ("Thêm … làm thương hiệu mới") | C | ❌ (không có entity `Brand`) | ❌ | Cần quyết định `brand` là text tự do hay bảng `brands` |
| V9 | **Giấy tờ chứng thực sản phẩm** (bắt buộc, PDF/DOC/XLS ≤4 file) | C | ❌ | ❌ | Client chặn đăng bán nếu thiếu. Admin xem ở "Hồ sơ pháp lý sản phẩm" |
| V10 | **Phí ship do ai chịu** + **shop hỗ trợ freeship từ X₫** (trong form SP và Cài đặt) | P, C | ❌ | ❌ | |
| V11 | "Gửi hàng từ" (tỉnh) trong form SP | P, C | ⚠️ | ⚠️ | Trùng với kho lấy hàng VND-05 → nên lấy tự động |
| V12 | **Ví & Escrow**: số dư khả dụng, đang giữ, doanh thu tháng, cách tính Net, **sổ giao dịch ví** (lọc, xuất CSV) | P, C | ❌ | ❌ | ADM-04 chỉ là rút tiền |
| V13 | Rút tiền: số tiền, ngân hàng, chi nhánh, số TK, tên chủ TK | P, C | ✅ ADM-04 | ✅ ADM-04 | Giao diện nhập lại TK ngân hàng mỗi lần rút, tài liệu dùng TK đã đăng ký ở VND-07 → mục 6, dòng X10 |
| V14 | KYC: loại hình (Cá nhân / Hộ KD / Doanh nghiệp), số CCCD, MST, người đại diện, CCCD 2 mặt, GPKD, **selfie chụp từ camera**, trạng thái `SUPPLEMENT_REQUESTED` | P, C | ✅ VND-02 | ✅ VND-02 | |
| V15 | **Danh mục toàn sàn** phía shop (nav ẩn) — xem cây danh mục + **đề xuất danh mục mới** | C-ẩn (+ Admin P) | ❌ | ❌ | Admin prototype có "Đề xuất danh mục từ người bán" (duyệt / từ chối) |
| V16 | Voucher shop: %, tiền, freeship; **phạm vi theo danh mục / SP + loại trừ danh mục / SP**; nhân bản; tắt | P, C-ẩn | ⚠️ PRO-01 | ⚠️ PRO-01 | Tài liệu có `scope = ALL/CATEGORY/PRODUCT` nhưng chưa có **loại trừ** |
| V17 | **Đăng ký Flash Sale** cho gian hàng | P, C-ẩn | ✅ PRO-05 | ❌ | |
| V18 | Chỉ số khuyến mãi: voucher đang chạy, lượt dùng, doanh thu từ KM | P, C-ẩn | ⚠️ | ❌ | |
| V19 | Chat của shop: danh sách hội thoại, thẻ SP, trạng thái đã xem | P, C | ✅ SOC-01/02 | ⚠️ SOC-01 | |
| V20 | Đánh giá: lọc Tất cả / Chưa phản hồi / ≤3★, phản hồi | P, C-ẩn | ✅ SOC-04 | ✅ SOC-04 | |
| V21 | **Báo cáo đánh giá vi phạm** (từ phía shop) | C (Admin hiển thị "Báo cáo bởi người bán") | ✅ SOC-05 | ❌ | |
| V22 | Phân tích: doanh thu, đơn, **lượt truy cập, tỉ lệ chuyển đổi, nguồn truy cập**, SP bán chạy | P, C-ẩn | ✅ VND-08 + ANA-* | ⚠️ VND-08 | TLCN không có dữ liệu lượt xem / nguồn truy cập. Client ghi "(Đang phát triển)" |
| V23 | **Thông tin gian hàng**: ảnh bìa, logo, tên, mô tả | P, C | ⚠️ VND-14 (trang trí nâng cao) | ❌ | |
| V24 | Địa chỉ lấy hàng GHN | C | ✅ VND-05 | ✅ VND-05 | |
| V25 | **Đơn vị vận chuyển**: GHN, GHTK, ViettelPost, J&T, Genie Express (FBL) | P, C | ⚠️ LOG-15 | ❌ | TLCN chỉ có GHN → mục 6, dòng X3 |
| V26 | Tài khoản nhận tiền: thêm / **xóa**, chọn ngân hàng, tên chủ TK trùng CCCD | P, C | ✅ VND-07 | ⚠️ VND-07 | Chưa có luồng xóa / đổi tài khoản |
| V27 | **Tự động trả lời chat** (lời chào + tin vắng mặt) | P, C (`vendor/auto-reply`) | ✅ VND-06 | ❌ | TLCN đã cắt VND-06 |
| V28 | **Cho phép đặt trước (Pre-order)** | P, C | ❌ | ❌ | |
| V29 | **Chế độ nghỉ (Vacation)** | P, C | ✅ VND-19 | ❌ | |
| V30 | Đổi mật khẩu shop có OTP | P, C | ✅ AUTH-06 | ✅ AUTH-06 | |
| V31 | Chuông thông báo, badge số trên nav (`vendor/badges`) | C | ✅ NOT-01 | ✅ NOT-01 | |
| V32 | "Kênh người mua / Trở về Genie" (chuyển vai trò) | C | — | — | Không cần UC |

### 2.5. Admin Console

| # | Chức năng | Nguồn | Bản ngoài | TLCN | Ghi chú |
| :-: | :--- | :-: | :-: | :-: | :--- |
| M1 | **Tổng quan tài chính**: GMV, hoa hồng, chi phí voucher, **lợi nhuận ròng**, escrow, **nợ vendor**, refund chờ, việc tồn, biểu đồ tăng trưởng, **top vendor** | C | ✅ ADM-06 | ⚠️ ADM-06 | TLCN chưa có lợi nhuận ròng, nợ vendor, việc tồn, top vendor có phân trang |
| M2 | Duyệt KYC: hàng chờ, trình xem tài liệu (phóng to / xoay / lật), kết quả OCR, so khuôn mặt, kiểm QR / **quốc huy** / MRZ / **vân tay**, mức rủi ro | P, C | ✅ VND-03/04 | ⚠️ VND-03/04 | TLCN có QR, MRZ, face, liveness. **Thiếu "quốc huy", "vân tay"** và cách tính "mức rủi ro" |
| M3 | Phê duyệt / từ chối (lý do chuẩn + lý do khác) / **yêu cầu bổ sung (7 ngày)** / đưa lại vào hàng chờ | P, C | ✅ VND-04 | ⚠️ VND-04 | Thời hạn bổ sung 7 ngày và "đưa lại vào hàng chờ" chưa ghi |
| M4 | Trả hàng & Hoàn tiền: phân xử, bằng chứng, hoàn / từ chối / mở lại | P, C-ẩn | ✅ SUP-03 | ✅ SUP-03 | |
| M5 | **Trạng thái hoàn tiền qua cổng**: đồng bộ, thử lại khi lỗi | P, C-ẩn | ✅ SUP-04 | ✅ SUP-04 | |
| M6 | **Kiểm duyệt đánh giá**: ẩn / hiện / giữ hiển thị, lý do (spam, thô tục…) | P, C-ẩn | ✅ SOC-05 | ❌ | |
| M7 | Xử lý rút tiền: duyệt chi, **từ chối có lý do (hoàn lại ví)**, chặn khi KYC chưa duyệt | P, C | ✅ ADM-05 | ⚠️ ADM-05 | Kiểm lại luồng từ chối có hoàn ví |
| M8 | Cấu hình hoa hồng: mặc định + theo ngành, lưu hàng loạt | P, C | ✅ CAT-14 | ✅ CAT-14 | |
| M9 | **Sổ cái tài chính** kép (Nợ / Có, sự kiện `ESCROW_IN / ESCROW_RELEASE / PAYOUT_ACCRUE / PAYOUT_SETTLE`…), xuất file | P, C | ✅ PAY-06 + ADM-06 | ❌ | TLCN đã bỏ `FinancialLedger` |
| M10 | **Nhật ký hệ thống**: lọc thời gian / loại / từ khóa, xuất file, lưu 12 tháng | P, C | ✅ ADM-07 | ❌ | TLCN có ghi `SystemLog` nhưng không có màn hình xem |
| M11 | Danh mục toàn sàn: cây, thêm / sửa / **xóa** / ẩn, hoa hồng mặc định, xem SP trong danh mục, từ chối SP | P, C-ẩn | ✅ CAT-14 | ⚠️ CAT-14 | Chưa có luồng xóa danh mục (khi còn SP?) |
| M12 | **Duyệt đề xuất danh mục của người bán** | P, C-ẩn | ❌ | ❌ | Đi cùng V15 |
| M13 | **Thông báo sàn**: soạn (rich-text, ảnh), gửi, thống kê | C | ✅ NOT-05 | ❌ | |
| M14 | **Duyệt sản phẩm**: hàng chờ, chi tiết SP, giấy tờ pháp lý, **phân tích AI / lý do AI từ chối**, duyệt / từ chối | C | ✅ CAT-12 (AI) + CAT-13 | ⚠️ CAT-13 | TLCN có CAT-13 nhưng **đã bỏ CAT-12 (AI)**, trong khi CAT-13 TLCN vẫn nhắc "cảnh báo do AI Worker" → tự mâu thuẫn |
| M15 | **Cấu hình nhân viên duyệt**: bật / tắt "Đang nhận việc", **phân phối lại**, **giao lại** từng SP | C | ⚠️ CAT-13 | ⚠️ CAT-13 | Đã có round-robin và `isActiveModerator`. Chưa có phân phối lại / giao lại thủ công |
| M16 | Voucher sàn: tên chương trình, %, tiền, freeship, phạm vi theo danh mục + **loại trừ**, **ngân sách tự tắt**, co-sponsor, nhân bản, tắt, **xóa vĩnh viễn** | P, C | ✅ PRO-02 | ⚠️ PRO-02 | TLCN chưa có loại trừ, ngân sách tự tắt, co-sponsor |
| M17 | **Phân quyền nhân viên**: danh sách, mời qua email + OTP, vai trò (Super Admin / Moderator / Finance / CS / KYC Reviewer / Tùy chỉnh), sửa quyền chi tiết, khóa / mở, thu hồi, xóa | P, C | ✅ ADM-01/02/03 | ⚠️ chỉ ADM-03 | TLCN ghi "STAFF một vai trò" nhưng client dùng **quyền chi tiết** (`REVIEW_KYC, HANDLE_DISPUTE, SUPPORT_CUSTOMER, PROCESS_PAYOUT, UPDATE_COMMISSION, MANAGE_VOUCHER, MANAGE_CATALOG, VIEW_AUDIT_LOG, VIEW_SYSTEM_LOG`). **→ Đã chốt ở mục 10:** không có Super Admin; TLCN có 3 chức danh (MODERATOR, CS_SPECIALIST, FINANCE_OFFICER) + Admin. |
| M18 | Nav theo quyền + màn "Bạn không có quyền truy cập" | P, C | ✅ ADM-02 | ❌ | Phụ thuộc M17 |
| M19 | Tài khoản admin: avatar, thông tin, đổi mật khẩu bắt buộc OTP, **phiên đăng nhập** | P, C | ✅ AUTH-06/08/11 | ⚠️ | AUTH-08 TLCN chỉ dành cho Customer → mở rộng tác nhân |
| M20 | Chuông thông báo admin (`admin/notifications`) | C | ✅ NOT-01 | ⚠️ | TLCN NOT-01 chưa ghi admin là người nhận |
| M21 | Khóa / mở khóa **người dùng** vi phạm (khách, shop) | — (chưa có màn hình) | ✅ ADM-03 | ✅ ADM-03 | **Ngược lại:** tài liệu có, giao diện chưa có → cần màn hình "Người dùng" |

---

## 3. Các màn hình tài liệu TLCN có nhưng giao diện chưa có

Hai bên phải khớp nhau, nên những mục dưới đây cần **thêm màn hình** vào client và prototype (giữ nguyên trong TLCN):

| UC TLCN | Thiếu gì trên giao diện |
| :--- | :--- |
| ADM-03 Khóa / mở khóa người dùng | Không có mục "Quản lý người dùng" (khách, shop) trong Admin. Client chỉ khóa **nhân viên** |
| CAT-13 Kiểm duyệt SP | Prototype chưa có, client có (M14) → cần cập nhật prototype |
| ADM-06 Dashboard | Prototype chưa có, client có (M1) → cần cập nhật prototype |
| PAY-01 VietQR (SePay) | Không màn hình nào có QR chuyển khoản / chờ webhook |
| ORD-06 + LOG-03 luồng COD | Prototype ghi "không hỗ trợ COD". Client có COD nhưng đang "Bảo trì" |
| VND-07 Xác minh tài khoản ngân hàng | Giao diện chỉ có nhãn "Đã xác minh", không có bước xác minh |
| ORD-08 Hủy đơn quá hạn | Có nhắc "tồn kho được giữ trong …", không có màn hình riêng → OK, là tác vụ hệ thống |
| VND-08 vs Phân tích | Có màn hình, nhưng nav đang ẩn trên client |

---

## 4. Việc cần làm ở TLCN

> ✅ **Đã thực hiện 2026-10-29** — kết quả ở [mục 14](#muc-14).

### 4.1. Đưa lại từ bản ngoài (giữ nguyên mã UC, rút gọn cho MVP)

| Ưu tiên | UC bản ngoài | Lý do (bằng chứng giao diện) | Entity cần đưa lại |
| :-: | :--- | :--- | :--- |
| **Cao** | **ADM-01** Quản lý nhân viên & lời mời | M17 — client có API `admin/staffs` | (dùng `User` + `StaffProfile`) |
| **Cao** | **ADM-02** Vai trò & quyền chi tiết | M17, M18 — client dùng 9 mã quyền | `StaffPermission` |
| **Cao** | **ADM-07** Tra cứu nhật ký hệ thống | M10 — client có `admin/system-logs` + export | (đã có `SystemLog`) |
| **Cao** | **PAY-06** Sổ cái tài chính kép | M9 — client có `admin/ledger` (Nợ / Có) | `FinancialLedger` |
| **Cao** | **NOT-05** Thông báo toàn sàn | M13, K10 — client có `admin/platform-notifications` | `PlatformNotification`, `UserPlatformNotificationRead` |
| **Cao** | **SOC-05** Báo cáo & kiểm duyệt đánh giá | M6, V21 | `ReviewReport` |
| **Cao** | **CAT-12** Kiểm duyệt SP tự động — **bằng Gemini API** (Q2, đặc tả ở mục 4.3) | M14 — client hiển thị "Phân tích từ AI", "Lý do AI từ chối" | `ProductModerationResult` (mới) hoặc các trường AI trên `Product` |
| **Cao** | **PRO-08** Điểm danh nhận Điểm | K7 — client có `coins/checkin` | `UserCoin`, `CoinTransaction` |
| **Cao** | **PRO-09** Số dư & lịch sử Điểm | K7 — `coins/history` | (như trên) |
| **Cao** | **PRO-10** Dùng Điểm khi thanh toán (≤50%) | S15 | (như trên) |
| **Cao** | **SOC-06** Theo dõi shop | K8 — `followed-shops`, `shops/{id}/follow` | `ShopFollow` |
| **Cao** | **VND-06** Tự động trả lời chat | V27 — `vendor/auto-reply` | (trường trên `VendorProfile`) |
| TB | **AUTH-03** Đăng nhập Google và Apple | A5, A8 | `UserSocialAccount` (hoặc trường `provider`) |
| TB | **AUTH-10** Xóa tài khoản (bản rút gọn: ẩn danh hóa) | K3 | — |
| TB | **SOC-02** Thẻ SP / ảnh trong chat | S25, V19 | (`ChatMessage.type`) |
| TB | **VND-19** Chế độ tạm nghỉ | V29 | (trường trên `VendorProfile`) |
| TB | **CAT-07** SP tương tự — **bản không AI** (cùng danh mục / shop) | S10 — `/similar` | — |
| TB | **CAT-15** Thuộc tính chuẩn theo ngành | V7 (bảng thông số gợi ý theo ngành) | `CategoryAttribute` |
| TB | **PRO-05** Flash Sale (sàn tạo khung giờ, shop đăng ký SP) | S2, V17 | `FlashSaleCampaign`, `FlashSaleItem` |
| TB | **PRO-06** Mua hàng Flash Sale (giữ tồn riêng, giá Flash Sale khi checkout) — rút gọn, không cần Redis Lua | S2 | (như trên) |
| TB | **LOG-15** Nhiều hãng vận chuyển (GHN, GHTK, ViettelPost, J&T) — rút gọn: shop bật/tắt hãng, khách chọn hãng | V25, X3 | `VendorCarrier` (mới) hoặc trường trên `VendorProfile` |
| TB | **ANA (rút gọn)** Ghi lượt xem SP / shop + nguồn truy cập để tính tỉ lệ chuyển đổi | V22, X13 | `ProductViewStat` (mới, đếm theo ngày) |
| TB | **ADM-12** Banner trang chủ | S1 | `Banner` |
| Thấp | **GAM-08 (rút gọn)** Đổi Điểm lấy quà | K7 (nút "Đổi quà") | — |
| Thấp | **Genie Express (FBL)** — chỉ ở mức lựa chọn "giao nội thành" trong Cài đặt shop, không cần app tài xế | V25 | — |

### 4.2. Sửa / mở rộng UC TLCN hiện có

| UC TLCN | Cần bổ sung |
| :--- | :--- |
| AUTH-01 | Luồng sai OTP 5 lần → hủy đăng ký |
| AUTH-02 | (a) Luồng **đăng nhập bằng OTP email** (A3)<br>(b) Đếm số lần sai → khóa tạm (A4) |
| AUTH-06 | Đổi tên thành "Đổi mật khẩu (có OTP)" — áp dụng cho mọi vai trò; thêm "thiết lập mật khẩu cho tài khoản SSO" |
| AUTH-08 | Thêm trường bio, ngôn ngữ; mở rộng tác nhân sang Vendor / Staff / Admin (avatar, hồ sơ) |
| AUTH-11 | Thêm bước **xem danh sách phiên** (thiết bị, IP, vị trí) |
| CAT-09 / CAT-10 | Tình trạng, xuất xứ, phí ship do ai chịu, freeship của shop, giấy tờ chứng thực, thương hiệu (mục 5) |
| CAT-13 | Phân phối lại, giao lại, bật / tắt nhận việc; xử lý mâu thuẫn AI (M14) |
| CAT-14 | Xóa danh mục; duyệt đề xuất danh mục (M12) |
| ORD-01 | "Mua lại" |
| ORD-10 | Hủy cả nhóm đơn (phiên checkout) |
| PAY-01 | Luồng **thanh toán lại** đơn `PENDING_PAYMENT` (S18); **chốt danh sách phương thức** (mục 6, dòng X1) |
| PRO-01 / PRO-02 | Loại trừ danh mục / SP; ngân sách tự tắt; co-sponsor; xóa vĩnh viễn; tên chương trình |
| PRO-03 | Xóa voucher khỏi kho, tab lịch sử, chi tiết |
| NOT-01 | Tab phân loại, đánh dấu tất cả đã đọc, người nhận là admin / staff |
| SOC-01 | Trạng thái đã nhận / đã xem |
| SOC-03 | Nút "Hữu ích", lọc "Có hình ảnh" |
| VND-01 | Bước OTP email, chọn ngành hàng, đồng ý điều khoản |
| VND-03 / VND-04 | Kiểm quốc huy, vân tay, mức rủi ro; hạn bổ sung 7 ngày; đưa lại vào hàng chờ |
| VND-07 | Xóa / đổi tài khoản nhận tiền |
| VND-08 | Lượt truy cập, tỉ lệ chuyển đổi, nguồn truy cập — lấy dữ liệu từ ANA rút gọn (mục 4.1) |
| LOG-01 / LOG-03 | Đủ trạng thái GHN: giao thất bại, đang hoàn, đã hoàn, sự cố |
| ADM-04 | Thống nhất: rút về TK đã đăng ký (VND-07) hay nhập TK mỗi lần (X10) |
| ADM-05 | Từ chối kèm lý do → hoàn lại ví |
| ADM-06 | Lợi nhuận ròng, nợ vendor, việc tồn, top vendor |

### 4.3. Đặc tả gợi ý cho các quyết định Q2, Q3, Q5

#### a) UC-CAT-12 (TLCN) — Kiểm duyệt sản phẩm tự động bằng Gemini API

| Mục | Nội dung gợi ý |
| :--- | :--- |
| **Tác nhân** | Chính: Hệ thống (job nền). Phụ: **`ACT-GEM` Google Gemini API** (tác nhân hệ thống mới) |
| **Kích hoạt** | Vendor bấm "Đăng bán" hoặc sửa SP đang bán (CAT-09/10) → SP chuyển `PENDING_APPROVAL` → gửi job kiểm duyệt (bất đồng bộ, không chặn người bán) |
| **Đầu vào gửi Gemini** | Tên, mô tả (bỏ HTML), danh mục, thương hiệu, giá; tối đa N ảnh (URL Silo đã ký hoặc base64); tên file giấy tờ chứng thực; danh sách chính sách cấm (hàng cấm, hàng giả, ngôn từ phản cảm, sai danh mục, giá bất thường) |
| **Đầu ra yêu cầu (JSON schema)** | `decision` (`APPROVE` / `REJECT` / `NEED_REVIEW`), `riskScore` 0–100, `violations[]` (mã lý do khớp danh sách lý do từ chối ở M14), `reasoning` (tiếng Việt, hiển thị ở "Phân tích từ AI") |
| **Xử lý kết quả** | • `APPROVE` và `riskScore` thấp → **vẫn vào hàng chờ CAT-13** (MVP không cho AI tự đăng bán; nếu muốn tự duyệt thì ghi rõ ngưỡng)<br>• `REJECT` → SP `REJECTED`, ghi "Lý do AI từ chối"; người bán sửa và gửi lại<br>• `NEED_REVIEW` → vào hàng chờ CAT-13, ưu tiên cao |
| **Ngoại lệ** | • Gemini lỗi / quá hạn / vượt quota → thử lại có giãn cách (3 lần), sau đó chuyển `NEED_REVIEW` để người duyệt<br>• JSON sai định dạng → coi như `NEED_REVIEW`<br>• Không gửi dữ liệu cá nhân của người bán sang Gemini |
| **Lưu trữ** | Entity mới `ProductModerationResult` (productId, model, decision, riskScore, violations, reasoning, rawResponse, createdAt) — giữ lịch sử mỗi lần chạy. Ghi `SystemLog` |
| **Cấu hình** | `GEMINI_API_KEY`, tên model, ngưỡng `riskScore` — để trong biến môi trường / bảng cấu hình |
| **Sửa kèm** | CAT-13 TLCN: lấy "cảnh báo AI" từ CAT-12 (hiện CAT-13 nhắc "AI Worker" → đổi thành Gemini). Thêm `ACT-GEM` vào Phần I Usecase.md, PlantUML, XMI, Excel. Phần chi phí trong `kien-thuc` thêm phí Gemini API |
| **Bản ngoài (KLTN)** | Giữ CAT-12 dùng AI Worker. Có thể ghi chú "TLCN dùng Gemini API" |

#### b) Thanh toán VNPay + SePay (Q3)

| Việc | Chi tiết |
| :--- | :--- |
| Tài liệu TLCN | PAY-01 / PAY-03 / SUP-04 đã đúng hướng VNPay + VietQR. **Không** thêm PAY-02. Enum `PaymentMethod` giữ {VNPAY, VIETQR, COD} (Q4). Luồng COD + escrow giữ nguyên như ORD-11/12, LOG-03, PAY-05/07, SUP-04 hiện tại |
| Giao diện checkout | Bỏ MoMo, ZaloPay, Thẻ quốc tế. Thêm "Chuyển khoản VietQR". Sau khi đặt hàng → màn hình QR (số TK, số tiền, nội dung `TR{id}`, đếm ngược 15 phút, tự chuyển sang "Thanh toán thành công" khi nhận webhook) |
| Giao diện Admin M5 | Hoàn tiền đơn VietQR: trạng thái `PENDING_MANUAL_TRANSFER` → nhân viên bấm "Đã chuyển khoản" hoặc webhook SePay (`TRRF{id}`) tự xác nhận. VNPay giữ "Đồng bộ / Thử lại" |
| Footer, trang chính sách | Gỡ chữ "Ví MoMo & ZaloPay", "Thẻ quốc tế", "Trả góp 0%" |

#### c) Trang SP và trang Shop (Q5)

| Chức năng | Hiện trạng | Cần làm ở TLCN |
| :--- | :--- | :--- |
| **Nút tim (Yêu thích)** | TLCN có CAT-08. Client: nút chưa gọi API, không có trang danh sách | Giữ CAT-08. Bổ sung vào CAT-08: trạng thái tim đầy / rỗng, bấm lần 2 để bỏ thích, trang "SP yêu thích" trong tab Tài khoản |
| **Hữu ích cho đánh giá** | TLCN ❌. Bản ngoài có `helpfulCount` | SOC-03 thêm luồng "Bấm Hữu ích" (mỗi người một lần, bấm lại để bỏ, không tự bấm đánh giá của mình), sắp xếp theo hữu ích. Entity mới **`ReviewHelpfulVote`** (reviewId, userId, unique). `ProductReview.helpfulCount` |
| **Chia sẻ deeplink SP** | Cả hai ❌. Client: nút chưa có chức năng | **N15** — UC mới (mục 5) |
| **Người theo dõi** | Bản ngoài SOC-06 + `ShopFollow`; TLCN ❌ | Đưa SOC-06 vào (mục 4.1). Thêm `VendorProfile.followerCount` (đếm sẵn) |
| **Đánh giá shop** | Cả hai ❌ (client ghi cứng 5★) | **N16** — `VendorProfile.ratingAvg`, `ratingCount`, tính từ đánh giá SP của shop (cập nhật khi có đánh giá mới / bị ẩn) |
| **Số sản phẩm** | Đếm SP `ACTIVE` | **N16** — `VendorProfile.activeProductCount` hoặc đếm khi truy vấn |
| **Tỉ lệ phản hồi chat** (+ thời gian phản hồi) | Cả hai ❌ (bản ngoài chỉ dùng trong luật phạt) | **N16** — % hội thoại do khách mở mà shop trả lời trong 24h, tính trong 30 ngày gần nhất (job hằng ngày). Trường `VendorProfile.chatResponseRate`, `avgResponseMinutes` |
| **Tham gia từ / Online** | Client có | Lấy `VendorProfile.createdAt`, trạng thái online từ WebSocket — ghi vào CAT-16 |

---

## 5. Chức năng cả hai bản đều chưa có (cần UC mới ở bản ngoài **và** TLCN)

> ✅ **Đã thực hiện 2026-10-29** — kết quả ở [mục 14](#muc-14).

| # | Đề xuất mã | Tên UC | Tác nhân | Nguồn giao diện | Entity / thay đổi CSDL |
| :-: | :--- | :--- | :--- | :--- | :--- |
| N1 | **UC-VND-20** | Xem ví & sổ giao dịch người bán (số dư, escrow, lịch sử, xuất CSV) | Vendor | V12 — `vendor/wallet/overview`, `transactions` | Dùng `VendorWallet`, `WalletTransaction`, `EscrowLedger` có sẵn |
| N2 | **UC-VND-21** | Cập nhật thông tin gian hàng (ảnh bìa, logo, tên, mô tả) | Vendor | V23 | Trường `coverUrl`, `logoUrl`, `description` trên `VendorProfile` |
| N3 | **UC-LOG-16** (hoặc tách khỏi LOG-01) | Shop xác nhận / từ chối đơn mới (PAID / COD → CONFIRMED hoặc CANCELLED_BY_VENDOR) | Vendor | V3 | Không thêm bảng |
| N4 | **UC-CAT-17** | Người bán đề xuất danh mục mới & Admin duyệt | Vendor, Staff | V15, M12 | `CategoryProposal` (mới) |
| N5 | **(mở rộng CAT-09 + CAT-13)** | Giấy tờ chứng thực sản phẩm (bắt buộc khi đăng bán, Admin xem khi duyệt) | Vendor, Staff | V9, M14 | `ProductDocument` (mới) |
| N6 | **(mở rộng CAT-09)** | Thương hiệu — chọn / tạo mới | Vendor | V8 | `Brand` (mới) hoặc giữ `brandName` text |
| N7 | **(mở rộng CAT-09 + VND-05)** | Chính sách phí ship của shop: người mua chịu / shop hỗ trợ freeship từ X₫ | Vendor | V10 | Trường `freeShipThreshold` trên `VendorProfile`; ORD-06 tính trừ |
| N8 | **(mở rộng CAT-09)** | Pre-order | Vendor | V28 | `isPreOrder`, `preOrderDays` trên `Product` |
| N9 | **(mở rộng AUTH-02)** | Đăng nhập bằng OTP email | Customer | A3 — `login-otp/*` | — |
| N10 | **(mở rộng ORD-01)** | Mua lại | Customer | S20 | — |
| N11 | **(mở rộng ORD-10)** | Hủy cả nhóm đơn | Customer | S19 | — |
| N12 | **(mở rộng PAY-01)** | Thanh toán lại đơn chờ / đổi phương thức | Customer | S18 | — |
| N13 | **(mở rộng AUTH-09)** | Lấy địa chỉ từ GPS | Customer | K5 | — |
| N14 | **(mở rộng CAT-13)** | Phân phối lại / giao lại việc duyệt | Admin | M15 | `Product.assignedModeratorId` (kiểm lại) |
| N15 | **UC-CAT-18** | Chia sẻ sản phẩm bằng deeplink (sao chép link `/p/{slug}-{id}`; thẻ Open Graph khi dán lên Facebook / Zalo; mở link trên trình duyệt đi thẳng tới trang SP, giữ biến thể nếu có) | Customer, Guest | S9 | Không thêm bảng (dùng `Product.slug`). Tùy chọn: đếm lượt chia sẻ |
| N16 | **(mở rộng CAT-16 + SOC-03 + SOC-01)** | Chỉ số trang Shop: đánh giá shop, người theo dõi, số SP, tỉ lệ & thời gian phản hồi chat | Hệ thống (job), Customer xem | S12 | Trường mới trên `VendorProfile`: `ratingAvg`, `ratingCount`, `followerCount`, `activeProductCount`, `chatResponseRate`, `avgResponseMinutes` |
| N17 | **(mở rộng SOC-03)** | Chống bấm "Hữu ích" nhiều lần | Customer | S11 | `ReviewHelpfulVote` (mới) |

---

## 6. Mâu thuẫn giữa giao diện và tài liệu (cần chốt một phía)

| # | Nội dung | Giao diện nói | TLCN nói | Đề xuất |
| :-: | :--- | :--- | :--- | :--- |
| **X1** | **Phương thức thanh toán** | VNPay, **MoMo, ZaloPay, Visa / Mastercard**, COD (bảo trì). **Không có VietQR** | VNPay + **VietQR (SePay)** + COD | **Đã chốt (Q3):** VNPay + SePay. Giao diện gỡ MoMo / ZaloPay / thẻ (để sang KLTN) và thêm màn hình VietQR. COD giữ (Q4) |
| **X2** | Hỗ trợ COD | Prototype: "không hỗ trợ COD (giữ escrow)". Client: COD "Không khả dụng cho đơn bảo chứng" | COD có, escrow sau đối soát GHN | **Đã chốt (Q4):** giữ COD + escrow. Giao diện bật COD; checkout giải thích "tiền COD vào escrow sau khi GHN đối soát"; OrderTracking và Ví shop hiển thị trạng thái "Chờ đối soát COD" |
| **X3** | Đơn vị vận chuyển | GHN, GHTK, ViettelPost, J&T, **Genie Express (FBL)** | Chỉ GHN | **Lấy hết:** TLCN thêm LOG-15 rút gọn (nhiều hãng 3PL) và FBL ở mức lựa chọn giao nội thành |
| **X4** | Thời hạn đổi trả | Footer prototype: **7 ngày**. Auth và footer client: **15 ngày** | 7 ngày (SUP-01) | Thống nhất 7 ngày |
| **X5** | Tự động hoàn tất đơn | OrderTracking prototype: "tự hoàn tất **sau 3 ngày**" | 7 ngày (ORD-12) | Sửa giao diện thành 7 ngày |
| **X6** | Thời gian duyệt KYC | "24 giờ" (KYC), "1–3 ngày" (Tổng quan client), "1–2 ngày" (trang Mở shop) | Kiểm lại VND-04 | Chốt một con số |
| **X7** | Số ảnh SP tối đa | Prototype: 8. Client: **6** | 8 (CAT-09) | Thống nhất |
| **X8** | Độ dài mã voucher | Voucher sàn client: **4–12** ký tự. Shop: 6–12 | 6–12 | Thống nhất 6–12 |
| **X9** | Hoa hồng sàn | Ví shop hiển thị cố định "Phí sàn 5%" | Theo ngành (CAT-14) | Giao diện lấy theo ngành của từng đơn |
| **X10** | Rút tiền | Form nhập lại ngân hàng / số TK mỗi lần | Dùng TK đã xác minh ở VND-07 | Theo TLCN: form chỉ hiện TK đã đăng ký |
| **X11** | Vai trò nhân viên | 5 vai trò + quyền chi tiết | "STAFF một vai trò" | **Đã chốt (mục 10):** 3 chức danh + Admin, 13 quyền; đưa ADM-01/02 vào TLCN (mục 4.1) |
| **X12** | Kiểm duyệt SP bằng AI | Có "Phân tích từ AI", "Lý do AI từ chối" | Bỏ CAT-12, nhưng CAT-13 vẫn nhắc AI | **Đã chốt (Q2):** CAT-12 dùng Gemini API (mục 4.3a) |
| **X13** | Lượt truy cập / chuyển đổi shop | Có thẻ chỉ số, ghi "(Đang phát triển)" | Không có nguồn dữ liệu | Thêm ANA rút gọn: bảng đếm lượt xem theo ngày (mục 4.1) |
| **X14** | Ví Genie của khách | Footer client có "Ví Genie", "Hướng dẫn sử dụng Ví". Đây chỉ là **liên kết tới trang chính sách**, không có màn hình ví | TLCN đã bỏ ví khách hàng | Giữ dạng trang nội dung tĩnh. Nếu muốn có màn hình ví thì đưa WAL-01/02/03/07 vào — **cần bạn xác nhận** |

---

## 7. Lưu ý kỹ thuật về `client/`

1. **Không có `client/src/`.** Thư mục chỉ có:
   - `dist/` — bản build Astro
   - `node_modules/`
   - cấu hình build
   - 4 file `Header*.jsx` lẻ ở thư mục gốc (bản sao lưu)

   Vì không có mã nguồn để sửa, cần khôi phục `src/` (git / máy khác).
2. Bundle client gọi API cứng `http://localhost:8080` và `https://api.lazadee.store` → nên chuyển sang biến môi trường.
3. `client/README.md` vẫn là README mẫu của Astro.
4. Các file rỗng `product.json`, `response.json`, `temp.html` có thể xóa.
5. `client/checkout_ux_plan.md` bị lỗi mã hóa tiếng Việt (mojibake). Nội dung là vấn đề "giỏ hàng bị xóa khi thanh toán lỗi" — liên quan S18 / N12.
6. Trang `/staff` mới là trang giữ chỗ (A11).

---

## 8. Thứ tự sửa đề xuất

1. **Chốt các mâu thuẫn ở mục 6.** X1, X2, X3, X11, X12 ảnh hưởng nhiều file nhất.
2. **Đưa lại các UC ưu tiên Cao ở mục 4.1** vào TLCN (Usecase.md, PlantUML, XMI, Excel), kèm entity tương ứng trong ENTITY.md, Java và XMI class.
3. **Thêm UC mới N1–N4 và N15 ở mục 5** vào **cả hai bản**. Các mục mở rộng N5–N14, N16, N17 thì sửa vào UC hiện có.
4. Sửa các UC ở mục 4.2.
5. Cập nhật:
   - `TLCN/BAO_CAO_PHAM_VI_MVP.md` (số UC / entity mới)
   - `TLCN/document/Document_Vi.md`
   - kế hoạch sprint trong `TLCN/kien-thuc/implement.md`
6. Cập nhật giao diện cho khớp mục 3 và mục 6. Việc này cần mã nguồn client — xem mục 7.
7. Chạy lại bộ kiểm tra chéo (UC ↔ PlantUML ↔ XMI ↔ Excel ↔ ENTITY). Lưu ý: thư mục `backup/audit` hiện đã bị xóa.

> Ước tính sau khi lấy **toàn bộ** (24 UC đưa lại ở mục 4.1 + 5 UC mới N1–N4, N15; các mục N còn lại là mở rộng UC hiện có): TLCN tăng từ **57 lên khoảng 86 UC**, từ **38 lên khoảng 56 entity**, từ **11 lên 15 tác nhân** (thêm `ACT-GEM`, `ACT-MOD`, `ACT-CS`, `ACT-FIN`). Cột "Ưu tiên" ở mục 4.1 chỉ là **thứ tự làm**, không phải mức tùy chọn — mọi dòng đều bắt buộc.

---

## 9. BÀN GIAO — Hướng dẫn cho lượt chỉnh sửa toàn bộ (đọc mục này trước khi bắt đầu)

> ✅ **Đã thực hiện 2026-10-29** — kết quả ở [mục 14](#muc-14).

> Mục này viết cho phiên làm việc sau (Claude hoặc người khác), khi ngữ cảnh hội thoại đã được reset. Đọc theo thứ tự **0.3 → 9 → 10 → 11 → 12 → 13** là đủ để bắt đầu (mục 11 là hai danh sách việc TLCN / DATN); các mục 2–6 là dữ liệu chi tiết để tra cứu.

### 9.1. Bối cảnh

- Dự án **Genie** — sàn TMĐT đa người bán. Thư mục gốc: `D:\TLCN-KLTN\TLCN\fix-doc\`.
- **Bản ngoài = `DATN/`** (`DATN/diagrams/`, `DATN/document/`, `DATN/kien-thuc/`, `DATN/implement.md` — trước đây nằm ở gốc `fix-doc/`, nay đã chuyển vào `DATN/`; ở gốc **không còn** các thư mục này): 200 UC, kế hoạch cho **đồ án tốt nghiệp (KLTN)**, triển khai sau.
- **TLCN** (`TLCN/`): bản đang làm. Hiện có 57 UC, 11 phân hệ, 11 tác nhân, 38 entity, 7 sơ đồ lớp. Mục tiêu: **phủ toàn bộ giao diện client + prototype** → khoảng 86 UC, 56–58 entity, 15 tác nhân.
- Xem như **chưa có mã nguồn backend**. Mọi UC TLCN ghi "*Hiện trạng:* Chưa triển khai — xây mới từ đầu" + "*Kế hoạch:* Sprint N".
- **Không còn script sinh tự động.** Thư mục `backup/` (mvp-generator, audit) đã bị xóa → phải **sửa tay** và tự giữ đồng bộ giữa các file.

### 9.2. Quy tắc bắt buộc khi sửa

1. **Giữ nguyên mã và tên của bản ngoài.** Đưa UC từ bản ngoài vào TLCN thì giữ **mã UC, tên entity, tên bảng, enum, mã lỗi** y hệt, chỉ rút gọn nội dung. Mục đích: lên KLTN không phải đổi tên. UC mới thì đánh mã tiếp theo trong phân hệ (VND-20, VND-21, LOG-16, CAT-17, CAT-18…) và **thêm vào cả bản ngoài**.
2. **OTP chỉ qua email.** Ngân hàng dùng **SePay** (VietQR + webhook):
   - Nội dung chuyển khoản: `TR{id}` (thanh toán), `TRWD{id}` (rút tiền), `TRRF{id}` (hoàn tiền).
   - SePay **không tự chuyển tiền đi**: rút tiền và hoàn tiền đơn VietQR do nhân viên chuyển khoản tay, webhook SePay xác nhận.
3. **Thanh toán TLCN:** VNPay + VietQR (SePay) + COD.
   - COD dùng chung escrow: `PENDING_FUNDING` → đối soát GHN → `HELD` 7 ngày → `RELEASED`.
   - Đơn COD: DELIVERED → PAID (sau đối soát) → COMPLETED.
4. **Vận chuyển:**
   - GHN là chính. Thêm GHTK / ViettelPost / J&T theo LOG-15 rút gọn.
   - Genie Express chỉ ở mức lựa chọn giao nội thành, **không** có app tài xế.
5. **Kiểm duyệt sản phẩm tự động:** **Gemini API**, tác nhân hệ thống mới `ACT-GEM` (mục 4.3a).
6. **Tác nhân phụ nối vào use case dùng chung:**
   - Email Service → «Gửi mã OTP xác thực»
   - Silo → «Tải tệp lên Silo»
7. **Mỗi UC phải khớp ở 5 nơi:**
   - Phần III `Usecase.md`
   - Ma trận Phần II `Usecase.md`
   - `UML_UseCase_PlantUML.md`
   - `Genie_UseCase_UML25.xmi`
   - `Quản Lý UseCase.xlsx`

   Mỗi entity phải khớp ở 3 nơi:
   - `ENTITY.md`
   - file Java tương ứng trong `java-code/`
   - `Genie_ClassDiagram_UML25.xmi`
8. **Lưu ý khi sửa XMI Enterprise Architect:** boundary và package phải có `seqno` **lớn nhất**, nếu không sẽ che mất use case. Trong nhãn PlantUML **không** dùng dấu `"` lồng nhau (dùng “ ”).
9. **Không xóa liên kết hỏng tới ảnh `.png` / `Document_Eng.md` / `implement.md`** ở bản ngoài. Người dùng đã cố ý xóa các file đó và chấp nhận để nguyên liên kết. `TLCN/implement.md` đã được người dùng chuyển vào `TLCN/kien-thuc/implement.md`.
10. **Sao lưu trước khi sửa.** Copy `TLCN/` và các file bản ngoài sắp sửa vào một thư mục backup có ngày.

### 9.3. Danh sách file phải sửa

**TLCN**

| File | Việc |
| :--- | :--- |
| `TLCN/diagrams/UseCaseDiagram/Usecase.md` | Phần I: thêm `ACT-GEM`, sửa mô tả STAFF (nhiều vai trò + quyền chi tiết). Phần II: ma trận, đếm lại. Phần III: thêm UC mục 4.1 + N1–N4, N15; sửa UC theo mục 4.2, 4.3; quan hệ include / extend. Phần IV: bảng thống kê. CHANGELOG |
| `TLCN/diagrams/UseCaseDiagram/UML_UseCase_PlantUML.md` | Thêm UC / tác nhân vào sơ đồ phân hệ + sơ đồ tổng quan + sơ đồ tác nhân |
| `TLCN/diagrams/UseCaseDiagram/Genie_UseCase_UML25.xmi` | Thêm element use case / actor / association / include / extend + vị trí trên diagram (chú ý `seqno`) |
| `TLCN/diagrams/UseCaseDiagram/Quản Lý UseCase.xlsx` | Thêm dòng UC (cột A là mã UC dạng chữ, cột Ghi chú là Sprint); sheet tác nhân thêm `ACT-GEM` |
| `TLCN/diagrams/ClassDiagram/ENTITY.md` | Thêm entity mục 4.1 / 4.3 / 5. Thêm trường mới vào `VendorProfile`, `Product`, `ProductReview`, `CustomerProfile`. Thêm enum, quan hệ, mục 13 (cấu hình), Phụ lục A–E, CHANGELOG |
| `TLCN/diagrams/ClassDiagram/java-code/*.java` | Cập nhật / thêm sơ đồ lớp (có thể cần Diagram08 mới cho Điểm + Theo dõi shop + Thông báo sàn + RBAC) |
| `TLCN/diagrams/ClassDiagram/Genie_ClassDiagram_UML25.xmi` | Đồng bộ với ENTITY.md |
| `TLCN/document/Document_Vi.md` | Danh sách chức năng, tác nhân, kiến trúc (Gemini API), thanh toán, i18n (yêu cầu phi chức năng), trang nội dung tĩnh |
| `TLCN/kien-thuc/implement.md` | Kế hoạch sprint: phân bổ khoảng 29 UC mới vào sprint (có thể phải kéo dài từ 6 lên 7–8 sprint) |
| `TLCN/kien-thuc/BAO_CAO_DU_TOAN_CHI_PHI_VA_GIAI_PHAP_AI_v1.0.md` | Chi phí Gemini API |
| `TLCN/kien-thuc/Kien_Thuc_Can_Nam.md` | Kiến thức: Gemini API (structured output), deeplink / Open Graph |
| `TLCN/BAO_CAO_PHAM_VI_MVP.md` | Cập nhật số liệu, bảng giữ / bỏ, phần "Điểm xuất phát & Kế hoạch" |

**Bản ngoài** — chỉ thêm các UC / mở rộng thuộc mục 5 (N1–N17) để KLTN cũng có:

| File | Việc |
| :--- | :--- |
| `DATN/diagrams/UseCaseDiagram/Usecase.md`, `UML_UseCase_PlantUML.md`, `Lazadee_UseCase_UML25.xmi`, `Quản Lý UseCase.xlsx` | Thêm VND-20, VND-21, LOG-16, CAT-17, CAT-18 và các mở rộng N5–N17 |
| `DATN/diagrams/ClassDiagram/ENTITY.md` + Java + XMI class | `CategoryProposal`, `ProductDocument`, `Brand`, `ReviewHelpfulVote`, các trường chỉ số shop trên `VendorProfile`, `freeShipThreshold`, `isPreOrder` |
| `DATN/document/Document_Vi.md`, `DATN/implement.md` | Nhắc tới các chức năng mới |

### 9.4. Trình tự thực hiện đề xuất

1. Sao lưu.
2. **ENTITY trước:** thêm entity / trường / enum (TLCN rồi bản ngoài) → Java → XMI class.
3. **Usecase.md TLCN:**
   - Phần I: tác nhân.
   - Phần III theo phân hệ: AUTH → CAT → ORD → PAY → VND → LOG → PRO → SOC → SUP → NOT → ADM.
   - Phần II: ma trận.
   - Phần IV: thống kê.
4. PlantUML → XMI use case → Excel (TLCN).
5. Bản ngoài: thêm các UC N1–N4, N15 và mở rộng N5–N17 ở 5 nơi.
6. Document_Vi, implement.md (sprint), kien-thuc, BAO_CAO_PHAM_VI_MVP (TLCN).
7. **Kiểm tra chéo cuối** (tự viết lại script vì `backup/audit` đã mất):
   - Mã UC: trùng / thiếu giữa 5 nơi.
   - Tham chiếu tới entity / trường / enum / mã lỗi có tồn tại.
   - Cú pháp PlantUML.
   - Liên kết và anchor nội bộ.
   - Số liệu: 7 ngày giữ escrow, 15 phút giữ chỗ, 7 ngày đổi trả…
8. Cập nhật báo cáo này: đánh dấu ✅ các dòng đã xử lý.

### 9.5. Việc còn treo (cần hỏi người dùng nếu chưa chốt)

| # | Câu hỏi | Mặc định nếu không trả lời |
| :-: | :--- | :--- |
| 1 | X4 — đổi trả 7 hay 15 ngày | 7 ngày (theo TLCN) |
| 2 | X5 — tự hoàn tất đơn 3 hay 7 ngày | 7 ngày |
| 3 | X6 — SLA duyệt KYC | 24 giờ làm việc |
| 4 | X7 — số ảnh SP tối đa 6 hay 8 | 8 (tài liệu); sửa client thành 8 |
| 5 | X8 — mã voucher 4–12 hay 6–12 ký tự | 6–12 |
| 6 | X14 — có làm màn hình Ví khách hàng (WAL) không | Không — chỉ là trang chính sách |
| 7 | CAT-12 Gemini — AI được **tự đăng bán** khi rủi ro thấp, hay luôn cần người duyệt | Luôn cần người duyệt (CAT-13) |
| 8 | A11 — cổng `/staff` riêng hay gộp vào `/admin` | Gộp vào `/admin`, phân quyền theo nav |
| 9 | Khôi phục `client/src/` để sửa giao diện | Người dùng tự tìm lại mã nguồn |

---

## 10. NHÂN VIÊN SÀN & PHÂN QUYỀN (quyết định 2026-10-28)

### 10.1. Hiện trạng các nguồn

| Nguồn | Vai trò / chức danh | Quyền |
| :--- | :--- | :--- |
| **Prototype** (`admin/data.jsx`, `AdminShell.jsx`, `Finance.jsx`) | Dropdown xem theo vai trò: `Super Admin`, `Moderator`, `Finance Staff`, `CS Staff`. Danh sách nhân viên mẫu có thêm `KYC Reviewer`, `Finance`, `Support` | 10 quyền dạng chấm: `kyc.review`, `refund.arbitrate`, `payout.process`, `catalog.manage`, `review.moderate`, `commission.update`, `voucher.manage`, `rbac.manage`, `finance.view`, `audit.view`. Có:<br>• preset quyền theo vai trò<br>• màn "Bạn không có quyền truy cập… Liên hệ Super Admin" |
| **Client** (`AdminApp`) | Còn `SUPER_ADMIN` / "Super Admin" (3 chỗ), có mục vai trò "Tuỳ chỉnh (Khác)" | Nav thêm `dashboard.view`, `notif.manage`. Quyền gửi về API dạng HOA: `REVIEW_KYC, HANDLE_DISPUTE, SUPPORT_CUSTOMER, PROCESS_PAYOUT, UPDATE_COMMISSION, MANAGE_VOUCHER, MANAGE_CATALOG, VIEW_AUDIT_LOG, VIEW_SYSTEM_LOG` |
| **DATN** (`DATN/diagrams/ClassDiagram/ENTITY.md`, dòng ~12904) | `UserRole {CUSTOMER, VENDOR, VENDOR_STAFF, SHIPPER, STAFF, ADMIN, B2B_BUYER}` — **đã không có SUPER_ADMIN**.<br>`StaffTitle` có 10 chức danh: KYC_OFFICER, MODERATOR, CS_SPECIALIST, FINANCE_OFFICER, MARKETING_OFFICER, DISPATCHER, RISK_ANALYST, COMPLIANCE_OFFICER, B2B_CREDIT_OFFICER, TECH_OPS.<br>Tác nhân con của `ACT-STF`: ACT-KYC, ACT-MOD, ACT-CS, ACT-FIN, ACT-MKT… | `StaffPermissionEnum` 18 quyền. Quy tắc "**chức danh là mẫu, quyền là luật**": `AdminProfile.staffTitle` chỉ dùng để nạp bộ quyền mặc định, quyền thật nằm ở bảng `StaffPermission` |
| **TLCN** (hiện tại) | `UserRole {CUSTOMER, VENDOR, STAFF, ADMIN}`; một tác nhân `ACT-STF`, không chia chức danh; có `AdminProfile`, `isActiveModerator` | Chưa có bảng quyền |

**Còn sót "super_admin" trong DATN:** `DATN/implement.md` dòng ~10547 và ~10559 có câu SQL RLS `current_setting('app.user_role') IN ('admin', 'super_admin')` → sửa thành `= 'admin'`.

### 10.2. Quyết định

**1. Không có Super Admin.**
- Vai trò cao nhất là **`ADMIN`** (Quản trị viên).
- Mọi chữ "Super Admin" / `SUPER_ADMIN` trên prototype, client và tài liệu đổi thành **Admin** / `ADMIN`.

**2. TLCN có 3 chức danh nhân viên** (role `STAFF` + trường `staffTitle`). **Giữ đúng tên enum của DATN** để sang KLTN không phải đổi:

| `StaffTitle` (TLCN) | Tên hiển thị | Tác nhân UML (mã DATN) | Phụ trách màn hình Admin |
| :--- | :--- | :--- | :--- |
| `MODERATOR` | Nhân viên kiểm duyệt | `ACT-MOD` | Duyệt KYC, Duyệt sản phẩm (gồm kết quả Gemini), Danh mục toàn sàn + đề xuất danh mục, Kiểm duyệt đánh giá |
| `CS_SPECIALIST` | Nhân viên CSKH | `ACT-CS` | Trả hàng & Hoàn tiền (phân xử tranh chấp), Kiểm duyệt đánh giá |
| `FINANCE_OFFICER` | Nhân viên tài chính | `ACT-FIN` | Xử lý rút tiền, Cấu hình hoa hồng, Sổ cái tài chính, Voucher sàn |

Quy tắc kèm theo:
- **ADMIN** (`ACT-ADM`) làm được tất cả.
- **Chỉ Admin** có các quyền sau, nhưng được **cấp thêm** cho từng nhân viên (trừ `MANAGE_RBAC`):
  - Thông báo sàn
  - Nhật ký hệ thống
  - Khóa / mở khóa người dùng (ADM-03)
- **Phân quyền nhân viên (`MANAGE_RBAC`) chỉ Admin có**, không cấp cho ai khác.
- Trong TLCN, **KYC do `MODERATOR` làm.** DATN tách riêng `KYC_OFFICER`; lên KLTN chỉ cần thêm lại chức danh này, không đổi tên gì.
- **UML:**
  - `ACT-MOD`, `ACT-CS`, `ACT-FIN` và `ACT-ADM` đều **kế thừa (generalization)** `ACT-STF`.
  - Gắn tác nhân vào use case:

| Tác nhân | Use case |
| :--- | :--- |
| `ACT-MOD` | VND-04, CAT-13, CAT-14 (danh mục), CAT-17, SOC-05 |
| `ACT-CS` | SUP-03, SOC-05 |
| `ACT-FIN` | ADM-05, PAY-06 (sổ cái), PRO-02, CAT-14 (hoa hồng) |
| `ACT-ADM` | ADM-01, ADM-02, ADM-03, ADM-07, NOT-05 + mọi UC trên |

**3. Bộ quyền TLCN** — dùng mã HOA giống DATN; mã nào DATN chưa có thì **thêm vào cả DATN**:

| Mã quyền | Có trong DATN? | Mã chấm trên giao diện | Màn hình | MODERATOR | CS_SPECIALIST | FINANCE_OFFICER | ADMIN |
| :--- | :-: | :--- | :--- | :-: | :-: | :-: | :-: |
| `VIEW_DASHBOARD` | **mới** | `dashboard.view` | Tổng quan | ✔ | ✔ | ✔ | ✔ |
| `REVIEW_KYC` | ✅ | `kyc.review` | Duyệt KYC | ✔ | | | ✔ |
| `MANAGE_CATALOG` | ✅ | `catalog.manage` | Danh mục toàn sàn, Duyệt sản phẩm | ✔ | | | ✔ |
| `MODERATE_REVIEW` | **mới** | `review.moderate` | Kiểm duyệt đánh giá | ✔ | ✔ | | ✔ |
| `HANDLE_DISPUTE` | ✅ | `refund.arbitrate` | Trả hàng & Hoàn tiền | | ✔ | | ✔ |
| `PROCESS_PAYOUT` | ✅ | `payout.process` | Xử lý rút tiền | | | ✔ | ✔ |
| `UPDATE_COMMISSION` | ✅ | `commission.update` | Cấu hình hoa hồng | | | ✔ | ✔ |
| `VIEW_FINANCE` | **mới** | `finance.view` | Sổ cái tài chính | | | ✔ | ✔ |
| `MANAGE_VOUCHER` | ✅ | `voucher.manage` | Voucher sàn | | | ✔ | ✔ |
| `MANAGE_PLATFORM_NOTIFICATION` | **mới** | `notif.manage` | Thông báo sàn | | | | ✔ (cấp thêm được) |
| `VIEW_AUDIT_LOG` | ✅ | `audit.view` | Nhật ký hệ thống | | | | ✔ (cấp thêm được) |
| `MANAGE_USER` | **mới** | — (chưa có màn hình) | Khóa / mở khóa khách & shop (ADM-03) | | | | ✔ (cấp thêm được) |
| `MANAGE_RBAC` | ✅ | `rbac.manage` | Phân quyền nhân viên | | | | ✔ (**chỉ Admin**) |

Tổng: TLCN dùng **13 mã**, trong đó **5 mã mới** (`VIEW_DASHBOARD`, `MODERATE_REVIEW`, `VIEW_FINANCE`, `MANAGE_PLATFORM_NOTIFICATION`, `MANAGE_USER`) → DATN tăng từ 18 lên **23 quyền**.

Ghi chú:
- **Quyền DATN không dùng trong TLCN** (giữ cho KLTN): `SUPPORT_CUSTOMER`, `VIEW_SYSTEM_LOG`, `MANAGE_FEATURE_FLAGS`, `RUN_EXPERIMENTS`, `MANAGE_B2B_CREDIT`, `MANAGE_DISPATCH`, `MANAGE_SHIPPERS`, `REVIEW_FRAUD`, `HANDLE_DSAR`, `HANDLE_TAKEDOWN`.
- **Giao diện phải dùng một kiểu mã.** Khuyến nghị đổi nav prototype / client sang mã HOA như bảng trên. Nếu giữ mã dạng chấm thì ghi bảng ánh xạ trong ENTITY.
- **Tài khoản Admin:**
  - Tạo bằng seed / biến môi trường lúc cài đặt; không có màn hình tạo Admin.
  - Admin không tự khóa và không tự hạ quyền của chính mình.
  - Hệ thống luôn còn ít nhất 1 Admin đang hoạt động.
- **Nhân viên mới:**
  - Admin mời qua email.
  - Nhân viên kích hoạt bằng OTP email và tự đặt mật khẩu.
  - Trạng thái: "Chờ kích hoạt" → "Hoạt động"; Admin có thể chuyển sang "Tạm khoá" hoặc xóa.

### 10.3. Việc cần làm — TLCN

> ✅ **Đã thực hiện 2026-10-29** — kết quả ở [mục 14](#muc-14).

| File | Việc |
| :--- | :--- |
| `TLCN/diagrams/ClassDiagram/ENTITY.md` | • `UserRole` giữ {CUSTOMER, VENDOR, STAFF, ADMIN}<br>• Thêm `enum StaffTitle { MODERATOR, CS_SPECIALIST, FINANCE_OFFICER }` (chú thích: KLTN thêm 7 chức danh còn lại)<br>• Thêm `enum StaffPermissionEnum` gồm 13 mã ở 10.2<br>• `AdminProfile` thêm `staffTitle` (NULL với ADMIN), `invitedBy`, `invitedAt`, `activatedAt`<br>• Thêm entity **`StaffPermission`** (userId, permission, grantedBy, grantedAt; UNIQUE(userId, permission)), chép cấu trúc từ DATN mục 1.7<br>• Thêm quan hệ vào Phụ lục B, enum vào Phụ lục A |
| `TLCN/diagrams/ClassDiagram/java-code/Diagram01_IdentityAndRbac.java` + XMI class | Đồng bộ với ENTITY |
| `TLCN/diagrams/UseCaseDiagram/Usecase.md` | • **Phần I:** thêm `ACT-MOD`, `ACT-CS`, `ACT-FIN` (con của `ACT-STF`); sửa mô tả `ACT-STF` (bỏ "không chia chức danh trong MVP") và `ACT-ADM` ("vai trò cao nhất, không có Super Admin")<br>• Đưa vào **ADM-01** (mời nhân viên, chọn chức danh, kích hoạt bằng OTP) và **ADM-02** (sửa quyền chi tiết, thu hồi, khóa / mở, xóa nhân viên); giữ **ADM-03**<br>• Đổi tác nhân của các UC theo bảng ở 10.2<br>• Thêm quy tắc chặn nav theo quyền (màn "Bạn không có quyền truy cập… Liên hệ Admin") |
| `UML_UseCase_PlantUML.md`, `Genie_UseCase_UML25.xmi`, `Quản Lý UseCase.xlsx` | Thêm 3 tác nhân + generalization; cập nhật association.<br>Số tác nhân TLCN: **11 → 15** (thêm `ACT-GEM`, `ACT-MOD`, `ACT-CS`, `ACT-FIN`) |
| `TLCN/document/Document_Vi.md` | Bảng vai trò và ma trận phân quyền ở 10.2 |
| `TLCN/kien-thuc/implement.md` | Spring Security `@PreAuthorize("hasAuthority('REVIEW_KYC')")`, seed Admin, sprint cho ADM-01/02 |
| Giao diện (khi có mã nguồn) | • Bỏ "Super Admin" / `SUPER_ADMIN`<br>• Dropdown "Xem với vai trò": Admin, Nhân viên kiểm duyệt, Nhân viên CSKH, Nhân viên tài chính<br>• Form mời nhân viên chỉ có 3 chức danh + "Tùy chỉnh"<br>• Thêm mục nav "Người dùng" cho ADM-03 |

### 10.4. Việc cần làm — DATN (KLTN)

> ✅ **Đã thực hiện 2026-10-29** — kết quả ở [mục 14](#muc-14).

| File | Việc |
| :--- | :--- |
| `DATN/diagrams/ClassDiagram/ENTITY.md` + Java + XMI class | • `StaffPermissionEnum` thêm 5 mã mới → **23 quyền**<br>• Cập nhật bộ quyền mặc định của `StaffTitle`:<br>&nbsp;&nbsp;– MODERATOR thêm `MODERATE_REVIEW`<br>&nbsp;&nbsp;– CS_SPECIALIST thêm `MODERATE_REVIEW`<br>&nbsp;&nbsp;– FINANCE_OFFICER thêm `VIEW_FINANCE`, `UPDATE_COMMISSION`, `VIEW_DASHBOARD`<br>&nbsp;&nbsp;– MARKETING_OFFICER thêm `MANAGE_PLATFORM_NOTIFICATION` |
| `DATN/diagrams/UseCaseDiagram/Usecase.md` (+ PlantUML / XMI / Excel nếu đổi tác nhân) | • ADM-02: danh sách quyền 18 → 23<br>• ADM-06 / PAY-06 dùng `VIEW_FINANCE`<br>• NOT-05 dùng `MANAGE_PLATFORM_NOTIFICATION`<br>• SOC-05 dùng `MODERATE_REVIEW`<br>• ADM-03 dùng `MANAGE_USER` |
| `DATN/implement.md` | Sửa 2 câu SQL RLS còn `'super_admin'` (dòng ~10547, ~10559) |

---

## 11. HAI DANH SÁCH VIỆC CHO NGÀY MAI

> ✅ **Đã thực hiện 2026-10-29** — kết quả ở [mục 14](#muc-14).

> **Thứ tự:** làm **TLCN trước** (đồ án đang làm), xong mới sang **DATN**.
> - Hai bộ phải dùng chung tên mã: DATN là **tập cha** của TLCN.
> - Trước khi sửa, sao lưu `TLCN/` và `DATN/` vào một thư mục backup có ngày.
> - Ở gốc `fix-doc/` giờ chỉ còn `TLCN/`, `DATN/`, `client/`, `prototype/`.

### 11.1. TLCN — `TLCN/` (tiểu luận chuyên ngành)

Mục tiêu: phủ **toàn bộ** client + prototype → khoảng **88 UC** (gồm PRO-04 ở mục 13), **58–60 entity**, **15 tác nhân**.

| # | Việc | Tham chiếu |
| :-: | :--- | :--- |
| T1 | Sao lưu `TLCN/` | 9.2 |
| T2 | **ENTITY → Java → XMI class** (+ bổ sung ENTITY ở 12.3). Thêm entity / trường / enum:<br>• Điểm (`UserCoin`, `CoinTransaction`)<br>• `ShopFollow`<br>• Thông báo sàn (`PlatformNotification`, `UserPlatformNotificationRead`)<br>• RBAC: `StaffPermission` + enum<br>• `FinancialLedger`<br>• `ReviewReport`, `ReviewHelpfulVote`<br>• `ProductModerationResult`<br>• `CategoryProposal`, `ProductDocument`, `Brand`<br>• `FlashSaleCampaign`, `FlashSaleItem`<br>• `Banner`, `ProductViewStat`, `CategoryAttribute`<br>• `UserSocialAccount`, `VendorCarrier`<br>• Trường chỉ số shop / freeship / vacation / auto-reply trên `VendorProfile`<br>• Trường pre-order, tình trạng, xuất xứ trên `Product` | 4.1, 4.3, 5, 10.3 |
| T3 | **Usecase.md Phần I:** 15 tác nhân (thêm `ACT-GEM`, `ACT-MOD`, `ACT-CS`, `ACT-FIN`) | 0.3 Q2, 10.2 |
| T4 | **Usecase.md Phần III** (+ CAT-04 rút gọn, các sửa ở 12.3, PRO-04 / PRO-03 / CAT-14 ở 13.3):<br>• Thêm 24 UC đưa lại (4.1) + 5 UC mới (N1–N4, N15)<br>• Sửa UC theo 4.2, 4.3, N5–N17, 10.2 | 4, 5, 10 |
| T5 | Usecase.md Phần II (ma trận) + Phần IV (thống kê) + CHANGELOG | 9.2 quy tắc 7 |
| T6 | PlantUML → XMI use case (boundary có `seqno` lớn nhất) → Excel | 9.2 quy tắc 8 |
| T7 | Cập nhật:<br>• Document_Vi<br>• `kien-thuc/implement.md` (sprint)<br>• chi phí Gemini<br>• Kien_Thuc<br>• BAO_CAO_PHAM_VI_MVP | 9.3 |
| T8 | Kiểm tra chéo 5 nơi + tham chiếu + cú pháp PlantUML + số liệu | 9.4 bước 7 |
| T9 | Cập nhật báo cáo này: đánh dấu ✅ các mục đã xong | — |

### 11.2. DATN — `DATN/` (đồ án tốt nghiệp, bản hoàn chỉnh cuối cùng)

Mục tiêu: mọi thứ TLCN có thì DATN cũng có (cùng mã), cộng toàn bộ 200 UC hiện tại.

| # | Việc | Tham chiếu |
| :-: | :--- | :--- |
| D1 | Sao lưu `DATN/` | — |
| D2 | **Thêm 5 UC mới vào đủ 5 nơi:**<br>• **VND-20** ví & sổ giao dịch người bán<br>• **VND-21** thông tin gian hàng<br>• **LOG-16** shop xác nhận / từ chối đơn<br>• **CAT-17** đề xuất danh mục<br>• **CAT-18** chia sẻ deeplink | 5 (N1–N4, N15) |
| D3 | **Mở rộng UC hiện có** (N5–N14, N16, N17 + F1, F4, F8–F11, F13–F17 ở mục 12.2):<br>• giấy tờ chứng thực SP, thương hiệu<br>• freeship của shop, pre-order<br>• đăng nhập OTP email<br>• mua lại, hủy nhóm đơn, thanh toán lại<br>• lấy địa chỉ từ GPS<br>• phân phối lại việc kiểm duyệt<br>• chỉ số trang shop, `ReviewHelpfulVote` | 5 |
| D4 | **ENTITY + Java + XMI:**<br>• `CategoryProposal`, `ProductDocument`, `Brand`, `ReviewHelpfulVote`<br>• `ProductModerationResult` (nếu dùng chung cho Gemini)<br>• Trường mới trên `VendorProfile` / `Product`<br>• 5 mã quyền mới | 5, 10.4 |
| D5 | CAT-12: ghi chú "TLCN dùng Gemini API" — giữ AI Worker hay chuyển sang Gemini thì **hỏi người dùng** (câu 11) | 4.3a |
| D6 | **Phân quyền:**<br>• 23 quyền<br>• bộ quyền mặc định của `StaffTitle`<br>• sửa `'super_admin'` trong `implement.md` | 10.4 |
| D7 | Document_Vi, implement.md, kien-thuc: nhắc tới các chức năng mới | 9.3 |
| D8 | Kiểm tra chéo 5 nơi; đối chiếu **mọi mã của TLCN đều có trong DATN** | — |

### 11.3. Câu hỏi còn treo (bổ sung vào 9.5)

| # | Câu hỏi | Mặc định |
| :-: | :--- | :--- |
| 10 | KYC trong TLCN do `MODERATOR` làm (gộp vào), đúng không? | Đúng |
| 11 | DATN CAT-12 giữ AI Worker hay đổi sang Gemini? | Giữ AI Worker, thêm ghi chú TLCN dùng Gemini |
| 12 | Giao diện dùng mã quyền dạng HOA hay dạng chấm? | Đổi sang mã HOA |
| 13 | Ba chức danh `MODERATOR` / `CS_SPECIALIST` / `FINANCE_OFFICER` đã đủ chưa, có muốn thêm `KYC_OFFICER` hoặc `MARKETING_OFFICER` không? | Giữ 3 chức danh |

---

## 12. QUÉT LẦN CUỐI (2026-10-29) — các chi tiết bổ sung

### 12.1. Phạm vi lượt quét này

**Prototype — đọc thêm phần trước chưa đọc:**

| File | Nội dung | Kết luận |
| :--- | :--- | :--- |
| `prototype/README.md` | 7 hành trình PRD | Khớp các mục đã có. Riêng **đổi trả 15 ngày** và **giải ngân escrow sau 15 ngày** → bổ sung vào X4 / X5 |
| `prototype/index.html`, `genie/shell.jsx` | Launcher 4 bề mặt + bảng 7 hành trình | Công cụ trình diễn, **không phải chức năng sản phẩm** → không cần UC |
| `genie/board.jsx` | Danh sách khoảng 50 màn hình | Dùng làm checklist: mọi màn hình trong board đều đã có trong mục 2 |
| `genie/tweaks-panel.jsx`, `design-canvas.jsx` | Bộ chỉnh màu / bo góc của design system | Công cụ thiết kế, **không phải chức năng sản phẩm** → không cần UC |

**Client:** trích **toàn bộ chuỗi giao diện** của 4 bundle:

| Bundle | Số chuỗi |
| :--- | :---: |
| `StorefrontApp` | 867 |
| `VendorApp` | 622 |
| `AdminApp` | 620 |
| `LazadeeAuth` | 98 |

Đã soát cả các mã sự kiện realtime (`PLATFORM_NOTIFICATION`, `CUSTOMER_NOTIFICATION`, `CHAT_MESSAGE`, `CHAT_READ_RECEIPT`, `VENDOR_PRODUCT_UPDATE`, `METRICS_SYNC`, `NOTIFICATION_SYNC`…).

**Đối chiếu:** mỗi chi tiết mới được tra trong:
- `TLCN/diagrams/UseCaseDiagram/Usecase.md` + `TLCN/diagrams/ClassDiagram/ENTITY.md`
- `DATN/diagrams/UseCaseDiagram/Usecase.md` + `DATN/diagrams/ClassDiagram/ENTITY.md`

### 12.2. Chức năng mới phát hiện (chưa có ở các mục 2–11)

| # | Chức năng trên giao diện | Nguồn | DATN | TLCN | Việc cần làm |
| :-: | :--- | :-: | :-: | :-: | :--- |
| F1 | **Cảnh báo "Đăng nhập mới trên thiết bị lạ"** (thông báo hệ thống gửi cho khách) | C | ❌ (SEC-03 chỉ chấm điểm rủi ro, không gửi thông báo) | ❌ | **Cả hai:** mở rộng AUTH-02 (thiết bị / IP mới → thông báo in-app + email) và thêm loại vào `NotificationType` |
| F2 | **Huy hiệu "MALL"**, bộ lọc **"Shop Chính Hãng"**, **"Chính hãng"** | P, C | ✅ (VND-09 gói `MALL_VIP`) | ❌ | **TLCN:** thêm `VendorProfile.isMall` (Admin gắn khi duyệt KYC doanh nghiệp / chính hãng) + bộ lọc ở CAT-05. KLTN thay bằng VND-09 |
| F3 | **Lỗi sót trong TLCN:** CAT-14 (Usecase.md dòng ~1004) nhắc "Shop dùng gói PRO/MALL_VIP … `VendorSubscriptionPlan.effectiveCommission()`", nhưng TLCN **không có** `VendorSubscriptionPlan` | — | — | ❌ lỗi | **TLCN:** xóa câu này (hoặc thay bằng `isMall` nếu muốn Mall có hoa hồng riêng) |
| F4 | **Huy hiệu "Shop Yêu Thích"** (đánh giá trung bình ≥ 4.5★, nhắc trong trang chính sách) | C | ❌ | ❌ | **Cả hai:** quy tắc tính từ `ratingAvg` (N16) + điều kiện tối thiểu (số đơn, tỉ lệ phản hồi). Hiển thị trên trang Shop / thẻ SP |
| F5 | **Điểm — luật kiếm & tiêu:** hoàn Điểm theo đơn ("Hoàn điểm đơn ORD-…"), **voucher hoàn Điểm** ("Hoàn 50.000₫ Điểm"), **tặng Điểm khi đánh giá có ảnh**, Điểm **hết hạn sau 12 tháng**, 1 Điểm = 1₫, dùng tối đa 50% tiền hàng | P, C | ✅ PRO-08/09/10, SOC-03 tặng Điểm, hết hạn | ❌ | **TLCN:** khi đưa PRO-08/09/10 vào (mục 4.1), giữ đủ 5 nguồn Điểm: điểm danh, hoàn Điểm đơn, voucher hoàn Điểm, đánh giá có ảnh, hoàn Điểm khi hủy đơn. Thêm `VoucherType` hoàn Điểm nếu DATN có |
| F6 | **Từ khóa gợi ý / phổ biến** dưới ô tìm kiếm (Smartphone, Áo thun, Serum, Tai nghe, Giày sneaker) | P, C | ✅ CAT-04 | ❌ | **TLCN:** đưa **CAT-04 rút gọn** (không Elasticsearch: lấy top từ khóa từ bảng đếm lượt tìm theo ngày; gợi ý theo tiền tố bằng `pg_trgm`) → thêm vào mục 4.1 |
| F7 | Nhãn **"Sắp hết"**, "Còn N sản phẩm", "Hết hàng" | P, C | ✅ | ❌ | **TLCN:** CAT-06 thêm quy tắc ngưỡng (ví dụ ≤ 5 = "Sắp hết") |
| F8 | **Lời nhắn cho Shop** khi checkout (mỗi shop một ô) | P, C | ❌ | ❌ | **Cả hai:** ORD-07 thêm bước nhập lời nhắn; `Order.buyerNote` (≤ 500 ký tự); hiển thị ở LOG-01 cho shop |
| F9 | **Đẩy realtime** kết quả duyệt SP cho shop (`VENDOR_PRODUCT_UPDATE`); **đồng bộ badge / thông báo giữa nhiều tab** (`METRICS_SYNC`, `NOTIFICATION_SYNC`, `READ_ONE`, `READ_ALL`) | C | ❌ | ❌ | **Cả hai:** NOT-01 liệt kê đủ loại sự kiện WebSocket; CAT-12/13 gửi sự kiện khi duyệt / từ chối |
| F10 | **Ghi log truy cập bị từ chối** (Nhật ký hiện "N lần truy cập bị từ chối", cột Kết quả = `DENIED`) | P, C | ❌ | ❌ | **Cả hai:** bước ghi nhật ký kiểm toán (H-AUDIT) ghi cả lần bị chặn quyền (`ACCESS_DENIED`); ADM-07 có bộ lọc theo kết quả |
| F11 | Mô tả SP **tối đa 10.000 ký tự**, chèn **ảnh** và **liên kết** trong mô tả (rich-text) | C | ❌ | ❌ | **Cả hai:** CAT-09/10 thêm quy tắc; ảnh mô tả upload qua Silo; lọc HTML (chống XSS) |
| F12 | **Ảnh theo thuộc tính phân loại** ("Thuộc tính có chứa ảnh": ví dụ mỗi màu một ảnh) | C | ⚠️ (entity có `variantImageAttribute`, UC không nhắc) | ⚠️ (như DATN) | **Cả hai:** CAT-09 thêm bước chọn thuộc tính có ảnh |
| F13 | **Mini-cart** ở header ("Sản phẩm mới thêm", "Còn N sản phẩm trong giỏ") + toast "Đã thêm vào giỏ" | C | ❌ | ❌ | **Cả hai:** ORD-01 thêm luồng "Thêm vào giỏ" từ trang SP, hiển thị mini-cart (không cần UC riêng) |
| F14 | **Trang biên nhận sau thanh toán:** mã giao dịch, mã phiên, đơn tách theo shop, "Cung cấp bởi", tổng tạm tính / giảm giá | C | ⚠️ (WAL-08 là hóa đơn VAT điện tử, khác biên nhận) | ⚠️ | **Cả hai:** PAY-03 / ORD-07 thêm màn hình kết quả thanh toán (thành công / thất bại) |
| F15 | **Câu hỏi gợi ý nhanh** trong khung chat ("Sản phẩm còn hàng không?", "Khi nào giao tới?"…) | P | ❌ | ❌ | **Cả hai:** SOC-01 thêm danh sách câu hỏi nhanh (cố định, hoặc shop cấu hình cùng VND-06) |
| F16 | Admin Tổng quan: **"Vendor tiềm năng bứt phá"** (xếp hạng tăng trưởng, lọc ngành hàng, phân trang) | C | ❌ | ⚠️ (chỉ top 10 shop) | **Cả hai:** ADM-06 thêm bảng tăng trưởng (so sánh kỳ trước) |
| F17 | Khuyến mãi shop: chỉ số **"Lượt dùng tháng này"**, **"Doanh thu từ KM"** | P, C | ❌ | ❌ | **Cả hai:** PRO-01 thêm thống kê (tổng từ `OrderDiscount`) |
| F18 | Thống kê **Thông báo sàn** (`notifications/stats`: số đã gửi, đã đọc) | C | ✅ NOT-05 | ❌ | Theo NOT-05 khi đưa vào TLCN |
| F19 | **"Tải ứng dụng"** ở header | P, C | ✅ (PWA) | ⚠️ (chỉ nhắc) | **TLCN:** yêu cầu phi chức năng: PWA cài được (manifest + service worker tối thiểu); link "Tải ứng dụng" mở hướng dẫn cài PWA |
| F20 | **"Hỗ trợ" / "Trung tâm trợ giúp"** | P, C | ✅ SUP-05 (ticket) | ❌ | **TLCN:** chỉ là trang nội dung tĩnh (giao diện chưa có màn hình ticket) → ghi ở Document_Vi. KLTN làm SUP-05 |
| F21 | **Liên kết tài khoản** Google với tài khoản email đang có ("Liên kết" trong Hồ sơ) | C | ⚠️ AUTH-03 | ❌ | **TLCN:** gộp vào AUTH-03 khi đưa vào |
| F22 | Duyệt SP: tab **Chờ duyệt / Đang bán / Bị từ chối**; trạng thái **"CHỜ DUYỆT LẠI"** sau khi shop sửa SP đang bán | C | ⚠️ | ⚠️ | **Cả hai:** CAT-10 ghi rõ sửa trường nào thì phải duyệt lại (tên, ảnh, danh mục, giấy tờ) và trường nào không (giá, tồn) |
| F23 | Giỏ hàng: **chọn tất cả**, **xóa nhiều món**, **cảnh báo vượt tồn** ("Sản phẩm này chỉ còn N"), cảnh báo "Giá đã tăng / giảm so với lúc thêm" | P, C | ✅ ORD-01 | ✅ ORD-01 | Đã có — kiểm lại luồng "xóa nhiều món" |
| F24 | Thông báo của Seller: **panel + modal "Chi tiết thông báo"** | C | ✅ NOT-01 | ✅ NOT-01 | Đã có |

### 12.3. Bổ sung vào các mục trước

| Mục | Bổ sung |
| :--- | :--- |
| **4.1** (đưa lại từ DATN) | Thêm **CAT-04 rút gọn** (F6). Thêm **cờ `isMall`** thay cho VND-09 (F2) |
| **4.2** (sửa UC TLCN) | • AUTH-02: cảnh báo thiết bị lạ (F1)<br>• CAT-05: bộ lọc Mall / Chính hãng (F2)<br>• CAT-06: nhãn sắp hết (F7)<br>• CAT-09/10: 10.000 ký tự, ảnh trong mô tả, ảnh theo thuộc tính, duyệt lại sau khi sửa (F11, F12, F22)<br>• CAT-14: xóa câu `VendorSubscriptionPlan` (F3)<br>• ORD-01: mini-cart (F13)<br>• ORD-07: lời nhắn cho shop (F8)<br>• PAY-03: trang biên nhận (F14)<br>• NOT-01: sự kiện realtime (F9)<br>• SOC-01: câu hỏi nhanh (F15)<br>• PRO-01: thống kê KM (F17)<br>• ADM-06: vendor tăng trưởng (F16)<br>• Nhật ký kiểm toán: `ACCESS_DENIED` (F10) |
| **5** (cả hai bản) | F1, F4, F8, F9, F10, F11, F13, F14, F15, F16, F17 — thêm vào DATN cùng lượt D3 |
| **6** (mâu thuẫn) | **X4 / X5 mở rộng:** README prototype ghi **đổi trả 15 ngày** và **giải ngân escrow sau hạn đổi trả 15 ngày**; TLCN / DATN ghi 7 ngày. Mặc định giữ **7 ngày** (9.5 câu 1) |
| **ENTITY** | • `Order.buyerNote`<br>• `VendorProfile.isMall`<br>• `NotificationType` thêm `NEW_DEVICE_LOGIN`, `PRODUCT_APPROVED`, `PRODUCT_REJECTED`<br>• `SystemLog` hỗ trợ kết quả `DENIED`<br>• Bảng đếm từ khóa tìm kiếm cho CAT-04 (`SearchKeywordStat`: keyword, date, count) |

### 12.4. Kết luận lượt quét cuối

- **Không còn màn hình nào bị bỏ sót.** Mọi màn hình của prototype (board khoảng 50 màn hình) và client (4 bundle, 14 mục nav Admin, 11 mục nav Seller kể cả mục ẩn, trang `/staff` giữ chỗ) đều đã có dòng tương ứng ở mục 2 hoặc 12.2.
- **24 chi tiết mới (F1–F24):**

| Loại | Số mục | Danh sách |
| :--- | :-: | :--- |
| Cả hai bản đều thiếu | **11** | F1, F4, F8, F9, F10, F11, F13, F14, F15, F16, F17 |
| Chỉ TLCN thiếu (DATN đã có) | **7** | F2, F5, F6, F7, F18, F20, F21 |
| Lỗi sót trong TLCN | **1** | F3 |
| Cả hai có một phần | **3** | F12, F19, F22 |
| Đã đủ | **2** | F23, F24 |

- **Ước tính cập nhật cho TLCN:** khoảng **87 UC** (thêm CAT-04), **khoảng 58–60 entity** (thêm `SearchKeywordStat` và các trường mới), **15 tác nhân**.
- Khi bắt đầu sửa: làm theo **mục 11**, và tra thêm **mục 12.3** ở các bước T2 (ENTITY), T4 (UC TLCN), D3 (mở rộng UC DATN).

---

## 13. VOUCHER XẾP CHỒNG (STACKING) & HOA HỒNG — hiện trạng TLCN (kiểm 2026-10-29)

### 13.1. Giao diện yêu cầu gì

| Nguồn | Yêu cầu |
| :--- | :--- |
| Prototype README (hành trình 2, `FR-ORD-003`) | **Xếp chồng 3 lớp** trong 1 lần thanh toán:<br>• 1 voucher Sàn<br>• 1 voucher Shop (cho **mỗi** shop)<br>• 1 mã Freeship<br>Giảm giá **phân bổ theo từng dòng SP** để hoàn tiền từng món cho đúng |
| Checkout prototype / client | Ô "Genie Voucher — **Tối đa 1 mỗi loại**" (Sàn / Freeship / Shop) + "Chọn Voucher của Shop" theo từng shop + **Điểm Genie (≤ 50%)** + dòng "Voucher Sàn phân bổ" theo từng shop |
| Form voucher (shop + sàn) | • Loại giảm: %, tiền, **Freeship**<br>• Phạm vi: toàn shop / toàn sàn, **danh mục cụ thể**, **SP cụ thể**, **loại trừ danh mục / SP**<br>• Nhà tài trợ: Genie / **Co-sponsor với Shop**<br>• Ngân sách tự tắt<br>• Giới hạn / người |
| Admin hoa hồng | Hoa hồng mặc định toàn sàn + theo ngành hàng (lưu hàng loạt) |

### 13.2. TLCN hiện có gì

| Hạng mục | TLCN hiện tại | Đánh giá |
| :--- | :--- | :--- |
| Loại voucher | `VoucherType { SHOP, PLATFORM }` — **chỉ 2 tầng** | ⚠️ **Thiếu tầng Freeship.** Enum ghi chú "mở rộng: FREESHIP, PAYMENT" |
| Kiểu giảm | `DiscountType { PERCENTAGE, FIXED_AMOUNT }` | ⚠️ Thiếu `FREESHIP` |
| Phạm vi | `VoucherScope { ALL }` | ❌ **Thiếu `CATEGORY`, `PRODUCT` và loại trừ** (giao diện đã có) |
| Nhà tài trợ | `FundingParty { PLATFORM, VENDOR }` | ⚠️ Thiếu **co-sponsor** (sàn và shop cùng chịu, cần tỉ lệ chia) |
| Ngân sách, giới hạn / người, ưu tiên, khóa lạc quan | `budget`, `totalDiscountUsed`, `usagePerUser`, `priority`, `version` | ✅ Đủ |
| **Thuật toán xếp chồng** | **Không có UC riêng.** PRO-03 có tên "Lưu voucher & **Áp voucher** khi thanh toán (2 tầng)" nhưng luồng chính **chỉ có bước lưu mã**. Bước áp mã nằm rải rác ở ORD-04 (phân bổ) và ORD-07 (ghi `order_discounts`, `voucher_usages`) | ❌ **Chưa đặc tả quy tắc chồng:** mỗi loại tối đa 1, thứ tự áp, đơn tối thiểu tính trên số tiền nào, loại trừ, voucher hết lượt giữa chừng, tự gợi ý mã tốt nhất |
| Phân bổ theo dòng hàng | `OrderItem.shopDiscount`, `platformDiscount`, `coinDiscount`, `proratedDiscount`; `OrderDiscount.fundedBy` | ✅ Có. Lưu ý: có sẵn `coinDiscount` nhưng TLCN chưa có entity Điểm → sẽ khớp khi đưa PRO-08/09/10 vào |
| **Hoa hồng** | • Tỉ lệ: shop riêng → ngành hàng → mặc định 5% (`effectiveCommissionRate`)<br>• Chụp tỉ lệ vào `OrderItem.commissionRate` lúc đặt hàng<br>• Tính trên `grossAmount = lineTotal − shopDiscount` (giảm giá do **sàn** tài trợ **không** làm giảm hoa hồng / tiền shop nhận)<br>• `EscrowLedger.netAmount = gross − commission − shopShippingSubsidy` | ✅ **Đã đúng và đủ** |
| Ai sửa hoa hồng | CAT-14: "chỉ `ADMIN`, MVP không ủy quyền cho Staff" | ⚠️ **Lệch với mục 10:** `FINANCE_OFFICER` có quyền `UPDATE_COMMISSION` → sửa tiền điều kiện CAT-14 |
| Hoa hồng riêng từng shop | Có trường `vendor_profiles.commissionRate` | ⚠️ Giao diện **không có màn hình** đặt hoa hồng riêng cho shop. Chọn một: thêm ô này ở Admin (KYC / hồ sơ shop), hoặc ghi "chỉ đặt qua dữ liệu" |
| Hoa hồng theo gói Mall | CAT-14 nhắc `VendorSubscriptionPlan` (không tồn tại trong TLCN) | ❌ Lỗi sót (= F3) |
| Hiển thị hoa hồng phía shop | Ví shop ghi cứng "Phí sàn 5%" | ⚠️ (= X9) — lấy theo ngành của từng dòng |

### 13.3. Việc cần làm — TLCN

> ✅ **Đã thực hiện 2026-10-29** — kết quả ở [mục 14](#muc-14).

1. **Đưa PRO-04 vào TLCN dạng rút gọn, giữ mã của DATN:** "Xếp chồng voucher **3 tầng** + Điểm khi checkout".

| Mục | Nội dung |
| :--- | :--- |
| **Tầng 1 — Voucher Shop** | Mỗi shop tối đa 1 mã; tính trên tạm tính của shop đó |
| **Tầng 2 — Voucher Sàn** | Tối đa 1 mã; tính trên tổng sau tầng 1; phân bổ về từng shop rồi từng dòng theo tỉ lệ |
| **Tầng 3 — Freeship** | Tối đa 1 mã; trừ vào tổng phí ship GHN, phân bổ theo phí ship từng shop |
| **Sau cùng — Điểm** | ≤ 50% tiền hàng còn lại, 1 Điểm = 1₫ |
| **Kiểm tra mỗi mã** | còn hiệu lực, còn lượt toàn hệ thống / mỗi người, còn ngân sách, đạt đơn tối thiểu (tính **trước** khi trừ tầng hiện tại), đúng phạm vi (danh mục / SP), không thuộc danh sách loại trừ |
| **Tự gợi ý** | Chọn bộ mã giảm nhiều nhất trong Kho voucher; khách đổi tay được |
| **Ngoại lệ** | • Mã hết lượt / hết ngân sách giữa lúc đặt hàng → báo lỗi, tính lại<br>• Hủy đơn / hoàn tiền → trả lượt dùng và Điểm theo phần đã phân bổ |
| **Thứ tự** | Ghi rõ thứ tự này trong UC; thống nhất với `Voucher.priority` |

2. **Tách PRO-03 thành đúng nghĩa:** "Lưu voucher vào Kho + xem / xóa / lịch sử" (K6). Phần áp mã chuyển sang PRO-04.
3. **ENTITY:**
   - `VoucherType` thêm `FREESHIP`
   - `DiscountType` thêm `FREESHIP` (hoặc chỉ dùng `VoucherType` — **giữ đúng cách DATN đang làm**)
   - `VoucherScope` thêm `CATEGORY`, `PRODUCT` + bảng nối phạm vi và loại trừ — **chép tên bảng từ DATN** (`voucher_excluded_categories`, `voucher_excluded_products` và bảng áp dụng tương ứng)
   - `FundingParty` thêm `CO_SPONSOR` + trường tỉ lệ (ví dụ `Voucher.platformSharePercent`)
   - `OrderItem` / `OrderDiscount` thêm phần freeship (`shippingDiscount`) nếu chưa có
4. **CAT-14:**
   - Tiền điều kiện cho phép `ADMIN` hoặc `UPDATE_COMMISSION` (FINANCE_OFFICER)
   - Thêm luồng "hoa hồng mặc định toàn sàn" và "lưu hàng loạt"
   - Xóa câu `VendorSubscriptionPlan` (F3)
   - Quyết định có màn hình hoa hồng riêng cho shop không (**mặc định: thêm ô ở Admin, mục hồ sơ shop**)
5. **PRO-01 / PRO-02:** thêm loại Freeship, phạm vi danh mục / SP, loại trừ, co-sponsor, ngân sách tự tắt (đã liệt kê ở 4.2 — làm cùng lượt này).

### 13.4. Việc cần làm — DATN

> ✅ **Đã thực hiện 2026-10-29** — kết quả ở [mục 14](#muc-14).

- PRO-04 DATN (4 tầng) đã có. Chỉ cần kiểm:
  - Thứ tự tầng và quy tắc "đơn tối thiểu tính trước khi trừ" khớp với bản rút gọn của TLCN.
  - Có **co-sponsor** (giao diện dùng mã `CO_SPONSOR_SHOP`).
- Xóa "Ghi chú mã nguồn tham khảo" ở PRO-04 (DATN cũng xem như chưa có code).
- CAT-14 DATN: tiền điều kiện theo `UPDATE_COMMISSION` (đã có trong DATN — kiểm lại).

### 13.5. Cập nhật ước tính

TLCN thêm **PRO-04** → khoảng **88 UC**. Danh sách việc T2 / T4 ở mục 11 tra thêm mục **13.3**.

---

<a id="muc-14"></a>
## 14. KẾT QUẢ THỰC HIỆN (2026-10-29)

### 14.1. Con số cuối cùng

| | TLCN v1.0 (trước) | **TLCN v1.1** | DATN v2.14 (trước) | **DATN v2.16** |
|---|:---:|:---:|:---:|:---:|
| Use case | 57 | **86** | 200 | **206** |
| Phân hệ | 11 | 11 | 23 | 23 |
| Tác nhân | 11 | **16** | 44 | 44 |
| «include» / «extend» | 18 / 7 | **37 / 15** | 27 / 16 | **28 / 20** |
| Entity (bảng) | 38 | **64** | 162 | **169** (180 class) |
| Enum | 37 | **52** | 124 | **129** |
| Quan hệ khóa ngoại | 61 | **100** | 195 | **205** |
| Phương thức nghiệp vụ | 145 | **201** | 455 | **469** |
| Sơ đồ lớp | 7 | **10** | 19 | 19 |
| Sprint | 6 | **8** (16 tuần) | — | — |
| Mã quyền nhân viên | — | **13** (3 chức danh + Admin) | 18 | **23** |

### 14.2. Use case thêm vào TLCN (29)

| Mã | Tên | Nguồn | Sprint |
|---|---|---|:---:|
| `UC-AUTH-03` | Đăng nhập & Liên kết tài khoản Google / Apple (OAuth2 / OpenID Connect) *(tên cũ: Đăng nhập một lần qua Mạng xã hội (SSO OAuth2) — đổi 2026-11-16 theo giao diện)* | Lấy lại từ DATN | S7 |
| `UC-AUTH-10` | Yêu cầu ẩn danh hóa & Xóa dữ liệu cá nhân (Xóa tài khoản) | Lấy lại từ DATN | S7 |
| `UC-CAT-04` | Gợi ý từ khóa tìm kiếm & Xu hướng (Autocomplete & Trending) | Lấy lại từ DATN | S7 |
| `UC-CAT-07` | Xem gợi ý sản phẩm tương tự | Lấy lại từ DATN | S7 |
| `UC-CAT-12` | Kiểm duyệt sản phẩm tự động qua AI (Google Gemini API) | Lấy lại từ DATN | S7 |
| `UC-CAT-15` | Cấu hình bộ thuộc tính chuẩn theo ngành hàng (Category Attributes) | Lấy lại từ DATN | S7 |
| `UC-CAT-17` | Người bán đề xuất danh mục mới & Duyệt đề xuất | Mới (thêm cả DATN) | S7 |
| `UC-CAT-18` | Chia sẻ sản phẩm bằng liên kết (Deeplink) | Mới (thêm cả DATN) | S7 |
| `UC-PAY-06` | Ghi sổ cái tài chính kép bất biến (Financial Ledger) | Lấy lại từ DATN | S3 |
| `UC-VND-06` | Cấu hình tin nhắn trả lời tự động (Auto-reply) | Lấy lại từ DATN | S7 |
| `UC-VND-19` | Bật chế độ tạm nghỉ bán hàng của shop (Vacation Mode) | Lấy lại từ DATN | S7 |
| `UC-VND-20` | Xem ví & sổ giao dịch người bán (Seller Wallet & Escrow Ledger) | Mới (thêm cả DATN) | S7 |
| `UC-VND-21` | Cập nhật thông tin gian hàng & cài đặt vận hành | Mới (thêm cả DATN) | S7 |
| `UC-LOG-15` | Điều phối đa hãng vận chuyển (Dynamic Carrier Routing) | Lấy lại từ DATN | S7 |
| `UC-LOG-16` | Người bán xác nhận / từ chối đơn hàng mới | Mới (thêm cả DATN) | S4 |
| `UC-PRO-04` | Thuật toán Stacking Voucher 3 Tầng & Điểm tự động khi Checkout | Lấy lại từ DATN | S8 |
| `UC-PRO-05` | Tạo chiến dịch Flash Sale & Cấu hình khung giờ vàng | Lấy lại từ DATN | S8 |
| `UC-PRO-06` | Mua hàng Flash Sale chống bán vượt suất | Lấy lại từ DATN | S8 |
| `UC-PRO-08` | Điểm danh nhận Điểm hàng ngày & Duy trì chuỗi điểm danh (Streak) | Lấy lại từ DATN | S8 |
| `UC-PRO-09` | Tra cứu số dư Điểm Genie & Lịch sử biến động Điểm | Lấy lại từ DATN | S8 |
| `UC-PRO-10` | Sử dụng Điểm Genie để giảm trừ thanh toán đơn hàng | Lấy lại từ DATN | S8 |
| `UC-SOC-02` | Gửi thẻ sản phẩm, hình ảnh và trạng thái đơn hàng trong chat | Lấy lại từ DATN | S8 |
| `UC-SOC-05` | Báo cáo đánh giá vi phạm / spam & Kiểm duyệt đánh giá (Review Moderation & Report) | Lấy lại từ DATN | S8 |
| `UC-SOC-06` | Khách hàng theo dõi gian hàng (Shop Follow) | Lấy lại từ DATN | S8 |
| `UC-NOT-05` | Admin phát thông báo toàn sàn (Broadcast Platform Notification) | Lấy lại từ DATN | S8 |
| `UC-ADM-01` | Quản lý danh sách nhân viên nội bộ & Lời mời kích hoạt | Lấy lại từ DATN | S1 |
| `UC-ADM-02` | Phân cấp vai trò & Cấp phát / Thu hồi mã quyền chi tiết (RBAC) | Lấy lại từ DATN | S1 |
| `UC-ADM-07` | Tra cứu nhật ký kiểm toán hệ thống bất biến (System Audit Logs) | Lấy lại từ DATN | S7 |
| `UC-ADM-12` | Quản lý banner trang chủ & chiến dịch tĩnh (CMS Banner) | Lấy lại từ DATN | S8 |

DATN thêm 6 use case: `UC-CAT-16` (TLCN có từ v1.0), `UC-CAT-17`, `UC-CAT-18`, `UC-VND-20`, `UC-VND-21`, `UC-LOG-16` — cùng mã, cùng tên với TLCN.

### 14.3. Các quyết định ở mục 0.3, 10, 13 đã áp dụng

| Quyết định | Áp dụng ở |
|---|---|
| Kiểm duyệt sản phẩm bằng **Gemini API** | TLCN: tác nhân `ACT-GEM`, UC-CAT-12 (Gemini sàng lọc, Moderator duyệt cuối), entity `ProductModerationResult`, Document_Vi 2.4.8 + SD-07, implement 5.3, chi phí Phần 6B. DATN: dùng chung bảng `product_moderation_results` (AI Worker) |
| Trang sản phẩm: ♥ yêu thích, **Chia sẻ deeplink**, **Hữu ích** | UC-CAT-08, UC-CAT-18, UC-SOC-03; `ReviewHelpfulVote`, `ProductReview.helpfulCount`, `ProductViewStat` (nguồn `EXTERNAL_LINK`) |
| Trang shop: đánh giá shop, người theo dõi, số sản phẩm, tỉ lệ phản hồi | UC-CAT-16, UC-SOC-06; `VendorProfile.ratingAvg / ratingCount / followerCount / activeProductCount / chatResponseRate / avgResponseMinutes` |
| Thanh toán **VNPay + SePay (VietQR) + COD kết hợp Escrow** | UC-PAY-01/03/05, FR-PAY-03, Document_Vi 2.4.6, ADR-13 |
| Nhân viên: **chỉ Admin, không Super Admin**, 3 chức danh MODERATOR / CS_SPECIALIST / FINANCE_OFFICER | Tác nhân `ACT-MOD`, `ACT-CS`, `ACT-FIN`; UC-ADM-01/02; `StaffPermission`, `AdminProfile.staffTitle`; ma trận quyền Document_Vi 3.6; implement 4.5. DATN: 23 mã quyền, sửa RLS `super_admin` → `admin` |
| Voucher **xếp chồng 3 tầng + Điểm** | UC-PRO-04, UC-PRO-10; `VoucherCategory` / `VoucherProduct` / loại trừ, `Voucher.platformSharePercent`, `OrderItem.coinDiscount`; Document_Vi 2.4.9; implement Phần 10; báo cáo kinh doanh Phần 5B |
| Flash Sale, điểm danh, Điểm, đổi Điểm lấy voucher | UC-PRO-05/06/08/09/10; `FlashSaleCampaign`, `FlashSaleItem`, `UserCoin`, `CoinTransaction`, `DailyCheckin`; Điểm hết hạn **12 tháng** ở cả hai bản |
| Đa ngôn ngữ, đa hãng vận chuyển, đăng nhập Google / Apple | NFR-14; UC-LOG-15 + `VendorShippingConfig`; UC-AUTH-03 + `ACT-IDP` |
| Quản lý tiến độ ở Excel DATN | `DATN/…/Quản Lý UseCase.xlsx`: 86 mã TLCN = “Đã triển khai” → **cả dòng nền xám**; chọn lại “Chưa triển khai” → dòng trở về màu phân hệ (quy tắc định dạng có điều kiện ưu tiên 1). Excel TLCN có cùng quy tắc (mặc định “Chưa triển khai”) |

### 14.4. Điều chỉnh thiết kế trong lúc làm (khác với kế hoạch ở mục 4–13)

- **Freeship / hãng vận chuyển của shop:** dùng `VendorShippingConfig` có sẵn ở DATN thay vì thêm trường `freeShipThreshold`, `enabledCarriersJson` vào `VendorProfile`.
- **KYC quốc huy / vân tay:** ghi vào `KycResult.riskSignalsJson` — không thêm trường riêng (DATN v2.10 đã bỏ các trường này).
- **Điểm danh:** thêm entity `DailyCheckin` vào TLCN (UC-PRO-08 cần lưu chuỗi ngày).
- **Chuông thông báo Admin (bổ sung sau khi đối chiếu mã nguồn cũ `server/`):** thêm entity `AdminNotification` (bảng `admin_notifications`, lấy nguyên từ DATN mục 11.4, enum TLCN 5 giá trị) vào nhóm 10 / sơ đồ lớp 10; nối với UC-NOT-01 (luồng chính bước 9), UC-PAY-03, UC-VND-02, UC-ADM-04 → **64 entity**.
- **Đồng bộ quan hệ use case (2026-10-30):** TLCN thêm `UC-CAT-13` «extend» `UC-CAT-12` {Gemini không tự từ chối}, đổi `UC-PRO-10` sang «extend» `UC-PRO-04` (Điểm là bước cuối của thuật toán xếp chồng), bỏ dây Customer — `UC-ORD-06` (use case nội bộ, được `UC-ORD-04` «include»); DATN thêm `UC-CAT-04` «extend» `UC-CAT-02` và `UC-ORD-12` «include» `UC-PAY-07`. Thêm `UC-ADM-01` «include» `UC-ADM-02` và `UC-PAY-03` «include» `UC-PAY-06` cho TLCN. TLCN 37 «include» / 15 «extend», DATN 28 / 20 — mọi quan hệ giữa các use case có ở cả hai bản đã trùng nhau.
- **GAM-08 (đổi Điểm lấy quà):** không đưa vào TLCN; "đổi Điểm lấy voucher" gộp vào UC-PRO-09.
- **Thống kê truy cập:** không có UC ANA riêng ở TLCN — ghi lượt xem trong UC-CAT-06, xem trong UC-VND-08.
- **Gemini không tự đăng bán:** `APPROVE` vẫn vào hàng chờ Moderator; chỉ `REJECT` có `riskScore` ≥ 85 bị từ chối tự động.
- **Shop không xác nhận đơn quá 24 giờ:** cron **tự xác nhận** (không hủy) và ghi điểm "chậm xác nhận" (UC-LOG-16).
- **Tên UC TLCN** lấy theo DATN, bỏ phần công nghệ TLCN không dùng; ở DATN, Phần II / XMI / PlantUML / Excel giữ quy ước tên ngắn (tiêu đề đặc tả có thêm hậu tố tiếng Anh).

### 14.5. File đã sửa

| Bộ | File |
|---|---|
| TLCN | `diagrams/ClassDiagram/ENTITY.md`, `java-code/*.java` (10 file), `Genie_ClassDiagram_UML25.xmi`; `diagrams/UseCaseDiagram/Usecase.md`, `UML_UseCase_PlantUML.md`, `Genie_UseCase_UML25.xmi`, `Quản Lý UseCase.xlsx`; `document/Document_Vi.md`; `kien-thuc/implement.md`, `Kien_Thuc_Can_Nam.md`, `BAO_CAO_DU_TOAN_CHI_PHI_VA_GIAI_PHAP_AI_v1.0.md`, `BAO_CAO_KIEN_THUC_KINH_DOANH_KINH_TE_v1.0.md`, `BAO_CAO_HE_THONG_CHINH_SACH_HOP_DONG_PHAP_LY_v1.0.md`; `BAO_CAO_PHAM_VI_MVP.md`; báo cáo này |
| DATN | `diagrams/ClassDiagram/ENTITY.md` (v2.16), `java-code/*.java` (19 file), `Lazadee_ClassDiagram_UML25.xmi`; `diagrams/UseCaseDiagram/Usecase.md`, `UML_UseCase_PlantUML.md` (v1.5), `Lazadee_UseCase_UML25.xmi`, `Quản Lý UseCase.xlsx`; `document/Document_Vi.md`; `implement.md` (Phần 36, sửa RLS); `kien-thuc/BAO_CAO_KIEN_THUC_KINH_DOANH_KINH_TE_v1.0.md`, `Kien_Thuc_Can_Nam.md` (Điểm 12 tháng) |

### 14.6. Kiểm tra chéo đã chạy

- Mã use case khớp ở 5 nơi (Usecase.md tiêu đề + Phần II, PlantUML, XMI, Excel): TLCN 86/86, DATN 206/206; mọi mã TLCN có ở DATN; 86 dòng “Đã triển khai” ở Excel DATN đúng bằng tập mã TLCN.
- Tên use case khớp giữa Phần II, PlantUML Phần VI, XMI, Excel (hai bản).
- Mọi entity được Usecase.md tham chiếu đều có trong ENTITY.md; mọi `@Entity` có trong Java và XMI lớp; các XMI parse được; PlantUML không lỗi cú pháp tĩnh.
- Bản sao trước khi sửa và công cụ sinh lại: `fix-doc-backup-2026-10-29/` (thư mục `TLCN/`, `DATN/`, `tools/`).

---

<a id="muc-15"></a>
## 15. ĐỐI CHIẾU LẠI VỚI GIAO DIỆN GENIE (2026-11-06)

### 15.1. Phạm vi

Giao diện ở `genie/ui_kits/` (Storefront, Seller Center, Admin Console, Auth) sau đợt làm lại 02–06/11: đổi thương hiệu Genie, làm lại Storefront, banner nhiều vị trí, trang chiến dịch, liên kết Google, dashboard mới. Thư mục `client/` và `prototype/` cũ không còn trong repo.

### 15.2. Sai lệch đã sửa phía tài liệu (TLCN v1.2)

| # | Giao diện | Tài liệu trước khi sửa | Đã sửa ở |
|---|---|---|---|
| 1 | Thương hiệu **Genie** (logo, Voucher Genie, Genie Express, email `@genie.example`, VietQR "GENIE GIU TIEN") | Lazadee ở mọi nơi | Mọi file TLCN: Lazadee → Genie, domain `genie.vn`, package `com.genie.platform`, mã lỗi / khóa Jira `GNE-`, file `Genie_*_UML25.xmi` |
| 2 | Điểm thưởng là **Điểm Genie / Điểm** | "Lazadee Xu" / "Xu" | Mọi file; enum `COIN_COST`, cột `coinDiscount`, `coinDiscountAmount`, `coinUsedCount` |
| 3 | Mã đơn `TR-20260608-000141`, nội dung chuyển khoản `TR…`, `TRRF…`, `TRWD…` | `LZD-…`, `LZD{id}`, `LZDRF{id}`, `LZDWD{id}` | Usecase.md, Document_Vi.md, ENTITY.md, Java, XMI, kien-thuc |
| 4 | Nhãn **Chính hãng**, bộ lọc **Shop Chính Hãng** | Nhãn MALL, bộ lọc Lazadee Mall | Usecase.md (CAT-05, CAT-06, CAT-16, VND-04, SOC-06), ENTITY / GIAI_NGHIA / Java / XMI (`isMall`) |
| 5 | Banner 6 vị trí (thêm "Cạnh băng chuyền") | `BannerPosition` 5 giá trị | Enum `HOME_TOP_SIDE` ở ENTITY.md, GIAI_NGHIA, Java, XMI; UC-ADM-12; FR-ADM-07 |
| 6 | Liên kết đích chọn từ dữ liệu sàn: danh mục, sản phẩm, cửa hàng, Flash Sale, Gợi ý hôm nay, trang chiến dịch; popup xếp chồng | 3 loại đích, không mô tả cách hiển thị | UC-ADM-12 bước 2, 6–8 và ngoại lệ; `HomeBanner.targetUrl` |
| 7 | Trang chiến dịch (`Campaign.jsx`) | Không có | UC-ADM-12 bước 8 (trang tĩnh cấu hình ở frontend, không thêm entity), mục 1.6 |
| 8 | Mục "Liên kết đăng nhập": liên kết / gỡ liên kết Google, không liên kết khi email chưa xác minh hoặc tài khoản bị khóa | Chỉ có liên kết tự động | UC-AUTH-03, FR-AUTH-03 |
| 9 | Banner gắn quyền `MANAGE_VOUCHER` (Finance Officer thấy) | Ma trận 3.6 xếp banner vào `MANAGE_PLATFORM_NOTIFICATION` | Document_Vi.md 3.6 |
| 10 | Tối đa 8 ảnh / sản phẩm | FR-CAT-07 ghi 9 ảnh | Document_Vi.md, BAO_CAO_DU_TOAN |
| 11 | Sai OTP 5 lần | `GNE-AUTH-4221` ghi 3 lần | Document_Vi.md 3.7.1 |
| 12 | Chế độ nghỉ: sản phẩm vẫn hiện, khóa nút mua | UC-VND-19 mục đích ghi "tạm ẩn toàn bộ sản phẩm" | Usecase.md, Excel |
| 13 | Phân tích bán hàng: kết quả đơn, tiền thực về shop, khiếu nại thua, đơn sàn tự xác nhận | Chưa liệt kê | UC-VND-08, FR-VND-06 |
| 14 | Trang "Gợi ý hôm nay", khối "Gợi ý cho bạn" 6 hàng | Chỉ nói lưới ở trang chủ | UC-CAT-07 |
| 15 | — | Đường dẫn `client/`, `prototype/` | Document_Vi.md 1.1, 1.10; implement.md |

### 15.3. Mô hình lớp: bội số `[0..1]` lệch với ý nghĩa nullable

ENTITY.md, Java và XMI vốn khớp nhau theo annotation, nhưng 53 thuộc tính có bội số `[0..1]` chỉ vì thiếu `nullable = false` (hoặc thiếu chú thích NULLABLE), cộng 3 đầu association lệch với FK:

- **Thêm `nullable = false`:** `version` của 10 entity; `User.failedLoginAttempts`; `KycResult.vendorProfileId` (TLCN chỉ KYC người bán); `UserCoin.balanceHash`; `DailyCheckin.lastCheckinAt`, `streakCount`; `Order.fundingStatus`; `VendorShippingConfig.vendorId` và 5 cờ hãng vận chuyển.
- **Thêm `optional = false`** cho 19 tham chiếu `@ManyToOne` / `@OneToOne` có FK bắt buộc (vd. `ProductVariant.product`, `Order.vendorProfile`, `CustomerProfile.user`).
- **Bổ sung chú thích NULLABLE** cho thuộc tính đúng là được phép rỗng: `User.password` (tài khoản Google / Apple), `User.customerProfile` / `vendorProfile` / `adminProfile` (phía nghịch), `Product.category`, `Voucher.notificationBody`, `EscrowLedger.fundedAt`, `codRemittedAt`, `SystemLog.ipAddress`; `PlatformNotification.readCount` đánh dấu thuộc tính dẫn xuất (chỉ đọc).
- **Association:** `StockReservation → Order` đổi đầu Order từ `0..1` thành `1` (FK `orderId` NOT NULL); `KycResult` và `VendorShippingConfig` nay khớp đầu `1` sẵn có.
- GIAI_NGHIA_ENTITY.md: 12 dòng cột "bắt buộc" đổi Không → Có cho khớp.

Kiểm tra sau khi sửa: 722 thuộc tính ENTITY ↔ Java ↔ XMI (phần UML và phần EA) cùng bội số; 0 association lệch FK; 2 XMI parse được.

### 15.4. Đã sửa phía giao diện (`genie/`, 2026-11-06)

| Chỗ lệch | Sửa |
|---|---|
| Thời gian duyệt KYC "1-3 ngày" (Tổng quan Seller), "1–2 ngày" (Trở thành người bán) | Thống nhất "24 giờ làm việc" như UC-VND-04 và màn Onboarding (`vendor/Overview.jsx`, `storefront/BecomeSeller.jsx`, `i18n.js`) |
| Còn chữ "Xu" | "Điểm Genie" ở Phân tích của Seller (`vendor/VendorPages.jsx`) và Dashboard Admin (`admin/Dashboard.jsx`) |
| Nhật ký mẫu có chức danh "KYC Reviewer", quyền `refund.arbitrate`, `nghia.vd` ghi Moderator | Đổi thành Moderator, `HANDLE_DISPUTE`, CS Specialist (`admin/data.jsx`) |
| Mã đối tượng thông báo sàn `BUYER` / `SELLER` / `SPECIFIC` | `CUSTOMER` / `VENDOR` / `SPECIFIC_USERS` theo `PlatformNotificationTarget` (`admin/data.jsx`, `admin/PlatformNotifications.jsx`) |
| Trạng thái `REQUEUED` không có trong `ProductStatus` | Giữ `PENDING_APPROVAL`, thêm cờ `requeued` chỉ để hiện nhãn "Chờ duyệt lại" (`admin/ProductModeration.jsx`, `admin/data.jsx`) |
| Trang chính sách: "Không hỗ trợ COD", thanh toán bằng thẻ quốc tế / ví điện tử, giải ngân "15 hoặc 30 ngày", rút tiền "15 phút – 2 giờ, 3 lần/ngày, rút tự động" | VNPay / VietQR / COD (COD cũng qua Tài khoản Đảm bảo), giải ngân sau 7 ngày, rút tối thiểu 100.000₫ có OTP về tài khoản chính chủ, kế toán duyệt trong 24 giờ làm việc (`storefront/pages-data.js`) |
| Trang chính sách thuế nói Genie đang khấu trừ, nộp thay, cấp chứng từ và có "Yêu cầu xuất hóa đơn" trong đơn | Giữ nghĩa vụ pháp lý, ghi rõ chức năng khấu trừ đang xây dựng (thuộc DATN), hiện người bán tự kê khai bằng Sổ giao dịch ví / CSV (UC-VND-20); yêu cầu hóa đơn qua Chat |
| Shop Chính Hãng "hoàn tiền 200% nếu hàng giả" | Hoàn 100% + khóa sản phẩm + cảnh báo shop; bổ sung quy tắc này vào UC-SUP-03 trường hợp A |
| Gọi tên phương thức thanh toán | "VNPay, chuyển khoản qua SePay (mã VietQR), COD" thống nhất với màn thanh toán |

### 15.5. Lưu ý

- DATN (không có trong repo) vẫn dùng tên Lazadee; khi mở rộng lên DATN cần đổi tên tương ứng để giữ quy tắc "tên trùng DATN" (NFR-12). Các tham chiếu tới file của DATN trong báo cáo này giữ tên cũ `Lazadee_*_UML25.xmi`.
- Thư mục mã nguồn giao diện đổi tên `lazadee/` → `genie/` (2026-11-06); chữ "Lazadee" còn lại trong báo cáo này là đường dẫn / tên thành phần của bản `prototype/`, `client/` cũ (mục 1–14) hoặc tên file của DATN.

---

<a id="muc-16"></a>
## 16. THÊM PHÂN TÍCH CẢM XÚC ĐÁNH GIÁ BẰNG PHOBERT (2026-11-06)

Không xuất phát từ giao diện mà từ yêu cầu của nhóm: đã có mô hình PhoBERT phân tích cảm xúc riêng, đưa vào TLCN ở mức MVP.

| Hạng mục | Quyết định |
|---|---|
| Use case | `UC-AI-07` lấy lại từ DATN (cùng mã), tên TLCN "Phân tích cảm xúc đánh giá bằng PhoBERT"; xếp phân hệ 08; Sprint 6; **87 use case** |
| Phạm vi | Cảm xúc tổng thể 3 nhãn (`POSITIVE` / `NEUTRAL` / `NEGATIVE`) + độ tin cậy; theo khía cạnh để DATN |
| Dùng ở đâu (MVP) | Phân tích bán hàng của shop (UC-VND-08) và ưu tiên hàng chờ Kiểm duyệt đánh giá (UC-SOC-05); chưa hiện cho khách |
| Dữ liệu | Dùng lại cột DATN `product_reviews.aspect_sentiment_json` (khóa `overall`) + `attachSentiment()` — không thêm bảng / enum |
| Hạ tầng | Chạy trong AI Worker có sẵn (ONNX CPU), tác nhân `ACT-AIW` đổi tên "AI Worker Service (KYC & PhoBERT)" |
| Excel DATN | `UC-AI-07` chuyển "Đã triển khai" (dòng xám), ghi chú "Có trong TLCN (Sprint 6) — bản rút gọn" |
| Giao diện | Prototype chưa có màn hiển thị cảm xúc — cần thêm khối "Cảm xúc đánh giá" ở Phân tích của Seller và nhãn "AI: Tiêu cực" ở Kiểm duyệt đánh giá của Admin |


---

## 17. ĐỒNG BỘ VỚI ĐẶC TẢ API (2026-11-08, TLCN v1.3)

Đặc tả API theo màn hình: `document/API_SPEC.md` (v1.1). Các chênh lệch tìm thấy khi viết đặc tả đã xử lý:

| Hạng mục | Đã sửa |
|---|---|
| Trường màn hình dùng mà mô hình chưa có | ENTITY.md v1.3 + 10 file `java-code/` + GIAI_NGHIA_ENTITY.md: `Address.label`, `VendorProfile.isFavorite`, `Category.description`, `FlashSaleItem.status`, `ProductReview.isAnonymous`, `PlatformNotification.sendAt` / `recipientUserIdsJson`, `Message.orderId`; entity mới `CodRemittance` (bảng `cod_remittances`); 3 enum mới. Tổng 65 entity, 55 enum, 101 quan hệ |
| Mã lỗi | 66 mã mới vào LLD 12.1 và Document_Vi 3.7.1 |
| Đường dẫn API | LLD 8.5 và implement.md Phần 17 viết lại theo `API_SPEC.md`; sơ đồ tuần tự, idempotency, upload, rate limit, webhook (`/payments/vnpay/ipn`, `/payments/sepay/webhook`) ở LLD, Document_Vi, implement, LUU_TRU_DU_LIEU, TECH_STACK, Usecase.md |
| Giao diện | Thanh toán hiện "Đơn 1/2" thay mã đơn chưa cấp; ô Flash Sale đổi "Sản phẩm / SKU" → "Sản phẩm"; thêm từ khóa xu hướng (Header), đổi email (Hồ sơ), cảm xúc đánh giá (Phân tích của shop), nhãn "AI: Tiêu cực" (Kiểm duyệt đánh giá), phong tỏa ký quỹ (Trả hàng & Hoàn tiền), chi tiết đơn (Đơn hàng của shop), xóa banner |
| XMI | `Genie_ClassDiagram_UML25.xmi` cập nhật theo ENTITY.md v1.3 (70 class, 55 enum, 101 association, 74 dependency; Sơ đồ 01/05/07/09 thêm ô mới); `Genie_UseCase_UML25.xmi` chỉ đổi nhãn phiên bản. `Quản Lý UseCase.xlsx` chưa sửa |

---

<a id="muc-18"></a>
## 18. ĐỐI CHIẾU VỚI MÃ NGUỒN `genie-multivendor-platform` (2026-11-16, TLCN v1.4)

### 18.1 Phạm vi lượt này
Đối chiếu toàn bộ `Document/` với repo code: giao diện `web/` (34 feature, plan G00–G13 đã xong 429/429 task, chạy trên lớp mock), backend `server-spring` (65 entity + `V1…V14`, module AUTH đã hiện thực — plan `plan/2026-11-14/auth.md`, ADR 0007 / 0008), hạ tầng `deploy/` (ADR 0009 Silo), bảng bàn giao `docs/web-backend-handoff.md`.

### 18.2 Thay đổi hạ tầng
| Hạng mục | Trước | Nay | Ghi chú |
|---|---|---|---|
| Kho tệp S3 | MinIO `minio/minio:RELEASE.2024-08-17` | **Silo** `pgsty/silo:RELEASE.2026-10-26T00-00-00Z` | MinIO ngừng phát hành bản cộng đồng; Silo là fork giữ API S3. Giữ `MINIO_*`, SDK `io.minio`, `GNE-MINIO-5003`, mã tác nhân `ACT-MIN` (đổi tên "Silo S3 Storage") |
| Thanh toán ngân hàng | SePay (đã có từ v1.1) | Giữ nguyên, đã có trong repo: `POST /api/v1/payments/sepay/webhook`, `SEPAY_API_KEY` / `SEPAY_ACCOUNT_NUMBER`, CSP `qr.sepay.vn`, `scripts/webhooks/sepay-webhook.http`; giao diện VietQR hỏi lại phiên 3 giây | Tài liệu kiểm lại: không còn chỗ chỉ ghi "VNPay / COD" mà thiếu VietQR |

### 18.3 Nghiệp vụ AUTH đổi khi triển khai (đã sửa tài liệu theo code)
| # | Nội dung | Tài liệu đã sửa |
|---|---|---|
| 1 | Đăng ký giữ tạm Redis 15 phút, tạo `users` khi OTP đúng; `UNVERIFIED` chỉ cho nhân viên được mời | Usecase AUTH-01, API_SPEC 2.1, LUU_TRU 4.2, TECH_STACK 12 |
| 2 | OTP dùng chung `otp:challenge:{id}`, OTP bước 2 bằng header | LUU_TRU 4.2, Usecase AUTH-01…10 |
| 3 | Khóa tạm không đổi `status` (`LOCKED` để dành), mở khóa qua email | ENTITY 1.1, GIAI_NGHIA, Usecase AUTH-02 |
| 4 | Tài khoản chỉ Google đăng nhập mật khẩu → `GNE-AUTH-4012`; quên mật khẩu = thiết lập mật khẩu | API_SPEC 2.5 / 15, Document_Vi 3.7.1, LLD 12.1, Kien_Thuc 20 |
| 5 | Refresh giữ `family_id`, ân hạn 10 giây, trả kèm `user`; thiết bị đăng nhập theo `family_id` | ENTITY 1.7 (V14), API_SPEC 2.6 / 3.6, LLD SD-01 / 11.2 |
| 6 | Liên kết Google ở trang tài khoản: thêm `POST /me/identities/{provider}/authorize`, email Google phải trùng email tài khoản (`GNE-AUTH-4098`), gỡ liên kết nhập lại mật khẩu, `provider` chữ thường | API_SPEC 3.3, Usecase AUTH-03, Kien_Thuc 20 |
| 7 | Ảnh Google tải về lưu Silo, tên / ảnh chỉ điền khi hồ sơ trống | Usecase AUTH-03, API_SPEC 2.10, pháp lý Phần 2 |
| 8 | Xóa tài khoản: chặn trước khi gửi mã, giữ phiên hiện tại, email nhắc, cron 02:30 | Usecase AUTH-10, API_SPEC 3.7, LLD 10, pháp lý Phần 2 |
| 9 | `V14__auth_additions.sql` (+`family_id`, `last_used_at`, `deletion_requested_at`, 5 index, +8 `AuditAction`); kế hoạch `V14__api_sync_v13.sql` bỏ (các cột v1.3 đã nằm trong `V1…V12`) | ENTITY, GIAI_NGHIA, java-code, Class XMI, LLD 5.2, implement 3.1 |

### 18.4 Đổi tên use case theo giao diện (mã giữ nguyên)
| Mã | Tên cũ | Tên mới (theo menu / màn hình) |
|---|---|---|
| `UC-AUTH-03` | Đăng nhập một lần qua Mạng xã hội (SSO OAuth2) | Đăng nhập & Liên kết tài khoản Google / Apple (OAuth2 / OpenID Connect) — "Tiếp tục với Google", "Liên kết đăng nhập" |
| `UC-AUTH-06` | Đổi mật khẩu tài khoản nội bộ | Đổi / Thiết lập mật khẩu — menu "Đổi mật khẩu" / "Thiết lập mật khẩu" |
| `UC-AUTH-11` | Đăng xuất & Thu hồi phiên đăng nhập | Đăng xuất & Quản lý thiết bị đăng nhập — menu "Thiết bị đăng nhập" |

Đã đổi ở: Usecase.md, UML_UseCase_PlantUML.md, `Genie_UseCase_UML25.xmi`, `Quản Lý UseCase.xlsx`, Document_Vi, SRS, BAO_CAO_PHAM_VI_MVP.

### 18.5 Giao diện ↔ API
- 6 endpoint giao diện đang gọi mà spec chưa có → **bổ sung API_SPEC 5.18** (chiến dịch, slug chiến dịch, bộ lọc thuộc tính, thương hiệu xác minh, tra SKU công khai, chính sách phí ship); thêm `product_slug` vào dòng giỏ và `$OrderItem`.
- 10 endpoint mã web đặt khác tên spec → **giữ spec**, sửa mã khi nối backend (API_SPEC 17.5 b).
- Lệch bố cục nhỏ so với prototype (mục lục chính sách gập lại ở mobile, ảnh đại diện lên đầu form, form mở shop 2 cột ở tablet) là cải tiến có chủ đích — không cần đổi đặc tả.

### 18.6 Theo dõi tiến độ
Tạo [CHECKLIST_TIEN_DO_UC.md](CHECKLIST_TIEN_DO_UC.md): 87 UC × 5 lớp (giao diện, backend, test, nối API, chạy thật) — hiện 3 đã triển khai, 5 đang triển khai (AUTH), 79 chưa có backend (giao diện đã dựng). Excel thêm lựa chọn "Đang triển khai".
