# TÀI LIỆU ĐẶC TẢ USE CASE — BẢN TLCN (TIỂU LUẬN CHUYÊN NGÀNH)
## SÀN THƯƠNG MẠI ĐIỆN TỬ ĐA NGƯỜI BÁN GENIE (MULTI-VENDOR MARKETPLACE)

> **Phạm vi:** 87 use case · 11 phân hệ · 16 tác nhân (8 con người / chức danh + 8 hệ thống) · 4 use case phụ dùng chung — phủ **toàn bộ giao diện client + prototype** (đối chiếu ở [BAO_CAO_DOI_CHIEU_GIAO_DIEN.md](../../BAO_CAO_DOI_CHIEU_GIAO_DIEN.md)).
> **Mã và tên use case lấy theo bản hoàn chỉnh DATN** (`DATN/diagrams/UseCaseDiagram/Usecase.md`) để đối chiếu và mở rộng; khoảng trống trong dãy số là use case chỉ có ở DATN. `UC-CAT-16/17/18`, `UC-VND-20/21`, `UC-LOG-16` là use case mới phát sinh từ giao diện — đã thêm cả vào DATN.
> **Nguồn đồng bộ:** [ENTITY.md](../ClassDiagram/ENTITY.md) (65 entity) · [UML_UseCase_PlantUML.md](./UML_UseCase_PlantUML.md) · `Genie_UseCase_UML25.xmi` · `Quản Lý UseCase.xlsx` — cùng sinh từ một mô hình (tác nhân, use case, «include» / «extend»).
> **Phạm vi so với DATN và lý do cắt:** [BAO_CAO_PHAM_VI_MVP.md](../../BAO_CAO_PHAM_VI_MVP.md).
> **Tiến độ hiện thực từng UC:** [CHECKLIST_TIEN_DO_UC.md](../../CHECKLIST_TIEN_DO_UC.md) · **Phiên bản:** TLCN v1.4 (2026-11-16) — đồng bộ với mã nguồn `genie-multivendor-platform` (xem CHANGELOG cuối file).

---

## 📑 MỤC LỤC TỔNG THỂ

- [PHẦN I: Tổng quan hệ thống & Ma trận tác nhân](#phan-i)
- [PHẦN II: Ma trận truy vết yêu cầu & thực thể dữ liệu](#phan-ii)
- [PHẦN III: Đặc tả chi tiết 87 use case theo 11 phân hệ](#phan-iii)
  - [Phân hệ 01 — Xác thực & Tài khoản người dùng (9 UC)](#ph-auth)
  - [Phân hệ 02 — Danh mục, Sản phẩm & Tìm kiếm (17 UC)](#ph-cat)
  - [Phân hệ 03 — Giỏ hàng, Đặt hàng & Đơn hàng (9 UC)](#ph-ord)
  - [Phân hệ 04 — Thanh toán, Ký quỹ (Escrow) & Sổ cái (5 UC)](#ph-pay)
  - [Phân hệ 05 — Gian hàng & Thẩm định KYC người bán (11 UC)](#ph-vnd)
  - [Phân hệ 06 — Vận chuyển qua đối tác 3PL (5 UC)](#ph-log)
  - [Phân hệ 07 — Khuyến mãi, Voucher, Flash Sale & Điểm (9 UC)](#ph-pro)
  - [Phân hệ 08 — Chat, Đánh giá & Theo dõi shop (7 UC)](#ph-soc)
  - [Phân hệ 09 — Trả hàng, Hoàn tiền & Khiếu nại (4 UC)](#ph-sup)
  - [Phân hệ 10 — Thông báo (3 UC)](#ph-not)
  - [Phân hệ 11 — Quản trị sàn, Nhân viên & Rút tiền (8 UC)](#ph-adm)
- [PHẦN IV: Sơ đồ Use Case UML 2.5](#phan-iv)

---

<a id="phan-i"></a>
# PHẦN I: TỔNG QUAN HỆ THỐNG & MA TRẬN TÁC NHÂN (ACTOR TAXONOMY)

### 1.1 Khung kiến trúc & Phạm vi nghiệp vụ TLCN
Genie TLCN là sàn TMĐT đa người bán **khép kín vòng nghiệp vụ và phủ toàn bộ giao diện đã dựng** (Storefront, Seller Center, Admin Console, Auth): shop mở gian hàng và qua KYC → đăng sản phẩm (kiểm duyệt tự động bằng Gemini + nhân viên) → khách mua từ nhiều shop trong một lần thanh toán, xếp chồng voucher 3 tầng và Điểm, Flash Sale → thanh toán **VNPay / VietQR (SePay) / COD** → 3PL giao hàng (GHN mặc định, GHTK / ViettelPost / J&T khi shop bật) → tiền giữ ký quỹ 7 ngày → giải ngân vào ví shop → shop rút tiền; kèm đánh giá, chat, theo dõi shop, trả hàng / hoàn tiền, thông báo toàn sàn, phân quyền nhân viên 3 chức danh.
1. **Kiến trúc ứng dụng:**
   - **Frontend:** Astro + React (storefront SSR cho SEO và thẻ Open Graph của link chia sẻ, Seller Center và Admin Console dạng React SPA), Tailwind CSS, đa ngôn ngữ (i18n).
   - **Backend:** **Spring Boot 3 (Java 21) dạng modular monolith** — mỗi phân hệ là một module; sự kiện nội bộ Spring `ApplicationEvent` + `@Async`, tác vụ định kỳ `@Scheduled`; WebSocket (STOMP) cho chat và thông báo; phân quyền `@PreAuthorize` theo 13 mã quyền.
   - **AI Worker (Python FastAPI):** KYC (QR, MRZ, so khớp khuôn mặt ONNX) và phân tích cảm xúc đánh giá bằng mô hình **PhoBERT** nhóm đã fine-tune (ONNX Runtime, CPU).
   - **Google Gemini API:** kiểm duyệt sản phẩm tự động (structured output JSON).
   - **Dữ liệu:** PostgreSQL 16 (65 bảng), Redis 7 (OTP, cache, bộ đếm lượt xem / từ khóa / banner, khóa ngắn), Silo (ảnh, hồ sơ KYC, giấy tờ sản phẩm).
2. **Quy tắc bất biến trong giao dịch:** tiền `DECIMAL(19,2)`; snapshot dữ liệu đơn hàng; khóa lạc quan `version` chống bán vượt tồn kho / suất Flash Sale / tiêu Điểm / rút tiền 2 lần; IPN / webhook idempotent; sổ cái kép chỉ ghi thêm.
3. **Thuộc DATN, không làm ở TLCN:** ví khách hàng, MoMo / ZaloPay / thẻ quốc tế / BNPL, Genie Express kèm app tài xế & Hub, voucher tầng 4 (ưu đãi thanh toán), gamification ngoài Điểm (hạng thành viên, nhiệm vụ, vòng quay, vật phẩm trang trí), AI gợi ý vector / trợ lý mua sắm, đa kho, tài khoản phụ shop, livestream, B2B, đa tiền tệ, A/B testing, Kafka / Saga / feature flag, ticket CSKH.

---

### 1.2 Danh mục tác nhân — 16 tác nhân (8 con người / chức danh + 8 hệ thống)

- **Con người:** `Customer` kế thừa `Guest`; `Vendor` kế thừa `Customer` (một tài khoản hai chế độ Mua / Bán). `ACT-MOD`, `ACT-CS`, `ACT-FIN` là **3 chức danh nhân viên** kế thừa `ACT-STF`; `Admin` kế thừa `ACT-STF` và có thêm use case quản trị riêng — **không có Super Admin**.
- **Hệ thống:** chỉ các hệ thống **bên ngoài ranh giới** ứng dụng Spring Boot (VNPay / SePay, hãng vận chuyển, AI Worker, Gemini, Google / Apple, Silo, Email) và bộ lập lịch. PostgreSQL, Redis, WebSocket hub **không phải tác nhân**.

```
                              [Tác nhân TLCN]
                                    │
        ┌───────────────────────────┴──────────────────────────────┐
  [8 Con người / chức danh]                              [8 Hệ thống «system»]
  ├─► ACT-GST Guest                                     ├─► ACT-PGW Payment Gateway (VNPay, SePay)
  │     └─△ ACT-CUS Customer                            ├─► ACT-3PL Đối tác vận chuyển (GHN, GHTK, VTP, J&T)
  │           └─△ ACT-VND Vendor                        ├─► ACT-AIW AI Worker Service (KYC & PhoBERT)
  └─► ACT-STF Staff (Nhân viên sàn)                     ├─► ACT-GEM Google Gemini API
        ├─△ ACT-MOD Moderator (kiểm duyệt)              ├─► ACT-IDP Identity Provider (Google, Apple)
        ├─△ ACT-CS  CS Specialist (CSKH)                ├─► ACT-MIN Silo S3 Storage
        ├─△ ACT-FIN Finance Officer (tài chính)         ├─► ACT-EML Email Service
        └─△ ACT-ADM Admin                               └─► ACT-CRN Platform Cron / Scheduler
```

*Ký hiệu:* `└─△` = Generalization (tác nhân con kế thừa use case của tác nhân cha).

#### Bảng định nghĩa 16 tác nhân

| Mã Tác Nhân | Tên Tác Nhân | Phân Loại | Vai Trò & Giới Hạn Nghiệp Vụ | Số UC chính / phụ |
| :--- | :--- | :--- | :--- | :---: |
| `ACT-GST` | **Guest (khách vãng lai)** | Con người | Người chưa đăng nhập. Xem danh mục, tìm kiếm (từ khóa gợi ý), xem chi tiết sản phẩm, sản phẩm tương tự, trang shop, đánh giá; mở link chia sẻ sản phẩm; thêm vào giỏ tạm (lưu trình duyệt); đăng ký tài khoản hoặc đăng nhập Google / Apple. | 11 / 0 |
| `ACT-CUS` | **Customer (khách hàng)** | Con người (kế thừa Guest) | Đã đăng ký và kích hoạt tài khoản. Quản lý hồ sơ, sổ địa chỉ, giỏ hàng; đặt hàng nhiều shop trong 1 lần, thanh toán VNPay / VietQR (SePay) / COD; lưu và xếp chồng voucher 3 tầng, dùng Điểm; mua Flash Sale; theo dõi đơn, xác nhận đã nhận, đánh giá, bấm Hữu ích, báo cáo đánh giá; chat, theo dõi shop; yêu cầu trả hàng / hoàn tiền; xóa tài khoản. | 39 / 0 |
| `ACT-VND` | **Vendor (người bán/shop)** | Con người (kế thừa Customer) | Khách hàng đã mở shop và được duyệt KYC (cùng 1 tài khoản, có thêm chế độ Bán). Quản lý sản phẩm / SKU / giấy tờ chứng thực, đề xuất danh mục, xác nhận và xử lý đơn, voucher shop, đăng ký Flash Sale, phản hồi và báo cáo đánh giá, chat (trả lời tự động), cài đặt gian hàng / vận chuyển / tạm nghỉ, ví & sổ giao dịch, tài khoản nhận tiền và rút tiền. | 29 / 0 |
| `ACT-STF` | **Staff (Nhân viên sàn)** | Con người | Nhân viên vận hành sàn (role STAFF). Tác nhân tổng quát của 3 chức danh MOD / CS / FIN — quyền thật do Admin cấp theo từng mã quyền (UC-ADM-02). Dùng chung các use case tài khoản (đăng nhập, đổi mật khẩu, thông báo). | 7 / 0 |
| `ACT-MOD` | **Moderator (Nhân viên kiểm duyệt)** | Chức danh nhân viên sàn (kế thừa Staff) | Chức danh MODERATOR: duyệt KYC, duyệt sản phẩm (kết quả Gemini), quản lý danh mục và duyệt đề xuất danh mục, kiểm duyệt đánh giá. | 7 / 0 |
| `ACT-CS` | **CS Specialist (Nhân viên CSKH)** | Chức danh nhân viên sàn (kế thừa Staff) | Chức danh CS_SPECIALIST: phân xử tranh chấp trả hàng / hoàn tiền, thực hiện hoàn tiền, kiểm duyệt đánh giá. | 3 / 0 |
| `ACT-FIN` | **Finance Officer (Nhân viên tài chính)** | Chức danh nhân viên sàn (kế thừa Staff) | Chức danh FINANCE_OFFICER: duyệt và chi lệnh rút tiền, cấu hình hoa hồng, xem sổ cái và dashboard tài chính, quản lý voucher sàn và chiến dịch Flash Sale. | 5 / 1 |
| `ACT-ADM` | **Admin** | Con người (kế thừa Staff) | Quản trị viên cao nhất (không có Super Admin). Mọi quyền của nhân viên, cộng thêm: quản lý nhân viên & phân quyền, khóa / mở khóa người dùng, nhật ký hệ thống, thông báo toàn sàn, banner trang chủ. | 19 / 0 |
| `ACT-PGW` | **Payment Gateway (VNPay, SePay)** | Hệ thống | VNPay (sandbox khi demo): chuyển hướng thanh toán thẻ / QR, IPN HMAC-SHA512, API hoàn tiền. SePay: theo dõi biến động tài khoản ngân hàng của sàn, gửi webhook khi có tiền vào (khách chuyển khoản VietQR, GHN chuyển tiền COD) và tiền ra (xác nhận lệnh rút tiền / hoàn tiền nhân viên chuyển tay); SePay không tự chi tiền. | 1 / 5 |
| `ACT-3PL` | **Đối tác vận chuyển (GHN, GHTK, ViettelPost, J&T)** | Hệ thống | GHN là hãng mặc định: API địa giới, tính phí, tạo vận đơn, in phiếu gửi, webhook hành trình, thu hộ COD và đối soát. GHTK / ViettelPost / J&T Express dùng cùng giao thức qua lớp adapter khi shop bật hãng đó (UC-LOG-15). | 1 / 7 |
| `ACT-AIW` | **AI Worker Service (KYC & PhoBERT)** | Hệ thống | Dịch vụ Python FastAPI chạy riêng, phục vụ hai việc: (1) KYC — giải mã QR CCCD, đọc MRZ, phát hiện quốc huy / vân tay, so khớp khuôn mặt (ONNX) và chấm điểm rủi ro; (2) phân tích cảm xúc đánh giá bằng PhoBERT (UC-AI-07). | 2 / 0 |
| `ACT-GEM` | **Google Gemini API** | Hệ thống | Mô hình ngôn ngữ lớn của Google gọi qua REST API (structured output JSON). Dùng cho kiểm duyệt sản phẩm tự động (UC-CAT-12): nhận tên, mô tả, danh mục, ảnh sản phẩm; trả quyết định APPROVE / REJECT / NEED_REVIEW, điểm rủi ro và lý do. | 0 / 1 |
| `ACT-IDP` | **Identity Provider (Google, Apple)** | Hệ thống | Nhà cung cấp danh tính OAuth2 / OpenID Connect cho đăng nhập mạng xã hội (UC-AUTH-03). | 0 / 1 |
| `ACT-MIN` | **Silo S3 Storage** *(trước đây MinIO — đổi 2026-11-15, ADR 0009; giữ mã `ACT-MIN`)* | Hệ thống | Lưu trữ đối tượng tương thích S3: ảnh sản phẩm / mô tả / đánh giá, ảnh đại diện, logo và ảnh bìa shop, banner, hồ sơ KYC và giấy tờ chứng thực sản phẩm (bucket riêng tư), bằng chứng trả hàng; cấp Presigned URL. Trong sơ đồ: nối với use case dùng chung «Tải tệp lên Silo (Presigned URL)». | 0 / 5 |
| `ACT-EML` | **Email Service** | Hệ thống | Dịch vụ SMTP (Gmail SMTP / Mailtrap khi demo) gửi OTP, lời mời kích hoạt nhân viên, cảnh báo đăng nhập và email giao dịch. Trong sơ đồ: nối với use case dùng chung «Gửi mã OTP xác thực» và trực tiếp với các use case gửi email. | 0 / 6 |
| `ACT-CRN` | **Platform Cron / Scheduler** | Hệ thống | Tác vụ định kỳ của Spring (@Scheduled): hủy đơn quá hạn thanh toán, tự hoàn tất đơn sau 7 ngày, giải ngân ký quỹ, mở / đóng Flash Sale, hết hạn Điểm, ẩn danh hóa tài khoản sau 30 ngày, tổng hợp lượt xem / từ khóa, tính chỉ số shop. | 3 / 4 |

#### Ánh xạ tác nhân DATN → TLCN

| DATN | TLCN |
| :--- | :--- |
| `ACT-MOD`, `ACT-CS`, `ACT-FIN` | Giữ nguyên mã (3 chức danh của TLCN) |
| `ACT-KYC` Thẩm định KYC, `ACT-MKT` Marketing | Gộp vào `ACT-MOD` (duyệt KYC) và `ACT-FIN` (voucher sàn, Flash Sale, banner) |
| `ACT-VST` Vendor Staff, `ACT-SHP` Shipper, `ACT-B2B`, `ACT-IPH`, `ACT-DSP`, `ACT-RSK`, `ACT-CMP`, `ACT-CRD`, `ACT-TEC` | Không dùng (tính năng thuộc DATN) |
| `ACT-ELS`, `ACT-QDR`, `ACT-KFK`, `ACT-NOT`, `ACT-ENG`, `ACT-FRD`, `ACT-SVC`, `ACT-LLM` | Không dùng (hạ tầng thuộc DATN; kiểm duyệt AI dùng `ACT-GEM`) |
| `ACT-FCM`, `ACT-ZNS`, `ACT-BNK`, `ACT-TAX`, `ACT-BNPL`, `ACT-WTH`, `ACT-FX`, `ACT-CAP` | Không dùng (Web Push, Zalo, chi hộ tự động, thuế, BNPL, thời tiết, tỷ giá, captcha) |
| — | `ACT-GEM` Google Gemini API (mới ở TLCN; DATN dùng AI Worker cho kiểm duyệt sản phẩm) |

---

### 1.3 Ma trận tham gia của tác nhân theo 11 phân hệ

| Phân hệ | GST | CUS | VND | STF | MOD | CS | FIN | ADM | PGW | 3PL | AIW | GEM | IDP | MIN | EML | CRN |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **01. Xác thực & Tài khoản người dùng** | C2 | C8 | C4 | C5 | — | — | — | C3 | — | P1 | — | — | P1 | — | — | P1 |
| **02. Danh mục, Sản phẩm & Tìm kiếm** | C8 | C9 | C4 | — | C5 | — | C1 | C4 | — | — | — | P1 | — | — | — | — |
| **03. Giỏ hàng, Đặt hàng & Đơn hàng** | C1 | C6 | — | — | — | — | — | — | P1 | P3 | — | — | — | — | — | C2 |
| **04. Thanh toán, Ký quỹ (Escrow) & Sổ cái** | — | C1 | — | — | — | — | — | — | C1 P1 | — | — | — | — | — | — | C1 |
| **05. Gian hàng & Thẩm định KYC người bán** | — | C1 | C8 | — | C1 | — | — | C1 | — | P1 | C1 | — | — | P1 | P2 | P1 |
| **06. Vận chuyển qua đối tác 3PL** | — | — | C3 | — | — | — | P1 | — | P1 | C1 P2 | — | — | — | — | — | P1 |
| **07. Khuyến mãi, Voucher, Flash Sale & Điểm** | — | C6 | C2 | — | — | — | C2 | C2 | — | — | — | — | — | — | — | P1 |
| **08. Chat, Đánh giá & Theo dõi shop** | — | C5 | C4 | — | C1 | C1 | — | — | — | — | — | — | — | P1 | — | — |
| **09. Trả hàng, Hoàn tiền & Khiếu nại** | — | C1 | C1 | — | — | C2 | — | C1 | P1 | — | — | — | — | — | — | — |
| **10. Thông báo** | — | C2 | C2 | C1 | — | — | — | C1 | — | — | — | — | — | P1 | P1 | — |
| **11. Quản trị sàn, Nhân viên & Rút tiền** | — | — | C1 | C1 | — | — | C2 | C7 | P1 | — | — | — | — | P1 | P2 | — |

*Ghi chú:* `Cn` = tác nhân chính ở n use case; `Pn` = tác nhân phụ ở n use case. Tác nhân con kế thừa use case của tác nhân cha (Vendor ⊃ Customer ⊃ Guest; MOD / CS / FIN / ADM ⊃ Staff).

---

<a id="phan-ii"></a>
# PHẦN II: MA TRẬN TRUY VẾT YÊU CẦU & THỰC THỂ DỮ LIỆU (TRACEABILITY MATRIX)

> Liên kết hai chiều giữa **87 use case**, **65 entity** (ENTITY.md), kế hoạch sprint và yêu cầu chức năng ở [Document_Vi.md](../../document/Document_Vi.md) mục 1.7.

| Mã UC | Tên Use Case | Phân Hệ | Tác Nhân Chính · Phụ | Thực Thể CSDL | Kế hoạch | Nguồn |
| :--- | :--- | :--- | :--- | :--- | :---: | :---: |
| **UC-AUTH-01** | Đăng ký tài khoản Khách hàng | 01. Xác thực & Tài khoản người dùng | Chính: Guest (khách vãng lai) · Phụ: — | `User`, `CustomerProfile` | Sprint 1 | TLCN v1.0 |
| **UC-AUTH-02** | Đăng nhập hệ thống | 01. Xác thực & Tài khoản người dùng | Chính: Customer (khách hàng); Vendor (người bán/shop); Staff (Nhân viên sàn); Admin · Phụ: — | `User`, `RefreshToken`, `SystemLog`, `Notification` | Sprint 1 | TLCN v1.0 |
| **UC-AUTH-03** | Đăng nhập & Liên kết tài khoản Google / Apple (OAuth2 / OpenID Connect) | 01. Xác thực & Tài khoản người dùng | Chính: Guest (khách vãng lai); Customer (khách hàng) · Phụ: Identity Provider (Google, Apple) | `User`, `CustomerProfile`, `RefreshToken` | Sprint 7 | Đưa lại từ DATN |
| **UC-AUTH-04** | Khôi phục mật khẩu qua Email OTP (Forgot Password) | 01. Xác thực & Tài khoản người dùng | Chính: Customer (khách hàng); Vendor (người bán/shop); Staff (Nhân viên sàn) · Phụ: — | `User`, `RefreshToken`, `SystemLog` | Sprint 1 | TLCN v1.0 |
| **UC-AUTH-06** | Đổi / Thiết lập mật khẩu | 01. Xác thực & Tài khoản người dùng | Chính: Customer (khách hàng); Vendor (người bán/shop); Staff (Nhân viên sàn); Admin · Phụ: — | `User`, `SystemLog` | Sprint 1 | TLCN v1.0 |
| **UC-AUTH-08** | Quản lý thông tin hồ sơ cá nhân (Customer Profile) | 01. Xác thực & Tài khoản người dùng | Chính: Customer (khách hàng); Staff (Nhân viên sàn) · Phụ: — | `CustomerProfile`, `User` | Sprint 2 | TLCN v1.0 |
| **UC-AUTH-09** | Quản lý sổ địa chỉ giao hàng chuẩn GHN | 01. Xác thực & Tài khoản người dùng | Chính: Customer (khách hàng) · Phụ: Đối tác vận chuyển (GHN, GHTK, ViettelPost, J&T) | `Address`, `CustomerProfile` | Sprint 2 | TLCN v1.0 |
| **UC-AUTH-10** | Yêu cầu ẩn danh hóa & Xóa dữ liệu cá nhân (Xóa tài khoản) | 01. Xác thực & Tài khoản người dùng | Chính: Customer (khách hàng) · Phụ: Platform Cron / Scheduler | `User`, `CustomerProfile`, `Address`, `RefreshToken`, `SystemLog` | Sprint 7 | Đưa lại từ DATN |
| **UC-AUTH-11** | Đăng xuất & Quản lý thiết bị đăng nhập | 01. Xác thực & Tài khoản người dùng | Chính: Customer (khách hàng); Vendor (người bán/shop); Staff (Nhân viên sàn); Admin · Phụ: — | `RefreshToken` | Sprint 1 | TLCN v1.0 |
| **UC-CAT-01** | Duyệt cây danh mục đa cấp (Category Hierarchy) | 02. Danh mục, Sản phẩm & Tìm kiếm | Chính: Guest (khách vãng lai); Customer (khách hàng) · Phụ: — | `Category` | Sprint 2 | TLCN v1.0 |
| **UC-CAT-02** | Tìm kiếm sản phẩm theo từ khóa | 02. Danh mục, Sản phẩm & Tìm kiếm | Chính: Guest (khách vãng lai); Customer (khách hàng) · Phụ: — | `Product`, `ProductVariant` | Sprint 2 | TLCN v1.0 |
| **UC-CAT-04** | Gợi ý từ khóa tìm kiếm & Xu hướng (Autocomplete & Trending) | 02. Danh mục, Sản phẩm & Tìm kiếm | Chính: Guest (khách vãng lai); Customer (khách hàng) · Phụ: — | `SearchKeywordStat`, `Product`, `Category` | Sprint 7 | Đưa lại từ DATN |
| **UC-CAT-05** | Lọc nâng cao & Sắp xếp sản phẩm đa tiêu chí | 02. Danh mục, Sản phẩm & Tìm kiếm | Chính: Guest (khách vãng lai); Customer (khách hàng) · Phụ: — | `Product`, `ProductVariant`, `VendorProfile`, `Brand`, `VendorShippingConfig` | Sprint 2 | TLCN v1.0 |
| **UC-CAT-06** | Xem chi tiết sản phẩm, ma trận biến thể SKU & Tồn kho | 02. Danh mục, Sản phẩm & Tìm kiếm | Chính: Guest (khách vãng lai); Customer (khách hàng) · Phụ: — | `Product`, `ProductVariant`, `ProductImage`, `Brand`, `ProductViewStat` | Sprint 2 | TLCN v1.0 |
| **UC-CAT-07** | Xem gợi ý sản phẩm tương tự | 02. Danh mục, Sản phẩm & Tìm kiếm | Chính: Guest (khách vãng lai); Customer (khách hàng) · Phụ: — | `Product`, `Category` | Sprint 7 | Đưa lại từ DATN |
| **UC-CAT-08** | Quản lý danh sách sản phẩm yêu thích (Wishlist) | 02. Danh mục, Sản phẩm & Tìm kiếm | Chính: Customer (khách hàng) · Phụ: — | `UserWishlist`, `Product` | Sprint 6 | TLCN v1.0 |
| **UC-CAT-09** | Thêm mới sản phẩm & Biến thể SKU (Người bán) | 02. Danh mục, Sản phẩm & Tìm kiếm | Chính: Vendor (người bán/shop) · Phụ: — | `Product`, `ProductVariant`, `ProductImage`, `Brand`, `ProductDocument`, `CategoryAttribute`, `ProductModerationResult` | Sprint 2 | TLCN v1.0 |
| **UC-CAT-10** | Cập nhật thông tin sản phẩm, giá bán & hình ảnh | 02. Danh mục, Sản phẩm & Tìm kiếm | Chính: Vendor (người bán/shop) · Phụ: — | `Product`, `ProductDocument` | Sprint 2 | TLCN v1.0 |
| **UC-CAT-11** | Ẩn / Khóa / Xóa mềm sản phẩm | 02. Danh mục, Sản phẩm & Tìm kiếm | Chính: Vendor (người bán/shop); Moderator (Nhân viên kiểm duyệt); Admin · Phụ: — | `Product` | Sprint 2 | TLCN v1.0 |
| **UC-CAT-12** | Kiểm duyệt sản phẩm tự động qua AI (Google Gemini API) | 02. Danh mục, Sản phẩm & Tìm kiếm | Chính: — · Phụ: Google Gemini API | `Product`, `ProductModerationResult`, `ProductDocument`, `Notification` | Sprint 7 | Đưa lại từ DATN |
| **UC-CAT-13** | Kiểm duyệt sản phẩm thủ công theo hàng đợi (Staff Moderator) | 02. Danh mục, Sản phẩm & Tìm kiếm | Chính: Moderator (Nhân viên kiểm duyệt) · Phụ: — | `Product`, `AdminProfile`, `ProductModerationResult`, `ProductDocument`, `Brand`, `SystemLog` | Sprint 2 | TLCN v1.0 |
| **UC-CAT-14** | Quản lý danh mục sàn toàn cục & Cấu hình tỷ lệ hoa hồng | 02. Danh mục, Sản phẩm & Tìm kiếm | Chính: Moderator (Nhân viên kiểm duyệt); Finance Officer (Nhân viên tài chính); Admin · Phụ: — | `Category`, `CategoryProposal`, `SystemLog` | Sprint 2 | TLCN v1.0 |
| **UC-CAT-15** | Cấu hình bộ thuộc tính chuẩn theo ngành hàng (Category Attributes) | 02. Danh mục, Sản phẩm & Tìm kiếm | Chính: Admin; Moderator (Nhân viên kiểm duyệt) · Phụ: — | `CategoryAttribute`, `Category`, `Product` | Sprint 7 | Đưa lại từ DATN |
| **UC-CAT-16** | Xem trang gian hàng (Shop Page) | 02. Danh mục, Sản phẩm & Tìm kiếm | Chính: Guest (khách vãng lai); Customer (khách hàng) · Phụ: — | `VendorProfile`, `Product`, `ProductReview`, `ShopFollow`, `VendorShippingConfig` | Sprint 2 | TLCN v1.0 |
| **UC-CAT-17** | Người bán đề xuất danh mục mới & Duyệt đề xuất | 02. Danh mục, Sản phẩm & Tìm kiếm | Chính: Vendor (người bán/shop); Moderator (Nhân viên kiểm duyệt); Admin · Phụ: — | `CategoryProposal`, `Category`, `Notification` | Sprint 7 | Mới (thêm cả DATN) |
| **UC-CAT-18** | Chia sẻ sản phẩm bằng liên kết (Deeplink) | 02. Danh mục, Sản phẩm & Tìm kiếm | Chính: Guest (khách vãng lai); Customer (khách hàng) · Phụ: — | `Product`, `ProductViewStat` | Sprint 7 | Mới (thêm cả DATN) |
| **UC-ORD-01** | Quản lý giỏ hàng mua sắm cá nhân (Shopping Cart) | 03. Giỏ hàng, Đặt hàng & Đơn hàng | Chính: Guest (khách vãng lai); Customer (khách hàng) · Phụ: — | `Cart`, `CartItem` | Sprint 3 | TLCN v1.0 |
| **UC-ORD-04** | Xem trước đơn hàng & Tách giỏ hàng đa nhà bán (Cart Preview & Splitting) | 03. Giỏ hàng, Đặt hàng & Đơn hàng | Chính: Customer (khách hàng) · Phụ: Đối tác vận chuyển (GHN, GHTK, ViettelPost, J&T) | `CheckoutSession`, `VendorProfile`, `Address` | Sprint 3 | TLCN v1.0 |
| **UC-ORD-06** | Tính phí vận chuyển qua GHN API | 03. Giỏ hàng, Đặt hàng & Đơn hàng | Chính: — · Phụ: Đối tác vận chuyển (GHN, GHTK, ViettelPost, J&T) | `VendorProfile`, `Address`, `Product`, `VendorShippingConfig` | Sprint 3 | TLCN v1.0 |
| **UC-ORD-07** | Tạo phiên Checkout & Các Đơn hàng con theo Shop (PersistCheckout) | 03. Giỏ hàng, Đặt hàng & Đơn hàng | Chính: Customer (khách hàng) · Phụ: — | `CheckoutSession`, `Order`, `OrderItem`, `OrderDiscount`, `UserCoin`, `FlashSaleItem` | Sprint 3 | TLCN v1.0 |
| **UC-ORD-08** | Tự động hủy đơn quá hạn thanh toán & Hoàn kho (Sweeper Cron) | 03. Giỏ hàng, Đặt hàng & Đơn hàng | Chính: Platform Cron / Scheduler · Phụ: — | `CheckoutSession`, `Order`, `StockReservation` | Sprint 3 | TLCN v1.0 |
| **UC-ORD-09** | Khách hàng theo dõi chi tiết hành trình đơn hàng | 03. Giỏ hàng, Đặt hàng & Đơn hàng | Chính: Customer (khách hàng) · Phụ: Đối tác vận chuyển (GHN, GHTK, ViettelPost, J&T) | `Order`, `ShipmentTracking`, `ShipmentEvent` | Sprint 4 | TLCN v1.0 |
| **UC-ORD-10** | Khách hàng yêu cầu hủy đơn hàng | 03. Giỏ hàng, Đặt hàng & Đơn hàng | Chính: Customer (khách hàng) · Phụ: Payment Gateway (VNPay, SePay) | `Order`, `EscrowLedger` | Sprint 4 | TLCN v1.0 |
| **UC-ORD-11** | Khách hàng xác nhận "Đã nhận hàng thành công" | 03. Giỏ hàng, Đặt hàng & Đơn hàng | Chính: Customer (khách hàng) · Phụ: — | `Order`, `EscrowLedger`, `VendorWallet`, `WalletTransaction` | Sprint 4 | TLCN v1.0 |
| **UC-ORD-12** | Hệ thống tự động hoàn tất đơn sau 7 ngày từ khi Delivered | 03. Giỏ hàng, Đặt hàng & Đơn hàng | Chính: Platform Cron / Scheduler · Phụ: — | `Order`, `EscrowLedger`, `RefundRequest` | Sprint 4 | TLCN v1.0 |
| **UC-PAY-01** | Khởi tạo thanh toán trực tuyến (VNPay / chuyển khoản VietQR qua SePay) | 04. Thanh toán, Ký quỹ (Escrow) & Sổ cái | Chính: Customer (khách hàng) · Phụ: Payment Gateway (VNPay, SePay) | `PaymentTransaction`, `CheckoutSession` | Sprint 3 | TLCN v1.0 |
| **UC-PAY-03** | Xử lý IPN VNPay & webhook SePay idempotent | 04. Thanh toán, Ký quỹ (Escrow) & Sổ cái | Chính: Payment Gateway (VNPay, SePay) · Phụ: — | `PaymentTransaction`, `Order`, `AdminNotification` | Sprint 3 | TLCN v1.0 |
| **UC-PAY-05** | Ghi nhận ký quỹ (Escrow) & Khấu trừ hoa hồng | 04. Thanh toán, Ký quỹ (Escrow) & Sổ cái | Chính: — · Phụ: — | `EscrowLedger`, `OrderItem`, `VendorProfile`, `Category` | Sprint 3 | TLCN v1.0 |
| **UC-PAY-06** | Ghi sổ cái tài chính kép bất biến (Financial Ledger) | 04. Thanh toán, Ký quỹ (Escrow) & Sổ cái | Chính: — · Phụ: — | `FinancialLedger`, `EscrowLedger`, `PaymentTransaction` | Sprint 3 | Đưa lại từ DATN |
| **UC-PAY-07** | Tự động giải phóng tiền ký quỹ về ví người bán | 04. Thanh toán, Ký quỹ (Escrow) & Sổ cái | Chính: Platform Cron / Scheduler · Phụ: — | `EscrowLedger`, `VendorWallet`, `WalletTransaction`, `FinancialLedger` | Sprint 4 | TLCN v1.0 |
| **UC-VND-01** | Đăng ký mở gian hàng (Become a Seller) | 05. Gian hàng & Thẩm định KYC người bán | Chính: Customer (khách hàng) · Phụ: — | `VendorProfile`, `User` | Sprint 2 | TLCN v1.0 |
| **UC-VND-02** | Tải hồ sơ pháp lý & Giấy tờ định danh KYC | 05. Gian hàng & Thẩm định KYC người bán | Chính: Vendor (người bán/shop) · Phụ: — | `VendorProfile`, `AdminNotification` | Sprint 2 | TLCN v1.0 |
| **UC-VND-03** | Thẩm định KYC tự động qua AI (QR, MRZ & Face Match) | 05. Gian hàng & Thẩm định KYC người bán | Chính: AI Worker Service (KYC & PhoBERT) · Phụ: — | `KycResult`, `VendorProfile` | Sprint 2 | TLCN v1.0 |
| **UC-VND-04** | Kiểm duyệt thủ công hồ sơ KYC (Staff / Admin) | 05. Gian hàng & Thẩm định KYC người bán | Chính: Moderator (Nhân viên kiểm duyệt); Admin · Phụ: Email Service | `VendorProfile`, `KycResult` | Sprint 2 | TLCN v1.0 |
| **UC-VND-05** | Cấu hình kho lấy hàng & Đồng bộ GHN Shop ID | 05. Gian hàng & Thẩm định KYC người bán | Chính: Vendor (người bán/shop) · Phụ: Đối tác vận chuyển (GHN, GHTK, ViettelPost, J&T) | `VendorProfile` | Sprint 2 | TLCN v1.0 |
| **UC-VND-06** | Cấu hình tin nhắn trả lời tự động (Auto-reply) | 05. Gian hàng & Thẩm định KYC người bán | Chính: Vendor (người bán/shop) · Phụ: — | `VendorProfile`, `Message` | Sprint 7 | Đưa lại từ DATN |
| **UC-VND-07** | Đăng ký tài khoản nhận giải ngân (Payout Bank Account) | 05. Gian hàng & Thẩm định KYC người bán | Chính: Vendor (người bán/shop) · Phụ: Email Service | `VendorProfile` | Sprint 4 | TLCN v1.0 |
| **UC-VND-08** | Xem báo cáo doanh thu & Phân tích bán hàng (Vendor Analytics) | 05. Gian hàng & Thẩm định KYC người bán | Chính: Vendor (người bán/shop) · Phụ: — | `Order`, `OrderItem`, `EscrowLedger`, `ProductViewStat` | Sprint 6 | TLCN v1.0 |
| **UC-VND-19** | Bật chế độ tạm nghỉ bán hàng của shop (Vacation Mode) | 05. Gian hàng & Thẩm định KYC người bán | Chính: Vendor (người bán/shop) · Phụ: Platform Cron / Scheduler | `VendorProfile` | Sprint 7 | Đưa lại từ DATN |
| **UC-VND-20** | Xem ví & sổ giao dịch người bán (Seller Wallet & Escrow Ledger) | 05. Gian hàng & Thẩm định KYC người bán | Chính: Vendor (người bán/shop) · Phụ: — | `VendorWallet`, `WalletTransaction`, `EscrowLedger`, `WithdrawalRequest` | Sprint 7 | Mới (thêm cả DATN) |
| **UC-VND-21** | Cập nhật thông tin gian hàng & cài đặt vận hành | 05. Gian hàng & Thẩm định KYC người bán | Chính: Vendor (người bán/shop) · Phụ: Silo S3 Storage | `VendorProfile`, `VendorShippingConfig` | Sprint 7 | Mới (thêm cả DATN) |
| **UC-LOG-01** | Tiếp nhận đơn hàng & Đóng gói tại kho shop (Fulfillment) | 06. Vận chuyển qua đối tác 3PL | Chính: Vendor (người bán/shop) · Phụ: — | `Order`, `OrderItem` | Sprint 4 | TLCN v1.0 |
| **UC-LOG-02** | Đẩy vận đơn sang GHN & In phiếu gửi hàng PDF (Waybill Label) | 06. Vận chuyển qua đối tác 3PL | Chính: Vendor (người bán/shop) · Phụ: Đối tác vận chuyển (GHN, GHTK, ViettelPost, J&T) | `Order` | Sprint 4 | TLCN v1.0 |
| **UC-LOG-03** | Đồng bộ hành trình giao nhận qua Webhook GHN | 06. Vận chuyển qua đối tác 3PL | Chính: Đối tác vận chuyển (GHN, GHTK, ViettelPost, J&T) · Phụ: Finance Officer (Nhân viên tài chính); Payment Gateway (VNPay, SePay) | `Order`, `ShipmentTracking`, `ShipmentEvent` | Sprint 4 | TLCN v1.0 |
| **UC-LOG-15** | Điều phối đa hãng vận chuyển (Dynamic Carrier Routing) | 06. Vận chuyển qua đối tác 3PL | Chính: — · Phụ: Đối tác vận chuyển (GHN, GHTK, ViettelPost, J&T) | `VendorShippingConfig`, `ShipmentTracking`, `Order` | Sprint 7 | Đưa lại từ DATN |
| **UC-LOG-16** | Người bán xác nhận / từ chối đơn hàng mới | 06. Vận chuyển qua đối tác 3PL | Chính: Vendor (người bán/shop) · Phụ: Platform Cron / Scheduler | `Order`, `OrderItem`, `StockReservation`, `Notification` | Sprint 4 | Mới (thêm cả DATN) |
| **UC-PRO-01** | Tạo & Quản lý Voucher Gian hàng (Vendor Shop Vouchers) | 07. Khuyến mãi, Voucher, Flash Sale & Điểm | Chính: Vendor (người bán/shop) · Phụ: — | `Voucher`, `VoucherCategory` | Sprint 5 | TLCN v1.0 |
| **UC-PRO-02** | Tạo & Quản lý Voucher Toàn sàn | 07. Khuyến mãi, Voucher, Flash Sale & Điểm | Chính: Finance Officer (Nhân viên tài chính); Admin · Phụ: — | `Voucher`, `VoucherCategory` | Sprint 5 | TLCN v1.0 |
| **UC-PRO-03** | Khách hàng thu thập Voucher vào Kho Voucher cá nhân | 07. Khuyến mãi, Voucher, Flash Sale & Điểm | Chính: Customer (khách hàng) · Phụ: — | `UserVoucher`, `Voucher` | Sprint 5 | TLCN v1.0 |
| **UC-PRO-04** | Thuật toán Stacking Voucher 3 Tầng & Điểm tự động khi Checkout | 07. Khuyến mãi, Voucher, Flash Sale & Điểm | Chính: Customer (khách hàng) · Phụ: — | `Voucher`, `VoucherCategory`, `UserVoucher`, `VoucherUsage`, `OrderDiscount`, `OrderItem` | Sprint 8 | Đưa lại từ DATN |
| **UC-PRO-05** | Tạo chiến dịch Flash Sale & Cấu hình khung giờ vàng | 07. Khuyến mãi, Voucher, Flash Sale & Điểm | Chính: Finance Officer (Nhân viên tài chính); Admin; Vendor (người bán/shop) · Phụ: Platform Cron / Scheduler | `FlashSaleCampaign`, `FlashSaleItem`, `Product` | Sprint 8 | Đưa lại từ DATN |
| **UC-PRO-06** | Mua hàng Flash Sale chống bán vượt suất | 07. Khuyến mãi, Voucher, Flash Sale & Điểm | Chính: Customer (khách hàng) · Phụ: — | `FlashSaleItem`, `FlashSaleCampaign`, `Order`, `OrderItem`, `StockReservation` | Sprint 8 | Đưa lại từ DATN |
| **UC-PRO-08** | Điểm danh nhận Điểm hàng ngày & Duy trì chuỗi điểm danh (Streak) | 07. Khuyến mãi, Voucher, Flash Sale & Điểm | Chính: Customer (khách hàng) · Phụ: — | `DailyCheckin`, `UserCoin`, `CoinTransaction` | Sprint 8 | Đưa lại từ DATN |
| **UC-PRO-09** | Tra cứu số dư Điểm Genie & Lịch sử biến động Điểm | 07. Khuyến mãi, Voucher, Flash Sale & Điểm | Chính: Customer (khách hàng) · Phụ: — | `UserCoin`, `CoinTransaction`, `UserVoucher` | Sprint 8 | Đưa lại từ DATN |
| **UC-PRO-10** | Sử dụng Điểm Genie để giảm trừ thanh toán đơn hàng | 07. Khuyến mãi, Voucher, Flash Sale & Điểm | Chính: Customer (khách hàng) · Phụ: — | `UserCoin`, `CoinTransaction`, `Order`, `OrderItem` | Sprint 8 | Đưa lại từ DATN |
| **UC-SOC-01** | Nhắn tin trò chuyện thời gian thực Khách - Shop (Live Chat) | 08. Chat, Đánh giá & Theo dõi shop | Chính: Customer (khách hàng); Vendor (người bán/shop) · Phụ: — | `Conversation`, `Message` | Sprint 6 | TLCN v1.0 |
| **UC-SOC-02** | Gửi thẻ sản phẩm, hình ảnh và trạng thái đơn hàng trong chat | 08. Chat, Đánh giá & Theo dõi shop | Chính: Customer (khách hàng); Vendor (người bán/shop) · Phụ: Silo S3 Storage | `Message`, `Conversation`, `ProductVariant`, `Order` | Sprint 8 | Đưa lại từ DATN |
| **UC-SOC-03** | Đánh giá sản phẩm đã mua & Tải ảnh thực tế (Verified Buyer Review) | 08. Chat, Đánh giá & Theo dõi shop | Chính: Customer (khách hàng) · Phụ: — | `ProductReview`, `Product`, `ReviewHelpfulVote`, `CoinTransaction` | Sprint 5 | TLCN v1.0 |
| **UC-SOC-04** | Người bán phản hồi đánh giá của khách hàng (Vendor Review Reply) | 08. Chat, Đánh giá & Theo dõi shop | Chính: Vendor (người bán/shop) · Phụ: — | `ProductReview` | Sprint 5 | TLCN v1.0 |
| **UC-SOC-05** | Báo cáo đánh giá vi phạm / spam & Kiểm duyệt đánh giá (Review Moderation & Report) | 08. Chat, Đánh giá & Theo dõi shop | Chính: Vendor (người bán/shop); Customer (khách hàng); Moderator (Nhân viên kiểm duyệt); CS Specialist (Nhân viên CSKH) · Phụ: — | `ReviewReport`, `ProductReview`, `VendorProfile`, `SystemLog` | Sprint 8 | Đưa lại từ DATN |
| **UC-SOC-06** | Khách hàng theo dõi gian hàng (Shop Follow) | 08. Chat, Đánh giá & Theo dõi shop | Chính: Customer (khách hàng) · Phụ: — | `ShopFollow`, `VendorProfile`, `Notification` | Sprint 8 | Đưa lại từ DATN |
| **UC-AI-07** | Phân tích cảm xúc đánh giá bằng PhoBERT | 08. Chat, Đánh giá & Theo dõi shop | Chính: AI Worker Service (KYC & PhoBERT) · Phụ: Vendor (người bán/shop); Moderator (Nhân viên kiểm duyệt); CS Specialist (Nhân viên CSKH) | `ProductReview` | Sprint 6 | Đưa lại từ DATN (rút gọn) |
| **UC-SUP-01** | Tạo yêu cầu Trả hàng / Hoàn tiền (Return & Refund Request) | 09. Trả hàng, Hoàn tiền & Khiếu nại | Chính: Customer (khách hàng) · Phụ: — | `RefundRequest`, `RefundRequestItem`, `RefundEvidence` | Sprint 5 | TLCN v1.0 |
| **UC-SUP-02** | Người bán tiếp nhận và xử lý yêu cầu trả hàng | 09. Trả hàng, Hoàn tiền & Khiếu nại | Chính: Vendor (người bán/shop) · Phụ: — | `RefundRequest`, `ShipmentTracking` | Sprint 5 | TLCN v1.0 |
| **UC-SUP-03** | Nhân viên sàn phân xử tranh chấp khiếu nại (Dispute Arbitration) | 09. Trả hàng, Hoàn tiền & Khiếu nại | Chính: CS Specialist (Nhân viên CSKH); Admin · Phụ: — | `RefundRequest`, `EscrowLedger` | Sprint 5 | TLCN v1.0 |
| **UC-SUP-04** | Thực hiện hoàn tiền (VNPay / VietQR / COD) | 09. Trả hàng, Hoàn tiền & Khiếu nại | Chính: CS Specialist (Nhân viên CSKH) · Phụ: Payment Gateway (VNPay, SePay) | `RefundRequest`, `PaymentRefund`, `PaymentTransaction`, `EscrowLedger`, `VendorWallet` | Sprint 5 | TLCN v1.0 |
| **UC-NOT-01** | Thông báo trong ứng dụng thời gian thực (WebSocket) | 10. Thông báo | Chính: Customer (khách hàng); Vendor (người bán/shop); Staff (Nhân viên sàn) · Phụ: — | `Notification`, `AdminNotification` | Sprint 5 | TLCN v1.0 |
| **UC-NOT-03** | Gửi email giao dịch tự động | 10. Thông báo | Chính: Customer (khách hàng); Vendor (người bán/shop) · Phụ: Email Service | `SystemLog` | Sprint 5 | TLCN v1.0 |
| **UC-NOT-05** | Admin phát thông báo toàn sàn (Broadcast Platform Notification) | 10. Thông báo | Chính: Admin · Phụ: Silo S3 Storage | `PlatformNotification`, `UserPlatformNotificationRead` | Sprint 8 | Đưa lại từ DATN |
| **UC-ADM-01** | Quản lý danh sách nhân viên nội bộ & Lời mời kích hoạt | 11. Quản trị sàn, Nhân viên & Rút tiền | Chính: Admin · Phụ: Email Service | `User`, `AdminProfile`, `StaffPermission`, `SystemLog` | Sprint 1 | Đưa lại từ DATN |
| **UC-ADM-02** | Phân cấp vai trò & Cấp phát / Thu hồi mã quyền chi tiết (RBAC) | 11. Quản trị sàn, Nhân viên & Rút tiền | Chính: Admin · Phụ: — | `StaffPermission`, `AdminProfile`, `User`, `SystemLog` | Sprint 1 | Đưa lại từ DATN |
| **UC-ADM-03** | Khóa / Mở khóa tài khoản nhân viên và người dùng vi phạm | 11. Quản trị sàn, Nhân viên & Rút tiền | Chính: Admin · Phụ: — | `User` | Sprint 6 | TLCN v1.0 |
| **UC-ADM-04** | Người bán gửi yêu cầu rút tiền từ Ví về Ngân hàng | 11. Quản trị sàn, Nhân viên & Rút tiền | Chính: Vendor (người bán/shop) · Phụ: — | `WithdrawalRequest`, `VendorWallet`, `WalletTransaction`, `AdminNotification` | Sprint 4 | TLCN v1.0 |
| **UC-ADM-05** | Nhân viên sàn phê duyệt & Thực hiện lệnh chuyển khoản (Payout) | 11. Quản trị sàn, Nhân viên & Rút tiền | Chính: Finance Officer (Nhân viên tài chính); Admin · Phụ: Payment Gateway (VNPay, SePay); Email Service | `WithdrawalRequest`, `VendorWallet`, `WalletTransaction` | Sprint 4 | TLCN v1.0 |
| **UC-ADM-06** | Dashboard doanh thu & ký quỹ toàn sàn | 11. Quản trị sàn, Nhân viên & Rút tiền | Chính: Admin; Staff (Nhân viên sàn) · Phụ: — | `Order`, `EscrowLedger`, `OrderDiscount`, `VendorProfile`, `FinancialLedger`, `VendorWallet` | Sprint 6 | TLCN v1.0 |
| **UC-ADM-07** | Tra cứu nhật ký kiểm toán hệ thống bất biến (System Audit Logs) | 11. Quản trị sàn, Nhân viên & Rút tiền | Chính: Admin · Phụ: — | `SystemLog` | Sprint 7 | Đưa lại từ DATN |
| **UC-ADM-12** | Quản lý banner trang chủ & chiến dịch tĩnh (CMS Banner) | 11. Quản trị sàn, Nhân viên & Rút tiền | Chính: Admin; Finance Officer (Nhân viên tài chính) · Phụ: Silo S3 Storage | `HomeBanner`, `Category` | Sprint 8 | Đưa lại từ DATN |

---

<a id="phan-iii"></a>
# PHẦN III: ĐẶC TẢ CHI TIẾT 87 USE CASE THEO 11 PHÂN HỆ

<a id="ph-auth"></a>
## PHÂN HỆ 01: XÁC THỰC & TÀI KHOẢN NGƯỜI DÙNG (AUTHENTICATION & ACCOUNT)

> **Phạm vi:** Đăng ký (OTP email), đăng nhập (mật khẩu / OTP email / Google; Apple để sau cờ cấu hình), mở khóa qua email, quên / đổi / thiết lập mật khẩu, đăng xuất & quản lý thiết bị, hồ sơ cá nhân & đổi email, liên kết đăng nhập Google, sổ địa chỉ chuẩn GHN, xóa tài khoản.
> **Use case (9):** `UC-AUTH-01`, `UC-AUTH-02`, `UC-AUTH-03`, `UC-AUTH-04`, `UC-AUTH-06`, `UC-AUTH-08`, `UC-AUTH-09`, `UC-AUTH-10`, `UC-AUTH-11`
> **Đồng bộ với mã nguồn (TLCN v1.4 — 2026-11-16):** đặc tả dưới đây đã chỉnh theo phần đã hiện thực ở `genie-multivendor-platform` (module `auth`, plan `plan/2026-11-14/auth.md` quyết định D2–D22, ADR 0007 / 0008 / 0009). Quy ước chung của cả phân hệ:
> - Email luôn chuẩn hóa `trim` + chữ thường trước khi ghi / tìm; CSDL có thêm `uk_users_email_lower` (UNIQUE `lower(email)`).
> - Mật khẩu băm **Argon2id** tham số OWASP 2024: `m = 19456 KiB, t = 2, p = 1`, muối 16 B, băm 32 B. Chính sách: 8–72 ký tự, có chữ hoa, chữ thường, chữ số, ký tự đặc biệt; sai → `422 GNE-VAL-4220` (`errors[].code = PASSWORD_TOO_WEAK`).
> - OTP dùng chung: khóa Redis `otp:challenge:{challengeId}` (mục đích, email, user, bản băm SHA-256 + pepper của mã, số lần sai) TTL 300 giây, mỗi email / mục đích chỉ một mã còn sống (`otp:active:{purpose}:{email}`); gửi lại sau 60 giây và tối đa 5 lần / giờ (`429 GNE-RAT-4290` kèm `Retry-After`); sai mã → `422 GNE-AUTH-4222` kèm `remaining_attempts`; sai lần thứ 5 → `422 GNE-AUTH-4221`; hết hạn → `410 GNE-AUTH-4101`. Thao tác nhạy cảm khi đã đăng nhập (đổi mật khẩu, xóa tài khoản…) dùng **OTP bước 2** (`POST /me/otp-challenges`, header `X-OTP-Challenge-Id` + `X-OTP-Code`).
> - Phiên: access token JWT HS256 15 phút (claim `sub`, `role`, `sid` = mã phiên, `jti`); refresh token 7 ngày ngẫu nhiên 48 byte, chỉ lưu SHA-256, gửi bằng cookie `genie_rt` (`HttpOnly; SameSite=Strict; Path=/api/v1/auth`). Mỗi phiên (thiết bị) có `family_id` ổn định qua các lần xoay vòng.
> - Khóa tạm do sai mật khẩu / OTP **không** đổi `status` (vẫn `ACTIVE`), chỉ đặt `lockedUntil`; `LOCKED` không dùng trong TLCN. Lý do khóa (`PASSWORD` / `OTP`) lưu Redis `auth:locked_by:{userId}`.
> - Mọi thao tác nhạy cảm ghi `system_logs` (thêm 8 hành động: `PASSWORD_CHANGED`, `PASSWORD_RESET`, `EMAIL_CHANGED`, `OAUTH_LINKED`, `OAUTH_UNLINKED`, `SESSION_REVOKED`, `ACCOUNT_DELETION_REQUESTED`, `ACCOUNT_DELETION_CANCELLED`).
> - Email gửi qua cổng `common.mail.EmailSender` (SMTP + mẫu Thymeleaf, thử lại 3 lần, timeout 10 giây); email OTP gửi đồng bộ, email cảnh báo gửi sau khi transaction commit.

### UC-AUTH-01: Đăng ký tài khoản Khách hàng
- **Mã Use Case:** `UC-AUTH-01`
- **Tác nhân chính:** `ACT-GST` Guest (khách vãng lai)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Cho phép người dùng vãng lai tạo tài khoản mới với vai trò Customer để mua hàng, lưu hồ sơ cá nhân và nhận khuyến mãi.
- **Tiền điều kiện:** Email (đã chuẩn hóa chữ thường) chưa thuộc tài khoản nào trong `users` ở mọi trạng thái trừ `ANONYMIZED`.
- **Hậu điều kiện:** Bản ghi `users` được tạo với `role = CUSTOMER`, `status = ACTIVE`, `emailVerifiedAt = now()`; `customer_profiles` được khởi tạo (họ tên, số điện thoại, ngôn ngữ theo `Accept-Language`); người dùng được đăng nhập luôn (cặp token như UC-AUTH-02).
- **Luồng sự kiện chính:**
  1. Guest mở `/auth/register`, nhập Họ tên, Email, Số điện thoại, Mật khẩu và tích đồng ý điều khoản. Mật khẩu chỉ truyền qua HTTPS (TLS) — không mã hóa thêm phía client.
  2. Client gửi `POST /api/v1/auth/registrations` (`full_name`, `email`, `phone_number` dạng `^0\d{9,10}$`, `password`, `accept_terms = true`); hệ thống kiểm tra định dạng và chính sách mật khẩu.
  3. Hệ thống kiểm tra email chưa được đăng ký.
  4. Hệ thống băm mật khẩu bằng Argon2id **trước** khi lưu dữ liệu đăng ký tạm vào Redis `register:temp:{email}` (họ tên, số điện thoại, bản băm mật khẩu) với TTL **15 phút** (đủ cho nhiều lần gửi lại OTP trong một lượt đăng ký); đăng ký lại cùng email thì ghi đè.
  5. Hệ thống phát OTP mục đích `REGISTER` (6 chữ số, TTL 5 phút) và gửi email mã qua dịch vụ Mailer; trả `202` kèm `$OtpChallenge` (`challenge_id`, `email_masked`, `expires_in = 300`, `resend_after = 60`).
  6. Giao diện hiển thị màn hình nhập OTP kèm bộ đếm ngược.
  7. Guest nhập mã 6 số → `POST /api/v1/auth/otp-challenges/{id}/verify`.
  8. Mã đúng: hệ thống đọc dữ liệu tạm, mở transaction kiểm tra lại email chưa tồn tại (hai lần xác minh song song chỉ tạo một tài khoản), tạo `users` + `customer_profiles`, xóa dữ liệu tạm và mã OTP (mã dùng một lần).
  9. Hệ thống cấp phiên đăng nhập (`201` kèm `$AuthTokens` + cookie `genie_rt`), phát sự kiện `UserRegisteredEvent` (để dành tặng voucher chào mừng) và đưa người dùng vào trang chủ đã đăng nhập.
- **Luồng thay thế / Ngoại lệ:**
  - *2a. Mật khẩu không đủ độ mạnh / dữ liệu sai định dạng:* `422 GNE-VAL-4220` kèm `errors[]` theo từng trường; giao diện hiện lỗi dưới ô tương ứng.
  - *3a. Email đã tồn tại:* `409 GNE-AUTH-4091`. Giao diện mở hộp thoại "Email này đã được đăng ký" kèm nút **Đi tới đăng nhập** (và gợi ý Quên mật khẩu).
  - *6a. Người dùng bấm Gửi lại OTP:* `POST /auth/otp-challenges/{id}/resend` — tối thiểu 60 giây giữa 2 lần, tối đa 5 lần / giờ; hợp lệ thì sinh mã mới, đặt lại thời hạn 5 phút.
  - *7a. Nhập sai OTP:* báo số lần còn lại; sai lần thứ 5 → hủy mã **và** xóa `register:temp:{email}`, người dùng đăng ký lại từ đầu.
  - *7b. OTP hết hạn (> 5 phút) hoặc dữ liệu tạm đã hết 15 phút:* `410 GNE-AUTH-4101`, yêu cầu gửi lại mã / đăng ký lại.
- **Quan hệ Use Case:** `<<include>>` Gửi mã OTP xác thực — tác nhân phụ: `ACT-EML` Email Service.
- **Ràng buộc nghiệp vụ & Kỹ thuật:**
  - Bảo mật mật khẩu: Argon2id (OWASP 2024, `m = 19456 KiB, t = 2, p = 1`); không lưu mật khẩu thô dưới bất kỳ hình thức nào, kể cả trong Redis.
  - Dữ liệu tạm chỉ ở Redis có TTL tự hủy — không ghi rác vào PostgreSQL 16 nếu người dùng bỏ dở bước nhập OTP. Trạng thái `UNVERIFIED` **không** dùng cho đăng ký thường (để dành cho tài khoản nhân viên được mời chưa kích hoạt).
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* **Đã triển khai** — backend (`auth/register`, `auth/otp`) + test `RegisterFlowIT`; web nối API thật (cờ `PUBLIC_REAL_API=auth`); đã chạy thật trên máy dev. Còn: bộ kịch bản `auth.http`, E2E với API thật.
  - *Kế hoạch:* Sprint 1 (tuần 1–2) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [User](../ClassDiagram/ENTITY.md#11-user--tài-khoản-đăng-nhập), [CustomerProfile](../ClassDiagram/ENTITY.md#12-customerprofile--hồ-sơ-khách-hàng).

---

### UC-AUTH-02: Đăng nhập hệ thống
- **Mã Use Case:** `UC-AUTH-02`
- **Tác nhân chính:** `ACT-CUS` Customer (khách hàng); `ACT-VND` Vendor (người bán/shop); `ACT-STF` Staff (Nhân viên sàn); `ACT-ADM` Admin
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Xác thực danh tính người dùng bằng email + mật khẩu (hoặc mã OTP email) để cấp quyền truy cập hệ thống theo đúng vai trò.
- **Tiền điều kiện:** Tài khoản đã được tạo trong bảng `users` và có trạng thái `ACTIVE`.
- **Hậu điều kiện:** Cấp Access Token (JWT) và Refresh Token (cookie `genie_rt`) cho một phiên mới; ghi nhật ký đăng nhập trong `system_logs`.
- **Luồng sự kiện chính:**
  1. Người dùng nhập Email và Mật khẩu tại `/auth/login`.
  2. Client gửi `POST /api/v1/auth/sessions` qua HTTPS (TLS); hệ thống giới hạn 10 lần / phút / IP (`rl:login:{ip}`).
  3. Server tìm `users` theo email đã chuẩn hóa.
  4. Hệ thống kiểm tra trạng thái: đình chỉ đã hết hạn thì tự mở lại (`reactivateIfSuspensionExpired`); đang bị khóa tạm (`lockedUntil > now`) hoặc đình chỉ thì từ chối (luồng 4a / 4b).
  5. Hệ thống so khớp mật khẩu với bản băm Argon2id trong `users.password` (thời gian xử lý đồng đều — có băm giả khi không tìm thấy email); nếu tham số Argon2 đã cũ thì băm lại.
  6. Hệ thống reset `failedLoginAttempts = 0`, tạo phiên mới: bản ghi `refresh_tokens` (lưu SHA-256, `familyId` = id của chính nó, hạn 7 ngày, thiết bị = User-Agent cắt 255 ký tự, IP) và Access Token JWT HS256 15 phút (`sub`, `role`, `sid` = `familyId`, `jti`).
  7. Hệ thống trả `201` kèm `$AuthTokens` (`access_token`, `expires_in`, `user`) và đặt cookie `genie_rt`.
  8. Hệ thống ghi `system_logs` `LOGIN_SUCCESS` (IP, User-Agent).
  9. Khi access token hết hạn, client gọi `POST /api/v1/auth/sessions/refresh` (đọc cookie): hệ thống thu hồi token cũ (`ROTATED`), cấp token mới **cùng `familyId`**, cập nhật `lastUsedAt` và trả kèm `user` (để web dựng lại phiên khi tải lại trang).
- **Luồng thay thế / Ngoại lệ:**
  - *3a. Email không tồn tại / tài khoản đã ẩn danh:* `401 GNE-AUTH-4011` "Email hoặc mật khẩu không chính xác" (cùng mã với sai mật khẩu — chống dò email).
  - *4a. Tài khoản đang bị khóa tạm (`lockedUntil > now`):* `423 GNE-AUTH-4230` kèm `locked_until`, `locked_by` (`PASSWORD` / `OTP`); giao diện mở màn "Tài khoản tạm khóa" với đếm ngược và nút **Mở khóa qua email**.
  - *4b. Tài khoản bị Admin đình chỉ (`status = SUSPENDED`):* `403 GNE-AUTH-4031` kèm lý do (`suspensionReason`) và thời hạn.
  - *4c. Tài khoản chỉ có Google / Apple (`password = NULL`):* `401 GNE-AUTH-4012` — **không** tính là một lần sai; giao diện gợi ý "Đăng nhập bằng Google", "Đăng nhập bằng mã email" hoặc "Thiết lập mật khẩu" (qua Quên mật khẩu — UC-AUTH-04).
  - *5a. Mật khẩu không chính xác:* `failedLoginAttempts += 1`, trả `401 GNE-AUTH-4011` kèm `remaining_attempts`. Lần sai thứ 5 → `User.recordFailedLogin()` đặt `lockedUntil = now + 15 phút` (trạng thái vẫn `ACTIVE`), ghi `auth:locked_by = PASSWORD`, `system_logs` `ACCOUNT_LOCKED`, trả `423`.
  - *1a. Đăng nhập bằng mã OTP email (không mật khẩu):* chọn "Đăng nhập bằng OTP", nhập email → `POST /api/v1/auth/otp-challenges` (`purpose = LOGIN`; email không tồn tại / không hoạt động vẫn trả `202` nhưng không gửi thư) → `POST /api/v1/auth/otp-challenges/{id}/verify` → kiểm tra như bước 4 → `200` kèm phiên. Sai 5 lần → khóa 15 phút (`locked_by = OTP`), `422 GNE-AUTH-4221` kèm `locked_until`.
  - *4d. Mở khóa sớm qua email:* trên màn tạm khóa bấm "Mở khóa qua email" → OTP `purpose = ACCOUNT_UNLOCK` (chỉ khi đang bị khóa, ngược lại `409 GNE-AUTH-4096`; đình chỉ → `403 GNE-AUTH-4031`) → mã đúng: xóa khóa, ghi `ACCOUNT_REACTIVATED`, cấp phiên.
  - *8a. Đăng nhập từ thiết bị / mạng chưa từng dùng:* nếu 90 ngày gần nhất chưa có phiên cùng "trình duyệt + hệ điều hành" **và** cùng dải IP /24 → gửi email cảnh báo `NEW_DEVICE_LOGIN` ("Đăng nhập mới trên thiết bị lạ — nếu không phải bạn, hãy đổi mật khẩu ngay"); thông báo in-app bổ sung khi có phân hệ Thông báo. Lần đăng nhập đầu tiên ngay sau đăng ký không báo.
  - *9a. Refresh token đã bị xoay vòng được dùng lại (nghi bị đánh cắp):* trong 10 giây ân hạn (hai tab làm mới cùng lúc) chỉ trả `401`; quá 10 giây → thu hồi **mọi** phiên của người dùng (`REUSE_DETECTED`), đặt `auth:revoked_before:{userId}` để chặn ngay các access token cũ, xóa cookie, trả `401 GNE-SEC-4010`.
- **Quan hệ Use Case:** `<<include>>` Ghi nhật ký kiểm toán; được mở rộng bởi `UC-AUTH-03` (điểm «Chọn đăng nhập mạng xã hội»).
- **Ràng buộc nghiệp vụ & Kỹ thuật:**
  - Chống Timing Attack: thời gian xử lý đồng đều kể cả khi email không tồn tại.
  - Không đưa email / quyền vào JWT; quyền nhân viên nạp lại mỗi request qua cổng `AuthorityProvider` (cache 60 giây). Kế thừa vai trò: `VENDOR` có quyền của `CUSTOMER`, `ADMIN` có quyền của `STAFF`.
  - Job dọn `refresh_tokens` 03:00 hằng ngày, giữ lịch sử 90 ngày (phục vụ phát hiện thiết bị lạ).
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* **Đã triển khai** — backend (`auth/login`, `auth/token`, `auth/otp`) + test `LoginFlowIT`, `SessionFlowIT`; web nối API thật (cờ `auth`). Còn: `GET /me` để web dựng đủ `SessionUser` (vai trò, shop, quyền), E2E với API thật.
  - *Kế hoạch:* Sprint 1 (tuần 1–2) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [User](../ClassDiagram/ENTITY.md#11-user--tài-khoản-đăng-nhập), [RefreshToken](../ClassDiagram/ENTITY.md#17-refreshtoken--phiên-đăng-nhập), [SystemLog](../ClassDiagram/ENTITY.md#121-systemlog--nhật-ký-kiểm-toán), [Notification](../ClassDiagram/ENTITY.md#101-notification--thông-báo-cá-nhân).

---

### UC-AUTH-03: Đăng nhập & Liên kết tài khoản Google / Apple (OAuth2 / OpenID Connect)
- **Mã Use Case:** `UC-AUTH-03`
- **Tác nhân chính:** `ACT-GST` Guest (khách vãng lai); `ACT-CUS` Customer (khách hàng)
- **Tác nhân phụ:** `ACT-IDP` Identity Provider (Google, Apple)
- **Mục đích nghiệp vụ:** Cho phép người dùng đăng nhập hoặc tạo nhanh tài khoản bằng Google (Apple để sau) mà không cần nhớ mật khẩu riêng — nút "Tiếp tục với Google" trên trang đăng nhập / đăng ký; và cho người đang đăng nhập **liên kết / gỡ liên kết** Google ở Tài khoản › **Liên kết đăng nhập**.
- **Tiền điều kiện:** Người dùng có tài khoản hợp lệ bên phía nhà cung cấp danh tính; Google đã cấu hình `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` (thiếu → `503`).
- **Hậu điều kiện:** Đăng nhập vào tài khoản đã liên kết, hoặc liên kết Google vào tài khoản có cùng email, hoặc tạo tài khoản mới; cấp cặp token như UC-AUTH-02.
- **Luồng sự kiện chính:**
  1. Người dùng bấm "Tiếp tục với Google" trên trang đăng nhập / đăng ký.
  2. Trình duyệt mở `GET /api/v1/auth/oauth/google/authorize?next=…`; backend sinh `state`, `nonce`, `code_verifier` (PKCE S256), lưu Redis `oauth:state:{state}` 10 phút (kèm trang cần quay về `next`), rồi `302` sang trang đồng ý Google (`scope = openid email profile`).
  3. Người dùng đồng ý; Google chuyển về địa chỉ callback **cố định** của web `/auth/oauth-callback` kèm `code` và `state`.
  4. Web gửi `POST /api/v1/auth/oauth/google/token` (`code`, `state`); backend lấy và xóa `state` (dùng một lần), đổi `code` lấy `id_token`, kiểm chữ ký theo JWKS, `iss`, `aud`, `exp`, `nonce`, trích `sub`, `email`, `email_verified`, `name`, `picture`.
  5. Backend xử lý theo thứ tự:
     - Tìm theo (`oauthProvider`, `oauthProviderId = sub`) → có: kiểm trạng thái như UC-AUTH-02 bước 4 → **`LOGGED_IN`**.
     - Không có: `email_verified` phải là `true`; tìm theo email → có tài khoản: kiểm trạng thái, gắn `oauthProvider` / `oauthProviderId` (nếu email chưa xác minh thì đánh dấu đã xác minh), ghi `OAUTH_LINKED` → **`LINKED`** — từ đó đăng nhập được bằng cả mật khẩu lẫn Google, vẫn là một tài khoản (cùng đơn hàng, ví, Điểm).
     - Không có tài khoản: tạo `users` (`password = NULL`, `role = CUSTOMER`, `status = ACTIVE`, email đã xác minh) + `customer_profiles` lấy tên Google; ảnh đại diện Google được **tải về và lưu vào Silo** (`avatar-assets`) — không lưu URL của Google → **`CREATED`**.
     - Với tài khoản đã có: tên / ảnh Google chỉ điền khi hồ sơ đang **trống** (không ghi đè dữ liệu người dùng tự nhập).
  6. Backend cấp phiên, trả `{ result, tokens, next }` + cookie; web chuyển về trang trước đó.
- **Luồng thay thế / Ngoại lệ:**
  - *4a. `state` không có / hết hạn / khác nhà cung cấp:* `400 GNE-AUTH-4001` (nghi giả mạo CSRF), yêu cầu đăng nhập lại. Lỗi khi bắt đầu → backend `302` về callback kèm `?error=`.
  - *4b. Apple "Ẩn email":* dùng email chuyển tiếp do Apple cấp làm email đăng nhập (khi bật Apple).
  - *5a. Tài khoản đã liên kết / cùng email đang bị khóa tạm hoặc đình chỉ:* `423 GNE-AUTH-4230` / `403 GNE-AUTH-4031` như UC-AUTH-02; không liên kết, không cấp phiên.
  - *5b. IdP trả `email_verified = false`:* `403 GNE-AUTH-4032` — không liên kết / không tạo tài khoản (tránh chiếm tài khoản).
  - *5c. Tài khoản cùng email đã gắn một nhà cung cấp khác:* `409 GNE-AUTH-4092` (mỗi tài khoản TLCN chỉ gắn một nhà cung cấp).
  - *Liên kết thủ công (đang đăng nhập):* Tài khoản › **Liên kết đăng nhập** › "Liên kết Google" → web gọi `POST /api/v1/me/identities/google/authorize` (XHR có access token, trả URL Google; `state` ghi kèm người đang đăng nhập) → Google → callback → `POST /api/v1/me/identities/google` (`code`, `state`) → `201` + danh sách cách đăng nhập, quay về `/account/logins?linked=google`. Quy tắc: **email Google phải trùng email tài khoản Genie** (khác → `409 GNE-AUTH-4098` kèm `account_email` — một người = một tài khoản = một email); Google đã gắn người khác → `409 GNE-AUTH-4092`; email Google chưa xác minh → `403 GNE-AUTH-4032`; `state` đăng nhập và `state` liên kết không dùng lẫn được (`400 GNE-AUTH-4001`); đã gắn đúng Google này → coi như thành công.
  - *Gỡ liên kết:* "Gỡ liên kết" → nhập lại **mật khẩu hiện tại** → `DELETE /api/v1/me/identities/google` (`current_password`) → `204`, ghi `OAUTH_UNLINKED`. Chưa có mật khẩu → `409 GNE-AUTH-4093` (giao diện yêu cầu "Thiết lập mật khẩu" trước); sai mật khẩu → `401 GNE-AUTH-4011`; tối đa 10 lần / phút.
  - *Xem cách đăng nhập:* `GET /api/v1/me/login-methods` — có mật khẩu chưa, Google đã liên kết chưa (email Google và thời điểm liên kết lấy từ bản ghi `OAUTH_LINKED` gần nhất trong `system_logs`, bảng `users` không lưu email Google).
- **Quan hệ Use Case:** `<<extend>>` Đăng nhập hệ thống (`UC-AUTH-02`) tại điểm mở rộng «Chọn đăng nhập mạng xã hội» khi {người dùng bấm “Tiếp tục với Google / Apple”}.
- **Ràng buộc nghiệp vụ & Kỹ thuật:**
  - Tài khoản chỉ đăng nhập mạng xã hội có `password = NULL`; muốn đăng nhập bằng email + mật khẩu thì "Thiết lập mật khẩu" (UC-AUTH-06 khi đang đăng nhập, hoặc UC-AUTH-04 qua OTP email).
  - Đổi email đăng nhập của tài khoản chưa có mật khẩu → `409 GNE-AUTH-4095` (phải thiết lập mật khẩu trước).
  - Một `sub` chỉ gắn với một user (UNIQUE `uk_users_oauth`); một user chỉ gắn **một** nhà cung cấp (Google **hoặc** Apple) — DATN tách bảng `user_identities`. Nhận diện bằng `sub`, không bằng email.
  - Apple Sign In để sau cờ `genie.oauth.apple.enabled = false` (cần Apple Developer Program trả phí, tên miền HTTPS, `client_secret` ký ES256) — làm nếu còn thời gian Sprint 7.
  - Hồ sơ tạo bằng Google để trống số điện thoại, giới tính, ngày sinh cho người dùng tự điền.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* **Đang triển khai** — đăng nhập Google (PKCE, tự liên kết theo email, nhập ảnh vào Silo) đã chạy thật; liên kết / gỡ liên kết ở trang tài khoản xong backend + web + test (`OAuthLoginIT`, `OAuthLinkIT`). Chưa làm: Apple.
  - *Kế hoạch:* Sprint 7 (tuần 13–14) — [implement.md](../../kien-thuc/implement.md) Phần 1.2 (Google đã làm sớm cùng S1–S2).
  - *Thực thể CSDL:* [User](../ClassDiagram/ENTITY.md#11-user--tài-khoản-đăng-nhập), [CustomerProfile](../ClassDiagram/ENTITY.md#12-customerprofile--hồ-sơ-khách-hàng), [RefreshToken](../ClassDiagram/ENTITY.md#17-refreshtoken--phiên-đăng-nhập), [SystemLog](../ClassDiagram/ENTITY.md#121-systemlog--nhật-ký-kiểm-toán).

---

### UC-AUTH-04: Khôi phục mật khẩu qua Email OTP (Forgot Password)
- **Mã Use Case:** `UC-AUTH-04`
- **Tác nhân chính:** `ACT-CUS` Customer (khách hàng); `ACT-VND` Vendor (người bán/shop); `ACT-STF` Staff (Nhân viên sàn)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Cho phép người dùng quên mật khẩu tự đặt lại mật khẩu mới bằng cách chứng minh sở hữu email; với tài khoản chỉ có Google / Apple, đây cũng là đường **thiết lập mật khẩu** lần đầu.
- **Tiền điều kiện:** Người dùng có hòm thư email đã đăng ký tài khoản trên hệ thống.
- **Hậu điều kiện:** Mật khẩu được cập nhật; khóa đăng nhập tạm (nếu có) được gỡ; toàn bộ phiên cũ bị thu hồi; gửi email cảnh báo bảo mật.
- **Luồng sự kiện chính:**
  1. Người dùng bấm "Quên mật khẩu" (`/auth/forgot`) và nhập Email đăng ký.
  2. Client gửi `POST /api/v1/auth/otp-challenges` (`purpose = PASSWORD_RESET`). Hệ thống **luôn** trả `202` "Nếu email tồn tại, mã OTP đã được gửi" (chống dò email):
     - Email thuộc tài khoản `ACTIVE` (kể cả tài khoản chỉ có Google): sinh OTP 6 số (TTL 5 phút) và gửi email.
     - Email không tồn tại / không hoạt động / đang bị khóa khôi phục: không gửi thư.
  3. Người dùng nhập mã → `POST /api/v1/auth/otp-challenges/{id}/verify` → hệ thống trả `reset_token` (`rst_…`, lưu Redis `auth:reset:{sha256}` TTL 10 phút, dùng một lần).
  4. Người dùng nhập Mật khẩu mới → `POST /api/v1/auth/password-resets` (`reset_token`, `new_password`). Hệ thống:
     - Kiểm mật khẩu mới đạt chính sách và **khác** mật khẩu hiện tại; băm Argon2id, cập nhật `users.password`.
     - Gỡ khóa đăng nhập tạm (`clearLock()` — đặt lại mật khẩu cũng là cách mở khóa).
     - Thu hồi toàn bộ refresh token (`revokedReason = 'PASSWORD_CHANGED'`) và đặt `auth:revoked_before:{userId}` để access token cũ hết hiệu lực ngay.
     - Xóa `reset_token`, ghi `system_logs` `PASSWORD_RESET` (tài khoản chưa có mật khẩu ghi `method = SETUP`).
     - Gửi email cảnh báo "Mật khẩu Genie Shop của bạn vừa được thay đổi" (thời điểm, thiết bị, IP, tài khoản; khung "Không phải bạn?" + nút **Đặt lại mật khẩu ngay**).
  5. Hệ thống trả `204`; giao diện chuyển về màn Đăng nhập.
- **Luồng thay thế / Ngoại lệ:**
  - *3a. Sai OTP:* báo số lần còn lại; sai lần thứ 5 → khóa chức năng khôi phục của email này 30 phút (`auth:pwreset_lock:{email}`), `422 GNE-AUTH-4221` kèm `locked_until`; trong thời gian khóa, yêu cầu gửi mã vẫn trả `202` nhưng không gửi thư.
  - *4a. `reset_token` hết hạn / đã dùng:* `410 GNE-AUTH-4102`, làm lại từ bước 1.
  - *4b. Mật khẩu mới trùng mật khẩu hiện tại:* `422 GNE-AUTH-4223`.
- **Quan hệ Use Case:** `<<include>>` Gửi mã OTP xác thực — tác nhân phụ: `ACT-EML` Email Service.
- **Ràng buộc nghiệp vụ & Kỹ thuật:** Toàn bộ phiên cũ phải bị đăng xuất ngay sau khi đặt lại mật khẩu (kể cả access token còn hạn) để bảo vệ tài khoản nếu phiên trước đó bị lộ.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* **Đã triển khai** — backend (`auth/password`) + test `PasswordFlowIT`, `PasswordChangedEmailTemplateTest`; web nối API thật; đã chạy thật.
  - *Kế hoạch:* Sprint 1 (tuần 1–2) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [User](../ClassDiagram/ENTITY.md#11-user--tài-khoản-đăng-nhập), [RefreshToken](../ClassDiagram/ENTITY.md#17-refreshtoken--phiên-đăng-nhập), [SystemLog](../ClassDiagram/ENTITY.md#121-systemlog--nhật-ký-kiểm-toán).

---

### UC-AUTH-06: Đổi / Thiết lập mật khẩu
- **Mã Use Case:** `UC-AUTH-06`
- **Tác nhân chính:** `ACT-CUS` Customer (khách hàng); `ACT-VND` Vendor (người bán/shop); `ACT-STF` Staff (Nhân viên sàn); `ACT-ADM` Admin
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Cho phép người dùng đang đăng nhập chủ động đổi mật khẩu (menu **Đổi mật khẩu**), hoặc thiết lập mật khẩu lần đầu cho tài khoản tạo bằng Google / Apple (menu **Thiết lập mật khẩu**).
- **Tiền điều kiện:** Người dùng đã đăng nhập; tài khoản có trạng thái `ACTIVE`.
- **Hậu điều kiện:** Mật khẩu mới được lưu; các phiên trên thiết bị **khác** bị thu hồi, phiên hiện tại giữ nguyên; gửi email cảnh báo.
- **Luồng sự kiện chính:**
  1. Người dùng vào Tài khoản › Đổi mật khẩu (Seller › Bảo mật, Admin › Tài khoản dùng chung chức năng). Màn hình đọc `GET /me/login-methods` để hiện đúng "Đổi" hay "Thiết lập".
  2. Nhập Mật khẩu hiện tại (khi đã có mật khẩu), Mật khẩu mới và Xác nhận; trong lúc gõ, giao diện có thể kiểm tra mật khẩu hiện tại qua `POST /me/password/verify` (trả `{valid}`, tối đa 10 lần / phút).
  3. Bấm "Nhận mã xác thực": `POST /me/otp-challenges` (`purpose = PASSWORD_CHANGE`) gửi OTP 6 số về email tài khoản.
  4. Người dùng nhập OTP và bấm "Xác nhận" → `PUT /me/password` kèm header `X-OTP-Challenge-Id`, `X-OTP-Code` (OTP bước 2; được kiểm **sau** khi dữ liệu hợp lệ để body sai không tiêu mất mã).
  5. Hệ thống xác thực OTP thuộc đúng người dùng và đúng mục đích, kiểm mật khẩu hiện tại khớp Argon2id.
  6. Kiểm tra mật khẩu mới đạt chính sách và khác mật khẩu cũ.
  7. Cập nhật `users.password` (tăng `version`).
  8. Thu hồi refresh token của mọi phiên **khác** phiên hiện tại (`PASSWORD_CHANGED`); ghi `system_logs` `PASSWORD_CHANGED` (`method = CHANGE` / `SETUP`).
  9. Gửi email cảnh báo đổi mật khẩu; trả `204`.
- **Luồng thay thế / Ngoại lệ:**
  - *4a. Thiếu / sai OTP bước 2:* `422 GNE-AUTH-4222` (thiếu header hoặc sai mã), OTP của mục đích khác hoặc của người khác bị từ chối.
  - *5a. Mật khẩu hiện tại không đúng:* `401 GNE-AUTH-4011` "Mật khẩu hiện tại không chính xác".
  - *6a. Mật khẩu mới trùng mật khẩu hiện tại:* `422 GNE-AUTH-4223`; yếu → `422 GNE-VAL-4220`.
  - *Thiết lập mật khẩu lần đầu (tài khoản Google / Apple, `password = NULL`):* không hỏi mật khẩu hiện tại; xác thực OTP email rồi đặt mật khẩu mới — sau đó đăng nhập được bằng cả email + mật khẩu lẫn Google. (Người chưa đăng nhập dùng Quên mật khẩu — UC-AUTH-04.)
- **Quan hệ Use Case:** `<<include>>` Gửi mã OTP xác thực — tác nhân phụ: `ACT-EML` Email Service.
- **Ràng buộc nghiệp vụ & Kỹ thuật:**
  - Áp dụng cho mọi vai trò (khách, shop, nhân viên, Admin) qua cùng một endpoint; luôn bắt buộc OTP bước 2.
  - Không đặt `auth:revoked_before` khi đổi mật khẩu chủ động (để giữ phiên hiện tại); access token của máy khác tự hết hạn trong ≤ 15 phút.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* **Đang triển khai** — backend + test `PasswordFlowIT` xong; web nối API thật (cờ `security`). Còn: chạy thật trên giao diện.
  - *Kế hoạch:* Sprint 1 (tuần 1–2) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [User](../ClassDiagram/ENTITY.md#11-user--tài-khoản-đăng-nhập), [RefreshToken](../ClassDiagram/ENTITY.md#17-refreshtoken--phiên-đăng-nhập), [SystemLog](../ClassDiagram/ENTITY.md#121-systemlog--nhật-ký-kiểm-toán).

---

### UC-AUTH-08: Quản lý thông tin hồ sơ cá nhân (Customer Profile)
- **Mã Use Case:** `UC-AUTH-08`
- **Tác nhân chính:** `ACT-CUS` Customer (khách hàng); `ACT-STF` Staff (Nhân viên sàn)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Cho phép khách hàng xem và cập nhật thông tin cá nhân (họ tên, số điện thoại, ngày sinh, giới tính, giới thiệu, ảnh đại diện, ngôn ngữ hiển thị) và đổi email đăng nhập.
- **Tiền điều kiện:** Khách hàng đã đăng nhập tài khoản.
- **Hậu điều kiện:** Cập nhật thông tin trong bảng `customer_profiles` (đổi email: `users.email`).
- **Luồng sự kiện chính:**
  1. Khách hàng vào "Hồ sơ của tôi" (`/account/profile`); hệ thống trả `GET /me/profile`.
  2. Hệ thống hiển thị: Họ tên, Email, Số điện thoại, Giới tính (`MALE`, `FEMALE`, `OTHER`, `PREFER_NOT_TO_SAY`), Ngày sinh, Giới thiệu (`bio`), Ngôn ngữ hiển thị (`preferredLanguage`: `vi` / `en`), Ảnh đại diện (khối ảnh đặt đầu form). Nếu tài khoản đang chờ xóa (UC-AUTH-10) thì hiện băng đỏ kèm nút "Hủy yêu cầu xóa".
  3. Khách hàng chỉnh sửa thông tin hoặc chọn ảnh đại diện mới.
  4. Nếu tải ảnh: client gọi `POST /uploads/presigned-urls` (`purpose = AVATAR`) lấy Presigned PUT URL (15 phút), PUT ảnh thẳng lên **Silo** vào khóa tạm `tmp/user/{id}/{uuid}.{ext}` của bucket `avatar-assets`, rồi gửi `avatar_object_key` trong request lưu hồ sơ.
  5. Khách hàng bấm "Lưu thay đổi" → `PATCH /me/profile` (chỉ trường có gửi; `null` = giữ nguyên, chuỗi rỗng ở số điện thoại / giới thiệu = xóa).
  6. Hệ thống validate (họ tên không rỗng; ngày sinh `YYYY-MM-DD` không lớn hơn hôm nay; ngôn ngữ ∈ `vi`, `en`); với ảnh: kiểm tra tệp đã tải đúng loại / kích thước (`statObject`), chuyển sang khóa chính thức `user/{id}/{uuid}.{ext}`.
  7. Hệ thống cập nhật `customer_profiles` (ảnh lưu URL công khai), ảnh cũ bị xóa khỏi Silo sau khi transaction commit; trả hồ sơ đầy đủ.
  8. Nhân viên / Admin cập nhật ảnh đại diện và họ tên ở mục "Tài khoản" của Admin Portal (`PATCH /admin/me`, ảnh lưu Silo).
- **Luồng thay thế / Ngoại lệ:**
  - *4a. Ảnh vượt 1 MB hoặc không phải JPG / PNG:* từ chối ngay khi xin URL (`422 GNE-VAL-4220`) và kiểm lại khi nhận `avatar_object_key`; URL quá 15 phút → `GNE-MINIO-5003`, xin URL mới.
  - *4b. `avatar_object_key` không thuộc người dùng / chưa tải lên:* từ chối.
  - *Đổi email đăng nhập:* `POST /me/email-change-requests` (`new_email`) → OTP `EMAIL_CHANGE` gửi tới **email mới** → `POST /me/email-change-requests/{id}/confirm` → đổi email, `emailVerifiedAt = now`, ghi `EMAIL_CHANGED`, gửi thông báo tới **email cũ**. Trùng email hiện tại → `422 GNE-AUTH-4224`; email đã thuộc người khác → `409 GNE-AUTH-4094`; tài khoản chưa có mật khẩu (chỉ Google) → `409 GNE-AUTH-4095` — phải "Thiết lập mật khẩu" trước. Liên kết Google giữ nguyên.
- **Quan hệ Use Case:** `<<include>>` Tải tệp lên Silo (Presigned URL) — tác nhân phụ: `ACT-MIN` Silo S3 Storage.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* **Đang triển khai** — xem / sửa hồ sơ + ảnh đại diện qua Silo xong backend + web + test `ProfileAvatarIT`. Còn: `GET /me` (bối cảnh tài khoản: vai trò, shop, bộ đếm), đổi email.
  - *Kế hoạch:* Sprint 2 (tuần 3–4) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [CustomerProfile](../ClassDiagram/ENTITY.md#12-customerprofile--hồ-sơ-khách-hàng), [User](../ClassDiagram/ENTITY.md#11-user--tài-khoản-đăng-nhập).

---

### UC-AUTH-09: Quản lý sổ địa chỉ giao hàng chuẩn GHN
- **Mã Use Case:** `UC-AUTH-09`
- **Tác nhân chính:** `ACT-CUS` Customer (khách hàng)
- **Tác nhân phụ:** `ACT-3PL` Đối tác vận chuyển (GHN)
- **Mục đích nghiệp vụ:** Cho phép khách hàng thêm, sửa, xóa và thiết lập địa chỉ giao hàng mặc định; chuẩn hóa mã tỉnh/quận/phường theo đơn vị vận chuyển GHN để phục vụ tính phí ship chính xác.
- **Tiền điều kiện:** Khách hàng đã đăng nhập tài khoản.
- **Hậu điều kiện:** Bản ghi trong bảng `addresses` được tạo/sửa; cập nhật `defaultAddressId` trong `customer_profiles`.
- **Luồng sự kiện chính:**
  1. Khách hàng vào mục "Địa chỉ" (`/account/address`), bấm "Thêm địa chỉ mới".
  2. Khách hàng nhập Tên người nhận, Số điện thoại nhận hàng, Địa chỉ chi tiết (Số nhà, tên đường) và nhãn (Nhà riêng / Văn phòng).
  3. Khách hàng chọn Tỉnh/Thành phố (`GET /public/regions/provinces` — backend lấy danh mục hành chính GHN, cache Redis 24 giờ; thiếu `GHN_TOKEN` ở máy dev thì đọc ảnh chụp JSON).
  4. Hệ thống nạp danh sách Quận/Huyện theo Tỉnh (`GET /public/regions/provinces/{id}/districts`, GHN District ID). Khách hàng chọn Quận/Huyện.
  5. Hệ thống nạp danh sách Phường/Xã theo Quận (`GET /public/regions/districts/{id}/wards`, GHN Ward Code). Khách hàng chọn Phường/Xã.
  6. Khách hàng tích chọn "Đặt làm địa chỉ mặc định" (nếu muốn).
  7. Khách hàng bấm "Hoàn thành" → `POST /me/addresses`.
  8. Hệ thống kiểm tra đủ 3 cấp hợp lệ và **tự điền tên** tỉnh / quận / phường từ mã (không tin tên client gửi), lưu `addresses` theo mô hình chuẩn hóa kép:
     - Lưu mã GHN: `provinceId`, `districtId`, `wardCode` (dùng để gọi API tính phí ship và tạo vận đơn).
     - Lưu tên hiển thị: `province`, `district`, `ward` (dùng hiển thị giao diện và bảo toàn lịch sử khi địa giới thay đổi).
  9. Nếu chọn mặc định (hoặc là địa chỉ đầu tiên): hệ thống mở transaction, khóa dòng `customer_profiles` (`FOR UPDATE`), đặt `is_default = false` cho các địa chỉ khác, đặt địa chỉ này `is_default = true` và cập nhật `customer_profiles.default_address_id`.
- **Luồng thay thế / Ngoại lệ:**
  - *Sửa địa chỉ:* `PUT /me/addresses/{id}`; đặt mặc định nhanh: `PATCH /me/addresses/{id}` (`is_default = true`). Địa chỉ của người khác → `404`.
  - *Xóa địa chỉ:* `DELETE /me/addresses/{id}` → `is_deleted = true` (xóa mềm) để bảo vệ toàn vẹn lịch sử đơn hàng cũ; địa chỉ mặc định không xóa được (`409 GNE-USR-4092`) — đặt địa chỉ khác làm mặc định trước.
  - *Dùng vị trí hiện tại (GPS):* trình duyệt cấp tọa độ → `GET /public/regions/reverse-geocode?lat&lng` (Nominatim / OpenStreetMap, ≤ 1 yêu cầu / giây, khớp tên sang mã GHN) → gợi ý Tỉnh / Quận / Phường; người dùng chỉnh lại trước khi lưu. Không khớp được phường → chỉ gợi ý tỉnh / quận. (Có thể cắt nếu thiếu thời gian.)
- **Quan hệ Use Case:** Không có.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* **Chưa triển khai backend** — giao diện (sổ địa chỉ + `RegionPicker`) đã xong trên lớp mock, chờ API.
  - *Kế hoạch:* Sprint 2 (tuần 3–4) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [Address](../ClassDiagram/ENTITY.md#13-address--địa-chỉ-giao-hàng), [CustomerProfile](../ClassDiagram/ENTITY.md#12-customerprofile--hồ-sơ-khách-hàng).

---

### UC-AUTH-10: Yêu cầu ẩn danh hóa & Xóa dữ liệu cá nhân (Xóa tài khoản)
- **Mã Use Case:** `UC-AUTH-10`
- **Tác nhân chính:** `ACT-CUS` Customer (khách hàng)
- **Tác nhân phụ:** `ACT-CRN` Platform Cron / Scheduler
- **Mục đích nghiệp vụ:** Cho phép khách hàng tự xóa tài khoản (nút "Xóa tài khoản" trong Hồ sơ) theo quyền xóa dữ liệu cá nhân của Luật Bảo vệ dữ liệu cá nhân, mà không làm mất chứng từ đơn hàng / tài chính phải lưu theo luật.
- **Tiền điều kiện:** Khách hàng đã đăng nhập; không còn đơn đang xử lý (`PENDING_PAYMENT` → `DELIVERING`, `RETURN_REQUESTED`), không có yêu cầu trả hàng đang mở; nếu là chủ shop thì ví shop phải bằng 0 và không còn đơn chưa hoàn tất.
- **Hậu điều kiện:** `users.deletionRequestedAt` được ghi; sau 30 ngày tài khoản chuyển `ANONYMIZED`, dữ liệu nhận dạng cá nhân bị xóa / ghi đè.
- **Luồng sự kiện chính:**
  1. Khách hàng vào Hồ sơ → bấm "Xóa tài khoản"; hệ thống hiển thị cảnh báo "Hành động này không thể hoàn tác sau 30 ngày…".
  2. Khách bấm "Nhận mã" → `POST /me/otp-challenges` (`purpose = ACCOUNT_DELETION`): hệ thống kiểm điều kiện **trước khi gửi mã** (cổng `AccountDeletionGuard` gộp lý do chặn từ các phân hệ đơn hàng / thanh toán / shop); chưa đủ điều kiện thì liệt kê lý do.
  3. Hệ thống gửi OTP email để xác nhận chủ tài khoản.
  4. Khách nhập OTP đúng → `PUT /me/deletion-request` (OTP bước 2) → hệ thống kiểm lại điều kiện, ghi `deletionRequestedAt = now`, ghi `system_logs` `ACCOUNT_DELETION_REQUESTED`, **đăng xuất các thiết bị khác** (giữ phiên hiện tại để khách thấy băng đỏ và nút hủy), gửi email "Tài khoản sẽ được ẩn danh vào ngày …" kèm nút **Giữ lại tài khoản**; trả `{requested_at, anonymize_at}`.
  5. Trong 30 ngày khách vẫn đăng nhập được; Hồ sơ hiện băng đỏ. Bấm "Hủy yêu cầu xóa" → `DELETE /me/deletion-request` → xóa `deletionRequestedAt`, ghi `ACCOUNT_DELETION_CANCELLED` → tài khoản trở lại bình thường.
  6. Cron `AnonymizeAccountsJob` chạy **02:30 hằng ngày** (giờ Việt Nam): lấy tài khoản đã yêu cầu quá 30 ngày (`FOR UPDATE SKIP LOCKED`, tối đa 1.000 tài khoản / lượt, mỗi tài khoản một transaction): `User.anonymize()` (email `deleted_user_{id}@anonymized.local`, xóa mật khẩu, xóa **cả** `oauthProvider` lẫn `oauthProviderId`), `CustomerProfile.anonymize()` (xóa họ tên, SĐT, ảnh, giới thiệu, ngày sinh), xóa mềm sổ địa chỉ, thu hồi mọi refresh token; ghi `system_logs` `PII_ANONYMIZED`; sau commit xóa ảnh đại diện trên Silo.
- **Luồng thay thế / Ngoại lệ:**
  - *2a. Còn đơn đang xử lý / số dư ví shop:* `409 GNE-USR-4091` kèm `details` (vd `active_order_codes`) **trước khi** gửi mã; đơn phát sinh trong lúc nhập mã vẫn bị chặn ở bước 4.
  - *3a. Sai OTP 5 lần:* hủy mã xác nhận, giữ nguyên tài khoản.
  - *4a. Đã có yêu cầu xóa:* trả lại yêu cầu cũ (không tính lại 30 ngày). Hủy khi không có yêu cầu → vẫn `204`.
- **Quan hệ Use Case:** `<<include>>` Gửi mã OTP xác thực — tác nhân phụ: `ACT-EML` Email Service.
- **Ràng buộc nghiệp vụ & Kỹ thuật:**
  - **Giữ nguyên** `orders`, `order_items`, `payment_transactions`, `escrow_ledgers`, `financial_ledger` (chứng từ kế toán lưu tối thiểu 10 năm) — chỉ còn liên kết tới tài khoản đã ẩn danh.
  - Đánh giá đã viết vẫn hiển thị với tên "Người dùng đã xóa". Sau khi ẩn danh, email cũ đăng ký lại được tài khoản mới.
  - Điều kiện chặn do phân hệ sở hữu dữ liệu cài cổng `AccountDeletionBlocker` (order, payment, vendor); phân hệ chưa làm thì không chặn.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* **Đang triển khai** — backend (yêu cầu / hủy / cron ẩn danh / email) + test `AccountDeletionIT` xong, web nối API thật (cờ `security`). Còn: chạy thật; phân hệ đơn hàng / ví cài cổng chặn.
  - *Kế hoạch:* Sprint 7 (tuần 13–14) — [implement.md](../../kien-thuc/implement.md) Phần 1.2 (đã làm sớm).
  - *Thực thể CSDL:* [User](../ClassDiagram/ENTITY.md#11-user--tài-khoản-đăng-nhập), [CustomerProfile](../ClassDiagram/ENTITY.md#12-customerprofile--hồ-sơ-khách-hàng), [Address](../ClassDiagram/ENTITY.md#13-address--địa-chỉ-giao-hàng), [RefreshToken](../ClassDiagram/ENTITY.md#17-refreshtoken--phiên-đăng-nhập), [SystemLog](../ClassDiagram/ENTITY.md#121-systemlog--nhật-ký-kiểm-toán).

---

### UC-AUTH-11: Đăng xuất & Quản lý thiết bị đăng nhập
- **Mã Use Case:** `UC-AUTH-11`
- **Tác nhân chính:** `ACT-CUS` Customer (khách hàng); `ACT-VND` Vendor (người bán/shop); `ACT-STF` Staff (Nhân viên sàn); `ACT-ADM` Admin
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Cho phép người dùng thoát khỏi phiên làm việc hiện tại, xem các thiết bị đang đăng nhập và đăng xuất từ xa từng thiết bị hoặc tất cả thiết bị khác (khi nghi ngờ bị lộ phiên).
- **Tiền điều kiện:** Người dùng đang đăng nhập với JWT hợp lệ (đăng xuất phiên hiện tại vẫn được khi access token đã hết hạn nhưng còn cookie).
- **Hậu điều kiện:** Refresh Token của phiên tương ứng bị đánh dấu `revokedAt = now()`; client xóa token khỏi bộ nhớ.
- **Luồng sự kiện chính:**
  1. Người dùng bấm "Đăng xuất" (xác nhận "Bạn có chắc muốn đăng xuất khỏi tài khoản … trên thiết bị này?").
  2. Client gửi `DELETE /api/v1/auth/sessions/current`.
  3. Server thu hồi mọi refresh token của phiên hiện tại theo `sid` (`familyId`) — `revokedReason = 'USER_LOGOUT'`.
  4. Server đưa `jti` của access token hiện tại vào Redis `auth:denylist:{jti}` (TTL bằng thời gian sống còn lại của token) để vô hiệu hóa ngay, xóa cookie `genie_rt`, trả `204`.
  5. Client xóa token trong bộ nhớ và điều hướng về trang đăng nhập.
  6. Mục "Thiết bị đăng nhập" (`/account/devices`): `GET /me/sessions` liệt kê mỗi phiên còn hiệu lực một dòng — tên thiết bị suy từ User-Agent (vd "Windows PC · Chrome 126"), loại thiết bị, IP, vị trí (`null` — chưa có GeoIP), lần hoạt động cuối (`lastUsedAt`), nhãn "Phiên này"; phiên hiện tại đứng đầu.
  7. Người dùng thu hồi từng phiên (`DELETE /me/sessions/{id}`, `id` = `familyId`) hoặc "Đăng xuất tất cả thiết bị khác" (`DELETE /me/sessions?scope=others`); ghi `system_logs` `SESSION_REVOKED`.
- **Luồng thay thế / Ngoại lệ:**
  - *7a. Thu hồi phiên hiện tại từ danh sách thiết bị:* `409 GNE-AUTH-4097` — dùng nút Đăng xuất.
  - *7b. Phiên không thuộc người dùng:* `404`.
  - *Thiết bị khác bị đăng xuất:* refresh token bị thu hồi ngay; access token đã cấp trên máy đó tự hết hạn trong ≤ 15 phút (không đặt `revoked_before` để không đá luôn phiên hiện tại).
- **Quan hệ Use Case:** Không có.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* **Đang triển khai** — backend (`SessionController`, `MeSessionController`) + test `SessionFlowIT`, `UserAgentParserTest` xong; web nối API thật. Còn: chạy thật trên giao diện.
  - *Kế hoạch:* Sprint 1 (tuần 1–2) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [RefreshToken](../ClassDiagram/ENTITY.md#17-refreshtoken--phiên-đăng-nhập), [SystemLog](../ClassDiagram/ENTITY.md#121-systemlog--nhật-ký-kiểm-toán).

---

<a id="ph-cat"></a>
## PHÂN HỆ 02: DANH MỤC, SẢN PHẨM & TÌM KIẾM (CATALOG, PRODUCT & SEARCH)

> **Phạm vi:** Cây danh mục & thuộc tính chuẩn, tìm kiếm full-text PostgreSQL kèm từ khóa gợi ý, lọc & sắp xếp, chi tiết sản phẩm, sản phẩm tương tự, yêu thích, chia sẻ deeplink, đăng / sửa / ẩn sản phẩm, kiểm duyệt tự động (Gemini) và thủ công, đề xuất danh mục, trang gian hàng.
> **Use case (17):** `UC-CAT-01`, `UC-CAT-02`, `UC-CAT-04`, `UC-CAT-05`, `UC-CAT-06`, `UC-CAT-07`, `UC-CAT-08`, `UC-CAT-09`, `UC-CAT-10`, `UC-CAT-11`, `UC-CAT-12`, `UC-CAT-13`, `UC-CAT-14`, `UC-CAT-15`, `UC-CAT-16`, `UC-CAT-17`, `UC-CAT-18`

### UC-CAT-01: Duyệt cây danh mục đa cấp (Category Hierarchy)
- **Mã Use Case:** `UC-CAT-01`
- **Tác nhân chính:** `ACT-GST` Guest (khách vãng lai); `ACT-CUS` Customer (khách hàng)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Giúp người mua khám phá các ngành hàng trên sàn thông qua cấu trúc phân cấp cây danh mục (Category Tree: Cha - Con).
- **Tiền điều kiện:** Hệ thống đã được Admin thiết lập danh mục sàn.
- **Hậu điều kiện:** Trả về danh sách danh mục phân cấp có icon và số lượng sản phẩm.
- **Luồng sự kiện chính:**
  1. Người dùng mở trang chủ hoặc bấm "Danh mục sản phẩm" trên header.
  2. Client gửi request `GET /api/v1/categories`.
  3. Server kiểm tra Redis Cache `categories:tree:all`:
     - Nếu Cache Hit: Trả về dữ liệu cây danh mục ngay lập tức (phản hồi < 5ms).
     - Nếu Cache Miss: Server truy vấn bảng `categories` (lọc `isActive = true`), dựng cây phân cấp (Parent - Child thông qua `parentId`), lưu vào Redis với TTL 24 giờ.
  4. Giao diện hiển thị Menu đa cấp với Icon, Tên ngành hàng, và các danh mục con trực thuộc.
- **Quan hệ Use Case:** Không có.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 2 (tuần 3–4) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [Category](../ClassDiagram/ENTITY.md#21-category--danh-mục-ngành-hàng).

---

### UC-CAT-02: Tìm kiếm sản phẩm theo từ khóa
- **Mã Use Case:** `UC-CAT-02`
- **Tác nhân chính:** `ACT-GST` Guest (khách vãng lai); `ACT-CUS` Customer (khách hàng)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Giúp người mua tìm sản phẩm nhanh theo từ khóa tiếng Việt có dấu hoặc không dấu, chịu được gõ sai nhẹ.
- **Tiền điều kiện:** Sản phẩm đang ở trạng thái `PUBLISHED`; cột `products.search_vector` (tsvector) được cập nhật bằng trigger khi tên / thương hiệu / mô tả thay đổi.
- **Hậu điều kiện:** Hiển thị danh sách sản phẩm khớp nhất (phân trang 20 sản phẩm / trang) kèm bộ lọc.
- **Luồng sự kiện chính:**
  1. Người dùng gõ từ khóa vào thanh tìm kiếm (ví dụ: "ao thun nam") và nhấn Enter.
  2. Backend chuẩn hóa từ khóa (`unaccent`, chữ thường) và truy vấn PostgreSQL: `WHERE search_vector @@ plainto_tsquery('simple', unaccent(:q))`, sắp xếp theo `ts_rank`.
  3. Nếu kết quả ít hơn 5 sản phẩm: chạy thêm truy vấn gần đúng bằng `pg_trgm` (`similarity(name, :q) > 0.3`) để bù lỗi gõ sai.
  4. Backend lấy kèm giá thấp nhất giữa các SKU, ảnh chính, `sold`, `rating` (cột tổng hợp) và trả về client.
  5. Client hiển thị lưới sản phẩm; người dùng có thể tiếp tục lọc / sắp xếp (`UC-CAT-05`).
- **Luồng thay thế / Ngoại lệ:**
  - *2a. Từ khóa rỗng hoặc chỉ có ký tự đặc biệt:* trả về danh sách sản phẩm mới nhất.
  - *3a. Không có kết quả:* hiển thị "Không tìm thấy sản phẩm" kèm gợi ý danh mục phổ biến.
- **Quan hệ Use Case:** được mở rộng bởi `UC-CAT-04` (điểm «Nhập từ khóa»).
- **Ràng buộc nghiệp vụ & Kỹ thuật:** Index `GIN (search_vector)` và `GIN (name gin_trgm_ops)` (ENTITY.md mục 13.1); giới hạn 60 request / phút / IP bằng bộ đếm Redis.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* 🟡 Đang triển khai — backend + test xong, web đã nối API thật (cờ `PUBLIC_REAL_API=product,search,shop`), chờ chạy thật end-to-end — plan `plan/2026-11-17/server-spring/plan_tim_kiem_va_trang_san_pham.md` (2026-11-18).
  - *Kế hoạch:* Sprint 2 (tuần 3–4) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [Product](../ClassDiagram/ENTITY.md#22-product--sản-phẩm-spu), [ProductVariant](../ClassDiagram/ENTITY.md#23-productvariant--biến-thể-sản-phẩm-sku).

---

### UC-CAT-04: Gợi ý từ khóa tìm kiếm & Xu hướng (Autocomplete & Trending)
- **Mã Use Case:** `UC-CAT-04`
- **Tác nhân chính:** `ACT-GST` Guest (khách vãng lai); `ACT-CUS` Customer (khách hàng)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Tăng tốc thao tác tìm kiếm và định hướng người dùng đến sản phẩm thịnh hành: dãy từ khóa phổ biến dưới ô tìm kiếm ("Smartphone", "Áo thun", "Serum"…) và gợi ý khi gõ.
- **Tiền điều kiện:** Người dùng ở trang có ô tìm kiếm (header storefront).
- **Hậu điều kiện:** Hiển thị tối đa 10 từ khóa phổ biến / gợi ý; lượt tìm được đếm cho thống kê.
- **Luồng sự kiện chính:**
  1. Khi trang tải, ô tìm kiếm hiển thị dãy **từ khóa phổ biến** (`GET /api/v1/public/search/trending`): top 5–10 từ khóa 24 giờ qua — Redis Sorted Set hôm nay (trọng số 1) + hôm qua (trọng số 0,5), bỏ từ `isBlocked`, cache 10 phút; Redis trống thì cộng `search_keyword_stats` (API_SPEC 5.4b).
  2. Khi người dùng gõ từ 2 ký tự, client chờ 250 ms rồi gọi `GET /api/v1/public/search/suggestions?q=…` (API_SPEC 5.4).
  3. Backend trả gợi ý theo tiền tố: từ khóa phổ biến khớp tiền tố + tên sản phẩm / danh mục khớp (`pg_trgm` + `unaccent`, giới hạn 10).
  4. Người dùng bấm một từ khóa → thực hiện tìm kiếm (UC-CAT-02).
  5. Mỗi lượt tìm hợp lệ tăng bộ đếm Redis `search:kw:{ngày}`; job 5 phút cộng dồn vào `search_keyword_stats` (`SearchKeywordStat.accumulate()`).
- **Luồng thay thế / Ngoại lệ:**
  - *Từ khóa nhạy cảm / spam:* Staff đánh dấu `isBlocked = true` — không còn xuất hiện trong danh sách phổ biến.
- **Quan hệ Use Case:** `<<extend>>` Tìm kiếm sản phẩm theo từ khóa (`UC-CAT-02`) tại điểm mở rộng «Nhập từ khóa» khi {người dùng bấm vào hoặc gõ vào ô tìm kiếm}.
- **Ràng buộc nghiệp vụ & Kỹ thuật:**
  - TLCN dùng full-text PostgreSQL (`tsvector` + `pg_trgm`), không dùng Elasticsearch / Redis Trie như bản DATN.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* 🟡 Đang triển khai — backend + test xong, web đã nối API thật (cờ `PUBLIC_REAL_API=product,search,shop`), chờ chạy thật end-to-end — plan `plan/2026-11-17/server-spring/plan_tim_kiem_va_trang_san_pham.md` (2026-11-18).
  - *Kế hoạch:* Sprint 7 (tuần 13–14) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [SearchKeywordStat](../ClassDiagram/ENTITY.md#211-searchkeywordstat--thống-kê-từ-khóa-tìm-kiếm-theo-ngày), [Product](../ClassDiagram/ENTITY.md#22-product--sản-phẩm-spu), [Category](../ClassDiagram/ENTITY.md#21-category--danh-mục-ngành-hàng).

---

### UC-CAT-05: Lọc nâng cao & Sắp xếp sản phẩm đa tiêu chí
- **Mã Use Case:** `UC-CAT-05`
- **Tác nhân chính:** `ACT-GST` Guest (khách vãng lai); `ACT-CUS` Customer (khách hàng)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Giúp người mua thu hẹp phạm vi sản phẩm theo nhu cầu cá nhân.
- **Tiền điều kiện:** Đang ở trang danh mục hoặc trang kết quả tìm kiếm.
- **Hậu điều kiện:** Danh sách sản phẩm được cập nhật theo bộ lọc mà không tải lại toàn trang (Client-side state / SWR).
- **Luồng sự kiện chính:**
  1. Người dùng chọn các tiêu chí lọc:
     - Khoảng giá (`minPrice` - `maxPrice`).
     - Đánh giá sao (từ 4 sao trở lên).
     - Địa điểm gian hàng (theo Tỉnh/Thành phố của kho người bán).
     - Thương hiệu / Danh mục con.
     - Tình trạng khuyến mãi (shop đang có voucher).
  2. Người dùng chọn tiêu chí sắp xếp: Phổ biến nhất, Bán chạy nhất, Giá tăng dần, Giá giảm dần, Hàng mới về (`sort` = `RELEVANCE` / `BEST_SELLING` / `PRICE_ASC` / `PRICE_DESC` / `NEWEST`; "Phổ biến nhất" = `RELEVANCE` khi có từ khóa, `BEST_SELLING` khi không — API_SPEC 5.3).
  3. Client cập nhật URL params và gửi request đến Backend.
  4. Backend dựng câu truy vấn kết hợp các facet filters và order clauses, trả về danh sách phân trang.
  5. Bộ lọc bổ sung theo giao diện: **Shop Chính Hãng** (`VendorProfile.isMall`), **Chính hãng** (shop `isMall` hoặc thương hiệu đã xác minh `Brand.isVerified`), **Thương hiệu** (`brandId`), **Miễn phí vận chuyển** (shop có `freeshipThreshold`), số sao (≥ N★), khoảng giá, thuộc tính `isFilterable` của ngành (UC-CAT-15).
  6. Tìm **trong shop** (từ trang shop) ↔ "Mở rộng tìm toàn sàn"; sắp xếp Liên quan / Mới nhất / Bán chạy / Giá.
- **Quan hệ Use Case:** Không có.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* 🟡 Đang triển khai — backend + test xong, web đã nối API thật (cờ `PUBLIC_REAL_API=product,search,shop`), chờ chạy thật end-to-end — plan `plan/2026-11-17/server-spring/plan_tim_kiem_va_trang_san_pham.md` (2026-11-18).
  - *Kế hoạch:* Sprint 2 (tuần 3–4) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [Product](../ClassDiagram/ENTITY.md#22-product--sản-phẩm-spu), [ProductVariant](../ClassDiagram/ENTITY.md#23-productvariant--biến-thể-sản-phẩm-sku), [VendorProfile](../ClassDiagram/ENTITY.md#14-vendorprofile--hồ-sơ-người-bán-shop), [Brand](../ClassDiagram/ENTITY.md#27-brand--thương-hiệu), [VendorShippingConfig](../ClassDiagram/ENTITY.md#73-vendorshippingconfig--cấu-hình-vận-chuyển-của-shop).

---

### UC-CAT-06: Xem chi tiết sản phẩm, ma trận biến thể SKU & Tồn kho
- **Mã Use Case:** `UC-CAT-06`
- **Tác nhân chính:** `ACT-GST` Guest (khách vãng lai); `ACT-CUS` Customer (khách hàng)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Cung cấp thông tin đầy đủ về thông số kỹ thuật, hình ảnh, ma trận phân loại (Màu sắc, Kích cỡ), giá tiền tương ứng từng SKU và số lượng còn lại trong kho.
- **Tiền điều kiện:** Sản phẩm đang ở trạng thái `PUBLISHED`.
- **Hậu điều kiện:** Hiển thị trang Product Detail Page (PDP); tăng biến đếm lượt xem trong Redis.
- **Luồng sự kiện chính:**
  1. Người dùng bấm vào thẻ sản phẩm từ trang chủ hoặc danh mục.
  2. Client gọi `GET /api/v1/public/products/{id}` (API_SPEC 5.6).
  3. Server lấy thông tin sản phẩm, danh sách ảnh gallery từ `product_images`, danh sách biến thể SKU từ `product_variants`, thông tin người bán từ `vendor_profiles`, và 5 đánh giá mới nhất từ `product_reviews`.
  4. Giao diện hiển thị:
     - Slider ảnh chất lượng cao.
     - Tên, Giá gốc, Giá bán hiện tại, Tỷ lệ giảm giá.
     - Các nút chọn thuộc tính biến thể (Options: Màu sắc, Size).
  5. Khi người dùng chọn kết hợp thuộc tính (vd: Màu "Đen" + Size "L"):
     - Hệ thống định vị SKU tương ứng trong `product_variants`.
     - Cập nhật giá tiền hiển thị theo SKU được chọn.
     - Hiển thị số lượng tồn kho khả dụng (`stockCount`). Nếu tồn kho = 0, vô hiệu hóa nút "Mua ngay" và "Thêm vào giỏ", hiển thị trạng thái "Hết hàng".
  6. Nhãn tồn kho: "Còn N sản phẩm" khi ≤ 10, "Sắp hết" khi ≤ 5, "Hết hàng" khi 0; nhãn Chính hãng (shop `isMall`), FLASH SALE (giá sốc + đếm ngược, UC-PRO-06), "Đặt trước" (`isPreOrder`), "Shop đang tạm nghỉ" (UC-VND-19).
  7. Thông tin thêm: thương hiệu, tình trạng hàng, xuất xứ & thông số theo ngành, "Gửi từ" (tỉnh kho lấy hàng), đánh giá có bộ lọc "Có hình ảnh" và nút "Hữu ích" (UC-SOC-03).
  8. Nút **Yêu thích** (UC-CAT-08), **Chia sẻ** (UC-CAT-18), **Chat ngay** (UC-SOC-01/02), **Theo dõi** shop (UC-SOC-06); khối **Sản phẩm tương tự** (UC-CAT-07).
  9. Mỗi lần mở trang tăng bộ đếm Redis lượt xem theo nguồn (`src`) → job 5 phút ghi `ProductViewStat` và `Product.viewCount`.
- **Quan hệ Use Case:** `<<include>>` Xem gợi ý sản phẩm tương tự (`UC-CAT-07`); được mở rộng bởi `UC-CAT-16` (điểm «Bấm vào tên shop»), `UC-CAT-18` (điểm «Chia sẻ»).
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* 🟡 Đang triển khai — backend + test xong, web đã nối API thật (cờ `PUBLIC_REAL_API=product,search,shop`), chờ chạy thật end-to-end — plan `plan/2026-11-17/server-spring/plan_tim_kiem_va_trang_san_pham.md` (2026-11-18).
  - *Kế hoạch:* Sprint 2 (tuần 3–4) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [Product](../ClassDiagram/ENTITY.md#22-product--sản-phẩm-spu), [ProductVariant](../ClassDiagram/ENTITY.md#23-productvariant--biến-thể-sản-phẩm-sku), [ProductImage](../ClassDiagram/ENTITY.md#24-productimage--ảnh-sản-phẩm), [Brand](../ClassDiagram/ENTITY.md#27-brand--thương-hiệu), [ProductViewStat](../ClassDiagram/ENTITY.md#212-productviewstat--lượt-xem-sản-phẩm-theo-ngày--nguồn-truy-cập).

---

### UC-CAT-07: Xem gợi ý sản phẩm tương tự
- **Mã Use Case:** `UC-CAT-07`
- **Tác nhân chính:** `ACT-GST` Guest (khách vãng lai); `ACT-CUS` Customer (khách hàng)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Tăng tỷ lệ chuyển đổi bằng khối "Sản phẩm tương tự" ở cuối trang chi tiết và lưới "Gợi ý hôm nay / Khám phá" ở trang chủ.
- **Tiền điều kiện:** Sản phẩm đang xem ở trạng thái `PUBLISHED`.
- **Hậu điều kiện:** Hiển thị tối đa 9 sản phẩm tương tự (trang chi tiết) hoặc lưới gợi ý có phân trang (trang chủ).
- **Luồng sự kiện chính:**
  1. Trang chi tiết gọi `GET /api/v1/public/products/{id}/similar?limit=6` (API_SPEC 5.9; `limit` 1–12).
  2. Backend chọn ứng viên: cùng danh mục lá, khoảng giá ±50%, đang bán, còn hàng; loại trừ chính sản phẩm và sản phẩm của shop đang tạm nghỉ.
  3. Xếp hạng theo điểm: độ giống tên (`similarity()` của `pg_trgm`) + số lượng bán + điểm đánh giá; cache Redis 30 phút theo sản phẩm.
  4. Trả danh sách dạng thẻ (ảnh, tên, giá, đã bán) hiển thị carousel.
  5. Trang chủ "Gợi ý hôm nay": trả sản phẩm bán chạy / mới theo danh mục khách xem gần đây (nếu có), còn lại ngẫu nhiên có phân trang (`sortBy=random` với seed theo ngày để phân trang ổn định). Trang chủ hiện khối "Gợi ý cho bạn" (6 hàng sản phẩm); "Xem thêm" mở trang riêng **Gợi ý hôm nay** dùng cùng truy vấn có phân trang (cũng là một loại liên kết đích của banner, UC-ADM-12).
- **Luồng thay thế / Ngoại lệ:**
  - *Danh mục có ít sản phẩm:* bổ sung bằng sản phẩm cùng danh mục cha; vẫn rỗng thì ẩn khối ("Không có sản phẩm tương tự").
- **Quan hệ Use Case:** được bao gồm bởi `UC-CAT-06`.
- **Ràng buộc nghiệp vụ & Kỹ thuật:**
  - Bản TLCN không dùng vector / AI (Qdrant, LLM) — chuyển sang DATN khi có dữ liệu hành vi đủ lớn.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* 🟡 Đang triển khai — backend + test xong, web đã nối API thật (cờ `PUBLIC_REAL_API=product,search,shop`), chờ chạy thật end-to-end — plan `plan/2026-11-17/server-spring/plan_tim_kiem_va_trang_san_pham.md` (2026-11-18).
  - *Kế hoạch:* Sprint 7 (tuần 13–14) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [Product](../ClassDiagram/ENTITY.md#22-product--sản-phẩm-spu), [Category](../ClassDiagram/ENTITY.md#21-category--danh-mục-ngành-hàng).

---

### UC-CAT-08: Quản lý danh sách sản phẩm yêu thích (Wishlist)
- **Mã Use Case:** `UC-CAT-08`
- **Tác nhân chính:** `ACT-CUS` Customer (khách hàng)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Cho phép người dùng lưu lại các sản phẩm quan tâm để mua sau và nhận thông báo khi sản phẩm giảm giá.
- **Tiền điều kiện:** Khách hàng đã đăng nhập tài khoản.
- **Hậu điều kiện:** Thêm/Xóa sản phẩm trong bảng `user_wishlists`.
- **Luồng sự kiện chính:**
  1. Người dùng bấm biểu tượng Trái tim trên thẻ sản phẩm hoặc trang PDP.
  2. Client gửi request `POST /api/v1/customer/wishlist` kèm `product_id`.
  3. Hệ thống kiểm tra: nếu chưa có thì thêm bản ghi vào `user_wishlists`; nếu đã có thì xóa khỏi wishlist (Toggle pattern).
  5. Người dùng có thể vào mục "Danh sách yêu thích" để xem toàn bộ và thêm nhanh vào giỏ hàng.
  6. Nút tim trên trang sản phẩm / thẻ sản phẩm hiển thị trạng thái đầy (đã thích) / rỗng; bấm lần 2 để bỏ thích.
  7. Mục Tài khoản → "Sản phẩm yêu thích": danh sách có ảnh, giá hiện tại, nhãn hết hàng; thao tác bỏ thích, thêm vào giỏ.
- **Quan hệ Use Case:** Không có.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 6 (tuần 11–12) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [UserWishlist](../ClassDiagram/ENTITY.md#25-userwishlist--danh-sách-sản-phẩm-yêu-thích), [Product](../ClassDiagram/ENTITY.md#22-product--sản-phẩm-spu).

---

### UC-CAT-09: Thêm mới sản phẩm & Biến thể SKU (Người bán)
- **Mã Use Case:** `UC-CAT-09`
- **Tác nhân chính:** `ACT-VND` Vendor (người bán/shop)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Cho phép người bán đã được duyệt KYC đăng tải sản phẩm kinh doanh mới lên sàn cùng các biến thể phân loại hàng.
- **Tiền điều kiện:** Người bán đã đăng nhập; `vendor_profiles.kyc_status == 'VERIFIED'`.
- **Hậu điều kiện:** Tạo bản ghi trong `products`, `product_variants`, `product_images`; trạng thái sản phẩm là `PENDING_APPROVAL` (vào hàng đợi duyệt của Staff — `UC-CAT-13`) hoặc `DRAFT`.
- **Luồng sự kiện chính:**
  1. Người bán truy cập Vendor Portal > Sản phẩm > Thêm sản phẩm mới.
  2. Nhập Tên sản phẩm, Chọn ngành hàng (`categoryId`), Thương hiệu, Mô tả chi tiết.
  3. Người bán tải lên ảnh đại diện và tối đa 8 ảnh mô tả (Tải trực tiếp lên Silo bucket `product-media` qua Presigned PUT URLs).
  4. Cấu hình biến thể phân loại:
     - Nhóm 1: Tên thuộc tính (vd: Màu sắc), các giá trị (Đỏ, Xanh).
     - Nhóm 2: Tên thuộc tính (vd: Kích thước), các giá trị (S, M, L).
  5. Hệ thống sinh ma trận tổ hợp SKU (SKU Grid): Mỗi dòng đại diện cho 1 SKU với các trường: Mã SKU, Giá niêm yết, Giá bán, Số lượng tồn kho ban đầu, Cân nặng đóng gói (gram).
  6. Người bán điền giá và tồn kho cho từng biến thể SKU.
  7. Người bán bấm "Gửi kiểm duyệt" (hoặc "Lưu bản nháp").
  8. Hệ thống kiểm tra hợp lệ dữ liệu (Giá bán > 0, Tồn kho >= 0, Cân nặng > 0). Nếu danh mục là **ngành hàng kinh doanh có điều kiện** (`categories.requires_business_license = true`: thuốc, thực phẩm bảo vệ sức khỏe, rượu…) thì bắt buộc tải giấy phép (`businessLicenseUrl`) và nhập ngày hết hạn (`businessLicenseExpiresAt`); `Product.submitForReview(true, now)` từ chối nếu thiếu hoặc đã hết hạn.
  9. Mở transaction lưu `products` (`status = 'PENDING_APPROVAL'`), lưu các dòng `product_variants`, lưu `product_images`.
  10. Hệ thống đưa sản phẩm vào hàng đợi kiểm duyệt thủ công (`UC-CAT-13`) và gửi thông báo in-app cho Staff đang trực (`isActiveModerator = true`).
  11. Trả về thông báo sản phẩm đã được gửi đến hàng đợi kiểm duyệt thành công.
  12. **Thương hiệu:** chọn trong danh sách hoặc "Thêm … làm thương hiệu mới" (`Brand.proposedBy()`, chưa xác minh — sản phẩm luôn qua duyệt thủ công).
  13. **Tình trạng** (Mới / Đã qua sử dụng / Refurbished — `itemCondition`), **xuất xứ** và thông số theo bộ thuộc tính ngành (UC-CAT-15, gợi ý tên thông số).
  14. **Mô tả rich-text** ≤ 10.000 ký tự: in đậm / nghiêng / gạch chân, tiêu đề, danh sách, chèn liên kết và ảnh (ảnh tải lên Silo); HTML được lọc whitelist chống XSS.
  15. **Ảnh:** tối đa 8 ảnh sản phẩm (ảnh đầu là ảnh chính, kéo thả sắp xếp); chọn **thuộc tính có ảnh** để mỗi giá trị phân loại (vd. màu) có ảnh riêng (`variantImageAttribute`).
  16. **Giấy tờ chứng thực:** tối đa 4 tệp PDF / DOC / XLS ≤ 10 MB (không nhận ảnh) → `product_documents`; bắt buộc ít nhất 1 tệp khi danh mục `requiresBusinessLicense = true`.
  17. **Vận chuyển:** cân nặng, kích thước đóng gói, "Gửi hàng từ" (lấy theo kho VND-05), phí ship do người mua chịu hoặc shop hỗ trợ; **Đặt trước** 7–30 ngày nếu shop bật (`markPreOrder`).
  18. Bấm "Đăng bán" → `PENDING_APPROVAL` và kích hoạt kiểm duyệt tự động (UC-CAT-12); "Lưu nháp" giữ `DRAFT`. Biến thể có giá = 0 và tồn = 0 → hỏi xác nhận trước khi lưu.
- **Luồng thay thế / Ngoại lệ:**
  - *8a. Dữ liệu không hợp lệ:* Rollback transaction, đánh dấu lỗi cụ thể trên các ô nhập liệu SKU.
  - *7a. Lưu bản nháp:* Sản phẩm lưu với `status = DRAFT`, không đẩy sang hàng đợi kiểm duyệt.
  - *Không tìm thấy danh mục phù hợp:* người bán đề xuất danh mục mới (UC-CAT-17).
- **Quan hệ Use Case:** `<<include>>` Tải tệp lên Silo (Presigned URL) — tác nhân phụ: `ACT-MIN` Silo S3 Storage; `<<include>>` Kiểm duyệt sản phẩm tự động qua AI (Google Gemini API) (`UC-CAT-12`); được mở rộng bởi `UC-CAT-17` (điểm «Chọn danh mục»).
- **Ràng buộc nghiệp vụ & Kỹ thuật:**
  - Ràng buộc quan hệ: `ON DELETE CASCADE` từ Product sang ProductVariant và ProductImage.
  - Tồn kho ban đầu trong `product_variants.stock_count` phải lớn hơn hoặc bằng 0.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 2 (tuần 3–4) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [Product](../ClassDiagram/ENTITY.md#22-product--sản-phẩm-spu), [ProductVariant](../ClassDiagram/ENTITY.md#23-productvariant--biến-thể-sản-phẩm-sku), [ProductImage](../ClassDiagram/ENTITY.md#24-productimage--ảnh-sản-phẩm), [Brand](../ClassDiagram/ENTITY.md#27-brand--thương-hiệu), [ProductDocument](../ClassDiagram/ENTITY.md#28-productdocument--giấy-tờ-chứng-thực-sản-phẩm), [CategoryAttribute](../ClassDiagram/ENTITY.md#26-categoryattribute--bộ-thuộc-tính-chuẩn-theo-ngành-hàng), [ProductModerationResult](../ClassDiagram/ENTITY.md#210-productmoderationresult--kết-quả-kiểm-duyệt-sản-phẩm-tự-động).

---

### UC-CAT-10: Cập nhật thông tin sản phẩm, giá bán & hình ảnh
- **Mã Use Case:** `UC-CAT-10`
- **Tác nhân chính:** `ACT-VND` Vendor (người bán/shop)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Cho phép người bán cập nhật số lượng tồn kho, giá bán, mô tả hoặc bổ sung biến thể mới trong quá trình kinh doanh.
- **Tiền điều kiện:** Người bán sở hữu sản phẩm này (`products.vendorId == currentVendorId`).
- **Hậu điều kiện:** Cập nhật thông tin trong CSDL; ghi `system_logs` khi đổi giá; nếu đổi tên/ảnh thì chuyển kiểm duyệt lại.
- **Luồng sự kiện chính:**
  1. Người bán chọn sản phẩm cần sửa trong danh sách sản phẩm.
  2. Thay đổi thông tin (giá bán, tồn kho, mô tả, ảnh).
  3. Người bán bấm "Lưu thay đổi".
  4. Nếu giá bán thay đổi: cập nhật `product_variants.price`; đơn đã đặt không bị ảnh hưởng vì giá đã chụp vào `order_items.unitPrice` (lịch sử giá `ProductPriceHistory` để bản mở rộng).
  5. Nếu người bán thay đổi Tên sản phẩm hoặc Ảnh đại diện: Hệ thống chuyển trạng thái `status = PENDING_APPROVAL` và đưa lại vào hàng đợi duyệt của Staff (`UC-CAT-13`).
  6. Nếu chỉ thay đổi giá bán hoặc số lượng tồn kho: Hệ thống cập nhật ngay lập tức mà không cần duyệt lại, đồng thời xóa cache sản phẩm trong Redis.
  7. Sửa trường quan trọng của sản phẩm đang bán (tên, ảnh, mô tả, danh mục, thương hiệu, giấy tờ) → sản phẩm về `PENDING_APPROVAL` (nhãn "CHỜ DUYỆT LẠI") và kiểm duyệt lại (UC-CAT-12 → UC-CAT-13); sửa giá, tồn kho, phân loại không cần duyệt lại.
  8. Sản phẩm đang chờ duyệt chỉ xem, không sửa được ("Sản phẩm đang chờ duyệt. Bạn chỉ có thể xem thông tin…").
- **Quan hệ Use Case:** `<<include>>` Tải tệp lên Silo (Presigned URL) — tác nhân phụ: `ACT-MIN` Silo S3 Storage; `<<include>>` Kiểm duyệt sản phẩm tự động qua AI (Google Gemini API) (`UC-CAT-12`).
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 2 (tuần 3–4) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [Product](../ClassDiagram/ENTITY.md#22-product--sản-phẩm-spu), [ProductDocument](../ClassDiagram/ENTITY.md#28-productdocument--giấy-tờ-chứng-thực-sản-phẩm).

---

### UC-CAT-11: Ẩn / Khóa / Xóa mềm sản phẩm
- **Mã Use Case:** `UC-CAT-11`
- **Tác nhân chính:** `ACT-VND` Vendor (người bán/shop); `ACT-MOD` Moderator (Nhân viên kiểm duyệt); `ACT-ADM` Admin
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Cho phép người bán tạm ẩn sản phẩm hết hàng hoặc Staff/Admin khóa sản phẩm có dấu hiệu vi phạm chính sách kinh doanh; bảo đảm các đơn hàng cũ không bị lỗi toàn vẹn dữ liệu.
- **Tiền điều kiện:** Sản phẩm tồn tại trong CSDL.
- **Hậu điều kiện:** `products.status` cập nhật thành `HIDDEN` (tạm ẩn / khóa do vi phạm) hoặc `DELETED` (xóa mềm); sản phẩm không còn xuất hiện trong kết quả tìm kiếm (truy vấn chỉ lấy `PUBLISHED`).
- **Luồng sự kiện chính:**
  1. Tác nhân chọn thao tác:
     - *Người bán:* Tạm ẩn (`HIDDEN`) để ngừng bán tạm thời, hoặc Xóa mềm sản phẩm (`DELETED`).
     - *Staff / Admin:* Khóa sản phẩm vi phạm (`hide(reason)` → `HIDDEN`, ghi `moderationReason`) do bản quyền, hàng giả, hàng cấm.
  2. Hệ thống thực hiện:
     - Nếu Xóa: Đặt `products.status = DELETED` (xóa mềm qua `@SQLDelete`, tuyệt đối không xóa vật lý `DELETE` khỏi database vì ràng buộc dữ liệu kế toán và lịch sử đơn hàng cũ).
     - Nếu Khóa/Ẩn: Cập nhật `products.status = HIDDEN`; nếu do Staff/Admin khóa thì shop không tự mở lại được, phải sửa và gửi duyệt lại (`UC-CAT-13`).
  3. Hệ thống xóa cache Redis `product:stats:{id}` và ghi `system_logs` nếu thao tác do Staff/Admin.
  4. Nếu sản phẩm bị Admin khóa: Gửi thông báo hệ thống kèm lý do chi tiết cho Người bán.
- **Quan hệ Use Case:** Không có.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 2 (tuần 3–4) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [Product](../ClassDiagram/ENTITY.md#22-product--sản-phẩm-spu).

---

### UC-CAT-12: Kiểm duyệt sản phẩm tự động qua AI (Google Gemini API)
- **Mã Use Case:** `UC-CAT-12`
- **Tác nhân chính:** — (use case nội bộ, được «include» bởi `UC-CAT-09`, `UC-CAT-10`)
- **Tác nhân phụ:** `ACT-GEM` Google Gemini API
- **Mục đích nghiệp vụ:** Tự động sàng lọc sản phẩm mới đăng / vừa sửa (hàng cấm, hàng giả, mô tả sai, ảnh không phù hợp, sai danh mục, giá bất thường) trước khi nhân viên duyệt, để hàng chờ duyệt thủ công tập trung vào ca nghi vấn.
- **Tiền điều kiện:** Sản phẩm vừa chuyển `PENDING_APPROVAL` (do UC-CAT-09 / UC-CAT-10 «include»).
- **Hậu điều kiện:** Tạo một bản ghi `product_moderation_results`; sản phẩm vào hàng chờ CAT-13 (kèm "Phân tích từ AI") hoặc bị từ chối tự động.
- **Luồng sự kiện chính:**
  1. Sau khi giao dịch lưu sản phẩm commit, hệ thống phát sự kiện nội bộ `ProductSubmitted` (Spring `ApplicationEvent` + `@Async`) — người bán không phải chờ.
  2. Bộ kiểm duyệt dựng yêu cầu gửi Gemini API: tên, mô tả (đã bỏ HTML), danh mục, thương hiệu, giá, tối đa 4 ảnh (Presigned URL / base64), tên + loại giấy tờ chứng thực, danh sách chính sách cấm của sàn.
  3. Yêu cầu Gemini trả **JSON theo schema cố định**: `decision` (`APPROVE` / `REJECT` / `NEED_REVIEW`), `riskScore` 0–100, `violations[]` (COUNTERFEIT, PROHIBITED_ITEM, MISLEADING_DESCRIPTION, BAD_IMAGE, PRICE_ANOMALY, WRONG_CATEGORY), `reasoning` tiếng Việt.
  4. Lưu kết quả vào `product_moderation_results` (`modelName`, toàn bộ phản hồi gốc).
  5. Xử lý kết quả:
     - `REJECT` và `riskScore` ≥ ngưỡng cấu hình (mặc định 85): sản phẩm `REJECTED`, `moderationReason` = lý do AI; thông báo người bán (`PRODUCT_REJECTED`) để sửa và gửi lại.
     - `NEED_REVIEW` hoặc `REJECT` dưới ngưỡng: `Product.flag()` (`FLAGGED`), vào hàng chờ CAT-13 **ưu tiên cao**.
     - `APPROVE`: vào hàng chờ CAT-13 ưu tiên thường (TLCN không cho AI tự đăng bán).
  6. Đẩy sự kiện realtime `VENDOR_PRODUCT_UPDATE` cho kênh người bán để cập nhật trạng thái sản phẩm mà không cần tải lại trang.
- **Luồng thay thế / Ngoại lệ:**
  - *3a. Gemini lỗi / quá 20 giây / hết hạn mức gọi:* thử lại 3 lần giãn cách (2 s, 8 s, 30 s); vẫn lỗi → `ProductModerationResult.fallback()` (`NEED_REVIEW`, `fallbackToManual = true`) — chuyển duyệt thủ công.
  - *3b. Phản hồi không đúng JSON schema:* coi như `NEED_REVIEW`, lưu phản hồi gốc để điều tra.
- **Quan hệ Use Case:** được bao gồm bởi `UC-CAT-09`, `UC-CAT-10`; được mở rộng bởi `UC-CAT-13` (điểm «Kết luận kiểm duyệt»).
- **Ràng buộc nghiệp vụ & Kỹ thuật:**
  - Không gửi dữ liệu cá nhân của người bán (CCCD, số tài khoản) và **nội dung** giấy tờ chứng thực sang Gemini — chỉ gửi tên tệp và loại giấy tờ.
  - Khóa `GEMINI_API_KEY`, tên model, ngưỡng tự từ chối đặt trong biến môi trường / cấu hình; chi phí ước tính ở `kien-thuc/BAO_CAO_DU_TOAN_CHI_PHI_VA_GIAI_PHAP_AI_v1.0.md`.
  - Bản DATN giữ phương án AI Worker tự host (CLIP + từ điển từ cấm); hai bản dùng chung bảng `product_moderation_results`.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai — xử lý phía máy chủ (cron / webhook / AI), chưa có backend; giao diện chỉ hiển thị kết quả trên lớp mock.
  - *Kế hoạch:* Sprint 7 (tuần 13–14) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [Product](../ClassDiagram/ENTITY.md#22-product--sản-phẩm-spu), [ProductModerationResult](../ClassDiagram/ENTITY.md#210-productmoderationresult--kết-quả-kiểm-duyệt-sản-phẩm-tự-động), [ProductDocument](../ClassDiagram/ENTITY.md#28-productdocument--giấy-tờ-chứng-thực-sản-phẩm), [Notification](../ClassDiagram/ENTITY.md#101-notification--thông-báo-cá-nhân).

---

### UC-CAT-13: Kiểm duyệt sản phẩm thủ công theo hàng đợi (Staff Moderator)
- **Mã Use Case:** `UC-CAT-13`
- **Tác nhân chính:** `ACT-MOD` Moderator (Nhân viên kiểm duyệt)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Phân công nhân viên kiểm duyệt rà soát sản phẩm chờ duyệt (đã qua kiểm duyệt tự động bằng Gemini — UC-CAT-12), ưu tiên sản phẩm bị gắn cờ `FLAGGED` hoặc bị khiếu nại nhiều lần (mục "Duyệt Sản phẩm" của Admin).
- **Tiền điều kiện:** Có quyền `MANAGE_CATALOG`; nhân viên có cờ `isActiveModerator = true` trong `admin_profiles` mới nhận việc từ hàng đợi.
- **Hậu điều kiện:** Sản phẩm chuyển trạng thái `PUBLISHED` hoặc `REJECTED`; ghi log người duyệt.
- **Luồng sự kiện chính:**
  1. Nhân viên kiểm duyệt truy cập Admin Portal > Hàng đợi duyệt sản phẩm.
  2. Hệ thống tải danh sách sản phẩm chờ duyệt được điều phối theo thuật toán Round-Robin hoặc số lượng việc tồn thấp nhất.
  3. Nhân viên xem chi tiết: Tên sản phẩm, mô tả, ảnh, và **"Phân tích từ AI" / "Lý do AI từ chối"** lấy từ `ProductModerationResult` mới nhất (quyết định, điểm rủi ro, mã vi phạm, giải thích của Gemini), giấy tờ chứng thực (xem trực tiếp / mở tab mới), thông tin vận chuyển, bảng phân loại SKU và thông số kỹ thuật.
  4. Nhân viên ra quyết định:
     - Bấm "Phê duyệt": Sản phẩm chuyển `PUBLISHED`, lưu `moderatorId = staffId`, `search_vector` được trigger cập nhật — sản phẩm xuất hiện ngay trong tìm kiếm.
     - Bấm "Từ chối": Nhập lý do vi phạm chi tiết (ví dụ: "Ảnh mờ, thiếu thông tin tem phụ tiếng Việt"), sản phẩm chuyển `REJECTED`.
  5. Hệ thống gửi thông báo In-App và Email đến người bán về kết quả duyệt.
  6. Lý do từ chối chuẩn: Thiếu chứng nhận an toàn, Vi phạm chính sách, Ảnh sản phẩm không đạt, Mô tả sai / gây hiểu lầm, Giá bất hợp lý, Hàng cấm / hạn chế (+ ghi chú). Kết quả ghi `system_logs` `PRODUCT_MODERATED`, gửi `PRODUCT_APPROVED` / `PRODUCT_REJECTED` realtime cho người bán; duyệt sản phẩm dùng thương hiệu mới thì `Brand.verify()`.
  7. **Cấu hình nhân viên duyệt:** bật / tắt "Đang nhận việc" (`isActiveModerator`) cho từng nhân viên; "Phân phối lại" toàn bộ hàng chờ cho nhân viên đang nhận việc; "Giao lại" một sản phẩm cho nhân viên khác.
  8. Danh sách có tab Chờ duyệt / Đang bán / Bị từ chối, tìm theo tên sản phẩm hoặc tên nhà bán.
- **Quan hệ Use Case:** `<<extend>>` Kiểm duyệt sản phẩm tự động qua AI (Google Gemini API) (`UC-CAT-12`) tại điểm mở rộng «Kết luận kiểm duyệt» khi {Gemini không tự từ chối (APPROVE / NEED_REVIEW / REJECT dưới ngưỡng)}.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 2 (tuần 3–4) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [Product](../ClassDiagram/ENTITY.md#22-product--sản-phẩm-spu) (`moderatorId`), [AdminProfile](../ClassDiagram/ENTITY.md#15-adminprofile--hồ-sơ-quản-trị-viên--nhân-viên), [ProductModerationResult](../ClassDiagram/ENTITY.md#210-productmoderationresult--kết-quả-kiểm-duyệt-sản-phẩm-tự-động), [ProductDocument](../ClassDiagram/ENTITY.md#28-productdocument--giấy-tờ-chứng-thực-sản-phẩm), [Brand](../ClassDiagram/ENTITY.md#27-brand--thương-hiệu), [SystemLog](../ClassDiagram/ENTITY.md#121-systemlog--nhật-ký-kiểm-toán).

---

### UC-CAT-14: Quản lý danh mục sàn toàn cục & Cấu hình tỷ lệ hoa hồng
- **Mã Use Case:** `UC-CAT-14`
- **Tác nhân chính:** `ACT-MOD` Moderator (Nhân viên kiểm duyệt); `ACT-FIN` Finance Officer (Nhân viên tài chính); `ACT-ADM` Admin
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Cho phép nhân viên kiểm duyệt / Admin kiến tạo cấu trúc cây danh mục toàn sàn và áp đặt tỷ lệ hoa hồng thu phí sàn theo từng ngành hàng (Commission Rate).
- **Tiền điều kiện:** Cây danh mục: `ADMIN` hoặc quyền `MANAGE_CATALOG`; tỷ lệ hoa hồng: `ADMIN` hoặc quyền `UPDATE_COMMISSION` (nhân viên tài chính).
- **Hậu điều kiện:** Danh mục được tạo/sửa; xóa cache Redis cây danh mục để đồng bộ toàn hệ thống.
- **Luồng sự kiện chính:**
  1. Admin truy cập Quản trị > Danh mục ngành hàng.
  2. Xem cây danh mục, bấm "Thêm danh mục mới" hoặc bấm sửa danh mục hiện có.
  3. Nhập Tên danh mục, Slug URL, Icon SVG, chọn Danh mục cha (nếu là cấp con).
  4. Cấu hình Tỷ lệ hoa hồng sàn (`commissionRate`, định dạng thập phân `DECIMAL(5,4)`, ví dụ: `0.0500` = 5.0%, `0.0800` = 8.0%).
  5. Bấm "Lưu danh mục".
  6. Hệ thống cập nhật bảng `categories`, đồng thời thực hiện xóa khóa Redis `categories:tree:all` để buộc các request sau nạp lại dữ liệu mới nhất.
  7. Ẩn / hiện danh mục; xóa danh mục chỉ khi không còn sản phẩm và danh mục con (hộp xác nhận "Hành động này không thể hoàn tác").
  8. Màn **Cấu hình hoa hồng**: phí hoa hồng **mặc định toàn sàn** (5%) áp cho mọi ngành chưa cấu hình riêng, và bảng hoa hồng theo ngành **lưu hàng loạt** (`/admin/commissions/bulk`); ghi `system_logs` `COMMISSION_UPDATED`.
  9. Hoa hồng riêng từng shop (`VendorProfile.commissionRate`) đặt ở hồ sơ shop trong màn Duyệt KYC (UC-VND-04); thứ tự áp dụng shop → ngành → mặc định.
  10. Duyệt đề xuất danh mục của người bán ở cùng màn (UC-CAT-17); xem danh sách sản phẩm trong từng danh mục và từ chối sản phẩm vi phạm.
- **Quan hệ Use Case:** Không có.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 2 (tuần 3–4) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [Category](../ClassDiagram/ENTITY.md#21-category--danh-mục-ngành-hàng) (`commissionRate`), [CategoryProposal](../ClassDiagram/ENTITY.md#29-categoryproposal--đề-xuất-danh-mục-mới-của-người-bán), [SystemLog](../ClassDiagram/ENTITY.md#121-systemlog--nhật-ký-kiểm-toán).

---

### UC-CAT-15: Cấu hình bộ thuộc tính chuẩn theo ngành hàng (Category Attributes)
- **Mã Use Case:** `UC-CAT-15`
- **Tác nhân chính:** `ACT-ADM` Admin; `ACT-MOD` Moderator (Nhân viên kiểm duyệt)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Chuẩn hóa thông số sản phẩm theo ngành: thông tin bắt buộc (xuất xứ, bảo hành, chất liệu, hạn sử dụng…), bộ lọc tìm kiếm thống nhất và trục biến thể (Màu, Size) — form đăng bán gợi ý "Gợi ý: Bảo hành, Kết nối, Pin…" theo ngành.
- **Tiền điều kiện:** Ngành hàng đã tồn tại (UC-CAT-14); người thao tác có quyền `MANAGE_CATALOG`.
- **Hậu điều kiện:** Bản ghi `category_attributes` được tạo / cập nhật; cache `cat:attrs:{categoryId}` được làm mới.
- **Luồng sự kiện chính:**
  1. Người thao tác chọn ngành hàng, thêm thuộc tính: `code`, tên hiển thị, `dataType` (TEXT / NUMBER / BOOLEAN / ENUM / MULTI_ENUM / DATE), danh sách giá trị (ENUM), đơn vị (NUMBER), cờ `isRequired`, `isFilterable`, `isVariantAxis`, thứ tự hiển thị.
  2. Hệ thống kiểm tra `code` không trùng trong ngành và các ngành cha (thuộc tính được ngành con kế thừa).
  3. Form đăng bán (UC-CAT-09) tự sinh ô nhập và gợi ý theo bộ thuộc tính; khi gửi duyệt, Service gọi `CategoryAttribute.accepts()` cho từng thuộc tính — thiếu thuộc tính bắt buộc / sai kiểu thì báo lỗi theo từng ô.
  4. Thuộc tính `isFilterable` xuất hiện làm bộ lọc ở trang tìm kiếm (UC-CAT-05).
- **Luồng thay thế / Ngoại lệ:**
  - *Thêm thuộc tính bắt buộc cho ngành đã có sản phẩm:* sản phẩm cũ không bị gỡ ngay — thông báo shop bổ sung trong 14 ngày.
  - *Ngừng dùng thuộc tính:* `deactivate()` — dữ liệu cũ trong `specsJson` giữ nguyên.
- **Quan hệ Use Case:** Không có.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 7 (tuần 13–14) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [CategoryAttribute](../ClassDiagram/ENTITY.md#26-categoryattribute--bộ-thuộc-tính-chuẩn-theo-ngành-hàng), [Category](../ClassDiagram/ENTITY.md#21-category--danh-mục-ngành-hàng), [Product](../ClassDiagram/ENTITY.md#22-product--sản-phẩm-spu).

---

### UC-CAT-16: Xem trang gian hàng (Shop Page)
- **Mã Use Case:** `UC-CAT-16`
- **Tác nhân chính:** `ACT-GST` Guest (khách vãng lai); `ACT-CUS` Customer (khách hàng)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Cho người mua xem toàn bộ sản phẩm đang bán của một shop cùng thông tin uy tín (tên, logo, ảnh bìa, mô tả, điểm đánh giá trung bình, số sản phẩm, ngày tham gia) để quyết định mua và chat với shop.
- **Tiền điều kiện:** Shop có `kycStatus = VERIFIED` và tài khoản chủ shop đang `ACTIVE`.
- **Hậu điều kiện:** Hiển thị trang shop với danh sách sản phẩm `PUBLISHED` phân trang.
- **Luồng sự kiện chính:**
  1. Người dùng bấm vào tên shop ở trang chi tiết sản phẩm (`UC-CAT-06`), trong giỏ hàng hoặc trong kết quả tìm kiếm.
  2. Client gọi `GET /api/v1/public/shops/{id}` và `GET /api/v1/public/products?shop_id={id}&sort=NEWEST&page=1` (API_SPEC 5.11, 5.3; `id` = `vendor_profiles.id`).
  3. Backend trả thông tin shop từ `vendor_profiles` (`shopName`, `logoUrl`, `coverUrl`, `shopDescription`, `createdAt`) cùng số liệu tổng hợp: tổng sản phẩm đang bán, điểm đánh giá trung bình của shop (trung bình `product_reviews.rating` đang hiển thị), số đơn đã hoàn tất.
  4. Hệ thống hiển thị lưới sản phẩm của shop, cho phép sắp xếp (mới nhất, bán chạy, giá tăng / giảm) và lọc theo danh mục sàn.
  5. Người dùng có thể bấm "Chat với shop" (`UC-SOC-01`) hoặc chọn sản phẩm để xem chi tiết (`UC-CAT-06`).
  6. Thanh thông tin shop: logo, ảnh bìa, tên, nhãn **Chính hãng**, huy hiệu **Shop Yêu Thích** (`VendorProfile.isPreferredShop()`), trạng thái Online / "Tham gia từ …".
  7. Chỉ số: **Đánh giá shop** (`ratingAvg` / `ratingCount`), **Người theo dõi** (`followerCount`), **Sản phẩm** (`activeProductCount`), **Phản hồi chat** (`chatResponseRate` % — "phản hồi trong vài phút / vài giờ" theo `avgResponseMinutes`).
  8. Nút **Theo dõi** (UC-SOC-06), **Chat ngay** (UC-SOC-01); voucher của shop có nút "Lưu" (UC-PRO-03); tab Tất cả sản phẩm / Mới nhất / Bán chạy, ô "Tìm trong Shop"; nhãn tạm nghỉ (UC-VND-19).
- **Luồng thay thế / Ngoại lệ:**
  - *2a. Shop không tồn tại, chưa duyệt KYC hoặc bị khóa:* trả `404` — "Gian hàng không tồn tại hoặc đã ngừng hoạt động".
  - *4a. Shop chưa có sản phẩm nào:* hiển thị trạng thái rỗng "Shop chưa đăng sản phẩm".
- **Quan hệ Use Case:** `<<extend>>` Xem chi tiết sản phẩm, ma trận biến thể SKU & Tồn kho (`UC-CAT-06`) tại điểm mở rộng «Bấm vào tên shop» khi {người dùng bấm vào tên shop}.
- **Ràng buộc nghiệp vụ & Kỹ thuật:**
  - Số liệu tổng hợp của shop cache Redis `shop:stats:{vendorId}` TTL 10 phút; truy vấn danh sách dùng index `(vendor_id, status)`.
  - Chỉ số shop được cập nhật bởi job hằng ngày (tỉ lệ phản hồi) và sự kiện (đánh giá mới / bị ẩn, theo dõi, đăng / ẩn sản phẩm) — không tính trực tiếp mỗi lần mở trang.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* 🟡 Đang triển khai — backend + test xong, web đã nối API thật (cờ `PUBLIC_REAL_API=product,search,shop`), chờ chạy thật end-to-end — plan `plan/2026-11-17/server-spring/plan_tim_kiem_va_trang_san_pham.md` (2026-11-18).
  - *Kế hoạch:* Sprint 2 (tuần 3–4) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [VendorProfile](../ClassDiagram/ENTITY.md#14-vendorprofile--hồ-sơ-người-bán-shop), [Product](../ClassDiagram/ENTITY.md#22-product--sản-phẩm-spu), [ProductReview](../ClassDiagram/ENTITY.md#91-productreview--đánh-giá-sản-phẩm), [ShopFollow](../ClassDiagram/ENTITY.md#92-shopfollow--theo-dõi-shop), [VendorShippingConfig](../ClassDiagram/ENTITY.md#73-vendorshippingconfig--cấu-hình-vận-chuyển-của-shop).

---

### UC-CAT-17: Người bán đề xuất danh mục mới & Duyệt đề xuất
- **Mã Use Case:** `UC-CAT-17`
- **Tác nhân chính:** `ACT-VND` Vendor (người bán/shop); `ACT-MOD` Moderator (Nhân viên kiểm duyệt); `ACT-ADM` Admin
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Khi không tìm thấy ngành hàng phù hợp, người bán đề xuất danh mục mới (mục "Danh mục toàn sàn" của kênh người bán); nhân viên kiểm duyệt duyệt để tạo danh mục hoặc từ chối kèm lý do (khối "Đề xuất danh mục từ người bán" của Admin).
- **Tiền điều kiện:** Người bán đã `VERIFIED`; người duyệt có quyền `MANAGE_CATALOG`.
- **Hậu điều kiện:** `category_proposals` được tạo; khi duyệt: `categories` mới được tạo và đề xuất `APPROVED`; khi từ chối: `REJECTED` kèm lý do.
- **Luồng sự kiện chính:**
  1. Người bán mở cây danh mục toàn sàn (chỉ xem), bấm "Đề xuất danh mục mới".
  2. Nhập tên danh mục, chọn danh mục cha (hoặc để trống = danh mục gốc), giải thích lý do → gửi.
  3. Hệ thống kiểm tra tên chưa tồn tại cùng cấp, mỗi shop tối đa 5 đề xuất `PENDING`; tạo `category_proposals` (`PENDING`).
  4. Nhân viên kiểm duyệt mở danh sách đề xuất, xem shop đề xuất và danh mục cha.
  5. Duyệt: hệ thống tạo `Category` (slug tự sinh, hoa hồng mặc định của danh mục cha) rồi `CategoryProposal.approve()`; thông báo người bán.
- **Luồng thay thế / Ngoại lệ:**
  - *5a. Từ chối:* nhập lý do bắt buộc → `reject()`; thông báo người bán kèm lý do.
  - *3a. Trùng danh mục đã có:* gợi ý danh mục có sẵn, không tạo đề xuất.
- **Quan hệ Use Case:** `<<extend>>` Thêm mới sản phẩm & Biến thể SKU (Người bán) (`UC-CAT-09`) tại điểm mở rộng «Chọn danh mục» khi {không có danh mục phù hợp}.
- **Ràng buộc nghiệp vụ & Kỹ thuật:**
  - Chỉ `ADMIN` hoặc người có `UPDATE_COMMISSION` sửa được tỷ lệ hoa hồng của danh mục vừa tạo (UC-CAT-14).
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 7 (tuần 13–14) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [CategoryProposal](../ClassDiagram/ENTITY.md#29-categoryproposal--đề-xuất-danh-mục-mới-của-người-bán), [Category](../ClassDiagram/ENTITY.md#21-category--danh-mục-ngành-hàng), [Notification](../ClassDiagram/ENTITY.md#101-notification--thông-báo-cá-nhân).

---

### UC-CAT-18: Chia sẻ sản phẩm bằng liên kết (Deeplink)
- **Mã Use Case:** `UC-CAT-18`
- **Tác nhân chính:** `ACT-GST` Guest (khách vãng lai); `ACT-CUS` Customer (khách hàng)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Cho phép chia sẻ sản phẩm qua mạng xã hội / tin nhắn (nút "Chia sẻ" trên trang chi tiết); người nhận mở liên kết đi thẳng tới đúng sản phẩm, kể cả biến thể đã chọn, và nguồn truy cập được ghi nhận.
- **Tiền điều kiện:** Sản phẩm ở trạng thái `PUBLISHED`.
- **Hậu điều kiện:** Liên kết đã được sao chép / chia sẻ; lượt mở liên kết được tính nguồn `EXTERNAL_LINK`.
- **Luồng sự kiện chính:**
  1. Người dùng bấm "Chia sẻ" trên trang chi tiết sản phẩm.
  2. Hệ thống tạo liên kết chuẩn `https://genie.vn/p/{slug}-{productId}?v={variantId}&src=share` (biến thể đang chọn nếu có).
  3. Trên thiết bị hỗ trợ Web Share API: mở bảng chia sẻ của hệ điều hành; nếu không: sao chép vào bộ nhớ tạm và báo "Đã sao chép liên kết".
  4. Khi liên kết được dán lên Facebook / Zalo / Messenger: trang sản phẩm render phía máy chủ (Astro SSR) với thẻ Open Graph (`og:title`, `og:image`, `og:price:amount`) để hiện ảnh xem trước.
  5. Người nhận mở liên kết: chuyển thẳng tới trang chi tiết, chọn sẵn biến thể; tham số `src=share` được ghi vào thống kê nguồn truy cập (UC-VND-08).
- **Luồng thay thế / Ngoại lệ:**
  - *Sản phẩm đã ẩn / xóa:* trang báo "Sản phẩm không còn bán" kèm sản phẩm tương tự (UC-CAT-07).
  - *`slug` đã đổi sau khi chia sẻ:* tìm theo `productId` và chuyển hướng 301 về liên kết mới.
- **Quan hệ Use Case:** `<<extend>>` Xem chi tiết sản phẩm, ma trận biến thể SKU & Tồn kho (`UC-CAT-06`) tại điểm mở rộng «Chia sẻ» khi {người dùng bấm “Chia sẻ”}.
- **Ràng buộc nghiệp vụ & Kỹ thuật:**
  - Không gắn mã giới thiệu / theo dõi cá nhân vào liên kết ở TLCN (chương trình giới thiệu bạn bè, affiliate thuộc DATN).
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* 🟡 Đang triển khai — backend + test xong, web đã nối API thật (cờ `PUBLIC_REAL_API=product,search,shop`), chờ chạy thật end-to-end — plan `plan/2026-11-17/server-spring/plan_tim_kiem_va_trang_san_pham.md` (2026-11-18).
  - *Kế hoạch:* Sprint 7 (tuần 13–14) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [Product](../ClassDiagram/ENTITY.md#22-product--sản-phẩm-spu), [ProductViewStat](../ClassDiagram/ENTITY.md#212-productviewstat--lượt-xem-sản-phẩm-theo-ngày--nguồn-truy-cập).

---

<a id="ph-ord"></a>
## PHÂN HỆ 03: GIỎ HÀNG, ĐẶT HÀNG & ĐƠN HÀNG (CART, CHECKOUT & ORDERS)

> **Phạm vi:** Giỏ hàng (mini-cart, mua lại), xem trước & tách đơn theo shop, phí vận chuyển, tạo phiên checkout, tự hủy đơn quá hạn, theo dõi / hủy đơn, xác nhận đã nhận, tự hoàn tất sau 7 ngày.
> **Use case (9):** `UC-ORD-01`, `UC-ORD-04`, `UC-ORD-06`, `UC-ORD-07`, `UC-ORD-08`, `UC-ORD-09`, `UC-ORD-10`, `UC-ORD-11`, `UC-ORD-12`

### UC-ORD-01: Quản lý giỏ hàng mua sắm cá nhân (Shopping Cart)
- **Mã Use Case:** `UC-ORD-01`
- **Tác nhân chính:** `ACT-GST` Guest (khách vãng lai); `ACT-CUS` Customer (khách hàng)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Cho phép khách hàng lưu trữ các mặt hàng muốn mua, thay đổi số lượng, xóa từng món hoặc chọn các món cần thanh toán; khách vãng lai được giữ giỏ tạm và không mất giỏ khi đăng nhập.
- **Tiền điều kiện:** Customer đã đăng nhập (giỏ lưu ở `carts`); Guest chưa đăng nhập dùng giỏ tạm lưu trên trình duyệt.
- **Hậu điều kiện:** Bảng `carts` và `cart_items` được cập nhật (hoặc giỏ tạm trên trình duyệt với Guest); số lượng hiển thị trên icon giỏ hàng header thay đổi.
- **Luồng sự kiện chính:**
  1. Người dùng bấm "Thêm vào giỏ" từ trang sản phẩm (đã chọn SKU và số lượng Q).
  2. Backend kiểm tra tính hợp lệ: SKU tồn tại, sản phẩm đang `PUBLISHED`, và Người bán không phải chính tài khoản của khách hàng (Cấm tự mua hàng của shop mình — `GNE-ORD-4094`; chủ shop ở chế độ Mua hàng vẫn thêm được sản phẩm của shop khác).
  3. Backend kiểm tra tồn kho: `stockCount >= Q`.
  4. Mở transaction:
     - Nếu SKU chưa có trong giỏ: Tạo bản ghi `cart_items` mới với số lượng Q.
     - Nếu SKU đã có sẵn: Cập nhật `quantity = quantity + Q` (nếu tổng không vượt tồn kho).
  5. Khi khách hàng mở trang Giỏ hàng:
     - Hệ thống tự động **gom nhóm sản phẩm theo từng Shop** (`vendor_id`).
     - Hiển thị giá niêm yết, giá khuyến mãi hiện tại, ô chọn checkbox từng món hoặc chọn toàn bộ món của 1 Shop.
     - Cho phép bấm nút tăng/giảm số lượng hoặc xóa dòng sản phẩm.
  6. **Mini-cart ở header:** thêm vào giỏ hiện thông báo "Đã thêm vào giỏ" và danh sách "Sản phẩm mới thêm" (5 dòng gần nhất), "Còn N sản phẩm trong giỏ", nút "Xem Giỏ Hàng".
  7. Chọn tất cả / xóa nhiều món đã chọn (hộp xác nhận); cảnh báo vượt tồn "Sản phẩm này chỉ còn N sản phẩm trong kho".
  8. **Mua lại / Mua lại tất cả** (từ Đơn mua): thêm lại các SKU còn bán với giá hiện tại; SKU hết hàng / ngừng bán được bỏ qua và báo cho khách.
- **Luồng thay thế / Ngoại lệ:**
  - *3a. Số lượng yêu cầu vượt quá tồn kho hiện có:* Báo lỗi 400 "Số lượng trong kho không đủ đáp ứng".
  - *Sản phẩm bị thay đổi giá trong lúc để trong giỏ:* Giao diện hiển thị cảnh báo "Giá của sản phẩm X vừa thay đổi từ A sang B" để khách hàng nắm rõ trước khi đặt hàng.
- **Luồng thay thế — Giỏ hàng khách vãng lai & gộp giỏ khi đăng nhập:**
  1. Guest bấm "Thêm vào giỏ": client lưu `{productVariantId, quantity}` vào giỏ tạm trên trình duyệt (LocalStorage, hạn 30 ngày), kiểm tra tồn kho qua API công khai; không tạo bản ghi `carts`.
  2. Guest bấm "Đặt hàng" → hệ thống yêu cầu đăng nhập / đăng ký (`UC-AUTH-01`, `UC-AUTH-02`); giỏ tạm được giữ nguyên.
  3. Đăng nhập thành công: client gửi giỏ tạm tới `POST /api/v1/me/cart/merge`; service bỏ SKU không còn `PUBLISHED`, bỏ sản phẩm của chính shop người dùng, giới hạn số lượng theo tồn kho rồi gọi `Cart.mergeGuestItems()` (SKU trùng được cộng dồn).
  4. Trả về danh sách dòng bị điều chỉnh / loại bỏ để giao diện thông báo; client xóa giỏ tạm.
- **Quan hệ Use Case:** Không có.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 3 (tuần 5–6) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [Cart](../ClassDiagram/ENTITY.md#31-cart--giỏ-hàng), [CartItem](../ClassDiagram/ENTITY.md#32-cartitem--dòng-sản-phẩm-trong-giỏ).

---

### UC-ORD-04: Xem trước đơn hàng & Tách giỏ hàng đa nhà bán (Cart Preview & Splitting)
- **Mã Use Case:** `UC-ORD-04`
- **Tác nhân chính:** `ACT-CUS` Customer (khách hàng)
- **Tác nhân phụ:** `ACT-3PL` Đối tác vận chuyển (GHN)
- **Mục đích nghiệp vụ:** Cho phép khách hàng xem trước chi tiết chi phí, tự động phân rã giỏ hàng chứa sản phẩm của nhiều người bán khác nhau thành các gói hàng độc lập, tính cước vận chuyển riêng cho từng gói từ các kho xuất phát điểm khác nhau.
- **Tiền điều kiện:** Giỏ hàng có ít nhất 1 sản phẩm được chọn mua; khách hàng đã chọn địa chỉ nhận hàng.
- **Hậu điều kiện:** Trả về đối tượng Preview gồm Tạm tính từng Shop, Phí ship từng Shop, Giảm giá Voucher (shop + sàn) và Tổng tiền cuối cùng.
- **Luồng sự kiện chính:**
  1. Khách hàng tích chọn các món hàng trong giỏ và bấm "Mua hàng". Giao diện chuyển sang màn hình Checkout Preview.
  2. Client gọi API `POST /api/v1/checkout/preview`.
  3. **Thuật toán Tách Giỏ Hàng (Cart Splitting):**
     - Backend nhóm toàn bộ các dòng hàng theo `vendor_id`. Giả sử giỏ có hàng của Shop A và Shop B, hệ thống tách thành 2 kiện hàng riêng biệt.
     - Với mỗi Shop: Lấy địa chỉ kho lấy hàng (`pickupProvinceId`, `pickupDistrictId`, `pickupWardCode`) từ `vendor_profiles`.
     - Tính tổng trọng lượng các món hàng của Shop đó.
  4. **Tính phí vận chuyển độc lập:**
     - Gọi API GHN tính cước từ Kho Shop A đến Địa chỉ Khách ➔ Phí ship A.
     - Gọi API vận chuyển tính cước từ Kho Shop B đến Địa chỉ Khách ➔ Phí ship B.
     - Tổng phí vận chuyển = Phí ship A + Phí ship B.
  5. Tính toán giảm giá Voucher (nếu có áp dụng): Phân bổ chiết khấu tương ứng cho từng dòng hàng (Prorated Discount).
  6. Trả về cấu trúc JSON xem trước đơn hàng đầy đủ để khách hàng rà soát trước khi đặt lệnh thanh toán.
  7. Mỗi shop có ô **"Lời nhắn cho Shop"** (≤ 500 ký tự) — lưu vào `Order.buyerNote` khi tạo đơn.
  8. Voucher (UC-PRO-04) và Điểm (UC-PRO-10) được tính lại ngay khi khách đổi lựa chọn.
- **Luồng thay thế / Ngoại lệ:**
  - *4a. API vận chuyển bên thứ ba bị timeout hoặc lỗi mạng:* Hệ thống tự động kích hoạt cơ chế dự phòng Fallback sang Bảng phí cố định vùng (Flat Fee: ví dụ 15.000 VNĐ nội tỉnh, 30.000 VNĐ liên tỉnh) để phiên xem trước không bị gián đoạn.
- **Quan hệ Use Case:** `<<include>>` Tính phí vận chuyển qua GHN API (`UC-ORD-06`); được bao gồm bởi `UC-ORD-07`; được mở rộng bởi `UC-PRO-04` (điểm «Chọn voucher»).
- **Ràng buộc nghiệp vụ & Kỹ thuật:** Đơn hàng từ các người bán khác nhau tuyệt đối không gộp chung 1 gói hàng vì xuất phát từ các kho vật lý ở các tỉnh thành khác nhau.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 3 (tuần 5–6) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [CheckoutSession](../ClassDiagram/ENTITY.md#51-checkoutsession--phiên-thanh-toán), [VendorProfile](../ClassDiagram/ENTITY.md#14-vendorprofile--hồ-sơ-người-bán-shop), [Address](../ClassDiagram/ENTITY.md#13-address--địa-chỉ-giao-hàng).

---

### UC-ORD-06: Tính phí vận chuyển qua GHN API
- **Mã Use Case:** `UC-ORD-06`
- **Tác nhân chính:** — (use case nội bộ, được «include» bởi `UC-ORD-04`)
- **Tác nhân phụ:** `ACT-3PL` Đối tác vận chuyển (GHN)
- **Mục đích nghiệp vụ:** Tính cước vận chuyển cho từng kiện hàng (mỗi shop 1 kiện) dựa trên địa chỉ kho shop, địa chỉ nhận, trọng lượng và kích thước quy đổi, theo bảng giá của GHN.
- **Tiền điều kiện:** Shop đã cấu hình kho lấy hàng (`VendorProfile.isPickupConfigured()`, `UC-VND-05`); khách đã chọn địa chỉ nhận có `districtId`, `wardCode` chuẩn GHN (`UC-AUTH-09`).
- **Hậu điều kiện:** Trả về số tiền cước chi tiết cho từng kiện hàng; kết quả được cache 10 phút.
- **Luồng sự kiện chính:**
  1. Hệ thống (trong `UC-ORD-04`) gom thông tin từng kiện: `from_district_id`, `from_ward_code` (kho shop), `to_district_id`, `to_ward_code` (địa chỉ khách), tổng `weight` (gram) và kích thước lớn nhất của các sản phẩm.
  2. Kiểm tra cache Redis `ship:fee:{hash(tham số)}`; có thì trả ngay.
  3. Gửi request tới API GHN `POST /shiip/public-api/v2/shipping-order/fee` với token của sàn và `shop_id` = `VendorProfile.ghnShopId`.
  4. Nhận số tiền cước, lưu cache 10 phút và trả về cho bước tính tổng đơn hàng.
  5. Shop đạt ngưỡng **freeship của shop** (`VendorShippingConfig.freeshipThreshold`): phí ship khách trả = 0, shop chịu (`EscrowLedger.shopShippingSubsidy`).
  6. Sản phẩm đặt trước: ngày giao dự kiến cộng thêm `preOrderDays`.
- **Luồng thay thế / Ngoại lệ:**
  - *3a. GHN timeout (> 3 giây) hoặc lỗi:* dùng bảng phí cố định dự phòng (15.000đ nội tỉnh, 30.000đ liên tỉnh), đánh dấu "phí tạm tính" trên giao diện; phí thật được cập nhật khi tạo vận đơn (`UC-LOG-02`) và chênh lệch do sàn chịu.
  - *3b. GHN không phục vụ tuyến (khu vực không hỗ trợ):* báo khách chọn địa chỉ khác; không cho đặt kiện của shop đó.
- **Quan hệ Use Case:** được bao gồm bởi `UC-ORD-04`.
- **Ràng buộc nghiệp vụ & Kỹ thuật:** Gói dịch vụ mặc định GHN "Chuẩn"; `Product.shippingPayer = SELLER` thì phí ship hiển thị 0đ cho khách và ghi `EscrowLedger.shopShippingSubsidy`.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 3 (tuần 5–6) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [VendorProfile](../ClassDiagram/ENTITY.md#14-vendorprofile--hồ-sơ-người-bán-shop), [Address](../ClassDiagram/ENTITY.md#13-address--địa-chỉ-giao-hàng), [Product](../ClassDiagram/ENTITY.md#22-product--sản-phẩm-spu), [VendorShippingConfig](../ClassDiagram/ENTITY.md#73-vendorshippingconfig--cấu-hình-vận-chuyển-của-shop).

---

### UC-ORD-07: Tạo phiên Checkout & Các Đơn hàng con theo Shop (PersistCheckout)
- **Mã Use Case:** `UC-ORD-07`
- **Tác nhân chính:** `ACT-CUS` Customer (khách hàng)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Tạo phiên thanh toán cha (`checkout_sessions`) và N đơn hàng con (`orders`) tương ứng với N người bán trong cùng 1 transaction CSDL nguyên tử (ACID), chụp ảnh dữ liệu bất biến (Snapshot Pattern).
- **Tiền điều kiện:** Giỏ hàng hợp lệ, đã giữ chỗ tồn kho thành công, đã chọn phương thức thanh toán.
- **Hậu điều kiện:** 1 bản ghi `checkout_sessions` (status `INITIATED`), N bản ghi `orders` (status `PENDING_PAYMENT`), các bản ghi `order_items`, `order_discounts` được lưu bền vững vào PostgreSQL 16.
- **Luồng sự kiện chính:**
  1. Khách hàng bấm "Xác nhận đặt hàng".
  2. Backend khởi động 1 Database Transaction duy nhất (`@Transactional` của Spring — một `TransactionTemplate` duy nhất).
  3. Tạo bản ghi cha `checkout_sessions`: Lưu tổng tiền toàn phiên (`grandTotal`), `userId`, trạng thái `INITIATED`.
  4. Với mỗi Shop trong giỏ hàng:
     - Sinh mã đơn hàng duy nhất `orderCode` theo định dạng chuẩn **`TR-yyyyMMdd-######`** (6 chữ số tuần tự trong ngày lấy từ Redis `INCR order:seq:{yyyyMMdd}`, vd `TR-20261015-000123` — khớp ENTITY.md mục 5.2).
     - Tạo bản ghi `orders`: Lưu `checkoutSessionId`, `vendorId`, `subTotal`, `shippingFee`, `discountAmount`, `platformDiscountAmount`, `orderCode`, `sourceType` (NORMAL / LIVESTREAM / AFFILIATE / SPONSORED_AD kèm `sourceId`), trạng thái `PENDING_PAYMENT`.
     - **Chụp ảnh dữ liệu bất biến (Snapshot):** Ghi toàn bộ tên người nhận, SĐT, địa chỉ chi tiết vào `shippingAddressSnapshotJson`.
     - Duyệt qua từng sản phẩm của Shop: Tạo `order_items`, chụp lại `productNameSnapshot`, `skuSnapshot`, đơn giá chốt `unitPrice`, tỷ lệ hoa hồng `commissionRate` (= `VendorProfile.effectiveCommissionRate(categoryRate)`: shop → ngành hàng → 5%).
     - Phân bổ chiết khấu voucher vào từng dòng hàng (`shopDiscount`, `platformDiscount`, `coinDiscount`, `proratedDiscount`) theo thực thể `OrderItem`.
  5. Nếu có dùng Voucher: Tạo các bản ghi `order_discounts` (mỗi đơn con 1 dòng), ghi **1 bản ghi `voucher_usages` cho mỗi voucher trong phiên** (`checkoutSessionId`; `orderId` chỉ có với voucher SHOP) và trừ 1 lượt `usageLimit`.
  6. Xóa các sản phẩm đã đặt mua khỏi `cart_items` của khách hàng.
  7. Commit transaction.
  8. Chuyển hướng sang bước khởi tạo thanh toán (`UC-PAY-01`).
  9. Lưu `Order.buyerNote`; áp kết quả xếp chồng voucher + Điểm (UC-PRO-04, UC-PRO-10): ghi `coinUsedCount`, `coinDiscountAmount`, `OrderItem.coinDiscount`, trừ Điểm trong cùng transaction.
  10. Sản phẩm đang Flash Sale: giữ suất theo UC-PRO-06; shop đang tạm nghỉ → từ chối (`GNE-ORD-4093`).
  11. Sau khi thanh toán: trang **"Đặt hàng thành công"** (mã giao dịch, mã phiên, danh sách đơn tách theo shop "Cung cấp bởi …", tạm tính, giảm giá voucher & khuyến mãi, nút Về trang chủ / Xem đơn mua) hoặc **"Thanh toán thất bại"** (`PAYMENT_FAILED`, tồn kho vẫn được giữ, nút Thử thanh toán lại / Đổi phương thức / Về giỏ hàng).
- **Luồng thay thế / Ngoại lệ:**
  - *Giỏ có sản phẩm của chính shop người mua:* từ chối toàn phiên với `GNE-ORD-4094` — kiểm tra lại ở bước này vì sản phẩm có thể đã vào giỏ trước khi người dùng mở shop.
  - *Bất kỳ bước nào gặp lỗi (DB lỗi, khóa ngoại lỗi):* `tx.Rollback()`, toàn bộ dữ liệu trở về trạng thái ban đầu, giỏ hàng giữ nguyên, báo lỗi cho khách hàng.
- **Quan hệ Use Case:** `<<include>>` Xem trước đơn hàng & Tách giỏ hàng đa nhà bán (Cart Preview & Splitting) (`UC-ORD-04`); được mở rộng bởi `UC-PRO-06` (điểm «Giữ chỗ tồn kho»).
- **Ràng buộc nghiệp vụ & Kỹ thuật:**
  - Áp dụng triệt để **Snapshot Pattern**: Đơn hàng cũ không bao giờ bị biến đổi giá trị hay tên sản phẩm dù sau này người bán có sửa giá hay đổi tên.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 3 (tuần 5–6) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [CheckoutSession](../ClassDiagram/ENTITY.md#51-checkoutsession--phiên-thanh-toán), [Order](../ClassDiagram/ENTITY.md#52-order--đơn-hàng-theo-từng-shop), [OrderItem](../ClassDiagram/ENTITY.md#53-orderitem--dòng-sản-phẩm-trong-đơn), [OrderDiscount](../ClassDiagram/ENTITY.md#54-orderdiscount--chi-tiết-các-mã-đã-áp-cho-đơn), [UserCoin](../ClassDiagram/ENTITY.md#47-usercoin--số-dư-điểm), [FlashSaleItem](../ClassDiagram/ENTITY.md#46-flashsaleitem--sản-phẩm-trong-flash-sale).

---

### UC-ORD-08: Tự động hủy đơn quá hạn thanh toán & Hoàn kho (Sweeper Cron)
- **Mã Use Case:** `UC-ORD-08`
- **Tác nhân chính:** `ACT-CRN` Platform Cron / Scheduler
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Quét và hủy các đơn hàng chưa thanh toán quá 15 phút, giải phóng tồn kho đã giữ chỗ để người khác có thể mua, hoàn trả lượt dùng voucher.
- **Tiền điều kiện:** Có các bản ghi `checkout_sessions` ở trạng thái `INITIATED` / `PAYMENT_PENDING` hoặc `stock_reservations` ở trạng thái `ACTIVE` mà `expires_at < now()`.
- **Hậu điều kiện:** `checkout_sessions` chuyển `EXPIRED`; các `orders` con chuyển `AUTO_CANCELLED`; giảm `reservedCount` (tồn vật lý `stockCount` **không đổi** vì chưa từng bị trừ); hoàn lượt dùng Voucher.
- **Luồng sự kiện chính:**
  1. Scheduled Task nền chạy theo chu kỳ mỗi 60 giây (`@Scheduled(fixedRate = 60000)`).
  2. Truy vấn danh sách các `checkout_sessions` có `status IN ('INITIATED','PAYMENT_PENDING')` và `created_at < now() - 15 phút` (`FOR UPDATE SKIP LOCKED`).
  3. Với mỗi phiên hết hạn, mở transaction:
     - Cập nhật `checkout_sessions.status = 'EXPIRED'`.
     - Cập nhật các `orders` con trực thuộc thành `status = 'AUTO_CANCELLED'`; lý do hủy (`PAYMENT_TIMEOUT`) ghi vào `system_logs` (`ORDER_STATUS_CHANGED`) — `orders` không có cột lý do hủy.
     - Nhả giữ chỗ **có điều kiện** để không đua với IPN: `UPDATE stock_reservations SET status = 'EXPIRED', released_at = now() WHERE id = ? AND status = 'ACTIVE'` (`StockReservation.release(now, true)`, `@Version`). Chỉ khi cập nhật được 1 dòng mới giảm `UPDATE product_variants SET reserved_count = reserved_count - Q WHERE id = ? AND reserved_count >= Q`; 0 dòng → IPN đã `CONFIRMED` trước, bỏ qua phiên này.
     - Nếu có dùng Voucher: Giảm `vouchers.used_count = used_count - 1`, xóa bản ghi trong `voucher_usages`.
  4. Commit transaction và ghi log hệ thống.
  5. Hoàn Điểm đã dùng (`ORDER_REFUND`), trả lượt voucher và trả suất Flash Sale (`FlashSaleItem.releaseReservation()`).
- **Quan hệ Use Case:** Không có.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai — xử lý phía máy chủ (cron / webhook / AI), chưa có backend; giao diện chỉ hiển thị kết quả trên lớp mock.
  - *Kế hoạch:* Sprint 3 (tuần 5–6) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [CheckoutSession](../ClassDiagram/ENTITY.md#51-checkoutsession--phiên-thanh-toán), [Order](../ClassDiagram/ENTITY.md#52-order--đơn-hàng-theo-từng-shop), [StockReservation](../ClassDiagram/ENTITY.md#55-stockreservation--giữ-chỗ-tồn-kho).

---

### UC-ORD-09: Khách hàng theo dõi chi tiết hành trình đơn hàng
- **Mã Use Case:** `UC-ORD-09`
- **Tác nhân chính:** `ACT-CUS` Customer (khách hàng)
- **Tác nhân phụ:** `ACT-3PL` Đối tác vận chuyển (GHN)
- **Mục đích nghiệp vụ:** Cho phép người mua tra cứu trạng thái đơn hàng theo thời gian thực từ lúc đặt hàng, đóng gói, bàn giao vận chuyển, đang giao hàng cho đến khi giao thành công.
- **Tiền điều kiện:** Đơn hàng thuộc về tài khoản khách hàng đang đăng nhập.
- **Hậu điều kiện:** Hiển thị Timeline dòng thời gian các mốc sự kiện giao nhận.
- **Luồng sự kiện chính:**
  1. Khách hàng vào mục Đơn mua > Chi tiết đơn hàng.
  2. Client gọi `GET /api/v1/customer/orders/{id}`.
  3. Server trả về thông tin trạng thái theo chuỗi chuẩn hóa 19 trạng thái:
     - Với đơn Online: `PENDING_PAYMENT` ➔ `PAID` ➔ `CONFIRMED` ➔ `PREPARING` ➔ `READY_FOR_PICKUP` ➔ `DELIVERING` ➔ `DELIVERED` ➔ `COMPLETED`.
     - Với đơn COD: `PENDING_PAYMENT` ➔ `CONFIRMED` ➔ `PREPARING` ➔ `READY_FOR_PICKUP` ➔ `DELIVERING` ➔ `DELIVERED` ➔ `PAID` ➔ `COMPLETED`.
  4. Server trả về mã vận đơn (`ghnOrderCode` hoặc `trackingCode` của `ShipmentTracking` / `DeliveryOrder`), đơn vị vận chuyển, lịch sử các sự kiện lấy từ bảng `shipment_events`.
- **Quan hệ Use Case:** Không có.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 4 (tuần 7–8) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [Order](../ClassDiagram/ENTITY.md#52-order--đơn-hàng-theo-từng-shop), [ShipmentTracking](../ClassDiagram/ENTITY.md#71-shipmenttracking--vận-đơn), [ShipmentEvent](../ClassDiagram/ENTITY.md#72-shipmentevent--sự-kiện-hành-trình).

---

### UC-ORD-10: Khách hàng yêu cầu hủy đơn hàng
- **Mã Use Case:** `UC-ORD-10`
- **Tác nhân chính:** `ACT-CUS` Customer (khách hàng)
- **Tác nhân phụ:** `ACT-PGW` Payment Gateway (VNPay, SePay)
- **Mục đích nghiệp vụ:** Cho phép khách hàng chủ động hủy đơn khi đổi ý hoặc muốn đổi địa chỉ, với điều kiện người bán chưa đóng gói và chưa bàn giao cho bên vận chuyển.
- **Tiền điều kiện:** Đơn hàng đang ở trạng thái `PENDING_PAYMENT`, `PAID`, `CONFIRMED` hoặc `PREPARING` (chưa chuyển sang `READY_FOR_PICKUP` hoặc `DELIVERING`).
- **Hậu điều kiện:** Đơn hàng chuyển `CANCELLED`; giải phóng tồn kho; hoàn tiền tự động nếu đơn đã thanh toán.
- **Luồng sự kiện chính:**
  1. Khách hàng bấm "Hủy đơn" trên giao diện chi tiết đơn hàng, chọn lý do hủy (Đổi ý, Đổi địa chỉ, Tìm được giá rẻ hơn...).
  2. Backend kiểm tra máy trạng thái đơn hàng (State Machine):
     - Nếu trạng thái là `READY_FOR_PICKUP`, `DELIVERING` hoặc `DELIVERED`: Từ chối hủy, thông báo: "Đơn hàng đã được đóng gói hoặc bàn giao cho đơn vị vận chuyển, không thể hủy trực tiếp. Vui lòng nhận hàng rồi tạo yêu cầu Trả hàng/Hoàn tiền".
     - Nếu trạng thái hợp lệ (`PENDING_PAYMENT`, `PAID`, `CONFIRMED`, `PREPARING`): Mở transaction cập nhật `orders.status = 'CANCELLED'` (hoặc `'CANCELLED_BY_VENDOR'` nếu shop hủy). **Shop hủy được ngay từ `PENDING_PAYMENT`** (đơn COD đang chờ shop xác nhận hoặc đơn online chưa thanh toán: hết hàng, không giao tới khu vực) mà không phải xác nhận đơn trước; tỷ lệ shop tự hủy hiển thị trong báo cáo shop (`UC-VND-08`).
  3. Hoàn trả tồn kho theo mô hình giữ chỗ: đơn `PENDING_PAYMENT` → `StockReservation.release()` rồi `reserved_count = reserved_count − Q` (chưa trừ tồn cứng); đơn đã `PAID` / `CONFIRMED` / `PREPARING` (đã `commitSale`) → `stock_count = stock_count + Q`.
  4. Nếu đơn hàng đã thanh toán trực tuyến (`PAID`):
     - Hệ thống tự tạo `RefundRequest` (`REFUND_ONLY`, `APPROVED`) và hoàn tiền theo `UC-SUP-04` (VNPay qua API refund; VietQR chuyển khoản tay, SePay xác nhận).
     - Cập nhật số tiền ký quỹ trong `escrow_ledgers` thành `REFUNDED`.
  5. Hoàn lại lượt dùng Voucher cho khách hàng.
  6. Gửi thông báo cho Người bán biết đơn hàng đã bị hủy.
- **Luồng thay thế / Ngoại lệ:**
  - *Hủy cả nhóm đơn (phiên checkout chưa thanh toán):* nút "Huỷ nhóm đơn" hủy mọi đơn con `PENDING_PAYMENT` của phiên trong một transaction — hoàn Điểm và voucher, nhả giữ chỗ tồn kho.
  - *Đơn chờ thanh toán:* nút "Thanh toán ngay" chuyển sang UC-PAY-01 (thanh toán lại cho phiên còn hiệu lực).
- **Quan hệ Use Case:** Không có.
- **Ràng buộc nghiệp vụ & Kỹ thuật — Hủy một phần:**
  - Đơn vị hủy là **đơn con theo shop** (`orders`); một phiên `checkout_sessions` nhiều shop có thể hủy từng đơn con, các đơn con còn lại tiếp tục vòng đời bình thường.
  - Hoàn tiền và hoàn tồn kho chỉ tính trên phần đã phân bổ cho đơn con bị hủy (`order_discounts`, `OrderItem` — xem phân bổ chiết khấu UC-PRO-04); phần giảm giá đã phân bổ cho đơn con bị hủy **không** chuyển sang đơn còn lại; lượt dùng voucher sàn chỉ được hoàn khi **toàn bộ** đơn con của phiên bị hủy.
  - Không bỏ từng món trong một đơn con trước khi giao; muốn bỏ một món thì hủy đơn con và đặt lại, hoặc nhận hàng rồi yêu cầu trả hàng một phần (`UC-SUP-01`).
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 4 (tuần 7–8) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [Order](../ClassDiagram/ENTITY.md#52-order--đơn-hàng-theo-từng-shop), [EscrowLedger](../ClassDiagram/ENTITY.md#63-escrowledger--sổ-ký-quỹ-tiền-tạm-giữ).

---

### UC-ORD-11: Khách hàng xác nhận "Đã nhận hàng thành công"
- **Mã Use Case:** `UC-ORD-11`
- **Tác nhân chính:** `ACT-CUS` Customer (khách hàng)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Người mua xác nhận đã nhận đúng và đủ hàng, đồng ý giải phóng tiền ký quỹ của đơn về ví người bán sớm hơn hạn 7 ngày.
- **Tiền điều kiện:** Đơn đã giao (`deliveredAt ≠ null`) và đang ở `DELIVERED` (VNPay / VietQR, hoặc COD chưa đối soát) hoặc `PAID` (COD đã đối soát với GHN); không có yêu cầu trả hàng đang mở.
- **Hậu điều kiện:** Đơn hàng chuyển `COMPLETED`; nếu ký quỹ đã `HELD` thì tiền được giải ngân ngay vào ví người bán.
- **Luồng sự kiện chính:**
  1. Khách hàng nhận hàng từ bưu tá GHN, kiểm tra gói hàng và bấm "Đã nhận được hàng".
  2. Client gọi `POST /api/v1/customer/orders/{id}/confirm-received`.
  3. Backend kiểm tra đơn thuộc khách hàng và đang `DELIVERED` / `PAID`; gọi `Order.markCustomerConfirmed(now)` (idempotent, ghi `customerConfirmedAt`).
  4. Kiểm tra ký quỹ của đơn:
     - **`escrow_ledgers.status = HELD`** (VNPay, hoặc COD đã đối soát): mở transaction — `Order.complete()` → `COMPLETED`; `EscrowLedger.release(now)` trả `netAmount`; `VendorWallet.credit(netAmount)` và ghi `WalletTransaction` `CREDIT_ESCROW_RELEASE`; ghi `system_logs` `ESCROW_RELEASED`.
     - **`PENDING_FUNDING`** (COD, GHN chưa chuyển tiền thu hộ): **không giải ngân**; vẫn ghi nhận xác nhận của khách, báo "Đơn COD đang chờ đối soát với GHN, tiền sẽ tự giải ngân cho shop khi đối soát xong" — khi Staff đối soát (`UC-LOG-03`), hệ thống thấy `customerConfirmedAt` đã có nên tự hoàn tất đơn và giải ngân ngay.
  5. Gửi thông báo in-app cho người bán (`ESCROW_RELEASED` / `ORDER_STATUS_CHANGED`) và mở nút "Đánh giá sản phẩm" cho khách (`UC-SOC-03`).
- **Luồng thay thế / Ngoại lệ:**
  - *1a. Hàng bị lỗi / bể vỡ / giao thiếu:* khách không xác nhận mà bấm "Yêu cầu Trả hàng / Hoàn tiền" ➔ `UC-SUP-01`.
- **Quan hệ Use Case:** được mở rộng bởi `UC-SUP-01` (điểm «Báo hàng có vấn đề»).
- **Ràng buộc nghiệp vụ & Kỹ thuật:** Giải ngân **chỉ** khi `EscrowLedger.status = HELD` (đã có tiền thật trong tài khoản sàn) — chống chiếm dụng vốn sàn với đơn COD.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 4 (tuần 7–8) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [Order](../ClassDiagram/ENTITY.md#52-order--đơn-hàng-theo-từng-shop), [EscrowLedger](../ClassDiagram/ENTITY.md#63-escrowledger--sổ-ký-quỹ-tiền-tạm-giữ), [VendorWallet](../ClassDiagram/ENTITY.md#64-vendorwallet--ví-tiền-của-người-bán), [WalletTransaction](../ClassDiagram/ENTITY.md#65-wallettransaction--lịch-sử-biến-động-ví).

---

### UC-ORD-12: Hệ thống tự động hoàn tất đơn sau 7 ngày từ khi Delivered
- **Mã Use Case:** `UC-ORD-12`
- **Tác nhân chính:** `ACT-CRN` Platform Cron / Scheduler
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Bảo đảm dòng tiền cho người bán: nếu người mua không bấm "Đã nhận hàng", hệ thống tự hoàn tất đơn sau **7 ngày (168 giờ)** kể từ khi GHN giao thành công — trùng cửa sổ khiếu nại của người mua.
- **Tiền điều kiện:** Đơn `DELIVERED` (VNPay / VietQR) hoặc `PAID` (COD đã đối soát), `delivered_at <= now() - 7 ngày`, ký quỹ `HELD`, không có yêu cầu trả hàng đang mở.
- **Hậu điều kiện:** Đơn chuyển `COMPLETED`; ký quỹ đủ điều kiện được giải ngân bởi `UC-PAY-07`.
- **Luồng sự kiện chính:**
  1. `@Scheduled(cron = "0 0 * * * *")` chạy mỗi giờ, truy vấn:
     ```sql
     SELECT o.id FROM orders o JOIN escrow_ledgers e ON e.order_id = o.id
     WHERE o.status IN ('DELIVERED', 'PAID') AND o.delivered_at <= NOW() - INTERVAL '7 days'
       AND e.status = 'HELD'
       AND NOT EXISTS (SELECT 1 FROM refund_requests r WHERE r.order_id = o.id
                       AND r.status IN ('REQUESTED','AWAITING_EVIDENCE','SHOP_REJECTED','DISPUTED','AWAITING_RETURN_SHIPMENT','APPROVED'))
     FOR UPDATE SKIP LOCKED LIMIT 200;
     ```
  2. Với từng đơn: `Order.complete()` → `COMPLETED`, `EscrowLedger.scheduleRelease(now)` đặt `releaseEligibleAt = now`.
  3. `UC-PAY-07` giải ngân ngay trong lượt chạy. Đơn COD chưa đối soát (`DELIVERED` + ký quỹ `PENDING_FUNDING`) **không** bị chọn — đơn đó được hoàn tất ở bước đối soát (`UC-LOG-03`) vì `markPaid()` chỉ hợp lệ khi đơn còn `DELIVERED`.
  4. Ghi `system_logs` `ORDER_STATUS_CHANGED` với lý do "Tự động hoàn tất do hết hạn 7 ngày".
- **Quan hệ Use Case:** `<<include>>` Tự động giải phóng tiền ký quỹ về ví người bán (`UC-PAY-07`).
- **Ràng buộc nghiệp vụ & Kỹ thuật:** Không bao giờ giải ngân ký quỹ ở trạng thái `PENDING_FUNDING`, `DISPUTED`, `FROZEN`.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai — xử lý phía máy chủ (cron / webhook / AI), chưa có backend; giao diện chỉ hiển thị kết quả trên lớp mock.
  - *Kế hoạch:* Sprint 4 (tuần 7–8) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [Order](../ClassDiagram/ENTITY.md#52-order--đơn-hàng-theo-từng-shop), [EscrowLedger](../ClassDiagram/ENTITY.md#63-escrowledger--sổ-ký-quỹ-tiền-tạm-giữ), [RefundRequest](../ClassDiagram/ENTITY.md#81-refundrequest--yêu-cầu-hoàn-tiền).

---

<a id="ph-pay"></a>
## PHÂN HỆ 04: THANH TOÁN, KÝ QUỸ (ESCROW) & SỔ CÁI (PAYMENTS, ESCROW & LEDGER)

> **Phạm vi:** Thanh toán VNPay / VietQR (SePay) / COD, IPN & webhook idempotent, ký quỹ & khấu trừ hoa hồng, sổ cái kép, giải ngân về ví người bán.
> **Use case (5):** `UC-PAY-01`, `UC-PAY-03`, `UC-PAY-05`, `UC-PAY-06`, `UC-PAY-07`

### UC-PAY-01: Khởi tạo thanh toán trực tuyến (VNPay / chuyển khoản VietQR qua SePay)
- **Mã Use Case:** `UC-PAY-01`
- **Tác nhân chính:** `ACT-CUS` Customer (khách hàng)
- **Tác nhân phụ:** `ACT-PGW` Payment Gateway (VNPay, SePay)
- **Mục đích nghiệp vụ:** Tạo giao dịch thanh toán trực tuyến an toàn và điều hướng người dùng sang cổng thanh toán đối tác để thực hiện trừ tiền thẻ/ví.
- **Tiền điều kiện:** Phiên `checkout_sessions` đã được tạo thành công ở trạng thái `INITIATED`.
- **Hậu điều kiện:** Tạo bản ghi trong `payment_transactions` với `status = PENDING`; trả về URL thanh toán an toàn.
- **Luồng sự kiện chính:**
  1. Sau khi tạo phiên checkout ở `UC-ORD-07`, client gọi API `POST /api/v1/me/checkout-sessions/{id}/payments`.
  2. Backend kiểm tra phương thức thanh toán khách chọn:
     - **VNPay:** Dựng các tham số `vnp_Version`, `vnp_Command=pay`, `vnp_TmnCode`, `vnp_Amount = grandTotal * 100`, `vnp_CreateDate`, `vnp_CurrCode=VND`, `vnp_TxnRef = checkoutSessionId`, `vnp_OrderInfo`, `vnp_ReturnUrl`.
     - Sắp xếp các tham số theo thứ tự alphabet (ASCII), tạo chuỗi hash data và ký số **HMAC-SHA512** với Secret Key.
     - Nối chữ ký `vnp_SecureHash` vào URL cổng thanh toán.
     - **Chuyển khoản ngân hàng VietQR (qua SePay):** Backend sinh mã thanh toán `TR{paymentTransactionId}` và ảnh QR động chuẩn VietQR (`https://qr.sepay.vn/img?acc={STK sàn}&bank={mã NH}&amount={grandTotal}&des=TR{id}`); client hiển thị QR + số tài khoản + nội dung chuyển khoản, đếm ngược 15 phút và hỏi trạng thái mỗi 3 giây (hoặc nhận qua WebSocket). Không có Return URL — xác nhận hoàn toàn bằng webhook SePay (`UC-PAY-03`).
  3. Tạo bản ghi `payment_transactions`: Lưu `checkoutSessionId`, `paymentMethod = 'VNPAY'`, `amount`, `status = 'PENDING'`, `idempotencyKey = vnp_TxnRef`.
  4. Trả URL thanh toán về cho client; client chuyển hướng trình duyệt của khách hàng sang VNPay.
- **Luồng thay thế / Ngoại lệ:**
  - *Đơn hàng có tổng tiền sau giảm giá = 0đ (voucher 100%):* không gọi VNPay; hệ thống xác nhận nội bộ như IPN thành công (`PaymentTransaction` số tiền 0, `status = SUCCESS`).
  - *Thanh toán lại / đổi phương thức:* trên màn "Thanh toán thất bại" hoặc Đơn mua ("Thanh toán ngay"), khi phiên còn giữ chỗ 15 phút, khách chọn lại VNPay / VietQR / COD → tạo `PaymentTransaction` mới cho cùng `CheckoutSession` (giữ nguyên đơn đã chốt, không xóa / tạo lại đơn, không làm trống giỏ).
  - *Màn VietQR:* hiển thị mã QR động, ngân hàng, số tài khoản sàn, số tiền, nội dung `TR{id}`, đồng hồ đếm ngược 15 phút; trang tự chuyển "Thanh toán thành công" khi webhook SePay báo nhận tiền (UC-PAY-03).
- **Quan hệ Use Case:** Không có.
- **Ràng buộc nghiệp vụ & Kỹ thuật:**
  - Phương thức của TLCN: **VNPay, chuyển khoản VietQR (SePay), COD** (COD dùng chung mô hình ký quỹ — escrow `PENDING_FUNDING` tới khi GHN đối soát). MoMo / ZaloPay / thẻ quốc tế / BNPL thuộc bản DATN.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 3 (tuần 5–6) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [PaymentTransaction](../ClassDiagram/ENTITY.md#61-paymenttransaction--giao-dịch-thanh-toán), [CheckoutSession](../ClassDiagram/ENTITY.md#51-checkoutsession--phiên-thanh-toán).

---

### UC-PAY-03: Xử lý IPN VNPay & webhook SePay idempotent
- **Mã Use Case:** `UC-PAY-03`
- **Tác nhân chính:** `ACT-PGW` Payment Gateway (VNPay, SePay)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Tiếp nhận kết quả thanh toán bất đồng bộ từ cổng thanh toán đối tác qua Webhook (Instant Payment Notification - IPN), xác thực chữ ký bảo mật và cập nhật trạng thái đơn hàng một cách an toàn, chống xử lý trùng lặp (Idempotent).
- **Tiền điều kiện:** Cổng thanh toán gửi request POST/GET đến Webhook URL của Genie.
- **Hậu điều kiện:** Xác thực chữ ký đúng; cập nhật giao dịch thành công; chuyển đơn sang `PAID`; ghi nhận sổ ký quỹ; phản hồi JSON mã `00` cho cổng thanh toán.
- **Luồng sự kiện chính:**
  1. Cổng thanh toán gọi Webhook URL `GET /api/v1/payments/vnpay/ipn` kèm các tham số query.
  2. Backend trích xuất toàn bộ tham số, tách riêng `vnp_SecureHash`.
  3. Dựng lại chuỗi hash data từ các tham số còn lại, băm HMAC-SHA512 với bí mật `VNPAY_HASH_SECRET`.
  4. So khớp chữ ký:
     - Nếu chữ ký không khớp: Trả về `{"RspCode": "97", "Message": "Invalid Checksum"}`, ghi log cảnh báo bảo mật.
  5. Nếu chữ ký hợp lệ: Tìm `payment_transactions` theo `vnp_TxnRef`.
  6. **Kiểm tra tính lặp lại (Idempotency Check):**
     - Nếu giao dịch đã có `status == 'SUCCESS'`: Trả ngay `{"RspCode": "02", "Message": "Order already confirmed"}` (tránh cộng tiền hoặc cập nhật đơn 2 lần khi Return URL và IPN gọi đồng thời).
  7. Kiểm tra mã phản hồi giao dịch `vnp_ResponseCode`:
     - Nếu `vnp_ResponseCode == "00"` (Thành công): Mở transaction CSDL:
       - Cập nhật `payment_transactions.status = 'SUCCESS'`.
       - `StockReservation.confirm()` + `ProductVariant.commitSale(qty)` (trừ tồn cứng), cập nhật `checkout_sessions` → `COMPLETED` và các `orders` con → `PAID` (`Order.markPaid()`).
       - Kích hoạt `UC-PAY-05`: Ghi nhận dòng tiền ký quỹ `escrow_ledgers`.
     - Nếu `vnp_ResponseCode != "00"` (Thất bại / Hủy): Cập nhật `payment_transactions.status = 'FAILED'`, hoàn kho.
  8. Trả về `{"RspCode": "00", "Message": "Confirm Success"}` cho cổng thanh toán.
- **Luồng thay thế / Ngoại lệ:**
  - *Không tìm thấy mã đơn hàng:* Trả `{"RspCode": "01", "Message": "Order not found"}`.
  - *Số tiền gửi lên không khớp với số tiền lưu trong DB:* Trả `{"RspCode": "04", "Message": "Invalid Amount"}`.
  - *Webhook SePay (chuyển khoản VietQR):* SePay gọi `POST /api/v1/payments/sepay/webhook` (header `Authorization: Apikey {SEPAY_API_KEY}`) với `id`, `gateway`, `transactionDate`, `accountNumber`, `content`, `transferType`, `transferAmount`, `referenceCode`. Backend chỉ xử lý `transferType = in`, tách mã `TR{id}` từ `content`, chống trùng theo `id` của SePay (UNIQUE trên `payment_transactions.gateway_transaction_id`), so `transferAmount` với `amount`: đủ → xử lý như IPN thành công; thiếu / thừa / không có mã → ghi giao dịch chờ Staff xử lý tay. Luôn trả HTTP 200 `{"success": true}` để SePay không gửi lại (SePay tự thử lại tối đa 7 lần nếu lỗi kết nối). *Tiền về sau khi phiên đã hết hạn 15 phút (`EXPIRED`, giữ chỗ đã nhả):* thử giữ chỗ lại — còn hàng → xác nhận như thanh toán thành công; hết hàng → đơn `AUTO_CANCELLED`, đưa khoản tiền vào hàng đợi hoàn tiền chuyển khoản (Staff hoàn trong 3 ngày làm việc, SePay xác nhận tiền ra).
- **Quan hệ Use Case:** `<<include>>` Ghi nhận ký quỹ (Escrow) & Khấu trừ hoa hồng (`UC-PAY-05`); `<<include>>` Ghi sổ cái tài chính kép bất biến (Financial Ledger) (`UC-PAY-06`).
- **Ràng buộc nghiệp vụ & Kỹ thuật:**
  - Bắt buộc kiểm tra Idempotency để chống Race Condition khi người dùng bấm quay lại từ trình duyệt (Return URL) cùng lúc với server-to-server IPN.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai — xử lý phía máy chủ (cron / webhook / AI), chưa có backend; giao diện chỉ hiển thị kết quả trên lớp mock.
  - *Kế hoạch:* Sprint 3 (tuần 5–6) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [PaymentTransaction](../ClassDiagram/ENTITY.md#61-paymenttransaction--giao-dịch-thanh-toán), [Order](../ClassDiagram/ENTITY.md#52-order--đơn-hàng-theo-từng-shop), [AdminNotification](../ClassDiagram/ENTITY.md#105-adminnotification--thông-báo-nội-bộ-cho-quản-trị-viên).

---

### UC-PAY-05: Ghi nhận ký quỹ (Escrow) & Khấu trừ hoa hồng
- **Mã Use Case:** `UC-PAY-05`
- **Tác nhân chính:** — (use case được bao gồm bởi `UC-PAY-03`; với COD được kích hoạt khi tạo đơn ở `UC-ORD-07`)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Đảm bảo tiền của khách hàng được sàn nắm giữ an toàn trong tài khoản trung gian (Escrow); tách bạch rõ ràng Doanh thu gộp của Shop, Tiền cước vận chuyển và Hoa hồng sàn (Take Rate) được hưởng.
- **Tiền điều kiện:** Đơn hàng vừa chuyển trạng thái `PAID` (online/ví) hoặc `CONFIRMED` (COD).
- **Hậu điều kiện:** Tạo bản ghi trong `escrow_ledgers` với `status = HELD` (online/ví) hoặc `PENDING_FUNDING` (COD).
- **Luồng sự kiện chính:**
  1. Với mỗi đơn hàng con (`Order`) vừa thanh toán:
  2. Lấy tỷ lệ hoa hồng áp dụng **cho từng dòng hàng** bằng `VendorProfile.effectiveCommissionRate(categoryRate)`: tỷ lệ **thương lượng riêng của Shop** (`vendor_profiles.commissionRate`, ghi đè) → nếu NULL thì tỷ lệ **Ngành hàng** của sản phẩm (`categories.commissionRate`) → nếu vẫn NULL thì **mặc định toàn sàn 5%**. Tỷ lệ được chụp vào `OrderItem.applyCommission(rate)`.
  3. Tính toán dòng tiền:
     - `OrderItem.grossAmount` = `lineTotal - shopDiscount`; `OrderItem.commissionAmount` = `grossAmount × commissionRate` (mỗi dòng theo ngành hàng của sản phẩm đó).
     - `EscrowLedger.grossAmount` = Σ `OrderItem.grossAmount`; `EscrowLedger.commissionFee` = Σ `OrderItem.commissionAmount` (không nhân một tỷ lệ chung — đơn có hàng điện tử 3.5% và thời trang 8% vẫn khớp từng dòng).
     - `EscrowLedger.netAmount` = `grossAmount - commissionFee - shopShippingSubsidy`.
  4. Tạo bản ghi `escrow_ledgers`:
     - `orderId = order.id`, `vendorId = order.vendorId`.
     - `grossAmount`, `commissionFee`, `netAmount`, `gatewayFundedAmount` / `codFundedAmount` theo nguồn tiền.
     - `status = 'HELD'` với đơn VNPay đã thanh toán đủ; `PENDING_FUNDING` với đơn COD (chờ đối soát COD với GHN — `UC-LOG-03`).
     - `releaseEligibleAt = deliveredAt + 7 ngày` (đặt bởi `scheduleRelease()` khi đơn `DELIVERED`).
  5. Ghi bút toán sổ cái `ESCROW_IN` / `COMMISSION` trong cùng transaction (UC-PAY-06).
- **Quan hệ Use Case:** `<<include>>` Ghi sổ cái tài chính kép bất biến (Financial Ledger) (`UC-PAY-06`); được bao gồm bởi `UC-PAY-03`.
- **Ràng buộc nghiệp vụ & Kỹ thuật:**
  - Tiền ký quỹ không được phép rút cho đến khi đơn hoàn tất hoặc hết thời gian khiếu nại.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai — xử lý phía máy chủ (cron / webhook / AI), chưa có backend; giao diện chỉ hiển thị kết quả trên lớp mock.
  - *Kế hoạch:* Sprint 3 (tuần 5–6) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [EscrowLedger](../ClassDiagram/ENTITY.md#63-escrowledger--sổ-ký-quỹ-tiền-tạm-giữ), [OrderItem](../ClassDiagram/ENTITY.md#53-orderitem--dòng-sản-phẩm-trong-đơn), [VendorProfile](../ClassDiagram/ENTITY.md#14-vendorprofile--hồ-sơ-người-bán-shop), [Category](../ClassDiagram/ENTITY.md#21-category--danh-mục-ngành-hàng).

---

### UC-PAY-06: Ghi sổ cái tài chính kép bất biến (Financial Ledger)
- **Mã Use Case:** `UC-PAY-06`
- **Tác nhân chính:** — (use case nội bộ, được «include» bởi `UC-PAY-05`, `UC-PAY-07`, `UC-ADM-05`, `UC-SUP-04`, `UC-PAY-03`)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Ghi nhận mọi dòng tiền của sàn theo nguyên tắc kế toán kép (Tổng Nợ = Tổng Có) để đối soát và lập báo cáo — nguồn dữ liệu cho màn "Sổ cái tài chính" của Admin.
- **Tiền điều kiện:** Phát sinh sự kiện tài chính: tiền vào ký quỹ (VNPay / VietQR / GHN chuyển COD), thu hoa hồng, sàn tài trợ voucher / freeship / Điểm, giải ngân, chi rút tiền, hoàn tiền, phí cổng.
- **Hậu điều kiện:** Một cặp (hoặc nhóm) bút toán mới trong `financial_ledger`.
- **Luồng sự kiện chính:**
  1. Use case gọi tới (PAY-05, PAY-07, ADM-05, SUP-04) truyền sự kiện kèm số tiền và mã tham chiếu.
  2. Hệ thống tạo các bút toán `FinancialLedger.entry()` trong **cùng transaction** với thay đổi nghiệp vụ, ví dụ khi tiền vào ký quỹ: Nợ `GATEWAY_CLEARING` / Có `ESCROW_LIABILITY` (`ESCROW_IN`); khi giải ngân: Nợ `ESCROW_LIABILITY` / Có `VENDOR_PAYABLE` và `COMMISSION_REVENUE`.
  3. Kiểm tra tổng Nợ = tổng Có của nhóm bút toán trước khi commit; lệch → hủy transaction và cảnh báo.
  4. Màn "Sổ cái tài chính" (UC-ADM-06) đọc bảng này theo thứ tự `seq`, lọc theo thời gian / sự kiện / mã tham chiếu và xuất CSV.
- **Luồng thay thế / Ngoại lệ:**
  - *Ghi sai:* không sửa / xóa dòng cũ — ghi **bút toán đảo** có cùng `referenceId`.
- **Quan hệ Use Case:** được bao gồm bởi `UC-PAY-05`, `UC-PAY-07`, `UC-ADM-05`, `UC-SUP-04`, `UC-PAY-03`.
- **Ràng buộc nghiệp vụ & Kỹ thuật:**
  - Tài khoản CSDL của ứng dụng chỉ có quyền `INSERT, SELECT` trên `financial_ledger` (cấm `UPDATE`, `DELETE`).
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai — xử lý phía máy chủ (cron / webhook / AI), chưa có backend; giao diện chỉ hiển thị kết quả trên lớp mock.
  - *Kế hoạch:* Sprint 3 (tuần 5–6) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [FinancialLedger](../ClassDiagram/ENTITY.md#67-financialledger--sổ-cái-tài-chính-chỉ-ghi-thêm), [EscrowLedger](../ClassDiagram/ENTITY.md#63-escrowledger--sổ-ký-quỹ-tiền-tạm-giữ), [PaymentTransaction](../ClassDiagram/ENTITY.md#61-paymenttransaction--giao-dịch-thanh-toán).

---

### UC-PAY-07: Tự động giải phóng tiền ký quỹ về ví người bán
- **Mã Use Case:** `UC-PAY-07`
- **Tác nhân chính:** `ACT-CRN` Platform Cron / Scheduler
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Khi đơn hàng hoàn tất hoặc sau 7 ngày ký quỹ an toàn, tiền được tự động chuyển từ tài khoản ký quỹ sàn sang số dư ví người bán (`vendor_wallets`).
- **Tiền điều kiện:** Đơn hàng ở trạng thái `COMPLETED` và `escrow_ledgers.status == 'HELD'`.
- **Hậu điều kiện:** `escrow_ledgers.status = 'RELEASED'`; số dư `vendor_wallets.balance` tăng; tạo `wallet_transactions`.
- **Luồng sự kiện chính:**
  1. Cron Job định kỳ quét các bản ghi `escrow_ledgers` thỏa mãn điều kiện giải ngân.
  2. Mở transaction CSDL:
     - Cập nhật `escrow_ledgers.status = 'RELEASED'`, `released_at = now()`.
     - Khóa dòng ví người bán bằng khóa bi quan có thời hạn: `SELECT * FROM vendor_wallets WHERE id = ? FOR UPDATE`.
     - Cập nhật số dư: `UPDATE vendor_wallets SET balance = balance + net_payout, version = version + 1 WHERE id = ?`.
     - Tạo bản ghi `wallet_transactions` ghi nhận dòng tiền `CREDIT_ESCROW_RELEASE`.
  3. Commit transaction. Gửi thông báo đẩy đến Vendor Portal.
  4. Ghi sổ cái `ESCROW_RELEASE`, `PAYOUT_ACCRUE` và chi phí sàn tài trợ của đơn (`PLATFORM_VOUCHER`, `FREESHIP_COST`, `COIN_COST`) — UC-PAY-06.
- **Quan hệ Use Case:** `<<include>>` Ghi sổ cái tài chính kép bất biến (Financial Ledger) (`UC-PAY-06`); được bao gồm bởi `UC-ORD-12`.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai — xử lý phía máy chủ (cron / webhook / AI), chưa có backend; giao diện chỉ hiển thị kết quả trên lớp mock.
  - *Kế hoạch:* Sprint 4 (tuần 7–8) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [EscrowLedger](../ClassDiagram/ENTITY.md#63-escrowledger--sổ-ký-quỹ-tiền-tạm-giữ), [VendorWallet](../ClassDiagram/ENTITY.md#64-vendorwallet--ví-tiền-của-người-bán), [WalletTransaction](../ClassDiagram/ENTITY.md#65-wallettransaction--lịch-sử-biến-động-ví), [FinancialLedger](../ClassDiagram/ENTITY.md#67-financialledger--sổ-cái-tài-chính-chỉ-ghi-thêm).

---

<a id="ph-vnd"></a>
## PHÂN HỆ 05: GIAN HÀNG & THẨM ĐỊNH KYC NGƯỜI BÁN (VENDOR MANAGEMENT & KYC)

> **Phạm vi:** Mở shop, KYC (AI Worker + duyệt thủ công), kho lấy hàng GHN, chat tự động, tài khoản nhận tiền, báo cáo & phân tích, tạm nghỉ, ví & sổ giao dịch, thông tin gian hàng & cài đặt vận hành.
> **Use case (11):** `UC-VND-01`, `UC-VND-02`, `UC-VND-03`, `UC-VND-04`, `UC-VND-05`, `UC-VND-06`, `UC-VND-07`, `UC-VND-08`, `UC-VND-19`, `UC-VND-20`, `UC-VND-21`

### UC-VND-01: Đăng ký mở gian hàng (Become a Seller)
- **Mã Use Case:** `UC-VND-01`
- **Tác nhân chính:** `ACT-CUS` Customer (khách hàng)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Cho phép người dùng đã có tài khoản khách hàng nâng cấp lên vai trò Người bán (Vendor), mở cửa hàng kinh doanh trên sàn mà không cần đăng ký tài khoản email mới (Mô hình Single Table Account + Profile mở rộng).
- **Tiền điều kiện:** Người dùng đã đăng nhập với vai trò `CUSTOMER`; chưa từng tạo hồ sơ người bán (`VendorProfile == NULL`).
- **Hậu điều kiện:** Tạo bản ghi trong `vendor_profiles` với `kycStatus = NOT_SUBMITTED`; tài khoản `users.role` được cập nhật thành `VENDOR` — vẫn giữ toàn bộ quyền mua hàng (`ROLE_VENDOR` kế thừa `ROLE_CUSTOMER`), hồ sơ, giỏ hàng, danh sách yêu thích và lịch sử đơn của khách hàng không đổi.
- **Luồng sự kiện chính:**
  1. Người dùng bấm "Trở thành Người bán" trên thanh điều hướng storefront.
  2. Giao diện hiển thị Form đăng ký gian hàng:
     - Tên gian hàng (`shopName`, kiểm tra không trùng lặp và không chứa từ cấm).
     - Loại hình kinh doanh: Cá nhân (`INDIVIDUAL`), Hộ kinh doanh (`HOUSEHOLD`), Doanh nghiệp (`BUSINESS`).
     - Ngành hàng chính (`categoryId`).
     - Số điện thoại và Email liên hệ của gian hàng.
  3. Người dùng bấm "Đăng ký mở shop".
  4. Hệ thống mở transaction CSDL:
     - Tạo bản ghi mới trong bảng `vendor_profiles`: Gán `userId = currentUserId`, `shopName`, `businessType`, `categoryId`, `kycStatus = 'NOT_SUBMITTED'`, `commissionRate = NULL` (chưa có thỏa thuận riêng → áp tỷ lệ ngành hàng, không có thì 5% mặc định). Các trường kho lấy hàng `pickup*` và `ghnShopId` để **NULL** — chỉ có sau khi đăng ký GHN ở `UC-VND-05` (shop chưa được đăng bán khi `isPickupConfigured() = false`).
     - Cập nhật vai trò người dùng: `users.role = 'VENDOR'`.
  5. Commit transaction. Hệ thống cấp lại JWT Access Token mang `ROLE_VENDOR` (kế thừa `ROLE_CUSTOMER`) và chuyển hướng người dùng vào giao diện Cài đặt hồ sơ định danh KYC (`UC-VND-02`).
  6. Form mở shop: tên shop, họ tên chủ shop, SĐT, email, **ngành hàng**, **loại hình** (Cá nhân / Hộ kinh doanh / Doanh nghiệp), đồng ý **Điều khoản Người bán** và **Chính sách hoa hồng**; xác thực **OTP email** (`/vendor/send-otp`) trước khi tạo hồ sơ.
- **Luồng thay thế / Ngoại lệ:**
  - *2a. Tên shop đã tồn tại hoặc vi phạm thuần phong mỹ tục:* Hệ thống báo lỗi và gợi ý đổi tên gian hàng khác.
- **Luồng thay thế — Chuyển chế độ Mua / Bán:**
  1. Sau khi mở shop, menu tài khoản hiển thị nút **"Chuyển sang Kênh Người bán"** (khi đang ở storefront) và **"Chuyển sang chế độ Mua hàng"** (khi đang ở Kênh Người bán).
  2. Bấm nút: client đổi `activeMode` (`BUYER` / `SELLER`, lưu cục bộ) và chuyển giữa storefront `/` và Kênh Người bán `/seller`; **không đăng xuất, không cấp token mới** — cùng JWT `ROLE_VENDOR` dùng được cho API mua hàng (`hasRole('CUSTOMER')` qua kế thừa) và API shop.
  3. Ở chế độ Mua hàng, chủ shop dùng giỏ hàng và voucher như mọi khách (trả bằng VNPay / VietQR / COD); ở Kênh Người bán xem doanh thu trong ví shop (`VendorWallet`).
  4. Hộp chat và thông báo tách theo chế độ: tin nhắn với tư cách người mua ở storefront, tin nhắn khách gửi shop ở Kênh Người bán.
- **Quan hệ Use Case:** `<<include>>` Gửi mã OTP xác thực — tác nhân phụ: `ACT-EML` Email Service.
- **Ràng buộc nghiệp vụ & Kỹ thuật:** 1 tài khoản `users` chỉ được phép sở hữu tối đa 1 gian hàng (`userId` có ràng buộc UNIQUE trên bảng `vendor_profiles`). Chủ shop không được mua sản phẩm của chính shop mình: giỏ hàng và checkout từ chối với `GNE-ORD-4094` (UC-ORD-01, UC-ORD-07).
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 2 (tuần 3–4) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [VendorProfile](../ClassDiagram/ENTITY.md#14-vendorprofile--hồ-sơ-người-bán-shop), [User](../ClassDiagram/ENTITY.md#11-user--tài-khoản-đăng-nhập).

---

### UC-VND-02: Tải hồ sơ pháp lý & Giấy tờ định danh KYC
- **Mã Use Case:** `UC-VND-02`
- **Tác nhân chính:** `ACT-VND` Vendor (người bán/shop)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Người bán tải lên các giấy tờ pháp lý (Căn cước công dân gắn chip 2 mặt, Giấy phép kinh doanh, Ảnh chân dung selfie) để chứng minh tính hợp pháp của chủ cửa hàng theo Luật Thương mại điện tử 2025 và Nghị định 248/2026/NĐ-CP (thay cho NĐ 52/2013, 85/2021).
- **Tiền điều kiện:** Gian hàng có `kycStatus` là `NOT_SUBMITTED`, `REJECTED` hoặc `SUPPLEMENT_REQUESTED`.
- **Hậu điều kiện:** Các file ảnh được mã hóa AES-256-GCM lưu trữ trên Silo bucket `kyc-dossiers`; `vendor_profiles.kycStatus` chuyển `PENDING`; Backend gọi bất đồng bộ (`@Async`) sang AI Worker để thẩm định (`UC-VND-03`).
- **Luồng sự kiện chính:**
  1. Người bán truy cập Vendor Portal > Hồ sơ KYC.
  2. Người bán điền thông tin: Số CCCD, Họ tên, Mã số thuế (bắt buộc với Hộ KD và Doanh nghiệp) và tích ô đồng ý xử lý dữ liệu sinh trắc học phục vụ KYC (thời điểm đồng ý lưu trong `system_logs` `KYC_SUBMITTED`). Không đồng ý → bỏ so khớp khuôn mặt tự động; hồ sơ chuyển thẳng người duyệt đối chiếu ảnh.
  3. Người bán chụp / tải lên:
     - Ảnh mặt trước CCCD (`idFrontUrl`) — phải thấy rõ **mã QR** và **ảnh chân dung**.
     - Ảnh mặt sau CCCD (`idBackUrl`) — phải thấy rõ **3 dòng MRZ**.
     - Selfie có **thử thách liveness** (nháy mắt / quay đầu theo hướng dẫn); lưu khung hình đại diện (`selfieUrl`).
     - Ảnh Giấy chứng nhận đăng ký kinh doanh (`licenseUrl` - nếu là Hộ KD/Doanh nghiệp).
     - Client kiểm tra nhanh độ nét, lóa sáng và giải mã thử QR ngay trên trình duyệt để người bán chụp lại tại chỗ, giảm hồ sơ phải nộp lại.
  4. Client tải ảnh lên Silo S3 qua Presigned PUT URL:
     - Backend mã hóa nội dung nhị phân của ảnh bằng thuật toán **AES-256-GCM** với khóa bảo mật hệ thống `AES_ENCRYPTION_KEY` (Chống rò rỉ dữ liệu PII nếu CDN bị lộ).
     - Upload file mã hóa lên Silo S3 thư mục an toàn `genie-kyc-encrypted` dưới dạng raw resource.
  5. Cập nhật `vendor_profiles`: Lưu các URL ảnh mã hóa, cập nhật `kycStatus = 'PENDING'`, `kycSubmittedAt = now()`.
  6. Backend gọi `POST {AI_WORKER_URL}/kyc/verify` bất đồng bộ (`@Async`, timeout 30 giây) kèm `vendorProfileId` và Presigned URL các ảnh; `kycStatus` → `AI_PROCESSING`.
  7. Hiển thị thông báo: "Hồ sơ của bạn đã được tiếp nhận và đang được thẩm định tự động".
- **Luồng thay thế / Ngoại lệ:**
  - *File tải lên sai định dạng hoặc mờ, dung lượng > 10MB:* Hệ thống từ chối tải và yêu cầu chụp lại ảnh rõ nét.
- **Quan hệ Use Case:** `<<include>>` Tải tệp lên Silo (Presigned URL) — tác nhân phụ: `ACT-MIN` Silo S3 Storage; `<<include>>` Thẩm định KYC tự động qua AI (QR, MRZ & Face Match) (`UC-VND-03`).
- **Ràng buộc nghiệp vụ & Kỹ thuật:**
  - Dữ liệu giấy tờ tùy thân là thông tin cá nhân đặc biệt nhạy cảm, bắt buộc mã hóa AES-256-GCM ở tầng ứng dụng trước khi đẩy sang dịch vụ lưu trữ đám mây.
  - **Cổng KYC:** khi chưa `VERIFIED`, Seller Center khóa mọi mục trừ Hồ sơ KYC và Cài đặt (thông báo "Hoàn tất xác minh KYC để sử dụng tính năng này").
  - Ảnh chân dung chụp trực tiếp từ camera hoặc tải lên; tệp được mã hóa trước khi lưu ("Đã tải lên (mã hóa)").
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 2 (tuần 3–4) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [VendorProfile](../ClassDiagram/ENTITY.md#14-vendorprofile--hồ-sơ-người-bán-shop) (`idFrontUrl`, `idBackUrl`, `selfieUrl`, `licenseUrl`, `kycStatus`), [AdminNotification](../ClassDiagram/ENTITY.md#105-adminnotification--thông-báo-nội-bộ-cho-quản-trị-viên).

---

### UC-VND-03: Thẩm định KYC tự động qua AI (QR, MRZ & Face Match)
- **Mã Use Case:** `UC-VND-03`
- **Tác nhân chính:** `ACT-AIW` AI Worker Service (KYC & PhoBERT)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Thẩm định hồ sơ định danh người bán bằng pipeline **theo tầng**: tầng rẻ chạy trước và dừng sớm, tầng nặng chỉ chạy khi cần; mỗi tín hiệu cộng vào điểm rủi ro thay vì chặn cứng, để nhân viên chỉ phải xem số ít hồ sơ.
- **Tiền điều kiện:** AI Worker nhận request `POST /kyc/verify` từ Backend (`UC-VND-02`).
- **Hậu điều kiện:** Tạo bản ghi trong `kyc_results` (cờ kiểm tra, `riskScore`, `riskSignalsJson`, `sampledForReview`); `vendor_profiles.kycStatus` chuyển theo kết luận; embedding khuôn mặt lưu trong CSDL riêng của AI Worker (SQLite / file `.npy`) để kiểm tra trùng.
- **Luồng sự kiện chính:**
  1. AI Worker (Python, `onnxruntime` + OpenCV, không dùng PyTorch/TensorFlow) nhận message, gọi `startAiProcessing()` (`PENDING` → `AI_PROCESSING`), tải ảnh mã hóa từ Silo bucket `kyc-dossiers` và giải mã.
  2. **Tầng 0 — dữ liệu (vài chục ms):**
     - Mặt trước: phát hiện và nắn phẳng thẻ, **giải mã QR** (`qrCodeDetected`) → số CCCD 12 số, số CMND cũ, họ tên có dấu, ngày sinh, giới tính, nơi thường trú, ngày cấp; cắt **ảnh chân dung** trên thẻ cho Tầng 1.
     - Mặt sau: cắt vùng **MRZ** (3 dòng × 30 ký tự, chuẩn ICAO 9303 TD1), nhận dạng ký tự `[A-Z0-9<]`, kiểm tra toàn bộ check digit trọng số 7-3-1 (`mrzValid`) → số giấy tờ, ngày sinh, giới tính, ngày hết hạn, quốc tịch, họ tên không dấu.
     - Đối chiếu chéo: QR khớp MRZ về số thẻ, ngày sinh, giới tính, họ tên đã bỏ dấu (`mrzQrMatch`); số CCCD đúng cấu trúc — 3 số mã tỉnh, 1 số thế kỷ + giới tính, 2 số năm sinh — và khớp ngày sinh, giới tính (`cccdNumberValid`); ngày hết hạn khớp quy tắc tuổi đổi thẻ 14 / 25 / 40 / 60 (`expiryRuleValid`); số CCCD chưa gắn shop khác đang hoạt động (`duplicateIdFound`); thông tin shop tự khai ở UC-VND-02 khớp QR.
     - **Không đọc được** QR/MRZ dù client đã cho chụp lại (ảnh mờ, lóa) → tín hiệu `UNREADABLE_IMAGE` (+30, chỉ để chuyển người duyệt, không coi là gian lận); tại UC-VND-04 nhân viên `requestSupplement()` yêu cầu chụp lại. Thẻ không có QR/MRZ (CMND 9 số, CCCD mã vạch) → tín hiệu `LEGACY_CARD`.
  3. **Tầng 1 — sinh trắc (~1 giây):** YuNet phát hiện khuôn mặt + SFace sinh embedding cho ảnh chân dung trên thẻ và khung hình selfie → cosine `faceMatchScore`, `faceMatchResult` (≥ 0,85); `livenessScore` từ thử thách chủ động phía client (nháy mắt / quay đầu) và mô hình chống giả thụ động; so cosine với embedding đã lưu để tìm khuôn mặt trùng gắn với CCCD khác (`faceDuplicateFound`; vài nghìn hồ sơ nên quét tuần tự là đủ). Tầng 1 vẫn chạy khi Tầng 0 đã có tín hiệu, để người duyệt có đủ dữ liệu.
  4. **Chấm điểm** theo bảng tín hiệu tại ENTITY.md mục 1.6 (vd. `QR_MRZ_MISMATCH` +60, `FACE_DUPLICATE` +70, `FACE_SCORE_BORDERLINE` +25, `LIVENESS_SUSPECT` +40) rồi gọi `KycResult.applyRiskScore(score, signals, sampled, 30, 70, now)`; `sampled` = ngẫu nhiên 5% (`genie.kyc.risk.sample-rate`).
  5. Kết luận (`overallStatus`):
     - **PASS** (`riskScore` < 30, không bị lấy mẫu) ➔ `approveKyc(…, OCR_AUTO)`, shop được đăng bán.
     - **PARTIAL** (30 ≤ `riskScore` < 70, hoặc bị lấy mẫu) ➔ `routeToManualReview()`, chuyển `UC-VND-04` kèm danh sách tín hiệu.
     - **FAIL** (`riskScore` ≥ 70) ➔ `rejectKyc(reason)` với lý do theo tín hiệu, cho phép nộp lại.
  6. AI Worker gọi callback `PATCH /api/v1/internal/vendor/{id}/kyc-result` để Spring Boot lưu `kyc_results` và thực thi chuyển trạng thái ở bước 5.
  7. Màn duyệt hiển thị **Thông tin trích xuất** (số CCCD, họ tên, ngày sinh, giới tính, quốc tịch, nơi thường trú, ngày cấp, nơi cấp) và **bảng kiểm tra**: Số CCCD hợp lệ, Mã QR, Quốc huy, MRZ hợp lệ, Vân tay, So sánh khuôn mặt — hai mục *quốc huy* và *vân tay* là tín hiệu trong `riskSignalsJson` (không có cột riêng); mức rủi ro thấp / trung bình / cao theo `riskScore`.
- **Quan hệ Use Case:** được bao gồm bởi `UC-VND-02`; được mở rộng bởi `UC-VND-04` (điểm «Kết luận thẩm định»).
- **Ràng buộc nghiệp vụ & Kỹ thuật:**
  - **Tầng 2 không chặn đăng bán mà chặn rút tiền:** tài khoản nhận tiền phải được xác minh chính chủ (`UC-VND-07`, `VendorProfile.isPayoutAccountVerified()`) trước lần rút đầu tiên (`UC-ADM-04`); với hộ KD / doanh nghiệp, MST được đối chiếu ở bước này (đồ án dùng API giả lập).
  - Ngưỡng điểm, bảng điểm và tỷ lệ lấy mẫu nằm trong cấu hình, không hard-code.
  - **Không để quyết định bất lợi hoàn toàn tự động:** hồ sơ bị `FAIL` hiển thị lý do theo tín hiệu và nút "Yêu cầu nhân viên xem xét" (1 lần/hồ sơ); hồ sơ nộp lại kèm yêu cầu này vẫn được AI chấm điểm nhưng Spring Boot luôn gọi `routeToManualReview()` thay vì `rejectKyc()`.
  - **Giới hạn:** thẻ giả in QR/MRZ nhất quán với chính khuôn mặt kẻ gian vẫn có thể qua Tầng 0–1; chỉ đọc chữ ký số chip NFC hoặc xác thực C06 / VNeID chặn được triệt để (ngoài phạm vi đồ án). Rủi ro còn lại được khống chế bằng Tầng 2.
  - Embedding khuôn mặt là dữ liệu sinh trắc học: chỉ lưu vector (không lưu ảnh), xóa cùng hồ sơ KYC theo thời hạn lưu trữ, chỉ chạy khi người bán đã tích đồng ý ở `UC-VND-02`.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai — xử lý phía máy chủ (cron / webhook / AI), chưa có backend; giao diện chỉ hiển thị kết quả trên lớp mock.
  - *Kế hoạch:* Sprint 2 (tuần 3–4) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [KycResult](../ClassDiagram/ENTITY.md#16-kycresult--kết-quả-ocrai-thẩm-định-giấy-tờ), [VendorProfile](../ClassDiagram/ENTITY.md#14-vendorprofile--hồ-sơ-người-bán-shop).

---

### UC-VND-04: Kiểm duyệt thủ công hồ sơ KYC (Staff / Admin)
- **Mã Use Case:** `UC-VND-04`
- **Tác nhân chính:** `ACT-MOD` Moderator (Nhân viên kiểm duyệt); `ACT-ADM` Admin
- **Tác nhân phụ:** `ACT-EML` Email Service
- **Mục đích nghiệp vụ:** Cho phép chuyên viên thẩm định rà soát các hồ sơ bị AI đánh giá `PARTIAL` hoặc xác nhận phê duyệt cấp phép hoạt động chính thức cho gian hàng.
- **Tiền điều kiện:** Đăng nhập vai trò `STAFF` / `ADMIN` (MVP không chia quyền chi tiết).
- **Hậu điều kiện:** `vendor_profiles.kycStatus` chuyển sang `VERIFIED`, `REJECTED` hoặc `SUPPLEMENT_REQUESTED`; lưu `kycVerifiedBy`, `kycVerifiedAt`.
- **Luồng sự kiện chính:**
  1. Nhân viên thẩm định vào Admin Portal > Quản lý KYC người bán.
  2. Mở chi tiết 1 hồ sơ đang chờ (sắp theo `riskScore` giảm dần; hồ sơ `sampledForReview` có nhãn "Hậu kiểm ngẫu nhiên"): Hệ thống hiển thị song song ảnh giấy tờ gốc (được backend giải mã tạm thời), dữ liệu QR/MRZ trong `kyc_results`, điểm khớp mặt, điểm liveness và danh sách tín hiệu rủi ro (`riskSignalsJson`) đã cộng điểm.
  3. Nhân viên đối chiếu và chọn 1 trong 3 quyết định:
     - **Phê duyệt (Verify):** `kycStatus = 'VERIFIED'`. Cửa hàng được kích hoạt toàn bộ tính năng đăng bán sản phẩm và tham gia chiến dịch.
     - **Từ chối (Reject):** `kycStatus = 'REJECTED'`, chọn lý do từ danh mục hoặc nhập lý do chi tiết.
     - **Yêu cầu bổ sung (Request Supplement):** `kycStatus = 'SUPPLEMENT_REQUESTED'` (ví dụ: "Ảnh mặt sau bị chói lóa, yêu cầu chụp lại ảnh mặt sau").
  4. Hệ thống cập nhật bảng `vendor_profiles`, ghi nhận `kycVerifiedBy = currentStaffId`, ghi nhật ký kiểm toán vào `system_logs`.
  5. Tự động gửi email thông báo kết quả thẩm định cho Người bán.
  6. **Trình xem tài liệu:** phóng to / thu nhỏ / xoay / lật ảnh giấy tờ.
  7. **Yêu cầu bổ sung:** chọn giấy tờ cần bổ sung + ghi chú → `SUPPLEMENT_REQUESTED`; người bán có **7 ngày** để nộp lại, quá hạn chuyển `REJECTED`. Có thể "Đưa lại vào hàng chờ" hồ sơ đã xử lý nhầm.
  8. Khi duyệt doanh nghiệp / thương hiệu chính hãng: có thể gắn nhãn **Chính hãng** (`VendorProfile.isMall`) và đặt hoa hồng riêng của shop (`commissionRate`, cần quyền `UPDATE_COMMISSION`).
  9. Cam kết xử lý trong **24 giờ làm việc** kể từ khi nộp (thống nhất với thông báo ở màn KYC của người bán).
- **Quan hệ Use Case:** `<<extend>>` Thẩm định KYC tự động qua AI (QR, MRZ & Face Match) (`UC-VND-03`) tại điểm mở rộng «Kết luận thẩm định» khi {30 ≤ riskScore < 70 hoặc hồ sơ bị lấy mẫu 5%}.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 2 (tuần 3–4) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [VendorProfile](../ClassDiagram/ENTITY.md#14-vendorprofile--hồ-sơ-người-bán-shop), [KycResult](../ClassDiagram/ENTITY.md#16-kycresult--kết-quả-ocrai-thẩm-định-giấy-tờ).

---

### UC-VND-05: Cấu hình kho lấy hàng & Đồng bộ GHN Shop ID
- **Mã Use Case:** `UC-VND-05`
- **Tác nhân chính:** `ACT-VND` Vendor (người bán/shop)
- **Tác nhân phụ:** `ACT-3PL` Đối tác vận chuyển (GHN)
- **Mục đích nghiệp vụ:** Thiết lập địa chỉ kho xuất hàng (MVP: mỗi shop **một** kho lấy hàng) để bưu tá GHN tới lấy kiện; tự động đăng ký cửa hàng con trên Giao Hàng Nhanh (`ghn_shop_id`).
- **Tiền điều kiện:** Người bán đã đăng nhập; `kycStatus == 'VERIFIED'`.
- **Hậu điều kiện:** Cập nhật các trường `pickup*` và `ghnShopId` trong `vendor_profiles`.
- **Luồng sự kiện chính:**
  1. Người bán vào Cài đặt gian hàng > Địa chỉ lấy hàng.
  2. Nhập Địa chỉ chi tiết (Số nhà, ngõ ngách, tên đường).
  3. Chọn Tỉnh/Thành phố, Quận/Huyện, Phường/Xã từ danh mục chuẩn hóa của GHN.
  4. Nhập Tên người liên hệ kho và Số điện thoại hotline kho.
  5. Người bán bấm "Lưu và Đồng bộ với GHN".
  6. Backend Spring Boot gửi request sang API GHN `https://dev-online-gateway.ghn.vn/.../shop/register` với thông tin kho hàng.
  7. GHN phản hồi thành công và trả về mã `shop_id` của GHN.
  8. Backend lưu thông tin vào `vendor_profiles` : `pickupAddressLine`, `pickupProvinceId` + `pickupProvince`, `pickupDistrictId` + `pickupDistrict`, `pickupWardCode` + `pickupWard` (tên dùng để hiển thị, không cần gọi lại API GHN), `ghnShopId`. Từ đây `isPickupConfigured() = true`.
  9. Giao diện: khối "Địa chỉ lấy hàng (GHN)" trong Cài đặt Shop (Tỉnh / Quận / Phường theo mã GHN, địa chỉ chi tiết).
- **Quan hệ Use Case:** Không có.
- **Ràng buộc nghiệp vụ & Kỹ thuật:** Địa chỉ kho lưu trực tiếp ở `vendor_profiles.pickup*` (bản đầy đủ tách bảng `vendor_warehouses` cho đa kho); đổi kho phải gọi lại API GHN cập nhật cửa hàng.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 2 (tuần 3–4) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [VendorProfile](../ClassDiagram/ENTITY.md#14-vendorprofile--hồ-sơ-người-bán-shop) (`pickupDistrictId`, `pickupWardCode`, tên tỉnh/huyện/xã, `ghnShopId` — NULLABLE tới bước này).

---

### UC-VND-06: Cấu hình tin nhắn trả lời tự động (Auto-reply)
- **Mã Use Case:** `UC-VND-06`
- **Tác nhân chính:** `ACT-VND` Vendor (người bán/shop)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Duy trì tỷ lệ phản hồi chat bằng lời chào khi khách mở chat và tin nhắn khi shop vắng mặt (mục "Tự động trả lời chat" trong Cài đặt Shop).
- **Tiền điều kiện:** Shop đang hoạt động.
- **Hậu điều kiện:** `vendor_profiles.autoReplyEnabled`, `autoReplyGreeting`, `autoReplyOffline` được cập nhật.
- **Luồng sự kiện chính:**
  1. Shop vào Cài đặt → Tuỳ chọn vận hành → bật "Tự động trả lời chat".
  2. Nhập (1) Lời chào khi khách mở khung chat; (2) Tin nhắn trả lời khi shop vắng mặt (ngoài 08:00–22:00 hoặc không có nhân viên trực tuyến).
  3. Bấm "Lưu cài đặt" → `PUT /api/v1/vendor/auto-reply`.
  4. Khi khách mở hội thoại: hệ thống gửi lời chào (tin nhắn hệ thống đứng tên shop, không tính là phản hồi của shop khi đo tỉ lệ phản hồi).
  5. Khi shop tạm nghỉ (UC-VND-19): tin nhắn tự động dùng `vacationMessage`.
- **Luồng thay thế / Ngoại lệ:**
  - *Để trống cả hai nội dung mà bật công tắc:* báo lỗi yêu cầu nhập ít nhất một nội dung.
- **Quan hệ Use Case:** Không có.
- **Ràng buộc nghiệp vụ & Kỹ thuật:**
  - Câu trả lời theo từ khóa (FAQ rules) thuộc bản DATN.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 7 (tuần 13–14) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [VendorProfile](../ClassDiagram/ENTITY.md#14-vendorprofile--hồ-sơ-người-bán-shop), [Message](../ClassDiagram/ENTITY.md#112-message--tin-nhắn).

---

### UC-VND-07: Đăng ký tài khoản nhận giải ngân (Payout Bank Account)
- **Mã Use Case:** `UC-VND-07`
- **Tác nhân chính:** `ACT-VND` Vendor (người bán/shop)
- **Tác nhân phụ:** `ACT-EML` Email Service
- **Mục đích nghiệp vụ:** Đăng ký thông tin tài khoản ngân hàng chính chủ để nhận tiền thanh toán khi thực hiện lệnh rút tiền từ Ví người bán.
- **Tiền điều kiện:** Người bán đã đăng nhập; `kycStatus == 'VERIFIED'`.
- **Hậu điều kiện:** Cập nhật `payoutBankName`, `payoutBankAccountNumber`, `payoutBankAccountName` trong `vendor_profiles`.
- **Luồng sự kiện chính:**
  1. Người bán vào Tài chính > Tài khoản thụ hưởng.
  2. Chọn Tên Ngân hàng từ danh mục ngân hàng Việt Nam (ví dụ: Vietcombank, Techcombank, MB Bank).
  3. Nhập Số tài khoản ngân hàng và Tên chủ tài khoản.
  4. **Xác minh chính chủ:** hệ thống tra cứu tên chủ tài khoản qua API giả lập (MVP; thực tế dùng dịch vụ tra cứu tên tài khoản của ngân hàng / đối tác chi hộ) và gọi `VendorProfile.verifyPayoutAccount(tênNgânHàngTrảVề, extractedFullName, now)`: tên trả về phải khớp họ tên trên CCCD sau khi bỏ dấu, viết hoa. Không khớp → từ chối lưu tài khoản; khớp → ghi `payoutAccountVerifiedAt`. Đổi tài khoản đặt lại `payoutAccountVerifiedAt = NULL`.
  5. Nhập mã OTP gửi qua email để xác thực thay đổi tài khoản ngân hàng.
  6. Hệ thống lưu trữ thông tin thụ hưởng vào `vendor_profiles`.
- **Luồng thay thế / Ngoại lệ:**
  - *Xóa / đổi tài khoản nhận tiền:* chỉ khi không có lệnh rút `PENDING`; xác thực OTP email; tài khoản mới phải xác minh lại chính chủ (tên chủ tài khoản trùng họ tên trên CCCD).
- **Quan hệ Use Case:** Không có.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 4 (tuần 7–8) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [VendorProfile](../ClassDiagram/ENTITY.md#14-vendorprofile--hồ-sơ-người-bán-shop) (`payoutBankName`, `payoutBankAccountNumber`, `payoutBankAccountName`, `payoutAccountVerifiedAt`).

---

### UC-VND-08: Xem báo cáo doanh thu & Phân tích bán hàng (Vendor Analytics)
- **Mã Use Case:** `UC-VND-08`
- **Tác nhân chính:** `ACT-VND` Vendor (người bán/shop)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Cung cấp bức tranh toàn cảnh về hiệu quả kinh doanh: Doanh thu thực tế, Số lượng đơn hàng, Tỷ lệ chuyển đổi, Sản phẩm bán chạy nhất theo từng khoảng thời gian.
- **Tiền điều kiện:** Người bán đã đăng nhập.
- **Hậu điều kiện:** Hiển thị biểu đồ Recharts và các chỉ số KPI doanh số.
- **Luồng sự kiện chính:**
  1. Người bán truy cập Vendor Portal > Tổng quan kinh doanh.
  2. Chọn mốc thời gian: Hôm nay, 7 ngày qua, 30 ngày qua, hoặc Tùy chọn ngày.
  3. Client gọi `GET /api/v1/vendor/analytics/overview`.
  4. Backend thực hiện các câu truy vấn tổng hợp (Aggregation queries):
     - Tổng doanh thu gộp (Gross Merchandise Value - GMV).
     - Doanh thu thuần sau trừ hoa hồng và chiết khấu.
     - Số đơn hàng mới, số đơn đã hoàn tất, số đơn bị hủy/trả hàng.
     - Top 5 SKU bán chạy nhất kèm tỷ lệ đóng góp doanh thu.
  5. Trả dữ liệu JSON; giao diện render biểu đồ cột/đường trực quan.
  6. Chỉ số lưu lượng: **Lượt truy cập**, **Tỉ lệ chuyển đổi** (đơn đã thanh toán / khách truy cập), **Nguồn truy cập** (Tìm kiếm trong sàn / Trang chủ – Gợi ý / Khuyến mãi – Flash Sale / Link ngoài – Chia sẻ) tổng hợp từ `product_view_stats`.
  7. Khối **Kết quả đơn** (30 ngày): hoàn tất / khách hủy / shop từ chối / trả hàng – hoàn tiền; **Tiền thực về shop** = doanh thu gộp − hoa hồng − voucher shop (voucher sàn và Điểm do sàn chịu, không trừ vào shop); chỉ số vận hành: tỉ lệ shop tự hủy hoặc từ chối đơn, số **khiếu nại shop thua** (UC-SUP-03) và số **đơn quá 24 giờ sàn tự xác nhận** (UC-LOG-16) trong 30 ngày.
  8. Khối **Cảm xúc đánh giá** (`UC-AI-07`): tỉ lệ Tích cực / Trung tính / Tiêu cực theo kỳ, xu hướng theo tuần, 5 sản phẩm bị đánh giá tiêu cực nhiều nhất (chỉ kết quả có độ tin cậy ≥ 0,6).
  9. Trang **Tổng quan** của Seller Center: doanh thu tháng, số dư khả dụng, chờ đối soát (escrow), đơn chờ xác nhận / chờ đóng gói / đang giao, hoàn trả / hủy; biểu đồ 7 ngày / 30 ngày / cả năm; đơn gần đây; lối tắt Thêm sản phẩm, Xử lý đơn, Quản lý ví, Chat.
- **Quan hệ Use Case:** Không có.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 6 (tuần 11–12) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [Order](../ClassDiagram/ENTITY.md#52-order--đơn-hàng-theo-từng-shop), [OrderItem](../ClassDiagram/ENTITY.md#53-orderitem--dòng-sản-phẩm-trong-đơn), [EscrowLedger](../ClassDiagram/ENTITY.md#63-escrowledger--sổ-ký-quỹ-tiền-tạm-giữ), [ProductViewStat](../ClassDiagram/ENTITY.md#212-productviewstat--lượt-xem-sản-phẩm-theo-ngày--nguồn-truy-cập).

---

### UC-VND-19: Bật chế độ tạm nghỉ bán hàng của shop (Vacation Mode)
- **Mã Use Case:** `UC-VND-19`
- **Tác nhân chính:** `ACT-VND` Vendor (người bán/shop)
- **Tác nhân phụ:** `ACT-CRN` Platform Cron / Scheduler
- **Mục đích nghiệp vụ:** Cho shop tạm ngừng nhận đơn khi nghỉ lễ / hết hàng diện rộng mà không phải ẩn từng sản phẩm ("Chế độ nghỉ (Vacation)": sản phẩm vẫn hiển thị nhưng khách không mua được).
- **Tiền điều kiện:** Shop không còn đơn `CONFIRMED` / `PREPARING` chưa bàn giao vận chuyển.
- **Hậu điều kiện:** `vacationStartsAt`, `vacationEndsAt`, `vacationMessage` được ghi; hết hạn tự mở bán lại.
- **Luồng sự kiện chính:**
  1. Shop bật Chế độ nghỉ, chọn ngày mở lại (tối đa 30 ngày) và lời nhắn.
  2. Hệ thống kiểm tra đơn chưa bàn giao; hợp lệ → `VendorProfile.startVacation(...)`.
  3. Trong thời gian nghỉ: sản phẩm vẫn hiển thị kèm nhãn "Shop đang tạm nghỉ — mở lại ngày …", nút mua bị khóa; checkout chứa sản phẩm của shop bị từ chối (`GNE-ORD-4093`); voucher của shop tạm ẩn; chat tự động trả `vacationMessage`.
  4. Tới `vacationEndsAt`, Cron gọi `endVacation()`; shop cũng có thể mở lại sớm.
- **Luồng thay thế / Ngoại lệ:**
  - *Còn đơn chưa bàn giao:* từ chối, hiển thị danh sách đơn cần xử lý trước.
- **Quan hệ Use Case:** Không có.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 7 (tuần 13–14) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [VendorProfile](../ClassDiagram/ENTITY.md#14-vendorprofile--hồ-sơ-người-bán-shop).

---

### UC-VND-20: Xem ví & sổ giao dịch người bán (Seller Wallet & Escrow Ledger)
- **Mã Use Case:** `UC-VND-20`
- **Tác nhân chính:** `ACT-VND` Vendor (người bán/shop)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Cho shop xem minh bạch dòng tiền (mục "Ví & Escrow"): số dư khả dụng, tiền đang giữ ký quỹ, doanh thu tháng, cách tính tiền thực nhận, sổ giao dịch ví — trước khi rút tiền (UC-ADM-04).
- **Tiền điều kiện:** Shop `VERIFIED`.
- **Hậu điều kiện:** Hiển thị số liệu ví; xuất CSV khi được yêu cầu.
- **Luồng sự kiện chính:**
  1. Shop mở Ví & Escrow → `GET /api/v1/vendor/wallet/overview?month=&year=`.
  2. Hệ thống trả: số dư khả dụng (`VendorWallet.balance`), đang giữ (Σ `EscrowLedger.netAmount` ở `HELD` / `DISPUTED`), chờ đối soát COD (`PENDING_FUNDING`), đang rút (`pendingWithdrawal`), doanh thu tháng và % so với tháng trước.
  3. Khối "Cách tính tiền về ví" lấy **tỷ lệ hoa hồng thực tế theo ngành** của đơn gần nhất (Gross − hoa hồng = Net).
  4. Sổ giao dịch ví (`GET /wallet/transactions`): thời gian, mã tham chiếu (đơn / lệnh rút / hoàn tiền), loại (Bán hàng → vào escrow, Giải ngân, Rút tiền, Hoàn tiền), Gross, Hoa hồng, Net, trạng thái (Escrow / Khả dụng / Đã rút / Hoàn) — gộp `EscrowLedger` và `WalletTransaction`, lọc theo tháng / loại.
  5. Bấm "Xuất CSV" tải sao kê của kỳ đang lọc; bấm "Yêu cầu rút tiền" chuyển sang UC-ADM-04.
- **Luồng thay thế / Ngoại lệ:**
  - *Chưa có giao dịch trong tháng:* hiển thị "Chưa có giao dịch nào trong tháng này".
- **Quan hệ Use Case:** Không có.
- **Ràng buộc nghiệp vụ & Kỹ thuật:**
  - Chỉ đọc; mọi thay đổi số dư diễn ra ở PAY-07, ADM-04/05, SUP-04 và được ghi sổ cái (UC-PAY-06).
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 7 (tuần 13–14) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [VendorWallet](../ClassDiagram/ENTITY.md#64-vendorwallet--ví-tiền-của-người-bán), [WalletTransaction](../ClassDiagram/ENTITY.md#65-wallettransaction--lịch-sử-biến-động-ví), [EscrowLedger](../ClassDiagram/ENTITY.md#63-escrowledger--sổ-ký-quỹ-tiền-tạm-giữ), [WithdrawalRequest](../ClassDiagram/ENTITY.md#66-withdrawalrequest--yêu-cầu-rút-tiền).

---

### UC-VND-21: Cập nhật thông tin gian hàng & cài đặt vận hành
- **Mã Use Case:** `UC-VND-21`
- **Tác nhân chính:** `ACT-VND` Vendor (người bán/shop)
- **Tác nhân phụ:** `ACT-MIN` Silo S3 Storage
- **Mục đích nghiệp vụ:** Cho shop tự chỉnh thông tin hiển thị trên trang shop (ảnh bìa, logo, tên, mô tả) và cài đặt vận hành (hãng vận chuyển, ngưỡng freeship của shop, cho phép đặt trước) — mục "Cài đặt Shop".
- **Tiền điều kiện:** Người bán đã đăng nhập (được vào Cài đặt kể cả khi KYC chưa duyệt).
- **Hậu điều kiện:** `vendor_profiles` (`shopName`, `logoUrl`, `coverUrl`, `shopDescription`, `preOrderEnabled`) và `vendor_shipping_config` được cập nhật.
- **Luồng sự kiện chính:**
  1. Shop tải ảnh bìa (khuyến nghị 1200×300, ≤ 2 MB) và logo qua Silo Presigned URL.
  2. Sửa tên shop (kiểm tra trùng, tối đa 1 lần / 30 ngày) và mô tả.
  3. Mục Đơn vị vận chuyển: bật / tắt GHN (mặc định), GHTK, ViettelPost, J&T Express, Genie Express nội thành → `vendor_shipping_config`; đặt "Miễn phí vận chuyển cho đơn từ …" (`freeshipThreshold`, shop tự chịu).
  4. Tuỳ chọn vận hành: "Cho phép đặt trước (Pre-order)" (`preOrderEnabled`), "Tự động trả lời chat" (UC-VND-06), "Chế độ nghỉ" (UC-VND-19).
  5. Bấm "Lưu cài đặt"; trang shop (UC-CAT-16) phản ánh ngay.
- **Luồng thay thế / Ngoại lệ:**
  - *Tắt hết hãng vận chuyển:* từ chối — phải còn ít nhất 1 hãng.
  - *Ảnh vượt dung lượng / sai tỉ lệ:* báo lỗi.
- **Quan hệ Use Case:** `<<include>>` Tải tệp lên Silo (Presigned URL) — tác nhân phụ: `ACT-MIN` Silo S3 Storage.
- **Ràng buộc nghiệp vụ & Kỹ thuật:**
  - Ngưỡng freeship của shop khác mã Freeship của sàn: shop chịu (ghi `EscrowLedger.shopShippingSubsidy`), không trừ vào voucher sàn.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 7 (tuần 13–14) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [VendorProfile](../ClassDiagram/ENTITY.md#14-vendorprofile--hồ-sơ-người-bán-shop), [VendorShippingConfig](../ClassDiagram/ENTITY.md#73-vendorshippingconfig--cấu-hình-vận-chuyển-của-shop).

---

<a id="ph-log"></a>
## PHÂN HỆ 06: VẬN CHUYỂN QUA ĐỐI TÁC 3PL (SHIPPING VIA 3PL)

> **Phạm vi:** Shop xác nhận đơn, đóng gói, điều phối đa hãng vận chuyển (GHN / GHTK / ViettelPost / J&T), tạo vận đơn & in phiếu, đồng bộ hành trình qua webhook.
> **Use case (5):** `UC-LOG-01`, `UC-LOG-02`, `UC-LOG-03`, `UC-LOG-15`, `UC-LOG-16`

### UC-LOG-01: Tiếp nhận đơn hàng & Đóng gói tại kho shop (Fulfillment)
- **Mã Use Case:** `UC-LOG-01`
- **Tác nhân chính:** `ACT-VND` Vendor (người bán/shop)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Người bán tiếp nhận đơn hàng đã thanh toán thành công, kiểm tra tồn kho vật lý và tiến hành đóng gói kiện hàng theo đúng quy chuẩn bảo vệ hàng hóa.
- **Tiền điều kiện:** Đơn hàng con đã được shop xác nhận ở UC-LOG-16 (`orders.status == 'CONFIRMED'`; Online đi từ `PAID` → `CONFIRMED`, COD đi từ `PENDING_PAYMENT` → `CONFIRMED`).
- **Hậu điều kiện:** `orders.status` chuyển thành `PREPARING`; kích hoạt thời hạn chuẩn bị hàng SLA (tối đa 24h).
- **Luồng sự kiện chính:**
  1. Người bán nhận thông báo có đơn hàng mới trên Vendor Portal (`Notification.type = NEW_ORDER_FOR_VENDOR`; người mua nhận `PAYMENT_SUCCESS` cho cùng sự kiện `order.paid`).
  2. Vào mục Quản lý đơn hàng > Chờ xử lý.
  3. Bấm xem chi tiết đơn: Danh sách sản phẩm, số lượng, phân loại SKU, địa chỉ khách nhận.
  4. Người bán bấm "Chuẩn bị hàng".
  5. Hệ thống chuyển trạng thái đơn hàng: `orders.status = 'PREPARING'`.
  6. Người bán đóng gói sản phẩm và dán nhãn thông tin đơn hàng.
  7. Danh sách đơn có tab Tất cả / Mới / Chờ chuẩn bị / Đang giao / Đã giao / Trả hàng, tìm theo mã đơn hoặc tên khách, sắp xếp Mới nhất / Cũ nhất; chi tiết đơn hiển thị **lời nhắn của khách** (`buyerNote`).
- **Quan hệ Use Case:** Không có.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 4 (tuần 7–8) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [Order](../ClassDiagram/ENTITY.md#52-order--đơn-hàng-theo-từng-shop), [OrderItem](../ClassDiagram/ENTITY.md#53-orderitem--dòng-sản-phẩm-trong-đơn).

---

### UC-LOG-02: Đẩy vận đơn sang GHN & In phiếu gửi hàng PDF (Waybill Label)
- **Mã Use Case:** `UC-LOG-02`
- **Tác nhân chính:** `ACT-VND` Vendor (người bán/shop)
- **Tác nhân phụ:** `ACT-3PL` Đối tác vận chuyển (GHN)
- **Mục đích nghiệp vụ:** Gọi API đối tác vận chuyển để sinh mã vận đơn chính thức, đặt lịch hẹn bưu tá GHN tới kho lấy hàng và in phiếu giao hàng chuẩn khổ A6 có mã barcode.
- **Tiền điều kiện:** Đơn hàng đang ở trạng thái `PREPARING`.
- **Hậu điều kiện:** Nhận mã vận đơn từ GHN; lưu vào `orders.ghnOrderCode`; sinh file PDF phiếu gửi hàng.
- **Luồng sự kiện chính:**
  1. Sau khi đóng gói xong, người bán bấm nút "Sẵn sàng giao cho ĐVVC".
  2. Backend Spring Boot gọi API tạo đơn của GHN `https://dev-online-gateway.ghn.vn/.../shipping-order/create`:
     - Truyền `shop_id`, `to_name`, `to_phone`, `to_address`, `to_ward_code`, `to_district_id`, `weight`, `items`.
  3. GHN xử lý và trả về mã vận đơn (Tracking Code, ví dụ: `GHN-ABC123XYZ`) và thời gian dự kiến bưu tá đến lấy hàng.
  4. Backend lưu `orders.ghnOrderCode = trackingCode`.
  5. Người bán bấm nút "In phiếu gửi hàng": Hệ thống sinh file nhãn PDF khổ chuẩn 100x150mm (A6) chứa thông tin người gửi, người nhận, danh sách hàng ẩn giá, và mã vạch Barcode Code128 để dán lên kiện hàng.
  6. Hãng vận chuyển do UC-LOG-15 chọn trong các hãng shop đã bật; có thể "In lại vận đơn" bất kỳ lúc nào trước khi hãng lấy hàng.
- **Luồng thay thế / Ngoại lệ:**
  - *2a. GHN API gặp sự cố timeout:* Hệ thống giữ nguyên trạng thái `PREPARING`, báo lỗi cho người bán thử lại sau vài phút mà không làm mất thông tin đóng gói.
- **Quan hệ Use Case:** `<<include>>` Điều phối đa hãng vận chuyển (Dynamic Carrier Routing) (`UC-LOG-15`).
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 4 (tuần 7–8) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [Order](../ClassDiagram/ENTITY.md#52-order--đơn-hàng-theo-từng-shop) (`ghnOrderCode`).

---

### UC-LOG-03: Đồng bộ hành trình giao nhận qua Webhook GHN
- **Mã Use Case:** `UC-LOG-03`
- **Tác nhân chính:** `ACT-3PL` Đối tác vận chuyển (GHN, GHTK, ViettelPost, J&T)
- **Tác nhân phụ:** `ACT-FIN` Finance Officer (Nhân viên tài chính); `ACT-PGW` Payment Gateway (VNPay, SePay)
- **Mục đích nghiệp vụ:** Tiếp nhận các thông báo chặng vận chuyển từ đơn vị giao vận đối tác thông qua Webhook để tự động cập nhật trạng thái đơn hàng của sàn sang Đang giao (`DELIVERING`) hoặc Đã giao thành công (`DELIVERED`).
- **Tiền điều kiện:** GHN gửi request Webhook tới endpoint `/webhooks/ghn`.
- **Hậu điều kiện:** Bản ghi `orders` được cập nhật trạng thái; tạo nhật ký sự kiện trong `shipment_events`; gửi thông báo cho người mua.
- **Luồng sự kiện chính:**
  1. Bưu tá GHN quét mã nhận hàng tại kho shop ➔ GHN gửi Webhook trạng thái `picking` / `storing`.
  2. Khi kiện hàng xuất kho trung chuyển đi giao ➔ GHN gửi Webhook trạng thái `delivering`. Backend cập nhật `orders.status = 'DELIVERING'`, tạo `shipment_events`.
  3. Khi bưu tá giao hàng thành công cho khách ➔ GHN gửi Webhook trạng thái `delivered`.
  4. Backend cập nhật `orders.status = 'DELIVERED'`, ghi nhận `delivered_at = now()`.
  5. Hệ thống kích hoạt bộ đếm ngược 7 ngày tự động hoàn tất đơn (`UC-ORD-12`).
  6. Gửi thông báo đẩy đến ứng dụng của người mua: "Đơn hàng của bạn đã được giao thành công. Vui lòng kiểm tra và xác nhận".
  7. Trạng thái hành trình của mọi hãng (GHN, GHTK, ViettelPost, J&T) chuẩn hóa về `ShipmentTrackingStatus`; giao diện người bán hiển thị đủ: Chờ lấy hàng, Đã lấy hàng, Đang trung chuyển, Đang giao, Đang thu tiền COD, Giao thành công, **Giao thất bại**, **Đang trả hàng / Đã trả hàng**, **Đơn gặp sự cố** (`LOST`).
- **Luồng thay thế / Ngoại lệ:**
  - *Giao hàng thất bại (Khách không nghe máy, sai địa chỉ):* GHN gửi trạng thái `delivery_fail`. Backend cập nhật nhật ký vận chuyển và gửi thông báo nhắc nhở khách hàng giữ liên lạc.
- **Luồng phụ — Đối soát tiền thu hộ COD với GHN (Staff):**
  1. GHN chuyển tiền thu hộ về tài khoản ngân hàng của sàn theo chu kỳ (thường 2–3 lần / tuần) kèm bảng kê đối soát COD; SePay báo tiền vào (webhook `in`) → hệ thống tạo phiên đối soát "Đã nhận X đồng từ GHN, chờ bảng kê".
  2. Staff tải bảng kê (CSV) lên Admin Portal; hệ thống kiểm tra tổng bảng kê (trừ phí thu hộ) bằng số tiền SePay đã ghi nhận, rồi khớp từng dòng theo `ShipmentTracking.trackingCode` và `Order.grandTotal`.
  3. Dòng khớp: `EscrowLedger.fund(amount, COD_CASH, now)` → `HELD`, `Order.markPaid()` (`fundedAt`); ghi `system_logs`. Dòng lệch số tiền hoặc không tìm thấy: đánh dấu để Staff xử lý tay với GHN.
  4. Sau `markPaid()`: nếu khách đã xác nhận (`customerConfirmedAt ≠ null`) hoặc đã quá 7 ngày từ `deliveredAt` và không có khiếu nại mở → `Order.complete()` + `EscrowLedger.scheduleRelease(now)` ngay; ký quỹ được giải ngân ở lượt Cron kế tiếp (`UC-PAY-07`). Còn lại chờ `UC-ORD-12`.
- **Luồng phụ — GHN hoàn hàng về shop (`returned`):** đơn chuyển `RETURNED_TO_VENDOR`, tồn kho được cộng lại. Đơn đã trả **online** (VNPay / VietQR) → hệ thống tự tạo `RefundRequest` (`REFUND_ONLY`, `APPROVED`, lý do "giao thất bại") và hoàn tiền theo `UC-SUP-04` → `REFUNDED`. Đơn **COD** → không phát sinh tiền, ký quỹ `PENDING_FUNDING` đóng lại; phí vận chuyển hai chiều do sàn ghi nhận vào chi phí.
- **Quan hệ Use Case:** Không có.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai — xử lý phía máy chủ (cron / webhook / AI), chưa có backend; giao diện chỉ hiển thị kết quả trên lớp mock.
  - *Kế hoạch:* Sprint 4 (tuần 7–8) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [Order](../ClassDiagram/ENTITY.md#52-order--đơn-hàng-theo-từng-shop), [ShipmentTracking](../ClassDiagram/ENTITY.md#71-shipmenttracking--vận-đơn), [ShipmentEvent](../ClassDiagram/ENTITY.md#72-shipmentevent--sự-kiện-hành-trình).

---

### UC-LOG-15: Điều phối đa hãng vận chuyển (Dynamic Carrier Routing)
- **Mã Use Case:** `UC-LOG-15`
- **Tác nhân chính:** — (use case nội bộ, được «include» bởi `UC-LOG-02`)
- **Tác nhân phụ:** `ACT-3PL` Đối tác vận chuyển (GHN, GHTK, ViettelPost, J&T)
- **Mục đích nghiệp vụ:** Chọn hãng vận chuyển cho từng đơn trong số các hãng shop đã bật (GHN, GHTK, ViettelPost, J&T Express; Genie Express chỉ giao nội thành) theo phí và thời gian giao dự kiến.
- **Tiền điều kiện:** Shop bấm "Yêu cầu lấy hàng (3PL)" ở UC-LOG-02; `vendor_shipping_config` có ít nhất 1 hãng được bật (mặc định GHN).
- **Hậu điều kiện:** Đơn được gán `carrierType`; vận đơn được tạo ở hãng đã chọn.
- **Luồng sự kiện chính:**
  1. Hệ thống đọc `VendorShippingConfig` của shop và lấy danh sách hãng đang bật.
  2. Gọi API tính phí song song tới các hãng đang bật (timeout 3 giây mỗi hãng) với cân nặng / kích thước kiện và địa chỉ lấy – giao.
  3. Chọn hãng có điểm tốt nhất: phí thấp nhất trong các hãng có thời gian giao dự kiến ≤ hãng nhanh nhất + 1 ngày; nội thành ≤ 15 km và ≤ 30 kg có thể chọn Genie Express nếu shop bật.
  4. Trả hãng đã chọn cho UC-LOG-02 để tạo vận đơn; `ShipmentTracking.carrierType` ghi nhận hãng.
  5. Webhook hành trình của mọi hãng chuẩn hóa về cùng `ShipmentTrackingStatus` (UC-LOG-03) qua lớp adapter theo hãng.
- **Luồng thay thế / Ngoại lệ:**
  - *Một hãng lỗi / quá hạn khi tính phí:* bỏ qua hãng đó.
  - *Mọi hãng đều lỗi:* dùng GHN với phí đã tính ở checkout (UC-ORD-06), báo shop thử lại nếu tạo vận đơn thất bại.
- **Quan hệ Use Case:** được bao gồm bởi `UC-LOG-02`.
- **Ràng buộc nghiệp vụ & Kỹ thuật:**
  - Phí ship khách đã trả ở checkout giữ nguyên; chênh lệch phí giữa các hãng do sàn hưởng / chịu (ghi `FinancialLedger` `FREESHIP_COST` khi âm).
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai — xử lý phía máy chủ (cron / webhook / AI), chưa có backend; giao diện chỉ hiển thị kết quả trên lớp mock.
  - *Kế hoạch:* Sprint 7 (tuần 13–14) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [VendorShippingConfig](../ClassDiagram/ENTITY.md#73-vendorshippingconfig--cấu-hình-vận-chuyển-của-shop), [ShipmentTracking](../ClassDiagram/ENTITY.md#71-shipmenttracking--vận-đơn), [Order](../ClassDiagram/ENTITY.md#52-order--đơn-hàng-theo-từng-shop).

---

### UC-LOG-16: Người bán xác nhận / từ chối đơn hàng mới
- **Mã Use Case:** `UC-LOG-16`
- **Tác nhân chính:** `ACT-VND` Vendor (người bán/shop)
- **Tác nhân phụ:** `ACT-CRN` Platform Cron / Scheduler
- **Mục đích nghiệp vụ:** Cho shop xác nhận có thể giao đơn mới (thẻ "Đơn chờ xác nhận" ở Tổng quan, tab "Mới" ở Đơn hàng) trước khi đóng gói, hoặc từ chối khi hết hàng thực tế — để LOG-01 chỉ xử lý đơn đã `CONFIRMED`.
- **Tiền điều kiện:** Đơn con của shop ở `PAID` (online) hoặc `PENDING_PAYMENT` với phương thức COD.
- **Hậu điều kiện:** Đơn chuyển `CONFIRMED`, hoặc `CANCELLED_BY_VENDOR` kèm hoàn tiền / hoàn kho.
- **Luồng sự kiện chính:**
  1. Shop nhận thông báo `NEW_ORDER_FOR_VENDOR` và badge số đơn mới trên menu Đơn hàng.
  2. Shop mở đơn, xem sản phẩm, phân loại, số lượng, lời nhắn của khách (`buyerNote`) và phương thức thanh toán.
  3. Shop bấm "Xác nhận" → `orders.status = CONFIRMED`; thông báo khách "Shop đã xác nhận đơn".
  4. Đơn hiển thị ở tab "Chờ chuẩn bị" để tiếp tục UC-LOG-01.
- **Luồng thay thế / Ngoại lệ:**
  - *3a. Shop từ chối (hết hàng, sai giá):* chọn lý do → `CANCELLED_BY_VENDOR`; hoàn kho; đơn đã thanh toán online chuyển hoàn tiền theo UC-SUP-04 (`REFUND_ONLY` tự động); Điểm / voucher của khách được hoàn.
  - *3b. Quá 24 giờ shop không xác nhận:* Cron tự xác nhận đơn và ghi điểm "chậm xác nhận" vào chỉ số shop (không hủy để tránh thiệt hại cho khách).
- **Quan hệ Use Case:** Không có.
- **Ràng buộc nghiệp vụ & Kỹ thuật:**
  - Từ chối đơn ảnh hưởng tỷ lệ hủy của shop (hiển thị ở UC-VND-08).
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 4 (tuần 7–8) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [Order](../ClassDiagram/ENTITY.md#52-order--đơn-hàng-theo-từng-shop), [OrderItem](../ClassDiagram/ENTITY.md#53-orderitem--dòng-sản-phẩm-trong-đơn), [StockReservation](../ClassDiagram/ENTITY.md#55-stockreservation--giữ-chỗ-tồn-kho), [Notification](../ClassDiagram/ENTITY.md#101-notification--thông-báo-cá-nhân).

---

<a id="ph-pro"></a>
## PHÂN HỆ 07: KHUYẾN MÃI, VOUCHER, FLASH SALE & ĐIỂM (PROMOTIONS, VOUCHERS, FLASH SALE & COINS)

> **Phạm vi:** Voucher shop / sàn, kho voucher, xếp chồng 3 tầng + Điểm, Flash Sale, điểm danh & lịch sử Điểm, dùng Điểm khi thanh toán.
> **Use case (9):** `UC-PRO-01`, `UC-PRO-02`, `UC-PRO-03`, `UC-PRO-04`, `UC-PRO-05`, `UC-PRO-06`, `UC-PRO-08`, `UC-PRO-09`, `UC-PRO-10`

### UC-PRO-01: Tạo & Quản lý Voucher Gian hàng (Vendor Shop Vouchers)
- **Mã Use Case:** `UC-PRO-01`
- **Tác nhân chính:** `ACT-VND` Vendor (người bán/shop)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Cho phép người bán tự thiết kế các chương trình kích cầu riêng cho shop: Mã giảm giá theo phần trăm, giảm tiền mặt cố định, hoặc hỗ trợ một phần phí vận chuyển; chi phí chiết khấu do Người bán tự chi trả.
- **Tiền điều kiện:** Người bán đã đăng nhập và được duyệt KYC.
- **Hậu điều kiện:** Tạo bản ghi trong `vouchers` với `type = 'SHOP'`, `scope = 'ALL'` (hoặc `CATEGORY` / `PRODUCT` theo cấu hình), `vendorId = currentVendorId`.
- **Luồng sự kiện chính:**
  1. Người bán vào Kênh Marketing > Tạo Voucher mới.
  2. Nhập Mã voucher (6-12 ký tự in hoa và số, vd: `SHOP50K`) hoặc bấm nút "Tự sinh mã ngẫu nhiên".
  3. Chọn Loại giảm giá:
     - `PERCENTAGE`: Giảm theo % (ví dụ: 10%), cài đặt mức giảm tối đa (`maxDiscountCap`).
     - `FIXED_AMOUNT`: Giảm số tiền cố định (ví dụ: 50.000 VNĐ).
  4. Cài đặt điều kiện: Đơn hàng tối thiểu (`minOrderValue`), Tổng số lượt (`usageLimit`), Giới hạn lượt dùng / khách hàng (`usagePerUser`), Ngân sách (`budget`, tùy chọn).
  5. Chọn thời gian hiệu lực: Ngày bắt đầu (`validFrom`) và Ngày kết thúc (`validTo`).
  6. Phạm vi áp dụng: tất cả sản phẩm của Shop (`scope = ALL`; giới hạn theo sản phẩm / danh mục để bản mở rộng).
  7. Bấm "Lưu và Kích hoạt".
  8. Hệ thống validate dữ liệu, tạo bản ghi `vouchers`. Trạng thái: `ACTIVE` (nếu bắt đầu ngay) hoặc `SCHEDULED`.
  9. Loại giảm: **%**, **tiền**, **Freeship** (shop tài trợ); phạm vi: **toàn shop / danh mục cụ thể / sản phẩm cụ thể**, kèm **loại trừ** danh mục / sản phẩm (4 bảng phạm vi); mã 6–12 ký tự chữ HOA và số (nút "Tự tạo").
  10. Danh sách voucher shop: Đang chạy / Đã lên lịch / Đã kết thúc; thao tác **Chỉnh sửa**, **Nhân bản**, **Tắt** (hộp xác nhận).
  11. Thẻ thống kê: Voucher đang chạy, **Lượt dùng tháng này**, **Doanh thu từ khuyến mãi** (Σ giá trị đơn đã hoàn tất có `OrderDiscount` của shop).
  12. Thẻ "FLASH SALE — Đăng ký gian hàng" dẫn tới UC-PRO-05.
- **Quan hệ Use Case:** Không có.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 5 (tuần 9–10) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [Voucher](../ClassDiagram/ENTITY.md#41-voucher--mã-giảm-giá), [VoucherCategory](../ClassDiagram/ENTITY.md#44-vouchercategory--voucherproduct--voucherexcludedcategory--voucherexcludedproduct).

---

### UC-PRO-02: Tạo & Quản lý Voucher Toàn sàn
- **Mã Use Case:** `UC-PRO-02`
- **Tác nhân chính:** `ACT-FIN` Finance Officer (Nhân viên tài chính); `ACT-ADM` Admin
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Staff / Admin phát hành mã giảm giá áp dụng chung toàn sàn (Platform Voucher, Tầng 2 — áp cho cả phiên thanh toán); chi phí do sàn chi trả (`sponsorType = PLATFORM`). Mã miễn phí vận chuyển và đồng tài trợ để bản mở rộng.
- **Tiền điều kiện:** Đăng nhập vai trò `STAFF` / `ADMIN`.
- **Hậu điều kiện:** Tạo `vouchers` với `type = 'PLATFORM'`, `scope = 'ALL'`, quản lý hạn mức ngân sách (`budget`).
- **Luồng sự kiện chính:**
  1. Admin truy cập Quản trị Khuyến mãi > Tạo Voucher Sàn.
  2. Nhập Mã, Loại giảm giá, Giá trị chiết khấu, Mức giảm tối đa, Đơn tối thiểu.
  3. Cài đặt **Hạn mức Ngân sách chiến dịch (Budget Cap)**: Ví dụ 100.000.000 VNĐ. Khi tổng số tiền giảm thực tế đạt ngưỡng ngân sách, voucher tự động ngừng kích hoạt kể cả khi chưa hết hạn ngày.
  4. Chọn Nhà tài trợ (`sponsorType: FundingParty`): Sàn chi trả (`PLATFORM`) hoặc Shop đồng tài trợ (`VENDOR`).
  5. Chọn thời gian hiệu lực và bấm "Phát hành".
  6. Hệ thống hiển thị Voucher trên mục "Mã giảm giá hot" ở trang chủ storefront.
  7. Trường: **tên chương trình**, mã (6–12 ký tự), loại %, tiền, **Freeship**; mức giảm, giảm tối đa, đơn tối thiểu, số lượng phát hành, giới hạn / người, **ngân sách tối đa** (voucher tự tắt khi tổng giảm vượt `budget`), thời gian hiệu lực (ngày bắt đầu không ở quá khứ).
  8. Phạm vi toàn sàn / danh mục cụ thể kèm **loại trừ**; nhà tài trợ **Genie** hoặc **Co-sponsor với Shop** (`sponsorType = CO_SPONSOR`, `platformSharePercent`).
  9. Thao tác Nhân bản, Sửa, Tắt, **Xóa vĩnh viễn** (chỉ khi chưa có lượt dùng); thẻ thống kê: đang chạy, lượt dùng hệ thống, tổng giảm giá đã dùng, voucher sắp ra mắt, ngân sách còn lại.
- **Quan hệ Use Case:** Không có.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 5 (tuần 9–10) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [Voucher](../ClassDiagram/ENTITY.md#41-voucher--mã-giảm-giá), `VoucherExcludedCategory`, `VoucherExcludedProduct` (loại trừ ngành hàng / sản phẩm khỏi voucher sàn), [VoucherCategory](../ClassDiagram/ENTITY.md#44-vouchercategory--voucherproduct--voucherexcludedcategory--voucherexcludedproduct).

---

### UC-PRO-03: Khách hàng thu thập Voucher vào Kho Voucher cá nhân
- **Mã Use Case:** `UC-PRO-03`
- **Tác nhân chính:** `ACT-CUS` Customer (khách hàng)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Người mua lưu trước các mã giảm giá của Sàn và Shop vào "Ví Voucher" cá nhân để hệ thống tự động gợi ý và áp dụng khi thanh toán.
- **Tiền điều kiện:** Khách hàng đã đăng nhập; voucher còn lượt lưu.
- **Hậu điều kiện:** Tạo bản ghi trong `user_vouchers` với `isUsed = false`.
- **Luồng sự kiện chính:**
  1. Khách hàng lướt xem Banner trang chủ, trang chi tiết shop hoặc trang sản phẩm.
  2. Thấy thẻ Voucher kèm nút "Lưu mã".
  3. Bấm "Lưu mã".
  4. Backend kiểm tra: Khách hàng chưa từng lưu mã này, hoặc chưa vượt quá giới hạn lượt dùng cá nhân (`perUserLimit`).
  5. Tạo bản ghi trong bảng `user_vouchers`: `userId`, `voucherId`, `isUsed = false`, `savedAt = now()`.
  6. Nút bấm trên giao diện chuyển thành "Đã lưu vào ví".
  7. **Kho Voucher** (Tài khoản): tab Tất cả / Voucher Sàn / Voucher Shop / Freeship; thẻ voucher hiển thị "Chưa tới giờ" (chưa đến `validFrom`), "Dùng ngay", lượt đã dùng.
  8. Xem **chi tiết voucher** (ưu đãi, nhà tài trợ, điều kiện, phạm vi áp dụng / ngoại trừ, hạn sử dụng, lượt mỗi tài khoản, ngân sách); **xóa voucher khỏi kho**; **Lịch sử voucher** (Đã sử dụng / Hết hiệu lực).
  9. Việc áp voucher khi thanh toán thuộc UC-PRO-04.
- **Quan hệ Use Case:** Không có.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 5 (tuần 9–10) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [UserVoucher](../ClassDiagram/ENTITY.md#43-uservoucher--ví-voucher-của-người-dùng), [Voucher](../ClassDiagram/ENTITY.md#41-voucher--mã-giảm-giá).

---

### UC-PRO-04: Thuật toán Stacking Voucher 3 Tầng & Điểm tự động khi Checkout
- **Mã Use Case:** `UC-PRO-04`
- **Tác nhân chính:** `ACT-CUS` Customer (khách hàng)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Cho phép người mua xếp chồng tối đa 1 voucher Shop cho **mỗi** shop + 1 voucher Sàn + 1 mã Freeship trong cùng lần thanh toán, rồi dùng Điểm; tự gợi ý bộ mã giảm nhiều nhất và phân bổ giảm giá vào từng dòng hàng để hoàn tiền từng món chính xác.
- **Tiền điều kiện:** Khách ở màn hình xem trước đơn (UC-ORD-04) và chọn / nhập mã voucher.
- **Hậu điều kiện:** Bộ voucher hợp lệ được áp; `OrderDiscount` và các cột `shopDiscount`, `platformDiscount`, `coinDiscount`, `proratedDiscount` của `OrderItem` được tính (ghi khi tạo đơn ở UC-ORD-07).
- **Luồng sự kiện chính:**
  1. Hệ thống quét Kho voucher (`user_vouchers` chưa dùng, còn hạn) và mã khách nhập; nhóm theo tầng.
  2. Kiểm tra từng mã: còn hiệu lực (`validFrom ≤ now ≤ validTo`, `isActive`), còn lượt toàn hệ thống (`usageLimit`) và theo người (`usagePerUser`), còn ngân sách (`totalDiscountUsed < budget`), đúng phạm vi (`scope` ALL / CATEGORY / PRODUCT qua bảng phạm vi) và không thuộc danh sách loại trừ.
  3. **Tầng 1 — Voucher Shop** (mỗi shop tối đa 1 mã): điều kiện đơn tối thiểu tính trên tạm tính hàng hợp lệ của shop đó; giảm trực tiếp trên tiền hàng của shop.
  4. **Tầng 2 — Voucher Sàn** (tối đa 1 mã): đơn tối thiểu tính trên tổng sau tầng 1; số giảm phân bổ về từng shop rồi từng dòng theo tỷ lệ giá trị (`Item Platform Discount = lineTotal / tổng hợp lệ × số giảm`); nhà tài trợ `PLATFORM` hoặc `CO_SPONSOR` (chia theo `platformSharePercent`).
  5. **Tầng 3 — Freeship** (tối đa 1 mã): trừ vào tổng phí ship GHN, phân bổ theo phí ship từng shop, không vượt phí ship thực tế.
  6. **Sau cùng — Điểm** (UC-PRO-10): tối đa 50% tiền hàng còn lại sau 3 tầng, 1 Điểm = 1₫.
  7. Hệ thống tự gợi ý bộ mã cho tổng giảm lớn nhất (thử các tổ hợp hợp lệ, mỗi tầng ≤ 5 ứng viên); khách đổi tay được.
  8. Hiển thị bảng chi tiết: tổng tiền hàng, phí ship, giảm Shop / Sàn / Freeship / Điểm, "Voucher Sàn phân bổ" theo shop, tổng thanh toán.
- **Luồng thay thế / Ngoại lệ:**
  - *Mã hết lượt / hết ngân sách / hết hạn lúc bấm Đặt hàng:* báo "Mã voucher không còn hiệu lực", tính lại không có mã đó (khóa lạc quan `Voucher.version`).
  - *Mã không đủ điều kiện:* hiển thị lý do ("Chưa đạt giá trị tối thiểu …", "Giỏ hàng chưa có sản phẩm phù hợp").
  - *Hủy đơn / hoàn tiền:* trả lượt dùng (`usedCount − 1`, xóa `voucher_usages`) và hoàn Điểm theo phần đã phân bổ của các dòng bị hủy.
- **Quan hệ Use Case:** `<<extend>>` Xem trước đơn hàng & Tách giỏ hàng đa nhà bán (Cart Preview & Splitting) (`UC-ORD-04`) tại điểm mở rộng «Chọn voucher» khi {khách chọn hoặc nhập mã voucher}; được mở rộng bởi `UC-PRO-10` (điểm «Dùng Điểm»).
- **Ràng buộc nghiệp vụ & Kỹ thuật:**
  - `grandTotal ≥ 0` luôn đúng; thứ tự áp dụng cố định Shop → Sàn → Freeship → Điểm (khớp `Voucher.priority`).
  - Giảm giá do sàn tài trợ không làm giảm hoa hồng / tiền shop nhận (UC-PAY-05).
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 8 (tuần 15–16) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [Voucher](../ClassDiagram/ENTITY.md#41-voucher--mã-giảm-giá), [VoucherCategory](../ClassDiagram/ENTITY.md#44-vouchercategory--voucherproduct--voucherexcludedcategory--voucherexcludedproduct), [UserVoucher](../ClassDiagram/ENTITY.md#43-uservoucher--ví-voucher-của-người-dùng), [VoucherUsage](../ClassDiagram/ENTITY.md#42-voucherusage--lịch-sử-sử-dụng-voucher), [OrderDiscount](../ClassDiagram/ENTITY.md#54-orderdiscount--chi-tiết-các-mã-đã-áp-cho-đơn), [OrderItem](../ClassDiagram/ENTITY.md#53-orderitem--dòng-sản-phẩm-trong-đơn).

---

### UC-PRO-05: Tạo chiến dịch Flash Sale & Cấu hình khung giờ vàng
- **Mã Use Case:** `UC-PRO-05`
- **Tác nhân chính:** `ACT-FIN` Finance Officer (Nhân viên tài chính); `ACT-ADM` Admin; `ACT-VND` Vendor (người bán/shop)
- **Tác nhân phụ:** `ACT-CRN` Platform Cron / Scheduler
- **Mục đích nghiệp vụ:** Tạo chiến dịch giảm giá sốc theo khung giờ (khu "FLASH SALE" trên trang chủ, thẻ "Đăng ký gian hàng" ở Khuyến mãi của shop) để kích thích mua sắm.
- **Tiền điều kiện:** Nhân viên tài chính / Admin có quyền `MANAGE_VOUCHER` tạo chiến dịch sàn; người bán `VERIFIED` đăng ký sản phẩm.
- **Hậu điều kiện:** `flash_sale_campaigns` và `flash_sale_items` được tạo; chiến dịch chạy và kết thúc tự động.
- **Luồng sự kiện chính:**
  1. Nhân viên tạo chiến dịch: tên ("FLASH SALE 12.12"), khung giờ `startsAt` – `endsAt`, số sản phẩm tối đa (mặc định 20), hạn đăng ký.
  2. Người bán đăng ký sản phẩm: chọn sản phẩm / SKU, giá Flash Sale (giảm tối thiểu 15% so với giá bán 30 ngày gần nhất), số suất `flashStock` (`FlashSaleItem.validateAgainst()`: 0 < suất ≤ tồn khả dụng).
  3. Nhân viên duyệt danh sách sản phẩm tham gia rồi `schedule()` (`DRAFT` → `SCHEDULED`).
  4. Đúng giờ bắt đầu, Cron kiểm tra lại tồn và `activate()` (`ACTIVE`); trang chủ hiện đồng hồ đếm ngược và giá sốc; gửi `FLASH_SALE_REMINDER` cho người theo dõi shop.
  5. Hết giờ hoặc hết suất, Cron `end()` (`ENDED`); sản phẩm trở về giá gốc.
- **Luồng thay thế / Ngoại lệ:**
  - *Tồn giảm sau khi đăng ký:* lúc mở bán hạ `flashStock` về mức khả dụng và báo người bán.
  - *Shop dừng chiến dịch riêng của shop:* gọi `end()` sớm; đơn đã đặt vẫn giữ giá Flash Sale.
- **Quan hệ Use Case:** Không có.
- **Ràng buộc nghiệp vụ & Kỹ thuật:**
  - Shop tự tạo chiến dịch riêng (`vendorId` có giá trị) với tối đa 1 chiến dịch đang chạy.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 8 (tuần 15–16) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [FlashSaleCampaign](../ClassDiagram/ENTITY.md#45-flashsalecampaign--chiến-dịch-flash-sale), [FlashSaleItem](../ClassDiagram/ENTITY.md#46-flashsaleitem--sản-phẩm-trong-flash-sale), [Product](../ClassDiagram/ENTITY.md#22-product--sản-phẩm-spu).

---

### UC-PRO-06: Mua hàng Flash Sale chống bán vượt suất
- **Mã Use Case:** `UC-PRO-06`
- **Tác nhân chính:** `ACT-CUS` Customer (khách hàng)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Cho phép khách mua sản phẩm đang Flash Sale với giá sốc mà không bán vượt số suất, kể cả khi nhiều người bấm cùng lúc.
- **Tiền điều kiện:** Chiến dịch `ACTIVE` (`isRunning(now)`); sản phẩm còn suất.
- **Hậu điều kiện:** Đơn được tạo với giá Flash Sale và suất được giữ (`reservedCount`) hoặc khách được báo hết suất.
- **Luồng sự kiện chính:**
  1. Khách bấm "Mua ngay" sản phẩm có nhãn FLASH SALE (mỗi khách tối đa 1 suất / sản phẩm / chiến dịch).
  2. Khi tạo đơn (UC-ORD-07), hệ thống giữ suất bằng **một câu UPDATE có điều kiện**: tăng `reserved_count` khi `flash_stock − sold_count − reserved_count ≥ số lượng` — nguyên tử trong PostgreSQL, không cần khóa bi quan.
  3. Thành công: `OrderItem.unitPrice` = giá Flash Sale; giữ chỗ tồn kho 15 phút như đơn thường.
  4. Thanh toán thành công: `recordSale()` (`reservedCount → soldCount`); thanh tiến trình "Đã bán 45/50" cập nhật.
  5. Quá hạn thanh toán / hủy trước khi trả tiền: `releaseReservation()` trả suất (UC-ORD-08).
- **Luồng thay thế / Ngoại lệ:**
  - *0 dòng được cập nhật:* hết suất — báo `GNE-INV-4091` "Sản phẩm Flash Sale đã hết suất", khách có thể mua giá thường.
  - *Khách đã mua suất này:* từ chối, báo đã đạt giới hạn mỗi người.
- **Quan hệ Use Case:** `<<extend>>` Tạo phiên Checkout & Các Đơn hàng con theo Shop (PersistCheckout) (`UC-ORD-07`) tại điểm mở rộng «Giữ chỗ tồn kho» khi {sản phẩm đang trong khung Flash Sale}.
- **Ràng buộc nghiệp vụ & Kỹ thuật:**
  - Ở quy mô TLCN (~50 người dùng đồng thời) dùng UPDATE có điều kiện; bản DATN chuyển cửa chặn tải sang Redis Lua + hàng đợi — mô hình dữ liệu giữ nguyên.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 8 (tuần 15–16) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [FlashSaleItem](../ClassDiagram/ENTITY.md#46-flashsaleitem--sản-phẩm-trong-flash-sale), [FlashSaleCampaign](../ClassDiagram/ENTITY.md#45-flashsalecampaign--chiến-dịch-flash-sale), [Order](../ClassDiagram/ENTITY.md#52-order--đơn-hàng-theo-từng-shop), [OrderItem](../ClassDiagram/ENTITY.md#53-orderitem--dòng-sản-phẩm-trong-đơn), [StockReservation](../ClassDiagram/ENTITY.md#55-stockreservation--giữ-chỗ-tồn-kho).

---

### UC-PRO-08: Điểm danh nhận Điểm hàng ngày & Duy trì chuỗi điểm danh (Streak)
- **Mã Use Case:** `UC-PRO-08`
- **Tác nhân chính:** `ACT-CUS` Customer (khách hàng)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Tạo thói quen truy cập hằng ngày bằng thưởng Điểm tăng dần theo chuỗi ngày liên tục (nút "Điểm danh nhận Điểm" ở mục Điểm Genie).
- **Tiền điều kiện:** Khách đã đăng nhập; chưa điểm danh hôm nay (theo ngày lịch giờ Việt Nam).
- **Hậu điều kiện:** `daily_checkins` cập nhật chuỗi; `user_coins` cộng Điểm; `coin_transactions` thêm dòng `CHECK_IN` hết hạn sau 12 tháng.
- **Luồng sự kiện chính:**
  1. Khách bấm "Điểm danh nhận Điểm".
  2. Hệ thống khóa ngắn trên Redis (`SET lock:checkin:{userId} NX EX 5`) chống bấm liên tiếp.
  3. Đọc `daily_checkins` của khách: hôm qua đã điểm danh → `streakCount + 1`; cũ hơn → đặt lại 1.
  4. Thưởng: ngày 1–6 mỗi ngày 100 Điểm; ngày thứ 7 liên tiếp 1.000 Điểm rồi bắt đầu chuỗi mới.
  5. Trong một transaction: cập nhật `daily_checkins`, `UserCoin.earn()`, ghi `CoinTransaction` (`CHECK_IN`, `expiresAt = now + 12 tháng`).
  6. Hiển thị hiệu ứng nhận Điểm và tiến trình chuỗi 7 ngày.
- **Luồng thay thế / Ngoại lệ:**
  - *Đã điểm danh hôm nay:* nút chuyển "Đã điểm danh", đếm ngược tới 00:00.
- **Quan hệ Use Case:** Không có.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 8 (tuần 15–16) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [DailyCheckin](../ClassDiagram/ENTITY.md#49-dailycheckin--điểm-danh-hằng-ngày), [UserCoin](../ClassDiagram/ENTITY.md#47-usercoin--số-dư-điểm), [CoinTransaction](../ClassDiagram/ENTITY.md#48-cointransaction--lịch-sử-biến-động-điểm).

---

### UC-PRO-09: Tra cứu số dư Điểm Genie & Lịch sử biến động Điểm
- **Mã Use Case:** `UC-PRO-09`
- **Tác nhân chính:** `ACT-CUS` Customer (khách hàng)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Cho khách xem số dư Điểm, số Điểm sắp hết hạn, lịch sử nhận / tiêu Điểm và đổi Điểm lấy voucher (nút "Đổi quà").
- **Tiền điều kiện:** Khách đã đăng nhập.
- **Hậu điều kiện:** Hiển thị số dư và lịch sử; khi đổi quà: trừ Điểm và thêm voucher vào Kho voucher.
- **Luồng sự kiện chính:**
  1. Khách mở Tài khoản → Điểm Genie; client gọi `GET /api/v1/customer/coins` và `/coins/history`.
  2. Hiển thị: số dư (1 Điểm = 1₫ khi thanh toán), số Điểm hết hạn trong 30 ngày tới, lịch sử: điểm danh (+100), hoàn Điểm đơn (+250), đánh giá có ảnh (+50), dùng giảm giá đơn (−500), hoàn Điểm khi hủy đơn.
  3. Kiểm tra toàn vẹn số dư (`UserCoin.verifyHash()`); lệch → khóa tiêu Điểm và cảnh báo Admin.
  4. **Đổi quà:** khách chọn voucher sàn có giá đổi bằng Điểm (danh sách do sàn cấu hình) → trừ Điểm theo lô sắp hết hạn trước (FEFO), ghi `VOUCHER_REDEEM`, thêm `user_vouchers`.
- **Luồng thay thế / Ngoại lệ:**
  - *Không đủ Điểm để đổi:* nút đổi bị khóa, hiển thị số Điểm còn thiếu.
- **Quan hệ Use Case:** Không có.
- **Ràng buộc nghiệp vụ & Kỹ thuật:**
  - Điểm không quy đổi thành tiền mặt / rút về ngân hàng. Lô Điểm hết hạn được Cron ghi dòng âm để `Σ amount = balance`.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 8 (tuần 15–16) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [UserCoin](../ClassDiagram/ENTITY.md#47-usercoin--số-dư-điểm), [CoinTransaction](../ClassDiagram/ENTITY.md#48-cointransaction--lịch-sử-biến-động-điểm), [UserVoucher](../ClassDiagram/ENTITY.md#43-uservoucher--ví-voucher-của-người-dùng).

---

### UC-PRO-10: Sử dụng Điểm Genie để giảm trừ thanh toán đơn hàng
- **Mã Use Case:** `UC-PRO-10`
- **Tác nhân chính:** `ACT-CUS` Customer (khách hàng)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Cho phép khách tiêu Điểm để trừ trực tiếp vào tiền thanh toán (ô "Sử dụng Điểm Genie — 1 Điểm = 1₫, dùng tối đa 50% tiền hàng" ở checkout).
- **Tiền điều kiện:** Số dư Điểm > 0 tại thời điểm checkout.
- **Hậu điều kiện:** Đơn ghi `coinUsedCount`, `coinDiscountAmount`; `OrderItem.coinDiscount` được phân bổ; Điểm bị trừ và ghi `USED_AT_CHECKOUT`.
- **Luồng sự kiện chính:**
  1. Checkout hiển thị "Dùng N Điểm (giảm N₫)" với `N = min(số dư, 50% tiền hàng sau voucher)`.
  2. Khách bật công tắc; tổng thanh toán tính lại (bước cuối của UC-PRO-04).
  3. Khi tạo đơn (UC-ORD-07), trong cùng transaction: `UserCoin.spend()` (khóa lạc quan `version`), ghi `CoinTransaction` âm, phân bổ `coinDiscount` vào từng dòng hàng theo tỷ lệ giá trị.
  4. Sàn chịu phần giảm Điểm: shop vẫn nhận đủ tiền hàng; ghi sổ cái `COIN_COST` khi đơn hoàn tất (UC-PAY-06).
- **Luồng thay thế / Ngoại lệ:**
  - *Đơn bị hủy / quá hạn thanh toán / hoàn tiền:* hoàn Điểm theo phần đã phân bổ của các dòng bị hủy (`ORDER_REFUND`, giữ hạn của lô cũ).
- **Quan hệ Use Case:** `<<extend>>` Thuật toán Stacking Voucher 3 Tầng & Điểm tự động khi Checkout (`UC-PRO-04`) tại điểm mở rộng «Dùng Điểm» khi {khách bật “Sử dụng Điểm Genie”}.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 8 (tuần 15–16) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [UserCoin](../ClassDiagram/ENTITY.md#47-usercoin--số-dư-điểm), [CoinTransaction](../ClassDiagram/ENTITY.md#48-cointransaction--lịch-sử-biến-động-điểm), [Order](../ClassDiagram/ENTITY.md#52-order--đơn-hàng-theo-từng-shop), [OrderItem](../ClassDiagram/ENTITY.md#53-orderitem--dòng-sản-phẩm-trong-đơn).

---

<a id="ph-soc"></a>
## PHÂN HỆ 08: CHAT, ĐÁNH GIÁ & THEO DÕI SHOP (CHAT, REVIEWS & SHOP FOLLOW)

> **Phạm vi:** Chat realtime khách – shop (thẻ sản phẩm, ảnh, trạng thái đã xem), đánh giá & Hữu ích, phản hồi đánh giá, báo cáo / kiểm duyệt đánh giá, theo dõi shop, phân tích cảm xúc đánh giá bằng PhoBERT.
> **Use case (7):** `UC-SOC-01`, `UC-SOC-02`, `UC-SOC-03`, `UC-SOC-04`, `UC-SOC-05`, `UC-SOC-06`, `UC-AI-07`

### UC-SOC-01: Nhắn tin trò chuyện thời gian thực Khách - Shop (Live Chat)
- **Mã Use Case:** `UC-SOC-01`
- **Tác nhân chính:** `ACT-CUS` Customer (khách hàng); `ACT-VND` Vendor (người bán/shop)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Trao đổi trực tiếp, tư vấn kích cỡ, màu sắc, tình trạng hàng hóa tức thời giữa người mua và chủ shop trước và sau khi đặt hàng.
- **Tiền điều kiện:** Cả hai bên đã đăng nhập tài khoản.
- **Hậu điều kiện:** Tạo cuộc hội thoại trong `conversations`; lưu tin nhắn vào `messages`; phát gói tin qua WebSocket; cập nhật số tin chưa đọc.
- **Luồng sự kiện chính:**
  1. Khách hàng bấm nút "Chat ngay" tại trang chi tiết sản phẩm hoặc trang shop.
  2. Client mở kết nối WebSocket `/ws?token=<JWT>`.
  3. Hệ thống tìm kiếm cuộc hội thoại trong bảng `conversations` theo cặp `(customerId, vendorId)`:
     - Nếu chưa có: Tạo mới bản ghi `conversations`.
     - Nếu đã có: Tải lịch sử 30 tin nhắn gần nhất.
  4. Người gửi nhập nội dung tin nhắn và bấm Gửi.
  5. Client bắn sự kiện `CHAT` qua WebSocket lên Server.
  6. Backend Spring Boot mở transaction:
     - Lưu bản ghi vào bảng `messages`: `conversationId`, `senderId`, `type = 'TEXT'`, `content`, `isRead = false`.
     - Cập nhật `conversations.lastMessageSnippet`, `conversations.lastMessageAt = now()`.
  7. Server đẩy message vào Redis Pub/Sub kênh `vendor_chat:{vendorId}` hoặc `customer_chat:{customerId}`.
  8. WebSocket Hub truyền tin nhắn đến thiết bị của người nhận ngay lập tức (< 100ms).
  9. Khi người nhận mở cửa sổ chat: Client gửi sự kiện `CHAT_READ`, hệ thống cập nhật `messages.isRead = true`, xóa huy hiệu tin nhắn chưa đọc.
  10. Danh sách hội thoại (người bán): ảnh khách, tin nhắn cuối, số chưa đọc, trạng thái Online; gửi thẻ sản phẩm / ảnh / thẻ đơn và trạng thái đã xem thuộc UC-SOC-02; lời chào / tin vắng mặt tự động theo UC-VND-06.
- **Luồng thay thế / Ngoại lệ:**
  - *Người nhận đang offline:* Hệ thống lưu tin nhắn vào CSDL và tạo thông báo in-app (`UC-NOT-01`) để người nhận thấy khi mở lại ứng dụng; tin nhắn đầu tiên trong ngày gửi kèm email nhắc (`UC-NOT-03`).
  - *Shop đang ngoài giờ làm việc:* Nếu shop bật trả lời tự động (`autoReplyEnabled`), hệ thống tự động sinh 1 tin nhắn trả lời với nội dung `autoReplyOffline` gửi lại cho khách.
- **Quan hệ Use Case:** được mở rộng bởi `UC-SOC-02` (điểm «Gửi tin nhắn»).
- **Ràng buộc nghiệp vụ & Kỹ thuật:**
  - Sử dụng Redis Pub/Sub làm message bus để đảm bảo chat hoạt động thông suốt kể cả khi hệ thống chạy nhiều cụm server (Horizontal Scaling).
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 6 (tuần 11–12) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [Conversation](../ClassDiagram/ENTITY.md#111-conversation--cuộc-hội-thoại), [Message](../ClassDiagram/ENTITY.md#112-message--tin-nhắn).

---

### UC-SOC-02: Gửi thẻ sản phẩm, hình ảnh và trạng thái đơn hàng trong chat
- **Mã Use Case:** `UC-SOC-02`
- **Tác nhân chính:** `ACT-CUS` Customer (khách hàng); `ACT-VND` Vendor (người bán/shop)
- **Tác nhân phụ:** `ACT-MIN` Silo S3 Storage
- **Mục đích nghiệp vụ:** Nâng chất lượng tư vấn: gửi thẻ sản phẩm đang hỏi ("Bạn đang trao đổi với Người bán về sản phẩm này"), ảnh thực tế, thẻ đơn hàng; hiển thị trạng thái đã gửi / đã nhận / đã xem.
- **Tiền điều kiện:** Đang trong hội thoại khách – shop (UC-SOC-01).
- **Hậu điều kiện:** `messages` mới với `type` = `IMAGE`, `PRODUCT_CARD` hoặc `ORDER_CARD`.
- **Luồng sự kiện chính:**
  1. *Thẻ sản phẩm:* khách bấm "Chat ngay" trên trang sản phẩm → hội thoại mở kèm thẻ (ảnh nhỏ, tên, giá) — `type = PRODUCT_CARD`, `productVariantId`.
  2. *Ảnh:* chọn ảnh (≤ 5 MB) → tải lên Silo qua Presigned URL → gửi `attachmentUrl`, `type = IMAGE`.
  3. *Thẻ đơn hàng:* người bán chọn đơn của khách → gửi thẻ mã đơn, trạng thái, mã vận đơn (`ORDER_CARD`, `content` JSON).
  4. Bấm thẻ sản phẩm mở trang sản phẩm; bấm thẻ đơn mở trang theo dõi đơn.
  5. Trạng thái tin nhắn: "Đang gửi…" → "Đã nhận" (WebSocket ack) → "Đã xem" (sự kiện `CHAT_READ_RECEIPT` khi bên kia mở hội thoại, cập nhật `readAt`).
- **Luồng thay thế / Ngoại lệ:**
  - *Ảnh vượt dung lượng / sai định dạng:* báo lỗi, không gửi.
  - *Khách chưa đăng nhập bấm Chat:* yêu cầu đăng nhập ("Bạn cần đăng nhập để có thể nhắn tin trực tiếp với Shop").
- **Quan hệ Use Case:** `<<include>>` Tải tệp lên Silo (Presigned URL) — tác nhân phụ: `ACT-MIN` Silo S3 Storage; `<<extend>>` Nhắn tin trò chuyện thời gian thực Khách - Shop (Live Chat) (`UC-SOC-01`) tại điểm mở rộng «Gửi tin nhắn» khi {gửi ảnh, thẻ sản phẩm hoặc thẻ đơn hàng}.
- **Ràng buộc nghiệp vụ & Kỹ thuật:**
  - Khung chat có **câu hỏi gợi ý nhanh** cố định ("Sản phẩm còn hàng không?", "Khi nào giao tới?", "Có giảm thêm không ạ?", "Cho mình xem ảnh thật") — bấm là gửi.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 8 (tuần 15–16) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [Message](../ClassDiagram/ENTITY.md#112-message--tin-nhắn), [Conversation](../ClassDiagram/ENTITY.md#111-conversation--cuộc-hội-thoại), [ProductVariant](../ClassDiagram/ENTITY.md#23-productvariant--biến-thể-sản-phẩm-sku), [Order](../ClassDiagram/ENTITY.md#52-order--đơn-hàng-theo-từng-shop).

---

### UC-SOC-03: Đánh giá sản phẩm đã mua & Tải ảnh thực tế (Verified Buyer Review)
- **Mã Use Case:** `UC-SOC-03`
- **Tác nhân chính:** `ACT-CUS` Customer (khách hàng)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Xây dựng niềm tin mua sắm dựa trên bằng chứng xã hội (Social Proof); chỉ cho phép những người thực sự đã mua và nhận hàng thành công được chấm điểm sao và nhận xét thực tế về chất lượng sản phẩm.
- **Tiền điều kiện:** Khách hàng đã mua sản phẩm này trong một đơn hàng có trạng thái `COMPLETED`; chưa từng đánh giá sản phẩm của đơn hàng đó.
- **Hậu điều kiện:** Tạo bản ghi trong `product_reviews`; cập nhật điểm đánh giá trung bình `rating` trong bảng `products`.
- **Luồng sự kiện chính:**
  1. Khách hàng vào mục Đơn mua > Đơn đã hoàn tất, bấm "Đánh giá sản phẩm".
  2. Chọn số sao đánh giá (1 đến 5 sao).
  3. Viết nội dung nhận xét chi tiết về chất lượng sản phẩm, độ đúng mô tả, thời gian giao hàng.
  4. Tải lên tối đa 5 ảnh chụp thực tế hoặc video mở hộp sản phẩm.
  5. Chọn chế độ: "Hiển thị tên công khai" hoặc "Đánh giá ẩn tên" (che tên người mua khi hiển thị).
  6. Bấm "Gửi đánh giá".
  7. Backend kiểm tra tính hợp lệ:
     - Xác thực quyền: Kiểm tra `order_items` để đảm bảo user này đã mua sản phẩm trong đơn đã `COMPLETED`.
  8. Mở transaction CSDL:
     - Tạo bản ghi `product_reviews`: `orderItemId` (UNIQUE — chỉ dòng hàng đã mua và đã giao mới đánh giá được), `productId`, `customerId`, `vendorId`, `rating`, `comment`, `imageUrlsJson`, `videoUrl`.
     - Tính toán lại điểm trung bình: `UPDATE products SET rating = (SELECT AVG(rating) FROM product_reviews WHERE product_id = ?), review_count = review_count + 1 WHERE id = ?`.
  9. Phản hồi thông báo đánh giá thành công.
  10. Tối đa 5 ảnh; đánh giá có ảnh được tặng **50 Điểm** (`REVIEW_BONUS`, UC-PRO-09).
  11. Trang sản phẩm: lọc Tất cả / Có hình ảnh; nút **"Hữu ích (N)"** — mỗi người 1 lần (`ReviewHelpfulVote`, UNIQUE), không tự bấm cho đánh giá của mình, bấm lại để bỏ (`ProductReview.adjustHelpful()`); đánh giá nhiều lượt Hữu ích lên đầu.
  12. Người dùng có thể **Báo cáo** đánh giá vi phạm (UC-SOC-05).
- **Quan hệ Use Case:** `<<include>>` Tải tệp lên Silo (Presigned URL) — tác nhân phụ: `ACT-MIN` Silo S3 Storage.
- **Ràng buộc nghiệp vụ & Kỹ thuật:**
  - Quy tắc Chống đánh giá ảo (Anti-Fake Reviews): Nghiêm cấm tuyệt đối tài khoản chưa từng mua hàng gửi đánh giá — mỗi đánh giá bắt buộc gắn `orderItemId` của một đơn đã giao thuộc chính người đánh giá (cột UNIQUE nên mỗi dòng hàng chỉ đánh giá 1 lần).
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 5 (tuần 9–10) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [ProductReview](../ClassDiagram/ENTITY.md#91-productreview--đánh-giá-sản-phẩm), [Product](../ClassDiagram/ENTITY.md#22-product--sản-phẩm-spu), [ReviewHelpfulVote](../ClassDiagram/ENTITY.md#94-reviewhelpfulvote--lượt-bấm-hữu-ích-cho-đánh-giá), [CoinTransaction](../ClassDiagram/ENTITY.md#48-cointransaction--lịch-sử-biến-động-điểm).

---

### UC-SOC-04: Người bán phản hồi đánh giá của khách hàng (Vendor Review Reply)
- **Mã Use Case:** `UC-SOC-04`
- **Tác nhân chính:** `ACT-VND` Vendor (người bán/shop)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Cho phép người bán trả lời các đánh giá của khách hàng nhằm giải thích, cảm ơn hoặc đề xuất phương án bảo hành/đổi trả nếu khách hàng gặp trải nghiệm không hài lòng.
- **Tiền điều kiện:** Người bán sở hữu sản phẩm được đánh giá; đánh giá chưa từng được phản hồi.
- **Hậu điều kiện:** Cập nhật `vendorReply`, `vendorRepliedAt` trong `product_reviews` (qua `ProductReview.reply()`, chỉ 1 lần).
- **Luồng sự kiện chính:**
  1. Người bán vào Vendor Portal > Quản lý đánh giá.
  2. Xem danh sách đánh giá mới, bấm "Phản hồi" tại một đánh giá của khách.
  3. Nhập nội dung phản hồi (tối đa 500 ký tự, lịch sự, không chứa số điện thoại cá nhân nhằm kéo khách ra ngoài giao dịch).
  4. Bấm "Gửi phản hồi".
  5. Hệ thống gọi `ProductReview.reply(text, now)` lưu `vendorReply` và `vendorRepliedAt = now()` vào bản ghi `product_reviews`.
  6. Khách hàng nhận được thông báo: "Người bán [Tên Shop] vừa phản hồi đánh giá của bạn".
  7. Nội dung phản hồi của shop hiển thị công khai ngay dưới phần đánh giá của khách trên trang sản phẩm.
  8. Lọc Tất cả / Chưa phản hồi / Cần chú ý (≤ 3★); hiển thị phân loại đã mua; báo cáo đánh giá vi phạm (UC-SOC-05).
- **Quan hệ Use Case:** Không có.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 5 (tuần 9–10) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [ProductReview](../ClassDiagram/ENTITY.md#91-productreview--đánh-giá-sản-phẩm).

---

### UC-SOC-05: Báo cáo đánh giá vi phạm / spam & Kiểm duyệt đánh giá (Review Moderation & Report)
- **Mã Use Case:** `UC-SOC-05`
- **Tác nhân chính:** `ACT-VND` Vendor (người bán/shop); `ACT-CUS` Customer (khách hàng); `ACT-MOD` Moderator (Nhân viên kiểm duyệt); `ACT-CS` CS Specialist (Nhân viên CSKH)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Cho người bán / khách báo cáo đánh giá thô tục, spam, chuyển hướng giao dịch, mua giả; nhân viên kiểm duyệt ẩn / giữ hiển thị (mục "Kiểm duyệt đánh giá" của Admin).
- **Tiền điều kiện:** Đánh giá đang hiển thị; người duyệt có quyền `MODERATE_REVIEW`.
- **Hậu điều kiện:** `review_reports` được tạo; nếu vi phạm: `product_reviews.isVisible = false` và điểm sản phẩm / shop được tính lại.
- **Luồng sự kiện chính:**
  1. Người bán hoặc khách bấm "Báo cáo" ở một đánh giá, chọn lý do (`SPAM`, `OFFENSIVE`, `FAKE_PURCHASE`) và ghi chú.
  2. Hệ thống tạo `review_reports`; đánh giá có từ 3 báo cáo lên đầu hàng chờ "CHỜ DUYỆT", kế đến đánh giá được PhoBERT gắn `NEGATIVE` với độ tin cậy ≥ 0,85 (`UC-AI-07`, nhãn "AI: Tiêu cực").
  3. Nhân viên mở mục Kiểm duyệt đánh giá: xem nội dung, số lượt báo cáo, lý do, người báo cáo.
  4. Vi phạm: bấm "Ẩn đánh giá" → `isVisible = false`, ghi `system_logs` `REVIEW_HIDDEN`, tính lại `Product.rating` và `VendorProfile.applyRating()`.
  5. Không vi phạm: "Giữ hiển thị" — báo cáo đóng; đánh giá đã ẩn có thể "Hiện lại" (thao tác được ghi log).
- **Luồng thay thế / Ngoại lệ:**
  - *Shop báo cáo hàng loạt đánh giá tiêu cực hợp lệ:* báo cáo chỉ là tín hiệu ưu tiên — quyết định ẩn luôn do người duyệt.
- **Quan hệ Use Case:** `<<include>>` Ghi nhật ký kiểm toán.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 8 (tuần 15–16) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [ReviewReport](../ClassDiagram/ENTITY.md#93-reviewreport--báo-cáo-đánh-giá-vi-phạm), [ProductReview](../ClassDiagram/ENTITY.md#91-productreview--đánh-giá-sản-phẩm), [VendorProfile](../ClassDiagram/ENTITY.md#14-vendorprofile--hồ-sơ-người-bán-shop), [SystemLog](../ClassDiagram/ENTITY.md#121-systemlog--nhật-ký-kiểm-toán).

---

### UC-SOC-06: Khách hàng theo dõi gian hàng (Shop Follow)
- **Mã Use Case:** `UC-SOC-06`
- **Tác nhân chính:** `ACT-CUS` Customer (khách hàng)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Cho khách theo dõi shop yêu thích (nút "Theo dõi" ở trang sản phẩm và trang shop; mục "Shop đang theo dõi") để nhận thông báo voucher / Flash Sale của shop.
- **Tiền điều kiện:** Khách đã đăng nhập; không theo dõi chính shop của mình.
- **Hậu điều kiện:** Tạo / xóa `shop_follows`; cập nhật `VendorProfile.followerCount`.
- **Luồng sự kiện chính:**
  1. Khách bấm "Theo dõi" → `POST /api/v1/shops/{vendorId}/follow`.
  2. Chưa theo dõi: tạo `ShopFollow.of()` (UNIQUE khách + shop), tăng `followerCount`; nút chuyển "Đang theo dõi".
  3. Đã theo dõi: hỏi "Hủy theo dõi Shop?" → xóa dòng, giảm `followerCount`.
  4. Mục Tài khoản → Shop đang theo dõi: danh sách shop (ảnh, nhãn Chính hãng, số người theo dõi), nút "Vào shop".
  5. Khi shop phát hành voucher / đăng ký Flash Sale: gửi `NEW_VENDOR_VOUCHER` / `FLASH_SALE_REMINDER` tới người theo dõi.
- **Luồng thay thế / Ngoại lệ:**
  - *Chưa đăng nhập:* yêu cầu đăng nhập.
  - *Bấm liên tục:* chống trùng nhờ UNIQUE (không đếm sai `followerCount`).
- **Quan hệ Use Case:** Không có.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 8 (tuần 15–16) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [ShopFollow](../ClassDiagram/ENTITY.md#92-shopfollow--theo-dõi-shop), [VendorProfile](../ClassDiagram/ENTITY.md#14-vendorprofile--hồ-sơ-người-bán-shop), [Notification](../ClassDiagram/ENTITY.md#101-notification--thông-báo-cá-nhân).

---

### UC-AI-07: Phân tích cảm xúc đánh giá bằng PhoBERT (Sentiment Analysis)
- **Mã Use Case:** `UC-AI-07`
- **Tác nhân chính:** `ACT-AIW` AI Worker Service (KYC & PhoBERT)
- **Tác nhân phụ:** `ACT-VND` Vendor (người bán/shop); `ACT-MOD` Moderator (Nhân viên kiểm duyệt); `ACT-CS` CS Specialist (Nhân viên CSKH)
- **Mục đích nghiệp vụ:** Tự động chấm **cảm xúc tổng thể** (Tích cực / Trung tính / Tiêu cực) của mỗi đánh giá có nội dung chữ bằng mô hình **PhoBERT** nhóm đã fine-tune cho tiếng Việt, để người bán theo dõi mức hài lòng của khách trong Phân tích bán hàng và nhân viên kiểm duyệt ưu tiên xem đánh giá tiêu cực.
- **Tiền điều kiện:** Có đánh giá mới (`UC-SOC-03`) với `comment` không rỗng; AI Worker đã nạp mô hình PhoBERT (ONNX).
- **Hậu điều kiện:** `product_reviews.aspectSentimentJson` lưu kết quả `{"overall": "POSITIVE" | "NEUTRAL" | "NEGATIVE", "score": 0.0–1.0, "model": "<tên + phiên bản>", "analyzedAt": "<ISO-8601>"}`; bộ đếm cảm xúc theo shop (cache Redis `sentiment:vendor:{id}`, 10 phút) được làm mới.
- **Luồng sự kiện chính:**
  1. Sau khi lưu đánh giá, backend phát `ReviewCreatedEvent`; `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` gọi AI Worker `POST /sentiment` (token nội bộ) với `reviewId` và nội dung đánh giá — **không** gửi tên, email, số điện thoại của khách.
  2. AI Worker tiền xử lý đúng như lúc huấn luyện mô hình: chuẩn hóa Unicode (NFC), viết thường, chuẩn hóa teencode / từ viết tắt phổ biến ("ko" → "không", "sp" → "sản phẩm"…), bỏ URL, tách từ tiếng Việt (RDRSegmenter / VnCoreNLP — PhoBERT yêu cầu đầu vào đã tách từ), cắt tối đa 256 token.
  3. Chạy mô hình PhoBERT phân loại 3 nhãn (xuất ONNX, chạy ONNX Runtime trên CPU); softmax → nhãn có xác suất cao nhất làm `overall`, xác suất đó làm `score`.
  4. AI Worker gọi lại `PATCH /api/v1/internal/reviews/{id}/sentiment`; backend gọi `ProductReview.attachSentiment(json)`, ghi kèm tên + phiên bản mô hình.
  5. **Seller Center → Phân tích bán hàng** (`UC-VND-08`): tỉ lệ đánh giá Tích cực / Trung tính / Tiêu cực trong kỳ, xu hướng theo tuần và 5 sản phẩm có nhiều đánh giá tiêu cực nhất (chỉ tính kết quả có `score ≥ 0,6`).
  6. **Admin → Kiểm duyệt đánh giá** (`UC-SOC-05`): đánh giá có nhãn `NEGATIVE` với `score ≥ 0,85` được ưu tiên lên đầu hàng chờ (sau các đánh giá có từ 3 báo cáo), hiển thị nhãn "AI: Tiêu cực" kèm độ tin cậy.
- **Luồng thay thế / Ngoại lệ:**
  - *Đánh giá chỉ có số sao, không có chữ:* không gọi mô hình, `aspectSentimentJson` giữ `NULL`; thống kê dùng số sao.
  - *AI Worker lỗi / quá 10 giây:* thử lại 3 lần (2 / 8 / 30 giây); vẫn lỗi thì để `NULL`, Cron 15 phút quét lại các đánh giá `NULL` có chữ được tạo trong 24 giờ qua.
  - *`score < 0,6` (mô hình không chắc chắn):* vẫn lưu kết quả nhưng không tính vào thống kê và không đổi thứ tự hàng chờ.
  - *Đổi mô hình mới:* chạy lại theo lô cho các đánh giá có `model` cũ (job chạy tay của Admin / kỹ thuật), không chặn luồng đánh giá mới.
- **Quan hệ Use Case:** Không có «include» / «extend» — được kích hoạt bởi sự kiện hệ thống sau `UC-SOC-03`; kết quả được dùng ở `UC-VND-08` và `UC-SOC-05`.
- **Ràng buộc nghiệp vụ & Kỹ thuật:**
  - Kết quả AI chỉ mang tính tham khảo: **không tự ẩn** đánh giá, không ảnh hưởng số sao hay điểm shop; quyết định ẩn luôn do nhân viên (`UC-SOC-05`).
  - TLCN không hiển thị nhãn cảm xúc cho khách ở trang sản phẩm.
  - Mô hình tự host trong AI Worker (không gọi API trả phí, không đưa nội dung đánh giá ra ngoài); nạp một lần khi khởi động; thời gian suy luận và độ chính xác (macro-F1) đo lại trên tập kiểm thử của nhóm khi triển khai.
  - Chỉ xử lý tiếng Việt; câu chủ yếu tiếng Anh vẫn chạy nhưng kết quả thường có `score` thấp.
  - Phân tích theo khía cạnh (chất lượng, giao hàng, đóng gói, phục vụ) thuộc DATN — dùng chung cột `aspectSentimentJson`, chỉ thêm khóa, không đổi cấu trúc khóa `overall`.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai — AI Worker mới có khung (`server-python/app/sentiment` còn TODO); nhóm đã có mô hình PhoBERT phân loại cảm xúc; cần đóng gói ONNX vào AI Worker và xây API.
  - *Kế hoạch:* Sprint 6 (tuần 11–12) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Đối chiếu DATN:* cùng mã `UC-AI-07` ("Phân tích cảm xúc đánh giá theo khía cạnh bằng PhoBERT", phân hệ 13 của DATN); TLCN làm phần cảm xúc tổng thể, xếp vào phân hệ 08.
  - *Thực thể CSDL:* [ProductReview](../ClassDiagram/ENTITY.md#91-productreview--đánh-giá-sản-phẩm) (`aspectSentimentJson`, `attachSentiment()`).

---

<a id="ph-sup"></a>
## PHÂN HỆ 09: TRẢ HÀNG, HOÀN TIỀN & KHIẾU NẠI (RETURNS, REFUNDS & DISPUTES)

> **Phạm vi:** Yêu cầu trả hàng / hoàn tiền, shop xử lý, nhân viên phân xử, thực hiện hoàn tiền (VNPay tự động; VietQR / COD chuyển khoản tay có SePay xác nhận).
> **Use case (4):** `UC-SUP-01`, `UC-SUP-02`, `UC-SUP-03`, `UC-SUP-04`

### UC-SUP-01: Tạo yêu cầu Trả hàng / Hoàn tiền (Return & Refund Request)
- **Mã Use Case:** `UC-SUP-01`
- **Tác nhân chính:** `ACT-CUS` Customer (khách hàng)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Cho phép người mua yêu cầu trả hàng và lấy lại tiền khi hàng nhận được bị bể vỡ, sai mẫu mã, hư hỏng hoặc không nhận được hàng; tự động đóng băng dòng tiền ký quỹ để bảo vệ người mua.
- **Tiền điều kiện:** Đơn hàng ở trạng thái `DELIVERED` (hoặc `PAID` với COD đã đối soát, hoặc `PARTIALLY_REFUNDED` khi trả tiếp món khác), trong vòng 7 ngày kể từ khi giao hàng; chưa quá thời hạn bảo vệ khiếu nại.
- **Hậu điều kiện:** Tạo `refund_requests`, `refund_request_items`, `refund_evidences`; chuyển `orders.status = 'RETURN_REQUESTED'`; tạm dừng giải ngân ký quỹ trong `escrow_ledgers`.
- **Luồng sự kiện chính:**
  1. Khách hàng vào chi tiết đơn hàng đã giao, bấm "Yêu cầu Trả hàng / Hoàn tiền".
  2. Chọn phương án mong muốn (lưu vào `refund_requests.requestType`):
     - "Trả hàng và Hoàn tiền" (`RETURN_AND_REFUND`): Trả lại hàng vật lý cho shop và nhận lại tiền của các món trả.
     - "Hoàn tiền ngay không cần trả hàng" (`REFUND_ONLY`): Áp dụng khi hàng bị thiếu hoặc hư hỏng một phần (thỏa thuận giảm giá).
  3. Chọn các sản phẩm bị lỗi trong đơn hàng và số lượng cần trả.
  4. Chọn Lý do hoàn tiền từ danh mục: Hàng bể vỡ do vận chuyển, Hàng sai kích thước/màu sắc, Hàng giả/nhái, Hàng thiếu phụ kiện, Không nhận được hàng.
  5. Tải lên **Bằng chứng xác thực (Evidences)**: Bắt buộc tối thiểu 1 ảnh chụp rõ kiện hàng có nhãn vận đơn và 1 video mở hộp (Unboxing video) quay liền mạch không cắt ghép.
  6. Khách hàng bấm "Gửi yêu cầu".
  7. Backend Spring Boot mở transaction:
     - Tạo bản ghi `refund_requests`: `orderId`, `requestedByUserId`, `refundAmount`, `requestType`, `status = 'REQUESTED'`, `requestedAt = now()`, `shopResponseDueAt = now() + 48h`.
     - Tạo các bản ghi chi tiết `refund_request_items` kèm số tiền hoàn cho từng món (lấy đúng số tiền sau chiết khấu prorate đã lưu lúc checkout).
     - Lưu các link ảnh/video bằng chứng vào bảng `refund_evidences`.
     - Gọi `EscrowLedger.dispute()`: đóng băng bản ghi `escrow_ledgers` tương ứng (`status = 'DISPUTED'`), dừng toàn bộ tiến trình giải ngân tự động.
     - Cập nhật đơn hàng cha `orders.status = 'RETURN_REQUESTED'`.
  8. Hệ thống gửi thông báo khẩn cấp cho Người bán kèm hạn chót phản hồi là 48 giờ.
- **Luồng thay thế / Ngoại lệ:**
  - *Quá thời hạn 7 ngày kể từ khi Delivered:* Nút "Trả hàng / Hoàn tiền" bị ẩn, hệ thống hiển thị thông báo: "Đơn hàng đã quá thời hạn 7 ngày bảo vệ khiếu nại".
  - *Shop yêu cầu bổ sung bằng chứng (`AWAITING_EVIDENCE`, UC-SUP-02):* khách nhận thông báo kèm nội dung `evidenceRequestNote`, tải thêm ảnh/video (`refund_evidences.submission_round = 2`) trong 48 giờ → `RefundRequest.submitAdditionalEvidence()` đưa yêu cầu về `REQUESTED` và shop có lại đủ 48 giờ (`shopResponseDueAt`). Quá 48 giờ không nộp → Cron `closeIfEvidenceNotProvided()` đóng yêu cầu (`REJECTED`). Khách cũng có thể bấm "Khiếu nại lên Sàn" nếu cho rằng yêu cầu bổ sung là vô lý (→ `DISPUTED`).
- **Quan hệ Use Case:** `<<include>>` Tải tệp lên Silo (Presigned URL) — tác nhân phụ: `ACT-MIN` Silo S3 Storage; `<<extend>>` Khách hàng xác nhận "Đã nhận hàng thành công" (`UC-ORD-11`) tại điểm mở rộng «Báo hàng có vấn đề» khi {hàng lỗi / thiếu / sai mô tả, còn trong hạn 7 ngày}.
- **Ràng buộc nghiệp vụ & Kỹ thuật:** Số tiền hoàn không bao giờ vượt quá số tiền thực tế khách đã trả cho các sản phẩm đó (`item_refund_amount <= prorated_price * quantity`).
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 5 (tuần 9–10) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [RefundRequest](../ClassDiagram/ENTITY.md#81-refundrequest--yêu-cầu-hoàn-tiền), [RefundRequestItem](../ClassDiagram/ENTITY.md#82-refundrequestitem--dòng-sản-phẩm-muốn-hoàn), [RefundEvidence](../ClassDiagram/ENTITY.md#83-refundevidence--bằng-chứng-khiếu-nại).

---

### UC-SUP-02: Người bán tiếp nhận và xử lý yêu cầu trả hàng
- **Mã Use Case:** `UC-SUP-02`
- **Tác nhân chính:** `ACT-VND` Vendor (người bán/shop)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Cho phép người bán xem xét lý do và bằng chứng của khách hàng để đưa ra hướng xử lý: Đồng ý hoàn tiền ngay (không thu hồi hàng), Đồng ý cho trả hàng (Reverse Logistics), Yêu cầu bổ sung bằng chứng, hoặc Từ chối.
- **Tiền điều kiện:** Có yêu cầu hoàn tiền đang chờ người bán phản hồi (`status == 'REQUESTED'` (chờ Shop duyệt)).
- **Hậu điều kiện:** Cập nhật `refund_requests` thành `APPROVED` (hoàn ngay, `resolutionMode = APPROVE_REFUND_ONLY`), `AWAITING_RETURN_SHIPMENT` (chờ hàng hoàn, `resolutionMode = APPROVE_WITH_RETURN`, sinh vận đơn chiều về), `AWAITING_EVIDENCE`, `SHOP_REJECTED` hoặc chuyển sang Sàn phân xử `DISPUTED`.
- **Luồng sự kiện chính:**
  1. Người bán nhận thông báo, mở chi tiết yêu cầu Trả hàng / Hoàn tiền.
  2. Xem video mở hộp và hình ảnh khách tải lên.
  3. Người bán chọn 1 trong 4 hướng giải quyết trong vòng 48 giờ (tính đến `shopResponseDueAt`):
     - **Phương án 0 - Yêu cầu bổ sung bằng chứng (tối đa 1 lần):** Bấm "Yêu cầu bổ sung" và ghi rõ cần gì → `RefundRequest.requestMoreEvidence(note)` → `AWAITING_EVIDENCE`; đồng hồ của shop tạm dừng, khách có 48 giờ để nộp (UC-SUP-01).
     - **Phương án 1 - Đồng ý hoàn tiền ngay, không thu hồi hàng (Chấp nhận lỗi thuộc về shop):** Bấm "Đồng ý hoàn tiền" → `approveByShop(APPROVE_REFUND_ONLY, …)` → `APPROVED`. Hệ thống kích hoạt `UC-SUP-04` hoàn tiền tức thì. Áp dụng cho mọi `requestType` (với `RETURN_AND_REFUND`, shop chủ động miễn gửi trả các món giá trị thấp).
     - **Phương án 2 - Đồng ý cho trả hàng (chỉ khi khách chọn `RETURN_AND_REFUND`):** Bấm "Đồng ý nhận lại hàng" → `approveByShop(APPROVE_WITH_RETURN, …)` → `AWAITING_RETURN_SHIPMENT`, `returnDueAt = now + 7 ngày`. Hệ thống sinh vận đơn chiều về `ShipmentTracking.forReturn(orderId, refundRequestId, carrier)` (`direction = RETURN`, tạo vận đơn chiều về trên GHN như `UC-LOG-02`), thông báo cho khách đóng gói gửi lại hàng về kho shop:
       + ĐVVC lấy hàng (vận đơn RETURN `PICKED_UP`) → `markReturnShipped()`; hàng về kho shop (`DELIVERED`) → `confirmReturnReceived()` → `APPROVED` → `UC-SUP-04` hoàn tiền.
       + Quá 7 ngày khách chưa giao hàng cho ĐVVC → Cron `failReturnIfOverdue()` → `RETURN_FAILED`: không hoàn tiền, đơn `RETURN_APPROVED` → `COMPLETED`, `EscrowLedger.releaseDisputeHold()` → `HELD` và giải ngân bình thường (tiền shop không bị khóa vô thời hạn).
     - **Phương án 3 - Từ chối khiếu nại (Shop từ chối giải quyết):**
       + *Trường hợp 3a (Shop từ chối bình thường):* Cập nhật `refund_requests.status = 'SHOP_REJECTED'`. Khách hàng có 48 giờ (tính từ `shop_rejected_at`) để chấp nhận hoặc bấm "Khiếu nại lên Sàn". Nếu quá 48h khách không phản hồi, Cron `RefundDeadlineJob` gọi `RefundRequest.closeIfCustomerSilent()` → `status = 'REJECTED'` (đóng vĩnh viễn, không hoàn tiền), đơn hàng `RETURN_REJECTED` → `COMPLETED`, ký quỹ `releaseDisputeHold()`.
       + *Trường hợp 3b (Khách leo thang hoặc Shop chủ động nhờ Sàn can thiệp):* Chuyển trạng thái `refund_requests.status = 'DISPUTED'` để Chuyên viên sàn tiến hành điều tra độc lập (`UC-SUP-03`). Sau khi phân xử, nếu Sàn bác bỏ yêu cầu của khách thì cập nhật `status = 'REJECTED'` (đóng tranh chấp, giải phóng Escrow cho Shop).
- **Luồng thay thế / Ngoại lệ:**
  - *Quá 48 giờ người bán không bấm phản hồi:* Hệ thống coi như người bán mặc nhiên đồng ý theo đúng phương án khách đã chọn (Auto-Approval Policy): Cron `RefundDeadlineJob` (15 phút/lần) gọi `RefundRequest.autoApproveIfShopSilent()` → `APPROVED` (khách chọn `REFUND_ONLY`, kích hoạt hoàn tiền `UC-SUP-04` ngay) hoặc `AWAITING_RETURN_SHIPMENT` (khách chọn `RETURN_AND_REFUND`, sinh vận đơn chiều về như Phương án 2); đơn hàng `RETURN_APPROVED`. Bảng đồng bộ trạng thái `RefundRequest` ↔ `Order` ↔ `EscrowLedger`: [ENTITY.md mục 8.1](../ClassDiagram/ENTITY.md#81-refundrequest--yêu-cầu-hoàn-tiền).
- **Quan hệ Use Case:** được mở rộng bởi `UC-SUP-03` (điểm «Phản hồi của shop»), `UC-SUP-04` (điểm «Phản hồi của shop»).
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 5 (tuần 9–10) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [RefundRequest](../ClassDiagram/ENTITY.md#81-refundrequest--yêu-cầu-hoàn-tiền) (`requestType`, `resolutionMode`, `returnDueAt`, `returnShippedAt`, `returnReceivedAt`), [ShipmentTracking](../ClassDiagram/ENTITY.md#71-shipmenttracking--vận-đơn) (`direction = RETURN`).

---

### UC-SUP-03: Nhân viên sàn phân xử tranh chấp khiếu nại (Dispute Arbitration)
- **Mã Use Case:** `UC-SUP-03`
- **Tác nhân chính:** `ACT-CS` CS Specialist (Nhân viên CSKH); `ACT-ADM` Admin
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Trọng tài công tâm độc lập của sàn; xem xét chứng cứ của cả hai bên (Người mua vs Người bán) để đưa ra phán quyết cuối cùng có giá trị cưỡng chế thi hành về mặt tài chính.
- **Tiền điều kiện:** Yêu cầu hoàn tiền ở trạng thái tranh chấp `DISPUTED` — do khách bấm "Khiếu nại lên Sàn" trong 48 giờ sau khi Shop từ chối (`SHOP_REJECTED`), hoặc do Shop chủ động nhờ Sàn can thiệp. *Shop im lặng quá 48 giờ **không** tạo tranh chấp mà được tự động chấp thuận hoàn tiền (Auto-Approval, UC-SUP-02).*
- **Hậu điều kiện:** Phán quyết được lưu trữ; thực hiện giải ngân cho shop hoặc hoàn tiền cho khách; ghi log kiểm toán.
- **Luồng sự kiện chính:**
  1. Chuyên viên CSKH mở màn hình "Phân xử tranh chấp đơn hàng" trên Admin Portal, xem xét chứng cứ và ra quyết định: `approve(staffId, resolution, note)` → `APPROVED` (hoàn ngay, `APPROVE_REFUND_ONLY`) hoặc `AWAITING_RETURN_SHIPMENT` (buộc gửi trả hàng, `APPROVE_WITH_RETURN`); hoặc `reject()` → `REJECTED`. Khi hoàn tiền thành công, chuyển sang `REFUNDED`.
  2. Rà soát độc lập:
     - Xem video unboxing của khách hàng (kiểm tra góc quay, tình trạng băng keo niêm phong).
     - Xem video/ảnh đóng gói tại kho do shop cung cấp trước khi bàn giao cho GHN.
     - Tra cứu biên bản làm việc với đơn vị vận chuyển (nếu nghi ngờ hàng bị vỡ trong quá trình GHN vận chuyển — sàn khiếu nại GHN theo chính sách bồi thường của hãng).
  3. Đưa ra Phán quyết Trọng tài (Arbitration Decision):
     - **Trường hợp A - Người mua đúng (Hàng lỗi từ người bán):** Phán quyết hoàn tiền 100% cho người mua. Tiền ký quỹ bị hủy/hoàn trả (`REFUNDED`); số khiếu nại thua của shop hiển thị trong báo cáo shop (`UC-VND-08`).
       + *Hàng giả / hàng nhái được xác minh (lý do "Hàng giả/nhái"):* vẫn hoàn **100%** giá trị phần hàng khiếu nại — không bồi thường vượt giá trị đơn, giữ đúng "khách + shop + sàn = tiền đơn"; đồng thời khóa sản phẩm (`UC-CAT-11`), ghi cảnh báo shop; shop có nhãn Chính hãng (`isMall`) có thể bị gỡ nhãn khi Moderator / Admin xét lại hồ sơ.
     - **Trường hợp B - Người bán đúng (Khách cố tình đánh tráo hoặc làm hỏng):** Phán quyết bác bỏ yêu cầu hoàn tiền. Mở khóa giải ngân số tiền ký quỹ vào ví người bán, gửi thông báo cảnh cáo tài khoản khách hàng.
     - **Trường hợp C - Lỗi thuộc về Đơn vị vận chuyển (Làm mất/vỡ trên đường):** Bảo hiểm vận chuyển bồi thường: Hoàn tiền cho khách hàng và đền bù tiền hàng cho người bán theo giá trị khai giá.
  4. Nhập biên bản kết luận phân xử vào hệ thống và bấm "Thực thi phán quyết".
- **Quan hệ Use Case:** `<<extend>>` Người bán tiếp nhận và xử lý yêu cầu trả hàng (`UC-SUP-02`) tại điểm mở rộng «Phản hồi của shop» khi {shop từ chối và khách khiếu nại lên sàn trong 48 giờ}; được mở rộng bởi `UC-SUP-04` (điểm «Phán quyết»).
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 5 (tuần 9–10) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [RefundRequest](../ClassDiagram/ENTITY.md#81-refundrequest--yêu-cầu-hoàn-tiền) (`resolutionNote`, `resolvedByStaffId`, `resolvedAt`), [EscrowLedger](../ClassDiagram/ENTITY.md#63-escrowledger--sổ-ký-quỹ-tiền-tạm-giữ).

---

### UC-SUP-04: Thực hiện hoàn tiền (VNPay / VietQR / COD)
- **Mã Use Case:** `UC-SUP-04`
- **Tác nhân chính:** `ACT-CS` CS Specialist (Nhân viên CSKH)
- **Tác nhân phụ:** `ACT-PGW` Payment Gateway (VNPay, SePay)
- **Mục đích nghiệp vụ:** Hoàn tiền chính xác về cho người mua sau khi yêu cầu hoàn tiền được chấp thuận (shop đồng ý `UC-SUP-02` hoặc sàn phân xử `UC-SUP-03`), hoặc khi đơn đã thanh toán bị hủy (`UC-ORD-10`) — hỗ trợ đơn VNPay, chuyển khoản VietQR và COD.
- **Tiền điều kiện:** Có `RefundRequest` ở `APPROVED`: do shop / sàn chấp thuận (hàng hoàn đã về kho nếu cần trả hàng), hoặc do hệ thống tự tạo (`REFUND_ONLY`) khi đơn đã thanh toán bị hủy (`UC-ORD-10`) hay bị GHN hoàn hàng (`UC-LOG-03`).
- **Hậu điều kiện:** Tiền hoàn về người mua; `RefundRequest` → `REFUNDED`; `EscrowLedger` → `REFUNDED` (hoàn toàn phần) hoặc giảm `netAmount` (hoàn một phần); hoàn kho nếu có trả hàng.
- **Luồng sự kiện chính:**
  1. Hệ thống đọc `Order.paymentMethod` và `EscrowLedger.fundingSource`:
     - **VNPay (`ONLINE_GATEWAY`):** tạo `PaymentRefund` (`PENDING`), gọi API refund VNPay (`vnp_Command = refund`, toàn phần `02` / một phần `03`) với `vnp_TxnRef` gốc; thành công → `PaymentRefund.markSuccess()`, `PaymentTransaction.recordRefund(amount)`.
     - **Chuyển khoản VietQR (SePay) và COD (`COD_CASH`):** không có API hoàn qua cổng. Staff chuyển khoản từ tài khoản sàn tới số tài khoản người mua cung cấp (nhập trong form yêu cầu hoàn tiền / chat) với nội dung bắt buộc `TRRF{refundRequestId}`; SePay báo **tiền ra** khớp mã và số tiền → hệ thống tự ghi `referenceCode` vào `RefundRequest.resolutionNote` và hoàn tất (không khớp sau 24 giờ → Staff xác nhận tay). Với COD nếu GHN **chưa** chuyển tiền thu hộ (`PENDING_FUNDING`), sàn vẫn hoàn cho khách và trừ vào lần đối soát COD kế tiếp.
  2. Mở transaction:
     - `RefundRequest.markRefunded()`; `Order.markRefunded(refundAmount)` → `REFUNDED` hoặc `PARTIALLY_REFUNDED`.
     - **Hoàn toàn phần:** `EscrowLedger.markRefunded()` → `REFUNDED`.
     - **Hoàn một phần:** `EscrowLedger.applyPartialRefund(refundGross, refundCommission)` giảm `grossAmount` và hoa hồng đúng các dòng bị trả, tính lại `netAmount`; rồi `releaseDisputeHold()` → `HELD` để Cron giải ngân phần còn lại cho shop.
     - Nếu ký quỹ đã giải ngân trước đó (hiếm — khiếu nại muộn được sàn chấp nhận): `VendorWallet.debit()` thu hồi từ ví shop, ghi `WalletTransaction` `DEBIT_REFUND`.
     - Nhập lại tồn kho cho số lượng hàng đã trả về (`RefundRequestItem`).
     - Ghi `system_logs` `REFUND_PROCESSED`.
  3. Gửi thông báo in-app + email hoàn tiền thành công cho khách (`UC-NOT-01`, `UC-NOT-03`).
  4. Màn **"Trạng thái hoàn tiền Gateway"**: VNPay — mã GD gốc, mã hoàn tiền, "Đồng bộ trạng thái", "Thử lại hoàn tiền" khi lỗi; VietQR & COD — trạng thái **"Chờ chuyển khoản thủ công"**, nhân viên chuyển khoản rồi bấm "Đã chuyển khoản" hoặc webhook SePay (nội dung `TRRF{id}`) tự xác nhận.
  5. Hoàn Điểm đã dùng cho các dòng được hoàn (`ORDER_REFUND`); ghi sổ cái `REFUND` (UC-PAY-06).
- **Luồng thay thế / Ngoại lệ:**
  - *1a. VNPay từ chối / lỗi hoàn tiền:* `PaymentRefund.markFailed()`, thông báo Staff; Staff thử lại hoặc chuyển khoản tay như đơn COD. Không giải ngân cho shop khi chưa hoàn xong.
  - *1b. Số tiền hoàn ≥ `grossAmount`:* xử lý như hoàn toàn phần.
- **Quan hệ Use Case:** `<<include>>` Ghi sổ cái tài chính kép bất biến (Financial Ledger) (`UC-PAY-06`); `<<extend>>` Người bán tiếp nhận và xử lý yêu cầu trả hàng (`UC-SUP-02`) tại điểm mở rộng «Phản hồi của shop» khi {shop chấp thuận hoặc im lặng quá 48 giờ}; `<<extend>>` Nhân viên sàn phân xử tranh chấp khiếu nại (Dispute Arbitration) (`UC-SUP-03`) tại điểm mở rộng «Phán quyết» khi {phán quyết có lợi cho người mua}.
- **Ràng buộc nghiệp vụ & Kỹ thuật:** Tổng hoàn không vượt `PaymentTransaction.refundableAmount()` (VNPay) hoặc `Order.grandTotal` (COD); khách + shop + sàn luôn bằng đúng số tiền gốc của đơn (ví dụ số ở ENTITY.md mục 8.1).
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 5 (tuần 9–10) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [RefundRequest](../ClassDiagram/ENTITY.md#81-refundrequest--yêu-cầu-hoàn-tiền), [PaymentRefund](../ClassDiagram/ENTITY.md#62-paymentrefund--giao-dịch-hoàn-tiền-qua-cổng), [PaymentTransaction](../ClassDiagram/ENTITY.md#61-paymenttransaction--giao-dịch-thanh-toán), [EscrowLedger](../ClassDiagram/ENTITY.md#63-escrowledger--sổ-ký-quỹ-tiền-tạm-giữ), [VendorWallet](../ClassDiagram/ENTITY.md#64-vendorwallet--ví-tiền-của-người-bán).

---

<a id="ph-not"></a>
## PHÂN HỆ 10: THÔNG BÁO (NOTIFICATIONS)

> **Phạm vi:** Thông báo in-app realtime (WebSocket), email giao dịch, thông báo toàn sàn.
> **Use case (3):** `UC-NOT-01`, `UC-NOT-03`, `UC-NOT-05`

### UC-NOT-01: Thông báo trong ứng dụng thời gian thực (WebSocket)
- **Mã Use Case:** `UC-NOT-01`
- **Tác nhân chính:** `ACT-CUS` Customer (khách hàng); `ACT-VND` Vendor (người bán/shop); `ACT-STF` Staff (Nhân viên sàn)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Bắn thông báo pop-up tức thời trên màn hình ứng dụng khi có biến động về đơn hàng (Đơn đã thanh toán, Đơn đang giao, Tiền đã về ví, Tin nhắn mới) mà không cần tải lại trang.
- **Tiền điều kiện:** Người dùng đang mở trang web hoặc ứng dụng và có kết nối WebSocket `/ws` đang hoạt động.
- **Hậu điều kiện:** Tạo bản ghi trong `notifications`; phát gói tin qua WebSocket; tăng số đếm chuông thông báo trên header.
- **Luồng sự kiện chính:**
  1. Khi một sự kiện nghiệp vụ xảy ra (ví dụ: `OrderPaid`, `ShipmentDelivering`, `WalletCredited`):
  2. Notification Service tạo bản ghi trong bảng `notifications`: `userId`, `title`, `body`, `type = 'ORDER_STATUS_CHANGED'`, `referenceType = 'ORDER'`, `referenceId`, `channel = 'IN_APP'`, `isRead = false`.
  3. Server bắn sự kiện qua Redis Pub/Sub kênh `notification_events:{userId}`.
  4. WebSocket Hub bắt được message, đẩy ngay lập tức qua kết nối TCP của user.
  5. Trình duyệt người dùng hiển thị thông báo Toast góc phải màn hình kèm âm thanh "Ting", chuông thông báo trên header tăng thêm 1 đơn vị.
  6. Trung tâm thông báo: tab **Đơn hàng / Khuyến mãi / Hệ thống / Chat**, "Đánh dấu tất cả đã đọc", modal "Chi tiết thông báo"; người nhận gồm khách, shop, nhân viên / Admin (chuông Admin Console).
  7. Sự kiện WebSocket: `NEW_NOTIFICATION`, `PLATFORM_NOTIFICATION` (UC-NOT-05), `CHAT_MESSAGE`, `CHAT_READ_RECEIPT`, `VENDOR_PRODUCT_UPDATE` (kết quả duyệt sản phẩm); badge số đơn / chat / thông báo đồng bộ giữa nhiều tab trình duyệt (BroadcastChannel: `METRICS_SYNC`, `NOTIFICATION_SYNC`, `READ_ONE`, `READ_ALL`).
  8. Loại cảnh báo bảo mật `NEW_DEVICE_LOGIN` (UC-AUTH-02).
  9. **Chuông Admin Console** dùng bảng riêng `admin_notifications` (hộp thư chung của đội vận hành, không gắn `userId`): `NEW_ORDER` khi đơn thanh toán thành công (UC-PAY-03), `USER_APPROVAL` khi shop nộp hồ sơ KYC (UC-VND-02), `WITHDRAW_REQUEST` khi shop yêu cầu rút tiền (UC-ADM-04), `ESCROW_DISCREPANCY` khi lệch số dư ví / sổ cái, `SYSTEM_ALERT`; mỗi bản ghi có `referenceUrl` tới trang xử lý; nhân viên mở → `AdminNotification.markRead()`; badge = số chưa đọc (`GET /admin/notifications/unread-count`).
- **Quan hệ Use Case:** Không có.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 5 (tuần 9–10) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [Notification](../ClassDiagram/ENTITY.md#101-notification--thông-báo-cá-nhân), [AdminNotification](../ClassDiagram/ENTITY.md#105-adminnotification--thông-báo-nội-bộ-cho-quản-trị-viên).

---

### UC-NOT-03: Gửi email giao dịch tự động
- **Mã Use Case:** `UC-NOT-03`
- **Tác nhân chính:** `ACT-CUS` Customer (khách hàng); `ACT-VND` Vendor (người bán/shop)
- **Tác nhân phụ:** `ACT-EML` Email Service
- **Mục đích nghiệp vụ:** Gửi hóa đơn điện tử, vé xác nhận đơn hàng, mã OTP bảo mật và thông báo pháp lý bắt buộc qua email với template HTML chuẩn responsive.
- **Tiền điều kiện:** Người dùng có địa chỉ email hợp lệ.
- **Hậu điều kiện:** Email được gửi thành công; ghi log gửi email.
- **Luồng sự kiện chính:**
  1. Khi phát sinh sự kiện cần gửi mail (Đăng ký OTP, Đổi mật khẩu, Xác nhận thanh toán đơn hàng thành công, Thông báo kết quả KYC):
  2. Notification Service gửi mail bất đồng bộ bằng `@Async` (thread pool riêng 4 luồng) qua Spring `JavaMailSender`; lỗi thì `Notification.markFailed()` và `@Scheduled` thử lại tối đa 3 lần.
  3. Worker nạp Template HTML tương ứng, điền các biến dữ liệu cá nhân hóa (Họ tên, Mã đơn, Bảng chi tiết sản phẩm, Tổng tiền).
  4. Gửi email qua giao thức SMTP (hiện tại) hoặc API AWS SES / SendGrid (kế hoạch tương lai).
  5. Xử lý kết quả: Nếu lỗi mạng, worker thử lại (Retry) với thuật toán Exponential Backoff tối đa 3 lần.
- **Quan hệ Use Case:** Không có.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai — xử lý phía máy chủ (cron / webhook / AI), chưa có backend; giao diện chỉ hiển thị kết quả trên lớp mock.
  - *Kế hoạch:* Sprint 5 (tuần 9–10) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [SystemLog](../ClassDiagram/ENTITY.md#121-systemlog--nhật-ký-kiểm-toán).

---

### UC-NOT-05: Admin phát thông báo toàn sàn (Broadcast Platform Notification)
- **Mã Use Case:** `UC-NOT-05`
- **Tác nhân chính:** `ACT-ADM` Admin
- **Tác nhân phụ:** `ACT-MIN` Silo S3 Storage
- **Mục đích nghiệp vụ:** Gửi thông điệp tới toàn bộ hoặc một nhóm người dùng (mục "Thông báo sàn" của Admin) — khách / shop thấy ở chuông thông báo, tab "Hệ thống".
- **Tiền điều kiện:** Admin, hoặc nhân viên được cấp `MANAGE_PLATFORM_NOTIFICATION`.
- **Hậu điều kiện:** Một dòng `platform_notifications` (fan-out on read); trạng thái đã đọc ghi khi người dùng mở.
- **Luồng sự kiện chính:**
  1. Admin nhập tiêu đề ("Siêu sale 9.9 giảm tới 50%…"), nội dung (rich-text, chèn ảnh / liên kết), ảnh banner (Silo), đối tượng (`ALL`, `CUSTOMER`, `VENDOR`, `SPECIFIC_USERS`).
  2. Bấm "Phát thông báo" → lưu `platform_notifications`, chốt `reachCount` = số người thuộc đối tượng.
  3. Phát sự kiện `PLATFORM_NOTIFICATION` qua WebSocket tới người dùng đang trực tuyến thuộc đối tượng.
  4. Người dùng mở thông báo → ghi `user_platform_notification_reads` (mỗi người 1 lần).
  5. Admin xem thống kê: số đã gửi, đã đọc, tỉ lệ đọc (`readCount / reachCount`).
- **Luồng thay thế / Ngoại lệ:**
  - *Hẹn giờ phát:* lưu nháp kèm thời điểm, Cron phát đúng giờ.
- **Quan hệ Use Case:** `<<include>>` Tải tệp lên Silo (Presigned URL) — tác nhân phụ: `ACT-MIN` Silo S3 Storage.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 8 (tuần 15–16) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [PlatformNotification](../ClassDiagram/ENTITY.md#102-platformnotification--thông-báo-phát-toàn-sàn), [UserPlatformNotificationRead](../ClassDiagram/ENTITY.md#103-userplatformnotificationread--đánh-dấu-đã-đọc-thông-báo-sàn).

---

<a id="ph-adm"></a>
## PHÂN HỆ 11: QUẢN TRỊ SÀN, NHÂN VIÊN & RÚT TIỀN (PLATFORM ADMIN, STAFF & PAYOUTS)

> **Phạm vi:** Nhân viên & phân quyền RBAC (3 chức danh + Admin), khóa / mở người dùng, rút tiền & duyệt chi, dashboard tài chính & sổ cái, nhật ký hệ thống, banner trang chủ.
> **Use case (8):** `UC-ADM-01`, `UC-ADM-02`, `UC-ADM-03`, `UC-ADM-04`, `UC-ADM-05`, `UC-ADM-06`, `UC-ADM-07`, `UC-ADM-12`

### UC-ADM-01: Quản lý danh sách nhân viên nội bộ & Lời mời kích hoạt
- **Mã Use Case:** `UC-ADM-01`
- **Tác nhân chính:** `ACT-ADM` Admin
- **Tác nhân phụ:** `ACT-EML` Email Service
- **Mục đích nghiệp vụ:** Tạo tài khoản nhân viên vận hành sàn với 1 trong 3 chức danh, gửi lời mời kích hoạt an toàn; khóa / mở, xóa nhân viên (mục "Phân quyền nhân viên").
- **Tiền điều kiện:** Admin đăng nhập (quyền `MANAGE_RBAC`). Tài khoản Admin đầu tiên được tạo bằng seed khi cài đặt.
- **Hậu điều kiện:** `users` (`role = STAFF`, `status = UNVERIFIED`), `admin_profiles` (`staffTitle`), `staff_permissions` (bộ quyền mặc định) được tạo; email mời đã gửi.
- **Luồng sự kiện chính:**
  1. Admin bấm "Mời nhân viên mới", nhập họ tên, email công việc (`@genie.vn`), chọn chức danh: Nhân viên kiểm duyệt (`MODERATOR`), CSKH (`CS_SPECIALIST`), Tài chính (`FINANCE_OFFICER`) hoặc "Tùy chỉnh".
  2. Hệ thống kiểm tra email chưa tồn tại (`/admin/staffs/check-email`); nạp sẵn bộ quyền mặc định `StaffTitle.defaultPermissions()` để Admin bỏ / thêm trước khi gửi.
  3. Bấm "Gửi lời mời": trong một transaction tạo `users` (`UNVERIFIED`, chưa có mật khẩu), `admin_profiles`, các dòng `staff_permissions` (`grantedByAdminId`); ghi `system_logs` `STAFF_INVITED`.
  4. Gửi email mời chứa liên kết kích hoạt dùng một lần (hết hạn 48 giờ).
  5. Nhân viên mở liên kết, xác thực OTP email, tự đặt mật khẩu → `status = ACTIVE` (trạng thái "Chờ kích hoạt" → "Hoạt động").
  6. Danh sách nhân viên: tìm theo tên / email, xem vai trò, số quyền, trạng thái; thao tác "Sửa quyền" (UC-ADM-02), "Khóa / Mở khóa" (UC-ADM-03), "Xóa tài khoản nhân viên".
- **Luồng thay thế / Ngoại lệ:**
  - *Email đã tồn tại:* báo "Email đã được sử dụng".
  - *Liên kết hết hạn:* Admin bấm "Gửi lại lời mời".
  - *Xóa nhân viên:* chỉ khi không phải Admin cuối cùng; thu hồi mọi quyền và phiên, giữ lịch sử trong `system_logs`.
- **Quan hệ Use Case:** `<<include>>` Gửi mã OTP xác thực — tác nhân phụ: `ACT-EML` Email Service; `<<include>>` Ghi nhật ký kiểm toán; `<<include>>` Phân cấp vai trò & Cấp phát / Thu hồi mã quyền chi tiết (RBAC) (`UC-ADM-02`).
- **Ràng buộc nghiệp vụ & Kỹ thuật:**
  - Không có Super Admin — vai trò cao nhất là `ADMIN`; Admin không tự khóa / tự hạ quyền mình, hệ thống luôn còn ≥ 1 Admin hoạt động.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 1 (tuần 1–2) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [User](../ClassDiagram/ENTITY.md#11-user--tài-khoản-đăng-nhập), [AdminProfile](../ClassDiagram/ENTITY.md#15-adminprofile--hồ-sơ-quản-trị-viên--nhân-viên), [StaffPermission](../ClassDiagram/ENTITY.md#18-staffpermission--phân-quyền-chi-tiết-cho-nhân-viên), [SystemLog](../ClassDiagram/ENTITY.md#121-systemlog--nhật-ký-kiểm-toán).

---

### UC-ADM-02: Phân cấp vai trò & Cấp phát / Thu hồi mã quyền chi tiết (RBAC)
- **Mã Use Case:** `UC-ADM-02`
- **Tác nhân chính:** `ACT-ADM` Admin
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Thực thi RBAC 2 tầng: Tầng 1 là vai trò (`CUSTOMER`, `VENDOR`, `STAFF`, `ADMIN`), Tầng 2 là 13 mã quyền chi tiết cho STAFF — quyết định nhân viên thấy mục nav nào của Admin.
- **Tiền điều kiện:** Admin đăng nhập (`MANAGE_RBAC`).
- **Hậu điều kiện:** `staff_permissions` thêm dòng mới hoặc `revokedAt = now()`; ghi `system_logs` `PERMISSION_GRANTED` / `PERMISSION_REVOKED` / `RBAC_ROLE_CHANGED`.
- **Luồng sự kiện chính:**
  1. Admin chọn nhân viên → "Sửa quyền"; giao diện hiển thị chức danh và 13 quyền: `VIEW_DASHBOARD` (Tổng quan), `REVIEW_KYC` (Duyệt KYC), `MANAGE_CATALOG` (Danh mục, Duyệt sản phẩm), `MODERATE_REVIEW` (Kiểm duyệt đánh giá), `HANDLE_DISPUTE` (Trả hàng & Hoàn tiền), `PROCESS_PAYOUT` (Xử lý rút tiền), `UPDATE_COMMISSION` (Cấu hình hoa hồng), `VIEW_FINANCE` (Sổ cái tài chính), `MANAGE_VOUCHER` (Voucher sàn, Flash Sale), `MANAGE_PLATFORM_NOTIFICATION` (Thông báo sàn), `VIEW_AUDIT_LOG` (Nhật ký hệ thống), `MANAGE_USER` (Khóa người dùng), `MANAGE_RBAC` (chỉ Admin).
  2. Đổi chức danh → `AdminProfile.changeTitle()` đề xuất bộ quyền mặc định; Admin xác nhận.
  3. Tích / bỏ tích quyền → thêm dòng mới hoặc `StaffPermission.revoke()`.
  4. Lưu: phiên đăng nhập của nhân viên được làm mới quyền (thu hồi refresh token) để quyền mới có hiệu lực ngay.
  5. Nav Admin chỉ hiện mục nhân viên có quyền; truy cập trực tiếp mục không có quyền → màn "Bạn không có quyền truy cập… Liên hệ Admin" và ghi `system_logs` `ACCESS_DENIED`.
- **Luồng thay thế / Ngoại lệ:**
  - *Cấp `MANAGE_RBAC` cho STAFF:* từ chối (`GNE-SEC-4031`).
  - *Cấp đồng thời `PROCESS_PAYOUT` + `UPDATE_COMMISSION` cho một người:* cảnh báo tách biệt nhiệm vụ, ghi log khi Admin vẫn xác nhận.
- **Quan hệ Use Case:** `<<include>>` Ghi nhật ký kiểm toán; được bao gồm bởi `UC-ADM-01`.
- **Ràng buộc nghiệp vụ & Kỹ thuật:**
  - Kiểm tra quyền ở tầng API bằng `@PreAuthorize("hasAuthority('REVIEW_KYC')")`; giao diện chỉ ẩn mục, không thay thế kiểm tra phía máy chủ.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 1 (tuần 1–2) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [StaffPermission](../ClassDiagram/ENTITY.md#18-staffpermission--phân-quyền-chi-tiết-cho-nhân-viên), [AdminProfile](../ClassDiagram/ENTITY.md#15-adminprofile--hồ-sơ-quản-trị-viên--nhân-viên), [User](../ClassDiagram/ENTITY.md#11-user--tài-khoản-đăng-nhập), [SystemLog](../ClassDiagram/ENTITY.md#121-systemlog--nhật-ký-kiểm-toán).

---

### UC-ADM-03: Khóa / Mở khóa tài khoản nhân viên và người dùng vi phạm
- **Mã Use Case:** `UC-ADM-03`
- **Tác nhân chính:** `ACT-ADM` Admin
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Cưỡng chế vô hiệu hóa quyền truy cập của nhân viên đã nghỉ việc, hoặc đình chỉ tài khoản người mua/người bán gian lận vi phạm nghiêm trọng chính sách pháp luật.
- **Tiền điều kiện:** Admin, hoặc nhân viên được cấp `MANAGE_USER` (khóa khách / shop); khóa nhân viên chỉ Admin (UC-ADM-01).
- **Hậu điều kiện:** `users.status` chuyển thành `LOCKED` hoặc `SUSPENDED`; thu hồi toàn bộ Refresh Token của tài khoản đó.
- **Luồng sự kiện chính:**
  1. Admin tìm kiếm tài khoản người dùng hoặc nhân viên theo Email/ID.
  2. Bấm nút "Khóa tài khoản" (hoặc "Đình chỉ hoạt động").
  3. Nhập lý do đình chỉ (`suspensionReason`, ví dụ: "Nhân viên đã chấm dứt hợp đồng lao động" hoặc "Người bán gian lận hàng giả").
  4. Backend gọi `User.suspend(reason, until)`: `users.status = 'SUSPENDED'`, lưu `suspensionReason` và `suspendedUntil` (NULL = vô thời hạn).
  5. Thu hồi ngay lập tức toàn bộ phiên làm việc của tài khoản này trong bảng `refresh_tokens`, đẩy Access Token vào Redis Blacklist để ngắt kết nối ngay tức thì.
  6. Ghi nhật ký kiểm toán `ACCOUNT_LOCKED` hoặc `ACCOUNT_SUSPENDED` vào `system_logs`.
  7. Mục **"Người dùng"** của Admin Console: tìm khách / shop theo email, SĐT, tên shop; xem trạng thái, số đơn, cảnh báo; thao tác Khóa (nhập lý do, thời hạn) / Mở khóa.
- **Luồng thay thế / Mở khóa:**
  - *Hết thời hạn đình chỉ:* Cron `SuspensionExpiryJob` (5 phút/lần) quét `status = 'SUSPENDED' AND suspended_until <= now()` và gọi `User.reactivateIfSuspensionExpired(now)` → `ACTIVE`, xóa lý do/hạn đình chỉ, ghi `system_logs` `ACCOUNT_REACTIVATED` và thông báo cho người dùng. Đình chỉ vô thời hạn (`suspended_until IS NULL`) không bao giờ tự mở.
  - *Admin mở khóa thủ công:* bấm "Mở khóa" → `User.reactivate()` (từ `SUSPENDED` hoặc `LOCKED`), bắt buộc nhập lý do, ghi log kiểm toán.
- **Quan hệ Use Case:** `<<include>>` Ghi nhật ký kiểm toán.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 6 (tuần 11–12) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [User](../ClassDiagram/ENTITY.md#11-user--tài-khoản-đăng-nhập) (`status`, `suspensionReason`, `suspendedUntil`).

---

### UC-ADM-04: Người bán gửi yêu cầu rút tiền từ Ví về Ngân hàng
- **Mã Use Case:** `UC-ADM-04`
- **Tác nhân chính:** `ACT-VND` Vendor (người bán/shop)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Cho phép người bán chuyển số dư doanh thu bán hàng đã hoàn tất từ Ví người bán về tài khoản ngân hàng thụ hưởng chính chủ.
- **Tiền điều kiện:** Ví người bán có số dư khả dụng lớn hơn hoặc bằng số tiền muốn rút (`balance >= amount`); số tiền rút tối thiểu 100.000 VNĐ; đã liên kết tài khoản ngân hàng thụ hưởng **đã xác minh chính chủ** (`VendorProfile.isPayoutAccountVerified()`, `UC-VND-07`).
- **Hậu điều kiện:** Tạo `withdrawal_requests` với `status = PENDING`; tạm trừ/tạm khóa số dư ví người bán; ghi log biến động.
- **Luồng sự kiện chính:**
  1. Người bán vào Quản lý Tài chính > Rút tiền.
  2. Giao diện hiển thị: Số dư ví khả dụng hiện tại, Thông tin tài khoản ngân hàng thụ hưởng đã đăng ký ở `UC-VND-07`.
  3. Người bán nhập số tiền cần rút (ví dụ: 15.000.000 VNĐ).
  4. Nhập mã OTP gửi qua email hoặc mã xác thực 2FA để xác nhận lệnh rút.
  5. Backend Spring Boot mở transaction CSDL:
     - Khóa dòng ví người bán bằng `SELECT balance FROM vendor_wallets WHERE id = ? FOR UPDATE`.
     - Kiểm tra điều kiện số dư: `balance >= amount`.
     - Trừ số dư ví người bán: `UPDATE vendor_wallets SET balance = balance - amount, pending_withdrawal = pending_withdrawal + amount, version = version + 1 WHERE id = ?`.
     - Tạo bản ghi trong `withdrawal_requests`: `walletId` (FK trỏ tới `vendor_wallets.id`), `amount`, `bankName`, `bankAccountNumber`, `bankAccountName`, `status = 'PENDING'`.
     - Tạo bản ghi `wallet_transactions` với `type = 'DEBIT_WITHDRAWAL'`.
  6. Commit transaction. Thông báo: "Lệnh rút tiền đã được tạo thành công và đang chờ kế toán sàn phê duyệt giải ngân trong 24 giờ làm việc".
  7. Form rút tiền chỉ hiển thị tài khoản nhận tiền đã đăng ký và xác minh ở UC-VND-07 (không nhập lại số tài khoản mỗi lần).
- **Luồng thay thế / Ngoại lệ:**
  - *Số tiền rút lớn hơn số dư khả dụng:* Báo lỗi 400 "Số dư ví không đủ để thực hiện giao dịch".
  - *Tài khoản nhận tiền chưa xác minh chính chủ:* Báo lỗi `GNE-WAL-4032`, chuyển người bán tới `UC-VND-07` để xác minh.
- **Quan hệ Use Case:** `<<include>>` Kiểm tra số dư khả dụng.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 4 (tuần 7–8) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [WithdrawalRequest](../ClassDiagram/ENTITY.md#66-withdrawalrequest--yêu-cầu-rút-tiền), [VendorWallet](../ClassDiagram/ENTITY.md#64-vendorwallet--ví-tiền-của-người-bán), [WalletTransaction](../ClassDiagram/ENTITY.md#65-wallettransaction--lịch-sử-biến-động-ví), [AdminNotification](../ClassDiagram/ENTITY.md#105-adminnotification--thông-báo-nội-bộ-cho-quản-trị-viên).

---

### UC-ADM-05: Nhân viên sàn phê duyệt & Thực hiện lệnh chuyển khoản (Payout)
- **Mã Use Case:** `UC-ADM-05`
- **Tác nhân chính:** `ACT-FIN` Finance Officer (Nhân viên tài chính); `ACT-ADM` Admin
- **Tác nhân phụ:** `ACT-PGW` Payment Gateway (VNPay, SePay); `ACT-EML` Email Service
- **Mục đích nghiệp vụ:** Nhân viên sàn kiểm tra lệnh rút tiền của shop, chuyển khoản về tài khoản chính chủ của shop và ghi nhận hoàn tất — bước chuyển khoản do người thực hiện qua internet banking, bước xác nhận do SePay tự động.
- **Tiền điều kiện:** Lệnh rút ở trạng thái `PENDING` (tạo ở `UC-ADM-04`); đăng nhập vai trò `STAFF` hoặc `ADMIN`.
- **Hậu điều kiện:** `withdrawal_requests.status` = `COMPLETED` (hoặc `REJECTED`); `VendorWallet.settleWithdrawal()`; ghi `WalletTransaction(DEBIT_WITHDRAWAL)` và `system_logs` `PAYOUT_APPROVED` / `PAYOUT_REJECTED`; email kết quả cho shop.
- **Luồng sự kiện chính:**
  1. Staff mở Admin Portal > Rút tiền, chọn lệnh `PENDING`; hệ thống hiển thị số dư, lệnh rút, khiếu nại đang mở của shop, tài khoản nhận (đã xác minh chính chủ — `UC-VND-07`).
  2. Staff bấm "Phê duyệt": `WithdrawalRequest.approve(staffId, now)` → `APPROVED`; hệ thống hiển thị thông tin chuyển khoản kèm **nội dung bắt buộc** `TRWD{withdrawalId}` và ảnh VietQR để quét bằng app ngân hàng của sàn.
  3. Staff chuyển khoản qua internet banking của tài khoản sàn → `startProcessing()` → `PROCESSING`.
  4. SePay phát hiện **tiền ra** khỏi tài khoản sàn và gọi webhook (`transferType = out`); hệ thống tách mã `TRWD{id}` trong `content`, kiểm tra `transferAmount = amount`, chống trùng theo `id` SePay.
  5. Khớp → mở transaction: `WithdrawalRequest.complete(referenceCode)` → `COMPLETED`; `VendorWallet.settleWithdrawal(amount)` (giảm `balance` và `pendingWithdrawal`, cập nhật `balanceHash`); ghi `WalletTransaction(DEBIT_WITHDRAWAL)`, `system_logs(PAYOUT_APPROVED)`.
  6. Gửi email + thông báo in-app cho shop kèm mã giao dịch ngân hàng (`referenceCode`).
- **Luồng thay thế / Ngoại lệ:**
  - *2a. Từ chối (nghi gian lận, shop đang có khiếu nại lớn):* `reject(reason)` → `REJECTED`, nhả `pendingWithdrawal` về số dư khả dụng, ghi `PAYOUT_REJECTED`, thông báo lý do.
  - *4a. Sau 24 giờ không có webhook khớp* (gõ sai nội dung, SePay gián đoạn): lệnh nằm ở hàng đợi "chưa xác nhận"; Staff đối chiếu sao kê và bấm "Xác nhận tay" nhập mã giao dịch ngân hàng.
  - *4b. Số tiền webhook khác số tiền lệnh:* không tự hoàn tất, cảnh báo Staff.
  - *Từ chối lệnh rút:* nhập lý do bắt buộc → `REJECTED`, số tiền trả lại số dư khả dụng của ví (`pendingWithdrawal` → `balance`), thông báo người bán.
  - *KYC của shop chưa `VERIFIED`:* nút duyệt chi bị chặn ("Chặn — KYC chưa duyệt").
- **Quan hệ Use Case:** `<<include>>` Ghi sổ cái tài chính kép bất biến (Financial Ledger) (`UC-PAY-06`).
- **Ràng buộc nghiệp vụ & Kỹ thuật:** Chỉ chuyển tới tài khoản đã xác minh chính chủ; webhook SePay xác thực bằng header `Authorization: Apikey`; SePay **không** tự chi tiền — chỉ xác nhận lệnh đã đi.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 4 (tuần 7–8) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [WithdrawalRequest](../ClassDiagram/ENTITY.md#66-withdrawalrequest--yêu-cầu-rút-tiền), [VendorWallet](../ClassDiagram/ENTITY.md#64-vendorwallet--ví-tiền-của-người-bán), [WalletTransaction](../ClassDiagram/ENTITY.md#65-wallettransaction--lịch-sử-biến-động-ví).

---

### UC-ADM-06: Dashboard doanh thu & ký quỹ toàn sàn
- **Mã Use Case:** `UC-ADM-06`
- **Tác nhân chính:** `ACT-ADM` Admin; `ACT-STF` Staff (Nhân viên sàn)
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Cho ban điều hành xem nhanh tình hình kinh doanh của sàn: GMV, doanh thu hoa hồng, tiền ký quỹ đang giữ, tiền COD chờ đối soát với GHN, số đơn theo trạng thái, top shop / top sản phẩm.
- **Tiền điều kiện:** Đăng nhập với vai trò `ADMIN` hoặc `STAFF`.
- **Hậu điều kiện:** Hiển thị dashboard và cho phép xuất CSV.
- **Luồng sự kiện chính:**
  1. Người dùng mở "Dashboard" trên Admin Portal, chọn khoảng thời gian (mặc định 30 ngày).
  2. Hệ thống tổng hợp bằng truy vấn SQL (cache Redis 5 phút):
     - **GMV** = Σ `orders.grand_total` của đơn `COMPLETED` / `DELIVERED` trong kỳ.
     - **Doanh thu hoa hồng** = Σ `escrow_ledgers.commission_fee` của ký quỹ `RELEASED` trong kỳ.
     - **Ký quỹ đang giữ** = Σ `gross_amount` của ký quỹ `HELD` / `DISPUTED`.
     - **COD chờ đối soát GHN** = Σ `cod_funded_amount` dự kiến của ký quỹ `PENDING_FUNDING`.
     - **Chi phí voucher sàn** = Σ `order_discounts.discount_amount` với `funded_by = PLATFORM`.
     - Số đơn theo trạng thái, tỷ lệ hủy, top 10 shop theo GMV, top 10 sản phẩm bán chạy.
  3. Hiển thị thẻ số liệu và biểu đồ theo ngày (Recharts).
  4. Bấm "Xuất CSV": hệ thống sinh file chi tiết theo đơn phục vụ đối soát.
  5. **Tab Tổng quan** (`VIEW_DASHBOARD`): GMV, doanh thu hoa hồng, chi phí voucher / freeship (trợ giá marketing), **lợi nhuận ròng** (hoa hồng − chi phí sàn tài trợ − Điểm − phí cổng), đang giữ escrow, **nợ vendor** (số dư ví chưa rút + escrow `HELD`), tiền chờ hoàn, **việc tồn** (KYC chờ duyệt, sản phẩm chờ duyệt, lệnh rút chờ, tranh chấp mở); biểu đồ tăng trưởng tuần / tháng / năm.
  6. Bảng **"Vendor tiềm năng bứt phá"**: xếp hạng shop theo tăng trưởng GMV so với kỳ trước, lọc ngành hàng, tìm tên shop, phân trang.
  7. **Tab Sổ cái tài chính** (`VIEW_FINANCE`): bảng `financial_ledger` theo `seq` (thời gian, sự kiện, tham chiếu, tài khoản, Nợ, Có — chỉ ghi thêm, điều chỉnh bằng bút toán đảo), lọc và "Xuất sổ cái".
- **Quan hệ Use Case:** Không có.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 6 (tuần 11–12) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [Order](../ClassDiagram/ENTITY.md#52-order--đơn-hàng-theo-từng-shop), [EscrowLedger](../ClassDiagram/ENTITY.md#63-escrowledger--sổ-ký-quỹ-tiền-tạm-giữ), [OrderDiscount](../ClassDiagram/ENTITY.md#54-orderdiscount--chi-tiết-các-mã-đã-áp-cho-đơn), [VendorProfile](../ClassDiagram/ENTITY.md#14-vendorprofile--hồ-sơ-người-bán-shop), [FinancialLedger](../ClassDiagram/ENTITY.md#67-financialledger--sổ-cái-tài-chính-chỉ-ghi-thêm), [VendorWallet](../ClassDiagram/ENTITY.md#64-vendorwallet--ví-tiền-của-người-bán).

---

### UC-ADM-07: Tra cứu nhật ký kiểm toán hệ thống bất biến (System Audit Logs)
- **Mã Use Case:** `UC-ADM-07`
- **Tác nhân chính:** `ACT-ADM` Admin
- **Tác nhân phụ:** —
- **Mục đích nghiệp vụ:** Truy vết hành động nhạy cảm (đăng nhập, đổi quyền, duyệt KYC, sửa hoa hồng, khóa tài khoản, giải ngân, ẩn đánh giá, truy cập bị chặn) phục vụ điều tra và tuân thủ (mục "Nhật ký hệ thống").
- **Tiền điều kiện:** Admin, hoặc nhân viên được cấp `VIEW_AUDIT_LOG`.
- **Hậu điều kiện:** Hiển thị nhật ký có phân trang; xuất tệp khi được yêu cầu.
- **Luồng sự kiện chính:**
  1. Mở Nhật ký hệ thống → `GET /api/v1/admin/system-logs?from=&to=&type=&q=`.
  2. Lọc theo khoảng thời gian (Hôm nay / 7 ngày / 30 ngày / tùy chọn), loại hành động (Đăng nhập, KYC, Tài chính, Kiểm duyệt, Bảo mật, Cấu hình), từ khóa (user, IP, mã tham chiếu).
  3. Hiển thị: thời gian, user / vai trò, hành động, chi tiết, kết quả (`SUCCESS` / `FAILED` / `DENIED`); dòng tóm tắt "N lần truy cập bị từ chối".
  4. Phân trang keyset theo (`createdAt`, `id`); "Xuất nhật ký" tạo CSV của bộ lọc hiện tại.
- **Luồng thay thế / Ngoại lệ:**
  - *Không có bản ghi khớp:* "Không có bản ghi nào khớp bộ lọc".
- **Quan hệ Use Case:** Không có.
- **Ràng buộc nghiệp vụ & Kỹ thuật:**
  - `system_logs` chỉ ghi thêm — cấm `UPDATE` / `DELETE`; lưu tối thiểu 12 tháng.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 7 (tuần 13–14) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [SystemLog](../ClassDiagram/ENTITY.md#121-systemlog--nhật-ký-kiểm-toán).

---

### UC-ADM-12: Quản lý banner trang chủ & chiến dịch tĩnh (CMS Banner)
- **Mã Use Case:** `UC-ADM-12`
- **Tác nhân chính:** `ACT-ADM` Admin; `ACT-FIN` Finance Officer (Nhân viên tài chính)
- **Tác nhân phụ:** `ACT-MIN` Silo S3 Storage
- **Mục đích nghiệp vụ:** Đặt lịch và quản lý banner trang chủ (slider "SIÊU SALE 12.12", banner ngành hàng, banner khu Flash Sale) mà không cần sửa mã giao diện.
- **Tiền điều kiện:** Admin hoặc nhân viên có `MANAGE_VOUCHER`.
- **Hậu điều kiện:** `home_banners` được tạo / cập nhật; cache `banner:live:{position}` làm mới.
- **Luồng sự kiện chính:**
  1. Tải ảnh desktop (bắt buộc) và mobile (tùy chọn) lên Silo.
  2. Khai báo tiêu đề; **liên kết đích** chọn từ dữ liệu sàn thay vì gõ tay: Danh mục, Sản phẩm (tìm theo tên, lọc theo shop, hiện shop và giá), Cửa hàng, khu Flash Sale, trang **Gợi ý hôm nay**, hoặc **Trang chiến dịch** (nhập đường dẫn, vd. `/sieu-sale-12-12`); vị trí (`HOME_TOP_CAROUSEL` — băng chuyền đầu trang chủ, `HOME_TOP_SIDE` — hai ô cạnh băng chuyền, `HOME_MIDDLE` — giữa "Gợi ý cho bạn" và Flash Sale, `CATEGORY_TOP` kèm danh mục, `FLASH_SALE_TOP` — trong khối Flash Sale, `POPUP` — hộp nổi khi mở trang chủ); thứ tự (hệ thống gợi ý số kế tiếp trong cùng vị trí / danh mục, số nhỏ hiện trước); thời gian hiển thị.
  3. Hệ thống kiểm tra `endsAt > startsAt`, `CATEGORY_TOP` phải có danh mục, tối đa 8 banner đang hiển thị mỗi vị trí.
  4. Trang chủ đọc banner đang hiển thị (`isLive(now)`) từ cache Redis 60 giây.
  5. Lượt hiển thị / bấm đếm trên Redis, job 5 phút ghi `recordStats()`; màn quản lý hiển thị CTR.
  6. Cách hiển thị theo vị trí: băng chuyền đổi banner mỗi 6 giây và chiếm hết chiều ngang khi không có banner `HOME_TOP_SIDE`; `HOME_TOP_SIDE` xếp banner lẻ vào ô trên, chẵn vào ô dưới, mỗi ô tự lướt; `HOME_MIDDLE` 1 banner chiếm hết, 2 banner chia đôi, từ 3 thì hai bên thay nhau lướt; `FLASH_SALE_TOP` từ 2 banner thì lướt; `POPUP` hiện mỗi phiên một lần, nhiều banner xếp chồng như bộ bài (thứ tự nhỏ nằm trên cùng).
  7. Nút "Xem" mở đúng trang storefront nơi banner hiển thị để kiểm tra.
  8. **Trang chiến dịch:** liên kết dạng `/{slug}` mở trang gồm màu / ảnh chủ đạo, tiêu đề, đoạn giới thiệu, voucher sàn để lưu ngay (UC-PRO-03) và lưới sản phẩm tham gia. TLCN cấu hình nội dung trang chiến dịch tĩnh trong mã nguồn frontend — không có bảng / màn quản lý riêng (để DATN); slug không khớp → "Chiến dịch không tồn tại hoặc đã kết thúc" kèm nút về trang chủ.
- **Luồng thay thế / Ngoại lệ:**
  - *Sai giá / sai nội dung:* "Tắt ngay" → `deactivate()`, xóa cache vị trí; "Bật lại" khi đã sửa.
  - *Liên kết ra ngoài miền sàn (`//other.com`, `/\other.com`, `javascript:`…):* từ chối "Liên kết đích phải thuộc miền của sàn".
  - *Thiếu ảnh desktop / thiếu thời gian / ngày kết thúc không sau ngày bắt đầu / vị trí đã đủ 8 banner đang hiển thị:* báo lỗi tại ô tương ứng.
- **Quan hệ Use Case:** `<<include>>` Tải tệp lên Silo (Presigned URL) — tác nhân phụ: `ACT-MIN` Silo S3 Storage.
- **Ràng buộc nghiệp vụ & Kỹ thuật:**
  - Liên kết đích chỉ trong miền của sàn (chống chuyển hướng mở); mọi thay đổi ghi `system_logs`.
- **Đối chiếu Hiện trạng & Thực thể:**
  - *Hiện trạng:* Chưa triển khai backend — giao diện đã dựng xong trên lớp mock (`web/`, plan G00–G13), chờ API.
  - *Kế hoạch:* Sprint 8 (tuần 15–16) — [implement.md](../../kien-thuc/implement.md) Phần 1.2.
  - *Thực thể CSDL:* [HomeBanner](../ClassDiagram/ENTITY.md#104-homebanner--banner-trang-chủ--chiến-dịch-tĩnh-cms), [Category](../ClassDiagram/ENTITY.md#21-category--danh-mục-ngành-hàng).

---

<a id="phan-iv"></a>
# PHẦN IV: SƠ ĐỒ USE CASE UML 2.5

Mã nguồn PlantUML đầy đủ (11 sơ đồ phân hệ + sơ đồ tác nhân + sơ đồ tổng quan) nằm ở [UML_UseCase_PlantUML.md](./UML_UseCase_PlantUML.md); mô hình Enterprise Architect ở `Genie_UseCase_UML25.xmi` (Import Package from XMI). Hai nguồn sinh cùng mô hình với Phần III nên tác nhân, use case, «include» / «extend» khớp 1–1.

| Phân hệ | Số UC | «include» | «extend» |
| :--- | :---: | :---: | :---: |
| 01. Xác thực & Tài khoản người dùng | 9 | 6 | 1 |
| 02. Danh mục, Sản phẩm & Tìm kiếm | 17 | 5 | 5 |
| 03. Giỏ hàng, Đặt hàng & Đơn hàng | 9 | 3 | 0 |
| 04. Thanh toán, Ký quỹ (Escrow) & Sổ cái | 5 | 4 | 0 |
| 05. Gian hàng & Thẩm định KYC người bán | 11 | 4 | 1 |
| 06. Vận chuyển qua đối tác 3PL | 5 | 1 | 0 |
| 07. Khuyến mãi, Voucher, Flash Sale & Điểm | 9 | 0 | 3 |
| 08. Chat, Đánh giá & Theo dõi shop | 7 | 3 | 1 |
| 09. Trả hàng, Hoàn tiền & Khiếu nại | 4 | 2 | 4 |
| 10. Thông báo | 3 | 1 | 0 |
| 11. Quản trị sàn, Nhân viên & Rút tiền | 8 | 8 | 0 |
| **Tổng** | **87** | **37** | **15** |

---

## CHANGELOG

### TLCN v1.4 — 2026-11-16 (đồng bộ với mã nguồn `genie-multivendor-platform`)
- **Kho tệp MinIO → Silo** (fork cộng đồng của MinIO, cùng API S3; MinIO ngừng phát hành bản cộng đồng — ADR 0009). Tác nhân `ACT-MIN` đổi tên thành "Silo S3 Storage", giữ mã; use case phụ "Tải tệp lên Silo (Presigned URL)".
- **Phân hệ 01 viết lại theo hiện thực** (plan AUTH D2–D22, ADR 0007 / 0008): OTP dùng chung `otp:challenge:{id}`; đăng ký giữ dữ liệu tạm 15 phút; Argon2id `m = 19456, t = 2, p = 1`; khóa tạm không đổi `status`; mã mới `GNE-AUTH-4012` (tài khoản chỉ Google đăng nhập bằng mật khẩu) và `GNE-AUTH-4098` (liên kết Google phải trùng email); gỡ liên kết phải nhập lại mật khẩu; ảnh Google lưu vào Silo, chỉ điền khi hồ sơ trống; refresh xoay vòng giữ `family_id`, ân hạn 10 giây, trả kèm `user`; đăng xuất `DELETE /auth/sessions/current`; quên mật khẩu = thiết lập mật khẩu cho tài khoản chỉ Google; xóa tài khoản giữ phiên hiện tại, cron 02:30.
- **Đổi tên 3 use case theo giao diện** (mã giữ nguyên): `UC-AUTH-03` → "Đăng nhập & Liên kết tài khoản Google / Apple (OAuth2 / OpenID Connect)"; `UC-AUTH-06` → "Đổi / Thiết lập mật khẩu"; `UC-AUTH-11` → "Đăng xuất & Quản lý thiết bị đăng nhập".
- **Hiện trạng từng UC** cập nhật: 3 UC đã triển khai, 5 UC đang triển khai (đều thuộc AUTH), 79 UC chưa có backend nhưng giao diện đã dựng xong trên lớp mock. Theo dõi chi tiết: [CHECKLIST_TIEN_DO_UC.md](../../CHECKLIST_TIEN_DO_UC.md).

### TLCN v1.2 — 2026-11-06 (đối chiếu giao diện Genie sau đợt làm lại 02–06/11)

- **Thêm `UC-AI-07` Phân tích cảm xúc đánh giá bằng PhoBERT** (đưa lại từ DATN, bản rút gọn: cảm xúc tổng thể 3 nhãn, mô hình nhóm tự fine-tune chạy trong AI Worker): **87 use case**, phân hệ 08 có 7 UC; `ACT-AIW` đổi tên "AI Worker Service (KYC & PhoBERT)"; kết quả dùng ở `UC-VND-08` (khối Cảm xúc đánh giá) và `UC-SOC-05` (ưu tiên hàng chờ). Excel DATN đánh "Đã triển khai" cho `UC-AI-07`.

- Đổi thương hiệu Lazadee → **Genie**, Xu → **Điểm Genie / Điểm**, Lazadee Express → Genie Express, nhãn MALL → **Chính hãng**, bộ lọc Lazadee Mall → **Shop Chính Hãng**; mã đơn / nội dung chuyển khoản `TR…`, `TRRF…`, `TRWD…`; mã lỗi `GNE-…`. File mô hình: `Genie_UseCase_UML25.xmi`.
- `UC-AUTH-03`: liên kết / gỡ liên kết Google ở mục "Liên kết đăng nhập", không liên kết khi email chưa xác minh hoặc tài khoản bị khóa.
- `UC-ADM-12`: vị trí `HOME_TOP_SIDE`, 6 loại liên kết đích, cách hiển thị từng vị trí, popup xếp chồng, trang chiến dịch tĩnh, kiểm tra liên kết ngoài miền.
- `UC-SUP-03`: hàng giả / nhái được xác minh → hoàn 100%, khóa sản phẩm, cảnh báo shop (trang chính sách Shop Chính Hãng bỏ cam kết hoàn 200%).
- `UC-CAT-07`: trang "Gợi ý hôm nay"; `UC-VND-08`: kết quả đơn, tiền thực về shop, khiếu nại thua, đơn sàn tự xác nhận; `UC-VND-19`: sửa mục đích cho khớp luồng.

### TLCN v1.1 — 2026-10-29 (đối chiếu giao diện client + prototype)

- Phạm vi mở rộng thành **toàn bộ giao diện client + prototype** (kể cả mục nav đang ẩn, mục chỉ có ở client): **86 use case** (57 → 86): đưa lại 24 use case từ DATN (giữ mã & tên), thêm 5 use case mới `UC-CAT-17`, `UC-CAT-18`, `UC-VND-20`, `UC-VND-21`, `UC-LOG-16` (đã thêm cả DATN).
- Tác nhân 11 → 16: thêm 3 chức danh nhân viên `ACT-MOD`, `ACT-CS`, `ACT-FIN` (không có Super Admin), `ACT-GEM` Google Gemini API (kiểm duyệt sản phẩm tự động), `ACT-IDP` (đăng nhập Google / Apple).
- Thanh toán chốt **VNPay + VietQR (SePay) + COD** (COD dùng chung mô hình ký quỹ); voucher xếp chồng 3 tầng + Điểm (`UC-PRO-04`); `UC-PRO-03` đổi tên theo DATN (chỉ còn thu thập / quản lý kho voucher).
- Sửa 40 use case cũ theo giao diện (mini-cart, mua lại, lời nhắn cho shop, hủy nhóm đơn, thanh toán lại, màn VietQR, chỉ số trang shop, Hữu ích, cảnh báo thiết bị lạ, đăng nhập OTP email, cấu hình hoa hồng mặc định, phân phối lại việc duyệt…).
- Kế hoạch 6 → 8 sprint (16 tuần); dòng "Quan hệ Use Case" sinh từ cùng danh sách «include» / «extend» với PlantUML và XMI.

### MVP v1.0 — 2026-10-28

- Tạo bản MVP 57 use case / 11 phân hệ / 11 tác nhân từ bản đầy đủ v2.15; chuyển các "use case" kỹ thuật (Kafka, Saga, circuit breaker…) sang yêu cầu phi chức năng ở Document_Vi.md mục 1.8.
