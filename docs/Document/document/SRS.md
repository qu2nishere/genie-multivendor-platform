# SRS — ĐẶC TẢ YÊU CẦU PHẦN MỀM (Software Requirements Specification)

| Thuộc tính | Nội dung |
|---|---|
| Mã tài liệu | GENIE-TLCN-SRS |
| Dự án | Genie — Sàn thương mại điện tử đa người bán (bản TLCN — Tiểu luận chuyên ngành) |
| Phiên bản | 1.1 (đồng bộ bộ tài liệu TLCN v1.4 và mã nguồn 2026-11-16) |
| Ngày ban hành | 2026-11-06 |
| Nhóm tác giả | Mo Ha Mach Bu Ba Ka · Nguyen Quoc Khoa · Nguyen Ngoc Thien · Nghiem Phu Dang Quan |
| Trạng thái | Bản phát hành để rà soát |
| Mục đích | Đặc tả đầy đủ, kiểm chứng được các yêu cầu chức năng, phi chức năng, giao diện và dữ liệu của hệ thống Genie bản TLCN (cấu trúc theo ISO/IEC/IEEE 29148). |
| Người đọc | Nhóm phát triển, kiểm thử, giảng viên hướng dẫn, hội đồng đánh giá |
| Tài liệu liên quan | BRD.md, PRD.md, HLD.md, LLD.md; Usecase.md (đặc tả 87 use case); ENTITY.md (65 bảng); Quản Lý UseCase.xlsx |

> Các tham chiếu trong tài liệu ghi theo **tên tệp và mục** (ví dụ "Usecase.md, Phần III, UC-ORD-07"), không dùng liên kết, để tài liệu dùng được khi tách khỏi thư mục gốc.

### Lịch sử phiên bản

| Phiên bản | Ngày | Nội dung |
|---|---|---|
| 1.0 | 2026-11-06 | Phát hành lần đầu, tổng hợp từ bộ tài liệu TLCN v1.2 (87 use case, 11 phân hệ, 16 tác nhân, 64 bảng) |
| 1.1 | 2026-11-16 | Đồng bộ với mã nguồn `genie-multivendor-platform` (TLCN v1.4): kho tệp MinIO → **Silo** (ADR-15 / ADR 0009 trong repo); AUTH chỉnh theo phần đã hiện thực (đăng ký giữ tạm Redis 15 phút, `GNE-AUTH-4012`, `GNE-AUTH-4098`, khóa tạm không đổi `status`, refresh giữ `family_id` + ân hạn 10 giây, xóa tài khoản giữ phiên hiện tại, cron 02:30); `V14__auth_additions.sql` thay cho `V14__api_sync_v13.sql`; đổi tên UC-AUTH-03 / 06 / 11 theo giao diện; thêm mã lỗi `GNE-SYS-4040/4050/5000` |
## Mục lục

1. Giới thiệu
2. Mô tả tổng thể
3. Yêu cầu giao diện ngoài
4. Yêu cầu chức năng
5. Yêu cầu phi chức năng
6. Yêu cầu dữ liệu
7. Ma trận truy vết
8. Kiểm chứng yêu cầu
9. Phụ lục A — Danh mục 87 use case

## 1. Giới thiệu

### 1.1 Mục đích
Tài liệu đặc tả các yêu cầu phần mềm của **Genie** — sàn thương mại điện tử đa người bán bản TLCN — làm căn cứ cho thiết kế (HLD.md, LLD.md), hiện thực, kiểm thử và nghiệm thu. Mỗi yêu cầu có mã duy nhất, mức ưu tiên và truy vết tới use case.

### 1.2 Phạm vi sản phẩm
Hệ thống gồm Storefront (người mua), Seller Center (người bán), Admin Portal (nhân viên sàn) và các dịch vụ nền: thanh toán có ký quỹ, vận chuyển qua 3PL, KYC bằng AI, kiểm duyệt sản phẩm bằng Gemini, phân tích cảm xúc đánh giá bằng PhoBERT. Quy mô: **87 use case · 11 phân hệ · 16 tác nhân · 65 bảng**, ~50 người dùng đồng thời trên 1 máy chủ.

#### 1.2.1 Trong phạm vi

| Mảng | Trong TLCN |
|---|---|
| Tài khoản | Đăng ký email + OTP, đăng nhập JWT (access 15 phút + refresh 7 ngày), đăng nhập Google / Apple, quên / đổi mật khẩu, đăng xuất & thu hồi phiên, hồ sơ, sổ địa chỉ chuẩn GHN, xóa tài khoản (ẩn danh hóa); **một tài khoản hai chế độ Mua / Bán** |
| Danh mục & sản phẩm | Cây danh mục 3 cấp + thuộc tính chuẩn theo ngành, thương hiệu, tìm kiếm full-text PostgreSQL + từ khóa gợi ý / xu hướng, lọc & sắp xếp, chi tiết SKU (lượt xem, nút ♥ yêu thích, nút Chia sẻ deeplink), sản phẩm tương tự, trang shop (điểm đánh giá, người theo dõi, số sản phẩm, tỉ lệ phản hồi chat); shop CRUD sản phẩm, hàng đặt trước, giấy tờ chứng thực, đề xuất danh mục mới; kiểm duyệt **Gemini** + Moderator |
| Đơn hàng | Giỏ hàng (cả khách vãng lai, gộp khi đăng nhập), tách đơn theo shop, phí ship theo hãng, ghi chú cho shop, tự hủy đơn quá hạn 15 phút, hủy đơn, xác nhận đã nhận, tự hoàn tất sau 7 ngày |
| Thanh toán | VNPay (Return URL + IPN idempotent, refund API), chuyển khoản VietQR qua SePay (webhook tiền vào / tiền ra), **COD kết hợp Escrow**; ký quỹ 7 ngày; hoa hồng theo ngành / theo shop; sổ cái kép bất biến |
| Gian hàng | Mở shop, KYC theo tầng (AI + duyệt tay), kho lấy hàng & cấu hình vận chuyển (freeship, hãng bật), tài khoản nhận tiền chính chủ, thông tin & cài đặt gian hàng (logo, ảnh bìa, mô tả), tự trả lời chat, tạm nghỉ, ví & sổ giao dịch, báo cáo & phân tích bán hàng |
| Vận chuyển | Shop xác nhận / từ chối đơn mới; điều phối đa hãng GHN / GHTK / ViettelPost / J&T; tạo vận đơn, in phiếu A6, webhook hành trình, đối soát COD |
| Khuyến mãi | Voucher **xếp chồng 3 tầng** (Shop → Sàn → Freeship) với phạm vi danh mục / sản phẩm + **Điểm** (tối đa 50% đơn, 1 Điểm = 1₫); Flash Sale; điểm danh nhận Điểm; đổi Điểm lấy voucher |
| Tương tác | Chat WebSocket (văn bản, ảnh, thẻ sản phẩm, trạng thái đơn), đánh giá có ảnh / video + nút "Hữu ích", shop phản hồi, báo cáo đánh giá vi phạm, phân tích cảm xúc đánh giá bằng PhoBERT (tích cực / trung tính / tiêu cực), theo dõi shop |
| Sau bán | Trả hàng / hoàn tiền (toàn phần & một phần), phân xử tranh chấp, hoàn tiền VNPay / VietQR / COD |
| Thông báo | In-app thời gian thực + email giao dịch + thông báo toàn sàn |
| Quản trị | Nhân viên & lời mời kích hoạt, RBAC 13 mã quyền (3 chức danh + Admin), khóa / mở khóa tài khoản, duyệt rút tiền, dashboard doanh thu & ký quỹ, nhật ký hệ thống, banner trang chủ |
| Giao diện | Đa ngôn ngữ vi / en, responsive 360–1440 px, SEO + Open Graph |

#### 1.2.2 Ngoài phạm vi (thuộc DATN)

Ví khách hàng & mã PIN, MoMo / ZaloPay / thẻ quốc tế / BNPL, hóa đơn điện tử & khấu trừ thuế; Genie Express (đội tài xế, app Shipper PWA, Hub, định tuyến VRPTW); voucher tầng 4 (ưu đãi thanh toán), combo / bundle; gamification ngoài Điểm (hạng thành viên, nhiệm vụ, vòng quay, vật phẩm trang trí), giới thiệu bạn bè, affiliate, livestream; trợ lý mua sắm AI, tìm kiếm bằng ảnh, gợi ý vector cá nhân hóa, phân tích cảm xúc theo khía cạnh (ABSA); đa kho, tài khoản phụ shop, trang trí gian hàng, gói VIP, quảng cáo PPC, điểm phạt; ticket CSKH theo SLA; MFA; A/B testing, analytics clickstream, đa tiền tệ, B2B; Kafka, RabbitMQ, Elasticsearch, Qdrant, Kubernetes, Saga, feature flag. Use case tương ứng ở `Usecase.md` (cột Hiện trạng trong `Quản Lý UseCase.xlsx` của DATN đánh “Đã triển khai” cho 87 use case đã có ở TLCN). Danh sách chi tiết theo từng use case / entity: BAO_CAO_PHAM_VI_MVP.md.

### 1.3 Quy ước ưu tiên

Mọi yêu cầu nghiệp vụ và kỹ thuật trong tài liệu đều được gán nhãn ưu tiên theo chuẩn MoSCoW:
- **MUST (Bắt buộc):** Các yêu cầu cốt lõi không thể thiếu để hệ thống vận hành hợp pháp, an toàn và đảm bảo giao dịch thông suốt. Thiếu yêu cầu này, sản phẩm không thể phát hành (Release Blocker).
- **SHOULD (Nên có):** Các tính năng mang lại giá trị vận hành và trải nghiệm cao, có giải pháp thay thế tạm thời trong ngắn hạn nhưng bắt buộc phải có trong phiên bản hoàn thiện.
- **COULD (Có thể có):** Các cải tiến nâng cao trải nghiệm người dùng, tối ưu hóa giao diện và tiện ích bổ trợ, triển khai khi nguồn lực cho phép.
- **WON'T (Chưa làm):** Các tính năng nằm ngoài phạm vi phiên bản hiện tại, được bảo lưu cho các chu kỳ phát triển tiếp theo (ví dụ: giao dịch xuyên biên giới đa tiền tệ, sàn giao dịch đấu giá trực tiếp).

### 1.4 Thuật ngữ & viết tắt

