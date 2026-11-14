-- Nhóm 3 — Giỏ hàng
-- Bảng: carts, cart_items
-- Nguồn: ENTITY.md → migration viết tay. KHÔNG sửa migration đã merge; thay đổi = V<n+1>__mo_ta.sql
-- PK: BIGINT GENERATED ALWAYS AS IDENTITY · tiền NUMERIC(19,2) · thời gian TIMESTAMPTZ · enum VARCHAR + CHECK
-- Đặt tên: idx_<bảng>_<cột>, uk_<bảng>_<ý nghĩa>, fk_<bảng>_<bảng đích>


-- Cart
CREATE TABLE carts (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    user_id BIGINT NOT NULL UNIQUE,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_carts PRIMARY KEY (id)
);

-- CartItem
CREATE TABLE cart_items (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    cart_id BIGINT NOT NULL,
    product_variant_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    price_snapshot NUMERIC(19,2) NOT NULL,
    added_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_cart_items PRIMARY KEY (id)
);

-- ===== Khóa ngoại =====
ALTER TABLE carts ADD CONSTRAINT fk_carts_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE cart_items ADD CONSTRAINT fk_cart_items_carts FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE;
ALTER TABLE cart_items ADD CONSTRAINT fk_cart_items_product_variants FOREIGN KEY (product_variant_id) REFERENCES product_variants(id);

