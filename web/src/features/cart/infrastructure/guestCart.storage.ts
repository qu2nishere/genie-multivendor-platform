// localStorage genie.cart.guest (LUU_TRU_DU_LIEU 6.3): chỉ SKU + số lượng, không giá / tên.
import { readJson, store, writeJson } from '@/shared/lib/storage';
import { emptyGuestCart, isGuestCartExpired } from '../domain/cart.rules';
import type { GuestCart } from '../domain/cart.types';

const KEY = 'cart.guest';

/** Đọc giỏ khách; hỏng / hết hạn → giỏ rỗng (và xóa khóa cũ). */
export function readGuestCart(now = new Date()): GuestCart {
  const c = readJson<GuestCart>('local', KEY, 1);
  if (!c || !Array.isArray(c.items)) return emptyGuestCart(now);
  if (isGuestCartExpired(c, now)) {
    store.remove('cartGuest');
    return emptyGuestCart(now);
  }
  return c;
}

export function writeGuestCart(c: GuestCart): void {
  writeJson('local', KEY, c);
}

export function clearGuestCart(): void {
  store.remove('cartGuest');
}