| Thuật ngữ / Viết tắt | Tên tiếng Anh đầy đủ | Định nghĩa & Ý nghĩa kỹ thuật trong Genie |
|---|---|---|
| **SRS** | Software Requirements Specification | Tài liệu Đặc tả Yêu cầu Phần mềm — văn bản hợp đồng kỹ thuật chuẩn. |
| **HLD** | High-Level Design | Thiết kế Mức Cao — cấu trúc kiến trúc tổng thể, phân rã hệ thống và luồng dữ liệu. |
| **LLD** | Low-Level Design | Thiết kế Mức Chi tiết — thiết kế CSDL vật lý, sơ đồ lớp, mã lỗi và giải thuật. |
| **SKU** | Stock Keeping Unit | Mã định danh quản lý tồn kho duy nhất cho từng biến thể sản phẩm (Màu, Size...). |
| **KYC / E-KYC** | Electronic Know Your Customer | Quy trình xác minh danh tính điện tử người bán qua OCR CCCD và đối chiếu khuôn mặt. |
| **3PL** | Third-Party Logistics | Đơn vị cung cấp dịch vụ giao vận bưu chính bên thứ ba (GHN, GHTK, Viettel Post). |
| **Silo** | Silo S3-Compatible Storage (fork cộng đồng của MinIO — ADR 0009) | Hệ thống lưu trữ đối tượng phân tán hiệu năng cao tương thích chuẩn AWS S3. |
| **HMAC** | Hash-based Message Authentication Code | Mã xác thực tin nhắn dựa trên băm mật mã dùng để bảo vệ tính toàn vẹn của số dư ví. |
| **Escrow** | Escrow Holding Account | Cơ chế tài khoản ký quỹ trung gian bảo đảm quyền lợi giao dịch giữa Người mua và Người bán. |
| **PDPA** | Personal Data Protection Act | Quy định bảo vệ dữ liệu cá nhân của Việt Nam: Luật Bảo vệ dữ liệu cá nhân 2025 (Luật 91/2025/QH15, hiệu lực 01/01/2026), kế thừa Nghị định 13/2023/NĐ-CP. |
| **Điểm Genie** | Loyalty points | Điểm thưởng của người mua, 1 Điểm = 1₫, hết hạn sau 12 tháng, dùng tối đa 50% tiền hàng |
| **PhoBERT** | Pre-trained Vietnamese RoBERTa | Mô hình ngôn ngữ tiếng Việt; nhóm fine-tune để phân loại cảm xúc đánh giá 3 nhãn |
| **IPN** | Instant Payment Notification | Thông báo kết quả thanh toán từ VNPay gửi máy chủ tới máy chủ |
| **COD** | Cash on Delivery | Thanh toán khi nhận hàng; ở Genie tiền vẫn đi qua ký quỹ |

### 1.5 Tài liệu tham khảo
- Usecase.md — đặc tả chi tiết 87 use case (luồng chính, luồng thay thế, ràng buộc).
- ENTITY.md, GIAI_NGHIA_ENTITY.md — mô hình dữ liệu 65 entity, 55 enum.
- Document_Vi.md — tài liệu gộp SRS / HLD / LLD gốc.
- TECH_STACK_VA_QUY_UOC.md, LUU_TRU_DU_LIEU.md — quy ước hiện thực và lưu trữ.
- ISO/IEC/IEEE 29148:2018 — cấu trúc tài liệu yêu cầu.

## 2. Mô tả tổng thể

### 2.1 Bối cảnh sản phẩm
Genie là hệ thống mới, xây từ đầu (0/87 use case có mã nguồn). Hệ thống tương tác với cổng thanh toán (VNPay, SePay), hãng vận chuyển (GHN, GHTK, ViettelPost, J&T), dịch vụ AI (Gemini API; AI Worker nội bộ cho KYC và PhoBERT), nhà cung cấp danh tính (Google, Apple), máy chủ email và lưu trữ tệp Silo. Sơ đồ ngữ cảnh: HLD.md, mục 3.

### 2.2 Chức năng sản phẩm

| Phân hệ | Mã | Số UC | Chức năng chính |
|---|---|:---:|---|
| 01. Xác thực & tài khoản | AUTH | 9 | Đăng ký OTP email, đăng nhập (mật khẩu / OTP / Google / Apple), mật khẩu, hồ sơ, sổ địa chỉ, xóa tài khoản |
| 02. Danh mục, sản phẩm & tìm kiếm | CAT | 17 | Cây danh mục, tìm kiếm full-text, chi tiết SKU, đăng bán, kiểm duyệt Gemini + Moderator, trang shop, chia sẻ |
| 03. Giỏ hàng & đơn hàng | ORD | 9 | Giỏ đa shop, tách đơn, giữ chỗ tồn kho, theo dõi, hủy, xác nhận nhận hàng |
| 04. Thanh toán, ký quỹ & sổ cái | PAY | 5 | VNPay, chuyển khoản VietQR (SePay), COD; ký quỹ 7 ngày; sổ cái kép; giải ngân |
| 05. Gian hàng & KYC | VND | 11 | Mở shop, KYC theo tầng, kho lấy hàng, tài khoản nhận tiền, ví, phân tích, cài đặt, tạm nghỉ |
| 06. Vận chuyển 3PL | LOG | 5 | Xác nhận đơn, vận đơn, webhook hành trình, điều phối đa hãng, đối soát COD |
| 07. Khuyến mãi & Điểm | PRO | 9 | Voucher 3 tầng, kho voucher, xếp chồng + Điểm, Flash Sale, điểm danh |
| 08. Chat, đánh giá & theo dõi shop | SOC | 7 | Chat realtime, đánh giá + Hữu ích, phản hồi, báo cáo, theo dõi shop, phân tích cảm xúc PhoBERT |
| 09. Trả hàng & hoàn tiền | SUP | 4 | Yêu cầu trả hàng, shop xử lý, sàn phân xử, hoàn tiền |
| 10. Thông báo | NOT | 3 | In-app realtime, email giao dịch, thông báo toàn sàn |
| 11. Quản trị sàn | ADM | 8 | Nhân viên & RBAC, khóa tài khoản, rút tiền, dashboard, nhật ký, banner |

### 2.3 Lớp người dùng & tác nhân

#### 2.3.1 Tác nhân con người
| Mã | Tác nhân | Kế thừa | Mô tả |
|---|---|---|---|
| `ACT-GST` | Guest (khách vãng lai) | — | Người chưa đăng nhập. Xem danh mục, tìm kiếm (từ khóa gợi ý), xem chi tiết sản phẩm, sản phẩm tương tự, trang shop, đánh giá; mở link chia sẻ sản phẩm; thêm vào giỏ tạm (lưu trình duyệt); đăng ký tài khoản hoặc đăng nhập Google / Apple. |
| `ACT-CUS` | Customer (khách hàng) | `ACT-GST` | Đã đăng ký và kích hoạt tài khoản. Quản lý hồ sơ, sổ địa chỉ, giỏ hàng; đặt hàng nhiều shop trong 1 lần, thanh toán VNPay / VietQR (SePay) / COD; lưu và xếp chồng voucher 3 tầng, dùng Điểm; mua Flash Sale; theo dõi đơn, xác nhận đã nhận, đánh giá, bấm Hữu ích, báo cáo đánh giá; chat, theo dõi shop; yêu cầu trả hàng / hoàn tiền; xóa tài khoản. |
| `ACT-VND` | Vendor (người bán/shop) | `ACT-CUS` | Khách hàng đã mở shop và được duyệt KYC (cùng 1 tài khoản, có thêm chế độ Bán). Quản lý sản phẩm / SKU / giấy tờ chứng thực, đề xuất danh mục, xác nhận và xử lý đơn, voucher shop, đăng ký Flash Sale, phản hồi và báo cáo đánh giá, chat (trả lời tự động), cài đặt gian hàng / vận chuyển / tạm nghỉ, ví & sổ giao dịch, tài khoản nhận tiền và rút tiền. |
| `ACT-STF` | Staff (Nhân viên sàn) | — | Nhân viên vận hành sàn (role STAFF). Tác nhân tổng quát của 3 chức danh MOD / CS / FIN — quyền thật do Admin cấp theo từng mã quyền (UC-ADM-02). Dùng chung các use case tài khoản (đăng nhập, đổi mật khẩu, thông báo). |
| `ACT-MOD` | Moderator (Nhân viên kiểm duyệt) | `ACT-STF` | Chức danh MODERATOR: duyệt KYC, duyệt sản phẩm (kết quả Gemini), quản lý danh mục và duyệt đề xuất danh mục, kiểm duyệt đánh giá. |
| `ACT-CS` | CS Specialist (Nhân viên CSKH) | `ACT-STF` | Chức danh CS_SPECIALIST: phân xử tranh chấp trả hàng / hoàn tiền, thực hiện hoàn tiền, kiểm duyệt đánh giá. |
| `ACT-FIN` | Finance Officer (Nhân viên tài chính) | `ACT-STF` | Chức danh FINANCE_OFFICER: duyệt và chi lệnh rút tiền, cấu hình hoa hồng, xem sổ cái và dashboard tài chính, quản lý voucher sàn và chiến dịch Flash Sale. |
| `ACT-ADM` | Admin | `ACT-STF` | Quản trị viên cao nhất (không có Super Admin). Mọi quyền của nhân viên, cộng thêm: quản lý nhân viên & phân quyền, khóa / mở khóa người dùng, nhật ký hệ thống, thông báo toàn sàn, banner trang chủ. |

> Không có Super Admin: vai trò cao nhất là `ADMIN`. Ba chức danh nhân viên (`AdminProfile.staffTitle`) chỉ là **mẫu** nạp quyền mặc định; quyền thật là 13 mã quyền ở `staff_permissions`, Admin chỉnh từng quyền (UC-ADM-02) — xem LLD.md, mục 11.1.

#### 2.3.2 Tác nhân hệ thống
| Mã | Tác nhân | Tương tác |
|---|---|---|
| `ACT-PGW` | Payment Gateway (VNPay, SePay) | VNPay (sandbox khi demo): chuyển hướng thanh toán thẻ / QR, IPN HMAC-SHA512, API hoàn tiền. SePay: theo dõi biến động tài khoản ngân hàng của sàn, gửi webhook khi có tiền vào (khách chuyển khoản VietQR, GHN chuyển tiền COD) và tiền ra (xác nhận lệnh rút tiền / hoàn tiền nhân viên chuyển tay); SePay không tự chi tiền. |
| `ACT-3PL` | Đối tác vận chuyển (GHN, GHTK, ViettelPost, J&T) | GHN là hãng mặc định: API địa giới, tính phí, tạo vận đơn, in phiếu gửi, webhook hành trình, thu hộ COD và đối soát. GHTK / ViettelPost / J&T Express dùng cùng giao thức qua lớp adapter khi shop bật hãng đó (UC-LOG-15). |
| `ACT-AIW` | AI Worker Service (KYC & PhoBERT) | Dịch vụ Python FastAPI chạy riêng: (1) KYC — giải mã QR CCCD, đọc MRZ, phát hiện quốc huy / vân tay, so khớp khuôn mặt (ONNX) và chấm điểm rủi ro; (2) phân tích cảm xúc đánh giá bằng mô hình PhoBERT nhóm đã fine-tune (UC-AI-07). |
| `ACT-GEM` | Google Gemini API | Mô hình ngôn ngữ lớn của Google gọi qua REST API (structured output JSON). Dùng cho kiểm duyệt sản phẩm tự động (UC-CAT-12): nhận tên, mô tả, danh mục, ảnh sản phẩm; trả quyết định APPROVE / REJECT / NEED_REVIEW, điểm rủi ro và lý do. |
| `ACT-IDP` | Identity Provider (Google, Apple) | Nhà cung cấp danh tính OAuth2 / OpenID Connect cho đăng nhập mạng xã hội (UC-AUTH-03). |
| `ACT-MIN` | Silo S3 Storage | Lưu trữ đối tượng tương thích S3: ảnh sản phẩm / mô tả / đánh giá, ảnh đại diện, logo và ảnh bìa shop, banner, hồ sơ KYC và giấy tờ chứng thực sản phẩm (bucket riêng tư), bằng chứng trả hàng; cấp Presigned URL. Trong sơ đồ: nối với use case dùng chung «Tải tệp lên Silo (Presigned URL)». |
| `ACT-EML` | Email Service | Dịch vụ SMTP (Gmail SMTP / Mailtrap khi demo) gửi OTP, lời mời kích hoạt nhân viên, cảnh báo đăng nhập và email giao dịch. Trong sơ đồ: nối với use case dùng chung «Gửi mã OTP xác thực» và trực tiếp với các use case gửi email. |
| `ACT-CRN` | Platform Cron / Scheduler | Tác vụ định kỳ của Spring (@Scheduled): hủy đơn quá hạn thanh toán, tự hoàn tất đơn sau 7 ngày, giải ngân ký quỹ, mở / đóng Flash Sale, hết hạn Điểm, ẩn danh hóa tài khoản sau 30 ngày, tổng hợp lượt xem / từ khóa, tính chỉ số shop. |

