# BÁO CÁO PHÂN NHÓM USE CASE THEO MỨC ĐỘ ĐỘC LẬP — GENIE (BẢN TLCN)

> **Ngày lập:** 2026-11-16 · **Phạm vi:** 87 use case của [Usecase.md](diagrams/UseCaseDiagram/Usecase.md) (TLCN v1.4)
> **Mục đích:** chỉ ra UC nào làm riêng được (một plan riêng, chạy song song) mà không chờ hay làm ảnh hưởng UC khác, UC nào phải làm theo cụm hoặc theo chuỗi — làm đầu vào để lập plan trong `plan/<ngày>/<thể loại>/`.
> **Nguồn đã quét:** quan hệ «include» / «extend», *Tiền điều kiện* và *Thực thể CSDL* của từng UC trong Usecase.md; bảng ánh xạ UC ↔ API ở [API_SPEC.md](document/API_SPEC.md); hiện trạng code `genie-multivendor-platform` (AUTH đã hiện thực; 65 bảng + `V1…V14` có sẵn; `common` đã có Silo, email, nhật ký kiểm toán, phân quyền). Tiến độ từng UC: [CHECKLIST_TIEN_DO_UC.md](CHECKLIST_TIEN_DO_UC.md).

---

## 1. Tiêu chí "độc lập"

Một UC được xếp **độc lập** khi thỏa cả 3 điều kiện:

1. **Không «include» / «extend» UC nào chưa làm** (theo mục *Quan hệ Use Case* của Usecase.md).
2. **Tiền điều kiện đáp ứng được bằng dữ liệu seed** (`R__seed_demo.sql`) — không phải đợi UC khác chạy trước để sinh dữ liệu.
3. **Chỉ ghi vào bảng của chính nó**; làm xong không buộc UC khác phải sửa. UC khác có thể *đọc* kết quả của nó sau này.

| Nhóm | Ý nghĩa | Cách lập plan |
|---|---|---|
| 🟢 **A** | Độc lập hoàn toàn | Mỗi UC một plan, làm song song được |
| 🟡 **B** | Độc lập theo cụm — các UC trong cụm dính nhau, nhưng cụm không đụng cụm khác | Mỗi cụm một plan |
| 🔴 **C** | Chuỗi đơn hàng → tiền, phụ thuộc lẫn nhau | Làm tuần tự theo chuỗi |

## 2. Tổng kết

| Nhóm | Số UC |
|---|---:|
| ✅ Đã / gần xong (AUTH-01, 02, 04, 06, 10, 11) | 6 |
| 🟢 A — Độc lập hoàn toàn | 10 |
| 🟡 B — Độc lập theo cụm (6 cụm) | 27 |
| 🔴 C — Chuỗi đơn hàng → tiền | 44 |
| **Tổng** | **87** |

## 3. ✅ UC đã / gần xong (không cần plan mới)

| UC | Trạng thái | Còn lại |
|---|---|---|
| UC-AUTH-01 Đăng ký tài khoản Khách hàng | ✅ Đã triển khai | — |
| UC-AUTH-02 Đăng nhập hệ thống | ✅ Đã triển khai | — |
| UC-AUTH-04 Khôi phục mật khẩu qua Email OTP | ✅ Đã triển khai | — |
| UC-AUTH-06 Đổi / Thiết lập mật khẩu | 🟡 Code + test xong | Chạy thử thật trên giao diện |
| UC-AUTH-11 Đăng xuất & Quản lý thiết bị đăng nhập | 🟡 Code + test xong | Chạy thử thật |
| UC-AUTH-10 Xóa tài khoản | 🟡 Code + test xong | Chạy thử thật; khi làm nhóm C, module đơn hàng / ví cài cổng `AccountDeletionBlocker` |

## 4. 🟢 Nhóm A — Độc lập hoàn toàn (mỗi UC một plan)

