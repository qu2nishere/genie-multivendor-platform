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
vi.mock('../infrastructure/guestCart.storage', () => ({
  readGuestCart: () => ({ version: 1, expires_at: '', items: storage.items }),
  writeGuestCart: vi.fn(),
  clearGuestCart: () => {
    storage.cleared += 1;
    storage.items = [];
  },
}));

import { $guestItems } from './cart.store';
import { mergeGuestCart } from './mergeGuestCart';

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
    api.mergeCart.mockResolvedValue({ merged: 1 });
    expect(await mergeGuestCart()).toBe(1);
    expect(api.mergeCart).toHaveBeenCalledWith([
      { product_variant_id: 100, quantity: 2, added_at: '' },
    ]);
    expect(storage.cleared).toBe(1);
    expect($guestItems.get()).toEqual([]);
    expect(api.fetchCart).toHaveBeenCalled();
  });

  it('lỗi mạng → giữ giỏ khách để thử lại lần sau', async () => {
    api.mergeCart.mockRejectedValue(new Error('net'));
    expect(await mergeGuestCart()).toBe(0);
    expect(storage.cleared).toBe(0);
  });
});
