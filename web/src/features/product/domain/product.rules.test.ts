import { describe, expect, it } from 'vitest';
import {
  badges,
  decodeSelection,
  discountPct,
  encodeSelection,
  isFlashSoldOut,
  maxQty,
  missingGroups,
  needsCanonicalRedirect,
  priceOf,
  productHref,
  stockOf,
  stockState,
  variantLabel,
} from './product.rules';
import type { ProductSummary, VariantGroup } from './product.types';

const GROUPS: VariantGroup[] = [
  {
    name: 'Màu sắc',
    controlsStock: true,
    options: [
      { label: 'Đen', priceDelta: 0, stock: 38 },
      { label: 'Trắng', priceDelta: 0, stock: 12 },
      { label: 'Xanh navy', priceDelta: 30000, stock: 5 },
      { label: 'Hồng', priceDelta: 20000, stock: 0 },
    ],
  },
  {
    name: 'Phiên bản',
    controlsStock: false,
    options: [
      { label: 'Tiêu chuẩn', priceDelta: 0 },
      { label: 'Bản Pro', priceDelta: 150000 },
    ],
  },
];

const base: ProductSummary = {
  id: 1,
  slug: 'tai-nghe',
  title: 'Tai nghe',
  image: '',
  price: 389000,
  originalPrice: 799000,
  discountPct: 51,
  rating: 4.8,
  soldCount: 2100,
  shopId: 1,
  shopName: 'TechZone',
  shopOnVacation: false,
  categoryId: 1,
  freeship: true,
  authentic: true,
  flash: { sold: 45, total: 50 },
  preorderDays: null,
  brand: 'Sony',
  brandVerified: true,
  attribute: null,
};

describe('product.rules', () => {
  it('đường dẫn và chuyển hướng slug sai', () => {
    expect(productHref(base)).toBe('/p/tai-nghe-1');
    expect(needsCanonicalRedirect('tai-nghe', base)).toBe(false);
    expect(needsCanonicalRedirect('ten-cu', base)).toBe(true);
  });

  it('phần trăm giảm', () => {
    expect(discountPct(389000, 799000)).toBe(51);
    expect(discountPct(100, null)).toBe(0);
    expect(discountPct(100, 90)).toBe(0);
  });

  it('nhãn và Flash Sale', () => {
    expect(badges(base)).toEqual(['authentic', 'flash']);
    expect(badges({ ...base, flash: null, preorderDays: 14 }, 0)).toEqual([
      'authentic',
      'preorder',
      'out_of_stock',
    ]);
    expect(isFlashSoldOut(base)).toBe(false);
    expect(isFlashSoldOut({ flash: { sold: 50, total: 50 } })).toBe(true);
    expect(maxQty(base, 38)).toBe(1);
    expect(maxQty({ flash: null }, 12)).toBe(12);
  });

  it('giá, tồn, nhãn theo lựa chọn phân loại', () => {
    const p = { price: 389000, variantGroups: GROUPS };
    expect(priceOf(p, {})).toBe(389000);
    expect(priceOf(p, { 'Màu sắc': 2, 'Phiên bản': 1 })).toBe(389000 + 30000 + 150000);
    expect(stockOf(GROUPS, {})).toBe(55);
    expect(stockOf(GROUPS, { 'Màu sắc': 3 })).toBe(0);
    expect(missingGroups(GROUPS, { 'Màu sắc': 0 })).toEqual(['Phiên bản']);
    expect(variantLabel(GROUPS, { 'Màu sắc': 0, 'Phiên bản': 1 })).toBe('Đen / Bản Pro');
  });

  it('trạng thái kho', () => {
    expect(stockState(38, null)).toBe('in_stock');
    expect(stockState(5, null)).toBe('low');
    expect(stockState(0, null)).toBe('out_of_stock');
    expect(stockState(0, 14)).toBe('preorder');
  });

  it('mã phân loại trong liên kết chia sẻ, bỏ lựa chọn hết hàng', () => {
    expect(encodeSelection(GROUPS, { 'Màu sắc': 1, 'Phiên bản': null })).toBe('1-');
    expect(decodeSelection(GROUPS, '1-1')).toEqual({ 'Màu sắc': 1, 'Phiên bản': 1 });
    expect(decodeSelection(GROUPS, '3-0')).toEqual({ 'Màu sắc': null, 'Phiên bản': 0 });
    expect(decodeSelection(GROUPS, null)).toEqual({ 'Màu sắc': null, 'Phiên bản': null });
  });
});
