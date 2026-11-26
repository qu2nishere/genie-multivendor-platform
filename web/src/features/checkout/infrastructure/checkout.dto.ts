// $CheckoutSession + khối payment (API_SPEC 6.10–6.12) — snake_case.
export type PaymentDto =
  /** payment_url = null khi chưa có cổng thanh toán thật (plan C2 D3 — cổng giả lập). */
  | { method: 'VNPAY'; payment_url: string | null }
  | {
      method: 'VIETQR';
      vietqr: {
        bank_name: string;
        account_number: string;
        account_name: string;
        amount: number;
        transfer_content: string;
        qr_payload: string;
        expires_at: string;
      } | null;
    }
  | { method: 'COD' };

export interface CheckoutSessionDto {
  id: number;
  code: string;
  status: 'INITIATED' | 'PAYMENT_PENDING' | 'COMPLETED' | 'FAILED' | 'EXPIRED';
  payment_method: 'VNPAY' | 'VIETQR' | 'COD';
  created_at: string;
  reservation_expires_at: string;
  orders: { id: number; code: string; shop: { id: number; name: string }; grand_total: number }[];
  summary: {
    subtotal: number;
    shipping_total: number;
    discount_total: number;
    coin_discount: number;
    grand_total: number;
  };
  payment?: PaymentDto;
}

export interface PlaceOrderDto {
  cart_item_ids: number[];
  address_id: number;
  /** null khi backend chưa áp voucher (plan C2 D2) — mã voucher của mock là chuỗi, API thật nhận id số. */
  vouchers: {
    platform_voucher_id: string | null;
    freeship_voucher_id: string | null;
    shop_vouchers: { shop_id: number; voucher_id: string }[];
  } | null;
  use_coins: boolean;
  notes: { shop_id: number; note: string }[];
  payment_method: 'VNPAY' | 'VIETQR' | 'COD';
  return_url: string;
}
