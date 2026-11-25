package com.genie.vendor.settings.application;

/**
 * Cổng đăng ký / đồng bộ cửa hàng (kho lấy hàng) của shop sang GHN — UC-VND-05 bước 6–7 (plan Mở shop & KYC D12, D16,
 * K07.02). Cài đặt chọn theo {@code genie.shipping.ghn.pickup-mode} ({@code GHN_PICKUP_MODE}): {@code stub} (mặc định —
 * {@code infra.StubPickupShopRegistrar}) hoặc {@code http} ({@code infra.GhnPickupShopRegistrar}). Chuyển về adapter
 * của module {@code shipping} khi module đó được hiện thực.
 */
public interface PickupShopRegistrar {

    /**
     * Đăng ký (lần đầu) hoặc cập nhật kho lấy hàng trên GHN và trả GHN Shop ID để lưu.
     *
     * @param currentGhnShopId Shop ID GHN đang lưu; {@code null} khi shop chưa từng đồng bộ
     * @throws PickupSyncException GHN lỗi / hết thời gian chờ — người gọi không được lưu dữ liệu mới
     */
    int sync(Integer currentGhnShopId, PickupInfo info);

    /** Thông tin kho gửi GHN — tên tỉnh / phường đã được server tra từ danh mục hành chính (không lấy từ client). */
    record PickupInfo(long vendorProfileId, String shopName, String phone, int provinceId, String provinceName,
                      String wardCode, String wardName, String addressLine) {

        /** Địa chỉ đầy đủ một dòng: "địa chỉ chi tiết, phường, tỉnh". */
        public String fullAddress() {
            return addressLine + ", " + wardName + ", " + provinceName;
        }
    }

    /** GHN không đồng bộ được kho lấy hàng → API trả {@code 502 GNE-LOG-5022}. */
    class PickupSyncException extends RuntimeException {

        public PickupSyncException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
