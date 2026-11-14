// Tài chính sàn (API_SPEC 11.6–11.9). Mock: lệnh rút / phiên COD ghi kho dùng chung (huy hiệu nav,
// Tổng quan đổi theo); duyệt chi xong ghi bút toán PAYOUT_SETTLE, đối soát xong ghi ESCROW_IN vào sổ cái.
import { api, getPage, request } from '@/shared/api/http';
import { mockDelay, mockFail, paginateCursor, USE_MOCK } from '@/shared/api/mock';
import {
  adminCodStore,
  adminWithdrawalsStore,
  COD_SHIPMENTS,
  type MockAdminWithdrawal,
} from '@/shared/api/mock/admin';
import type { CursorPage } from '@/shared/api/types';
import { reconcile } from '../domain/cod.rules';
import {
  payoutTransferContent,
  withdrawalSummary,
  type WithdrawRejectReason,
} from '../domain/finance.rules';
import type {
  CodRemittance,
  CodRemittanceDetail,
  CommissionSettings,
  LedgerEntry,
  LedgerEvent,
  PayoutTransfer,
  Reconciliation,
  StatementLine,
  Withdrawal,
  WithdrawalSummary,
} from '../domain/finance.types';
import type {
  CodRemittanceDetailDto,
  CodRemittanceDto,
  CommissionSettingsDto,
  LedgerEntryDto,
  ReconciliationDto,
  TransferDto,
  WithdrawalDto,
} from './finance.dto';
import {
  toCodDetail,
  toCodRemittance,
  toCommission,
  toLedgerEntry,
  toLineDto,
  toReconciliation,
  toReconciliationDto,
  toTransfer,
  toWithdrawal,
} from './finance.mapper';
import { appendLedger, commissionStore, ledgerStore } from './finance.mock';

// ---------- 11.6 Rút tiền ----------
export interface WithdrawalList {
  items: Withdrawal[];
  summary: WithdrawalSummary;
}

export async function listWithdrawals(): Promise<WithdrawalList> {
  if (USE_MOCK) {
    await mockDelay(200);
    const items = adminWithdrawalsStore.get().map(toWithdrawal);
    return { items, summary: withdrawalSummary(items) };
  }
  const res = await getPage<WithdrawalDto, Withdrawal>('/admin/withdrawals', toWithdrawal, {
    query: { size: 50 },
  });
  const s = res.summary as {
    pending_count?: number;
    payable_amount?: number;
    blocked_amount?: number;
  };
  return {
    items: res.items,
    summary: {
      pendingCount: s.pending_count ?? 0,
      payableAmount: s.payable_amount ?? 0,
      blockedAmount: s.blocked_amount ?? 0,
    },
  };
}

const mockTransfer = (w: MockAdminWithdrawal): PayoutTransfer => {
  const content = payoutTransferContent(w.code);
  return toTransfer({
    ...w.transfer,
    bank_name: w.transfer.bank_name,
    account_number: w.transfer.account_number,
    account_holder: w.transfer.account_holder,
    amount: w.amount,
    transfer_content: content,
    qr_payload: `000201010212|${w.transfer.account_number}|${w.amount}|${content}`,
  });
};

function patchWithdrawal(id: number, fn: (w: MockAdminWithdrawal) => MockAdminWithdrawal) {
  const rows = adminWithdrawalsStore.get();
  const cur = rows.find((w) => w.id === id);
  if (!cur) mockFail('GNE-SYS-4040', { status: 404 });
  const next = fn(cur);
  adminWithdrawalsStore.set(rows.map((w) => (w.id === id ? next : w)));
  return next;
}

/** Duyệt chi → trả thông tin chuyển khoản (VietQR, nội dung TRWD…). */
export async function approveWithdrawal(id: number): Promise<PayoutTransfer> {
  if (USE_MOCK) {
    await mockDelay(300);
    const w = patchWithdrawal(id, (x) => {
      if (x.shop_kyc_status !== 'VERIFIED') mockFail('GNE-WAL-4092');
      if (x.status !== 'PENDING') mockFail('GNE-SYS-4090', { status: 409 });
      return { ...x, status: 'APPROVED' };
    });
    return mockTransfer(w);
  }
  const res = await api.post<{ status: string; transfer: TransferDto }>(
    `/admin/withdrawals/${id}/approve`,
  );
  return toTransfer(res.transfer);
}