Chi tiết: Usecase.md Phần I.

### 2.4 Môi trường vận hành
- Máy khách: trình duyệt hiện đại trên điện thoại và máy tính (360–1440 px); chưa có ứng dụng native.
- Máy chủ: 1 VPS 4 vCPU / 8 GB RAM, Docker Compose (Nginx, backend Spring Boot, AI Worker, frontend Astro, PostgreSQL 16, Redis 7, Silo).
- Môi trường: local (mỗi thành viên), staging / demo (VPS). Chi tiết: HLD.md, mục 11.

### 2.5 Ràng buộc thiết kế & hiện thực
- Java 21 + Spring Boot 3.3 (modular monolith), Python 3.12 + FastAPI cho AI Worker, Astro + React 19 + Tailwind cho frontend, PostgreSQL 16 / Redis 7 / Silo.
- Không dùng message broker, search engine riêng, Kubernetes (ADR-01…03, ADR-08).
- Tiền `DECIMAL(19,2)`; thời gian `TIMESTAMPTZ`; enum lưu chuỗi.
- Tên bảng, cột, enum, mã use case trùng bản DATN — chỉ được thêm.
- OTP chỉ qua email; không giữ ví tiền của người mua.

- **Tiền tệ:** VND, `DECIMAL(19,2)`.
- **Thanh toán:** sàn **không giữ ví tiền của người mua** (tránh yêu cầu giấy phép trung gian thanh toán của NHNN); tiền khách đi qua VNPay / chuyển khoản trực tiếp / GHN rồi về tài khoản doanh nghiệp của sàn (SePay chỉ **theo dõi** biến động số dư, không giữ tiền), được ghi nhận ở `escrow_ledgers` và chỉ chi cho shop theo quy tắc ký quỹ.
- **Dữ liệu cá nhân:** tuân thủ Luật Bảo vệ dữ liệu cá nhân 2025 (kế thừa NĐ 13/2023) ở mức MVP: thông báo & đồng ý khi đăng ký và khi nộp KYC, mã hóa ảnh CCCD (AES-256-GCM), che dữ liệu khi hiển thị, nhật ký truy cập hồ sơ KYC.
- **TMĐT:** sàn hiển thị thông tin người bán, quy chế hoạt động, chính sách đổi trả 7 ngày (tham khảo `BAO_CAO_HE_THONG_CHINH_SACH_HOP_DONG_PHAP_LY_v1.0.md`).

### 2.6 Giả định & phụ thuộc
| Phụ thuộc | Vai trò | Phương án khi lỗi |
|---|---|---|
| VNPay sandbox / production | Thanh toán online, hoàn tiền | Khách chọn COD; IPN lỡ → đối chiếu thủ công qua cổng merchant VNPay |
| GHN API | Phí ship, vận đơn, webhook | Phí cố định dự phòng 15.000 / 30.000đ; tạo vận đơn thử lại; webhook lỡ → job đồng bộ trạng thái mỗi 30 phút gọi API tra cứu |
| SePay | Webhook biến động tài khoản ngân hàng (VietQR, tiền GHN về, xác nhận lệnh chi) | SePay tự thử lại tối đa 7 lần; quá 24 giờ chưa khớp → Staff xác nhận tay theo sao kê |
| SMTP (Gmail / Mailtrap) | OTP (kênh OTP duy nhất), email giao dịch | Thử lại 3 lần; OTP vẫn hiển thị lỗi để người dùng gửi lại |
| AI Worker (nội bộ) | KYC tự động; phân tích cảm xúc đánh giá (PhoBERT) | KYC: hết thời gian / lỗi → hồ sơ chuyển `MANUAL_REVIEW` cho Staff. Cảm xúc: để `NULL`, Cron chạy lại; Phân tích bán hàng chỉ dùng số sao |
| Google Gemini API | Kiểm duyệt sản phẩm tự động (UC-CAT-12) | Hết thời gian / vượt hạn mức / JSON sai → sản phẩm vào hàng đợi Moderator (`decision = MANUAL_REVIEW`) |
| Google / Apple (OAuth2 / OIDC) | Đăng nhập mạng xã hội (UC-AUTH-03) | Ẩn nút đăng nhập mạng xã hội, người dùng đăng nhập email + mật khẩu / OTP |
| GHTK / ViettelPost / J&T Express | Hãng vận chuyển phụ khi shop bật (UC-LOG-15) | Bỏ qua hãng lỗi khi điều phối, dùng GHN |
| Silo | Ảnh, hồ sơ KYC | Chạy cùng Docker Compose, sao lưu ổ đĩa hằng ngày |

**Giả định:** một môi trường production duy nhất (VPS), một môi trường staging trên máy nhóm; người dùng truy cập qua trình duyệt (desktop & mobile web), chưa có ứng dụng native.

## 3. Yêu cầu giao diện ngoài

### 3.1 Giao diện người dùng
| Khu vực | Công nghệ | Màn hình chính |
|---|---|---|
| **Storefront** (`/`) | Astro SSR + React islands | Trang chủ (banner do Admin quản lý ở 6 vị trí kể cả popup, khối "Genie giữ tiền đến khi bạn nhận hàng", voucher, Flash Sale, danh mục, "Gợi ý cho bạn" → trang "Gợi ý hôm nay"), trang chiến dịch (đích của banner), tìm kiếm & lọc (từ khóa gợi ý / xu hướng), chi tiết sản phẩm (♥ yêu thích, Chia sẻ, Hữu ích ở đánh giá, sản phẩm tương tự), trang shop, giỏ hàng & mini-cart, checkout (voucher 3 tầng + Điểm), đơn của tôi, chat, thông báo, Điểm Genie & điểm danh, kho voucher, sản phẩm yêu thích, shop đang theo dõi, hồ sơ, liên kết đăng nhập Google / Apple, thiết bị đăng nhập |
| **Kênh Người bán** (`/seller`) | React SPA | Đăng ký shop & KYC, sản phẩm (giấy tờ chứng thực, đề xuất danh mục), đơn hàng (xác nhận / từ chối, in phiếu), vận chuyển (hãng, freeship), voucher shop, Flash Sale của shop, đánh giá, trả hàng, chat & tự trả lời, phân tích bán hàng (kể cả tỉ lệ cảm xúc đánh giá do PhoBERT chấm), ví & sổ giao dịch, rút tiền, thông tin & cài đặt gian hàng, tạm nghỉ |
| **Admin Portal** (`/admin`) | React SPA | Dashboard, duyệt KYC, duyệt sản phẩm (kết quả Gemini), danh mục, thuộc tính & hoa hồng, đề xuất danh mục, kiểm duyệt đánh giá (ưu tiên đánh giá PhoBERT gắn tiêu cực), voucher sàn, Flash Sale, khiếu nại & hoàn tiền, đối soát COD, rút tiền, sổ cái tài chính, người dùng, nhân viên & phân quyền, nhật ký hệ thống, banner, thông báo toàn sàn — mục nav hiện theo mã quyền |

- **Responsive** từ 360 px (mobile web) đến 1440 px; Tailwind breakpoint `sm/md/lg`.
- **Chuyển chế độ Mua / Bán** bằng nút trên header khi tài khoản là Vendor — không đăng nhập lại.
- **SEO & chia sẻ** cho trang sản phẩm / danh mục / shop: SSR, thẻ `title` / `meta description`, Open Graph (ảnh, tên, giá khi dán link deeplink vào Messenger / Zalo — UC-CAT-18), `sitemap.xml`, JSON-LD `Product`.
- **Đa ngôn ngữ:** chuỗi giao diện tách file `vi.json` / `en.json`, chọn ngôn ngữ ở header, lưu trong cookie; dữ liệu do người dùng nhập (tên sản phẩm, mô tả) giữ nguyên ngôn ngữ gốc.
- **Khả năng truy cập:** độ tương phản WCAG AA, nhãn cho ô nhập, điều hướng bàn phím ở form chính.
- **Chủ đề màu:** prototype có bộ chủ đề màu (Đèn thần, Phố Hội, Trống đồng…) chọn ở thanh demo để so sánh thiết kế — đây là công cụ trình diễn, không phải yêu cầu chức năng; bản hiện thực dùng một chủ đề mặc định.

### 3.2 Giao diện phần cứng
Không có thiết bị chuyên dụng. Camera điện thoại / máy tính dùng để chụp selfie KYC (thử thách nháy mắt / quay đầu); máy in A6 (tùy chọn) để in phiếu gửi hàng.

### 3.3 Giao diện phần mềm & hệ thống ngoài

| Hệ thống | Giao thức | Dữ liệu trao đổi | Xác thực |
|---|---|---|---|
| VNPay `vnp_Version 2.1.0` | HTTPS redirect, IPN, API refund | Số tiền, mã giao dịch, kết quả | Chữ ký HMAC-SHA512 |
| SePay | Webhook HTTPS (JSON) | Biến động số dư: `id`, `content`, `transferType`, `transferAmount` | Header `Authorization: Apikey …` |
| GHN / GHTK / ViettelPost / J&T | REST + webhook | Phí ship, vận đơn, hành trình, đối soát COD | Token / khóa riêng từng hãng |
| Google Gemini API | REST, structured output JSON | Tên, mô tả, danh mục, ≤ 4 ảnh sản phẩm → quyết định, điểm rủi ro | API key (chỉ ở backend) |
| AI Worker (nội bộ) | HTTP nội bộ + callback | Ảnh KYC (khóa Silo); nội dung đánh giá → nhãn cảm xúc | Token nội bộ |
| Google / Apple | OAuth2 / OIDC (Authorization Code + PKCE) | `id_token` (`sub`, `email`, `email_verified`) | JWKS |
| SMTP | SMTP + TLS | OTP, lời mời nhân viên, email giao dịch | Tài khoản SMTP |
| Silo | S3 API, Presigned URL | Tệp ảnh, video, giấy tờ | Khóa truy cập (backend), URL ký (client) |

### 3.4 Giao diện truyền thông
HTTPS (TLS, HSTS) cho mọi kết nối từ ngoài; REST JSON `/api/v1` theo chuẩn ở LLD.md, mục 8; WebSocket STOMP qua `wss://` cho chat và thông báo; webhook đối tác là HTTPS công khai có xác thực chữ ký / khóa.

## 4. Yêu cầu chức năng

> Mỗi FR truy vết tới use case ở Usecase.md. Ưu tiên MoSCoW ở Phụ lục A; FR không ghi chú là **MUST**.

