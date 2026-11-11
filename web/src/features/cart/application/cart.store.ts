// Giỏ dùng chung giữa các island trên cùng trang (TS 5.3): số trên header ↔ nút "Thêm vào giỏ".
import { atom, computed } from 'nanostores';
import type { CartLine } from '../domain/cart.types';
import { fetchCart } from '../infrastructure/cart.api';

export const $cartLines = atom<CartLine[]>([]);
export const $cartStatus = atom<'idle' | 'loading' | 'ready' | 'error'>('idle');
/** Prototype: số trên biểu tượng giỏ = số dòng (cartCount = cart.length). */
export const $cartCount = computed($cartLines, (lines) => lines.length);

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