export async function getWithdrawalTransfer(id: number): Promise<PayoutTransfer> {
  if (USE_MOCK) {
    await mockDelay(150);
    const w = adminWithdrawalsStore.get().find((x) => x.id === id);
    if (!w) mockFail('GNE-SYS-4040', { status: 404 });
    return mockTransfer(w);
  }
  return toTransfer(await api.get<TransferDto>(`/admin/withdrawals/${id}/transfer`));
}

export async function rejectWithdrawal(
  id: number,
  reason: WithdrawRejectReason,
  note: string | null,
): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(250);
    patchWithdrawal(id, (x) => ({ ...x, status: 'REJECTED', reject_reason: reason }));
    return;
  }
  await api.post(`/admin/withdrawals/${id}/reject`, { reason_code: reason, note });
}

/**
 * Hoàn tất chi: thật = webhook SePay (nội dung TRWD…) tự đánh dấu, hoặc "Xác nhận tay" sau 24 giờ
 * kèm mã giao dịch ngân hàng. Mock: cả hai đường dùng hàm này.
 */
export async function completeWithdrawal(id: number, bankReference: string): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(250);
    const w = patchWithdrawal(id, (x) => {
      if (x.status !== 'APPROVED' && x.status !== 'PROCESSING')
        mockFail('GNE-SYS-4090', { status: 409 });
      return { ...x, status: 'COMPLETED' };
    });
    appendLedger([
      {
        event_type: 'PAYOUT_SETTLE',
        reference: w.code,
        account: 'SELLER_PAYABLE',
        debit: w.amount,
        credit: null,
      },
    ]);
    return;
  }
  await api.post(`/admin/withdrawals/${id}/complete`, { bank_reference: bankReference });
}

// ---------- 11.8 Hoa hồng ----------
export async function getCommission(): Promise<CommissionSettings> {
  if (USE_MOCK) {
    await mockDelay(150);
    return toCommission(commissionStore.get());
  }
  return toCommission(await api.get<CommissionSettingsDto>('/admin/commission-settings'));
}

export async function saveDefaultRate(rate: number): Promise<CommissionSettings> {
  if (USE_MOCK) {
    await mockDelay(220);
    return toCommission(commissionStore.set({ ...commissionStore.get(), default_rate: rate }));
  }
  return toCommission(
    await api.patch<CommissionSettingsDto>('/admin/commission-settings', { default_rate: rate }),
  );
}

export async function saveCategoryRates(
  rows: CommissionSettings['categories'],
): Promise<CommissionSettings> {
  const categories = rows.map((c) => ({
    category_id: c.categoryId,
    rate: c.rate,
    is_enabled: c.enabled,
  }));
  if (USE_MOCK) {
    await mockDelay(250);
    const cur = commissionStore.get();
    return toCommission(
      commissionStore.set({
        ...cur,
        categories: cur.categories.map((c) => {
          const hit = categories.find((x) => x.category_id === c.category_id);
          return hit ? { ...c, rate: hit.rate, is_enabled: hit.is_enabled } : c;
        }),
      }),
    );
  }
  return toCommission(
    await api.patch<CommissionSettingsDto>('/admin/commission-settings', { categories }),
  );
}

// ---------- 11.9 Sổ cái ----------
export interface LedgerQuery {
  eventType?: LedgerEvent;
  q?: string;
}

const LEDGER_LIMIT = 50;

export async function listLedger(
  f: LedgerQuery,
  cursor: string | null = null,
): Promise<CursorPage<LedgerEntry>> {
  const keep = (rows: LedgerEntryDto[]) =>
    rows.map(toLedgerEntry).filter((x): x is LedgerEntry => x !== null);
  if (USE_MOCK) {
    await mockDelay(180);
    const needle = f.q?.trim().toLowerCase() ?? '';
    const rows = ledgerStore
      .get()
      .filter((r) => !f.eventType || r.event_type === f.eventType)
      .filter((r) => `${r.reference} ${r.account}`.toLowerCase().includes(needle));
    const page = paginateCursor(rows, { cursor, limit: LEDGER_LIMIT });
    return { ...page, items: keep(page.items) };
  }
  const res = await request<LedgerEntryDto[]>('/admin/ledger-entries', {
    query: { event_type: f.eventType, q: f.q?.trim() || undefined, cursor, limit: LEDGER_LIMIT },
  });
  const p = res.pagination && 'has_more' in res.pagination ? res.pagination : null;
  return {
    items: keep(res.data),
    limit: p?.limit ?? LEDGER_LIMIT,
    has_more: p?.has_more ?? false,
    next_cursor: p?.next_cursor ?? null,
  };
}

