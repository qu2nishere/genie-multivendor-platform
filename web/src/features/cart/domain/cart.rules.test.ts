import { describe, expect, it } from 'vitest';
import {
  addGuestItem,
  changeVariant,
  checkState,
  decideQty,
  emptyGuestCart,
  groupByShop,
  initialSelection,
  isGuestCartExpired,
  lineKeyOf,
  linesSubtotal,
  lineSubtotal,
  MAX_GUEST_LINES,
  mergeLine,
  priceChange,
  pruneSelection,
  setGuestQty,
  toggleGroup,
  toggleKey,
} from './cart.rules';
import type { CartLine, CartLineInput } from './cart.types';

const now = new Date('2026-11-13T08:00:00Z');
const shopA = { id: 1, name: 'TechZone', authentic: true };
const shopB = { id: 2, name: 'Shoe', authentic: false };

const line = (over: Partial<CartLine> = {}): CartLine => ({
  id: 1,
  key: '1|Đen',
  productId: 1,
  variantId: 100,
  productSlug: 'tai-nghe',
  shop: shopA,
  title: 'Tai nghe',
  image: '/img/x.jpg',
  category: 'Điện tử',
  variantLabel: 'Đen',
  price: 389000,
  originalPrice: 799000,
  snapshotPrice: null,
  qty: 1,
  stock: 3,
  available: true,
  unavailableReason: null,
  ...over,
});

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
const make = (i: CartLineInput): CartLine =>
  line({ key: lineKeyOf(i.productId, i.variantLabel), variantLabel: i.variantLabel, qty: i.qty });

describe('cart.rules — tính tiền & nhóm', () => {
  it('khóa dòng, thành tiền', () => {
    expect(lineKeyOf(1, 'Đen')).toBe('1|Đen');
    expect(lineSubtotal({ price: 89000, qty: 2 })).toBe(178000);
    expect(linesSubtotal([line(), line({ price: 10000, qty: 3 })])).toBe(419000);
  });

  it('nhóm theo shop giữ thứ tự xuất hiện', () => {
    const g = groupByShop([
      line({ key: 'a' }),
      line({ key: 'b', shop: shopB }),
      line({ key: 'c' }),
    ]);
    expect(g.map((x) => x.shop.id)).toEqual([1, 2]);
    expect(g[0].lines.map((l) => l.key)).toEqual(['a', 'c']);
  });

  it('báo giá tăng / giảm so với lúc thêm', () => {
    expect(priceChange({ price: 459000, snapshotPrice: 429000 })).toBe('up');
    expect(priceChange({ price: 400000, snapshotPrice: 429000 })).toBe('down');
    expect(priceChange({ price: 1, snapshotPrice: 1 })).toBeNull();
    expect(priceChange({ price: 1, snapshotPrice: null })).toBeNull();
  });

  it('đổi số lượng: 0 → hỏi xóa, vượt tồn → báo, còn lại đặt số mới', () => {
    expect(decideQty({ stock: 3 }, 0)).toEqual({ kind: 'confirm_remove' });
    expect(decideQty({ stock: 3 }, 4)).toEqual({ kind: 'over_stock', stock: 3 });
    expect(decideQty({ stock: 3 }, 3)).toEqual({ kind: 'set', qty: 3 });
  });
});

describe('cart.rules — chọn dòng', () => {
  it('chọn / bỏ từng dòng và cả shop', () => {
    let s = toggleKey(new Set(), 'a');
    expect([...s]).toEqual(['a']);
    s = toggleGroup(s, ['a', 'b']);
    expect(checkState(s, ['a', 'b'])).toBe('all');
    s = toggleGroup(s, ['a', 'b']);
    expect(checkState(s, ['a', 'b'])).toBe('none');
    expect(checkState(new Set(['a']), ['a', 'b'])).toBe('some');
  });

  it('Mua ngay chỉ chọn dòng vừa thêm; mặc định chọn dòng mua được', () => {
    const lines = [line({ key: 'a' }), line({ key: 'b', available: false })];
    expect([...initialSelection(lines, 'a')]).toEqual(['a']);
    expect([...initialSelection(lines, null)]).toEqual(['a']);
    expect([...initialSelection(lines, 'zzz')]).toEqual(['a']);
    expect([...pruneSelection(new Set(['a', 'x']), lines)]).toEqual(['a']);
  });
});

describe('cart.rules — cộng dồn & đổi phân loại', () => {
  it('cộng dồn cùng phân loại, không vượt tồn', () => {
    let lines = mergeLine([], input(), make);
    expect(lines).toHaveLength(1);
    expect(lines[0].qty).toBe(2);
    lines = mergeLine(lines, input({ qty: 5 }), make);
    expect(lines).toHaveLength(1);
    expect(lines[0].qty).toBe(3);
    lines = mergeLine(lines, input({ variantLabel: 'Trắng / Tiêu chuẩn', qty: 1 }), make);
    expect(lines).toHaveLength(2);
  });

  it('đổi sang phân loại mới: đổi khóa, giá, tồn', () => {
    const r = changeVariant([line({ qty: 2, snapshotPrice: 1 })], '1|Đen', {
      variantId: 110,
      variantLabel: 'Trắng / Tiêu chuẩn',
      price: 400000,
      stock: 12,
    });
    expect(r.key).toBe('1|Trắng / Tiêu chuẩn');
    expect(r.lines[0]).toMatchObject({
      variantId: 110,
      price: 400000,
      qty: 2,
      snapshotPrice: null,
    });
  });

  it('đổi trùng phân loại đã có → gộp số lượng, bỏ dòng cũ', () => {
    const r = changeVariant(
      [line({ key: '1|Đen', qty: 1 }), line({ id: 2, key: '1|Trắng', qty: 2, stock: 10 })],
      '1|Đen',
      { variantId: 110, variantLabel: 'Trắng', price: 1, stock: 10 },
    );
    expect(r.lines).toHaveLength(1);
    expect(r.lines[0]).toMatchObject({ key: '1|Trắng', qty: 3 });
  });
});

describe('cart.rules — giỏ khách', () => {
  it('cộng dồn, gia hạn 30 ngày, tối đa 50 dòng', () => {
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

  it('đổi số lượng / bỏ SKU giỏ khách', () => {
    const c = addGuestItem(emptyGuestCart(now), 100, 1, now).cart;
    expect(setGuestQty(c, 100, 4).items[0].quantity).toBe(4);
    expect(setGuestQty(c, 100, 0).items).toEqual([]);
  });
});
