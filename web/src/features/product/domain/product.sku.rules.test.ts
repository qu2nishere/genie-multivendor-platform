// Luật ma trận SKU thật (plan 2026-11-17 C07.07, A7) — cùng ca với VariantMatrixTest của backend.
import { describe, expect, it } from 'vitest';
import { decodeShared, isOptionOut, priceOf, shareVariantParam, stockFor } from './product.rules';
import type { ProductSku, VariantGroup } from './product.types';

const GROUPS: VariantGroup[] = [
  {
    name: 'Màu sắc',
    controlsStock: true,
    options: [
      { label: 'Đen', priceDelta: 0 },
      { label: 'Trắng', priceDelta: 0 },
    ],
  },
  {
    name: 'Phiên bản',
    controlsStock: false,
    options: [
      { label: 'Tiêu chuẩn', priceDelta: 0 },
      { label: 'Bản Pro', priceDelta: 0 },
    ],
  },
];
// 101 Đen/Tiêu chuẩn còn 5 · 102 Đen/Pro hết · 103 Trắng/Tiêu chuẩn còn 2
const SKUS: ProductSku[] = [
  { id: 101, options: [0, 0], price: 389000, stock: 5 },
  { id: 102, options: [0, 1], price: 539000, stock: 0 },
  { id: 103, options: [1, 0], price: 389000, stock: 2 },
];
const P = { price: 389000, variantGroups: GROUPS, skus: SKUS };

describe('product sku rules', () => {
  it('prices the chosen sku and falls back to the lowest price', () => {
    expect(priceOf(P, { 'Màu sắc': 0, 'Phiên bản': 1 })).toBe(539000);
    expect(priceOf(P, { 'Màu sắc': 0, 'Phiên bản': null })).toBe(389000);
  });

  it('sums stock of matching skus', () => {
    expect(stockFor(P, {})).toBe(7);
    expect(stockFor(P, { 'Màu sắc': 0, 'Phiên bản': null })).toBe(5);
    expect(stockFor(P, { 'Màu sắc': 0, 'Phiên bản': 1 })).toBe(0);
  });

  it('locks options without an in-stock combination', () => {
    // Đã chọn Đen → "Bản Pro" bị khóa, "Tiêu chuẩn" còn
    expect(isOptionOut(P, { 'Màu sắc': 0, 'Phiên bản': null }, 1, 1)).toBe(true);
    expect(isOptionOut(P, { 'Màu sắc': 0, 'Phiên bản': null }, 1, 0)).toBe(false);
    // Đã chọn Bản Pro → không màu nào còn
    expect(isOptionOut(P, { 'Màu sắc': null, 'Phiên bản': 1 }, 0, 0)).toBe(true);
    expect(isOptionOut(P, { 'Màu sắc': null, 'Phiên bản': 1 }, 0, 1)).toBe(true);
    // Chưa chọn gì → cả hai màu còn
    expect(isOptionOut(P, {}, 0, 1)).toBe(false);
  });

  it('shares the sku id and reads it back', () => {
    expect(shareVariantParam(P, { 'Màu sắc': 1, 'Phiên bản': 0 })).toBe('103');
    expect(shareVariantParam(P, { 'Màu sắc': 1, 'Phiên bản': null })).toBeNull();
    expect(decodeShared(P, '103')).toEqual({ 'Màu sắc': 1, 'Phiên bản': 0 });
    // SKU hết hàng / không tồn tại → không chọn sẵn
    expect(decodeShared(P, '102')).toEqual({ 'Màu sắc': null, 'Phiên bản': null });
    expect(decodeShared(P, null)).toEqual({ 'Màu sắc': null, 'Phiên bản': null });
  });

  it('still reads the legacy index format (legacy rule: stock-controlling group needs option stock)', () => {
    expect(decodeShared(P, '1-0')).toEqual({ 'Màu sắc': null, 'Phiên bản': 0 });
  });
});

describe('viewSourceOf', () => {
  const O = 'https://genie.vn';
  it('maps share links, referrers and direct visits', async () => {
    const { viewSourceOf } = await import('./product.rules');
    expect(viewSourceOf('share', `${O}/search?q=a`, O)).toBe('EXTERNAL_LINK');
    expect(viewSourceOf(null, `${O}/search?q=ao`, O)).toBe('SEARCH');
    expect(viewSourceOf(null, `${O}/c/dien-tu`, O)).toBe('SEARCH');
    expect(viewSourceOf(null, `${O}/shop/15`, O)).toBe('SHOP_PAGE');
    expect(viewSourceOf(null, `${O}/flash-sale`, O)).toBe('PROMOTION');
    expect(viewSourceOf(null, `${O}/`, O)).toBe('HOME_FEED');
    expect(viewSourceOf(null, 'https://facebook.com/x', O)).toBe('EXTERNAL_LINK');
    expect(viewSourceOf(null, '', O)).toBe('HOME_FEED');
  });
});
