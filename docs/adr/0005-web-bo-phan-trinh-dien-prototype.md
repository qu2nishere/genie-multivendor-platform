# 0005 — Không chuyển phần chỉ để trình diễn của prototype

- **Trạng thái:** Chấp nhận (chủ dự án chốt 2026-11-11, plan D5)
- **Phạm vi:** `web/`

## Bối cảnh

Prototype có nhiều thứ chỉ phục vụ buổi trình diễn: bảng nổi hình đèn thần ở mép màn hình để nhảy
giữa các bề mặt / màn (`sh-dock` trong `shell.jsx`), `board.jsx`, `design-canvas.jsx`,
`tweaks-panel.jsx` (đổi màu nhấn / bo góc trực tiếp), bảng 7 hành trình PRD, `DemoBox`, `AcctDemo` /
`SCENARIOS`, bảng mô phỏng `GoogleFlow`, nút xem trước vai trò `PREVIEW_ROLES`, các nút "Mô phỏng: …".

## Quyết định

- Không làm các phần trên; không chuyển thành tham số `?mock=`.
- Nút "Mô phỏng: …" gắn với nghiệp vụ thật thì đổi thành hành động thật (ví dụ "Đã chuyển khoản" khi
  duyệt rút tiền, "Đã làm xong" ở bước camera KYC); trạng thái do bên khác đổi (shop / sàn / cron) thì
  bỏ hẳn khỏi màn của người đang xem.
- Mock chỉ có một bộ dữ liệu mặc định (ADR 0003).

## Hệ quả

- Màn nào cần trạng thái khác (shop chưa KYC, đơn trả hàng…) phải đi đúng luồng nghiệp vụ để tới
  (ví dụ mở shop ở `/become-seller` → shop `NOT_SUBMITTED`).
- Ngoại lệ có kiểm soát cho kiểm thử: khóa `sessionStorage['genie.mock.staff_title']` đăng nhập
  `/admin` bằng nhân viên theo chức danh để e2e kiểm phân quyền (G13.16). Không có nút trên giao diện.
