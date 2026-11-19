package com.genie.auth.address.application;

import com.genie.auth.address.domain.AddressLabel;

/**
 * {@code $AddressInput} đã kiểm định dạng ở controller (API_SPEC 3.8). Địa giới 2 cấp (plan AUTH D25): tỉnh + phường,
 * không còn quận. Tên hành chính do server tra theo mã.
 */
public record AddressInput(String recipientName, String phoneNumber, int provinceId, String wardCode,
                           String addressLine, AddressLabel label, boolean isDefault, String deliveryNote) {

    /** Ghi chú cho người giao hàng đã cắt khoảng trắng; rỗng → {@code null} (plan AUTH D27). */
    public String cleanDeliveryNote() {
        if (deliveryNote == null) {
            return null;
        }
        String s = deliveryNote.strip().replaceAll("\\s+", " ");
        return s.isEmpty() ? null : s;
    }
}
