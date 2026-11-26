package com.genie.shipping.fee.application;

/**
 * Một kiện hàng của một shop cần tính phí (UC-ORD-06; plan C2 3.2). Kho lấy hàng lấy từ {@code vendor_profiles.pickup_*}
 * (2 cấp — {@code fromDistrictId} chỉ còn ở kho cũ), điểm nhận từ sổ địa chỉ (2 cấp — {@code toDistrictId} thường NULL).
 *
 * @param vendorUserId     {@code users.id} của chủ shop (đọc ngưỡng freeship)
 * @param fromProvinceId   NULL = shop chưa cấu hình kho lấy hàng (D5)
 * @param ghnShopId        mã shop GHN (header {@code ShopId}); NULL = chưa đăng ký GHN
 * @param weightGrams      tổng khối lượng (gram)
 * @param lengthCm         kích thước lớn nhất của các món; NULL = bỏ qua
 * @param subtotal         tạm tính tiền hàng của shop (VND) — so ngưỡng freeship, khai giá bảo hiểm
 * @param allSellerPays    mọi sản phẩm của kiện có {@code shipping_payer = SELLER}
 */
public record Parcel(long vendorUserId, Integer fromProvinceId, Integer fromDistrictId, String fromWardCode,
                     Integer ghnShopId, int toProvinceId, Integer toDistrictId, String toWardCode, int weightGrams,
                     Integer lengthCm, Integer widthCm, Integer heightCm, long subtotal, boolean allSellerPays) {
}
