-- Nhóm 2 — Danh mục & sản phẩm
-- Bảng: categories, category_attributes, brands, products, product_variants, product_images, product_documents, category_proposals, product_moderation_results, search_keyword_stats, product_view_stats, user_wishlists
-- Nguồn: ENTITY.md → migration viết tay. KHÔNG sửa migration đã merge; thay đổi = V<n+1>__mo_ta.sql
-- PK: BIGINT GENERATED ALWAYS AS IDENTITY · tiền NUMERIC(19,2) · thời gian TIMESTAMPTZ · enum VARCHAR + CHECK
-- Đặt tên: idx_<bảng>_<cột>, uk_<bảng>_<ý nghĩa>, fk_<bảng>_<bảng đích>


-- Category
CREATE TABLE categories (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255),
    parent_id BIGINT,
    display_order INT,
    icon VARCHAR(50),
    description VARCHAR(500),
    commission_rate NUMERIC(5,4),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    requires_business_license BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_categories PRIMARY KEY (id)
);

-- CategoryAttribute
CREATE TABLE category_attributes (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    category_id BIGINT NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    data_type VARCHAR(20) NOT NULL,
    allowed_values_json jsonb,
    unit VARCHAR(20),
    is_required BOOLEAN NOT NULL DEFAULT FALSE,
    is_filterable BOOLEAN NOT NULL DEFAULT FALSE,
    is_variant_axis BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_category_attributes PRIMARY KEY (id),
    CONSTRAINT uq_category_attribute_code UNIQUE (category_id, code),
    CONSTRAINT ck_category_attributes_data_type CHECK (data_type IN ('TEXT', 'NUMBER', 'BOOLEAN', 'ENUM', 'MULTI_ENUM', 'DATE'))
);

-- Brand
CREATE TABLE brands (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    logo_url VARCHAR(500),
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_by_user_id BIGINT,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_brands PRIMARY KEY (id)
);

