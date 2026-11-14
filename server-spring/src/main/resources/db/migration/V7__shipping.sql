-- Nhóm 7 — Vận chuyển
-- Bảng: shipment_trackings, shipment_events, vendor_shipping_config
-- Nguồn: ENTITY.md → migration viết tay. KHÔNG sửa migration đã merge; thay đổi = V<n+1>__mo_ta.sql
-- PK: BIGINT GENERATED ALWAYS AS IDENTITY · tiền NUMERIC(19,2) · thời gian TIMESTAMPTZ · enum VARCHAR + CHECK
-- Đặt tên: idx_<bảng>_<cột>, uk_<bảng>_<ý nghĩa>, fk_<bảng>_<bảng đích>


-- ShipmentTracking
CREATE TABLE shipment_trackings (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    order_id BIGINT NOT NULL,
    direction VARCHAR(10) NOT NULL,
    refund_request_id BIGINT,
    tracking_code VARCHAR(100),
    carrier_type VARCHAR(30),
    carrier_order_id VARCHAR(100),
    status VARCHAR(50) NOT NULL,
    estimated_delivery_at TIMESTAMPTZ,
    actual_delivered_at TIMESTAMPTZ,
    CONSTRAINT pk_shipment_trackings PRIMARY KEY (id),
    CONSTRAINT ck_shipment_trackings_direction CHECK (direction IN ('OUTBOUND', 'RETURN')),
    CONSTRAINT ck_shipment_trackings_carrier_type CHECK (carrier_type IN ('GHN', 'GHTK', 'VIETTEL_POST', 'JNT_EXPRESS', 'GENIE_EXPRESS')),
    CONSTRAINT ck_shipment_trackings_status CHECK (status IN ('AWAITING_PICKUP', 'PICKED_UP', 'IN_TRANSIT', 'OUT_FOR_DELIVERY', 'DELIVERED', 'FAILED', 'RETURNING', 'RETURNED', 'LOST'))
);

-- ShipmentEvent
CREATE TABLE shipment_events (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    shipment_id BIGINT NOT NULL,
    event_status VARCHAR(100) NOT NULL,
    location VARCHAR(255),
    occurred_at TIMESTAMPTZ NOT NULL,
    description VARCHAR(500),
    received_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_shipment_events PRIMARY KEY (id)
);

-- VendorShippingConfig
CREATE TABLE vendor_shipping_config (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    vendor_id BIGINT NOT NULL UNIQUE,
    ghn_enabled BOOLEAN NOT NULL,
    ghtk_enabled BOOLEAN NOT NULL,
    vtp_enabled BOOLEAN NOT NULL,
    jt_enabled BOOLEAN NOT NULL,
    genie_express_enabled BOOLEAN NOT NULL,
    freeship_threshold NUMERIC(15,2),
    CONSTRAINT pk_vendor_shipping_config PRIMARY KEY (id)
);

-- ===== Khóa ngoại =====
ALTER TABLE shipment_trackings ADD CONSTRAINT fk_shipment_trackings_orders FOREIGN KEY (order_id) REFERENCES orders(id);
ALTER TABLE shipment_events ADD CONSTRAINT fk_shipment_events_shipment_trackings FOREIGN KEY (shipment_id) REFERENCES shipment_trackings(id) ON DELETE CASCADE;
ALTER TABLE vendor_shipping_config ADD CONSTRAINT fk_vendor_shipping_config_users FOREIGN KEY (vendor_id) REFERENCES users(id) ON DELETE CASCADE;
-- FK nợ: shipment_trackings.refund_request_id -> refund_requests(id) (tạo ở V8)

-- ===== Bổ sung viết tay (ENTITY.md 7.1 — chú thích DDL của @Table) =====
-- Mỗi đơn đúng 1 vận đơn chiều đi; mỗi yêu cầu trả hàng đúng 1 vận đơn chiều về (Reverse Logistics)
CREATE UNIQUE INDEX uq_shipment_outbound ON shipment_trackings (order_id) WHERE direction = 'OUTBOUND';
CREATE UNIQUE INDEX uq_shipment_return ON shipment_trackings (refund_request_id) WHERE direction = 'RETURN';
