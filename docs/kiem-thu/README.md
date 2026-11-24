# Kiểm thử thủ công

| File | Nội dung |
|---|---|
| [kich-ban-2026-11-16-chuc-nang-chung.md](kich-ban-2026-11-16-chuc-nang-chung.md) | Plan 2026-11-16 — seed, Nhật ký hệ thống (UC-ADM-07), Banner (UC-ADM-12), Điểm Genie (UC-PRO-08/09), Yêu thích (UC-CAT-08), Theo dõi shop (UC-SOC-06) |
| [kich-ban-2026-11-16-mo-shop-kyc.md](kich-ban-2026-11-16-mo-shop-kyc.md) | Plan 2026-11-16 — toàn bộ luồng **Mở shop & KYC**: khách đăng ký → mở shop (UC-VND-01) → nộp hồ sơ (02) → AI thẩm định (03) → nhân viên duyệt (04) → tài khoản nhận tiền (07) → cài đặt shop (21) → khách hàng theo dõi shop mới; AI thật (tùy chọn) |
| [kich-ban-2026-11-24-b1-b2-b3.md](kich-ban-2026-11-24-b1-b2-b3.md) | Plan 2026-11-24 — rà soát làm bù B1 / B2 / B3: khóa / mở khóa + cron hết hạn đình chỉ (UC-ADM-03), cây danh mục (UC-CAT-01), quản lý danh mục & hoa hồng (UC-CAT-14), bộ thuộc tính (UC-CAT-15), lọc "Shop có voucher" + thuộc tính (UC-CAT-05) — 60 kịch bản kèm kết quả 2026-11-24 |
| [kyc-viec-chua-lam.md](kyc-viec-chua-lam.md) | Chức năng KYC chưa làm / chưa kiểm thật, lý do và thứ tự gợi ý |

Tài liệu liên quan: [../huong-dan-chay-local.md](../huong-dan-chay-local.md) (chạy máy dev) · [../kyc-giai-thich-cho-nguoi-moi.md](../kyc-giai-thich-cho-nguoi-moi.md) (KYC từ số 0) · [../redis-local.md](../redis-local.md) (xem Redis).

Quy ước: mã `TC-…`; đạt tick `☑`, sai `☒` + ghi chú / ảnh / log. Cột **Chạy** trong `docs/Document/CHECKLIST_TIEN_DO_UC.md` chỉ tick khi chính người
kiểm đã thao tác trên giao diện với hệ thống thật.