### 4.1 FR-AUTH: Xác thực & Tài khoản
- **FR-AUTH-01** Đăng ký bằng email, xác thực OTP 6 số (TTL 5 phút, gửi lại sau 60 giây, tối đa 5 mã / giờ, sai quá 5 lần hủy phiên); dữ liệu đăng ký giữ tạm ở Redis 15 phút, chỉ tạo tài khoản khi OTP đúng; email không phân biệt hoa thường — UC-AUTH-01.
- **FR-AUTH-02** Đăng nhập email + mật khẩu hoặc OTP email; JWT access 15 phút, refresh token 7 ngày lưu băm và xoay vòng khi làm mới; sai 5 lần (mật khẩu hoặc OTP) khóa tạm 15 phút bằng `lockedUntil`, mở khóa sớm qua OTP email; tài khoản chỉ có Google đăng nhập bằng mật khẩu → `GNE-AUTH-4012` (không tính lần sai); cảnh báo email khi đăng nhập từ thiết bị / mạng lạ — UC-AUTH-02.
- **FR-AUTH-03** Đăng nhập Google / Apple (OAuth2 / OIDC): email đã có thì liên kết, chưa có thì tạo tài khoản khách hàng; tài khoản chỉ có đăng nhập mạng xã hội được đặt mật khẩu sau; mục **Liên kết đăng nhập** ở Tài khoản cho liên kết / gỡ liên kết Google (chỉ gỡ được khi tài khoản đã có mật khẩu); không liên kết khi IdP trả `email_verified = false` hoặc email trùng tài khoản đang bị khóa — UC-AUTH-03 (**SHOULD**). Liên kết thủ công yêu cầu email Google trùng email tài khoản (`GNE-AUTH-4098`); gỡ liên kết phải nhập lại mật khẩu; mỗi tài khoản tối đa một nhà cung cấp; Apple bật sau (cần Apple Developer Program).
- **FR-AUTH-04** Quên mật khẩu qua OTP email (tài khoản chỉ có Google dùng để thiết lập mật khẩu; đặt lại xong thu hồi mọi phiên); đổi mật khẩu cần mật khẩu cũ + OTP bước 2, thiết lập lần đầu chỉ cần OTP; đổi xong thu hồi phiên ở thiết bị khác — UC-AUTH-04, UC-AUTH-06.
- **FR-AUTH-05** Đăng xuất thu hồi phiên hiện tại (refresh token + chặn access token theo `jti`); màn **Thiết bị đăng nhập** liệt kê phiên (mã phiên `family_id` ổn định), đăng xuất từng thiết bị hoặc mọi thiết bị khác — UC-AUTH-11.
- **FR-AUTH-06** Hồ sơ cá nhân (ảnh đại diện qua Silo) và sổ địa chỉ có mã tỉnh / huyện / xã GHN, một địa chỉ mặc định — UC-AUTH-08, UC-AUTH-09.
- **FR-AUTH-07** Xóa tài khoản: xác nhận OTP, chặn khi còn đơn chưa hoàn tất / số dư ví shop; trong 30 ngày vẫn đăng nhập và hủy yêu cầu được (các thiết bị khác bị đăng xuất); sau 30 ngày cron 02:30 ẩn danh hóa dữ liệu cá nhân, giữ đơn hàng & sổ cái — UC-AUTH-10 (**SHOULD**).

### 4.2 FR-CAT: Danh mục, Sản phẩm & Tìm kiếm
- **FR-CAT-01** Cây danh mục tối đa 3 cấp, mỗi danh mục có tỷ lệ hoa hồng và bộ thuộc tính chuẩn (bắt buộc / tùy chọn, kiểu dữ liệu, danh sách giá trị) — UC-CAT-01, UC-CAT-14, UC-CAT-15.
- **FR-CAT-02** Tìm kiếm full-text tiếng Việt có / không dấu, chịu gõ sai nhẹ; gợi ý từ khóa khi gõ và từ khóa xu hướng (`search_keyword_stats`); lọc theo danh mục, thương hiệu, khoảng giá, đánh giá, tình trạng; sắp xếp mới nhất / bán chạy / giá — UC-CAT-02, UC-CAT-04, UC-CAT-05.
- **FR-CAT-03** Chi tiết sản phẩm với ma trận biến thể (tối đa 2 thuộc tính), ảnh theo biến thể, tồn khả dụng, thương hiệu, tình trạng, hàng đặt trước (số ngày chuẩn bị); ghi lượt xem theo ngày / nguồn truy cập (`product_view_stats`); nút ♥ yêu thích — UC-CAT-06, UC-CAT-08.
- **FR-CAT-04** Sản phẩm tương tự cùng danh mục / thương hiệu / khoảng giá (truy vấn SQL, không dùng vector) — UC-CAT-07 (**SHOULD**).
- **FR-CAT-05** Trang shop: logo, ảnh bìa, mô tả, **điểm đánh giá shop, số người theo dõi, số sản phẩm đang bán, tỉ lệ phản hồi chat & thời gian phản hồi trung bình**, ngày tham gia, danh sách sản phẩm, nút Theo dõi / Chat — UC-CAT-16.
- **FR-CAT-06** Chia sẻ sản phẩm: nút "Chia sẻ" tạo link `https://genie.vn/p/{slug}-{productId}?v={variantId}&src=share` (Web Share API hoặc sao chép); trang render SSR có thẻ Open Graph để hiện ảnh xem trước trên Facebook / Zalo / Messenger; lượt mở link tính nguồn `EXTERNAL_LINK` — UC-CAT-18 (**SHOULD**).
- **FR-CAT-07** Shop thêm / sửa / ẩn / xóa mềm sản phẩm; tối đa 8 ảnh + 1 video / sản phẩm, 50 SKU / sản phẩm; thương hiệu, tình trạng (mới / đã qua sử dụng), hàng đặt trước; tải giấy tờ chứng thực (giấy phép, CO/CQ) khi danh mục yêu cầu — UC-CAT-09…11.
- **FR-CAT-08** Kiểm duyệt 2 bước: **Gemini API** chấm tự động ngay sau khi lưu (JSON theo schema: `decision` APPROVE / REJECT / NEED_REVIEW, `riskScore` 0–100, `violations[]`, `reasoning`); `REJECT` và `riskScore` ≥ 85 → `REJECTED` kèm lý do; `NEED_REVIEW` hoặc `REJECT` dưới ngưỡng → `FLAGGED`, hàng chờ ưu tiên cao; `APPROVE` → hàng chờ thường. **Moderator luôn là người đăng bán cuối cùng** (TLCN không cho AI tự đăng) — UC-CAT-12, UC-CAT-13.
- **FR-CAT-09** Shop đề xuất danh mục mới khi không có danh mục phù hợp; Moderator / Admin duyệt (tạo danh mục) hoặc từ chối có lý do — UC-CAT-17 (**SHOULD**).

### 4.3 FR-ORD: Giỏ hàng & Đơn hàng
- **FR-ORD-01** Giỏ hàng tài khoản; khách vãng lai lưu giỏ tạm ở trình duyệt và gộp khi đăng nhập; mini-cart ở header; cấm mua sản phẩm của chính shop mình (`GNE-ORD-4094`) — UC-ORD-01.
- **FR-ORD-02** Xem trước & tách đơn theo shop, phí ship theo hãng đã chọn cho từng shop, áp voucher 3 tầng + Điểm, ghi chú cho shop — UC-ORD-04, UC-ORD-06.
- **FR-ORD-03** Tạo phiên thanh toán: 1 `CheckoutSession` + N `Order`, snapshot giá / địa chỉ / voucher / Điểm, giữ chỗ tồn kho 15 phút — UC-ORD-07.
- **FR-ORD-04** Scheduler 60 giây hủy phiên quá hạn, nhả tồn, hoàn lượt voucher và Điểm — UC-ORD-08.
- **FR-ORD-05** Theo dõi đơn theo dòng thời gian trạng thái + hành trình hãng vận chuyển — UC-ORD-09.
- **FR-ORD-06** Khách hủy đơn khi shop chưa giao cho hãng vận chuyển (hoàn tiền tự động nếu đã trả VNPay, hoàn Điểm) — UC-ORD-10.
- **FR-ORD-07** Xác nhận đã nhận hàng; tự hoàn tất sau 7 ngày từ khi `DELIVERED` nếu không có khiếu nại — UC-ORD-11, UC-ORD-12.

### 4.4 FR-PAY: Thanh toán, Ký quỹ & Sổ cái
- **FR-PAY-01** Thanh toán VNPay (`vnp_Version 2.1.0`, chữ ký HMAC-SHA512) hoặc **chuyển khoản VietQR** (QR động, nội dung `TR{paymentTransactionId}`), hết hạn 15 phút; hoặc **COD** — UC-PAY-01.
- **FR-PAY-02** IPN VNPay và webhook SePay idempotent: kiểm tra chữ ký / API Key, số tiền, trạng thái đã xử lý (SePay chống trùng theo `id`); Return URL chỉ hiển thị, không cập nhật trạng thái — UC-PAY-03.
- **FR-PAY-03** Ghi ký quỹ cho từng đơn con: `grossAmount`, `commissionFee` (theo từng dòng hàng: shop → ngành → 5%), `netAmount`; **COD kết hợp Escrow**: ký quỹ `PENDING_FUNDING` tới khi tiền thu hộ về tài khoản sàn và đối soát xong mới `HELD` — UC-PAY-05.
- **FR-PAY-04** Mọi biến động tiền ghi **sổ cái kép bất biến** (`financial_ledger`: nợ / có cân bằng, chỉ ghi thêm, bút toán đảo khi điều chỉnh) — UC-PAY-06.
- **FR-PAY-05** Giải ngân tự động vào ví shop khi đơn `COMPLETED` và ký quỹ `HELD`; ví có `balanceHash` chống sửa trực tiếp CSDL — UC-PAY-07.

### 4.5 FR-VND: Gian hàng & KYC
- **FR-VND-01** Mở shop từ tài khoản khách (một tài khoản một shop) — UC-VND-01.
- **FR-VND-02** Nộp CCCD 2 mặt + selfie (kiểm tra mờ / lóa phía client), ảnh mã hóa AES-256-GCM trên Silo — UC-VND-02.
- **FR-VND-03** AI thẩm định theo tầng: QR mặt trước, MRZ mặt sau, đối chiếu QR ↔ MRZ, dấu hiệu quốc huy / vân tay ghi vào `riskSignalsJson`, so khớp khuôn mặt + liveness, chấm `riskScore` (< 30 tự duyệt, 30–69 duyệt tay, ≥ 70 từ chối; lấy mẫu 5% hồ sơ tự duyệt để hậu kiểm) — UC-VND-03, UC-VND-04.
- **FR-VND-04** Kho lấy hàng (một kho), đăng ký cửa hàng GHN; cấu hình vận chuyển của shop (`vendor_shipping_config`: hãng bật, ngưỡng freeship) — UC-VND-05, UC-VND-21.
- **FR-VND-05** Tài khoản nhận tiền phải **chính chủ** (tên chủ tài khoản khớp họ tên CCCD) — UC-VND-07.
- **FR-VND-06** Báo cáo doanh thu & phân tích: doanh thu, số đơn, tỷ lệ hủy, tiền đang ký quỹ, lượt xem / tỉ lệ chuyển đổi theo nguồn truy cập, sản phẩm bán chạy; kết quả đơn (hoàn tất / khách hủy / shop từ chối / trả hàng), tiền thực về shop (sau hoa hồng và voucher shop), tỉ lệ shop tự hủy / từ chối, số khiếu nại shop thua và số đơn sàn tự xác nhận do quá 24 giờ trong 30 ngày; tỉ lệ đánh giá tích cực / trung tính / tiêu cực do PhoBERT chấm (UC-AI-07) — UC-VND-08 (**SHOULD**).
- **FR-VND-07** Tin nhắn tự trả lời (chào mừng, ngoài giờ) — UC-VND-06; chế độ tạm nghỉ (ẩn nút mua, tự tắt khi hết hạn) — UC-VND-19 (**SHOULD**).
- **FR-VND-08** Ví & sổ giao dịch người bán: số dư khả dụng, tiền đang ký quỹ, lịch sử `wallet_transactions` lọc theo loại / thời gian, xuất CSV — UC-VND-20.
- **FR-VND-09** Thông tin & cài đặt gian hàng: tên, logo, ảnh bìa, mô tả, giờ hỗ trợ, bật hàng đặt trước, hãng vận chuyển & freeship — UC-VND-21.

