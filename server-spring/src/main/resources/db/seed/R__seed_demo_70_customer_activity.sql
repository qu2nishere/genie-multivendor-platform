-- Dữ liệu mẫu 70 — hoạt động của khách mẫu mai.tran@gmail.com (plan seed 2026-11-16 S06.01–S06.04; API 3.10, 3.11, 3.13).
-- SQL thuần, chạy lại được, không ghi cứng id.
--
-- Điểm Genie: 1.250 Điểm, Σ coin_transactions.amount = user_coins.balance. Sổ Điểm chỉ ghi thêm → cả khối Điểm chỉ chèn khi
-- tài khoản CHƯA có ví Điểm (đã điểm danh / đổi quà thật thì seed không đụng vào để không lệch số dư).
-- Các lô (FEFO — trừ lô hết hạn sớm nhất trước, plan Điểm Genie P02.03):
--   -370 ngày  +200 hoàn Điểm đơn (hết hạn -5 ngày)  → bị -200 lúc -360 ngày tiêu hết, không còn lô hết hạn dư
--   -345 ngày  +150 đánh giá có ảnh (hết hạn +20 ngày) → "150 Điểm sắp hết hạn trong 30 ngày"
--   -90 ngày   +800 hoàn Điểm đơn (hết hạn sau 12 tháng)
--   -3/-2/-1 ngày +100 × 3 điểm danh (chuỗi 3 ngày — hôm nay điểm danh được ngày thứ 4)
-- balance_hash = HMAC-SHA256(WALLET_HMAC_SECRET, 'coin:' || user_id || ':' || balance) hex chữ thường (plan Điểm Genie D2),
-- khóa máy dev 'local-wallet-secret' — tính bằng sha256() với ipad / opad đệm sẵn (xem giải thích ở R__seed_demo_30_shops.sql).

WITH me AS (
    SELECT id FROM users WHERE email = 'mai.tran@gmail.com'
), wallet AS (
    INSERT INTO user_coins (user_id, balance, version, balance_hash, last_updated)
    SELECT me.id, 1250, 0,
           encode(sha256(
               decode('30333f3d30712b3d30303928712f393f2e39285c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c', 'hex')
               || sha256(decode('5a5955575a1b41575a5a53421b455355445342363636363636363636363636363636363636363636363636363636363636363636363636363636363636363636', 'hex')
                         || convert_to('coin:' || me.id || ':1250', 'UTF8'))), 'hex'),
           now() - interval '1 day'
    FROM me
    WHERE NOT EXISTS (SELECT 1 FROM coin_transactions t WHERE t.user_id = me.id)
    ON CONFLICT (user_id) DO NOTHING
    RETURNING user_id
)
INSERT INTO coin_transactions (user_id, amount, type, reference_id, label, expires_at, created_at)
SELECT w.user_id, v.amount, v.type, v.reference_id, v.label,
       CASE WHEN v.amount > 0 THEN now() + make_interval(days => v.created_days) + interval '12 months' END,
       now() + make_interval(days => v.created_days)
FROM wallet w
CROSS JOIN (VALUES
    (200,  'ORDER_CASHBACK',   'TR-SEED-000101', 'Hoàn Điểm đơn hàng',               -370),
    (-200, 'USED_AT_CHECKOUT', 'TR-SEED-000102', 'Dùng Điểm khi thanh toán',         -360),
    (150,  'REVIEW_BONUS',     'RV-SEED-000201', 'Thưởng đánh giá có ảnh',           -345),
    (800,  'ORDER_CASHBACK',   'TR-SEED-000103', 'Hoàn Điểm đơn hàng',               -90),
    (100,  'CHECK_IN',         NULL,             'Điểm danh hằng ngày (ngày 1)',     -3),
    (100,  'CHECK_IN',         NULL,             'Điểm danh hằng ngày (ngày 2)',     -2),
    (100,  'CHECK_IN',         NULL,             'Điểm danh hằng ngày (ngày 3)',     -1)
) AS v(amount, type, reference_id, label, created_days);

-- ===== daily_checkins — mỗi khách một dòng (uk_daily_checkin_customer): chuỗi 3 ngày, lần cuối hôm qua =====
INSERT INTO daily_checkins (customer_id, last_checkin_at, streak_count)
SELECT u.id, now() - interval '1 day', 3
FROM users u WHERE u.email = 'mai.tran@gmail.com'
ON CONFLICT (customer_id) DO NOTHING;

-- ===== user_wishlists — thích 2 sản phẩm (tra theo tên sản phẩm của shop seed) =====
INSERT INTO user_wishlists (user_id, product_id, created_at)
SELECT u.id, p.id, now() - make_interval(days => v.days_ago)
FROM (VALUES
    ('Serum dưỡng da Vitamin C 30ml làm sáng da', 5),
    ('Giày sneaker thể thao Air Run nam nữ êm chân', 2)
) AS v(product_name, days_ago)
JOIN users u ON u.email = 'mai.tran@gmail.com'
JOIN products p ON p.name = v.product_name
ON CONFLICT (user_id, product_id) DO NOTHING;

-- ===== shop_follows — theo dõi 3 shop KHÔNG phải TechZone (không tự theo dõi shop mình);
--       chỉ cộng follower_count cho dòng vừa chèn (chạy lại không cộng thêm) =====
WITH ins AS (
    INSERT INTO shop_follows (customer_id, vendor_id, created_at)
    SELECT c.id, o.id, now() - make_interval(days => v.days_ago)
    FROM (VALUES
        ('owner.boutique@genie.example', 40),
        ('owner.beauty@genie.example', 25),
        ('owner.shoe@genie.example', 10)
    ) AS v(owner_email, days_ago)
    JOIN users c ON c.email = 'mai.tran@gmail.com'
    JOIN users o ON o.email = v.owner_email
    ON CONFLICT (customer_id, vendor_id) DO NOTHING
    RETURNING vendor_id
)
UPDATE vendor_profiles vp SET follower_count = vp.follower_count + 1
FROM ins WHERE vp.user_id = ins.vendor_id;
