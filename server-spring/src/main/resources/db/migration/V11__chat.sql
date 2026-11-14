-- Nhóm 11 — Nhắn tin (chat)
-- Bảng: conversations, messages
-- Nguồn: ENTITY.md → migration viết tay. KHÔNG sửa migration đã merge; thay đổi = V<n+1>__mo_ta.sql
-- PK: BIGINT GENERATED ALWAYS AS IDENTITY · tiền NUMERIC(19,2) · thời gian TIMESTAMPTZ · enum VARCHAR + CHECK
-- Đặt tên: idx_<bảng>_<cột>, uk_<bảng>_<ý nghĩa>, fk_<bảng>_<bảng đích>


-- Conversation
CREATE TABLE conversations (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    customer_id BIGINT NOT NULL,
    vendor_id BIGINT NOT NULL,
    last_message_snippet VARCHAR(255),
    last_message_at TIMESTAMPTZ,
    customer_unread_count INT NOT NULL DEFAULT 0,
    vendor_unread_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_conversations PRIMARY KEY (id),
    CONSTRAINT uk_conversations_customer_id_vendor_id UNIQUE (customer_id, vendor_id)
);

-- Message
CREATE TABLE messages (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    content TEXT,
    attachment_url VARCHAR(500),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    product_variant_id BIGINT,
    order_id BIGINT,
    CONSTRAINT pk_messages PRIMARY KEY (id),
    CONSTRAINT ck_messages_type CHECK (type IN ('TEXT', 'IMAGE', 'PRODUCT_CARD', 'ORDER_CARD'))
);

-- ===== Khóa ngoại =====
ALTER TABLE conversations ADD CONSTRAINT fk_conversations_users FOREIGN KEY (customer_id) REFERENCES users(id);
ALTER TABLE conversations ADD CONSTRAINT fk_conversations_vendor_id FOREIGN KEY (vendor_id) REFERENCES users(id);
ALTER TABLE messages ADD CONSTRAINT fk_messages_conversations FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE;
ALTER TABLE messages ADD CONSTRAINT fk_messages_users FOREIGN KEY (sender_id) REFERENCES users(id);
ALTER TABLE messages ADD CONSTRAINT fk_messages_product_variants FOREIGN KEY (product_variant_id) REFERENCES product_variants(id) ON DELETE SET NULL;
ALTER TABLE messages ADD CONSTRAINT fk_messages_orders FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE SET NULL;


-- ===== Index đường nóng (ENTITY.md 13.1) =====
CREATE INDEX idx_messages_conversation_created ON messages (conversation_id, created_at DESC);  -- UC-SOC-01 tải lịch sử chat (cursor)
