// Phân xử trả hàng / hoàn tiền (API_SPEC 11.4). Mock ghi kho dùng chung adminDisputesStore (huy hiệu
// nav, "Tiền chờ hoàn cho khách" ở Tổng quan đổi theo) và mô phỏng phía server: tạo lệnh hoàn VNPay
// (đồng bộ → SUCCESS) hoặc chuyển khoản tay TRRF… cho đơn VietQR / COD.
import { api, getPage } from '@/shared/api/http';
import { mockDelay, mockFail, USE_MOCK } from '@/shared/api/mock';
import {
  adminDisputesStore,
  type MockAdminDispute,
  type MockAdminRefund,
} from '@/shared/api/mock/admin';
import { countByStatus, refundTransferContent, statusAfter } from '../domain/dispute.rules';
import type {
  Dispute,
  DisputeCounts,
  DisputeOutcome,
  GatewayRefund,
} from '../domain/dispute.types';
import type { DisputeDto, RefundDto } from './dispute.dto';
import { toDispute, toRefund } from './dispute.mapper';

export interface DisputeList {
  items: Dispute[];
  counts: DisputeCounts;
}

export async function listDisputes(): Promise<DisputeList> {
  if (USE_MOCK) {
    await mockDelay(200);
    const rows = adminDisputesStore.get();
    return { items: rows.map(toDispute), counts: countByStatus(rows) };
  }
  const res = await getPage<DisputeDto, Dispute>('/admin/disputes', toDispute, {
    query: { size: 50 },
  });
  const c = (res.summary.counts ?? {}) as Partial<DisputeCounts>;
  return {
    items: res.items,
    counts: {
      OPEN: c.OPEN ?? 0,
      AWAITING_RETURN: c.AWAITING_RETURN ?? 0,
      REFUNDED: c.REFUNDED ?? 0,
      DENIED: c.DENIED ?? 0,
    },
  };
}

// ---------- mock ----------
function patch(id: number, fn: (d: MockAdminDispute) => MockAdminDispute): Dispute {
  const rows = adminDisputesStore.get();
  const cur = rows.find((d) => d.id === id);
  if (!cur) mockFail('GNE-SYS-4040', { status: 404 });
  const next = fn(cur);
  adminDisputesStore.set(rows.map((d) => (d.id === id ? next : d)));
  return toDispute(next);
}

function newRefund(d: MockAdminDispute): MockAdminRefund {
  const vnpay = d.order.payment_method === 'VNPAY';
  return {
    id: d.id + 1000,
    gateway: vnpay ? 'VNPAY' : null,
    original_transaction_id: vnpay ? `TXN-${8800 + (d.id % 100)}` : null,
    gateway_refund_id: vnpay ? `VNP-RF-${d.code.replace(/\D/g, '')}` : null,
    amount: d.amount,
    status: 'PENDING',
    created_at: new Date().toISOString(),
    manual_transfer: vnpay
      ? null
      : {
          to_account: d.refund_account ?? '',
          transfer_content: refundTransferContent(d.code),
        },
  };
}

const refunded = (d: MockAdminDispute): MockAdminDispute => ({
  ...d,
  status: 'REFUNDED',
  escrow: { ...d.escrow, status: 'REFUNDED' },
  refund: newRefund(d),
});

// ---------- thao tác ----------
export async function resolveDispute(
  id: number,
  outcome: DisputeOutcome,
  verdict: string,
): Promise<Dispute> {
  if (USE_MOCK) {
    await mockDelay(300);
    return patch(id, (d) => {
      if (d.status !== 'OPEN') mockFail('GNE-SYS-4090', { status: 409 });
      const status = statusAfter(outcome);
      const base = { ...d, verdict };
      if (status === 'REFUNDED') return refunded(base);
      if (status === 'DENIED') return { ...base, status, escrow: { ...d.escrow, status: 'HELD' } };
      return { ...base, status };
    });
  }
  return toDispute(
    await api.post<DisputeDto>(`/admin/disputes/${id}/resolve`, { outcome, verdict }),
  );
}

export async function confirmReturnReceived(id: number): Promise<Dispute> {
  if (USE_MOCK) {
    await mockDelay(250);
    return patch(id, refunded);
  }
  return toDispute(await api.post<DisputeDto>(`/admin/disputes/${id}/confirm-return-received`));
}

export async function reopenDispute(id: number): Promise<Dispute> {
  if (USE_MOCK) {
    await mockDelay(200);
    return patch(id, (d) => ({
      ...d,
      status: 'OPEN',
      escrow: { ...d.escrow, status: 'DISPUTED' },
      refund: null,
      verdict: null,
    }));
  }
  return toDispute(await api.post<DisputeDto>(`/admin/disputes/${id}/reopen`));
}

export async function freezeEscrow(
  disputeId: number,
  escrowId: number,
  reason: string,
): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(200);
    patch(disputeId, (d) => {
      if (d.escrow.status === 'RELEASED' || d.escrow.status === 'REFUNDED')
        mockFail('GNE-PAY-4093');
      return { ...d, escrow: { ...d.escrow, status: 'FROZEN' } };
    });
    return;
  }
  await api.post(`/admin/escrows/${escrowId}/freeze`, { reason });
}

export async function unfreezeEscrow(disputeId: number, escrowId: number): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(200);
    patch(disputeId, (d) => ({ ...d, escrow: { ...d.escrow, status: 'DISPUTED' } }));
    return;
  }
  await api.post(`/admin/escrows/${escrowId}/unfreeze`);
}

function patchRefund(refundId: number, fn: (r: MockAdminRefund) => MockAdminRefund): GatewayRefund {
  const d = adminDisputesStore.get().find((x) => x.refund?.id === refundId);
  if (!d?.refund) mockFail('GNE-SYS-4040', { status: 404 });
  const next = fn(d.refund);
  patch(d.id, (x) => ({ ...x, refund: next }));
  return toRefund(next);
}

export async function syncRefund(refundId: number): Promise<GatewayRefund> {
  if (USE_MOCK) {
    await mockDelay(1100);
    return patchRefund(refundId, (r) => (r.status === 'PENDING' ? { ...r, status: 'SUCCESS' } : r));
  }
  return toRefund(await api.post<RefundDto>(`/admin/refunds/${refundId}/sync`));
}

export async function retryRefund(
  refundId: number,
  idempotencyKey: string,
): Promise<GatewayRefund> {
  if (USE_MOCK) {
    await mockDelay(1300);
    return patchRefund(refundId, (r) => ({ ...r, status: 'SUCCESS' }));
  }
  return toRefund(
    await api.post<RefundDto>(`/admin/refunds/${refundId}/retry`, undefined, { idempotencyKey }),
  );
}

export async function markRefundTransferred(
  refundId: number,
  bankReference: string | null,
): Promise<GatewayRefund> {
  if (USE_MOCK) {
    await mockDelay(250);
    return patchRefund(refundId, (r) => ({ ...r, status: 'SUCCESS' }));
  }
  return toRefund(
    await api.post<RefundDto>(`/admin/refunds/${refundId}/mark-transferred`, {
      bank_reference: bankReference,
    }),
  );
}
