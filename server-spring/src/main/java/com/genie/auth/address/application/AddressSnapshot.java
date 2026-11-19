package com.genie.auth.address.application;

/**
 * Địa chỉ giao hàng đầy đủ tên + mã (địa giới 2 cấp, plan AUTH D25) — module {@code order} chụp lại khi tạo phiên
 * checkout (plan AUTH A12.04), để đơn không đổi khi khách sửa / xóa địa chỉ sau này. Tên tỉnh / phường dùng làm
 * {@code to_province_name} / {@code to_ward_name} khi tạo đơn GHN ({@code is_new_to_address: true});
 * {@code deliveryNote} (NULLABLE, plan AUTH D27) gửi vào trường {@code note} của vận đơn để tài xế đọc.
 */
public record AddressSnapshot(long addressId, String recipientName, String phoneNumber, String addressLine,
                              String wardCode, String ward, int provinceId, String province, String deliveryNote) {

    /** "25 Nguyễn Huệ, Phường Sài Gòn, Thành phố Hồ Chí Minh" — {@code $AddressSnapshot.full_address}. */
    public String fullAddress() {
        return String.join(", ", addressLine, ward, province);
    }
}
