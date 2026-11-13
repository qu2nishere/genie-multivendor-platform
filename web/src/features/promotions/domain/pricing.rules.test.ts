import { describe, expect, it } from 'vitest';
import { toVoucher } from '../infrastructure/voucher.mapper';
import { VOUCHER_DTOS } from '../infrastructure/voucher.mock';
import {
  bestVouchers,
  eligibleBase,
  emptySelection,
  extraSaving,
  priceCart,
  pruneVoucherSelection,
  selectionFromCodes,
  selectionToCodes,
  usableVouchers,
  voucherCheck,
  voucherDiscount,
  voucherLines,
} from './pricing.rules';
import type { PricingContext, PricingLine, VoucherSelection } from './pricing.types';
import type { Voucher } from './voucher.types';

// Shop mẫu: 1 TechZone (25k, miễn phí từ 199k), 2 Shoe (18k), 3 Boutique (20k), 4 Home (30k),
// 5 Beauty (16k, miễn phí từ 300k) — đúng prototype data.jsx V.
const POLICY: Record<number, { fee: number; freeFrom: number | null }> = {
  1: { fee: 25000, freeFrom: 199000 },
  2: { fee: 18000, freeFrom: null },
  3: { fee: 20000, freeFrom: null },
  4: { fee: 30000, freeFrom: null },
  5: { fee: 16000, freeFrom: 300000 },
};
const ship = (shopId: number, lines: PricingLine[]) => {
  const p = POLICY[shopId];
  const sub = lines.filter((l) => l.shopId === shopId).reduce((n, l) => n + l.price * l.qty, 0);
  return p.freeFrom != null && sub >= p.freeFrom ? 0 : p.fee;
};
const vouchers = VOUCHER_DTOS.map(toVoucher);
const ctx: PricingContext = { vouchers, ship };
const V = (code: string): Voucher => {
  const v = vouchers.find((x) => x.code === code);
  if (!v) throw new Error(code);
  return v;
};
const L = (key: string, shopId: number, category: string, price: number, qty = 1): PricingLine => ({
  key,
  shopId,
  category,
  price,
  qty,
});

// Giỏ mẫu của prototype (11 dòng, 5 shop)
const SEED: PricingLine[] = [
  L('headphones', 1, 'Điện tử', 389000),
  L('keyboard', 1, 'Điện tử', 459000),
  L('watch', 1, 'Điện tử', 690000),
  L('sneakers', 2, 'Thời trang', 259000, 2),
  L('heels', 2, 'Thời trang', 320000),
  L('serum', 5, 'Làm đẹp', 178000),
  L('cosmetics', 5, 'Làm đẹp', 235000),
  L('perfume', 5, 'Làm đẹp', 420000),
  L('jacket', 3, 'Thời trang', 245000),
  L('tshirt', 3, 'Thời trang', 89000, 2),
  L('lamp', 4, 'Nhà cửa', 149000),
];
const none = { use: false, balance: 0 };

describe('voucher — điều kiện', () => {
  it('lọc dòng theo shop / ngành / loại trừ', () => {
    expect(voucherLines(V('TECH15'), SEED).map((l) => l.key)).toEqual([
      'headphones',
      'keyboard',
      'watch',
    ]);
    expect(voucherLines(V('TECH10'), SEED).every((l) => l.category === 'Điện tử')).toBe(true);
    expect(voucherLines(V('STYLE20K'), SEED).some((l) => l.category === 'Điện tử')).toBe(false);
  });

  it('chưa đạt tối thiểu / không có dòng phù hợp / chưa hiệu lực', () => {
    const small = [L('lamp', 4, 'Nhà cửa', 149000)];
    expect(voucherCheck(V('GENIE30K'), small, ctx)).toMatchObject({
      ok: false,
      reason: 'min_order',
      minOrder: 199000,
    });
    expect(voucherCheck(V('TECH15'), small, ctx)).toMatchObject({ ok: false, reason: 'no_items' });
    expect(voucherCheck(V('SALE1212'), SEED, ctx)).toMatchObject({
      ok: false,
      reason: 'not_active',
    });
    expect(voucherDiscount(V('GENIE30K'), small, ctx)).toBe(0);
  });

  it('cố định / phần trăm có trần / freeship có trần', () => {
    const tech = SEED.slice(0, 3);
    expect(voucherDiscount(V('GENIE30K'), tech, ctx)).toBe(30000);
    expect(voucherDiscount(V('TECH15'), tech, ctx)).toBe(50000); // 15% của 1.538.000 vượt trần 50k
    expect(voucherDiscount(V('SHIPMAX'), SEED, ctx)).toBe(50000); // phí ship 68k, trần 50k
    expect(voucherDiscount(V('FREESHIP30'), SEED, ctx)).toBe(30000);
    expect(voucherDiscount(V('SHIPMAX'), tech, ctx)).toBe(0); // shop đã tự miễn phí → 0₫
  });

  it('voucher sàn tính trên phần còn lại sau voucher shop (tầng 2)', () => {
    const sel: VoucherSelection = { ...emptySelection(), shop: { 1: V('TECH15').id } };
    expect(eligibleBase(V('TECH10'), SEED, ctx)).toBe(1538000);
    expect(eligibleBase(V('TECH10'), SEED, ctx, sel)).toBe(1488000);
    expect(voucherDiscount(V('TECH10'), SEED, ctx, sel)).toBe(148800);
  });
});

