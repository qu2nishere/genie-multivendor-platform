-- V15 — Thêm loại địa chỉ (UC-AUTH-09; plan/2026-11-14/auth.md D24): ngoài Nhà riêng / Văn phòng có thêm
-- Trường học (SCHOOL), Chung cư (APARTMENT), Khác (OTHER). V1…V14 đã merge → KHÔNG sửa, đổi ràng buộc ở file mới.
-- Không thêm cột: chỉ nới danh sách giá trị của addresses.label (VARCHAR(20) — đủ độ dài).

ALTER TABLE addresses DROP CONSTRAINT ck_addresses_label;
ALTER TABLE addresses ADD CONSTRAINT ck_addresses_label
    CHECK (label IN ('HOME', 'OFFICE', 'SCHOOL', 'APARTMENT', 'OTHER'));
