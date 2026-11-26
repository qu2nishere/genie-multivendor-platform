package com.genie.shipping.fee.application;

/**
 * Một kiện hàng của một shop cần tính phí (UC-ORD-06; plan C2 3.2, O11.01). Địa giới 2 cấp (sau 01/07/2025): GHN lấy
 * kho gửi theo mã shop GHN ({@code ghnShopId} — header {@code ShopId}), điểm nhận theo mã phường mới của sổ địa chỉ
 * (master-data v3) — không cần mã quận ở đầu nào (spike D4, progress C2 mục 7).
 *
 * @param vendorUserId   {@code users.id} của chủ shop (đọc ngưỡng freeship)
 * @param fromProvinceId tỉnh kho lấy hàng; NULL = shop chưa cấu hình kho (D5) — chỉ dùng chọn bảng phí dự phòng
 * @param ghnShopId      mã shop GHN; NULL = chưa đăng ký GHN → bảng phí dự phòng
 * @param toProvinceId   tỉnh người nhận (mã v3) — chọn bảng phí dự phòng
 * @param toWardCode     mã phường người nhận (mã v3, dạng chuỗi) — gửi GHN với {@code is_new_to_address: true}
 * @param toName         tên người nhận — GHN đòi khi xem trước đơn
 * @param toPhone        SĐT người nhận
 * @param toAddress      địa chỉ đầy đủ người nhận (số nhà, phường, tỉnh)
 * @param weightGrams    tổng khối lượng (gram)
 * @param lengthCm       kích thước lớn nhất của các món; NULL = bỏ qua
 * @param subtotal       tạm tính tiền hàng của shop (VND) — so ngưỡng freeship, khai giá bảo hiểm
 * @param allSellerPays  mọi sản phẩm của kiện có {@code shipping_payer = SELLER}
 */
public record Parcel(long vendorUserId, Integer fromProvinceId, Integer ghnShopId, int toProvinceId, String toWardCode,
                     String toName, String toPhone, String toAddress, int weightGrams, Integer lengthCm,
                     Integer widthCm, Integer heightCm, long subtotal, boolean allSellerPays) {
}
