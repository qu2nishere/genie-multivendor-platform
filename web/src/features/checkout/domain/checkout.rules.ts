// Quy tắc trang thanh toán — TS thuần (plan G10.16, G10.19).
import { HOLD_MINUTES, type CheckoutSession, type PaymentMethod } from './checkout.types';

/** Hạn giữ chỗ tính từ lúc bắt đầu (ms epoch). */
export const holdDeadline = (startedAt: number): number => startedAt + HOLD_MINUTES * 60_000;

/** Số giây còn lại tới hạn (không âm). */
export const secondsLeft = (deadline: number, now: number = Date.now()): number =>
  Math.max(0, Math.ceil((deadline - now) / 1000));

/** Nhãn nút đặt hàng theo phương thức (prototype: "Đặt hàng & thanh toán VNPAY"…). */
export const placeLabelKey = (m: PaymentMethod): string =>
  m === 'COD'
    ? 'checkout.place.cod'
    : m === 'VIETQR'
      ? 'checkout.place.vietqr'
      : 'checkout.place.vnpay';

/** Phương thức giữ tiền qua Genie (VNPay, VietQR) — COD trả tiền cho người giao. */
export const isEscrowed = (m: PaymentMethod): boolean => m !== 'COD';

/** Lỗi 409 GNE-INV-4090 → mã dòng giỏ vừa hết hàng (`details.out_of_stock_cart_item_ids`). */
export function outOfStockIds(details: unknown): number[] {
  if (!details || typeof details !== 'object') return [];
  const ids = (details as { out_of_stock_cart_item_ids?: unknown }).out_of_stock_cart_item_ids;
  return Array.isArray(ids) ? ids.filter((x): x is number => Number.isInteger(x)) : [];
}

/** Kết quả hiển thị ở /checkout/result (chỉ đọc trạng thái, không đổi đơn — TS 10.3). */
export type ResultView = 'success' | 'pending' | 'failed';
export function resultView(s: Pick<CheckoutSession, 'status' | 'paymentMethod'>): ResultView {
  if (s.status === 'COMPLETED') return 'success';
  if (s.status === 'FAILED' || s.status === 'EXPIRED') return 'failed';
  return 'pending';
}

/** Mã đơn TR-yyyyMMdd-NNNNNN theo ngày giờ Việt Nam (R11). */
export function orderCode(date: Date, seq: number): string {
  const vn = new Date(date.getTime() + 7 * 3_600_000);
  const ymd = vn.toISOString().slice(0, 10).replace(/-/g, '');
  return `TR-${ymd}-${String(seq).padStart(6, '0')}`;
}
