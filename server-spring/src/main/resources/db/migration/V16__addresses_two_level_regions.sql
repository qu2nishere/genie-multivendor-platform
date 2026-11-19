-- Địa giới hành chính 2 cấp từ 01/07/2025 (Nghị quyết 202/2025/QH15): bỏ cấp quận / huyện — plan AUTH A12, D25.
-- Địa chỉ mới chỉ lưu tỉnh + phường / xã; hai cột quận cho phép NULL. KHÔNG xóa cột: địa chỉ cũ (trước sáp nhập)
-- vẫn đọc được, người dùng sửa lại thì server tự đặt quận = NULL.
ALTER TABLE addresses ALTER COLUMN district DROP NOT NULL;
ALTER TABLE addresses ALTER COLUMN district_id DROP NOT NULL;
