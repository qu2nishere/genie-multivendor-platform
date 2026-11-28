# Kịch bản kiểm thử — Voucher gian hàng (UC-PRO-01) · Voucher toàn sàn (UC-PRO-02) · Kho Voucher (UC-PRO-03)

> Plan / progress: [../../plan/2026-11-26/server-spring/plan_voucher_b5.md](../../plan/2026-11-26/server-spring/plan_voucher_b5.md) · [progress](../../plan/2026-11-26/server-spring/progress_voucher_b5.md). Phụ trách: **Nguyễn Ngọc Thiện**.
> Chuẩn bị môi trường: [../huong-dan-chay-local.md](../huong-dan-chay-local.md) — Docker (postgres, redis, silo, mailpit), backend JDK 21 profile `local` + dữ liệu mẫu, web bản build cổng 4321.
> Mật khẩu tài khoản seed: `Genie@123`. API thử tay: [../api-tests/vouchers.http](../api-tests/vouchers.http).

## Cấu hình bắt buộc

| Nơi | Giá trị |
|---|---|
| `web/.env` | `PUBLIC_USE_MOCK=true` và `PUBLIC_REAL_API` có thêm `seller-vouchers,admin-vouchers,vouchers` (cùng các nhóm đã có). Flash Sale (Seller / Admin) và tra mã trong Voucher Picker vẫn mock tới chuỗi C6 |
| Migration | `V26__b5_b6_notification_chat_voucher.sql` (2 index `vouchers`, 4 `AuditAction.VOUCHER_*`) |
| Seed | `R__seed_demo_50_vouchers.sql` có thêm `GENIESOON` (sàn, sắp diễn ra, đồng tài trợ), `TECHAUDIO10` (TechZone theo ngành *Thiết bị âm thanh*, loại trừ *Phụ kiện điện tử*), `TECHSHIP25` (TechZone tài trợ phí ship) |

**Tài khoản:** chủ TechZone (`shop_id = 1`) = `mai.tran@gmail.com`; Finance Officer (`MANAGE_VOUCHER`) = `linh.tm@genie.example`; nhân viên không có quyền voucher = `phat.nt@genie.example`; khách = tài khoản **mới** đăng ký (OTP qua Mailpit `http://localhost:8025`).

## Ca kiểm thử

| Mã | Thao tác | Kết quả mong đợi |
|---|---|---|
| TC-VCH-00 | Đăng ký khách mới; kiểm seed | Có 3 voucher demo mới |
| TC-VCH-01 | Seller › Khuyến mãi › Tạo voucher: Tự tạo mã, Giảm 15 %, tối đa 50.000, đơn từ 200.000, 100 lượt, từ hôm nay | Toast "Đã tạo voucher …"; tiêu đề **tự sinh** "Giảm 15% tối đa 50.000₫ đơn từ 200.000₫"; dòng "Đang chạy" |
| TC-VCH-02 | Tạo Freeship, phạm vi "Danh mục cụ thể" (chọn 1 ngành), bắt đầu ngày mai | Lưu `FREESHIP` + `CATEGORY` + 1 dòng `voucher_categories`; "Đã lên lịch" |
| TC-VCH-03 | "Chỉnh sửa" voucher đang chạy | Ghi chú khóa trường; ô Giá trị / Đơn tối thiểu bị khóa; tăng số lượng 100 → 250 lưu được |
| TC-VCH-04 | "Nhân bản" | Form mở với dữ liệu cũ, mã trống; tạo voucher mới giữ điều kiện |
| TC-VCH-05 | "Tắt" → xác nhận | "Đã tắt"; voucher biến khỏi `GET /public/vouchers?shop_id=1` ngay (cache đã xóa) |
| TC-VCH-06 | Mở `/shop/1` khi **tắt JavaScript** | Vé voucher của shop có trong HTML SSR; `shop.id` trả về = 1 (`vendor_profiles.id`) |
| TC-VCH-07 | Admin › Voucher sàn › Tạo voucher sàn: tên, 20 %, Đồng tài trợ 60 %, bắt đầu ngày mai | `PLATFORM` / `CO_SPONSOR` / 60; nhật ký `VOUCHER_CREATED` |
| TC-VCH-08 | Tab "Sắp diễn ra"; dòng `COIN500` | Voucher vừa tạo ở tab; `COIN500` có nhãn **Quà đổi Điểm**, không có nút Xoá |
| TC-VCH-09 | "Huỷ" voucher vừa tạo rồi "Xoá" → "Xoá vĩnh viễn" | Mất khỏi CSDL; nhật ký `CREATED → DISABLED → DELETED` |
| TC-VCH-10 | Khách mở trang chủ, bấm "Lưu" ở dải "Mã giảm giá hot" | Vé lấy từ API `featured` (không có voucher shop / `COIN*`); toast "Đã lưu…", nút chuyển trạng thái đã lưu |
| TC-VCH-11 | Kho Voucher: nhập `geniesoon` → Lưu; nhập `COIN500` → Lưu | GENIESOON lưu được (sắp diễn ra) với nhãn "Chưa tới giờ"; COIN500 → "Mã voucher không tồn tại hoặc đã hết hạn." |
| TC-VCH-12 | Kho: nhập `TECHAUDIO10` | Thẻ "Voucher Shop" ghi "Chỉ áp dụng danh mục Thiết bị âm thanh" |
| TC-VCH-13 | Xoá một voucher khỏi Kho (xác nhận) | "Đã xóa voucher khỏi kho"; còn 1 dòng ít hơn |
| TC-VCH-14 | Khách vãng lai bấm "Lưu" | Hộp nhắc đăng nhập |
| TC-VCH-15 | Nhân viên mở trang chủ | Không có nút "Lưu"; API Kho → `403 GNE-ORD-4031` (IT) |
| TC-VCH-16 | Khổ 360 px, Kho Voucher với 9 theme | Không tràn ngang |
| TC-VCH-17 | Gọi API theo `vouchers.http` (4092, 4094, 4093, 4091, 4041, 403, 422) | Đúng mã lỗi — đã phủ bằng IT (`SellerVoucherIT`, `AdminVoucherIT`, `VoucherWalletIT`, `VoucherSecurityIT`) |

