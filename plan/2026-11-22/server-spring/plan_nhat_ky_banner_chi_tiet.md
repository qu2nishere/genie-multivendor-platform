# Plan — Nhật ký banner theo hành động cụ thể & xem chi tiết dòng nhật ký

> Người làm: Nguyễn Ngọc Thiện · Ngày: 2026-11-22 · UC liên quan: UC-ADM-07 (Nhật ký hệ thống), UC-ADM-12 (Banner)
> Tiến độ: [progress_nhat_ky_banner_chi_tiet.md](progress_nhat_ky_banner_chi_tiet.md)

## 1. Vấn đề

- Mọi thao tác banner (tạo / sửa / bật / tắt / xóa) đang ghi **một** mã `BANNER_CHANGED`, loại thao tác nằm trong `details.op`.
  Màn Nhật ký chỉ hiện `BN-22 · hihi` → không biết là tạo hay xóa; không lọc được "ai đã xóa banner".
  Lệch với phần còn lại của `AuditAction` (đều là động từ cụ thể: `KYC_APPROVED`, `PAYOUT_REJECTED`…).
- Giá trị trước / sau (`details.changes`) đã lưu nhưng API không trả → màn hình không xem được.
- Màn Nhật ký hiện mã thô (`BANNER_CHANGED`), không dịch; không có chỗ xem `user_agent`, `reference_id`.

## 2. Quyết định

| Mã | Câu hỏi | Chốt |
|---|---|---|
| N1 | Tách mã hay giữ `op`? | **Tách** thành 5 mã động từ: `BANNER_CREATED`, `BANNER_UPDATED`, `BANNER_ACTIVATED`, `BANNER_DEACTIVATED`, `BANNER_DELETED` — cùng kiểu với các mã khác, lọc / thống kê được bằng cột `action_type`. |
| N2 | `BANNER_CHANGED` cũ? | **Giữ** (NFR-12 chỉ thêm, không đổi tên / xóa enum) — chỉ còn cho bản ghi cũ, code mới không ghi nữa. Nhóm vẫn `CONFIG`. |
| N3 | Đổi schema? | Migration mới `V21__banner_audit_granular_actions.sql` nới `ck_system_logs_action_type` (chép đủ danh sách V20 + 5 mã). ENTITY.md sửa trước (TS 7.3). |
| N4 | API trả thêm gì? | Mỗi dòng `GET /admin/audit-logs` thêm `reference_id`, `user_agent`, `changes` (object `{trường: {from, to}}` lấy từ `details.changes`, `null` nếu không có). Không trả nguyên `details` (có thể chứa dữ liệu nội bộ). CSV giữ nguyên cột. |
| N5 | Hiển thị | Web dịch tên hành động qua i18n (`admin-system.audit.actions.<MÃ>`, thiếu khóa thì hiện mã), bấm một dòng mở hộp chi tiết: thời gian, người làm, hành động, đối tượng, mã tham chiếu, IP, user agent, bảng thay đổi trước → sau. |

## 3. Task

### P00 — Tài liệu trước
- **[P00.01]** ENTITY.md Phụ lục A: thêm 5 mã, ghi chú `BANNER_CHANGED` chỉ còn cho bản ghi cũ; câu "Nhóm hiển thị" thêm 5 mã vào CONFIG. Đồng bộ `java-code/Diagram10_NotificationBannerAndAudit.java`.
- **[P00.02]** API_SPEC: mục Nhật ký (11.10) thêm 3 trường mới vào ví dụ + mô tả; mục 16 `AuditCategory` thêm 5 mã vào CONFIG.
- **[P00.03]** COMMIT `docs(admin): split banner audit action into explicit verbs`

### P01 — CSDL & enum
- **[P01.01]** `V21__banner_audit_granular_actions.sql` — DROP/ADD `ck_system_logs_action_type` với toàn bộ giá trị V20 + 5 mã mới.
- **[P01.02]** `AuditAction` thêm 5 mã (khối Banner), `category()` → `CONFIG`; javadoc `BANNER_CHANGED` = bản ghi cũ.
- **[P01.03]** COMMIT `feat(db): add explicit banner audit actions`

### P02 — Ghi nhật ký banner
- **[P02.01]** `BannerAdminService.record` nhận `AuditAction` thay cho chuỗi `op`; tạo → `BANNER_CREATED`, sửa → `BANNER_UPDATED` (kèm `changes` from/to), bật / tắt → `BANNER_ACTIVATED` / `BANNER_DEACTIVATED`, xóa → `BANNER_DELETED`. `BannerChangedEvent` giữ chuỗi op (xóa cache).
- **[P02.02]** `BannerAdminIT` kiểm đúng mã theo từng thao tác.
- **[P02.03]** COMMIT `feat(admin): log banner changes with explicit audit actions`

### P03 — API Nhật ký trả chi tiết
- **[P03.01]** `AuditLogView` thêm `referenceId`, `userAgent`, `changes` (`Map<String, AuditChange>`; `AuditChange(from, to)`).
- **[P03.02]** `AuditLogQueryService.toView` đọc `details` một lần: `target_label` + `changes` (chỉ nhận object có `from` / `to`, bỏ qua dạng lạ).
- **[P03.03]** Test: unit cho đọc `changes` / `details` hỏng; IT kiểm 3 trường mới trên response.
- **[P03.04]** COMMIT `feat(admin): expose reference, user agent and changes in audit log api`

### P04 — Web
- **[P04.01]** DTO / types / mapper thêm `referenceId`, `userAgent`, `changes`; mock có dữ liệu mẫu.
- **[P04.02]** i18n `admin-system.audit.actions.*` (vi / en) cho mọi `AuditAction`; hàm `actionLabel` (thiếu khóa → mã).
- **[P04.03]** `AuditLogPage`: cột Hành động hiện nhãn đã dịch (giữ mã trong `title`), màu: `*_DELETED` / `*_REJECTED` / `*_FAILED` đỏ…; bấm dòng (chuột / Enter) mở `AuditLogDetail` (Modal) — bảng trước → sau.
- **[P04.04]** Test rules (`actionLabel`, `actionTone`), `npm run verify`.
- **[P04.05]** COMMIT `feat(admin-ui): show translated audit actions and a detail dialog`

### P05 — Đồng bộ tiến độ
- **[P05.01]** Kịch bản kiểm thử (TC-01-09, TC-02-15), CHECKLIST nhật ký, CLAUDE.md mục 2 (một dòng).
- **[P05.02]** COMMIT `docs: update audit log scenarios and progress`
