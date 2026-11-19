-- Ghi chú cho người giao hàng gắn với địa chỉ (vd. "Gần cổng ĐH NEU, gửi phòng bảo vệ") — UC-AUTH-09, plan AUTH D27.
-- Không bắt buộc; khác orders.buyer_note (lời nhắn cho shop, viết theo từng đơn).
ALTER TABLE addresses ADD COLUMN delivery_note VARCHAR(200);
