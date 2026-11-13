// Quy tắc trả hàng — TS thuần (plan G10.39).
import type { RefundStatus } from './refund.types';

/** Nhãn + tông của trạng thái yêu cầu (prototype REFUND_STATE). */
export const REFUND_TONE: Record<string, 'warning' | 'primary' | 'danger' | 'success' | 'neutral'> =
  {
    REQUESTED: 'warning',
    AWAITING_EVIDENCE: 'primary',
    SHOP_REJECTED: 'danger',
    AWAITING_RETURN_SHIPMENT: 'primary',
    DISPUTED: 'danger',
    REFUNDED: 'success',
    REJECTED: 'neutral',
  };

export const isOpenRefund = (s: RefundStatus | string): boolean =>
  !['REFUNDED', 'REJECTED'].includes(s);

/** Đơn VNPAY hoàn về thẻ; VietQR / COD hoàn bằng chuyển khoản → cần tài khoản nhận (7.5). */
export const needsBank = (method: string): boolean => method !== 'VNPAY';

/** Bằng chứng tối thiểu: ≥ 1 ảnh và ≥ 1 video (GNE-SUP-4221). */
export function evidenceMissing(ev: { mediaType: string }[]): ('IMAGE' | 'VIDEO')[] {
  const miss: ('IMAGE' | 'VIDEO')[] = [];
  if (!ev.some((e) => e.mediaType === 'IMAGE')) miss.push('IMAGE');
  if (!ev.some((e) => e.mediaType === 'VIDEO')) miss.push('VIDEO');
  return miss;
}

/**
 * Số tiền hoàn dự kiến: theo đơn giá đã trả (paid_unit_price) nếu có, nếu không chia đều giảm giá của
 * đơn theo giá trị (prototype paidRatio).
 */
export function refundEstimate(
  lines: { price: number; paidUnitPrice: number | null; qty: number }[],
  order: { subtotal: number; discountTotal: number },
): number {
  const ratio = order.subtotal ? 1 - order.discountTotal / order.subtotal : 1;
  return Math.round(lines.reduce((n, l) => n + (l.paidUnitPrice ?? l.price * ratio) * l.qty, 0));
}

/** Che số tài khoản: "Vietcombank ••6712". */
export const maskBank = (bankName: string, account: string): string =>
  `${bankName} ••${account.slice(-4)}`;
