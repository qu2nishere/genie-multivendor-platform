# 0004 — Port bảng icon `ICONS` của prototype, không thêm thư viện icon

- **Trạng thái:** Chấp nhận (chạy theo khuyến nghị, plan D4)
- **Phạm vi:** `web/`

## Bối cảnh

Prototype vẽ icon bằng bảng `ICONS` (SVG nội tuyến, tên theo Lucide). Thêm `lucide-react` cho thêm
phụ thuộc và phải rà lại tên / nét vẽ để khớp hình.

## Quyết định

- Chép bảng sang `shared/ui/icon/icons.ts` với kiểu `IconName`; component `Icon` ở
  `shared/ui/icon/Icon.tsx`.
- Icon thiếu so với nhu cầu (ví dụ `arrow-down-right` cho `Delta`) bổ sung tay theo nét Lucide.

## Hệ quả

- Không thêm phụ thuộc; hình khớp 100% prototype.
- Tên icon sai bị `tsc` bắt (kiểu `IconName`).
- Bảng icon nhỏ (~20 KB nguồn) nên không phải nguyên nhân chunk chung lớn (chunk đó chứa từ điển i18n —
  xem G13.09).
