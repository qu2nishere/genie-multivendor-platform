// Quy tắc trả hàng phía shop: nút theo trạng thái, màu huy hiệu, hạn phản hồi 48 giờ.
import type { SellerRefund, SellerRefundAction, SellerRefundStatus } from './refund.types';

export function refundActions(
  r: Pick<SellerRefund, 'status' | 'type' | 'evidenceRequested'>,
): SellerRefundAction[] {
  if (r.status === 'REQUESTED') {
    const a: SellerRefundAction[] = [];
    if (!r.evidenceRequested) a.push('REQUEST_EVIDENCE');
    a.push('REJECT');
    if (r.type === 'RETURN_AND_REFUND') a.push('APPROVE_WITH_RETURN');
    a.push('APPROVE_REFUND_ONLY');
    return a;
  }
  if (r.status === 'AWAITING_RETURN_SHIPMENT') return ['CONFIRM_RETURN_RECEIVED'];
  if (r.status === 'SHOP_REJECTED') return ['ESCALATE'];
  return [];
}

/** Biến thể Badge (prototype R_STATE). */
export const REFUND_TONE: Record<
  SellerRefundStatus,
  'warning' | 'neutral' | 'primary' | 'success' | 'danger'
> = {
  REQUESTED: 'warning',
  AWAITING_EVIDENCE: 'neutral',
  AWAITING_RETURN_SHIPMENT: 'primary',
  REFUNDED: 'success',
  SHOP_REJECTED: 'danger',
  DISPUTED: 'danger',
};

/** Trạng thái sau mỗi thao tác (mock; API trả bản ghi đã cập nhật). */
export const NEXT_STATUS: Record<SellerRefundAction, SellerRefundStatus> = {
  REQUEST_EVIDENCE: 'AWAITING_EVIDENCE',
  REJECT: 'SHOP_REJECTED',
  APPROVE_WITH_RETURN: 'AWAITING_RETURN_SHIPMENT',
  APPROVE_REFUND_ONLY: 'REFUNDED',
  CONFIRM_RETURN_RECEIVED: 'REFUNDED',
  ESCALATE: 'DISPUTED',
};

/** Thao tác cần lời nhắn (bổ sung bằng chứng / lý do từ chối). */
export const needsText = (a: SellerRefundAction): boolean =>
  a === 'REQUEST_EVIDENCE' || a === 'REJECT';

/** Giờ còn lại tới hạn (làm tròn lên); null khi không có hạn. */
export function refundHoursLeft(deadlineAt: string | null, now: number): number | null {
  if (!deadlineAt) return null;
  return Math.max(0, Math.ceil((Date.parse(deadlineAt) - now) / 3_600_000));
}
