package com.genie.shipping.fee.application;

/**
 * Cổng gọi API phí GHN {@code POST /v2/shipping-order/fee} (UC-ORD-06; plan C2 D6). Chỉ có cài đặt khi
 * {@code GHN_MODE=http} ({@code HttpGhnFeeClient}); không có bean → {@link ShippingFeeQuoter} dùng bảng phí giả lập.
 */
public interface GhnFeeClient {

    /**
     * Phí gói "Chuẩn" (VND). GHN báo không phục vụ tuyến → {@link RouteNotSupportedException}; lỗi mạng / timeout / phản hồi
     * lạ → {@link RuntimeException} khác (quoter chuyển sang bảng dự phòng).
     */
    long fee(Parcel parcel);

    /** GHN không giao được tuyến này. */
    class RouteNotSupportedException extends RuntimeException {
        public RouteNotSupportedException(String message) {
            super(message);
        }
    }
}
