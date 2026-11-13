// Trả hàng / hoàn tiền (plan G10.39; API_SPEC 7.5–7.8, 16.4; prototype OrderTracking.jsx REFUND_STATE,
// ReturnModal).

export type RefundType = 'RETURN_AND_REFUND' | 'REFUND_ONLY';

export type RefundStatus =
  | 'REQUESTED'
  | 'AWAITING_EVIDENCE'
  | 'SHOP_REJECTED'
  | 'AWAITING_RETURN_SHIPMENT'
  | 'DISPUTED'
  | 'REFUNDED'
  | 'REJECTED';

/** Lý do trả hàng (16.4). */
export type ReturnReason =
  | 'DAMAGED_IN_TRANSIT'
  | 'WRONG_SIZE_COLOR'
  | 'COUNTERFEIT'
  | 'MISSING_ACCESSORIES'
  | 'NOT_RECEIVED'
  | 'NOT_AS_DESCRIBED'
  | 'DEFECTIVE';

export const RETURN_REASONS: ReturnReason[] = [
  'DAMAGED_IN_TRANSIT',
  'WRONG_SIZE_COLOR',
  'COUNTERFEIT',
  'MISSING_ACCESSORIES',
  'NOT_RECEIVED',
  'NOT_AS_DESCRIBED',
  'DEFECTIVE',
];

/** Hạn yêu cầu trả hàng: 7 ngày kể từ khi giao. */
export const RETURN_WINDOW_DAYS = 7;

export interface RefundRequest {
  id: number;
  code: string;
  orderId: number;
  status: RefundStatus | string;
  requestType: RefundType;
  reasonCode: string;
  refundAmount: number;
  shopMessage: string | null;
  deadlineAt: string | null;
  refundBankMasked: string | null;
  resolvedAt: string | null;
}

export interface Bank {
  code: string;
  name: string;
}

export interface EvidenceRef {
  objectKey: string;
  mediaType: 'IMAGE' | 'VIDEO';
}

export interface RefundInput {
  orderId: number;
  requestType: RefundType;
  reasonCode: ReturnReason;
  items: { orderItemId: number; qty: number }[];
  evidence: EvidenceRef[];
  bank: { code: string; accountNumber: string; holder: string } | null;
}
