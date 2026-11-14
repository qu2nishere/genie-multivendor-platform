-- Nhóm 8 — Hoàn tiền / khiếu nại
-- Bảng: refund_requests, refund_request_items, refund_evidences
-- Nguồn: ENTITY.md → migration viết tay. KHÔNG sửa migration đã merge; thay đổi = V<n+1>__mo_ta.sql
-- PK: BIGINT GENERATED ALWAYS AS IDENTITY · tiền NUMERIC(19,2) · thời gian TIMESTAMPTZ · enum VARCHAR + CHECK
-- Đặt tên: idx_<bảng>_<cột>, uk_<bảng>_<ý nghĩa>, fk_<bảng>_<bảng đích>


-- RefundRequest
CREATE TABLE refund_requests (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    order_id BIGINT NOT NULL,
    requested_by_user_id BIGINT NOT NULL,
    refund_amount NUMERIC(19,2) NOT NULL,
    reason TEXT,
    request_type VARCHAR(30) NOT NULL,
    resolution_mode VARCHAR(30),
    status VARCHAR(30) NOT NULL,
    resolved_by_staff_id BIGINT,
    resolution_note TEXT,
    requested_at TIMESTAMPTZ NOT NULL,
    shop_response_due_at TIMESTAMPTZ NOT NULL,
    evidence_requested_at TIMESTAMPTZ,
    evidence_request_note VARCHAR(500),
    resolved_at TIMESTAMPTZ,
    shop_rejected_at TIMESTAMPTZ,
    return_due_at TIMESTAMPTZ,
    return_shipped_at TIMESTAMPTZ,
    return_received_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_refund_requests PRIMARY KEY (id),
    CONSTRAINT ck_refund_requests_request_type CHECK (request_type IN ('RETURN_AND_REFUND', 'REFUND_ONLY')),
    CONSTRAINT ck_refund_requests_resolution_mode CHECK (resolution_mode IN ('APPROVE_WITH_RETURN', 'APPROVE_REFUND_ONLY')),
    CONSTRAINT ck_refund_requests_status CHECK (status IN ('REQUESTED', 'AWAITING_EVIDENCE', 'SHOP_REJECTED', 'DISPUTED', 'AWAITING_RETURN_SHIPMENT', 'RETURN_FAILED', 'APPROVED', 'REJECTED', 'REFUNDED'))
);

-- RefundRequestItem
CREATE TABLE refund_request_items (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    refund_request_id BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    item_refund_amount NUMERIC(19,2) NOT NULL,
    CONSTRAINT pk_refund_request_items PRIMARY KEY (id)
);

-- RefundEvidence
CREATE TABLE refund_evidences (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    refund_request_id BIGINT NOT NULL,
    media_url VARCHAR(500) NOT NULL,
    media_type VARCHAR(50) NOT NULL,
    display_order INT NOT NULL,
    submission_round INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_refund_evidences PRIMARY KEY (id),
    CONSTRAINT ck_refund_evidences_media_type CHECK (media_type IN ('IMAGE', 'VIDEO'))
);

-- ===== Khóa ngoại =====
ALTER TABLE payment_refunds ADD CONSTRAINT fk_payment_refunds_refund_requests FOREIGN KEY (refund_request_id) REFERENCES refund_requests(id);
ALTER TABLE shipment_trackings ADD CONSTRAINT fk_shipment_trackings_refund_requests FOREIGN KEY (refund_request_id) REFERENCES refund_requests(id) ON DELETE SET NULL;
ALTER TABLE refund_requests ADD CONSTRAINT fk_refund_requests_orders FOREIGN KEY (order_id) REFERENCES orders(id);
ALTER TABLE refund_requests ADD CONSTRAINT fk_refund_requests_users FOREIGN KEY (requested_by_user_id) REFERENCES users(id);
ALTER TABLE refund_request_items ADD CONSTRAINT fk_refund_request_items_refund_requests FOREIGN KEY (refund_request_id) REFERENCES refund_requests(id) ON DELETE CASCADE;
ALTER TABLE refund_request_items ADD CONSTRAINT fk_refund_request_items_order_items FOREIGN KEY (order_item_id) REFERENCES order_items(id);
ALTER TABLE refund_evidences ADD CONSTRAINT fk_refund_evidences_refund_requests FOREIGN KEY (refund_request_id) REFERENCES refund_requests(id) ON DELETE CASCADE;

