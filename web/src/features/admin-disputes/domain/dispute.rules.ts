// Hàm thuần của màn phân xử (API_SPEC 11.4).
import type { Dispute, DisputeOutcome, DisputeStatus, GatewayRefund } from './dispute.types';

export const OUTCOMES: DisputeOutcome[] = [
  'REFUND_NOW',
  'RETURN_THEN_REFUND',
  'DENY',
  'CARRIER_FAULT',
];
export const COUNTED: DisputeStatus[] = ['OPEN', 'REFUNDED', 'DENIED'];

/** Trạng thái sau phán quyết (mock dùng để mô phỏng server). */
export function statusAfter(o: DisputeOutcome): DisputeStatus {
  if (o === 'RETURN_THEN_REFUND') return 'AWAITING_RETURN';
  if (o === 'DENY') return 'DENIED';
  return 'REFUNDED';
}

export type DisputeAction = 'resolve' | 'freeze' | 'unfreeze' | 'confirm_return' | 'reopen';

/**
 * Thao tác theo trạng thái: đang mở → phân xử + phong tỏa / gỡ; chờ hàng hoàn → "Hàng đã về kho shop";
 * đã từ chối → "Mở lại". Đã hoàn tiền thì không mở lại được (tiền đã rời escrow).
 */
export function disputeActions(d: Pick<Dispute, 'status' | 'escrow'>): DisputeAction[] {
  switch (d.status) {
    case 'OPEN':
      return ['resolve', d.escrow.status === 'FROZEN' ? 'unfreeze' : 'freeze'];
    case 'AWAITING_RETURN':
      return ['confirm_return'];
    case 'DENIED':
      return ['reopen'];
    default:
      return [];
  }
}

/** Hoàn qua cổng (VNPay) hay chuyển khoản tay (VietQR / COD). */
export const isManualRefund = (r: GatewayRefund): boolean => r.manualTransfer !== null;

/** Nội dung chuyển khoản hoàn tiền: RFN-0231 → TRRF0231. */
export const refundTransferContent = (code: string): string => `TRRF${code.replace(/\D/g, '')}`;

/** Thao tác trên khối hoàn tiền Gateway. */
export function refundActions(r: GatewayRefund): ('sync' | 'retry' | 'mark_transferred')[] {
  if (isManualRefund(r)) return r.status === 'SUCCESS' ? [] : ['mark_transferred'];
  if (r.status === 'FAILED') return ['retry'];
  return r.status === 'PENDING' ? ['sync'] : [];
}

export function countByStatus(rows: Pick<Dispute, 'status'>[]): Record<DisputeStatus, number> {
  const c: Record<DisputeStatus, number> = { OPEN: 0, AWAITING_RETURN: 0, REFUNDED: 0, DENIED: 0 };
  for (const r of rows) c[r.status] += 1;
  return c;
}
