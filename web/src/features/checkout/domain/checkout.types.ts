// Miền thanh toán (plan G10.16; API_SPEC 6.10–6.13; prototype Checkout.jsx PAYMENTS, OrderDone).

/** Phương thức thanh toán trong phạm vi (TS 3.5): VNPay, VietQR (SePay xác nhận), COD. */
export type PaymentMethod = 'VNPAY' | 'VIETQR' | 'COD';
export const PAYMENT_METHODS: PaymentMethod[] = ['VNPAY', 'VIETQR', 'COD'];

/** Giữ chỗ tồn kho 15 phút sau khi đặt (soft-lock). */
export const HOLD_MINUTES = 15;
/** Lời nhắn cho shop tối đa 500 ký tự. */
export const NOTE_MAX = 500;

export type CheckoutStatus = 'INITIATED' | 'PAYMENT_PENDING' | 'COMPLETED' | 'FAILED' | 'EXPIRED';

export interface VietQrInfo {
  bankName: string;
  accountNumber: string;
  accountName: string;
  amount: number;
  /** Nội dung chuyển khoản `TR{mã}` để SePay đối soát. */
  transferContent: string;
  qrPayload: string;
  expiresAt: string;
}

export type PaymentInstruction =
  | { method: 'VNPAY'; paymentUrl: string }
  | { method: 'VIETQR'; vietqr: VietQrInfo }
  | { method: 'COD' };

export interface CheckoutOrderRef {
  id: number;
  /** Mã đơn TR-yyyyMMdd-NNNNNN (R11). */
  code: string;
  shop: { id: number; name: string };
  grandTotal: number;
}

/** Phiên đặt hàng = một lần bấm "Đặt hàng", tách N đơn con theo shop ($CheckoutSession). */
export interface CheckoutSession {
  id: number;
  code: string;
  status: CheckoutStatus;
  paymentMethod: PaymentMethod;
  createdAt: string;
  reservationExpiresAt: string;
  orders: CheckoutOrderRef[];
  summary: {
    subtotal: number;
    shippingTotal: number;
    discountTotal: number;
    coinDiscount: number;
    grandTotal: number;
  };
  payment: PaymentInstruction | null;
}

/** Yêu cầu đặt hàng (6.10). Server tính lại giá — client không gửi tiền. */
export interface PlaceOrderInput {
  cartItemIds: number[];
  addressId: number;
  vouchers: {
    platformId: string | null;
    freeshipId: string | null;
    shop: { shopId: number; voucherId: string }[];
  };
  useCoins: boolean;
  notes: { shopId: number; note: string }[];
  paymentMethod: PaymentMethod;
  returnUrl: string;
}

/**
 * Bảng giá phía client đã hiển thị (giỏ + trang thanh toán). Khi chạy mock không có engine giá phía
 * server nên lớp mock dựng đơn từ bảng này; API thật bỏ qua (server tự tính lại — 6.8 / 6.10).
 */
export interface CheckoutQuote {
  orders: {
    shop: { id: number; name: string };
    items: {
      cartItemId: number;
      productId: number;
      variantId: number;
      productSlug: string;
      title: string;
      image: string;
      variantLabel: string;
      price: number;
      qty: number;
    }[];
    subtotal: number;
    shipping: number;
    discount: number;
    total: number;
  }[];
  summary: CheckoutSession['summary'];
  address: { recipient: string; phone: string; full: string };
}
