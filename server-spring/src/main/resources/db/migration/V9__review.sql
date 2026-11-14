-- Nhóm 9 — Đánh giá & tương tác shop
-- Bảng: product_reviews, shop_follows, review_reports, review_helpful_votes
-- Nguồn: ENTITY.md → migration viết tay. KHÔNG sửa migration đã merge; thay đổi = V<n+1>__mo_ta.sql
-- PK: BIGINT GENERATED ALWAYS AS IDENTITY · tiền NUMERIC(19,2) · thời gian TIMESTAMPTZ · enum VARCHAR + CHECK
-- Đặt tên: idx_<bảng>_<cột>, uk_<bảng>_<ý nghĩa>, fk_<bảng>_<bảng đích>


-- ProductReview
CREATE TABLE product_reviews (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    order_item_id BIGINT NOT NULL UNIQUE,
    product_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    vendor_id BIGINT NOT NULL,
    rating INT NOT NULL,
    comment TEXT,
    image_urls_json jsonb,
    video_url VARCHAR(500),
    vendor_reply TEXT,
    vendor_replied_at TIMESTAMPTZ,
    is_visible BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    helpful_count INT NOT NULL DEFAULT 0,
    is_anonymous BOOLEAN NOT NULL DEFAULT FALSE,
    aspect_sentiment_json jsonb,
    CONSTRAINT pk_product_reviews PRIMARY KEY (id)
);

-- ShopFollow
CREATE TABLE shop_follows (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    customer_id BIGINT NOT NULL,
    vendor_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_shop_follows PRIMARY KEY (id),
    CONSTRAINT uk_shop_follows_customer_id_vendor_id UNIQUE (customer_id, vendor_id)
);

-- ReviewReport
CREATE TABLE review_reports (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    review_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,
    reason VARCHAR(50) NOT NULL,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_review_reports PRIMARY KEY (id),
    CONSTRAINT ck_review_reports_reason CHECK (reason IN ('SPAM', 'OFFENSIVE', 'FAKE_PURCHASE'))
);

-- ReviewHelpfulVote
CREATE TABLE review_helpful_votes (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    review_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_review_helpful_votes PRIMARY KEY (id),
    CONSTRAINT uq_review_helpful_vote UNIQUE (review_id, user_id)
);

-- ===== Khóa ngoại =====
ALTER TABLE product_reviews ADD CONSTRAINT fk_product_reviews_order_items FOREIGN KEY (order_item_id) REFERENCES order_items(id);
ALTER TABLE product_reviews ADD CONSTRAINT fk_product_reviews_products FOREIGN KEY (product_id) REFERENCES products(id);
ALTER TABLE product_reviews ADD CONSTRAINT fk_product_reviews_users FOREIGN KEY (customer_id) REFERENCES users(id);
ALTER TABLE product_reviews ADD CONSTRAINT fk_product_reviews_vendor_id FOREIGN KEY (vendor_id) REFERENCES users(id);
ALTER TABLE shop_follows ADD CONSTRAINT fk_shop_follows_users FOREIGN KEY (customer_id) REFERENCES users(id);
ALTER TABLE shop_follows ADD CONSTRAINT fk_shop_follows_vendor_id FOREIGN KEY (vendor_id) REFERENCES users(id);
ALTER TABLE review_reports ADD CONSTRAINT fk_review_reports_product_reviews FOREIGN KEY (review_id) REFERENCES product_reviews(id);
ALTER TABLE review_reports ADD CONSTRAINT fk_review_reports_users FOREIGN KEY (reporter_id) REFERENCES users(id);
ALTER TABLE review_helpful_votes ADD CONSTRAINT fk_review_helpful_votes_product_reviews FOREIGN KEY (review_id) REFERENCES product_reviews(id) ON DELETE CASCADE;
ALTER TABLE review_helpful_votes ADD CONSTRAINT fk_review_helpful_votes_users FOREIGN KEY (user_id) REFERENCES users(id);


-- ===== Index đường nóng (ENTITY.md 9.1 a, 13.1) =====
CREATE INDEX idx_product_reviews_product_visible ON product_reviews (product_id, is_visible);          -- trang sản phẩm: đánh giá đang hiển thị
CREATE INDEX idx_product_reviews_vendor_created ON product_reviews (vendor_id, created_at DESC);        -- UC-VND-08 đánh giá theo shop
CREATE INDEX idx_product_reviews_sentiment_overall ON product_reviews ((aspect_sentiment_json ->> 'overall'));  -- UC-AI-07 / UC-SOC-05 tỉ lệ cảm xúc
