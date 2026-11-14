// Trả hàng & Hoàn tiền — phân xử (API_SPEC 11.4; prototype admin/Disputes.jsx Refunds, GatewayCard).
export type DisputeStatus = 'OPEN' | 'AWAITING_RETURN' | 'REFUNDED' | 'DENIED';
export type DisputeOutcome = 'REFUND_NOW' | 'RETURN_THEN_REFUND' | 'DENY' | 'CARRIER_FAULT';
export type EscrowStatus = 'HELD' | 'DISPUTED' | 'FROZEN' | 'RELEASED' | 'REFUNDED';
export type RefundStatus = 'PENDING' | 'SUCCESS' | 'FAILED';
export type PaymentMethod = 'VNPAY' | 'VIETQR' | 'COD';

export interface Media {
  type: 'IMAGE' | 'VIDEO';
  url: string;
}

export interface GatewayRefund {
  id: number;
  gateway: 'VNPAY' | null;
  originalTransactionId: string | null;
  gatewayRefundId: string | null;
  amount: number;
  status: RefundStatus;
  createdAt: string;
  /** Đơn VietQR / COD: chuyển khoản tay tới tài khoản khách, nội dung TRRF… */
  manualTransfer: { toAccount: string; transferContent: string } | null;
}

export interface Dispute {
  id: number;
  code: string;
  order: { id: number; code: string; paymentMethod: PaymentMethod };
  customer: { id: number; name: string };
  shop: { id: number; name: string };
  reasonCode: string;
  amount: number;
  status: DisputeStatus;
  openedAt: string;
  customerEvidence: Media[];
  shopEvidence: Media[];
  escrow: { id: number; status: EscrowStatus };
  refund: GatewayRefund | null;
}

export type DisputeCounts = Record<DisputeStatus, number>;
