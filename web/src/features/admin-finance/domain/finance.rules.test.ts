import { describe, expect, it } from 'vitest';
import { canConfirm, parseCodCsv, reconcile } from './cod.rules';
import {
  parseRate,
  payoutTransferContent,
  withdrawalSummary,
  withdrawalView,
} from './finance.rules';

const SHIP: Record<string, { orderCode: string; amount: number }> = {
  VN931042698: { orderCode: 'TR-20260608-000136', amount: 640000 },
  VN880077120: { orderCode: 'TR-20260608-000128', amount: 484000 },
  VN931042544: { orderCode: 'TR-20260608-000134', amount: 540000 },
  VN931042455: { orderCode: 'TR-20260608-000131', amount: 320000 },
};
const lookup = (c: string) => SHIP[c] ?? null;

describe('finance.rules', () => {
  it('nội dung chuyển khoản chi', () => {
    expect(payoutTransferContent('PAYOUT-0012')).toBe('TRWD0012');
  });
  it('trạng thái hiển thị lệnh rút', () => {
    expect(withdrawalView({ status: 'PENDING', shopKycStatus: 'VERIFIED' })).toBe('review');
    expect(withdrawalView({ status: 'PENDING', shopKycStatus: 'SUPPLEMENT_REQUESTED' })).toBe(
      'blocked',
    );
    expect(withdrawalView({ status: 'PROCESSING', shopKycStatus: 'VERIFIED' })).toBe(
      'awaiting_transfer',
    );
  });
  it('3 chỉ số rút tiền khớp prototype', () => {
    const rows = [
      { status: 'PENDING' as const, shopKycStatus: 'VERIFIED' as const, amount: 5000000 },
      { status: 'PENDING' as const, shopKycStatus: 'VERIFIED' as const, amount: 2300000 },
      { status: 'PENDING' as const, shopKycStatus: 'VERIFIED' as const, amount: 850000 },
      {
        status: 'PENDING' as const,
        shopKycStatus: 'SUPPLEMENT_REQUESTED' as const,
        amount: 1200000,
      },
      { status: 'COMPLETED' as const, shopKycStatus: 'VERIFIED' as const, amount: 3400000 },
    ];
    expect(withdrawalSummary(rows)).toEqual({
      pendingCount: 4,
      payableAmount: 8150000,
      blockedAmount: 1200000,
    });
  });
  it('tỷ lệ hoa hồng 0–50', () => {
    expect(parseRate('7,5')).toBe(7.5);
    expect(parseRate('51')).toBeNull();
    expect(parseRate('')).toBeNull();
  });
});

describe('cod.rules', () => {
  it('đọc CSV có tiêu đề, BOM, dòng trống, dòng lỗi', () => {
    const csv =
      '\uFEFFma_van_don,so_tien,phi\r\nVN931042698,640000,28000\r\n\r\n"VN880077120","484000","21000"\r\n,1,1\r\nVNX,abc,0\r\n';
    const r = parseCodCsv(csv);
    expect(r.lines).toEqual([
      { trackingCode: 'VN931042698', amount: 640000, fee: 28000 },
      { trackingCode: 'VN880077120', amount: 484000, fee: 21000 },
    ]);
    expect(r.errors).toEqual([5, 6]);
  });
  it('phiên GHN 12/06: cân, 2 dòng khớp', () => {
    const r = reconcile(
      [
        { trackingCode: 'VN931042698', amount: 640000, fee: 28000 },
        { trackingCode: 'VN880077120', amount: 484000, fee: 21000 },
      ],
      1075000,
      lookup,
    );
    expect(r.totals).toEqual({
      statementNet: 1075000,
      receivedAmount: 1075000,
      matchedCount: 2,
      balanced: true,
    });
    expect(canConfirm(r)).toBe(true);
  });
  it('phiên GHTK 10/06: lệch số tiền + không tìm thấy vận đơn', () => {
    const r = reconcile(
      [
        { trackingCode: 'VN931042544', amount: 540000, fee: 24000 },
        { trackingCode: 'VN931042455', amount: 300000, fee: 14000 },
        { trackingCode: 'VN000000000', amount: 18000, fee: 0 },
      ],
      820000,
      lookup,
    );
    expect(r.lines.map((l) => l.result)).toEqual(['MATCH', 'AMOUNT_MISMATCH', 'NOT_FOUND']);
    expect(r.totals.balanced).toBe(true);
    expect(canConfirm(reconcile([], 0, lookup))).toBe(false);
  });
});