-- Product
CREATE TABLE products (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    vendor_id BIGINT NOT NULL,
    category_id BIGINT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    brand VARCHAR(255),
    condition VARCHAR(50),
    origin VARCHAR(100),
    specs_json jsonb,
    category_json jsonb,
    weight DOUBLE PRECISION,
    length DOUBLE PRECISION,
    width DOUBLE PRECISION,
    height DOUBLE PRECISION,
    ships_from VARCHAR(255),
    shipping_payer VARCHAR(20),
    shipping_fee_support NUMERIC(15,2),
    base_price NUMERIC(19,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    moderation_reason TEXT,
    moderator_id BIGINT,
    is_published BOOLEAN,
    version BIGINT NOT NULL DEFAULT 0,
    variant_image_attribute VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    sold INT,
    rating DOUBLE PRECISION,
    brand_id BIGINT,
    item_condition VARCHAR(20) NOT NULL DEFAULT 'NEW',
    is_pre_order BOOLEAN NOT NULL DEFAULT FALSE,
    pre_order_days INT,
    view_count BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT ck_products_shipping_payer CHECK (shipping_payer IN ('BUYER', 'SELLER')),
    CONSTRAINT ck_products_status CHECK (status IN ('DRAFT', 'PENDING_APPROVAL', 'PUBLISHED', 'HIDDEN', 'REJECTED', 'FLAGGED', 'DELETED')),
    CONSTRAINT ck_products_item_condition CHECK (item_condition IN ('NEW', 'USED', 'REFURBISHED'))
);

-- ProductVariant
CREATE TABLE product_variants (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    product_id BIGINT NOT NULL,
    sku VARCHAR(100) NOT NULL UNIQUE,
    variant_name VARCHAR(255),
    attributes_json jsonb,
    price NUMERIC(19,2) NOT NULL,
    stock_count INT NOT NULL,
    reserved_count INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    image_url VARCHAR(500),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT pk_product_variants PRIMARY KEY (id)
);

-- ProductImage
CREATE TABLE product_images (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    product_id BIGINT NOT NULL,
    variant_id BIGINT,
    image_url VARCHAR(500) NOT NULL,
    cdn_url VARCHAR(500),
    width INT,
    height INT,
    file_size BIGINT,
    alt_text VARCHAR(500),
    is_primary BOOLEAN,
    mime_type VARCHAR(50),
    display_order INT NOT NULL,
    CONSTRAINT pk_product_images PRIMARY KEY (id)
);

-- ProductDocument
CREATE TABLE product_documents (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    product_id BIGINT NOT NULL,
    document_type VARCHAR(30) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_extension VARCHAR(20) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    expires_at DATE,
    uploaded_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_product_documents PRIMARY KEY (id),
    CONSTRAINT ck_product_documents_document_type CHECK (document_type IN ('BUSINESS_LICENSE', 'QUALITY_CERTIFICATE', 'IMPORT_DOCUMENT', 'BRAND_AUTHORIZATION', 'OTHER'))
);
CREATE INDEX idx_product_document_product ON product_documents (product_id);

-- CategoryProposal
CREATE TABLE category_proposals (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    vendor_id BIGINT NOT NULL,
    parent_category_id BIGINT,
    proposed_name VARCHAR(100) NOT NULL,
    reason TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_category_id BIGINT,
    reviewed_by_staff_id BIGINT,
    rejection_reason VARCHAR(500),
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_category_proposals PRIMARY KEY (id),
    CONSTRAINT ck_category_proposals_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);
CREATE INDEX idx_category_proposal_status ON category_proposals (status, created_at);

-- ProductModerationResult
CREATE TABLE product_moderation_results (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    product_id BIGINT NOT NULL,
    model_name VARCHAR(60) NOT NULL,
    decision VARCHAR(20) NOT NULL,
    risk_score INT NOT NULL,
    violations_json jsonb,
    reasoning TEXT,
    raw_response TEXT,
    fallback_to_manual BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_product_moderation_results PRIMARY KEY (id),
    CONSTRAINT ck_product_moderation_results_decision CHECK (decision IN ('APPROVE', 'REJECT', 'NEED_REVIEW'))
);
CREATE INDEX idx_moderation_product_time ON product_moderation_results (product_id, created_at);

-- SearchKeywordStat
CREATE TABLE search_keyword_stats (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    normalized_keyword VARCHAR(100) NOT NULL,
    stat_date DATE NOT NULL,
    search_count INT NOT NULL DEFAULT 0,
    result_click_count INT NOT NULL DEFAULT 0,
    is_blocked BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_search_keyword_stats PRIMARY KEY (id),
    CONSTRAINT uq_keyword_day UNIQUE (normalized_keyword, stat_date)
);

-- ProductViewStat
CREATE TABLE product_view_stats (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    product_id BIGINT NOT NULL,
    vendor_id BIGINT NOT NULL,
    stat_date DATE NOT NULL,
    source VARCHAR(20) NOT NULL,
    view_count INT NOT NULL DEFAULT 0,
    unique_visitor_count INT NOT NULL DEFAULT 0,
    add_to_cart_count INT NOT NULL DEFAULT 0,
    CONSTRAINT pk_product_view_stats PRIMARY KEY (id),
    CONSTRAINT uq_product_view_day_source UNIQUE (product_id, stat_date, source),
    CONSTRAINT ck_product_view_stats_source CHECK (source IN ('SEARCH', 'HOME_FEED', 'PROMOTION', 'SHOP_PAGE', 'EXTERNAL_LINK'))
);

-- UserWishlist
CREATE TABLE user_wishlists (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_user_wishlists PRIMARY KEY (id),
    CONSTRAINT uk_user_wishlists_user_id_product_id UNIQUE (user_id, product_id)
);

-- ===== Khóa ngoại =====
ALTER TABLE vendor_profiles ADD CONSTRAINT fk_vendor_profiles_categories FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL;
ALTER TABLE category_attributes ADD CONSTRAINT fk_category_attributes_categories FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE;
ALTER TABLE products ADD CONSTRAINT fk_products_users FOREIGN KEY (vendor_id) REFERENCES users(id);
ALTER TABLE products ADD CONSTRAINT fk_products_categories FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL;
ALTER TABLE products ADD CONSTRAINT fk_products_brands FOREIGN KEY (brand_id) REFERENCES brands(id) ON DELETE SET NULL;
ALTER TABLE product_variants ADD CONSTRAINT fk_product_variants_products FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE;
ALTER TABLE product_images ADD CONSTRAINT fk_product_images_products FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE;
ALTER TABLE product_images ADD CONSTRAINT fk_product_images_product_variants FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE SET NULL;
ALTER TABLE product_documents ADD CONSTRAINT fk_product_documents_products FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE;
ALTER TABLE category_proposals ADD CONSTRAINT fk_category_proposals_users FOREIGN KEY (vendor_id) REFERENCES users(id);
ALTER TABLE category_proposals ADD CONSTRAINT fk_category_proposals_categories FOREIGN KEY (parent_category_id) REFERENCES categories(id) ON DELETE SET NULL;
ALTER TABLE category_proposals ADD CONSTRAINT fk_category_proposals_created_category_id FOREIGN KEY (created_category_id) REFERENCES categories(id) ON DELETE SET NULL;
ALTER TABLE product_moderation_results ADD CONSTRAINT fk_product_moderation_results_products FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE;
ALTER TABLE product_view_stats ADD CONSTRAINT fk_product_view_stats_products FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE;
ALTER TABLE product_view_stats ADD CONSTRAINT fk_product_view_stats_users FOREIGN KEY (vendor_id) REFERENCES users(id);


-- ===== Bổ sung viết tay (ENTITY.md 2.1, 2.2, 2.6, 13.1) =====
-- Category.parentId "FK → chính bảng này" (cây danh mục) — RESTRICT: không xóa được danh mục cha còn con
ALTER TABLE categories ADD CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories(id);

-- Tìm kiếm full-text (UC-CAT-02): unaccent() không IMMUTABLE nên bọc lại để dùng trong cột sinh tự động.
-- Cột search_vector chỉ có ở DB, không ánh xạ trong entity Product (Hibernate validate bỏ qua cột thừa).
CREATE OR REPLACE FUNCTION immutable_unaccent(text) RETURNS text
    LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT
    AS $$ SELECT public.unaccent('public.unaccent'::regdictionary, $1) $$;

ALTER TABLE products ADD COLUMN search_vector tsvector
    GENERATED ALWAYS AS (to_tsvector('simple', immutable_unaccent(
        coalesce(name, '') || ' ' || coalesce(brand, '') || ' ' || coalesce(description, '')))) STORED;

CREATE INDEX idx_products_search_vector ON products USING GIN (search_vector);
CREATE INDEX idx_products_name_trgm ON products USING GIN (name gin_trgm_ops);             -- gợi ý khi gõ sai nhẹ
CREATE INDEX idx_products_category_status_created ON products (category_id, status, created_at DESC);  -- UC-CAT-01 / 05
CREATE INDEX idx_products_vendor_status ON products (vendor_id, status);                   -- UC-CAT-16, kênh người bán
CREATE INDEX idx_products_specs_json ON products USING GIN (specs_json jsonb_path_ops);    -- bộ lọc thuộc tính (2.6)
-- product_variants.sku UNIQUE: đã có ở cột (unique = true)