### 4.6 FR-LOG: Vận chuyển qua 3PL
- **FR-LOG-01** Shop **xác nhận hoặc từ chối** đơn mới (từ chối kèm lý do → `CANCELLED_BY_VENDOR`, hoàn kho, hoàn tiền / Điểm / voucher); quá 24 giờ cron tự xác nhận và ghi điểm "chậm xác nhận" của shop — UC-LOG-16.
- **FR-LOG-02** Shop đóng gói, tạo vận đơn và in phiếu A6 (hạn bàn giao 2 ngày làm việc) — UC-LOG-01, UC-LOG-02.
- **FR-LOG-03** Điều phối đa hãng: gọi tính phí song song các hãng shop đã bật (GHN mặc định, GHTK, ViettelPost, J&T; timeout 3 giây mỗi hãng), chọn phí thấp nhất trong các hãng có thời gian giao ≤ hãng nhanh nhất + 1 ngày; webhook mọi hãng chuẩn hóa về `ShipmentTrackingStatus` qua adapter — UC-LOG-15.
- **FR-LOG-04** Webhook hãng vận chuyển (xác thực token, idempotent theo `trackingCode + status + time`) cập nhật `ShipmentEvent` và trạng thái đơn — UC-LOG-03.
- **FR-LOG-05** Đối soát COD: SePay báo tiền hãng chuyển về, Finance Officer tải bảng kê, hệ thống khớp tổng tiền và từng vận đơn, nạp ký quỹ — UC-LOG-03.

### 4.7 FR-PRO: Khuyến mãi, Voucher, Flash Sale & Điểm
- **FR-PRO-01** Voucher shop (giảm % có trần / số tiền cố định, đơn tối thiểu, tổng lượt, lượt / người, ngân sách, phạm vi danh mục / sản phẩm) — UC-PRO-01.
- **FR-PRO-02** Voucher sàn và voucher Freeship (sàn tài trợ, `platformSharePercent` khi đồng tài trợ) — UC-PRO-02.
- **FR-PRO-03** Khách lưu voucher vào kho cá nhân — UC-PRO-03.
- **FR-PRO-04** **Xếp chồng 3 tầng + Điểm** khi checkout: Tầng 1 — tối đa 1 voucher Shop cho **mỗi** shop; Tầng 2 — 1 voucher Sàn trên tổng sau tầng 1; Tầng 3 — 1 mã Freeship trừ vào phí ship; sau cùng Điểm tối đa 50% tiền hàng còn lại (1 Điểm = 1₫); hệ thống tự gợi ý bộ mã giảm nhiều nhất; phân bổ chiết khấu xuống từng dòng hàng — UC-PRO-04.
- **FR-PRO-05** Flash Sale: nhân viên (Finance Officer / Admin) tạo chiến dịch & khung giờ, shop đăng ký sản phẩm (giảm ≥ 15% so với giá 30 ngày, suất ≤ tồn khả dụng), nhân viên duyệt; cron mở / đóng; mua chống bán vượt suất bằng UPDATE có điều kiện trên `flash_stock − sold_count − reserved_count` — UC-PRO-05, UC-PRO-06 (**SHOULD**).
- **FR-PRO-06** Điểm: điểm danh hằng ngày (ngày 1–6: 100 Điểm, ngày 7 liên tiếp: 1.000 Điểm), tra cứu số dư, Điểm sắp hết hạn & lịch sử, đổi Điểm lấy voucher sàn (trừ lô sắp hết hạn trước — FEFO); Điểm hết hạn sau **12 tháng**; dùng Điểm giảm trừ khi thanh toán, sàn chịu phần giảm — UC-PRO-08, UC-PRO-09, UC-PRO-10 (**SHOULD**).

### 4.8 FR-SOC: Chat, Đánh giá & Theo dõi shop
- **FR-SOC-01** Chat khách – shop thời gian thực (STOMP over WebSocket), văn bản + ảnh, trạng thái đã xem, lưu lịch sử — UC-SOC-01 (**SHOULD**).
- **FR-SOC-02** Gửi thẻ sản phẩm (kèm biến thể), ảnh và thẻ trạng thái đơn trong chat — UC-SOC-02 (**SHOULD**).
- **FR-SOC-03** Đánh giá 1–5 sao, chữ, tối đa 5 ảnh + 1 video, chỉ với dòng hàng của đơn `COMPLETED`, mỗi dòng 1 lần; nút **"Hữu ích"** (mỗi người 1 lần, `helpfulCount`), sắp xếp theo hữu ích — UC-SOC-03.
- **FR-SOC-04** Shop phản hồi đánh giá một lần — UC-SOC-04.
- **FR-SOC-05** Báo cáo đánh giá vi phạm / spam; Moderator / CS ẩn hoặc giữ đánh giá — UC-SOC-05 (**SHOULD**).
- **FR-SOC-06** Theo dõi / bỏ theo dõi shop, danh sách shop đang theo dõi, đếm `followerCount` — UC-SOC-06 (**SHOULD**).
- **FR-SOC-07** Phân tích cảm xúc tổng thể của đánh giá có nội dung chữ bằng mô hình **PhoBERT** nhóm đã fine-tune (3 nhãn `POSITIVE` / `NEUTRAL` / `NEGATIVE` + độ tin cậy), chạy bất đồng bộ trong AI Worker sau khi lưu đánh giá, lưu ở `product_reviews.aspectSentimentJson` (khóa `overall`); dùng cho Phân tích bán hàng của shop và ưu tiên hàng chờ kiểm duyệt đánh giá; kết quả chỉ tham khảo, không tự ẩn đánh giá — UC-AI-07 (**SHOULD**).

### 4.9 FR-SUP: Trả hàng & Hoàn tiền
- **FR-SUP-01** Yêu cầu trả hàng / hoàn tiền trong 7 ngày từ khi giao, kèm bằng chứng; ký quỹ đóng băng (`DISPUTED`) — UC-SUP-01.
- **FR-SUP-02** Shop phản hồi trong 48 giờ (đồng ý hoàn, đồng ý nhận lại hàng, yêu cầu thêm bằng chứng, từ chối); im lặng quá hạn → tự chấp thuận — UC-SUP-02.
- **FR-SUP-03** Khách khiếu nại lên sàn trong 48 giờ sau khi shop từ chối; CS Specialist phân xử — UC-SUP-03.
- **FR-SUP-04** Hoàn tiền VNPay qua API refund; VietQR / COD chuyển khoản tay với nội dung `TRRF{id}`, SePay xác nhận; hoàn một phần giữ đúng "khách + shop + sàn = tiền đơn"; Điểm đã dùng hoàn theo tỉ lệ — UC-SUP-04.

### 4.10 FR-NOT: Thông báo
- **FR-NOT-01** Thông báo in-app lưu bảng `notifications` và đẩy qua WebSocket khi người dùng online; badge số chưa đọc; chuông Admin Console dùng bảng riêng `admin_notifications` (hộp thư chung của đội vận hành: đơn mới, KYC chờ duyệt, yêu cầu rút tiền, lệch số dư, cảnh báo hệ thống — có `referenceUrl` tới trang xử lý) — UC-NOT-01.
- **FR-NOT-02** Email giao dịch: xác nhận đơn, giao thành công, hoàn tiền, kết quả KYC, kết quả rút tiền — UC-NOT-03.
- **FR-NOT-03** Admin phát thông báo toàn sàn (tiêu đề, nội dung, ảnh banner) theo đối tượng `ALL` / `CUSTOMER` / `VENDOR` / `SPECIFIC_USERS`; đẩy WebSocket cho người đang trực tuyến; thống kê đã đọc / tỉ lệ đọc — UC-NOT-05 (**SHOULD**).

### 4.11 FR-ADM: Quản trị sàn, Nhân viên & Rút tiền
- **FR-ADM-01** Admin mời nhân viên qua email (link kích hoạt 48 giờ), chọn 1 trong 3 chức danh; khóa / mở nhân viên — UC-ADM-01.
- **FR-ADM-02** RBAC 2 tầng: vai trò (`CUSTOMER`, `VENDOR`, `STAFF`, `ADMIN`) + 13 mã quyền cho STAFF; Admin cấp / thu hồi từng quyền; nav Admin hiện theo quyền; API kiểm tra `@PreAuthorize` — UC-ADM-02.
- **FR-ADM-03** Khóa / mở khóa tài khoản có lý do và thời hạn, ghi nhật ký — UC-ADM-03.
- **FR-ADM-04** Shop yêu cầu rút tiền (tối thiểu 100.000đ, tối đa số dư khả dụng); Finance Officer duyệt / từ chối, chuyển khoản tay với nội dung `TRWD{id}`; webhook SePay (tiền ra) tự hoàn tất lệnh — UC-ADM-04, UC-ADM-05.
- **FR-ADM-05** Dashboard GMV, hoa hồng, ký quỹ đang giữ, COD chờ đối soát, chi phí voucher sàn / Điểm, xuất CSV — UC-ADM-06 (**SHOULD**).
- **FR-ADM-06** Tra cứu nhật ký hệ thống (lọc theo người thực hiện, hành động, thời gian) — UC-ADM-07 (**SHOULD**).
- **FR-ADM-07** Quản lý banner (ảnh desktop / mobile; liên kết đích chọn từ dữ liệu sàn: danh mục, sản phẩm, cửa hàng, khu Flash Sale, trang Gợi ý hôm nay hoặc trang chiến dịch, chỉ trong miền sàn; 6 vị trí `HOME_TOP_CAROUSEL`, `HOME_TOP_SIDE`, `HOME_MIDDLE`, `CATEGORY_TOP`, `FLASH_SALE_TOP`, `POPUP`; thứ tự, thời gian hiển thị, tối đa 8 banner đang hiển thị mỗi vị trí; đếm lượt hiển thị / bấm, CTR; "Tắt ngay") và trang chiến dịch tĩnh làm đích của banner — UC-ADM-12 (**SHOULD**).

Luồng chính, luồng thay thế, tiền / hậu điều kiện của từng use case: Usecase.md, Phần III.

## 5. Yêu cầu phi chức năng

> Các mục "use case kỹ thuật" của bản đầy đủ (xoay vòng token, khóa brute-force, idempotency, rate limit, nhật ký kiểm toán, sổ ví…) được đặt ở đây vì không có tác nhân chủ động — đúng chuẩn UML.