| UC | Vì sao độc lập | Cần chuẩn bị |
|---|---|---|
| **UC-AUTH-09** Quản lý sổ địa chỉ giao hàng chuẩn GHN | Chỉ ghi `addresses`, `customer_profiles.default_address_id`; không quan hệ UC nào. UC-ORD-06 sau này chỉ *đọc* địa chỉ | `GHN_TOKEN` hoặc ảnh chụp JSON danh mục hành chính (plan AUTH A12) |
| **UC-AUTH-08** (phần còn lại) `GET /me` + đổi email | Bộ đếm / shop lấy qua cổng đảo ngược, mặc định `0` / `null` khi module khác chưa có | Không |
| **UC-AUTH-03** (phần Apple) | Chỉ thêm một nhà cung cấp, đã có cờ `genie.oauth.apple.enabled` | Tài khoản Apple Developer (trả phí) |
| **UC-ADM-07** Tra cứu nhật ký kiểm toán | Chỉ *đọc* `system_logs` — bảng AUTH đang ghi thật | Không |
| **UC-ADM-12** Quản lý banner trang chủ | Bảng `home_banners` riêng; storefront chỉ đọc `GET /public/banners` | Thêm purpose `BANNER_IMAGE` ở `UploadPurpose` |
| **UC-NOT-05** Admin phát thông báo toàn sàn | Bảng `platform_notifications`, `user_platform_notification_reads` riêng | Đẩy realtime cần UC-NOT-01 (cụm B6); không có thì vẫn xem được qua danh sách thông báo |
| **UC-PRO-08** Điểm danh nhận Điểm + **UC-PRO-09** Tra cứu Điểm | Chỉ cần user; ghi `daily_checkins`, `user_coins`, `coin_transactions` | Không — nguồn Điểm từ đơn / đánh giá cộng thêm sau |
| **UC-CAT-08** Sản phẩm yêu thích | Bảng `user_wishlists` riêng | Seed vài sản phẩm |
| **UC-SOC-06** Theo dõi gian hàng | Bảng `shop_follows` riêng | Seed shop đã KYC; thông báo "shop có hàng mới" để sau |

> PRO-08 và PRO-09 dùng chung bảng `user_coins` nên đếm là 2 UC nhưng nên gộp một plan.

## 5. 🟡 Nhóm B — Độc lập theo cụm (mỗi cụm một plan)

| Cụm | UC | Ghi chú |
|---|---|---|
| **B1. Nhân viên & phân quyền** | UC-ADM-01, UC-ADM-02, UC-ADM-03 | ADM-01 «include» ADM-02 nên phải làm chung. ADM-03 dùng `UserFacade.suspend` đã có. Nối AUTH qua `POST /auth/staff-activations`; đổi quyền gọi `StaffPermissionAuthorityProvider.evict` |
| **B2. Danh mục sàn** | UC-CAT-14, UC-CAT-15, UC-CAT-01 | CAT-15 cần ngành từ CAT-14; CAT-01 chỉ đọc cây. Là **gốc** của mọi cụm sản phẩm — nên làm sớm |
| **B3. Storefront đọc công khai** | UC-CAT-02, UC-CAT-04, UC-CAT-05, UC-CAT-06, UC-CAT-07, UC-CAT-16, UC-CAT-18 | Chỉ đọc `products` + ghi bộ đếm riêng (lượt xem, từ khóa). Chạy trên **seed sản phẩm**, không chờ người bán đăng bán. CAT-06 «include» CAT-07; CAT-04 «extend» CAT-02; CAT-16, CAT-18 «extend» CAT-06. Cần B2 có cây danh mục (hoặc seed) |
| **B4. Mở shop & KYC** | UC-VND-01, UC-VND-02, UC-VND-03, UC-VND-04, UC-VND-05, UC-VND-07, UC-VND-21 | Chuỗi VND-02 «include» VND-03, VND-04 «extend» VND-03. VND-03 cần AI Worker (`server-python` mới có khung) — có thể làm VND-04 duyệt tay trước, AI sau. VND-05 (kho lấy hàng GHN), VND-07 (tài khoản nhận tiền), VND-21 (cài đặt shop) chỉ cần shop `VERIFIED` |
| **B5. Voucher (quản lý, chưa áp vào giá)** | UC-PRO-02, UC-PRO-03, UC-PRO-01 | Chỉ tạo / sửa / lưu voucher. PRO-01 cần shop `VERIFIED` (seed được). **Áp** voucher khi mua (PRO-04) thuộc nhóm C |
| **B6. Chat & realtime** | UC-SOC-01, UC-SOC-02, UC-VND-06, UC-NOT-01 | Cùng hạ tầng WebSocket / STOMP. SOC-02 «extend» SOC-01; VND-06 tự trả lời cần chat. NOT-01 là nền mà nhiều UC khác đẩy thông báo vào → làm cụm này *sớm* thì các nhóm khác đỡ chờ |

## 6. 🔴 Nhóm C — Chuỗi đơn hàng → tiền (làm tuần tự)