/** Mọi dòng khớp bộ lọc (để xuất CSV phía client khi chạy mock; thật dùng /ledger-entries/export). */
export async function listAllLedger(f: LedgerQuery): Promise<LedgerEntry[]> {
  const out: LedgerEntry[] = [];
  let cursor: string | null = null;
  for (let i = 0; i < 40; i += 1) {
    const page: CursorPage<LedgerEntry> = await listLedger(f, cursor);
    out.push(...page.items);
    if (!page.has_more || !page.next_cursor) break;
    cursor = page.next_cursor;
  }
  return out;
}

// ---------- 11.7 Đối soát COD ----------
export async function listCodRemittances(): Promise<CodRemittance[]> {
  if (USE_MOCK) {
    await mockDelay(180);
    return adminCodStore.get().map(toCodRemittance);
  }
  const res = await getPage<CodRemittanceDto, CodRemittance>(
    '/admin/cod-remittances',
    toCodRemittance,
    {
      query: { size: 50 },
    },
  );
  return res.items;
}

const codLookup = (code: string) => {
  const hit = COD_SHIPMENTS[code];
  return hit ? { orderCode: hit.order_code, amount: hit.amount } : null;
};

export async function getCodRemittance(id: number): Promise<CodRemittanceDetail> {
  if (USE_MOCK) {
    await mockDelay(150);
    const s = adminCodStore.get().find((x) => x.id === id);
    if (!s) mockFail('GNE-SYS-4040', { status: 404 });
    const done =
      s.status === 'DONE'
        ? toReconciliationDto(
            reconcile(
              s.confirmed_lines.map((l) => ({
                trackingCode: l.tracking_code,
                amount: l.amount,
                fee: l.fee,
              })),
              s.received_amount,
              codLookup,
            ),
          )
        : null;
    const dto: CodRemittanceDetailDto = {
      ...s,
      statement_file_name: s.statement_file_name,
      sample_lines: s.sample_lines,
      ...(done ? { lines: done.lines, totals: done.totals } : {}),
    };
    return toCodDetail(dto);
  }
  return toCodDetail(await api.get<CodRemittanceDetailDto>(`/admin/cod-remittances/${id}`));
}

export async function previewReconciliation(
  id: number,
  fileName: string,
  lines: StatementLine[],
): Promise<Reconciliation> {
  if (USE_MOCK) {
    await mockDelay(250);
    const s = adminCodStore.get().find((x) => x.id === id);
    if (!s) mockFail('GNE-SYS-4040', { status: 404 });
    return reconcile(lines, s.received_amount, codLookup);
  }
  return toReconciliation(
    await api.post<ReconciliationDto>(`/admin/cod-remittances/${id}/reconciliation-preview`, {
      statement_file_name: fileName,
      lines: lines.map(toLineDto),
    }),
  );
}

export async function confirmReconciliation(
  id: number,
  fileName: string,
  lines: StatementLine[],
  idempotencyKey: string,
): Promise<number> {
  if (USE_MOCK) {
    await mockDelay(300);
    const rows = adminCodStore.get();
    const s = rows.find((x) => x.id === id);
    if (!s) mockFail('GNE-SYS-4040', { status: 404 });
    const r = reconcile(lines, s.received_amount, codLookup);
    if (!r.totals.balanced || r.totals.matchedCount === 0) mockFail('GNE-LOG-4221');
    adminCodStore.set(
      rows.map((x) =>
        x.id === id
          ? {
              ...x,
              status: 'DONE',
              statement_file_name: fileName,
              confirmed_lines: lines.map(toLineDto),
            }
          : x,
      ),
    );
    appendLedger(
      r.lines
        .filter((l) => l.result === 'MATCH')
        .map((l) => ({
          event_type: 'ESCROW_IN',
          reference: l.orderCode ?? l.trackingCode,
          account: 'ESCROW_LIABILITY',
          debit: null,
          credit: l.amount,
        })),
    );
    return r.totals.matchedCount;
  }
  const res = await api.post<{ status: string; funded_order_count: number }>(
    `/admin/cod-remittances/${id}/confirm`,
    { statement_file_name: fileName, lines: lines.map(toLineDto) },
    { idempotencyKey },
  );
  return res.funded_order_count;
}
