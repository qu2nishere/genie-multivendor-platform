-- V23 — Cấu hình chung toàn sàn dạng khóa → giá trị JSON (plan B2 D2; UC-CAT-14 bước 8; API 11.8; ENTITY 13.x)
--
-- Khóa đầu tiên: commission.default_rate — hoa hồng mặc định toàn sàn (phân số, vd 0.0500 = 5 %), áp cho ngành không
-- cấu hình riêng (thứ tự: riêng của shop → theo ngành → mặc định). Đổi giá trị ghi system_logs COMMISSION_UPDATED.

CREATE TABLE platform_settings (
    setting_key VARCHAR(100) NOT NULL,
    value_json JSONB NOT NULL,
    updated_by_admin_id BIGINT,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_platform_settings PRIMARY KEY (setting_key)
);

ALTER TABLE platform_settings ADD CONSTRAINT fk_platform_settings_users
    FOREIGN KEY (updated_by_admin_id) REFERENCES users(id) ON DELETE SET NULL;

INSERT INTO platform_settings (setting_key, value_json, updated_at)
VALUES ('commission.default_rate', '0.0500'::jsonb, now())
ON CONFLICT (setting_key) DO NOTHING;
