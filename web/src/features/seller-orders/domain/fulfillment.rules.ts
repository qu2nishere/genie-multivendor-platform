// Xử lý đơn phía shop (API_SPEC 10.3; prototype Fulfillment.jsx): thao tác được phép theo trạng thái,
// lý do từ chối, chọn hãng vận chuyển, thu hộ COD.
import type { Carrier, PaymentMethod, SellerOrder } from './order.types';

export type OrderAction = 'CONFIRM' | 'REJECT' | 'PREPARE' | 'HAND_OVER' | 'PRINT_LABEL';

/** 10.3 `allowed_actions` — mock tự suy; API thật trả sẵn. */
export function allowedActions(o: Pick<SellerOrder, 'status' | 'shipment'>): OrderAction[] {
  if (o.status === 'NEW') return ['CONFIRM', 'REJECT'];
  if (o.status === 'CONFIRMED') return ['PREPARE'];
  if (o.status === 'PREPARING') return ['HAND_OVER'];
  if (o.shipment?.trackingCode && o.status === 'DELIVERING') return ['PRINT_LABEL'];
  return [];
}

/** Lý do từ chối (prototype REJECT_REASONS) — `OTHER` bắt buộc ghi chú. */
export const REJECT_REASONS = [
  'OUT_OF_STOCK',
  'WRONG_PRICE',
  'UNSERVICEABLE_AREA',
  'OTHER',
] as const;
export type RejectReason = (typeof REJECT_REASONS)[number];

export interface ShippingQuote {
  carrier: Carrier;
  fee: number;
  estimatedDays: number;
  selected: boolean;
}

/** Hãng được chọn: hệ thống đánh dấu sẵn; không có thì lấy phí thấp nhất (LLD 9.5). */
export function pickQuote(quotes: ShippingQuote[]): ShippingQuote | null {
  if (quotes.length === 0) return null;
  return quotes.find((q) => q.selected) ?? quotes.reduce((a, b) => (b.fee < a.fee ? b : a));
}

export const CARRIER_NAME: Record<Carrier, string> = {
  GHN: 'GHN',
  GHTK: 'GHTK',
  VIETTEL_POST: 'ViettelPost',
  JNT_EXPRESS: 'J&T Express',
  GENIE_EXPRESS: 'Genie Express',
};

export const PAYMENT_NAME: Record<PaymentMethod, string> = {
  VNPAY: 'VNPay',
  VIETQR: 'VietQR',
  COD: 'COD',
};

/** Số tiền hãng thu hộ khi giao (0 nếu khách đã trả online). */
export const codAmount = (o: Pick<SellerOrder, 'payment' | 'total'>): number =>
  o.payment === 'COD' ? o.total : 0;
