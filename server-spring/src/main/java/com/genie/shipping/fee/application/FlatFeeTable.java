package com.genie.shipping.fee.application;

/**
 * Bảng phí dự phòng khi không hỏi được GHN (plan C2 D4, D5, D6): cùng tỉnh 15.000đ, khác tỉnh / chưa biết kho 30.000đ.
 * Cũng là phí "giả lập" ở {@code GHN_MODE=stub}.
 */
public final class FlatFeeTable {

    public static final long SAME_PROVINCE = 15_000L;
    public static final long OTHER_PROVINCE = 30_000L;

    private FlatFeeTable() {
    }

    /** {@code fromProvinceId} NULL (shop chưa có kho) → coi như khác tỉnh. */
    public static long fee(Integer fromProvinceId, int toProvinceId) {
        return fromProvinceId != null && fromProvinceId == toProvinceId ? SAME_PROVINCE : OTHER_PROVINCE;
    }
}
