// Giỏ dùng chung giữa các island trên cùng trang (TS 5.3): số trên header ↔ nút "Thêm vào giỏ".
import { atom, computed } from 'nanostores';
import { addGuestItem } from '../domain/cart.rules';
import type { CartLine, CartLineInput, GuestCartItem } from '../domain/cart.types';
import { addCartItem, fetchCart } from '../infrastructure/cart.api';
import { readGuestCart, writeGuestCart } from '../infrastructure/guestCart.storage';

export const $cartLines = atom<CartLine[]>([]);
export const $cartStatus = atom<'idle' | 'loading' | 'ready' | 'error'>('idle');
/** Giỏ khách vãng lai (chỉ SKU + số lượng, LUU_TRU_DU_LIEU 6.3). */
export const $guestItems = atom<GuestCartItem[]>([]);
/** Prototype: số trên biểu tượng giỏ = số dòng (cartCount = cart.length); khách → số dòng giỏ khách. */
export const $cartCount = computed(
  [$cartLines, $guestItems],
  (lines, guest) => lines.length || guest.length,
);

let loading: Promise<void> | null = null;

/** Nạp giỏ một lần cho mọi island (gọi lại an toàn). */
export function loadCart(force = false): Promise<void> {
  if (loading && !force) return loading;
  $cartStatus.set('loading');
  loading = fetchCart()
    .then((lines) => {
      $cartLines.set(lines);
      $cartStatus.set('ready');
    })
    .catch(() => {
      $cartStatus.set('error');
      loading = null;
    });
  return loading;
}

export function setCartLines(lines: CartLine[]): void {
  $cartLines.set(lines);
}

/** Khách chưa đăng nhập: đọc giỏ khách từ localStorage (số trên header). */
export function loadGuestCart(): void {
  $guestItems.set(readGuestCart().items);
}

export type AddResult =
  { ok: true; key: string } | { ok: false; reason: 'guest_full' | 'error'; error?: unknown };

/**
 * Thêm vào giỏ: đã đăng nhập → API; khách → giỏ khách trong trình duyệt (gộp vào giỏ tài khoản
 * sau khi đăng nhập — G10).
 */
export async function addToCart(input: CartLineInput, authenticated: boolean): Promise<AddResult> {
  const key = `${input.productId}|${input.variantLabel}`;
  if (!authenticated) {
    const r = addGuestItem(
      readGuestCart(),
      input.variantId,
      input.qty,
      new Date(),
      Math.max(1, input.stock),
    );
    if (!r.ok) return { ok: false, reason: 'guest_full' };
    writeGuestCart(r.cart);
    $guestItems.set(r.cart.items);
    return { ok: true, key };
  }
  try {
    $cartLines.set(await addCartItem(input));
    $cartStatus.set('ready');
    return { ok: true, key };
  } catch (error) {
    return { ok: false, reason: 'error', error };
  }
}
