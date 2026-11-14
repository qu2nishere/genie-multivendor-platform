# 0002 — CSS lai: utility Tailwind + giữ lớp `tr-*` / theme trong `styles/`

- **Trạng thái:** Chấp nhận (chạy theo khuyến nghị, plan D2)
- **Phạm vi:** `web/`

## Bối cảnh

Prototype dùng inline style dày đặc + `storefront.css` (lớp `tr-*`) + `dash.css` + `themes.css`.
Viết lại 100% bằng utility Tailwind tốn công và làm khó việc giữ 9 theme (ADR 0001), vốn dựa vào bộ
chọn `[data-theme] .tr-…`.

## Quyết định

- `shared/ui`, màn Seller / Admin, inline style của prototype → **utility Tailwind**, giá trị lấy từ
  token (`styles/tokens.css` → `tailwind.config.ts`).
- Lớp `tr-*` của storefront → giữ CSS, tách theo khối ở `styles/storefront/*.css`, bọc
  `@layer components`; lớp `dash-*` ở `styles/dash.css`. Màu cứng của prototype đổi sang token.
- `themes.css` giữ nguyên, không bọc `@layer`.
- Tailwind `safelist: /^(tr|dash|gn|lz)-/` để lớp ghép động không bị loại.

## Hệ quả

- Hai cách viết cùng tồn tại; quy tắc: component mới viết utility, chỉ sửa `styles/` khi đụng lớp
  `tr-*` / `dash-*` / theme.
- Lỗi kiểu "đè thứ tự" có thể xảy ra giữa utility và `@layer components` (ví dụ G13.05: `<h2>` trong
  footer đặt màu cứng đè màu theme) — khi so hình phải kiểm cả 9 theme.