| Mã | Nhóm | Yêu cầu | Cách kiểm chứng |
|---|---|---|---|
| NFR-01 | Hiệu năng | API đọc P95 < 300 ms, API ghi P95 < 500 ms với 50 CCU; trang sản phẩm SSR < 1,5 s (LCP) | k6 50 VU × 10 phút; Lighthouse |
| NFR-02 | Sẵn sàng | ≥ 99% thời gian demo / thử nghiệm; khởi động lại tự động (`restart: unless-stopped`) | Uptime Kuma |
| NFR-03 | Toàn vẹn tồn kho | Không bán vượt tồn: giữ chỗ bằng `UPDATE … WHERE stock_count - reserved_count >= ?` + `@Version` | Test 100 luồng đặt cùng 1 SKU còn 10 |
| NFR-04 | Toàn vẹn tiền | Tiền `DECIMAL(19,2)`; IPN / webhook idempotent; `balanceHash` HMAC-SHA256 ở ví shop; sổ `wallet_transactions` chỉ ghi thêm, `balanceBefore + amount = balanceAfter` | Test IPN gửi lặp 5 lần; job đối soát đêm |
| NFR-05 | Bảo mật xác thực | Mật khẩu Argon2id; JWT HS256 15 phút; refresh token băm SHA-256, xoay vòng, phát hiện dùng lại → thu hồi cả họ token; khóa 15 phút sau 5 lần sai | Test tích hợp Spring Security |
| NFR-06 | Bảo mật ứng dụng | HTTPS bắt buộc; CORS theo domain; chống OWASP Top 10 cơ bản (Bean Validation, JPA tham số hóa, escape đầu ra, CSRF cho form cookie); rate limit Redis cho đăng nhập / OTP / tìm kiếm | OWASP ZAP baseline scan |
| NFR-07 | Dữ liệu cá nhân | Ảnh CCCD mã hóa AES-256-GCM, bucket riêng tư, xem qua Presigned URL 5 phút; che số CCCD / STK khi hiển thị; ghi `system_logs` khi Staff xem hồ sơ | Rà soát mã |
| NFR-08 | Kiểm toán | Ghi `system_logs` cho đăng nhập, khóa tài khoản, duyệt KYC / sản phẩm, giải ngân, rút tiền, hoàn tiền, đổi hoa hồng | Kiểm tra bản ghi sau mỗi luồng E2E |
| NFR-09 | Khả năng bảo trì | Modular monolith: mỗi phân hệ 1 package (`auth`, `catalog`, `order`, `payment`, `vendor`, `shipping`, `promotion`, `social`, `support`, `notification`, `admin`), chỉ giao tiếp qua service interface / sự kiện; tích hợp ngoài (VNPay, SePay, 3PL, Gemini, OAuth) sau interface adapter; Flyway migration; coverage service ≥ 60% | SonarCloud |
| NFR-10 | Quan sát | Log JSON có `traceId`; Spring Boot Actuator `/health`, `/metrics`; cảnh báo email khi health DOWN | Kiểm tra thủ công |
| NFR-11 | Sao lưu | `pg_dump` hằng ngày giữ 7 bản; sao lưu volume Silo hằng tuần; thử khôi phục 1 lần trước demo | Biên bản thử khôi phục |
| NFR-12 | Khả năng mở rộng mô hình | Tên bảng / cột / enum, mã & tên use case trùng DATN; chỉ thêm, không đổi tên | Đối chiếu ENTITY.md Phụ lục D, `Quản Lý UseCase.xlsx` của DATN |
| NFR-13 | Phân quyền | Mọi API quản trị có `@PreAuthorize` theo mã quyền; giao diện chỉ ẩn mục, không thay kiểm tra phía máy chủ; truy cập trái quyền ghi `ACCESS_DENIED` | Test tích hợp theo ma trận quyền ở LLD.md, mục 11.1 |
| NFR-14 | Đa ngôn ngữ | 100% chuỗi giao diện có khóa i18n vi / en; định dạng tiền / ngày theo ngôn ngữ | Script kiểm thiếu khóa trong CI |
| NFR-15 | Chi phí AI | Gemini chỉ gọi khi tạo / sửa tên, mô tả, ảnh sản phẩm; tối đa 4 ảnh / lần; không gửi dữ liệu cá nhân và nội dung giấy tờ. PhoBERT tự host trong AI Worker (0 đồng / lượt), mỗi đánh giá có chữ chạy 1 lần | Đếm lượt gọi theo `product_moderation_results`; đếm đánh giá có / chưa có `aspectSentimentJson` |

## 6. Yêu cầu dữ liệu

| Nhóm | Entity | Ghi chú |
|---|---|---|
| 1. Người dùng & định danh | User, CustomerProfile, Address, VendorProfile, AdminProfile, KycResult, RefreshToken, StaffPermission | 1 tài khoản – nhiều hồ sơ theo vai trò; quyền nhân viên tách bảng |
| 2. Danh mục & sản phẩm | Category, Product, ProductVariant, ProductImage, UserWishlist, CategoryAttribute, Brand, ProductDocument, CategoryProposal, ProductModerationResult, SearchKeywordStat, ProductViewStat | SPU / SKU tách biệt; kiểm duyệt & thống kê truy cập |
| 3. Giỏ hàng | Cart, CartItem | Giỏ trỏ SKU |
| 4. Khuyến mãi, voucher, flash sale & điểm | Voucher, VoucherUsage, UserVoucher, VoucherCategory, VoucherProduct, VoucherExcludedCategory, VoucherExcludedProduct, FlashSaleCampaign, FlashSaleItem, UserCoin, CoinTransaction, DailyCheckin | 3 tầng + phạm vi; Flash Sale; Điểm 12 tháng |
| 5. Đơn hàng | CheckoutSession, Order, OrderItem, OrderDiscount, StockReservation | Snapshot bất biến |
| 6. Thanh toán & ký quỹ | PaymentTransaction, PaymentRefund, EscrowLedger, VendorWallet, WalletTransaction, WithdrawalRequest, FinancialLedger | Tiền đi một chiều có kiểm soát; sổ cái kép |
| 7. Vận chuyển | ShipmentTracking, ShipmentEvent, VendorShippingConfig | Đa hãng 3PL |
| 8. Hoàn tiền / khiếu nại | RefundRequest, RefundRequestItem, RefundEvidence | Hoàn toàn phần / một phần |
| 9. Đánh giá & tương tác shop | ProductReview, ShopFollow, ReviewReport, ReviewHelpfulVote | Hữu ích, báo cáo, theo dõi |
| 10. Thông báo & nội dung sàn | Notification, PlatformNotification, UserPlatformNotificationRead, HomeBanner, AdminNotification | Cá nhân + toàn sàn + chuông Admin + banner |
| 11. Nhắn tin (chat) | Conversation, Message | Chat realtime |
| 12. Nhật ký hệ thống | SystemLog | Chỉ ghi thêm |

Đặc tả đầy đủ từng cột, ràng buộc, phương thức: ENTITY.md.

### 6.1 Lưu giữ & vòng đời dữ liệu

| Dữ liệu | Thời hạn | Yêu cầu |
|---|---|---|
| OTP, dữ liệu đăng ký tạm | 5–15 phút | Hết hạn tự động |
| Giữ chỗ tồn kho / đơn chờ thanh toán | 15 phút | Hết hạn thì hủy, nhả tồn, hoàn voucher / Điểm |
| Tài khoản yêu cầu xóa | 30 ngày | Ẩn danh hóa, giữ đơn hàng & sổ cái |
| Điểm Genie | 12 tháng | Hết hạn theo lô (trừ lô sắp hết hạn trước) |
| Sổ cái, sổ ví, lịch sử Điểm, nhật ký hệ thống | Vĩnh viễn | Chỉ ghi thêm |
| Ảnh CCCD | Theo hồ sơ KYC | Mã hóa AES-256-GCM; embedding khuôn mặt xóa khi hồ sơ bị từ chối quá 30 ngày |
| Sao lưu CSDL | 7 bản hằng ngày | Thử khôi phục trước demo |

Phân bổ dữ liệu giữa PostgreSQL / Redis / Silo / trình duyệt: LUU_TRU_DU_LIEU.md.

## 7. Ma trận truy vết

| Nhóm FR | Use case | Entity chính | Sơ đồ lớp |
|---|---|---|---|
| FR-AUTH | AUTH-01, AUTH-02, AUTH-03, AUTH-04, AUTH-06, AUTH-08, AUTH-09, AUTH-10, AUTH-11 | `User`, `CustomerProfile`, `RefreshToken`, `SystemLog`, `Notification`, `Address` | 1, 10 |
| FR-CAT | CAT-01, CAT-02, CAT-04, CAT-05, CAT-06, CAT-07, CAT-08, CAT-09, CAT-10, CAT-11, CAT-12, CAT-13, CAT-14, CAT-15, CAT-16, CAT-17, CAT-18 | `Category`, `Product`, `ProductVariant`, `SearchKeywordStat`, `VendorProfile`, `Brand`, `VendorShippingConfig`, `ProductImage`, `ProductViewStat`, `UserWishlist`, `ProductDocument`, `CategoryAttribute`, `ProductModerationResult`, `Notification`, `AdminProfile`, `SystemLog`, `CategoryProposal`, `ProductReview`, `ShopFollow` | 1, 2, 6, 7, 8, 10 |
| FR-ORD | ORD-01, ORD-04, ORD-06, ORD-07, ORD-08, ORD-09, ORD-10, ORD-11, ORD-12 | `Cart`, `CartItem`, `CheckoutSession`, `VendorProfile`, `Address`, `Product`, `VendorShippingConfig`, `Order`, `OrderItem`, `OrderDiscount`, `UserCoin`, `FlashSaleItem`, `StockReservation`, `ShipmentTracking`, `ShipmentEvent`, `EscrowLedger`, `VendorWallet`, `WalletTransaction`, `RefundRequest` | 1, 2, 4, 5, 6, 9 |
| FR-PAY | PAY-01, PAY-03, PAY-05, PAY-06, PAY-07 | `PaymentTransaction`, `CheckoutSession`, `Order`, `AdminNotification`, `EscrowLedger`, `OrderItem`, `VendorProfile`, `Category`, `FinancialLedger`, `VendorWallet`, `WalletTransaction` | 1, 2, 4, 5, 10 |
| FR-VND | VND-01, VND-02, VND-03, VND-04, VND-05, VND-06, VND-07, VND-08, VND-19, VND-20, VND-21 | `VendorProfile`, `User`, `AdminNotification`, `KycResult`, `Message`, `Order`, `OrderItem`, `EscrowLedger`, `ProductViewStat`, `VendorWallet`, `WalletTransaction`, `WithdrawalRequest`, `VendorShippingConfig` | 1, 4, 5, 6, 7, 8, 10 |
| FR-LOG | LOG-01, LOG-02, LOG-03, LOG-15, LOG-16 | `Order`, `OrderItem`, `ShipmentTracking`, `ShipmentEvent`, `VendorShippingConfig`, `StockReservation`, `Notification` | 4, 6, 10 |
| FR-PRO | PRO-01, PRO-02, PRO-03, PRO-04, PRO-05, PRO-06, PRO-08, PRO-09, PRO-10 | `Voucher`, `VoucherCategory`, `UserVoucher`, `VoucherUsage`, `OrderDiscount`, `OrderItem`, `FlashSaleCampaign`, `FlashSaleItem`, `Product`, `Order`, `StockReservation`, `DailyCheckin`, `UserCoin`, `CoinTransaction` | 2, 3, 4, 9 |
| FR-SOC | SOC-01, SOC-02, SOC-03, SOC-04, SOC-05, SOC-06, AI-07 | `Conversation`, `Message`, `ProductVariant`, `Order`, `ProductReview`, `Product`, `ReviewHelpfulVote`, `CoinTransaction`, `ReviewReport`, `VendorProfile`, `SystemLog`, `ShopFollow`, `Notification` | 1, 2, 4, 7, 9, 10 |
| FR-SUP | SUP-01, SUP-02, SUP-03, SUP-04 | `RefundRequest`, `RefundRequestItem`, `RefundEvidence`, `ShipmentTracking`, `EscrowLedger`, `PaymentRefund`, `PaymentTransaction`, `VendorWallet` | 5, 6 |
| FR-NOT | NOT-01, NOT-03, NOT-05 | `Notification`, `AdminNotification`, `SystemLog`, `PlatformNotification`, `UserPlatformNotificationRead` | 10 |
| FR-ADM | ADM-01, ADM-02, ADM-03, ADM-04, ADM-05, ADM-06, ADM-07, ADM-12 | `User`, `AdminProfile`, `StaffPermission`, `SystemLog`, `WithdrawalRequest`, `VendorWallet`, `WalletTransaction`, `AdminNotification`, `Order`, `EscrowLedger`, `OrderDiscount`, `VendorProfile`, `FinancialLedger`, `HomeBanner`, `Category` | 1, 2, 4, 5, 10 |

Ma trận chi tiết từng use case: Usecase.md Phần II.

## 8. Kiểm chứng yêu cầu

