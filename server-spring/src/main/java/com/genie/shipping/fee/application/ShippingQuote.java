package com.genie.shipping.fee.application;

/**
 * Kết quả tính phí một kiện (API 6.8 khối shop).
 *
 * @param carrierFee         phí thật của hãng (VND) — sàn / shop trợ khi miễn phí (C3 ghi ký quỹ)
 * @param customerFee        phí khách trả: 0 khi shop miễn phí vận chuyển
 * @param freeByShop         {@code is_free_shipping_by_shop} — đạt ngưỡng freeship của shop hoặc mọi món người bán trả ship
 * @param estimated          {@code is_estimated_fee} — phí tạm tính theo bảng dự phòng (thiếu kho / thiếu mã GHN / GHN lỗi)
 * @param unavailable        GHN báo không phục vụ tuyến → {@code unavailable_reason = SHIPPING_UNAVAILABLE}
 * @param freeShipThreshold  ngưỡng freeship của shop (VND); 0 khi mọi món người bán trả ship; NULL = không có
 */
public record ShippingQuote(long carrierFee, long customerFee, boolean freeByShop, boolean estimated,
                            boolean unavailable, Long freeShipThreshold) {
}
