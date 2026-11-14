import { describe, expect, it } from 'vitest';
import {
  addGuestItem,
  emptyGuestCart,
  isGuestCartExpired,
  lineKeyOf,
  MAX_GUEST_LINES,
  mergeLine,
} from './cart.rules';
import type { CartLineInput } from './cart.types';

const now = new Date('2026-11-13T08:00:00Z');
const input = (over: Partial<CartLineInput> = {}): CartLineInput => ({
  variantId: 100,
  productId: 1,
  productSlug: 'tai-nghe',
  shopId: 1,
  title: 'Tai nghe',
  image: '/img/x.jpg',
  variantLabel: 'Đen / Tiêu chuẩn',
  price: 389000,
  originalPrice: 799000,
  qty: 2,
  stock: 3,
  ...over,
});

describe('cart.rules', () => {
  it('khóa dòng', () => {
    expect(lineKeyOf(1, 'Đen')).toBe('1|Đen');
  });

  it('cộng dồn cùng phân loại, không vượt tồn', () => {
    let lines = mergeLine([], input());
    expect(lines).toHaveLength(1);
    expect(lines[0].qty).toBe(2);
    lines = mergeLine(lines, input({ qty: 5 }));
    expect(lines).toHaveLength(1);
    expect(lines[0].qty).toBe(3);
    lines = mergeLine(lines, input({ variantLabel: 'Trắng / Tiêu chuẩn', qty: 1 }));
    expect(lines).toHaveLength(2);
  });

  it('giỏ khách: cộng dồn, gia hạn 30 ngày, tối đa 50 dòng', () => {
    let r = addGuestItem(emptyGuestCart(now), 100, 1, now);
    r = addGuestItem(r.cart, 100, 2, now, 2);
    expect(r.ok).toBe(true);
    expect(r.cart.items).toEqual([
      { product_variant_id: 100, quantity: 2, added_at: now.toISOString() },
    ]);
    expect(r.cart.expires_at).toBe('2026-12-13T08:00:00.000Z');

    let full = emptyGuestCart(now);
    for (let i = 0; i < MAX_GUEST_LINES; i++) full = addGuestItem(full, i, 1, now).cart;
    const over = addGuestItem(full, 999, 1, now);
    expect(over.ok).toBe(false);
    expect(over.cart.items).toHaveLength(MAX_GUEST_LINES);
  });

  it('giỏ khách hết hạn thì bỏ', () => {
    const old = {
      ...emptyGuestCart(new Date('2026-01-01T00:00:00Z')),
      items: [{ product_variant_id: 1, quantity: 1, added_at: '' }],
    };
    expect(isGuestCartExpired(old, now)).toBe(true);
    expect(addGuestItem(old, 5, 1, now).cart.items.map((i) => i.product_variant_id)).toEqual([5]);
  });
});
