# 0001 — Storefront mang đủ 9 theme của prototype

- **Trạng thái:** Chấp nhận (chủ dự án chốt 2026-11-11, plan D1)
- **Phạm vi:** `web/`

## Bối cảnh

Prototype `MultivendorEcommercePrototype/genie/ui_kits/storefront/themes.css` có 9 theme
(`genie` mặc định, `classic`, `dongho`, `dongson`, `hoian`, `polymer`, `buudien`, `saigon`, `sleeve`).
Mỗi theme đổi màu, font hiển thị, ảnh nền, hình khối của thẻ sản phẩm / phiếu voucher / footer bằng
bộ chọn `[data-theme=…]`. Chỉ mang theme mặc định thì ít việc hơn nhưng mất phần nhận diện đã thiết kế.

## Quyết định

- Làm đủ 9 theme, `genie` trước, 8 theme còn lại ở G07 nhóm D.
- Theme áp cho cả 4 bề mặt (storefront, auth, Seller, Admin); nút đổi theme đặt đúng vị trí prototype.
- Theme chọn bằng `?theme=` → `localStorage['genie.theme']` → mặc định `genie`, áp trước khi vẽ
  (`layouts/partials/ThemeBoot.astro`) để không nháy theme.
- Font của 8 theme (OFL 1.1) tự lưu trữ ở `public/fonts/themes/`, chỉ tải khi theme đó bật.

## Hệ quả

- `styles/themes.css` giữ nguyên dạng CSS thuần, không bọc `@layer` (để `[data-theme]` đè utility).
- CSS chặn render lớn hơn (~186 KB, 34 KB gzip, gồm cả 9 theme) — ghi ở G13.09; có thể tách CSS theo
  theme khi cần tối ưu.
- Mọi màn mới phải kiểm tương phản ở cả 9 theme (G07.26, G13.07).
