-- Dữ liệu mẫu 80 — hồ sơ KYC cho hàng chờ Admin (plan seed 2026-11-16 S06.05–S06.07; mock web adminKycStore; ENTITY 1.6).
-- SQL thuần, chạy lại được, không ghi cứng id. Mật khẩu chủ shop: Genie@123.
--   3 MANUAL_REVIEW (rủi ro 12 — bị lấy mẫu hậu kiểm, 42, 65) · 1 SUPPLEMENT_REQUESTED · 1 REJECTED; kyc_submitted_at lệch nhau.
-- "TravelGear" của mock trùng tên shop đã KYC ở file 30 (V15 thêm UNIQUE lower(shop_name)) → đặt "TravelGear Outdoor".
-- Cột ảnh giấy tờ (id_front_url…) để NULL: ảnh thật được mã hóa bởi luồng nộp hồ sơ (UC-VND-02) — muốn xem ảnh thì tự nộp một hồ sơ.

DROP TABLE IF EXISTS seed_kyc;
CREATE TEMP TABLE seed_kyc AS
SELECT * FROM (VALUES
    -- email                          người đại diện     tên shop               ngành        loại hình     trạng thái              nộp (giờ trước) ghi chú / lý do
    ('kyc.nhasachmoc@genie.example',  'Phạm Thị Lan',    'Nhà Sách Mộc',        'sach',       'HOUSEHOLD',  'MANUAL_REVIEW',        2,   NULL),
    ('kyc.gombattrang@genie.example', 'Trần Quốc Bảo',   'Gốm Bát Tràng Xưa',   'nha-cua',    'BUSINESS',   'MANUAL_REVIEW',        4,   NULL),
    ('kyc.mebehappy@genie.example',   'Nguyễn Thị Hoa',  'Mẹ & Bé Happy',       'me-be',      'INDIVIDUAL', 'MANUAL_REVIEW',        19,  NULL),
    ('kyc.travelgear@genie.example',  'Đỗ Minh Khang',   'TravelGear Outdoor',  'phu-kien',   'HOUSEHOLD',  'SUPPLEMENT_REQUESTED', 44,  'Ảnh mặt sau CCCD bị lóa, vui lòng chụp lại rõ nét.'),
    ('kyc.cheapelec@genie.example',   'Lý Văn Tèo',      'Cheap Electronics',   'dien-tu',    'INDIVIDUAL', 'REJECTED',             70,  'Thông tin trên giấy tờ không khớp với ảnh chân dung.')
) AS v(email, representative, shop_name, category_slug, business_type, kyc_status, submitted_hours_ago, note);

INSERT INTO users (email, password, role, status, email_verified_at, failed_login_attempts, version, created_at, updated_at)
SELECT k.email, '$argon2id$v=19$m=19456,t=2,p=1$ZebNGoJ3c4LESBr+rpyuLQ$9pb9gk4E8gkfmpVrHDFU7QHxJBsSLf1b8ysu6AWoH4M',
       'VENDOR', 'ACTIVE', now(), 0, 0, now() - make_interval(hours => k.submitted_hours_ago + 24), now()
FROM seed_kyc k
ON CONFLICT (email) DO NOTHING;

INSERT INTO customer_profiles (user_id, full_name, preferred_language, created_at, updated_at)
SELECT u.id, k.representative, 'vi', now(), now()
FROM seed_kyc k JOIN users u ON u.email = k.email
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO vendor_profiles (user_id, shop_name, category_id, business_type, representative, kyc_status,
                             kyc_rejection_reason, kyc_verification_method, kyc_verified_by, kyc_submitted_at, kyc_verified_at,
                             created_at, is_mall, rating_avg, rating_count, follower_count, active_product_count,
                             pre_order_enabled, is_favorite, auto_reply_enabled)
SELECT u.id, k.shop_name, c.id, k.business_type, k.representative, k.kyc_status,
       k.note,
       CASE WHEN k.kyc_status = 'REJECTED' THEN 'MANUAL_STAFF' END,
       CASE WHEN k.kyc_status IN ('REJECTED', 'SUPPLEMENT_REQUESTED') THEN a.id END,
       now() - make_interval(hours => k.submitted_hours_ago),
       NULL,
       now() - make_interval(hours => k.submitted_hours_ago + 24), FALSE, 0, 0, 0, 0, FALSE, FALSE, FALSE