## Kết quả chạy thật (2026-11-28)

Chromium điều khiển bằng Playwright, 3 ngữ cảnh song song (chủ shop, Finance Officer, khách) + khách vãng lai / nhân viên / điện thoại 360 px; hệ thống thật: web bản build `PUBLIC_REAL_API=…,seller-vouchers,admin-vouchers,vouchers` + backend `local` (JDK 21) + Postgres + Redis + Silo + Mailpit.

| Mã | Kết quả | Ghi chú |
|---|:---:|---|
| TC-VCH-00 | Đạt | Đăng ký thật qua OTP Mailpit |
| TC-VCH-01 | Đạt | |
| TC-VCH-02 | Đạt | `FREESHIP / CATEGORY / 1` |
| TC-VCH-03 | Đạt | Sau bản sửa `a0ae85f` |
| TC-VCH-04 | Đạt | Bản nhân giữ 15 % / 250 lượt |
| TC-VCH-05 | Đạt | |
| TC-VCH-06 | Đạt | SSR, tắt JavaScript |
| TC-VCH-07 | Đạt | |
| TC-VCH-08 | Đạt | |
| TC-VCH-09 | Đạt | |
| TC-VCH-10 | Đạt | 4 vé `featured` |
| TC-VCH-11 | Đạt | |
| TC-VCH-12 | Đạt | |
| TC-VCH-13 | Đạt | |
| TC-VCH-14 | Đạt | Popup banner khuyến mãi (modal) phải đóng trước — đúng hành vi |
| TC-VCH-15 | Đạt | |
| TC-VCH-16 | Đạt | 9 / 9 theme |
| TC-VCH-17 | Đạt | IT |

**17 / 17 ca chạy trên trình duyệt đạt** (+ TC-VCH-17 bằng IT). Lỗi phát hiện trong lúc chạy và đã sửa: form voucher chỉ giữ **ngày**, nên sửa voucher đang chạy gửi lại giờ bắt đầu / kết thúc khác bản gốc → server coi là đổi thời gian và khóa (`409 GNE-VCH-4094`) — form nay gửi lại đúng mốc ISO gốc khi ngày không đổi (`a0ae85f`, áp cho cả Seller và Admin). Ngoài ra trang chủ trước đó ghi cứng 4 mã mẫu không có trong dữ liệu thật → nay đọc `featured` từ API (`c37f7e0`).
