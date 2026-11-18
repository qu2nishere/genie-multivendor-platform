// Sản phẩm yêu thích (API_SPEC 3.10 — UC-CAT-08): GET /me/wishlist/product-ids, PUT / DELETE /me/wishlist/{id}
// (idempotent). Nối API thật khi PUBLIC_REAL_API có `wishlist`. Mock: lưu id trong localStorage genie.wishlist
// như prototype (lz_wishlist, mặc định p2, p13).
import { api } from '@/shared/api/http';
import { mockDelay } from '@/shared/api/mock';
import { isMocked } from '@/shared/config/env';
import { store } from '@/shared/lib/storage';

const WISHLIST_MOCK = isMocked('wishlist');

const DEFAULT_MOCK = [2, 13];

const readMock = (): number[] => {
  const v = store.get<number[]>('wishlist');
  return Array.isArray(v) ? v.filter((x) => Number.isInteger(x)) : DEFAULT_MOCK;
};

export async function fetchWishlistIds(): Promise<number[]> {
  if (WISHLIST_MOCK) return readMock();
  return api.get<number[]>('/me/wishlist/product-ids');
}

export async function setWishlisted(productId: number, on: boolean): Promise<void> {
  if (WISHLIST_MOCK) {
    await mockDelay(80);
    const cur = readMock();
    store.set(
      'wishlist',
      on ? [...new Set([...cur, productId])] : cur.filter((x) => x !== productId),
    );
    return;
  }
  // PUT / DELETE idempotent — bấm đúp không làm lệch trạng thái; sản phẩm không còn bán → 404 GNE-CAT-4042
  if (on) await api.put(`/me/wishlist/${productId}`);
  else await api.delete(`/me/wishlist/${productId}`);
}