| Tầng | Công cụ | Phạm vi bắt buộc | Mục tiêu |
|---|---|---|---|
| Unit | JUnit 5 + Mockito | Phương thức nghiệp vụ entity (`Order.transitionTo`, `EscrowLedger.release`, `Voucher.calculateDiscount`, `ProductVariant.reserve`), service tính tiền | Coverage service ≥ 60% |
| Tích hợp | Spring Boot Test + Testcontainers (PostgreSQL, Redis) | Checkout giữ chỗ đồng thời, IPN lặp, giải ngân, hoàn tiền một phần | 100% luồng tiền |
| API | Apidog (hoặc Postman) collection | 87 use case — mỗi UC ít nhất 1 kịch bản thành công + 1 kịch bản lỗi; kịch bản quyền cho 13 mã quyền | Chạy trước mỗi lần merge `develop` |
| E2E | Playwright | Mua hàng VNPay sandbox, mua VietQR, mua COD, mở shop + KYC, đăng sản phẩm qua Gemini, voucher 3 tầng + Điểm, Flash Sale, trả hàng, nhân viên đúng quyền | 9 kịch bản chính |
| Tải | k6 | 50 VU: tìm kiếm, xem sản phẩm, checkout | Đạt NFR-01 |
| Bảo mật | OWASP ZAP baseline | Storefront, API | Không có cảnh báo High |

**Kịch bản kiểm thử bắt buộc cho luồng tiền:** (1) 100 luồng đặt cùng SKU tồn 10 → đúng 10 đơn thành công; (2) IPN gửi 5 lần → 1 lần ghi nhận; (3) Return URL tới trước IPN → không đổi trạng thái; (4) hoàn một phần 300.000đ của đơn 1.000.000đ, hoa hồng 5% → shop nhận 665.000đ, sàn 35.000đ; (5) Cron giải ngân chạy 2 instance song song → không giải ngân trùng (`SKIP LOCKED`); (6) 50 luồng mua Flash Sale suất 10 → đúng 10 thành công; (7) xếp chồng Shop 10% + Sàn 50.000đ + Freeship + Điểm 50% → tổng chiết khấu phân bổ khớp từng dòng, sổ cái cân bằng.

## Phụ lục A — Danh mục 87 use case

