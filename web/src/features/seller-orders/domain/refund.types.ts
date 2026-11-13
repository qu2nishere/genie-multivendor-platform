// Yêu cầu trả hàng phía shop (API_SPEC 10.4; prototype Fulfillment.jsx RETURNS, R_STATE).
export type SellerRefundStatus =
  | 'REQUESTED'
  | 'AWAITING_EVIDENCE'
  | 'AWAITING_RETURN_SHIPMENT'
  | 'SHOP_REJECTED'
  | 'DISPUTED'
  | 'REFUNDED';

export type SellerRefundAction =
  | 'REQUEST_EVIDENCE'
  | 'REJECT'
  | 'APPROVE_WITH_RETURN'
  | 'APPROVE_REFUND_ONLY'
  | 'CONFIRM_RETURN_RECEIVED'
  | 'ESCALATE';

export interface SellerRefund {
  id: number;
  code: string;
  order: { id: number; code: string };
  customerName: string;
  items: { name: string; variant: string; quantity: number }[];
  type: 'RETURN_AND_REFUND' | 'REFUND_ONLY';
  reasonCode: string;
  evidence: { url: string; kind: 'IMAGE' | 'VIDEO'; name: string }[];
  amount: number;
  status: SellerRefundStatus;
  deadlineAt: string | null;
  /** Shop đã dùng lượt "Yêu cầu bổ sung" (chỉ 1 lần — 409 GNE-SUP-4091). */
  evidenceRequested: boolean;
}
