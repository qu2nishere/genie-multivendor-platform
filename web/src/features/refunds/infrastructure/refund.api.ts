// Trả hàng / hoàn tiền (API_SPEC 7.5–7.8) + danh sách ngân hàng (10.11). Mock ghi vào kho "hoạt động"
// dùng chung để trang đơn đọc lại trạng thái yêu cầu.
import { api } from '@/shared/api/http';
import { newIdempotencyKey } from '@/shared/api/idempotency';
import { mockDelay, mockFail, USE_MOCK } from '@/shared/api/mock';
import { activityStore, type MockRefund } from '@/shared/api/mock/activity';
import { evidenceMissing, maskBank } from '../domain/refund.rules';
import type { Bank, EvidenceRef, RefundInput, RefundRequest } from '../domain/refund.types';

/** Khối refund_request (7.2) — snake_case. */
export interface RefundDto {
  id: number;
  code: string;
  order_id?: number;
  status: string;
  request_type: 'RETURN_AND_REFUND' | 'REFUND_ONLY';
  reason_code: string;
  refund_amount: number;
  shop_message: string | null;
  deadline_at: string | null;
  refund_bank_masked: string | null;
  resolved_at: string | null;
}

const toRefund = (d: RefundDto, orderId: number): RefundRequest => ({
  id: d.id,
  code: d.code,
  orderId: d.order_id ?? orderId,
  status: d.status,
  requestType: d.request_type,
  reasonCode: d.reason_code,
  refundAmount: d.refund_amount,
  shopMessage: d.shop_message,
  deadlineAt: d.deadline_at,
  refundBankMasked: d.refund_bank_masked,
  resolvedAt: d.resolved_at,
});

const BANKS: Bank[] = [
  { code: 'VCB', name: 'Vietcombank' },
  { code: 'TCB', name: 'Techcombank' },
  { code: 'BIDV', name: 'BIDV' },
  { code: 'CTG', name: 'VietinBank' },
  { code: 'MB', name: 'MB Bank' },
  { code: 'ACB', name: 'ACB' },
];
const H48 = 48 * 3_600_000;

/** Mock: ghi đè / thêm một yêu cầu (yêu cầu mẫu của đơn cũ chưa có trong kho thì lấy từ dữ liệu UI). */
function upsert(base: RefundRequest, patch: Partial<MockRefund>): RefundRequest {
  const db = activityStore.get();
  const cur: MockRefund = db.refunds.find((r) => r.id === base.id) ?? {
    id: base.id,
    code: base.code,
    order_id: base.orderId,
    status: base.status,
    request_type: base.requestType,
    reason_code: base.reasonCode,
    refund_amount: base.refundAmount,
    shop_message: base.shopMessage,
    deadline_at: base.deadlineAt,
    refund_bank_masked: base.refundBankMasked,
    resolved_at: base.resolvedAt,
    evidence_count: 2,
  };
  const next = { ...cur, ...patch };
  activityStore.set({ ...db, refunds: [...db.refunds.filter((r) => r.id !== next.id), next] });
  return toRefund(next, next.order_id);
}

export async function listBanks(): Promise<Bank[]> {
  if (USE_MOCK) return BANKS;
  return api.get<Bank[]>('/public/banks', { auth: false });
}

/** POST /me/refund-requests 🔁 — thiếu ảnh / video → 422 GNE-SUP-4221; quá hạn / đang mở → 409 GNE-SUP-4092. */
export async function createRefund(input: RefundInput, amount: number): Promise<RefundRequest> {
  if (USE_MOCK) {
    await mockDelay(500);
    if (evidenceMissing(input.evidence).length) mockFail('GNE-SUP-4221');
    const db = activityStore.get();
    if (
      db.refunds.some(
        (r) => r.order_id === input.orderId && !['REJECTED', 'REFUNDED'].includes(r.status),
      )
    )
      mockFail('GNE-SUP-4092');
    const id = 300 + db.refunds.length + 1;
    const bankName = BANKS.find((b) => b.code === input.bank?.code)?.name ?? '';
    const row: MockRefund = {
      id,
      code: `RFN-0${id}`,
      order_id: input.orderId,
      status: 'REQUESTED',
      request_type: input.requestType,
      reason_code: input.reasonCode,
      refund_amount: amount,
      shop_message: null,
      deadline_at: new Date(Date.now() + H48).toISOString(),
      refund_bank_masked: input.bank ? maskBank(bankName, input.bank.accountNumber) : null,
      resolved_at: null,
      evidence_count: input.evidence.length,
    };
    activityStore.set({ ...db, refunds: [...db.refunds, row] });
    return toRefund(row, input.orderId);
  }
  const d = await api.post<RefundDto>(
    '/me/refund-requests',
    {
      order_id: input.orderId,
      request_type: input.requestType,
      reason_code: input.reasonCode,
      items: input.items.map((i) => ({ order_item_id: i.orderItemId, quantity: i.qty })),
      evidence: input.evidence.map((e) => ({ object_key: e.objectKey, media_type: e.mediaType })),
      refund_bank: input.bank && {
        bank_code: input.bank.code,
        account_number: input.bank.accountNumber,
        account_holder: input.bank.holder,
      },
    },
    { idempotencyKey: newIdempotencyKey() },
  );
  return toRefund(d, input.orderId);
}

/** POST /me/refund-requests/{id}/evidence — bổ sung bằng chứng, trạng thái về REQUESTED. */
export async function addEvidence(
  r: RefundRequest,
  evidence: EvidenceRef[],
): Promise<RefundRequest> {
  if (USE_MOCK) {
    await mockDelay(300);
    return upsert(r, {
      status: 'REQUESTED',
      deadline_at: new Date(Date.now() + H48).toISOString(),
    });
  }
  const d = await api.post<RefundDto>(`/me/refund-requests/${r.id}/evidence`, {
    evidence: evidence.map((e) => ({ object_key: e.objectKey, media_type: e.mediaType })),
  });
  return toRefund(d, r.orderId);
}

/** POST /me/refund-requests/{id}/escalate — khiếu nại lên Sàn. */
export async function escalateRefund(r: RefundRequest): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(300);
    upsert(r, { status: 'DISPUTED' });
    return;
  }
  await api.post(`/me/refund-requests/${r.id}/escalate`);
}

/** POST /me/refund-requests/{id}/accept-rejection — chấp nhận shop từ chối, đơn về COMPLETED. */
export async function acceptRejection(r: RefundRequest): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(300);
    upsert(r, { status: 'REJECTED', resolved_at: new Date().toISOString() });
    return;
  }
  await api.post(`/me/refund-requests/${r.id}/accept-rejection`);
}