describe('priceCart — xếp chồng', () => {
  it('khớp số liệu giỏ mẫu của prototype (bộ mã tốt nhất + Điểm tắt)', () => {
    const sel = bestVouchers(SEED, ctx);
    expect(sel).toEqual({
      platform: V('TECH10').id,
      freeship: V('SHIPMAX').id,
      shop: {
        1: V('TECH15').id,
        2: V('SHOE10').id,
        5: V('BEAUTY50').id,
        3: V('BTQ20K').id,
        4: V('HOME15K').id,
      },
    });
    const p = priceCart(SEED, sel, ctx, none);
    expect(p.subtotal).toBe(3781000);
    expect(p.shipping).toBe(68000);
    expect(p.shopByShop).toEqual({ 1: 50000, 2: 30000, 3: 20000, 4: 15000, 5: 50000 });
    expect(p.platformDiscount).toBe(148800);
    expect(p.freeshipDiscount).toBe(50000);
    expect(p.discountTotal).toBe(363800);
    expect(p.total).toBe(3485200);
    // phân bổ voucher sàn theo dòng, phần dư dồn dòng cuối
    expect(p.perLine).toEqual({ headphones: 37635, keyboard: 44408, watch: 66757 });
    // freeship chia theo phí ship từng shop: 18k / 20k / 30k
    expect(Object.values(p.shipCutByShop).reduce((a, b) => a + b, 0)).toBe(50000);
    expect(p.shipCutByShop[4]).toBe(50000 - p.shipCutByShop[2] - p.shipCutByShop[3]);
  });

  it('Điểm: tối đa 50% tiền hàng còn lại, không vượt số dư', () => {
    const lines = [L('lamp', 4, 'Nhà cửa', 149000)];
    const p = priceCart(lines, emptySelection(), ctx, { use: true, balance: 1250 });
    expect(p.pointsCap).toBe(74500);
    expect(p.pointsDiscount).toBe(1250);
    expect(p.total).toBe(149000 + 30000 - 1250);
    const rich = priceCart(lines, emptySelection(), ctx, { use: true, balance: 1_000_000 });
    expect(rich.pointsDiscount).toBe(74500);
    expect(
      priceCart(lines, emptySelection(), ctx, { use: false, balance: 1250 }).pointsDiscount,
    ).toBe(0);
  });

  it('tổng giảm tầng shop + sàn không vượt 85% tiền hàng', () => {
    const big: Voucher = { ...V('GENIE100K'), id: 'big', minOrder: 0, value: 100000 };
    const lines = [L('mug', 4, 'Nhà cửa', 59000)];
    const p = priceCart(
      lines,
      { ...emptySelection(), platform: 'big' },
      { ...ctx, vouchers: [...vouchers, big] },
      none,
    );
    expect(p.platformDiscount).toBe(Math.floor(59000 * 0.85));
  });

  it('không chọn gì → chỉ tiền hàng + ship', () => {
    const p = priceCart(SEED, emptySelection(), ctx, none);
    expect(p.discountTotal).toBe(0);
    expect(p.total).toBe(3781000 + 68000);
    expect(p.platformVoucher).toBeNull();
  });

  it('giỏ rỗng', () => {
    expect(priceCart([], emptySelection(), ctx, none)).toMatchObject({ subtotal: 0, total: 0 });
    expect(bestVouchers([], ctx)).toEqual(emptySelection());
  });
});

describe('lựa chọn voucher', () => {
  it('bỏ voucher hết hợp lệ khi bỏ chọn dòng', () => {
    const sel = bestVouchers(SEED, ctx);
    const only = [L('lamp', 4, 'Nhà cửa', 149000)];
    expect(pruneVoucherSelection(sel, only, ctx)).toEqual({
      platform: null,
      freeship: V('FREESHIP30').id === sel.freeship ? sel.freeship : null,
      shop: { 4: V('HOME15K').id },
    });
  });

  it('đổi qua lại dạng mã để lưu nháp', () => {
    const sel = bestVouchers(SEED, ctx);
    const codes = selectionToCodes(sel, vouchers);
    expect(codes).toContain('TECH10');
    expect(selectionFromCodes(codes, vouchers)).toEqual(sel);
  });

  it('gợi ý tiết kiệm thêm khi chưa dùng bộ tốt nhất', () => {
    const r = extraSaving(SEED, emptySelection(), ctx);
    expect(r.saving).toBe(363800);
    expect(extraSaving(SEED, r.best, ctx).saving).toBe(0);
  });

  it('kho dùng được: đã lưu và đang hiệu lực', () => {
    expect(usableVouchers(vouchers).map((v) => v.code)).not.toContain('SALE1212');
    expect(usableVouchers(vouchers).map((v) => v.code)).not.toContain('MAY15');
  });
});