| # | Mã UC | Tên use case | Phân hệ | Tác nhân chính | Sprint | Mức ưu tiên |
|:---:|---|---|---|---|:---:|:---:|
| 1 | `UC-AUTH-01` | Đăng ký tài khoản Khách hàng | 01. Xác thực & Tài khoản người dùng | Guest (khách vãng lai) | S1 | MUST |
| 2 | `UC-AUTH-02` | Đăng nhập hệ thống | 01. Xác thực & Tài khoản người dùng | Customer (khách hàng), Vendor (người bán/shop), Staff (Nhân viên sàn), Admin | S1 | MUST |
| 3 | `UC-AUTH-03` | Đăng nhập & Liên kết tài khoản Google / Apple (OAuth2 / OpenID Connect) | 01. Xác thực & Tài khoản người dùng | Guest (khách vãng lai), Customer (khách hàng) | S7 | SHOULD |
| 4 | `UC-AUTH-04` | Khôi phục mật khẩu qua Email OTP (Forgot Password) | 01. Xác thực & Tài khoản người dùng | Customer (khách hàng), Vendor (người bán/shop), Staff (Nhân viên sàn) | S1 | MUST |
| 5 | `UC-AUTH-06` | Đổi / Thiết lập mật khẩu | 01. Xác thực & Tài khoản người dùng | Customer (khách hàng), Vendor (người bán/shop), Staff (Nhân viên sàn), Admin | S1 | MUST |
| 6 | `UC-AUTH-08` | Quản lý thông tin hồ sơ cá nhân (Customer Profile) | 01. Xác thực & Tài khoản người dùng | Customer (khách hàng), Staff (Nhân viên sàn) | S2 | MUST |
| 7 | `UC-AUTH-09` | Quản lý sổ địa chỉ giao hàng chuẩn GHN | 01. Xác thực & Tài khoản người dùng | Customer (khách hàng) | S2 | MUST |
| 8 | `UC-AUTH-10` | Yêu cầu ẩn danh hóa & Xóa dữ liệu cá nhân (Xóa tài khoản) | 01. Xác thực & Tài khoản người dùng | Customer (khách hàng) | S7 | SHOULD |
| 9 | `UC-AUTH-11` | Đăng xuất & Quản lý thiết bị đăng nhập | 01. Xác thực & Tài khoản người dùng | Customer (khách hàng), Vendor (người bán/shop), Staff (Nhân viên sàn), Admin | S1 | MUST |
| 10 | `UC-CAT-01` | Duyệt cây danh mục đa cấp (Category Hierarchy) | 02. Danh mục, Sản phẩm & Tìm kiếm | Guest (khách vãng lai), Customer (khách hàng) | S2 | MUST |
| 11 | `UC-CAT-02` | Tìm kiếm sản phẩm theo từ khóa | 02. Danh mục, Sản phẩm & Tìm kiếm | Guest (khách vãng lai), Customer (khách hàng) | S2 | MUST |
| 12 | `UC-CAT-04` | Gợi ý từ khóa tìm kiếm & Xu hướng (Autocomplete & Trending) | 02. Danh mục, Sản phẩm & Tìm kiếm | Guest (khách vãng lai), Customer (khách hàng) | S7 | SHOULD |
| 13 | `UC-CAT-05` | Lọc nâng cao & Sắp xếp sản phẩm đa tiêu chí | 02. Danh mục, Sản phẩm & Tìm kiếm | Guest (khách vãng lai), Customer (khách hàng) | S2 | MUST |
| 14 | `UC-CAT-06` | Xem chi tiết sản phẩm, ma trận biến thể SKU & Tồn kho | 02. Danh mục, Sản phẩm & Tìm kiếm | Guest (khách vãng lai), Customer (khách hàng) | S2 | MUST |
| 15 | `UC-CAT-07` | Xem gợi ý sản phẩm tương tự | 02. Danh mục, Sản phẩm & Tìm kiếm | Guest (khách vãng lai), Customer (khách hàng) | S7 | SHOULD |
| 16 | `UC-CAT-08` | Quản lý danh sách sản phẩm yêu thích (Wishlist) | 02. Danh mục, Sản phẩm & Tìm kiếm | Customer (khách hàng) | S6 | SHOULD |
| 17 | `UC-CAT-09` | Thêm mới sản phẩm & Biến thể SKU (Người bán) | 02. Danh mục, Sản phẩm & Tìm kiếm | Vendor (người bán/shop) | S2 | MUST |
| 18 | `UC-CAT-10` | Cập nhật thông tin sản phẩm, giá bán & hình ảnh | 02. Danh mục, Sản phẩm & Tìm kiếm | Vendor (người bán/shop) | S2 | MUST |
| 19 | `UC-CAT-11` | Ẩn / Khóa / Xóa mềm sản phẩm | 02. Danh mục, Sản phẩm & Tìm kiếm | Vendor (người bán/shop), Moderator (Nhân viên kiểm duyệt), Admin | S2 | MUST |
| 20 | `UC-CAT-12` | Kiểm duyệt sản phẩm tự động qua AI (Google Gemini API) | 02. Danh mục, Sản phẩm & Tìm kiếm | — (được include) | S7 | SHOULD |
| 21 | `UC-CAT-13` | Kiểm duyệt sản phẩm thủ công theo hàng đợi (Staff Moderator) | 02. Danh mục, Sản phẩm & Tìm kiếm | Moderator (Nhân viên kiểm duyệt) | S2 | MUST |
| 22 | `UC-CAT-14` | Quản lý danh mục sàn toàn cục & Cấu hình tỷ lệ hoa hồng | 02. Danh mục, Sản phẩm & Tìm kiếm | Moderator (Nhân viên kiểm duyệt), Finance Officer (Nhân viên tài chính), Admin | S2 | MUST |
| 23 | `UC-CAT-15` | Cấu hình bộ thuộc tính chuẩn theo ngành hàng (Category Attributes) | 02. Danh mục, Sản phẩm & Tìm kiếm | Admin, Moderator (Nhân viên kiểm duyệt) | S7 | SHOULD |
| 24 | `UC-CAT-16` | Xem trang gian hàng (Shop Page) | 02. Danh mục, Sản phẩm & Tìm kiếm | Guest (khách vãng lai), Customer (khách hàng) | S2 | MUST |
| 25 | `UC-CAT-17` | Người bán đề xuất danh mục mới & Duyệt đề xuất | 02. Danh mục, Sản phẩm & Tìm kiếm | Vendor (người bán/shop), Moderator (Nhân viên kiểm duyệt), Admin | S7 | SHOULD |
| 26 | `UC-CAT-18` | Chia sẻ sản phẩm bằng liên kết (Deeplink) | 02. Danh mục, Sản phẩm & Tìm kiếm | Guest (khách vãng lai), Customer (khách hàng) | S7 | SHOULD |
| 27 | `UC-ORD-01` | Quản lý giỏ hàng mua sắm cá nhân (Shopping Cart) | 03. Giỏ hàng, Đặt hàng & Đơn hàng | Guest (khách vãng lai), Customer (khách hàng) | S3 | MUST |
| 28 | `UC-ORD-04` | Xem trước đơn hàng & Tách giỏ hàng đa nhà bán (Cart Preview & Splitting) | 03. Giỏ hàng, Đặt hàng & Đơn hàng | Customer (khách hàng) | S3 | MUST |
| 29 | `UC-ORD-06` | Tính phí vận chuyển qua GHN API | 03. Giỏ hàng, Đặt hàng & Đơn hàng | Customer (khách hàng) | S3 | MUST |
| 30 | `UC-ORD-07` | Tạo phiên Checkout & Các Đơn hàng con theo Shop (PersistCheckout) | 03. Giỏ hàng, Đặt hàng & Đơn hàng | Customer (khách hàng) | S3 | MUST |
| 31 | `UC-ORD-08` | Tự động hủy đơn quá hạn thanh toán & Hoàn kho (Sweeper Cron) | 03. Giỏ hàng, Đặt hàng & Đơn hàng | Platform Cron / Scheduler | S3 | MUST |
| 32 | `UC-ORD-09` | Khách hàng theo dõi chi tiết hành trình đơn hàng | 03. Giỏ hàng, Đặt hàng & Đơn hàng | Customer (khách hàng) | S4 | MUST |
| 33 | `UC-ORD-10` | Khách hàng yêu cầu hủy đơn hàng | 03. Giỏ hàng, Đặt hàng & Đơn hàng | Customer (khách hàng) | S4 | MUST |
| 34 | `UC-ORD-11` | Khách hàng xác nhận "Đã nhận hàng thành công" | 03. Giỏ hàng, Đặt hàng & Đơn hàng | Customer (khách hàng) | S4 | MUST |
| 35 | `UC-ORD-12` | Hệ thống tự động hoàn tất đơn sau 7 ngày từ khi Delivered | 03. Giỏ hàng, Đặt hàng & Đơn hàng | Platform Cron / Scheduler | S4 | MUST |
| 36 | `UC-PAY-01` | Khởi tạo thanh toán trực tuyến (VNPay / chuyển khoản VietQR qua SePay) | 04. Thanh toán, Ký quỹ (Escrow) & Sổ cái | Customer (khách hàng) | S3 | MUST |
| 37 | `UC-PAY-03` | Xử lý IPN VNPay & webhook SePay idempotent | 04. Thanh toán, Ký quỹ (Escrow) & Sổ cái | Payment Gateway (VNPay, SePay) | S3 | MUST |
| 38 | `UC-PAY-05` | Ghi nhận ký quỹ (Escrow) & Khấu trừ hoa hồng | 04. Thanh toán, Ký quỹ (Escrow) & Sổ cái | — (được include) | S3 | MUST |
| 39 | `UC-PAY-06` | Ghi sổ cái tài chính kép bất biến (Financial Ledger) | 04. Thanh toán, Ký quỹ (Escrow) & Sổ cái | — (được include) | S3 | MUST |
| 40 | `UC-PAY-07` | Tự động giải phóng tiền ký quỹ về ví người bán | 04. Thanh toán, Ký quỹ (Escrow) & Sổ cái | Platform Cron / Scheduler | S4 | MUST |
| 41 | `UC-VND-01` | Đăng ký mở gian hàng (Become a Seller) | 05. Gian hàng & Thẩm định KYC người bán | Customer (khách hàng) | S2 | MUST |
| 42 | `UC-VND-02` | Tải hồ sơ pháp lý & Giấy tờ định danh KYC | 05. Gian hàng & Thẩm định KYC người bán | Vendor (người bán/shop) | S2 | MUST |
| 43 | `UC-VND-03` | Thẩm định KYC tự động qua AI (QR, MRZ & Face Match) | 05. Gian hàng & Thẩm định KYC người bán | AI Worker Service (KYC & PhoBERT) | S2 | MUST |
| 44 | `UC-VND-04` | Kiểm duyệt thủ công hồ sơ KYC (Staff / Admin) | 05. Gian hàng & Thẩm định KYC người bán | Moderator (Nhân viên kiểm duyệt), Admin | S2 | MUST |
| 45 | `UC-VND-05` | Cấu hình kho lấy hàng & Đồng bộ GHN Shop ID | 05. Gian hàng & Thẩm định KYC người bán | Vendor (người bán/shop) | S2 | MUST |
| 46 | `UC-VND-06` | Cấu hình tin nhắn trả lời tự động (Auto-reply) | 05. Gian hàng & Thẩm định KYC người bán | Vendor (người bán/shop) | S7 | SHOULD |
| 47 | `UC-VND-07` | Đăng ký tài khoản nhận giải ngân (Payout Bank Account) | 05. Gian hàng & Thẩm định KYC người bán | Vendor (người bán/shop) | S4 | MUST |
| 48 | `UC-VND-08` | Xem báo cáo doanh thu & Phân tích bán hàng (Vendor Analytics) | 05. Gian hàng & Thẩm định KYC người bán | Vendor (người bán/shop) | S6 | SHOULD |
| 49 | `UC-VND-19` | Bật chế độ tạm nghỉ bán hàng của shop (Vacation Mode) | 05. Gian hàng & Thẩm định KYC người bán | Vendor (người bán/shop) | S7 | SHOULD |
| 50 | `UC-VND-20` | Xem ví & sổ giao dịch người bán (Seller Wallet & Escrow Ledger) | 05. Gian hàng & Thẩm định KYC người bán | Vendor (người bán/shop) | S7 | SHOULD |
| 51 | `UC-VND-21` | Cập nhật thông tin gian hàng & cài đặt vận hành | 05. Gian hàng & Thẩm định KYC người bán | Vendor (người bán/shop) | S7 | SHOULD |
| 52 | `UC-LOG-01` | Tiếp nhận đơn hàng & Đóng gói tại kho shop (Fulfillment) | 06. Vận chuyển qua đối tác 3PL | Vendor (người bán/shop) | S4 | MUST |
| 53 | `UC-LOG-02` | Đẩy vận đơn sang GHN & In phiếu gửi hàng PDF (Waybill Label) | 06. Vận chuyển qua đối tác 3PL | Vendor (người bán/shop) | S4 | MUST |
| 54 | `UC-LOG-03` | Đồng bộ hành trình giao nhận qua Webhook GHN | 06. Vận chuyển qua đối tác 3PL | Đối tác vận chuyển (GHN, GHTK, ViettelPost, J&T) | S4 | MUST |
| 55 | `UC-LOG-15` | Điều phối đa hãng vận chuyển (Dynamic Carrier Routing) | 06. Vận chuyển qua đối tác 3PL | — (được include) | S7 | SHOULD |
| 56 | `UC-LOG-16` | Người bán xác nhận / từ chối đơn hàng mới | 06. Vận chuyển qua đối tác 3PL | Vendor (người bán/shop) | S4 | MUST |
| 57 | `UC-PRO-01` | Tạo & Quản lý Voucher Gian hàng (Vendor Shop Vouchers) | 07. Khuyến mãi, Voucher, Flash Sale & Điểm | Vendor (người bán/shop) | S5 | MUST |
| 58 | `UC-PRO-02` | Tạo & Quản lý Voucher Toàn sàn | 07. Khuyến mãi, Voucher, Flash Sale & Điểm | Finance Officer (Nhân viên tài chính), Admin | S5 | MUST |
| 59 | `UC-PRO-03` | Khách hàng thu thập Voucher vào Kho Voucher cá nhân | 07. Khuyến mãi, Voucher, Flash Sale & Điểm | Customer (khách hàng) | S5 | MUST |
| 60 | `UC-PRO-04` | Thuật toán Stacking Voucher 3 Tầng & Điểm tự động khi Checkout | 07. Khuyến mãi, Voucher, Flash Sale & Điểm | Customer (khách hàng) | S8 | SHOULD |
| 61 | `UC-PRO-05` | Tạo chiến dịch Flash Sale & Cấu hình khung giờ vàng | 07. Khuyến mãi, Voucher, Flash Sale & Điểm | Finance Officer (Nhân viên tài chính), Admin, Vendor (người bán/shop) | S8 | SHOULD |
| 62 | `UC-PRO-06` | Mua hàng Flash Sale chống bán vượt suất | 07. Khuyến mãi, Voucher, Flash Sale & Điểm | Customer (khách hàng) | S8 | SHOULD |
| 63 | `UC-PRO-08` | Điểm danh nhận Điểm hàng ngày & Duy trì chuỗi điểm danh (Streak) | 07. Khuyến mãi, Voucher, Flash Sale & Điểm | Customer (khách hàng) | S8 | SHOULD |
| 64 | `UC-PRO-09` | Tra cứu số dư Điểm Genie & Lịch sử biến động Điểm | 07. Khuyến mãi, Voucher, Flash Sale & Điểm | Customer (khách hàng) | S8 | SHOULD |
| 65 | `UC-PRO-10` | Sử dụng Điểm Genie để giảm trừ thanh toán đơn hàng | 07. Khuyến mãi, Voucher, Flash Sale & Điểm | Customer (khách hàng) | S8 | SHOULD |
| 66 | `UC-SOC-01` | Nhắn tin trò chuyện thời gian thực Khách - Shop (Live Chat) | 08. Chat, Đánh giá & Theo dõi shop | Customer (khách hàng), Vendor (người bán/shop) | S6 | SHOULD |
| 67 | `UC-SOC-02` | Gửi thẻ sản phẩm, hình ảnh và trạng thái đơn hàng trong chat | 08. Chat, Đánh giá & Theo dõi shop | Customer (khách hàng), Vendor (người bán/shop) | S8 | SHOULD |
| 68 | `UC-SOC-03` | Đánh giá sản phẩm đã mua & Tải ảnh thực tế (Verified Buyer Review) | 08. Chat, Đánh giá & Theo dõi shop | Customer (khách hàng) | S5 | MUST |
| 69 | `UC-SOC-04` | Người bán phản hồi đánh giá của khách hàng (Vendor Review Reply) | 08. Chat, Đánh giá & Theo dõi shop | Vendor (người bán/shop) | S5 | MUST |
| 70 | `UC-SOC-05` | Báo cáo đánh giá vi phạm / spam & Kiểm duyệt đánh giá (Review Moderation & Report) | 08. Chat, Đánh giá & Theo dõi shop | Vendor (người bán/shop), Customer (khách hàng), Moderator (Nhân viên kiểm duyệt), CS Specialist (Nhân viên CSKH) | S8 | SHOULD |
| 71 | `UC-SOC-06` | Khách hàng theo dõi gian hàng (Shop Follow) | 08. Chat, Đánh giá & Theo dõi shop | Customer (khách hàng) | S8 | SHOULD |
| 72 | `UC-AI-07` | Phân tích cảm xúc đánh giá bằng PhoBERT | 08. Chat, Đánh giá & Theo dõi shop | AI Worker Service (KYC & PhoBERT) | S6 | SHOULD |
| 73 | `UC-SUP-01` | Tạo yêu cầu Trả hàng / Hoàn tiền (Return & Refund Request) | 09. Trả hàng, Hoàn tiền & Khiếu nại | Customer (khách hàng) | S5 | MUST |
| 74 | `UC-SUP-02` | Người bán tiếp nhận và xử lý yêu cầu trả hàng | 09. Trả hàng, Hoàn tiền & Khiếu nại | Vendor (người bán/shop) | S5 | MUST |
| 75 | `UC-SUP-03` | Nhân viên sàn phân xử tranh chấp khiếu nại (Dispute Arbitration) | 09. Trả hàng, Hoàn tiền & Khiếu nại | CS Specialist (Nhân viên CSKH), Admin | S5 | MUST |
| 76 | `UC-SUP-04` | Thực hiện hoàn tiền (VNPay / VietQR / COD) | 09. Trả hàng, Hoàn tiền & Khiếu nại | CS Specialist (Nhân viên CSKH) | S5 | MUST |
| 77 | `UC-NOT-01` | Thông báo trong ứng dụng thời gian thực (WebSocket) | 10. Thông báo | Customer (khách hàng), Vendor (người bán/shop), Staff (Nhân viên sàn) | S5 | MUST |
| 78 | `UC-NOT-03` | Gửi email giao dịch tự động | 10. Thông báo | Customer (khách hàng), Vendor (người bán/shop) | S5 | MUST |
| 79 | `UC-NOT-05` | Admin phát thông báo toàn sàn (Broadcast Platform Notification) | 10. Thông báo | Admin | S8 | SHOULD |
| 80 | `UC-ADM-01` | Quản lý danh sách nhân viên nội bộ & Lời mời kích hoạt | 11. Quản trị sàn, Nhân viên & Rút tiền | Admin | S1 | MUST |
| 81 | `UC-ADM-02` | Phân cấp vai trò & Cấp phát / Thu hồi mã quyền chi tiết (RBAC) | 11. Quản trị sàn, Nhân viên & Rút tiền | Admin | S1 | MUST |
| 82 | `UC-ADM-03` | Khóa / Mở khóa tài khoản nhân viên và người dùng vi phạm | 11. Quản trị sàn, Nhân viên & Rút tiền | Admin | S6 | MUST |
| 83 | `UC-ADM-04` | Người bán gửi yêu cầu rút tiền từ Ví về Ngân hàng | 11. Quản trị sàn, Nhân viên & Rút tiền | Vendor (người bán/shop) | S4 | MUST |
| 84 | `UC-ADM-05` | Nhân viên sàn phê duyệt & Thực hiện lệnh chuyển khoản (Payout) | 11. Quản trị sàn, Nhân viên & Rút tiền | Finance Officer (Nhân viên tài chính), Admin | S4 | MUST |
| 85 | `UC-ADM-06` | Dashboard doanh thu & ký quỹ toàn sàn | 11. Quản trị sàn, Nhân viên & Rút tiền | Admin, Staff (Nhân viên sàn) | S6 | SHOULD |
| 86 | `UC-ADM-07` | Tra cứu nhật ký kiểm toán hệ thống bất biến (System Audit Logs) | 11. Quản trị sàn, Nhân viên & Rút tiền | Admin | S7 | SHOULD |
| 87 | `UC-ADM-12` | Quản lý banner trang chủ & chiến dịch tĩnh (CMS Banner) | 11. Quản trị sàn, Nhân viên & Rút tiền | Admin, Finance Officer (Nhân viên tài chính) | S8 | SHOULD |
