import { describe, expect, it } from 'vitest';
import { pageList } from './Pagination';
import { discountPercent } from './Price';
import { qrCells } from './QrPlaceholder';
import { clampQty } from './QuantityStepper';
import { sealCurves } from './Seal';
import { $toasts, toast } from './toast.store';

describe('pageList (prototype Discover)', () => {
  it('chèn … đúng chỗ', () => {
    expect(pageList(1, 1)).toEqual([1]);
    expect(pageList(1, 3)).toEqual([1, 2, 3]);
    expect(pageList(2, 8)).toEqual([1, 2, 3, '…', 8]);
    expect(pageList(5, 8)).toEqual([1, '…', 4, 5, 6, '…', 8]);
    expect(pageList(8, 8)).toEqual([1, '…', 7, 8]);
  });
});

describe('toast store', () => {
  it('giữ tối đa 3 toast, bỏ cái cũ nhất, đóng theo id', () => {
    $toasts.set([]);
    const ids = ['a', 'b', 'c', 'd'].map((m) => toast.info(m, 0));
    expect($toasts.get().map((x) => x.message)).toEqual(['b', 'c', 'd']);
    toast.dismiss(ids[2]);
    expect($toasts.get().map((x) => x.message)).toEqual(['b', 'd']);
    toast.error('lỗi', 0);
    expect($toasts.get().at(-1)?.type).toBe('error');
  });
});

describe('hàm thuần nhóm C', () => {
  it('discountPercent', () => {
    expect(discountPercent(389000, 799000)).toBe(51);
    expect(discountPercent(100, 100)).toBe(0);
    expect(discountPercent(100, null)).toBe(0);
  });
  it('clampQty theo tồn kho', () => {
    expect(clampQty(0, 1, 5)).toBe(1);
    expect(clampQty(9, 1, 5)).toBe(5);
    expect(clampQty(2.7, 1, 5)).toBe(2);
  });
  it('sealCurves ổn định theo mã shop, khác nhau giữa các shop', () => {
    expect(sealCurves('v1')).toEqual(sealCurves('v1'));
    expect(sealCurves('v1')[0]).not.toBe(sealCurves('v2')[0]);
    expect(sealCurves('v1')).toHaveLength(3);
  });
  it('qrCells có 3 ô định vị và ổn định theo seed', () => {
    const cells = qrCells('TR141');
    const has = (x: number, y: number) => cells.some(([a, b]) => a === x && b === y);
    expect(has(0, 0) && has(28, 0) && has(0, 28)).toBe(true);
    expect(has(28, 28)).toBe(qrCells('TR141').some(([a, b]) => a === 28 && b === 28));
    expect(qrCells('A')).not.toEqual(qrCells('B'));
  });
});
