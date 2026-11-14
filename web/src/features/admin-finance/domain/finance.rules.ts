// Hàm thuần tài chính sàn (API_SPEC 11.6, 11.8, 11.9, 16.4).
import type { LedgerAccount, LedgerEvent, Withdrawal, WithdrawalSummary } from './finance.types';

// ---------- Rút tiền ----------
/** Lý do từ chối rút tiền (16.4). */
export const WITHDRAW_REJECT_REASONS = [
  'ACCOUNT_MISMATCH',
  'OPEN_DISPUTE',
  'SUSPICIOUS',
  'OTHER',
] as const;
export type WithdrawRejectReason = (typeof WITHDRAW_REJECT_REASONS)[number];

/** Nội dung chuyển khoản chi: PAYOUT-0012 → TRWD0012. */
export const payoutTransferContent = (code: string): string => `TRWD${code.replace(/\D/g, '')}`;

export type WithdrawalView = 'review' | 'blocked' | 'awaiting_transfer' | 'completed' | 'rejected';

/** Cột thao tác: chờ + shop đã xác minh → Từ chối / Duyệt chi; shop chưa xác minh → chặn. */
export function withdrawalView(w: Pick<Withdrawal, 'status' | 'shopKycStatus'>): WithdrawalView {
  switch (w.status) {
    case 'PENDING':
      return w.shopKycStatus === 'VERIFIED' ? 'review' : 'blocked';
    case 'APPROVED':
    case 'PROCESSING':
      return 'awaiting_transfer';
    case 'COMPLETED':
      return 'completed';
    case 'REJECTED':
      return 'rejected';
  }
}

export function withdrawalSummary(
  rows: Pick<Withdrawal, 'status' | 'shopKycStatus' | 'amount'>[],
): WithdrawalSummary {
  const pending = rows.filter((w) => w.status === 'PENDING');
  const payable = pending.filter((w) => w.shopKycStatus === 'VERIFIED');
  const sum = (xs: { amount: number }[]) => xs.reduce((n, x) => n + x.amount, 0);
  return {
    pendingCount: pending.length,
    payableAmount: sum(payable),
    blockedAmount: sum(pending) - sum(payable),
  };
}

// ---------- Hoa hồng ----------
export const MAX_RATE = 50;

/** Tỷ lệ 0–50, cho phép thập phân (dấu phẩy hoặc chấm); sai → null. */
export function parseRate(raw: string): number | null {
  const s = raw.trim().replace(',', '.');
  if (!s) return null;
  const n = Number(s);
  return Number.isFinite(n) && n >= 0 && n <= MAX_RATE ? Math.round(n * 100) / 100 : null;
}

// ---------- Sổ cái ----------
export const LEDGER_EVENTS: LedgerEvent[] = [
  'ESCROW_IN',
  'COMMISSION',
  'PLATFORM_VOUCHER',
  'FREESHIP_COST',
  'COIN_COST',
  'ESCROW_RELEASE',
  'PAYOUT_ACCRUE',
  'PAYOUT_SETTLE',
  'REFUND',
  'GATEWAY_FEE',
];

/** Màu nhãn sự kiện (prototype EVENT). */
export const EVENT_TONE: Record<
  LedgerEvent,
  'warning' | 'primary' | 'success' | 'mint' | 'neutral' | 'danger'
> = {
  ESCROW_IN: 'warning',
  COMMISSION: 'primary',
  PLATFORM_VOUCHER: 'neutral',
  FREESHIP_COST: 'neutral',
  COIN_COST: 'neutral',
  ESCROW_RELEASE: 'success',
  PAYOUT_ACCRUE: 'mint',
  PAYOUT_SETTLE: 'neutral',
  REFUND: 'danger',
  GATEWAY_FEE: 'neutral',
};

export const LEDGER_ACCOUNTS: LedgerAccount[] = [
  'ESCROW_LIABILITY',
  'PLATFORM_REVENUE',
  'SELLER_PAYABLE',
  'PLATFORM_EXPENSE',
  'CASH',
];
