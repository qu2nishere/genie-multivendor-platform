# BÁO CÁO PHẠM VI TLCN — NHỮNG GÌ ĐƯỢC CHỌN & BỊ BỎ
## Genie — bộ tài liệu TLCN (`fix-doc/TLCN/`) so với bản hoàn chỉnh DATN (`fix-doc/DATN/`)

> **Ngày lập:** 2026-10-28 (v1.0) · **Cập nhật:** 2026-10-29 (v1.1 — phủ toàn bộ giao diện client + prototype, xem mục 0). Bản hoàn chỉnh cho đồ án tốt nghiệp nay ở `fix-doc/DATN/` (trước nằm ở gốc `fix-doc/`).
> **Bản MVP:** cùng cấu trúc thư mục và tên file trong `fix-doc/TLCN/`. Mọi danh sách bên dưới được **sinh tự động** từ dữ liệu (`backup/mvp-generator/scope.py` + mô hình ENTITY.md) nên khớp 100% với tài liệu.

> **Cập nhật v1.4 (2026-11-16):** phạm vi **không đổi** (87 UC / 65 bảng / 55 enum). Đổi tên 3 UC theo giao diện (`UC-AUTH-03`, `UC-AUTH-06`, `UC-AUTH-11` — giữ mã), kho tệp MinIO → Silo, AUTH bổ sung schema bằng `V14__auth_additions.sql` (chỉ thêm cột / index / giá trị enum — đúng quy tắc mở rộng NFR-12). Chi tiết: [BAO_CAO_DOI_CHIEU_GIAO_DIEN.md mục 18](BAO_CAO_DOI_CHIEU_GIAO_DIEN.md#muc-18); tiến độ: [CHECKLIST_TIEN_DO_UC.md](CHECKLIST_TIEN_DO_UC.md).

---

## 0. Cập nhật v1.1 (2026-10-29) — phủ toàn bộ giao diện client + prototype

> Mục 1–6 bên dưới là **ảnh chụp lúc tạo bản v1.0** (57 use case) để truy vết quyết định cắt giảm ban đầu. Phạm vi hiện hành là mục 0 này; danh sách màn hình ↔ use case ở [BAO_CAO_DOI_CHIEU_GIAO_DIEN.md](./BAO_CAO_DOI_CHIEU_GIAO_DIEN.md).

### 0.1 Con số hiện hành

| Hạng mục | DATN v2.16 | TLCN v1.0 | **TLCN v1.1** | TLCN / DATN |
|---|:---:|:---:|:---:|:---:|
| Use case | 206 | 57 | **87** | 42% |
| Phân hệ | 23 | 11 | **11** | 48% |
| Tác nhân | 44 | 11 | **16** | 36% |
| Use case dùng chung (helper) | 6 | 4 | **4** | — |
| «include» / «extend» | 28 / 20 | 18 / 7 | **37 / 15** | — |
| Nhóm thực thể | 32 | 12 | **12** | — |
| Entity / bảng | 169 | 38 | **64** | 38% |
| Class `@IdClass` | 11 | 0 | **5** | — |
| Enum | 129 | 37 | **52** | 40% |
| Quan hệ khóa ngoại | 205 | 61 | **100** | 49% |
| Sơ đồ lớp (file Java) | 19 | 7 | **10** | — |
| Thuộc tính (field) | 1733 | 493 | **724** | 42% |
| Phương thức nghiệp vụ | 469 | 145 | **201** | 43% |
| Sprint | — | 6 (12 tuần) | **8 (16 tuần)** | — |

### 0.2 Use case thêm vào TLCN (29)

| # | Mã | Tên | Nguồn | Sprint |
|:---:|---|---|---|:---:|
| 1 | `UC-AUTH-03` | Đăng nhập & Liên kết tài khoản Google / Apple (OAuth2 / OpenID Connect) *(tên cũ: Đăng nhập một lần qua Mạng xã hội (SSO OAuth2) — đổi 2026-11-16 theo giao diện)* | Lấy lại từ DATN (cùng mã & tên) | S7 |
| 2 | `UC-AUTH-10` | Yêu cầu ẩn danh hóa & Xóa dữ liệu cá nhân (Xóa tài khoản) | Lấy lại từ DATN (cùng mã & tên) | S7 |
| 3 | `UC-CAT-04` | Gợi ý từ khóa tìm kiếm & Xu hướng (Autocomplete & Trending) | Lấy lại từ DATN (cùng mã & tên) | S7 |
| 4 | `UC-CAT-07` | Xem gợi ý sản phẩm tương tự | Lấy lại từ DATN (cùng mã & tên) | S7 |
| 5 | `UC-CAT-12` | Kiểm duyệt sản phẩm tự động qua AI (Google Gemini API) | Lấy lại từ DATN (cùng mã & tên) | S7 |
| 6 | `UC-CAT-15` | Cấu hình bộ thuộc tính chuẩn theo ngành hàng (Category Attributes) | Lấy lại từ DATN (cùng mã & tên) | S7 |
| 7 | `UC-CAT-17` | Người bán đề xuất danh mục mới & Duyệt đề xuất | **Mới** — đã thêm cả DATN | S7 |
| 8 | `UC-CAT-18` | Chia sẻ sản phẩm bằng liên kết (Deeplink) | **Mới** — đã thêm cả DATN | S7 |
| 9 | `UC-PAY-06` | Ghi sổ cái tài chính kép bất biến (Financial Ledger) | Lấy lại từ DATN (cùng mã & tên) | S3 |
| 10 | `UC-VND-06` | Cấu hình tin nhắn trả lời tự động (Auto-reply) | Lấy lại từ DATN (cùng mã & tên) | S7 |
| 11 | `UC-VND-19` | Bật chế độ tạm nghỉ bán hàng của shop (Vacation Mode) | Lấy lại từ DATN (cùng mã & tên) | S7 |
| 12 | `UC-VND-20` | Xem ví & sổ giao dịch người bán (Seller Wallet & Escrow Ledger) | **Mới** — đã thêm cả DATN | S7 |
| 13 | `UC-VND-21` | Cập nhật thông tin gian hàng & cài đặt vận hành | **Mới** — đã thêm cả DATN | S7 |
| 14 | `UC-LOG-15` | Điều phối đa hãng vận chuyển (Dynamic Carrier Routing) | Lấy lại từ DATN (cùng mã & tên) | S7 |
| 15 | `UC-LOG-16` | Người bán xác nhận / từ chối đơn hàng mới | **Mới** — đã thêm cả DATN | S4 |
| 16 | `UC-PRO-04` | Thuật toán Stacking Voucher 3 Tầng & Điểm tự động khi Checkout | Lấy lại từ DATN (cùng mã & tên) | S8 |
| 17 | `UC-PRO-05` | Tạo chiến dịch Flash Sale & Cấu hình khung giờ vàng | Lấy lại từ DATN (cùng mã & tên) | S8 |
| 18 | `UC-PRO-06` | Mua hàng Flash Sale chống bán vượt suất | Lấy lại từ DATN (cùng mã & tên) | S8 |
| 19 | `UC-PRO-08` | Điểm danh nhận Điểm hàng ngày & Duy trì chuỗi điểm danh (Streak) | Lấy lại từ DATN (cùng mã & tên) | S8 |
| 20 | `UC-PRO-09` | Tra cứu số dư Điểm Genie & Lịch sử biến động Điểm | Lấy lại từ DATN (cùng mã & tên) | S8 |
| 21 | `UC-PRO-10` | Sử dụng Điểm Genie để giảm trừ thanh toán đơn hàng | Lấy lại từ DATN (cùng mã & tên) | S8 |
| 22 | `UC-SOC-02` | Gửi thẻ sản phẩm, hình ảnh và trạng thái đơn hàng trong chat | Lấy lại từ DATN (cùng mã & tên) | S8 |
| 23 | `UC-SOC-05` | Báo cáo đánh giá vi phạm / spam & Kiểm duyệt đánh giá (Review Moderation & Report) | Lấy lại từ DATN (cùng mã & tên) | S8 |
| 24 | `UC-SOC-06` | Khách hàng theo dõi gian hàng (Shop Follow) | Lấy lại từ DATN (cùng mã & tên) | S8 |
| 25 | `UC-NOT-05` | Admin phát thông báo toàn sàn (Broadcast Platform Notification) | Lấy lại từ DATN (cùng mã & tên) | S8 |
| 26 | `UC-ADM-01` | Quản lý danh sách nhân viên nội bộ & Lời mời kích hoạt | Lấy lại từ DATN (cùng mã & tên) | S1 |
| 27 | `UC-ADM-02` | Phân cấp vai trò & Cấp phát / Thu hồi mã quyền chi tiết (RBAC) | Lấy lại từ DATN (cùng mã & tên) | S1 |
| 28 | `UC-ADM-07` | Tra cứu nhật ký kiểm toán hệ thống bất biến (System Audit Logs) | Lấy lại từ DATN (cùng mã & tên) | S7 |
| 29 | `UC-ADM-12` | Quản lý banner trang chủ & chiến dịch tĩnh (CMS Banner) | Lấy lại từ DATN (cùng mã & tên) | S8 |

`UC-CAT-16` (Xem trang gian hàng) có ở TLCN từ v1.0 nhưng DATN chưa có → đã thêm vào DATN. DATN v2.16 vì vậy có thêm 6 use case: `UC-CAT-16`, `UC-CAT-17`, `UC-CAT-18`, `UC-VND-20`, `UC-VND-21`, `UC-LOG-16` (200 → 206).

### 0.3 Tác nhân thêm vào TLCN

`ACT-MOD` Moderator, `ACT-CS` CS Specialist, `ACT-FIN` Finance Officer (3 chức danh kế thừa Staff — **không có Super Admin**), `ACT-GEM` Google Gemini API (kiểm duyệt sản phẩm), `ACT-IDP` Identity Provider (Google / Apple). DATN dùng AI Worker cho kiểm duyệt sản phẩm nên không có `ACT-GEM`.

### 0.4 Entity thêm vào TLCN (26)

`StaffPermission`, `CategoryAttribute`, `Brand`, `VoucherCategory`, `VoucherProduct`, `VoucherExcludedCategory`, `VoucherExcludedProduct`, `FinancialLedger`, `VendorShippingConfig`, `ReviewHelpfulVote`, `ReviewReport`, `ShopFollow`, `ProductDocument`, `ProductModerationResult`, `CategoryProposal`, `SearchKeywordStat`, `ProductViewStat`, `FlashSaleCampaign`, `FlashSaleItem`, `UserCoin`, `CoinTransaction`, `DailyCheckin`, `PlatformNotification`, `UserPlatformNotificationRead`, `HomeBanner`, `AdminNotification`.

Trong đó 7 entity **mới cho cả hai bản** (thêm vào DATN v2.16): `Brand`, `ProductDocument`, `CategoryProposal`, `ProductModerationResult`, `SearchKeywordStat`, `ProductViewStat`, `ReviewHelpfulVote`. Trường mới thêm vào entity có sẵn (cả hai bản): `VendorProfile` (`isMall`, `ratingAvg`, `ratingCount`, `followerCount`, `activeProductCount`, `chatResponseRate`, `avgResponseMinutes`, `preOrderEnabled`), `Product` (`brandId`, `itemCondition`, `isPreOrder`, `preOrderDays`, `viewCount`), `ProductReview.helpfulCount`, `Order.buyerNote`.

### 0.5 Quyết định thiết kế của v1.1

| Chủ đề | Quyết định |
|---|---|
| Kiểm duyệt sản phẩm | Google Gemini API (structured output JSON) sàng lọc, Moderator duyệt cuối — UC-CAT-12, UC-CAT-13 |
| Thanh toán | VNPay + chuyển khoản VietQR (SePay) + **COD kết hợp Escrow** (ký quỹ COD `PENDING_FUNDING` tới khi đối soát) — giống DATN |
| Khuyến mãi | Voucher xếp chồng 3 tầng Shop → Sàn → Freeship + Điểm (≤ 50%, 1 Điểm = 1₫, hết hạn **12 tháng** — DATN cũng đổi từ 180 ngày sang 12 tháng); đổi Điểm lấy voucher gộp vào UC-PRO-09 thay cho UC-GAM-08 |
| Nhân viên | Chỉ Admin + 3 chức danh (MODERATOR, CS_SPECIALIST, FINANCE_OFFICER), 13 mã quyền; DATN 23 mã quyền, không có Super Admin |
| Trang sản phẩm / shop | ♥ yêu thích, Chia sẻ deeplink (Open Graph), "Hữu ích" ở đánh giá; trang shop có điểm đánh giá, người theo dõi, số sản phẩm, tỉ lệ phản hồi |
| Vận chuyển của shop | Dùng `VendorShippingConfig` sẵn có của DATN (hãng bật, ngưỡng freeship) — không thêm trường vào `VendorProfile` |
| KYC quốc huy / vân tay | Ghi vào `KycResult.riskSignalsJson` — không thêm trường riêng (DATN v2.10 đã bỏ) |
| Thống kê truy cập | Không có UC ANA riêng: lượt xem ghi trong UC-CAT-06, xem trong UC-VND-08 (`ProductViewStat`) |
| Theo dõi tiến độ | `Quản Lý UseCase.xlsx` của DATN đánh "Đã triển khai" (cả dòng chuyển xám) cho 87 mã có trong TLCN; bỏ chọn thì dòng trở lại màu phân hệ |

### 0.7 Cập nhật v1.2 (2026-11-06) — đưa lại UC-AI-07 (PhoBERT)

- **`UC-AI-07` Phân tích cảm xúc đánh giá bằng PhoBERT** đưa lại từ DATN, bản rút gọn: chỉ **cảm xúc tổng thể** 3 nhãn bằng mô hình PhoBERT nhóm đã fine-tune, chạy trong AI Worker có sẵn; xếp vào phân hệ 08 (Chat, Đánh giá & Theo dõi shop), Sprint 6. Phân tích theo khía cạnh vẫn thuộc DATN.
- PhoBERT không còn trong danh sách "công nghệ bị bỏ" (mục 1 là ảnh chụp v1.0); dòng `UC-AI-07` ở mục 4 và dòng `ProductReview.aspectSentimentJson` / `attachSentiment()` ở mục 6.3 nay đã có trong TLCN.
- Con số: 87 / 206 use case, 724 thuộc tính (thêm `aspectSentimentJson`), không thêm bảng / enum; Excel DATN đánh "Đã triển khai" cho 87 mã.

### 0.8 Cập nhật v1.3 (2026-11-08) — đồng bộ đặc tả API

- Viết `document/API_SPEC.md` (API REST theo từng màn hình) và đồng bộ ngược vào tài liệu: LLD 1.1 (mục 5, 7, 8, 12), Document_Vi 3.3 / 3.5 / 3.7.1, implement.md Phần 3 & 17, LUU_TRU_DU_LIEU, TECH_STACK, Usecase.md.
- Mô hình: **65 entity** (thêm `CodRemittance`), **55 enum** (thêm `AddressLabel`, `FlashSaleItemStatus`, `CodRemittanceStatus`), **101 quan hệ**, **202 phương thức**, 9 cột mới; migration `V14__api_sync_v13.sql`. Use case giữ nguyên 87.
- `Genie_ClassDiagram_UML25.xmi` đã cập nhật theo v1.3 (thêm trực tiếp, kiểm tra hợp lệ XML và tham chiếu ID); Excel chưa sửa.

### 0.6 File đã cập nhật ở v1.1

| Bộ | File |
|---|---|
| TLCN | `ENTITY.md`, 10 file `java-code/*.java` (xóa `Diagram07_ReviewChatNotificationAudit.java` cũ), `Genie_ClassDiagram_UML25.xmi`, `Usecase.md`, `UML_UseCase_PlantUML.md`, `Genie_UseCase_UML25.xmi`, `Quản Lý UseCase.xlsx`, `document/Document_Vi.md`, `kien-thuc/*` (4 file), báo cáo này, `BAO_CAO_DOI_CHIEU_GIAO_DIEN.md` |
| DATN | `ENTITY.md` v2.16, 19 file Java, `Lazadee_ClassDiagram_UML25.xmi`, `Usecase.md` (206 UC), `UML_UseCase_PlantUML.md` v1.5, `Lazadee_UseCase_UML25.xmi`, `Quản Lý UseCase.xlsx`, `document/Document_Vi.md`, `implement.md` |

---

## 1. Tóm tắt con số (v1.0 — ảnh chụp lúc tạo bản, số hiện hành ở mục 0.1)

| Hạng mục | Bản đầy đủ | MVP | Giảm |
|---|:---:|:---:|:---:|
| Use case | 200 | 57 | 72% |
| Phân hệ | 23 | 11 | 52% |
| Tác nhân | 44 | 11 | 75% |
| Use case dùng chung (helper) | 6 | 4 | 33% |
| «include» / «extend» | 27 / 16 | 18 / 7 | — |
| Nhóm thực thể | 32 | 12 | 62% |
| Entity / bảng | 162 | 38 | 77% |
| Class `@IdClass` | 11 | 0 | 100% |
| Enum | 124 | 37 | 70% |
| Quan hệ khóa ngoại | 195 | 61 | 69% |
| Sơ đồ lớp (file Java) | 19 | 7 | 63% |
| Thuộc tính (field) | 1660 | 493 | 70% |
| Phương thức nghiệp vụ | 455 | 145 | 68% |

**Công nghệ bị bỏ khỏi MVP:** Apache Kafka, RabbitMQ, Elasticsearch, Qdrant, Kubernetes, Saga, Resilience4j, OSRM / bản đồ, Prometheus / Grafana / Loki, LLM, PhoBERT, CLIP, MoMo, ZaloPay, VietQR, BNPL, Firebase FCM, Zalo ZNS, SSO Google / Apple.
**Công nghệ giữ lại:** Java 21 + Spring Boot 3 (modular monolith), PostgreSQL 16 (thêm full-text `tsvector` + `unaccent` + `pg_trgm`), Redis 7, Silo, Python FastAPI + ONNX (chỉ KYC), Astro + React + Tailwind, VNPay, SePay (chuyển khoản VietQR & xác nhận tiền vào / ra), GHN, SMTP (kênh OTP duy nhất), Docker Compose + Nginx, GitHub Actions.

## 2. Nguyên tắc cắt giảm

1. **Giữ một vòng nghiệp vụ khép kín:** mở shop + KYC → đăng bán → mua nhiều shop → VNPay / VietQR (SePay) / COD → GHN giao → ký quỹ 7 ngày → giải ngân → rút tiền; kèm đánh giá, chat, trả hàng / hoàn tiền, quản trị.
2. **"Use case" kỹ thuật không phải use case UML** (Kafka, Saga, circuit breaker, feature flag, xoay token, khóa brute-force, DLQ, sổ cái…): bỏ khỏi danh mục use case; phần cần thiết chuyển thành yêu cầu phi chức năng (Document_Vi.md mục 1.8).
3. **Giữ nguyên mã use case, tên entity, tên bảng, tên cột, tên enum** của bản đầy đủ → mở rộng sau này chỉ **thêm**; dãy mã UC có khoảng trống là có chủ đích. Phân hệ được đánh số lại 01–11; nhóm thực thể đánh số lại 1–12 (bảng ánh xạ ở ENTITY.md Phụ lục D).
4. **Triển khai vừa sức:** một VPS, Docker Compose, mục tiêu ~50 CCU thay vì 10.000 / 100.000 CCU.

## 3. Use case được CHỌN (57)

| Mã | Tên trong MVP | Phân hệ MVP | Ghi chú |
|---|---|---|---|
| `UC-AUTH-01` | Đăng ký tài khoản Khách hàng | 01. Xác thực & Tài khoản người dùng | Giữ nguyên tên |
| `UC-AUTH-02` | Đăng nhập hệ thống | 01. Xác thực & Tài khoản người dùng | Đổi tên (gốc: Đăng nhập hệ thống (RSA-OAEP & Argon2id (OWASP 2024))) |
| `UC-AUTH-04` | Khôi phục mật khẩu qua Email OTP (Forgot Password) | 01. Xác thực & Tài khoản người dùng | Giữ nguyên tên |
| `UC-AUTH-06` | Đổi / Thiết lập mật khẩu | 01. Xác thực & Tài khoản người dùng | Đổi tên 2026-11-16 theo giao diện (cũ: Đổi mật khẩu tài khoản nội bộ) |
| `UC-AUTH-08` | Quản lý thông tin hồ sơ cá nhân (Customer Profile) | 01. Xác thực & Tài khoản người dùng | Giữ nguyên tên |
| `UC-AUTH-09` | Quản lý sổ địa chỉ giao hàng chuẩn GHN | 01. Xác thực & Tài khoản người dùng | Giữ nguyên tên |
| `UC-AUTH-11` | Đăng xuất & Quản lý thiết bị đăng nhập | 01. Xác thực & Tài khoản người dùng | Đổi tên (gốc DATN: Đăng xuất & Thu hồi phiên trên đa thiết bị; TLCN v1.3: Đăng xuất & Thu hồi phiên đăng nhập; 2026-11-16 theo giao diện) |
| `UC-CAT-01` | Duyệt cây danh mục đa cấp (Category Hierarchy) | 02. Danh mục, Sản phẩm & Tìm kiếm | Giữ nguyên tên |
| `UC-CAT-02` | Tìm kiếm sản phẩm theo từ khóa | 02. Danh mục, Sản phẩm & Tìm kiếm | Đổi tên (gốc: Tìm kiếm sản phẩm toàn văn chịu lỗi chính tả (Elasticsearch 8.15 LTS Fuzzy Search)) |
| `UC-CAT-05` | Lọc nâng cao & Sắp xếp sản phẩm đa tiêu chí | 02. Danh mục, Sản phẩm & Tìm kiếm | Giữ nguyên tên |
| `UC-CAT-06` | Xem chi tiết sản phẩm, ma trận biến thể SKU & Tồn kho | 02. Danh mục, Sản phẩm & Tìm kiếm | Giữ nguyên tên |
| `UC-CAT-08` | Quản lý danh sách sản phẩm yêu thích (Wishlist) | 02. Danh mục, Sản phẩm & Tìm kiếm | Đổi tên (gốc: Quản lý danh sách sản phẩm yêu thích (Wishlist) & Đã xem gần đây) |
| `UC-CAT-09` | Thêm mới sản phẩm & Biến thể SKU (Người bán) | 02. Danh mục, Sản phẩm & Tìm kiếm | Giữ nguyên tên |
| `UC-CAT-10` | Cập nhật thông tin sản phẩm, giá bán & hình ảnh | 02. Danh mục, Sản phẩm & Tìm kiếm | Giữ nguyên tên |
| `UC-CAT-11` | Ẩn / Khóa / Xóa mềm sản phẩm | 02. Danh mục, Sản phẩm & Tìm kiếm | Giữ nguyên tên |
| `UC-CAT-13` | Kiểm duyệt sản phẩm thủ công theo hàng đợi (Staff Moderator) | 02. Danh mục, Sản phẩm & Tìm kiếm | Giữ nguyên tên |
| `UC-CAT-14` | Quản lý danh mục sàn toàn cục & Cấu hình tỷ lệ hoa hồng | 02. Danh mục, Sản phẩm & Tìm kiếm | Giữ nguyên tên |
| `UC-CAT-16` | Xem trang gian hàng (Shop Page) | 02. Danh mục, Sản phẩm & Tìm kiếm | Mới trong MVP |
| `UC-ORD-01` | Quản lý giỏ hàng mua sắm cá nhân (Shopping Cart) | 03. Giỏ hàng, Đặt hàng & Đơn hàng | Giữ nguyên tên |
| `UC-ORD-04` | Xem trước đơn hàng & Tách giỏ hàng đa nhà bán (Cart Preview & Splitting) | 03. Giỏ hàng, Đặt hàng & Đơn hàng | Giữ nguyên tên |
| `UC-ORD-06` | Tính phí vận chuyển qua GHN API | 03. Giỏ hàng, Đặt hàng & Đơn hàng | Đổi tên (gốc: Tính phí vận chuyển đa kiện hàng (GHN API & Genie Express)) |
| `UC-ORD-07` | Tạo phiên Checkout & Các Đơn hàng con theo Shop (PersistCheckout) | 03. Giỏ hàng, Đặt hàng & Đơn hàng | Giữ nguyên tên |
| `UC-ORD-08` | Tự động hủy đơn quá hạn thanh toán & Hoàn kho (Sweeper Cron) | 03. Giỏ hàng, Đặt hàng & Đơn hàng | Giữ nguyên tên |
| `UC-ORD-09` | Khách hàng theo dõi chi tiết hành trình đơn hàng | 03. Giỏ hàng, Đặt hàng & Đơn hàng | Giữ nguyên tên |
| `UC-ORD-10` | Khách hàng yêu cầu hủy đơn hàng | 03. Giỏ hàng, Đặt hàng & Đơn hàng | Giữ nguyên tên |
| `UC-ORD-11` | Khách hàng xác nhận "Đã nhận hàng thành công" | 03. Giỏ hàng, Đặt hàng & Đơn hàng | Giữ nguyên tên |
| `UC-ORD-12` | Hệ thống tự động hoàn tất đơn sau 7 ngày từ khi Delivered | 03. Giỏ hàng, Đặt hàng & Đơn hàng | Đổi tên (gốc: Hệ thống tự động hoàn tất đơn sau 7 ngày từ khi Delivered — **CHUẨN HÓA 168h**) |
| `UC-PAY-01` | Khởi tạo thanh toán trực tuyến (VNPay / chuyển khoản VietQR qua SePay) | 04. Thanh toán & Ký quỹ (Escrow) | Đổi tên (gốc: Khởi tạo thanh toán trực tuyến qua Cổng thanh toán) |
| `UC-PAY-03` | Xử lý IPN VNPay & webhook SePay idempotent | 04. Thanh toán & Ký quỹ (Escrow) | Đổi tên (gốc: Xử lý Webhook / IPN thanh toán Idempotent (HMAC-SHA512)) |
| `UC-PAY-05` | Ghi nhận ký quỹ (Escrow) & Khấu trừ hoa hồng | 04. Thanh toán & Ký quỹ (Escrow) | Đổi tên (gốc: Ghi nhận Sổ Ký quỹ (Escrow Ledger) & Khấu trừ Hoa hồng) |
| `UC-PAY-07` | Tự động giải phóng tiền ký quỹ về ví người bán | 04. Thanh toán & Ký quỹ (Escrow) | Giữ nguyên tên |
| `UC-VND-01` | Đăng ký mở gian hàng (Become a Seller) | 05. Gian hàng & Thẩm định KYC người bán | Giữ nguyên tên |
| `UC-VND-02` | Tải hồ sơ pháp lý & Giấy tờ định danh KYC | 05. Gian hàng & Thẩm định KYC người bán | Giữ nguyên tên |
| `UC-VND-03` | Thẩm định KYC tự động qua AI (QR, MRZ & Face Match) | 05. Gian hàng & Thẩm định KYC người bán | Đổi tên (gốc: Thẩm định KYC tự động qua AI (OCR & Face Match)) |
| `UC-VND-04` | Kiểm duyệt thủ công hồ sơ KYC (Staff / Admin) | 05. Gian hàng & Thẩm định KYC người bán | Giữ nguyên tên |
| `UC-VND-05` | Cấu hình kho lấy hàng & Đồng bộ GHN Shop ID | 05. Gian hàng & Thẩm định KYC người bán | Giữ nguyên tên |
| `UC-VND-07` | Đăng ký tài khoản nhận giải ngân (Payout Bank Account) | 05. Gian hàng & Thẩm định KYC người bán | Giữ nguyên tên |
| `UC-VND-08` | Xem báo cáo doanh thu & Phân tích bán hàng (Vendor Analytics) | 05. Gian hàng & Thẩm định KYC người bán | Giữ nguyên tên |
| `UC-LOG-01` | Tiếp nhận đơn hàng & Đóng gói tại kho shop (Fulfillment) | 06. Vận chuyển qua đối tác GHN | Giữ nguyên tên |
| `UC-LOG-02` | Đẩy vận đơn sang GHN & In phiếu gửi hàng PDF (Waybill Label) | 06. Vận chuyển qua đối tác GHN | Giữ nguyên tên |
| `UC-LOG-03` | Đồng bộ hành trình giao nhận qua Webhook GHN | 06. Vận chuyển qua đối tác GHN | Đổi tên (gốc: Đồng bộ hành trình giao nhận qua Webhook 3PL (GHN)) |
| `UC-PRO-01` | Tạo & Quản lý Voucher Gian hàng (Vendor Shop Vouchers) | 07. Khuyến mãi & Voucher | Giữ nguyên tên |
| `UC-PRO-02` | Tạo & Quản lý Voucher Toàn sàn & Chiến dịch Freeship (Admin) | 07. Khuyến mãi & Voucher | Giữ nguyên tên |
| `UC-PRO-03` | Lưu voucher & Áp voucher khi thanh toán (2 tầng: Shop + Sàn) | 07. Khuyến mãi & Voucher | Đổi tên (gốc: Khách hàng thu thập Voucher vào Kho Voucher cá nhân) |
| `UC-SOC-01` | Nhắn tin trò chuyện thời gian thực Khách - Shop (Live Chat) | 08. Chat & Đánh giá sản phẩm | Giữ nguyên tên |
| `UC-SOC-03` | Đánh giá sản phẩm đã mua & Tải ảnh thực tế (Verified Buyer Review) | 08. Chat & Đánh giá sản phẩm | Giữ nguyên tên |
| `UC-SOC-04` | Người bán phản hồi đánh giá của khách hàng (Vendor Review Reply) | 08. Chat & Đánh giá sản phẩm | Giữ nguyên tên |
| `UC-SUP-01` | Tạo yêu cầu Trả hàng / Hoàn tiền (Return & Refund Request) | 09. Trả hàng, Hoàn tiền & Khiếu nại | Giữ nguyên tên |
| `UC-SUP-02` | Người bán tiếp nhận và xử lý yêu cầu trả hàng | 09. Trả hàng, Hoàn tiền & Khiếu nại | Giữ nguyên tên |
| `UC-SUP-03` | Nhân viên sàn phân xử tranh chấp khiếu nại (Dispute Arbitration) | 09. Trả hàng, Hoàn tiền & Khiếu nại | Giữ nguyên tên |
| `UC-SUP-04` | Thực hiện hoàn tiền (VNPay / VietQR / COD) | 09. Trả hàng, Hoàn tiền & Khiếu nại | Đổi tên (gốc: Thực hiện hoàn tiền (Online / Ví / **COD**) — **Bổ sung case COD**) |
| `UC-NOT-01` | Thông báo trong ứng dụng thời gian thực (WebSocket) | 10. Thông báo | Đổi tên (gốc: Thông báo biến động giao dịch In-App thời gian thực (WebSocket)) |
| `UC-NOT-03` | Gửi email giao dịch tự động | 10. Thông báo | Đổi tên (gốc: Gửi Email giao dịch tự động (Transactional Emails)) |
| `UC-ADM-03` | Khóa / Mở khóa tài khoản nhân viên và người dùng vi phạm | 11. Quản trị sàn & Rút tiền | Giữ nguyên tên |
| `UC-ADM-04` | Người bán gửi yêu cầu rút tiền từ Ví về Ngân hàng | 11. Quản trị sàn & Rút tiền | Giữ nguyên tên |
| `UC-ADM-05` | Nhân viên tài chính phê duyệt & Thực hiện lệnh chuyển khoản (Payout) | 11. Quản trị sàn & Rút tiền | Giữ nguyên tên |
| `UC-ADM-06` | Dashboard doanh thu & ký quỹ toàn sàn | 11. Quản trị sàn & Rút tiền | Đổi tên (gốc: Giám sát Dashboard Báo cáo Doanh thu & Sổ cái Toàn sàn (Admin BI)) |

**Use case dùng chung (không mã):** Gửi mã OTP xác thực, Ghi nhật ký kiểm toán, Tải tệp lên Silo (Presigned URL), Kiểm tra số dư khả dụng.

## 4. Use case bị BỎ (144) và lý do

| Mã | Tên (bản đầy đủ) | Lý do | Ánh xạ trong MVP |
|---|---|---|---|
| `UC-AUTH-03` | Đăng nhập & Liên kết tài khoản Google / Apple (OAuth2 / OpenID Connect) *(tên cũ: Đăng nhập một lần qua Mạng xã hội (SSO OAuth2) — đổi 2026-11-16 theo giao diện)* | SSO Google / Apple — bỏ để đơn giản xác thực (thêm sau khi cần) | `UC-AUTH-02` |
| `UC-AUTH-05` | Làm mới phiên làm việc & Xoay vòng Token (Refresh Token Rotation) | Xoay vòng refresh token — yêu cầu kỹ thuật, chuyển thành NFR-05 (vẫn hiện thực trong UC-AUTH-02) | `UC-AUTH-02` |
| `UC-AUTH-07` | Tự động khóa tài khoản chống Brute-force | Khóa brute-force — yêu cầu kỹ thuật, chuyển NFR-05 (vẫn hiện thực trong UC-AUTH-02) | `UC-AUTH-02` |
| `UC-AUTH-10` | Yêu cầu ẩn danh hóa & Xóa dữ liệu cá nhân (GDPR / PDPA VN) | Ẩn danh hóa tự động theo yêu cầu xóa dữ liệu — MVP xử lý thủ công qua email (báo cáo pháp lý MVP Phần 2) | `UC-AUTH-08` |
| `UC-AUTH-12` | Bật & xác thực đa yếu tố (MFA / TOTP) cho tài khoản đặc quyền — bổ sung v2.8 | MFA / TOTP cho tài khoản đặc quyền — để bản mở rộng | `UC-AUTH-02` |
| `UC-CAT-03` | Tìm kiếm bằng hình ảnh (Visual Search với CLIP & Qdrant) | Tìm kiếm bằng hình ảnh (CLIP + Qdrant) | `UC-CAT-02` |
| `UC-CAT-04` | Gợi ý từ khóa tìm kiếm & Xu hướng (Autocomplete & Trending) | Gợi ý từ khóa / xu hướng — cần dữ liệu tìm kiếm | `UC-CAT-02` |
| `UC-CAT-07` | Xem gợi ý sản phẩm tương tự (Semantic Similarity Qdrant + Ollama) | Sản phẩm tương tự (Qdrant + LLM) | `UC-CAT-06` |
| `UC-CAT-12` | Kiểm duyệt sản phẩm tự động qua AI (Worker Service) | AI kiểm duyệt ảnh — thay bằng Staff duyệt (UC-CAT-13) | `UC-CAT-13` |
| `UC-CAT-15` | Cấu hình bộ thuộc tính chuẩn theo ngành hàng (Category Attributes) — bổ sung v2.8 | Bộ thuộc tính chuẩn theo ngành — MVP dùng `specsJson` tự do | `UC-CAT-14` |
| `UC-ORD-02` | Giỏ hàng chia sẻ mua sắm nhóm (Shared Cart & Group Buying) | Giỏ hàng chia sẻ / mua nhóm | `UC-ORD-01` |
| `UC-ORD-03` | Giỏ hàng ngoại tuyến PWA & Tự động đồng bộ nền (Offline Cart) | Giỏ hàng offline PWA | `UC-ORD-01` |
| `UC-ORD-05` | Giữ chỗ tồn kho mềm nguyên tử (Soft-lock Stock Reservation) | Giữ chỗ tồn kho — bước nội bộ, gộp vào UC-ORD-07 (không có tác nhân) | `UC-ORD-07` |
| `UC-PAY-02` | Điều hướng cổng thanh toán động (Dynamic Gateway Router & Failover) | Điều hướng đa cổng & failover — MVP chỉ VNPay | `UC-PAY-01` |
| `UC-PAY-04` | Trừ tồn kho cứng & Chuyển trạng thái đơn PAID | Trừ tồn cứng & PAID — bước nội bộ, gộp vào UC-PAY-03 | `UC-PAY-03` |
| `UC-PAY-06` | Ghi sổ cái tài chính kép bất biến (Financial Ledger) | Sổ cái kép — gộp vào ghi sổ ví shop (`WalletTransaction`) và ký quỹ | `UC-PAY-07` |
| `UC-PAY-08` | Đóng băng số dư ký quỹ khi phát sinh tranh chấp | Đóng băng ký quỹ — bước nội bộ của UC-SUP-01 | `UC-SUP-03` |
| `UC-PAY-09` | Xác nhận nội bộ đơn hàng nhỏ / 0đ | Xác nhận đơn 0đ — gộp thành luồng thay thế của UC-PAY-01 | `UC-PAY-03` |
| `UC-PAY-10` | Khấu trừ & nộp thuế thay cho hộ, cá nhân kinh doanh (Nghị định 117/2025) | Khấu trừ thuế thay (NĐ 117/2025) — **bắt buộc bổ sung trước khi thương mại** | — |
| `UC-PAY-11` | Thanh toán Mua trước trả sau qua đối tác tài chính (BNPL Checkout) | BNPL | — |
| `UC-PAY-12` | Đối soát, hủy & hoàn tiền giao dịch BNPL với đối tác (BNPL Settlement & Refund) | Đối soát BNPL | — |
| `UC-PAY-13` | Đối soát giao dịch với cổng thanh toán & xử lý tra soát (Gateway Reconciliation) — bổ sung v2.8 | Đối soát tự động với cổng — MVP đối chiếu thủ công trên cổng merchant VNPay | — |
| `UC-WAL-01` | Kích hoạt ví khách hàng & Thiết lập mã PIN 6 số | Ví khách hàng / thẻ quà tặng / hóa đơn điện tử — cần giấy phép trung gian thanh toán, ngoài phạm vi (ADR-04) | — |
| `UC-WAL-02` | Nạp tiền vào ví khách hàng (Wallet Top-up) | Ví khách hàng / thẻ quà tặng / hóa đơn điện tử — cần giấy phép trung gian thanh toán, ngoài phạm vi (ADR-04) | — |
| `UC-WAL-03` | Thanh toán đơn hàng bằng số dư ví khách hàng | Ví khách hàng / thẻ quà tặng / hóa đơn điện tử — cần giấy phép trung gian thanh toán, ngoài phạm vi (ADR-04) | — |
| `UC-WAL-04` | Thanh toán chia nguồn tiền kết hợp (Split Payment) | Ví khách hàng / thẻ quà tặng / hóa đơn điện tử — cần giấy phép trung gian thanh toán, ngoài phạm vi (ADR-04) | — |
| `UC-WAL-05` | Hoàn tiền tức thì vào ví khách hàng (Instant Refund Engine) | Ví khách hàng / thẻ quà tặng / hóa đơn điện tử — cần giấy phép trung gian thanh toán, ngoài phạm vi (ADR-04) | — |
| `UC-WAL-06` | Nạp và áp dụng Thẻ quà tặng điện tử (Gift Card Redemption) | Ví khách hàng / thẻ quà tặng / hóa đơn điện tử — cần giấy phép trung gian thanh toán, ngoài phạm vi (ADR-04) | — |
| `UC-WAL-07` | Tra cứu số dư ví & Lịch sử biến động số dư chi tiết | Ví khách hàng / thẻ quà tặng / hóa đơn điện tử — cần giấy phép trung gian thanh toán, ngoài phạm vi (ADR-04) | — |
| `UC-WAL-08` | Xuất hóa đơn giá trị gia tăng điện tử (e-Invoice) | Ví khách hàng / thẻ quà tặng / hóa đơn điện tử — cần giấy phép trung gian thanh toán, ngoài phạm vi (ADR-04) | — |
| `UC-WAL-09` | Rút tiền từ ví khách hàng về tài khoản ngân hàng (Wallet Withdrawal) | Ví khách hàng / thẻ quà tặng / hóa đơn điện tử — cần giấy phép trung gian thanh toán, ngoài phạm vi (ADR-04) | — |
| `UC-VND-06` | Cấu hình tin nhắn trả lời tự động (Auto-reply) | Trả lời tự động trong chat | `UC-SOC-01` |
| `UC-VND-09` | Đăng ký Gói thành viên Người bán VIP (Vendor Subscriptions) | Gói thành viên VIP | — |
| `UC-VND-10` | Quản lý điểm vận hành & Xử lý phạt Sao Quả Tạ (Vendor Penalties) | Điểm phạt Sao Quả Tạ | — |
| `UC-VND-11` | Quảng cáo tìm kiếm nội sàn trả phí theo click (Sponsored Ads PPC) | Quảng cáo PPC | — |
| `UC-VND-12` | Cập nhật tồn kho & giá bán hàng loạt qua tệp Excel (Bulk Import/Export) | Nhập / xuất Excel hàng loạt | — |
| `UC-VND-13` | Ký hợp đồng điện tử & chấp thuận Quy chế hoạt động sàn (Vendor E-Contract) | Hợp đồng điện tử có lưu phiên bản — MVP tick đồng ý điều khoản | — |
| `UC-VND-14` | Trang trí trang chủ gian hàng: banner trượt, khối sản phẩm nổi bật, video giới thiệu (Shop Decoration) | Trang trí gian hàng | — |
| `UC-VND-15` | Quản lý danh mục riêng của shop & sắp xếp sản phẩm (Shop Categories) | Danh mục riêng của shop | — |
| `UC-VND-16` | Quản lý đa kho, tồn kho theo kho & phân bổ kho xuất hàng (Multi-Warehouse Inventory) | Đa kho — MVP một kho / shop | `UC-VND-05` |
| `UC-VND-17` | Quản lý tài khoản phụ nhân viên shop & phân quyền chi tiết (Vendor Sub-accounts) | Tài khoản phụ nhân viên shop | `UC-VND-01` |
| `UC-VND-18` | Shop mua, gia hạn & gắn vật phẩm trang trí (khung avatar, linh vật, giao diện shop) | Vật phẩm trang trí | `UC-ADM-04` |
| `UC-VND-19` | Bật chế độ tạm nghỉ bán hàng của shop (Vacation Mode) — bổ sung v2.8 | Chế độ tạm nghỉ | `UC-VND-05` |
| `UC-LOG-04` | Ghép tài xế Genie Express tự động (Shipper Matching Engine) | Ghép tài xế Genie Express | `UC-LOG-02` |
| `UC-LOG-05` | Tối ưu tuyến đường giao hàng đa điểm (VRPTW Algorithm) | Tối ưu tuyến VRPTW | — |
| `UC-LOG-06` | Tài xế nhận đơn & Quét barcode qua PWA Shipper App | App tài xế PWA | `UC-LOG-03` |
| `UC-LOG-07` | Cập nhật tọa độ GPS tài xế thời gian thực qua WebSocket | GPS realtime tài xế | — |
| `UC-LOG-08` | Khách hàng xem bản đồ lộ trình di chuyển trực tiếp của tài xế | Bản đồ theo dõi tài xế | — |
| `UC-LOG-09` | Xử lý giao thất bại & Lập lịch giao lại (Delivery Retry Machine) | Giao thất bại Express — GHN tự xử lý, trạng thái qua webhook (UC-LOG-03) | `UC-LOG-03` |
| `UC-LOG-10` | Quản lý tiền thu hộ COD & Đối soát ký quỹ tài xế + **Handoff sang Escrow** | COD tài xế nội bộ — thay bằng đối soát COD với GHN trong UC-LOG-03 | `UC-LOG-03` |
| `UC-LOG-11` | Trung tâm điều hành giám sát vận chuyển Admin (Heatmap Dispatch) | Trung tâm điều phối Express | — |
| `UC-LOG-12` | Đăng ký đối tác tài xế Genie Express, ký hợp đồng dịch vụ & nạp ký quỹ (Shipper Onboarding) | Tuyển tài xế & ký quỹ | — |
| `UC-LOG-13` | Quản lý thu nhập, thưởng/phạt & rút tiền ví tài xế (Shipper Earnings & Payout) | Ví thu nhập tài xế | — |
| `UC-LOG-14` | Khách hàng đánh giá tài xế sau khi nhận hàng (Shipper Rating) | Đánh giá tài xế | — |
| `UC-LOG-15` | Điều phối đa hãng vận chuyển & Nhập/Xuất Hub trung chuyển (Dynamic Carrier Routing & Hub-and-Spoke) | Đa hãng & Hub — MVP chỉ GHN | `UC-LOG-02` |
| `UC-PRO-04` | Thuật toán Stacking Voucher 4 Tầng tự động khi Checkout | Xếp chồng voucher 4 tầng — MVP 2 tầng, gộp vào UC-PRO-03 | `UC-PRO-03` |
| `UC-PRO-05` | Tạo chiến dịch Flash Sale & Cấu hình khung giờ vàng | Tạo Flash Sale | — |
| `UC-PRO-06` | Săn deal Flash Sale nguyên tử tốc độ cao bằng Redis Lua Script | Săn Flash Sale (Redis Lua) | — |
| `UC-PRO-07` | Cấu hình Khuyến mãi mua kèm, Combo Deal & Bundle Deal | Combo / Bundle deal | — |
| `UC-PRO-08` | Điểm danh nhận Điểm hàng ngày & Duy trì chuỗi điểm danh (Streak) | Điểm danh nhận Điểm | — |
| `UC-PRO-09` | Tra cứu số dư Điểm Genie & Lịch sử biến động Điểm | Số dư Điểm | — |
| `UC-PRO-10` | Sử dụng Điểm Genie để giảm trừ thanh toán đơn hàng | Dùng Điểm giảm giá | `UC-PRO-03` |
| `UC-GAM-01` | Hệ thống Thứ bậc Khách hàng (Loyalty Tiers) | Gamification, Điểm, affiliate, livestream — tính năng tăng trưởng, chưa cần để chạy vòng nghiệp vụ cốt lõi | — |
| `UC-GAM-02` | Thực hiện Nhiệm vụ Hàng ngày nhận thưởng (Daily Missions) | Gamification, Điểm, affiliate, livestream — tính năng tăng trưởng, chưa cần để chạy vòng nghiệp vụ cốt lõi | — |
| `UC-GAM-03` | Vòng quay may mắn (Lucky Wheel với Server-side Alias Method) | Gamification, Điểm, affiliate, livestream — tính năng tăng trưởng, chưa cần để chạy vòng nghiệp vụ cốt lõi | — |
| `UC-GAM-04` | Mở khóa Huy hiệu & Xem Bảng xếp hạng (Leaderboards) | Gamification, Điểm, affiliate, livestream — tính năng tăng trưởng, chưa cần để chạy vòng nghiệp vụ cốt lõi | — |
| `UC-GAM-05` | Chương trình Giới thiệu bạn bè nhận Voucher (Referral Program) | Gamification, Điểm, affiliate, livestream — tính năng tăng trưởng, chưa cần để chạy vòng nghiệp vụ cốt lõi | — |
| `UC-GAM-06` | Tiếp thị liên kết dành cho KOL/KOC (Affiliate Marketing) | Gamification, Điểm, affiliate, livestream — tính năng tăng trưởng, chưa cần để chạy vòng nghiệp vụ cốt lõi | — |
| `UC-GAM-07` | Livestream bán hàng tương tác trực tiếp (Live Stream Session) | Gamification, Điểm, affiliate, livestream — tính năng tăng trưởng, chưa cần để chạy vòng nghiệp vụ cốt lõi | — |
| `UC-GAM-08` | Khách hàng đổi Điểm lấy vật phẩm trang trí & gắn lên hồ sơ (Cosmetic Redemption) | Gamification, Điểm, affiliate, livestream — tính năng tăng trưởng, chưa cần để chạy vòng nghiệp vụ cốt lõi | — |
| `UC-SOC-02` | Gửi thẻ sản phẩm, hình ảnh và trạng thái đơn hàng trong chat | Thẻ sản phẩm / đơn hàng trong chat | `UC-SOC-01` |
| `UC-SOC-05` | Báo cáo đánh giá vi phạm / spam (Review Moderation & Report) | Báo cáo đánh giá vi phạm — Staff ẩn đánh giá trực tiếp | `UC-SOC-03` |
| `UC-SOC-06` | Khách hàng theo dõi gian hàng (Shop Follow) | Theo dõi shop | — |
| `UC-SOC-07` | Hỏi đáp sản phẩm công khai & Sinh FAQ Schema (Product Q&A) | Hỏi đáp sản phẩm công khai | — |
| `UC-SOC-08` | Mua chung theo nhóm hưởng giá chiết khấu trong 24 giờ (Group Buying) | Mua chung theo nhóm | — |
| `UC-SUP-05` | Khách hàng tạo Ticket hỗ trợ kỹ thuật / khiếu nại (Support Ticket) | Ticket hỗ trợ SLA | `UC-SUP-01` |
| `UC-SUP-06` | Điều phối Ticket tự động theo ma trận SLA & Mức độ ưu tiên | Điều phối ticket SLA | `UC-SUP-03` |
| `UC-SUP-07` | Phân tích cảm xúc Ticket bằng PhoBERT & Trợ lý Chatbot AI | Phân tích cảm xúc ticket & chatbot | — |
| `UC-NOT-02` | Gửi thông báo đẩy Web Push qua Firebase Cloud Messaging (FCM) | Web Push FCM — MVP dùng in-app + email | `UC-NOT-01` |
| `UC-NOT-04` | Gửi tin nhắn Zalo Notification Service (ZNS) | Zalo ZNS — chi phí cao (báo cáo dự toán) | `UC-NOT-01` |
| `UC-NOT-05` | Admin phát thông báo toàn sàn (Broadcast Platform Notification) | Phát thông báo toàn sàn | `UC-NOT-01` |
| `UC-NOT-06` | Cài đặt tùy chọn nhận tin của người dùng (Preferences & Opt-out) | Tùy chọn nhận tin | `UC-NOT-01` |
| `UC-NOT-07` | Xử lý lỗi thông báo & Hàng đợi chết (DLQ & Exponential Backoff) | DLQ & backoff — yêu cầu kỹ thuật (retry đơn giản trong UC-NOT-03) | `UC-NOT-03` |
| `UC-NOT-08` | Nhắc giỏ hàng bị bỏ quên & Tôn trọng khung giờ yên lặng (Abandoned Cart & Quiet Hours) | Nhắc giỏ hàng bỏ quên | — |
| `UC-AI-01` | Trợ lý mua sắm AI tương tác đối thoại (Agentic Shopping Assistant) | AI ngữ cảnh / trợ lý / phân tích — cần dữ liệu & hạ tầng lớn; MVP chỉ giữ AI cho KYC | — |
| `UC-AI-02` | Khảo sát sở thích ban đầu (Zero-Party Data Onboarding) | AI ngữ cảnh / trợ lý / phân tích — cần dữ liệu & hạ tầng lớn; MVP chỉ giữ AI cho KYC | — |
| `UC-AI-03` | Mua sắm theo ngữ cảnh thời tiết & vị trí (Ambient Commerce) | AI ngữ cảnh / trợ lý / phân tích — cần dữ liệu & hạ tầng lớn; MVP chỉ giữ AI cho KYC | — |
| `UC-AI-04` | Đo lường tâm trạng mua sắm trên Client (Client-Side Mood Detection) | AI ngữ cảnh / trợ lý / phân tích — cần dữ liệu & hạ tầng lớn; MVP chỉ giữ AI cho KYC | — |
| `UC-AI-05` | AI tự động tạo mô tả sản phẩm cho Người bán (AI Content Generator) | AI ngữ cảnh / trợ lý / phân tích — cần dữ liệu & hạ tầng lớn; MVP chỉ giữ AI cho KYC | — |
| `UC-AI-06` | Dự báo xu hướng & Nguy cơ khách hàng rời bỏ (Trend & Churn Prediction) | AI ngữ cảnh / trợ lý / phân tích — cần dữ liệu & hạ tầng lớn; MVP chỉ giữ AI cho KYC | — |
| `UC-AI-07` | Phân tích cảm xúc đánh giá theo khía cạnh bằng PhoBERT (Aspect-Based Sentiment Analysis) | AI ngữ cảnh / trợ lý / phân tích — cần dữ liệu & hạ tầng lớn; MVP chỉ giữ AI cho KYC | — |
| `UC-ADM-01` | Quản lý danh sách nhân viên nội bộ & Lời mời kích hoạt | Quản lý nhân viên & lời mời — MVP tạo tài khoản Staff bằng seed / Admin | `UC-ADM-03` |
| `UC-ADM-02` | Phân cấp vai trò & Cấp phát / Thu hồi 18 mã quyền chi tiết (RBAC) | RBAC 18 quyền chi tiết — MVP chỉ ROLE_STAFF / ROLE_ADMIN | `UC-ADM-03` |
| `UC-ADM-07` | Tra cứu nhật ký kiểm toán hệ thống bất biến (System Audit Logs) | Tra cứu nhật ký kiểm toán có giao diện — MVP ghi `system_logs`, tra cứu bằng SQL | `UC-ADM-03` |
| `UC-ADM-08` | Tiếp nhận cảnh báo sự cố vận hành khẩn cấp (Panic Alert) | Cảnh báo sự cố khẩn cấp | — |
| `UC-ADM-09` | Cấu hình tham số hệ thống & Bật/Tắt tính năng động (Feature Flags) | Tham số hệ thống & feature flag | `UC-ADM-06` |
| `UC-ADM-10` | Tiếp nhận & xử lý báo cáo vi phạm sở hữu trí tuệ, hàng giả trong 24 giờ (Notice-and-Takedown) | Gỡ vi phạm SHTT 24 giờ — MVP nhận qua email | — |
| `UC-ADM-11` | Quản lý danh mục vật phẩm trang trí & mở bán theo sự kiện (Cosmetic Catalog Management) | Danh mục vật phẩm trang trí | — |
| `UC-ADM-12` | Quản lý banner trang chủ & chiến dịch tĩnh (CMS Banner) — bổ sung v2.8 | CMS banner — MVP banner tĩnh trong mã | — |
| `UC-SEC-01` | Quản lý & Thu thập sự đồng ý xử lý dữ liệu cá nhân (PDPA VN) | Chống gian lận tự động & PDPA nâng cao — MVP dùng KYC, ký quỹ, giới hạn voucher và xử lý thủ công | — |
| `UC-SEC-02` | Ghi nhận nhật ký kiểm toán đồng ý PDPA bất biến | Chống gian lận tự động & PDPA nâng cao — MVP dùng KYC, ký quỹ, giới hạn voucher và xử lý thủ công | — |
| `UC-SEC-03` | Thu thập Dấu vân tay thiết bị & Chấm điểm rủi ro phiên (Fingerprinting) | Chống gian lận tự động & PDPA nâng cao — MVP dùng KYC, ký quỹ, giới hạn voucher và xử lý thủ công | — |
| `UC-SEC-04` | Ngăn chặn Bot và công cụ tự động săn Deal Flash Sale | Chống gian lận tự động & PDPA nâng cao — MVP dùng KYC, ký quỹ, giới hạn voucher và xử lý thủ công | — |
| `UC-SEC-05` | Phát hiện và xử lý gian lận Voucher & Đơn hàng ảo | Chống gian lận tự động & PDPA nâng cao — MVP dùng KYC, ký quỹ, giới hạn voucher và xử lý thủ công | `UC-SUP-03` |
| `UC-EXP-01` | Định nghĩa Thử nghiệm A/B & Cấu hình Biến thể (Experiment Definition) | A/B testing — chỉ có ý nghĩa khi có lưu lượng thật | — |
| `UC-EXP-02` | Phân bổ Người dùng vào Biến thể theo Hash Phân đoạn Đồng nhất (Variant Bucket Assignment) | A/B testing — chỉ có ý nghĩa khi có lưu lượng thật | — |
| `UC-EXP-03` | Thu thập & Ghi nhận Sự kiện Chuyển đổi Thử nghiệm (Conversion Event Tracking) | A/B testing — chỉ có ý nghĩa khi có lưu lượng thật | — |
| `UC-EXP-04` | Tính toán Kiểm định Thống kê & Mức độ Ý nghĩa (Statistical Significance Calculation) | A/B testing — chỉ có ý nghĩa khi có lưu lượng thật | — |
| `UC-EXP-05` | Tự động Rollout Biến thể Chiến thắng & Đóng Thử nghiệm (Winner Rollout & Conclude) | A/B testing — chỉ có ý nghĩa khi có lưu lượng thật | — |
| `UC-REC-01` | Huấn luyện & Sinh Vector Nhúng Người dùng và Sản phẩm (Embedding Generation) | Gợi ý cá nhân hóa — cần dữ liệu hành vi và Vector DB | — |
| `UC-REC-02` | Gợi ý Sản phẩm Tương tự theo Ngữ cảnh Trang (Similar Items Recommendation) | Gợi ý cá nhân hóa — cần dữ liệu hành vi và Vector DB | — |
| `UC-REC-03` | Gợi ý Danh sách Sản phẩm Dành riêng cho Bạn (Personalized Feed - "For You") | Gợi ý cá nhân hóa — cần dữ liệu hành vi và Vector DB | — |
| `UC-REC-04` | Đóng gói Bundle Thường Mua Cùng Nhau (Frequently Bought Together) | Gợi ý cá nhân hóa — cần dữ liệu hành vi và Vector DB | — |
| `UC-REC-05` | Theo dõi Chỉ số Hiệu quả Gợi ý (Recommendation Performance Analytics) | Gợi ý cá nhân hóa — cần dữ liệu hành vi và Vector DB | — |
| `UC-ANA-01` | Thu thập Sự kiện Clickstream qua SDK Trình duyệt/Mobile (Client Event Ingestion) | Clickstream / analytics thời gian thực — dashboard SQL đủ cho MVP | — |
| `UC-ANA-02` | Đẩy Sự kiện vào Hàng đợi Kafka & Lưu trữ Phân vùng PostgreSQL (Streaming Ingestion & Partitioning) | Clickstream / analytics thời gian thực — dashboard SQL đủ cho MVP | — |
| `UC-ANA-03` | Tổng hợp Số liệu Bán hàng & Doanh thu Thời gian thực (Real-time Metric Aggregation) | Clickstream / analytics thời gian thực — dashboard SQL đủ cho MVP | — |
| `UC-ANA-04` | Phân tích Phễu Chuyển đổi Mua hàng (Funnel Analytics) | Clickstream / analytics thời gian thực — dashboard SQL đủ cho MVP | — |
| `UC-ANA-05` | Xuất Báo cáo Phân tích Hành vi & Hiệu suất Sàn (Analytics Data Export) | Clickstream / analytics thời gian thực — dashboard SQL đủ cho MVP | — |
| `UC-CUR-01` | Quản lý Danh mục Tiền tệ & Cấu hình Tỷ giá Hối đoái (Currency Configuration) | Đa tiền tệ — MVP chỉ VND | — |
| `UC-CUR-02` | Cập nhật Tỷ giá Hối đoái Tự động từ Nguồn Trung ương (Automated FX Rate Sync) | Đa tiền tệ — MVP chỉ VND | — |
| `UC-CUR-03` | Hiển thị Giá Sản phẩm Đã Quy đổi theo Tiền tệ Khách hàng (Dynamic Currency Display) | Đa tiền tệ — MVP chỉ VND | — |
| `UC-CUR-04` | Cấu hình Bảng giá Động theo Khu vực Địa lý (Geo-pricing Rule Configuration) | Đa tiền tệ — MVP chỉ VND | — |
| `UC-CUR-05` | Khóa Tỷ giá & Thanh toán Chuyển đổi Ngoại tệ tại Checkout (FX Lock & Settlement) | Đa tiền tệ — MVP chỉ VND | — |
| `UC-B2B-01` | Đăng ký Hồ sơ Doanh nghiệp B2B & Cấp Hạn mức Tín dụng (B2B Corporate Onboarding) | Cổng B2B bán sỉ — mô hình kinh doanh khác | — |
| `UC-B2B-02` | Cấu hình Bảng giá Bán buôn Phân cấp theo Số lượng (Tiered Wholesale Pricing) | Cổng B2B bán sỉ — mô hình kinh doanh khác | — |
| `UC-B2B-03` | Tạo Yêu cầu Báo giá & Đàm phán Hợp đồng B2B (Request for Quotation - RFQ) | Cổng B2B bán sỉ — mô hình kinh doanh khác | — |
| `UC-B2B-04` | Đặt Hàng Mua sỉ & Thanh toán qua Hạn mức Công nợ (Wholesale Order via Credit Line) | Cổng B2B bán sỉ — mô hình kinh doanh khác | — |
| `UC-B2B-05` | Đối soát Công nợ B2B & Quản lý Hóa đơn VAT Doanh nghiệp (B2B Settlement & VAT Invoice) | Cổng B2B bán sỉ — mô hình kinh doanh khác | — |
| `UC-FF-01` | Tạo Mới Cờ Tính năng & Cấu hình Chiến lược Kích hoạt (Feature Flag Creation) | Feature flag — use case kỹ thuật, chuyển NFR / để bản mở rộng | — |
| `UC-FF-02` | Đánh giá Cờ Tính năng Thời gian thực qua In-Memory Cache (High-Performance Flag Evaluation) | Feature flag — use case kỹ thuật, chuyển NFR / để bản mở rộng | — |
| `UC-FF-03` | Triển khai Cờ Tính năng theo Tỷ lệ % (Canary Rollout Strategy) | Feature flag — use case kỹ thuật, chuyển NFR / để bản mở rộng | — |
| `UC-FF-04` | Bật Tắt Khẩn cấp Tính năng (Emergency Kill-Switch Trigger) | Feature flag — use case kỹ thuật, chuyển NFR / để bản mở rộng | — |
| `UC-FF-05` | Kiểm toán Lịch sử Đổi Cờ & Phân tích Tác động Hệ thống (Audit & Impact Analysis) | Feature flag — use case kỹ thuật, chuyển NFR / để bản mở rộng | — |
| `UC-KFK-01` | Xuất Bản Sự kiện Nghiệp vụ vào Kafka Topic Đảm bảo Thứ tự (Ordered Event Publishing) | Kafka — không có tác nhân chủ động (không phải use case UML); MVP không dùng message broker | — |
| `UC-KFK-02` | Tiêu thụ Sự kiện Bất đồng bộ với Idempotency Consumer (Idempotent Event Consumption) | Kafka — không có tác nhân chủ động (không phải use case UML); MVP không dùng message broker | — |
| `UC-KFK-03` | Xử lý Hàng đợi Lỗi Phân tầng & Dead Letter Queue (Tiered Error Handling & DLQ) | Kafka — không có tác nhân chủ động (không phải use case UML); MVP không dùng message broker | — |
| `UC-KFK-04` | Đồng bộ Dữ liệu Đơn hàng & Tồn kho qua Kafka CDC (Change Data Capture Sync) | Kafka — không có tác nhân chủ động (không phải use case UML); MVP không dùng message broker | — |
| `UC-KFK-05` | Giám sát Lag Người tiêu dùng (Consumer Lag) & Cảnh báo Tắc nghẽn (Consumer Lag Monitoring) | Kafka — không có tác nhân chủ động (không phải use case UML); MVP không dùng message broker | — |
| `UC-RES-01` | Ngắt mạch Tự động khi Dịch vụ Đối tác Quá tải (Circuit Breaker & Fallback) | Saga / circuit breaker / rate limit / tracing — yêu cầu phi chức năng, không phải use case UML | — |
| `UC-RES-02` | Điều phối Giao dịch Phân tán Đặt hàng qua Saga Orchestrator (Saga Workflow Orchestration) | Saga / circuit breaker / rate limit / tracing — yêu cầu phi chức năng, không phải use case UML | — |
| `UC-RES-03` | Kích hoạt Giao dịch Bù trừ khi Đặt hàng Thất bại (Compensating Transactions on Failure) | Saga / circuit breaker / rate limit / tracing — yêu cầu phi chức năng, không phải use case UML | — |
| `UC-RES-04` | Giới hạn Tần suất Truy cập Đa tầng Chống Nghẽn (Multi-tier Rate Limiting & Bulkhead) | Saga / circuit breaker / rate limit / tracing — yêu cầu phi chức năng, không phải use case UML | — |
| `UC-RES-05` | Truy vết Phân tán Giao dịch E2E qua Trace ID (End-to-End Distributed Tracing) | Saga / circuit breaker / rate limit / tracing — yêu cầu phi chức năng, không phải use case UML | — |

> Cột *Ánh xạ trong MVP*: chức năng gần nhất còn lại (dùng khi chuyển tham chiếu trong javadoc / tài liệu).

## 5. Tác nhân

| Tác nhân bản đầy đủ | MVP |
|---|---|
| `ACT-GST` | `ACT-GST` |
| `ACT-CUS` | `ACT-CUS` |
| `ACT-VND` | `ACT-VND` |
| `ACT-STF` | `ACT-STF` |
| `ACT-ADM` | `ACT-ADM` |
| `ACT-KYC` | `ACT-STF` |
| `ACT-MOD` | `ACT-STF` |
| `ACT-CS` | `ACT-STF` |
| `ACT-FIN` | `ACT-STF` |
| `ACT-MKT` | `ACT-STF` |
| `ACT-DSP` | Bỏ |
| `ACT-RSK` | Bỏ |
| `ACT-CMP` | Bỏ |
| `ACT-CRD` | Bỏ |
| `ACT-TEC` | Bỏ |
| `ACT-VST` | Bỏ |
| `ACT-SHP` | Bỏ |
| `ACT-B2B` | Bỏ |
| `ACT-IPH` | Bỏ |
| `ACT-PGW` | `ACT-PGW` |
| `ACT-3PL` | `ACT-3PL` |
| `ACT-MIN` | `ACT-MIN` |
| `ACT-AIW` | `ACT-AIW` |
| `ACT-EML` | `ACT-EML` |
| `ACT-CRN` | `ACT-CRN` |
| `ACT-QDR` | Bỏ |
| `ACT-NOT` | Bỏ |
| `ACT-KFK` | Bỏ |
| `ACT-ENG` | Bỏ |
| `ACT-FRD` | Bỏ |
| `ACT-ELS` | Bỏ |
| `ACT-IDP` | Bỏ |
| `ACT-FCM` | Bỏ |
| `ACT-ZNS` | Bỏ |
| `ACT-BNK` | Bỏ |
| `ACT-TAX` | Bỏ |
| `ACT-BNPL` | Bỏ |
| `ACT-WTH` | Bỏ |
| `ACT-FX` | Bỏ |
| `ACT-CAP` | Bỏ |
| `ACT-LLM` | Bỏ |
| `ACT-SVC` | Bỏ |
| `ACT-MON` | Bỏ |
| `ACT-K8S` | Bỏ |

## 6. Entity: giữ 38 / 162

### 6.1 Entity được giữ (theo nhóm MVP)

| Nhóm MVP | Entity (mục bản đầy đủ) |
|---|---|
| 1. NHÓM NGƯỜI DÙNG & ĐỊNH DANH | `User` (1.1), `CustomerProfile` (1.2), `Address` (1.3), `VendorProfile` (1.4), `AdminProfile` (1.5), `KycResult` (1.6), `RefreshToken` (1.8) |
| 2. NHÓM DANH MỤC & SẢN PHẨM | `Category` (2.1), `Product` (2.2), `ProductVariant` (2.3), `ProductImage` (2.4), `UserWishlist` (18.7) |
| 3. NHÓM GIỎ HÀNG | `Cart` (3.1), `CartItem` (3.2) |
| 4. NHÓM KHUYẾN MÃI / VOUCHER | `Voucher` (4.1), `VoucherUsage` (4.3), `UserVoucher` (4.4) |
| 5. NHÓM ĐƠN HÀNG | `CheckoutSession` (5.1), `Order` (5.2), `OrderItem` (5.3), `OrderDiscount` (5.4), `StockReservation` (5.5) |
| 6. NHÓM THANH TOÁN & KÝ QUỸ | `PaymentTransaction` (6.1), `PaymentRefund` (6.2), `EscrowLedger` (6.3), `VendorWallet` (6.4), `WalletTransaction` (6.5), `WithdrawalRequest` (6.6) |
| 7. NHÓM VẬN CHUYỂN | `ShipmentTracking` (7.1), `ShipmentEvent` (7.2) |
| 8. NHÓM HOÀN TIỀN / KHIẾU NẠI | `RefundRequest` (8.1), `RefundRequestItem` (8.2), `RefundEvidence` (8.3) |
| 9. NHÓM ĐÁNH GIÁ SẢN PHẨM | `ProductReview` (9.1) |
| 10. NHÓM THÔNG BÁO | `Notification` (11.1) |
| 11. NHÓM NHẮN TIN (CHAT) | `Conversation` (12.1), `Message` (12.2) |
| 12. NHÓM NHẬT KÝ HỆ THỐNG | `SystemLog` (14.1) |

### 6.2 Entity bị bỏ (124)

| Nhóm bản đầy đủ | Entity bị bỏ |
|---|---|
| 1. NHÓM NGƯỜI DÙNG & ĐỊNH DANH | `StaffPermission` |
| 2. NHÓM DANH MỤC & SẢN PHẨM | `CategoryAttribute` |
| 4. NHÓM KHUYẾN MÃI / VOUCHER | `VoucherCategory`, `VoucherExcludedCategory`, `VoucherExcludedProduct`, `VoucherProduct` |
| 6. NHÓM THANH TOÁN & TÀI CHÍNH | `FinancialLedger`, `TaxWithholdingRecord` |
| 7. NHÓM VẬN CHUYỂN | `VendorShippingConfig` |
| 9. NHÓM ĐÁNH GIÁ & THEO DÕI SHOP | `ProductQuestion`, `ReviewReport`, `ShopFollow` |
| 10. NHÓM ĐIỂM THƯỞNG | `CoinTransaction`, `DailyCheckin`, `UserCoin` |
| 11. NHÓM THÔNG BÁO | `AdminNotification`, `DeviceToken`, `PlatformNotification`, `UserNotificationPreference`, `UserPlatformNotificationRead` |
| 13. NHÓM FLASH SALE | `FlashSaleCampaign`, `FlashSaleItem` |
| 14. NHÓM NHẬT KÝ HỆ THỐNG & SỰ KIỆN | `OutboxEvent` |
| 15. NHÓM VÍ KHÁCH HÀNG & GIAO DỊCH TÀI CHÍNH MỞ RỘNG | `CustomerWallet`, `CustomerWalletTransaction`, `EInvoice`, `GiftCard` |
| 16. NHÓM VẬN CHUYỂN NỘI BỘ (GENIE EXPRESS) | `DeliveryOrder`, `Shipper`, `ShipperCodLedger`, `ShipperWalletTransaction`, `SortingHub` |
| 17. NHÓM GAMIFICATION & KHÁCH HÀNG THÂN THIẾT | `Achievement`, `AffiliateCommission`, `AffiliateLink`, `CustomerTier`, `DailyMission`, `LuckyWheelReward`, `LuckyWheelSpinHistory`, `Referral`, `ReferralCode`, `UserAchievement`, `UserMissionProgress` |
| 18. NHÓM KHUYẾN MÃI NÂNG CAO & SOCIAL COMMERCE | `AddonDeal`, `BundleDeal`, `BundleDealItem`, `ComboDeal`, `ComboDealProduct`, `HomeBanner`, `LiveStreamProduct`, `LiveStreamSession`, `ProductPriceHistory`, `SharedCart`, `SharedCartMember`, `SponsoredAdCampaign`, `SponsoredAdClick` |
| 19. NHÓM HỖ TRỢ KHÁCH HÀNG & QUẢN TRỊ VẬN HÀNH | `CannedResponse`, `FeatureFlag`, `ProductViolationReport`, `SupportTicket`, `TicketMessage`, `VendorAutoReplyRule`, `VendorPenalty`, `VendorSubscription`, `VendorSubscriptionPlan` |
| 20. NHÓM TUÂN THỦ PHÁP LÝ & BẢO MẬT DỮ LIỆU (PDPA VN & FRAUD DETECTION) | `Consent`, `ConsentAuditLog`, `DeviceFingerprint`, `PartnerAgreement`, `RiskAssessment` |
| 21. NHÓM A/B TESTING & KHUNG THỬ NGHIỆM TỐI ƯU | `Experiment`, `ExperimentAudience`, `ExperimentExposure`, `ExperimentMetric`, `ExperimentVariant` |
| 22. NHÓM RECOMMENDATION ENGINE & CÁ NHÂN HÓA | `AiChatMessage`, `ItemEmbedding`, `RecommendationBlacklist`, `SearchHistory`, `UserEmbedding`, `UserInteractionEvent` |
| 23. NHÓM ANALYTICS & THU THẬP HÀNH VI (CLICKSTREAM) | `ClickstreamEvent`, `ConversionFunnel`, `PagePerformanceLog`, `SessionAnalytics` |
| 24. NHÓM ĐA TIỀN TỆ & ĐỊNH GIÁ QUỐC TẾ (MULTI-CURRENCY) | `CountryPricingPolicy`, `Currency`, `CurrencyConversion`, `ExchangeRate` |
| 25. NHÓM CỔNG B2B BÁN SỈ & BÁO GIÁ RFQ (WHOLESALE) | `B2BCompany`, `B2BContract`, `B2BInvoice`, `CorporateCreditTerm`, `RfqQuote`, `RfqRequest`, `WholesalePriceTier` |
| 26. NHÓM CỜ TÍNH NĂNG NÂNG CAO (ADVANCED FEATURE FLAGS) | `FeatureFlagAuditLog`, `FeatureFlagRecord`, `FeatureFlagSegment` |
| 27. NHÓM HÀNG ĐỢI SỰ KIỆN & KAFKA CATALOG (100K CCU) | `ConsumerGroupOffset`, `DeadLetterEvent`, `EventSchema`, `KafkaTopicCatalog` |
| 28. NHÓM GIỚI HẠN TẦN SUẤT & NGẮT MẠCH (RESILIENCE) | `CircuitBreakerState`, `IpBlacklist`, `RateLimitRule` |
| 29. NHÓM ĐIỀU PHỐI GIAO DỊCH PHÂN TÁN SAGA (ORCHESTRATION) | `SagaCompensation`, `SagaInstance`, `SagaStepExecution` |
| 30. NHÓM ĐỒNG BỘ TÌM KIẾM & ĐA VÙNG (ELASTICSEARCH SYNC) | `CacheInvalidationEvent`, `CdcHeartbeat`, `DataResidencyRule`, `ElasticsearchSyncCheckpoint`, `ReadReplicaRoutingRule` |
| 31. NHÓM VẬN HÀNH GIAN HÀNG NÂNG CAO (ĐA KHO, TÀI KHOẢN PHỤ & TRANG TRÍ GIAN HÀNG) | `ShopCategory`, `ShopCategoryProduct`, `ShopDecoration`, `VendorStaffMember`, `VendorWarehouse`, `WarehouseInventory` |
| 32. NHÓM VẬT PHẨM TRANG TRÍ & MUA TRƯỚC TRẢ SAU (COSMETIC ITEMS & BNPL) | `BnplPlan`, `BnplTransaction`, `CosmeticItem`, `UserCosmeticItem` |

### 6.3 Thuộc tính & phương thức bị cắt trong entity được giữ

| Entity | Thuộc tính bị bỏ | Phương thức bị bỏ |
|---|---|---|
| `User` | `oauthProvider`, `oauthProviderId`, `mfaSecretEncrypted`, `mfaEnabledAt`, `mfaRecoveryCodesJson`, `failedMfaAttempts`, `mfaLockedUntil` | `anonymize()`, `isMfaRequiredAtLogin()`, `enableMfa()`, `disableMfa()`, `isMfaLocked()`, `registerMfaFailure()`, `recordMfaSuccess()` |
| `CustomerProfile` | `anonymizedAt`, `referredByUserId`, `onboardingPreferencesJson` | `anonymize()` |
| `VendorProfile` | `currentPenaltyPoints`, `autoReplyEnabled`, `vacationStartsAt`, `vacationEndsAt`, `vacationMessage`, `autoReplyGreeting`, `autoReplyOffline` | `isWithholdingTaxPayer()`, `syncDefaultPickup()`, `refreshPenaltyPoints()`, `isCampaignBanned()`, `startVacation()`, `endVacation()`, `isOnVacation()` |
| `AdminProfile` | `staffTitle` | `changeTitle()` |
| `KycResult` | `shipperId`, `shipper` | — |
| `Category` | `requiresBusinessLicense` | — |
| `Product` | `businessLicenseUrl`, `businessLicenseExpiresAt` | `flag()`, `hasValidLicense()`, `suspendForExpiredLicense()`, `renewLicense()` |
| `ProductVariant` | `safetyStockTotal` | — |
| `UserWishlist` | `notifyOnPriceDrop` | `shouldNotify()` |
| `Voucher` | `applicablePaymentMethod`, `categories`, `products`, `excludedCategories`, `excludedProducts` | — |
| `CheckoutSession` | `currencyCode`, `lockedExchangeRate`, `fxLockExpiresAt` | `isFxLockValid()` |
| `Order` | `warehouseId`, `sourceType`, `sourceId`, `coinUsedCount`, `coinDiscountAmount` | — |
| `OrderItem` | `coinDiscount` | — |
| `StockReservation` | `warehouseId` | — |
| `PaymentTransaction` | `reconciliationStatus`, `reconciledAt`, `reconciliationNote` | `markReconciled()`, `resolveDiscrepancy()` |
| `EscrowLedger` | `walletFundedAmount` | — |
| `WithdrawalRequest` | `ownerType`, `reviewedByStaffId`, `reviewedAt` | `requiresDualApproval()`, `review()` |
| `ProductReview` | `aspectSentimentJson` | `attachSentiment()` |
| `Message` | `productVariantId` | — |

### 6.4 Enum

**Giá trị bị cắt (enum vẫn giữ tên):**

| Enum | Số giá trị | Giá trị bị bỏ |
|---|:---:|---|
| `UserRole` | 7 → 4 | `VENDOR_STAFF`, `SHIPPER`, `B2B_BUYER` |
| `AccountStatus` | 6 → 5 | `ANONYMIZED` |
| `ProductStatus` | 7 → 6 | `FLAGGED` |
| `VoucherScope` | 3 → 1 | `CATEGORY`, `PRODUCT` |
| `VoucherType` | 4 → 2 | `FREESHIP`, `PAYMENT` |
| `DiscountType` | 3 → 2 | `FREESHIP` |
| `PaymentMethod` | 7 → 3 | `MOMO`, `ZALOPAY`, `INTERNAL_WALLET`, `BNPL` |
| `FundingSource` | 4 → 2 | `WALLET`, `SPLIT` |
| `WalletTransactionType` | 11 → 5 | `CREDIT_TOPUP`, `DEBIT_PURCHASE`, `DEBIT_AD_SPEND`, `DEBIT_SUBSCRIPTION_FEE`, `DEBIT_TAX_WITHHOLDING`, `DEBIT_COSMETIC_PURCHASE` |
| `WalletReferenceType` | 8 → 4 | `SUBSCRIPTION`, `AD_CAMPAIGN`, `COSMETIC`, `TAX` |
| `CarrierType` | 4 → 1 | `GENIE_EXPRESS`, `GHTK`, `VIETTEL_POST` |
| `NotificationType` | 15 → 12 | `FLASH_SALE_REMINDER`, `PRICE_DROP_ALERT`, `B2B_RFQ_QUOTED` |
| `NotificationChannel` | 6 → 2 | `PUSH`, `SMS`, `ZNS`, `WEBSOCKET` |
| `MessageType` | 4 → 2 | `PRODUCT_CARD`, `ORDER_CARD` |
| `AuditAction` | 24 → 15 | `PERMISSION_GRANTED`, `PERMISSION_REVOKED`, `RBAC_ROLE_CHANGED`, `STAFF_INVITED`, `PII_ANONYMIZED`, `MFA_ENABLED`, `MFA_DISABLED`, `MFA_CHALLENGE_FAILED`, `PAYOUT_REVIEWED` |

**Enum bị bỏ hẳn (87):** `AchievementCategory`, `AdCampaignStatus`, `AdminNotificationType`, `AgreementType`, `AiMessageRole`, `AttributeDataType`, `B2BInvoiceStatus`, `B2BVerificationStatus`, `BannerPosition`, `BlacklistReason`, `BnplTransactionStatus`, `CircuitState`, `CleanupPolicy`, `ClickstreamType`, `CodStatus`, `CoinTransactionType`, `CommissionStatus`, `CompatibilityMode`, `CompensationStatus`, `ConnectorStatus`, `ConsentAction`, `ConsentStatus`, `ConsentType`, `ContractStatus`, `CosmeticAudience`, `CosmeticItemType`, `CosmeticPaymentMethod`, `CosmeticRarity`, `CreditStatus`, `CustomerWalletTxType`, `DataClassification`, `DeadLetterStatus`, `DeliveryFailureReason`, `DeliveryOrderStatus`, `DevicePlatform`, `DeviceType`, `ExperimentStatus`, `ExpressServiceType`, `FinancialEventType`, `FlashSaleStatus`, `GiftCardTemplate`, `InteractionType`, `InvoiceStatus`, `LiveStreamStatus`, `MemberRole`, `MetricType`, `NotificationCategory`, `OrderSourceType`, `OutboxStatus`, `PenaltyReason`, `PlatformNotificationTarget`, `QuoteStatus`, `RateLimitScope`, `ReconciliationStatus`, `ReferralStatus`, `RequesterType`, `ResidencyRequirement`, `ReviewReportReason`, `RewardType`, `RfqStatus`, `RiskDecision`, `RolloutStrategy`, `SagaStatus`, `SagaType`, `SchemaType`, `ShipperStatus`, `ShipperWalletTxType`, `ShopDecorationStatus`, `StaffPermissionEnum`, `StaffTitle`, `StationType`, `StepStatus`, `SubscriptionStatus`, `SupportChannel`, `SyncStatus`, `TaxWithholdingStatus`, `TicketCategory`, `TicketPriority`, `TicketStatus`, `TierLevel`, `VehicleType`, `VendorStaffPermission`, `VendorStaffStatus`, `ViolationReportStatus`, `ViolationType`, `WalletOwnerType`, `WalletStatus`.

## 7. Thay đổi theo từng file

| File trong `TLCN/` | Cách tạo | Nội dung chính |
|---|---|---|
| `diagrams/ClassDiagram/ENTITY.md` | Trích 38 entity từ bản đầy đủ + script cắt field / method + sửa mô tả; header, mục 13, Phụ lục A–E viết mới / sinh tự động | 12 nhóm, 38 entity, 37 enum, 61 quan hệ, 145 phương thức |
| `diagrams/ClassDiagram/java-code/*.java` | Sinh từ ENTITY.md MVP (`gen_java.py`) | 7 file `Diagram01…07`; 12 file của nhóm bị bỏ (08–19) không còn |
| `diagrams/ClassDiagram/Genie_ClassDiagram_UML25.xmi` | Sinh từ ENTITY.md MVP (`gen_xmi.py`) | 7 package / sơ đồ EA, 38 class + 37 enum |
| `diagrams/UseCaseDiagram/Usecase.md` | Trích 56 UC gốc + viết lại 7 UC + thêm UC-CAT-16; Phần I–II sinh mới | 57 UC, 11 phân hệ, 11 tác nhân |
| `diagrams/UseCaseDiagram/UML_UseCase_PlantUML.md` | Sinh từ `scope.py` + Usecase.md | 13 sơ đồ PlantUML |
| `diagrams/UseCaseDiagram/Genie_UseCase_UML25.xmi` | Sinh mới (`gen_uc_xmi.py`) + bố trí lại (`relayout_uc.py` của bản gốc) | 14 sơ đồ EA, 61 use case (gồm 4 dùng chung) |
| `diagrams/UseCaseDiagram/Quản Lý UseCase.xlsx` | Giữ định dạng / công thức của file gốc, thay dữ liệu | 57 UC, 11 phân hệ, 11 tác nhân; cột mã UC đổi sang nhập tay để giữ mã gốc |
| `document/Document_Vi.md` | Mẫu `templates/Document_Vi.tpl.md` + trích mục chung (1.3, 3.4, 3.5, 3.7.1, 4.1, 4.2) | SRS + HLD + LLD + triển khai mức MVP |
| `implement.md` | Viết mới | Lộ trình 6 sprint, đặc tả triển khai 11 phân hệ, API, điểm xuất phát & rủi ro khi xây mới |
| `kien-thuc/Kien_Thuc_Can_Nam.md` | Viết mới | Kiến thức theo ngăn xếp MVP + câu hỏi bảo vệ |
| `kien-thuc/BAO_CAO_KIEN_THUC_KINH_DOANH_KINH_TE_v1.0.md` | Viết mới (giữ tham số bản đầy đủ) | Một dòng doanh thu (hoa hồng), unit economics, KPI, dự báo 6 tháng |
| `kien-thuc/BAO_CAO_HE_THONG_CHINH_SACH_HOP_DONG_PHAP_LY_v1.0.md` | Viết mới (rút gọn) | 10 nhóm chính sách cho MVP; rủi ro thuế NĐ 117/2025 |
| `kien-thuc/BAO_CAO_DU_TOAN_CHI_PHI_VA_GIAI_PHAP_AI_v1.0.md` | Viết mới (giữ đơn giá bản đầy đủ) | M0 / M1 thay T0–T3; giải pháp KYC tự host |

## 8. Điểm xuất phát & Kế hoạch triển khai: TLCN so với DATN

> **Hai bộ tài liệu là hai bản KẾ HOẠCH, chưa có mã nguồn:** `fix-doc/DATN/` = kế hoạch đồ án tốt nghiệp (dài hạn); `fix-doc/TLCN/` = kế hoạch cho đồ án (TLCN). Đồ án **xây mới từ đầu** — hiện trạng mọi use case: *Chưa triển khai*.

### 8.1 Con số

| | DATN (đồ án tốt nghiệp) | TLCN v1.1 | TLCN / DATN |
|:---|:---:|:---:|:---:|
| Use case | 206 | 87 | 42% |
| Entity / bảng | 169 | 64 | 38% |
| Phân hệ | 23 | 11 | 48% |
| Đã triển khai (mã nguồn) | 0 | **0** | — |
| Thời gian dự kiến | nhiều năm (lộ trình T1–T3) | **16 tuần** (8 sprint × 2 tuần) | — |

> Hoàn thành TLCN tương đương làm xong **87/206 use case** của DATN; 119 use case còn lại là lộ trình đồ án tốt nghiệp — lọc cột *Hiện trạng* = "Chưa triển khai" trong `DATN/diagrams/UseCaseDiagram/Quản Lý UseCase.xlsx`.

### 8.2 Kế hoạch theo sprint (chưa triển khai → triển khai)

| Sprint | Tuần | Mục tiêu | Số UC | Use case | Đánh dấu hoàn thành |
|:---:|:---:|---|:---:|---|:---:|
| S1 | tuần 1–2 | Nền tảng tài khoản & phân quyền | 7 | `UC-AUTH-01`, `UC-AUTH-02`, `UC-AUTH-04`, `UC-AUTH-06`, `UC-AUTH-11`, `UC-ADM-01`, `UC-ADM-02` | ☐ |
| S2 | tuần 3–4 | Danh mục, sản phẩm & KYC | 17 | `UC-AUTH-08`, `UC-AUTH-09`, `UC-CAT-01`, `UC-CAT-02`, `UC-CAT-05`, `UC-CAT-06`, `UC-CAT-09`, `UC-CAT-10`, `UC-CAT-11`, `UC-CAT-13`, `UC-CAT-14`, `UC-CAT-16`, `UC-VND-01`, `UC-VND-02`, `UC-VND-03`, `UC-VND-04`, `UC-VND-05` | ☐ |
| S3 | tuần 5–6 | Giỏ hàng, đặt hàng & thanh toán | 9 | `UC-ORD-01`, `UC-ORD-04`, `UC-ORD-06`, `UC-ORD-07`, `UC-ORD-08`, `UC-PAY-01`, `UC-PAY-03`, `UC-PAY-05`, `UC-PAY-06` | ☐ |
| S4 | tuần 7–8 | Vận chuyển, ví & rút tiền | 12 | `UC-ORD-09`, `UC-ORD-10`, `UC-ORD-11`, `UC-ORD-12`, `UC-PAY-07`, `UC-VND-07`, `UC-LOG-01`, `UC-LOG-02`, `UC-LOG-03`, `UC-LOG-16`, `UC-ADM-04`, `UC-ADM-05` | ☐ |
| S5 | tuần 9–10 | Khuyến mãi, chat, đánh giá & trả hàng | 11 | `UC-PRO-01`, `UC-PRO-02`, `UC-PRO-03`, `UC-SOC-03`, `UC-SOC-04`, `UC-SUP-01`, `UC-SUP-02`, `UC-SUP-03`, `UC-SUP-04`, `UC-NOT-01`, `UC-NOT-03` | ☐ |
| S6 | tuần 11–12 | Thông báo, quản trị, phân tích cảm xúc & hoàn thiện | 6 | `UC-CAT-08`, `UC-VND-08`, `UC-SOC-01`, `UC-ADM-03`, `UC-ADM-06`, `UC-AI-07` | ☐ |
| S7 | tuần 13–14 | Mở rộng tài khoản, sản phẩm, gian hàng & quản trị (đối chiếu giao diện) | 14 | `UC-AUTH-03`, `UC-AUTH-10`, `UC-CAT-04`, `UC-CAT-07`, `UC-CAT-12`, `UC-CAT-15`, `UC-CAT-17`, `UC-CAT-18`, `UC-VND-06`, `UC-VND-19`, `UC-VND-20`, `UC-VND-21`, `UC-LOG-15`, `UC-ADM-07` | ☐ |
| S8 | tuần 15–16 | Khuyến mãi nâng cao, Điểm, tương tác & nội dung sàn | 11 | `UC-PRO-04`, `UC-PRO-05`, `UC-PRO-06`, `UC-PRO-08`, `UC-PRO-09`, `UC-PRO-10`, `UC-SOC-02`, `UC-SOC-05`, `UC-SOC-06`, `UC-NOT-05`, `UC-ADM-12` | ☐ |
| **Σ** | 16 tuần | | **87** | | |

**Việc ngoài use case cần làm song song:** Flyway 64 bảng + seed Admin (S1), CI + kiểm thử (mọi sprint — Document_Vi 4.3), rate limit & bảo mật (S1), AI Worker KYC (thành viên AI làm từ S1, ghép ở S2), triển khai VPS + HTTPS (S6, cập nhật S8), API key Gemini / OAuth (trước S7).

**Nếu trễ tiến độ, cắt theo thứ tự:** UC SHOULD của S8 rồi S7 (Document_Vi Phụ lục C) — không cắt use case thuộc luồng tiền (ORD, PAY, SUP) và phân quyền (ADM-01, ADM-02).

### 8.3 Theo dõi

Cập nhật cột *Hiện trạng mã nguồn* trong `diagrams/UseCaseDiagram/Quản Lý UseCase.xlsx` (Chưa triển khai → Đã triển khai) khi use case đạt Definition of Done: API + giao diện + test + đã demo. Cột *Ghi chú* đã ghi sẵn sprint dự kiến của từng use case; mỗi use case trong Usecase.md cũng có dòng *Kế hoạch: Sprint N*.

## 9. Rủi ro & việc cần làm trước khi vận hành thật

- **Thuế:** sàn có chức năng thanh toán phải khấu trừ thuế thay hộ / cá nhân kinh doanh (NĐ 117/2025) — TLCN chưa có (thêm lại `TaxWithholdingRecord`, UC-PAY-10 từ DATN).
- **Đăng ký sàn TMĐT** với Bộ Công Thương, quy chế được pháp chế duyệt.
- **VNPay / GHN production** cần pháp nhân; tra cứu tên tài khoản ngân hàng thật thay API giả lập.
- **Chỉ một instance backend** (simple broker WebSocket, cron) — đủ cho MVP; muốn chạy nhiều instance cần broker relay (đã dùng `SKIP LOCKED` cho cron).

## 10. Cách sinh lại / mở rộng

Công cụ v1.1 (Python) lưu ở `fix-doc-backup-2026-10-29/tools/` (cạnh bản sao tài liệu trước khi sửa):

```bash
# Sơ đồ lớp: sửa ENTITY.md bằng tay → sinh lại Java + XMI
cd tools/cd
LZD_SET=tlcn LZD_PREFIX="TLCN/" python parse_entity.py <TLCN ENTITY.md> model_tlcn.json "# 13. "
LZD_SET=tlcn LZD_PREFIX="TLCN/" python gen_java.py model_tlcn.json <java-code> "ENTITY.md TLCN v1.1"
LZD_SET=tlcn LZD_PREFIX="TLCN/" python gen_xmi.py model_tlcn.json <xmi> "ENTITY.md TLCN v1.1" "<thời điểm>"
# DATN: bỏ LZD_SET, LZD_PREFIX="DATN/", dòng kết thúc "# 33. ", nhãn "ENTITY.md v2.16"
# Phạm vi use case: sửa tk/tlcn_scope.py (UC_KEEP, ACTORS, CLASS_DIAGRAMS), tk/uc_relations_tlcn.py, tk/uc_content_*.py rồi chạy
#   build_uc_tlcn.py → gen_uc_plantuml_tlcn.py → gen_uc_xmi_tlcn.py (+ cd/relayout_uc.py) → build_xlsx_tlcn.py → build_xlsx_datn.py
#   → regen_entity_tlcn.py → patch_doc_tlcn.py → patch_impl_tlcn.py
```

Khi đưa một tính năng của DATN vào TLCN: thêm mã UC vào `UC_KEEP`, entity vào `CLASS_DIAGRAMS`, giá trị enum vào Phụ lục A, viết migration Flyway mới — **không đổi tên** bất cứ thứ gì đang có.
