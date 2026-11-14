// Cấu hình hoa hồng + sổ cái (prototype admin/Finance.jsx Commission, data.jsx LEDGER) — hình DTO.
// Lệnh rút và phiên COD ở kho dùng chung shared/api/mock/admin.ts (huy hiệu nav, Tổng quan đọc).
import { mockState } from '@/shared/api/mock';
import type { CommissionSettingsDto, LedgerEntryDto } from './finance.dto';

export const commissionStore = mockState<CommissionSettingsDto>('admin.commission', () => ({
  default_rate: 5,
  categories: (
    [
      [1, 'Điện tử', 5],
      [2, 'Thời trang', 8],
      [3, 'Làm đẹp', 10],
      [4, 'Nhà cửa', 6],
      [5, 'Phụ kiện', 7],
      [8, 'Bách hoá', 4],
    ] as [number, string, number][]
  ).map(([category_id, name, rate]) => ({ category_id, name, rate, is_enabled: true })),
}));

const e = (
  seq: number,
  at: string,
  event_type: string,
  reference: string,
  account: string,
  debit: number | null,
  credit: number | null,
): LedgerEntryDto => ({
  seq,
  created_at: `${at}+07:00`,
  event_type,
  reference,
  account,
  debit,
  credit,
});

/** Sổ cái append-only, mới nhất trước. */
export const ledgerStore = mockState<LedgerEntryDto[]>('admin.ledger', () => [
  e(
    100482,
    '2026-06-08T14:22:07',
    'ESCROW_IN',
    'TR-20260608-000141',
    'ESCROW_LIABILITY',
    null,
    778000,
  ),
  e(
    100481,
    '2026-06-08T14:22:07',
    'COMMISSION',
    'TR-20260608-000141',
    'PLATFORM_REVENUE',
    null,
    38900,
  ),
  e(
    100480,
    '2026-06-08T09:10:55',
    'ESCROW_IN',
    'TR-20260608-000140',
    'ESCROW_LIABILITY',
    null,
    690000,
  ),
  e(
    100479,
    '2026-06-07T18:46:12',
    'ESCROW_RELEASE',
    'TR-20260608-000139',
    'ESCROW_LIABILITY',
    1245000,
    null,
  ),
  e(
    100478,
    '2026-06-07T18:46:12',
    'PAYOUT_ACCRUE',
    'WALLET·techzone',
    'SELLER_PAYABLE',
    null,
    1245000,
  ),
  e(100477, '2026-06-06T16:30:41', 'PAYOUT_SETTLE', 'PAYOUT-0008', 'SELLER_PAYABLE', 3400000, null),
  e(100476, '2026-06-06T08:15:33', 'REFUND', 'RFN-0229', 'ESCROW_LIABILITY', 389000, null),
]);

/** Ghi thêm bút toán (mock của server — sổ cái chỉ thêm, không sửa / xóa). */
export function appendLedger(rows: Omit<LedgerEntryDto, 'seq' | 'created_at'>[]): void {
  const cur = ledgerStore.get();
  let seq = Math.max(0, ...cur.map((r) => r.seq));
  const at = new Date().toISOString();
  ledgerStore.set([...rows.map((r) => ({ ...r, seq: ++seq, created_at: at })).reverse(), ...cur]);
}
