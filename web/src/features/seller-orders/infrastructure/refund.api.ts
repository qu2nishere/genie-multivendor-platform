// Trả hàng phía shop (API_SPEC 10.4). Mock ghi kho dùng chung (Tổng quan đếm yêu cầu đang chờ).
import { api } from '@/shared/api/http';
import { mockDelay, mockFail, USE_MOCK } from '@/shared/api/mock';
import { sellerRefundsStore, type MockSellerRefund } from '@/shared/api/mock/seller';
import { NEXT_STATUS, refundActions } from '../domain/refund.rules';
import type { SellerRefund, SellerRefundAction } from '../domain/refund.types';

type RefundDto = MockSellerRefund;

const toRefund = (d: RefundDto): SellerRefund => ({
  id: d.id,
  code: d.code,
  order: d.order,
  customerName: d.customer_name,
  items: d.items.map((i) => ({
    name: i.product_name,
    variant: i.variant_name,
    quantity: i.quantity,
  })),
  type: d.request_type,
  reasonCode: d.reason_code,
  evidence: d.evidence.map((e) => ({ url: e.url, kind: e.media_type, name: e.name })),
  amount: d.refund_amount,
  status: d.status,
  deadlineAt: d.deadline_at,
  evidenceRequested: d.evidence_requested,
});

export async function listSellerRefunds(): Promise<SellerRefund[]> {
  if (USE_MOCK) {
    await mockDelay(200);
    return sellerRefundsStore.get().map(toRefund);
  }
  return (await api.get<RefundDto[]>('/seller/refund-requests', { query: { size: 50 } })).map(
    toRefund,
  );
}

const PATH: Record<SellerRefundAction, string> = {
  REQUEST_EVIDENCE: 'request-evidence',
  REJECT: 'reject',
  APPROVE_WITH_RETURN: 'approve',
  APPROVE_REFUND_ONLY: 'approve',
  CONFIRM_RETURN_RECEIVED: 'confirm-return-received',
  ESCALATE: 'escalate',
};

export async function actOnRefund(
  id: number,
  action: SellerRefundAction,
  text?: string,
): Promise<SellerRefund> {
  if (USE_MOCK) {
    await mockDelay(300);
    const all = sellerRefundsStore.get();
    const cur = all.find((r) => r.id === id);
    if (!cur) mockFail('GNE-ORD-4041');
    if (action === 'REQUEST_EVIDENCE' && cur.evidence_requested) mockFail('GNE-SUP-4091');
    if (!refundActions(toRefund(cur)).includes(action)) mockFail('GNE-ORD-4092');
    const next: RefundDto = {
      ...cur,
      status: NEXT_STATUS[action],
      evidence_requested: cur.evidence_requested || action === 'REQUEST_EVIDENCE',
      // Đồng ý nhận lại hàng: khách có 7 ngày gửi hàng; các trạng thái khác hết hạn của shop
      deadline_at: null,
    };
    sellerRefundsStore.set(all.map((r) => (r.id === id ? next : r)));
    return toRefund(next);
  }
  const body =
    action === 'REQUEST_EVIDENCE'
      ? { message: text }
      : action === 'REJECT'
        ? { reason: text }
        : action === 'APPROVE_WITH_RETURN' || action === 'APPROVE_REFUND_ONLY'
          ? { resolution: action }
          : undefined;
  return toRefund(await api.post<RefundDto>(`/seller/refund-requests/${id}/${PATH[action]}`, body));
}
