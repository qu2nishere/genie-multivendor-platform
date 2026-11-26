import { beforeEach, describe, expect, it, vi } from 'vitest';

const api = vi.hoisted(() => ({
  mergeCart: vi.fn(),
  fetchCart: vi.fn(async () => []),
}));
const storage = vi.hoisted(() => ({
  items: [] as { product_variant_id: number; quantity: number; added_at: string }[],
  cleared: 0,
}));

vi.mock('../infrastructure/cart.api', () => ({
  mergeCart: api.mergeCart,
  fetchCart: api.fetchCart,
  addCartItem: vi.fn(),
  addCartItemsBatch: vi.fn(),
  removeCartItems: vi.fn(),
  updateCartItem: vi.fn(),
}));
const session = vi.hoisted(() => new Map<string, unknown>());
vi.mock('@/shared/lib/storage', () => ({
  store: {
    get: (k: string) => session.get(k) ?? null,
    set: (k: string, v: unknown) => session.set(k, v),
    remove: (k: string) => session.delete(k),
  },
}));
vi.mock('../infrastructure/guestCart.storage', () => ({
  readGuestCart: () => ({ version: 1, expires_at: '', items: storage.items }),
  writeGuestCart: vi.fn(),
  clearGuestCart: () => {
    storage.cleared += 1;
    storage.items = [];
  },
}));

import { $guestItems } from './cart.store';
import { mergeGuestCart, takeMergeNotice } from './mergeGuestCart';

describe('mergeGuestCart', () => {
  beforeEach(() => {
    api.mergeCart.mockReset();
    storage.cleared = 0;
    storage.items = [{ product_variant_id: 100, quantity: 2, added_at: '' }];
    $guestItems.set(storage.items);
  });

  it('giỏ khách rỗng → không gọi API', async () => {
    storage.items = [];
    expect(await mergeGuestCart()).toBe(0);
    expect(api.mergeCart).not.toHaveBeenCalled();
  });

  it('gộp xong thì xóa giỏ khách và tải lại giỏ tài khoản', async () => {
    api.mergeCart.mockResolvedValue({ merged: 1, skipped: 0, adjusted: 0 });
    expect(await mergeGuestCart()).toBe(1);
    expect(api.mergeCart).toHaveBeenCalledWith([
      { product_variant_id: 100, quantity: 2, added_at: '' },
    ]);
    expect(storage.cleared).toBe(1);
    expect($guestItems.get()).toEqual([]);
    expect(api.fetchCart).toHaveBeenCalled();
  });

  it('giữ kết quả gộp để báo toast ở trang kế tiếp, đọc một lần', async () => {
    api.mergeCart.mockResolvedValue({ merged: 1, skipped: 2, adjusted: 1 });
    await mergeGuestCart();
    expect(takeMergeNotice()).toEqual({ merged: 1, skipped: 2, adjusted: 1 });
    expect(takeMergeNotice()).toBeNull();
  });

  it('lỗi mạng → giữ giỏ khách để thử lại lần sau', async () => {
    api.mergeCart.mockRejectedValue(new Error('net'));
    expect(await mergeGuestCart()).toBe(0);
    expect(storage.cleared).toBe(0);
  });
});
