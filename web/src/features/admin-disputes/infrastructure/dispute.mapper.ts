import type { Dispute, GatewayRefund } from '../domain/dispute.types';
import type { DisputeDto, RefundDto } from './dispute.dto';

export const toRefund = (r: RefundDto): GatewayRefund => ({
  id: r.id,
  gateway: r.gateway,
  originalTransactionId: r.original_transaction_id,
  gatewayRefundId: r.gateway_refund_id,
  amount: r.amount,
  status: r.status,
  createdAt: r.created_at,
  manualTransfer: r.manual_transfer
    ? {
        toAccount: r.manual_transfer.to_account,
        transferContent: r.manual_transfer.transfer_content,
      }
    : null,
});

export const toDispute = (d: DisputeDto): Dispute => ({
  id: d.id,
  code: d.code,
  order: { id: d.order.id, code: d.order.code, paymentMethod: d.order.payment_method },
  customer: d.customer,
  shop: d.shop,
  reasonCode: d.reason_code,
  amount: d.amount,
  status: d.status,
  openedAt: d.opened_at,
  customerEvidence: d.customer_evidence,
  shopEvidence: d.shop_evidence,
  escrow: d.escrow,
  refund: d.refund ? toRefund(d.refund) : null,
});
