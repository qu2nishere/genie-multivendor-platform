// Nháp checkout (LUU_TRU_DU_LIEU 6.2 `genie.checkout.draft`, sessionStorage): CHỈ mã — không giá,
// không địa chỉ. Giỏ ghi khi bấm "Mua hàng"; trang Thanh toán đọc lại, tải lại trang không mất.
export interface CheckoutDraft {
  /** Dòng giỏ được chọn để đặt (cart_item_id). */
  itemIds: number[];
  /** Lời nhắn cho từng shop (shop_id → nội dung). */
  notes: Record<number, string>;
  /** Mã voucher đã chọn (sàn, freeship, voucher shop) — shop của voucher suy từ kho voucher. */
  voucherCodes: string[];
  usePoints: boolean;
}

export const emptyDraft = (): CheckoutDraft => ({
  itemIds: [],
  notes: {},
  voucherCodes: [],
  usePoints: false,
});