FROM seed_kyc k
JOIN users u ON u.email = k.email
LEFT JOIN categories c ON c.slug = k.category_slug AND c.parent_id IS NULL
LEFT JOIN users a ON a.email = 'phat.nt@genie.example'
ON CONFLICT (user_id) DO NOTHING;

-- ===== kyc_results cho 3 hồ sơ chờ duyệt (ENTITY 1.6 — bảng tín hiệu rủi ro) =====
INSERT INTO kyc_results (vendor_profile_id, extracted_id_number, extracted_full_name, extracted_dob, extracted_gender,
                         extracted_nationality, extracted_address, extracted_issue_date, extracted_expiry_date, extracted_issuer,
                         qr_code_detected, mrz_valid, mrz_qr_match, cccd_number_valid, expiry_rule_valid, duplicate_id_found,
                         face_match_result, face_match_score, liveness_score, face_duplicate_found,
                         risk_score, risk_signals_json, sampled_for_review, errors, overall_status, processed_at, created_at)
SELECT vp.id, r.id_number, r.full_name, r.dob, r.gender, 'Việt Nam', r.address, r.issue_date, r.expiry_date,
       'Cục Cảnh sát quản lý hành chính về trật tự xã hội',
       r.qr, r.mrz, r.mrz_qr, TRUE, TRUE, FALSE,
       r.face_ok, r.face_score, r.liveness, FALSE,
       r.risk, r.signals, r.sampled, NULL, 'PARTIAL',
       vp.kyc_submitted_at + interval '2 minutes', vp.kyc_submitted_at + interval '1 minute'
FROM (VALUES
    -- email                          số CCCD (mẫu)    họ tên             ngày sinh     giới  địa chỉ                                      cấp           hạn           QR     MRZ    QR=MRZ mặt   điểm mặt liveness rủi ro tín hiệu                                                                                   lấy mẫu
    ('kyc.nhasachmoc@genie.example',  '079190000012', 'PHẠM THỊ LAN',    '12/04/1990', 'Nữ', 'Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh',    '10/05/2021', '12/04/2030', TRUE,  TRUE,  TRUE,  TRUE,  0.9120, 0.9600, 12, '[]',                                                                                       TRUE),
    ('kyc.gombattrang@genie.example', '001085000042', 'TRẦN QUỐC BẢO',   '03/09/1985', 'Nam', 'Xã Bát Tràng, Huyện Gia Lâm, Hà Nội',          '22/07/2021', '03/09/2025', TRUE,  TRUE,  TRUE,  TRUE,  0.7800, 0.9100, 42, '[{"code":"FACE_SCORE_BORDERLINE","points":25},{"code":"DECLARED_DATA_MISMATCH","points":20}]', FALSE),
    ('kyc.mebehappy@genie.example',   '036195000065', 'NGUYỄN THỊ HOA',  '21/11/1995', 'Nữ', 'Phường Hải Châu 1, Quận Hải Châu, Đà Nẵng',   '02/03/2022', '21/11/2035', FALSE, FALSE, NULL,  TRUE,  0.8100, 0.8800, 65, '[{"code":"UNREADABLE_IMAGE","points":30},{"code":"FACE_SCORE_BORDERLINE","points":25}]',      FALSE)
) AS r(email, id_number, full_name, dob, gender, address, issue_date, expiry_date, qr, mrz, mrz_qr, face_ok, face_score, liveness,
       risk, signals, sampled)
JOIN users u ON u.email = r.email
JOIN vendor_profiles vp ON vp.user_id = u.id
ON CONFLICT (vendor_profile_id) DO NOTHING;

-- ===== S06.07 cột của V15 (plan Mở shop & KYC) — chỉ điền khi cột đã có, để file chạy được cả trước và sau V15 =====
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = current_schema() AND table_name = 'vendor_profiles'
                 AND column_name = 'kyc_supplement_deadline_at') THEN
        EXECUTE $q$
            UPDATE vendor_profiles vp
            SET kyc_supplement_deadline_at = vp.kyc_submitted_at + interval '7 days'
            FROM users u
            WHERE vp.user_id = u.id AND u.email = 'kyc.travelgear@genie.example'
              AND vp.kyc_status = 'SUPPLEMENT_REQUESTED' AND vp.kyc_supplement_deadline_at IS NULL
        $q$;
    END IF;
END
$$;

DROP TABLE seed_kyc;
