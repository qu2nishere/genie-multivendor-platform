import { describe, expect, it } from 'vitest';
import { csvName, filterLedger, monthLabel, withdrawIssue } from './wallet.rules';
import type { LedgerEntry } from './wallet.types';

const acc = {
  bank: 'Vietcombank',
  holder: 'DANG QUOC HUY',
  numberMasked: '•••• 6712',
  verified: true,
};

describe('wallet.rules', () => {
  it('kiểm số tiền rút', () => {
    const w = { available: 12_450_000, payoutAccount: acc, minWithdrawal: 100_000 };
    expect(withdrawIssue(99_999, w)).toBe('min');
    expect(withdrawIssue(12_450_001, w)).toBe('over');
    expect(withdrawIssue(5_000_000, w)).toBeNull();
    expect(withdrawIssue(5_000_000, { ...w, payoutAccount: null })).toBe('no_account');
    expect(withdrawIssue(5_000_000, { ...w, payoutAccount: { ...acc, verified: false } })).toBe(
      'no_account',
    );
  });

  it('nhãn tháng + tên tệp', () => {
    expect(monthLabel('2026-06')).toBe('06/2026');
    expect(csvName('2026-06')).toBe('so-giao-dich-vi-06-2026.csv');
  });

  it('lọc sổ theo tháng, loại, chữ', () => {
    const row = (id: number, at: string, type: LedgerEntry['type'], ref: string): LedgerEntry => ({
      id,
      createdAt: at,
      reference: ref,
      type,
      description: 'Bán hàng — vào escrow',
      gross: 1,
      commission: 0,
      net: 1,
      state: 'HELD',
    });
    const rows = [
      row(1, '2026-06-08T14:22:00+07:00', 'SALE', 'TR-20260608-000141'),
      row(2, '2026-06-07T18:46:00+07:00', 'RELEASE', 'TR-20260608-000139'),
      row(3, '2026-05-30T10:00:00+07:00', 'SALE', 'TR-20260530-000101'),
    ];
    expect(filterLedger(rows, { month: '2026-06', type: null, q: '' }).map((r) => r.id)).toEqual([
      1, 2,
    ]);
    expect(filterLedger(rows, { month: '2026-06', type: 'SALE', q: '' }).map((r) => r.id)).toEqual([
      1,
    ]);
    expect(
      filterLedger(rows, { month: '2026-06', type: null, q: '000139' }).map((r) => r.id),
    ).toEqual([2]);
  });
});
