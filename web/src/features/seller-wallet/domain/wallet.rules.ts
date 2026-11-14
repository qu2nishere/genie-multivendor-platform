// Quy tắc ví: kiểm số tiền rút (tối thiểu 100.000₫, không vượt khả dụng, có tài khoản đã xác minh),
// huy hiệu loại / trạng thái giao dịch, nhãn tháng.
import type { LedgerEntry, TxState, TxType, Wallet } from './wallet.types';

export const MIN_WITHDRAW = 100_000;
export const TX_TYPES: TxType[] = ['SALE', 'RELEASE', 'PAYOUT', 'REFUND'];

export type WithdrawIssue = 'no_account' | 'min' | 'over';

export function withdrawIssue(
  amount: number,
  w: Pick<Wallet, 'available' | 'payoutAccount' | 'minWithdrawal'>,
): WithdrawIssue | null {
  if (!w.payoutAccount?.verified) return 'no_account';
  if (amount < (w.minWithdrawal || MIN_WITHDRAW)) return 'min';
  if (amount > w.available) return 'over';
  return null;
}

/** Biến thể Badge theo loại (prototype TYPE_BADGE). */
export const TYPE_TONE: Record<TxType, 'warning' | 'success' | 'neutral' | 'danger'> = {
  SALE: 'warning',
  RELEASE: 'success',
  PAYOUT: 'neutral',
  REFUND: 'danger',
};

/** Hiển thị cột Trạng thái: StatusBadge (HELD / AVAILABLE) hoặc Badge. */
export const STATE_VIEW: Record<
  TxState,
  | { kind: 'status'; status: 'HELD' | 'AVAILABLE' }
  | { kind: 'badge'; variant: 'warning' | 'neutral' | 'danger' }
> = {
  HELD: { kind: 'status', status: 'HELD' },
  AVAILABLE: { kind: 'status', status: 'AVAILABLE' },
  PAYOUT_PENDING: { kind: 'badge', variant: 'warning' },
  PAYOUT_DONE: { kind: 'badge', variant: 'neutral' },
  REFUNDED: { kind: 'badge', variant: 'danger' },
};

/** "2026-06" → "06/2026". */
export const monthLabel = (m: string): string => `${m.slice(5, 7)}/${m.slice(0, 4)}`;

/** Tên tệp xuất (API: so-giao-dich-vi-06-2026.csv). */
export const csvName = (m: string): string =>
  `so-giao-dich-vi-${m.slice(5, 7)}-${m.slice(0, 4)}.csv`;

/** Lọc phía client theo tháng / loại / chữ (mock + tìm tức thời). */
export function filterLedger(
  rows: LedgerEntry[],
  q: { month: string; type: TxType | null; q: string },
) {
  const n = q.q.trim().toLowerCase();
  return rows.filter(
    (r) =>
      r.createdAt.slice(0, 7) === q.month &&
      (!q.type || r.type === q.type) &&
      (!n || `${r.reference} ${r.description}`.toLowerCase().includes(n)),
  );
}