| Chuỗi | UC | Phụ thuộc |
|---|---|---|
| **C1. Đăng bán** | UC-CAT-09, UC-CAT-10, UC-CAT-11, UC-CAT-12, UC-CAT-13, UC-CAT-17 | Cần B2 + B4 (shop `VERIFIED`); CAT-09 / 10 «include» CAT-12 (Gemini), CAT-13 «extend» CAT-12, CAT-17 «extend» CAT-09 |
| **C2. Giỏ → Checkout** | UC-ORD-01, UC-ORD-04, UC-ORD-06, UC-ORD-07, UC-ORD-08 | ORD-06 cần AUTH-09 + VND-05; ORD-07 «include» ORD-04 «include» ORD-06 |
| **C3. Thanh toán & ký quỹ** | UC-PAY-01, UC-PAY-03, UC-PAY-05, UC-PAY-06, UC-PAY-07 | Cần phiên checkout (ORD-07); PAY-03 «include» PAY-05, PAY-06; VNPay sandbox + webhook SePay |
| **C4. Vận chuyển** | UC-LOG-16, UC-LOG-01, UC-LOG-02, UC-LOG-15, UC-LOG-03 | Cần đơn đã thanh toán; LOG-02 «include» LOG-15; webhook GHN |
| **C5. Sau giao hàng** | UC-ORD-09, UC-ORD-10, UC-ORD-11, UC-ORD-12 | Cần đơn + vận đơn + ký quỹ; ORD-12 «include» PAY-07 |
| **C6. Khuyến mãi khi mua** | UC-PRO-04, UC-PRO-10, UC-PRO-05, UC-PRO-06 | PRO-04 «extend» ORD-04, PRO-10 «extend» PRO-04, PRO-06 «extend» ORD-07; cần B5 có voucher |
| **C7. Đánh giá** | UC-SOC-03, UC-SOC-04, UC-SOC-05, UC-AI-07 | SOC-03 yêu cầu đơn `COMPLETED`; AI-07 cần PhoBERT trong AI Worker |
| **C8. Trả hàng & hoàn tiền** | UC-SUP-01, UC-SUP-02, UC-SUP-03, UC-SUP-04 | SUP-01 «extend» ORD-11; dùng ký quỹ + sổ cái |
| **C9. Ví & rút tiền** | UC-VND-20, UC-ADM-04, UC-ADM-05 | Cần ví có tiền từ PAY-07; ADM-04 dùng tài khoản nhận tiền của VND-07 (cụm B4) |
| **C10. Báo cáo** | UC-VND-08, UC-ADM-06, UC-VND-19 | Đọc đơn / ký quỹ — viết code sớm được nhưng không có số liệu để kiểm; VND-19 chặn khi còn đơn đang xử lý |
| **C11. Email giao dịch** | UC-NOT-03 | Hạ tầng `EmailSender` đã có; nội dung email gắn với từng UC phát sự kiện |

## 7. ⚠️ Điểm chung dễ xung đột khi chạy nhiều plan song song

UC độc lập về nghiệp vụ nhưng các nhánh vẫn sửa chung vài file — dễ **conflict khi merge**:

| File / chỗ | Vì sao đụng nhau | Cách tránh |
|---|---|---|
| `common/error/ErrorCode.java` + `web/src/shared/api/error-codes.ts` + khóa `errors.GNE-*` (`vi.json`, `en.json`) | Mỗi UC thêm mã lỗi | Ghi trước trong plan các mã sẽ thêm; merge `develop` thường xuyên |
| `common/storage/UploadPurpose.java` | Mỗi UC có ảnh thêm một purpose | Ghi rõ purpose trong plan |
| `common/audit/AuditAction.java` + `CHECK` của `system_logs` | Giá trị mới cần migration sửa `CHECK` | Chốt **số migration** (`V15`, `V16`…) theo thứ tự merge trước khi làm — hai nhánh cùng `V15` sẽ hỏng |
| `R__seed_demo.sql` | Nhiều cụm cần seed shop / sản phẩm | Một người giữ file seed, các plan gửi phần cần thêm |
| `docs/web-backend-handoff.md` | Nối web theo từng nhóm | Mỗi plan chỉ sửa dòng của nhóm mình |

## 8. Thứ tự đề xuất

1. **Song song ngay:** nhóm A (bắt đầu UC-AUTH-09, UC-ADM-07) + cụm **B1** + cụm **B2**.
2. **Tiếp theo:** **B3** (storefront trên seed), **B4** (mở shop — VND-04 duyệt tay trước, AI sau), **B6** (WebSocket làm nền), **B5**.
3. **Sau cùng:** nhóm C theo chuỗi C1 Đăng bán → C2 Giỏ → C3 Thanh toán → C4 Vận chuyển → C5 Sau giao → C6 / C7 / C8 / C9 → C10, C11.
