# 0003 — Dựng giao diện trên lớp mock trước khi có backend

- **Trạng thái:** Chấp nhận (chủ dự án chốt 2026-11-11, plan D3)
- **Phạm vi:** `web/`

## Bối cảnh

Backend `server-spring` chưa chạy được khi chuyển giao diện. Chờ API thì không dựng được màn nào;
viết giao diện gọi thẳng dữ liệu tĩnh thì phải làm lại khi có API.

## Quyết định

- Mỗi feature có `infrastructure/<x>.api.ts` với **chữ ký hàm cuối cùng** (tham số, kiểu trả về theo
  `API_SPEC.md`, DTO snake_case ở `<x>.dto.ts`, đổi sang miền ở `<x>.mapper.ts`).
- Thân hàm rẽ nhánh theo cờ `USE_MOCK` (`PUBLIC_USE_MOCK`, mặc định `true`): mock trả DTO mẫu (từ
  `data.jsx` của prototype) qua `mockDelay` / `mockFail` (lỗi `GNE-*` như server); thật gọi `/api/v1`.
- Dữ liệu dùng chung nhiều feature đặt ở `shared/api/mock/` (`seed.ts` sinh từ prototype, `seller.ts`,
  `admin.ts`, `banners.ts`, `activity.ts`, `placed.ts`) để các màn đổi số theo nhau (ví dụ duyệt KYC ở
  Admin làm giảm huy hiệu hàng chờ).
- Trạng thái mock sửa được giữ trong `sessionStorage['genie.mock.<tên>']` (`mockState`) vì storefront
  là MPA; SSR / test giữ trong bộ nhớ.
- Một bộ dữ liệu mặc định duy nhất, không có công tắc kịch bản (ADR 0005).

## Hệ quả

- Giai đoạn nối backend chỉ thay thân hàm `*.api.ts` và xóa `*.mock.ts` / `shared/api/mock/` —
  danh sách đầy đủ ở `docs/web-backend-handoff.md`.
- Một số trường client cần mà API_SPEC chưa có (ví dụ `product_slug` trong dòng giỏ / đơn) và vài
  endpoint tạm (tra SKU công khai, chính sách phí ship) đã ghi vào danh sách đó để bổ sung spec.
- Mock không có engine giá phía server: tổng tiền checkout do client tính rồi gửi kèm (chỉ ở mock).
