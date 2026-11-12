// Danh sách yêu thích (GET/POST/DELETE /me/wishlist). Mock: lưu id trong localStorage genie.wishlist
// như prototype (lz_wishlist, mặc định p2, p13).
import { api } from '@/shared/api/http';
import { mockDelay, USE_MOCK } from '@/shared/api/mock';
import { store } from '@/shared/lib/storage';

const DEFAULT_MOCK = [2, 13];

const readMock = (): number[] => {
  const v = store.get<number[]>('wishlist');
  return Array.isArray(v) ? v.filter((x) => Number.isInteger(x)) : DEFAULT_MOCK;
};

export async function fetchWishlistIds(): Promise<number[]> {
  if (USE_MOCK) return readMock();
  return api.get<number[]>('/me/wishlist/ids');
}

export async function setWishlisted(productId: number, on: boolean): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(80);
    const cur = readMock();
    store.set(
      'wishlist',
      on ? [...new Set([...cur, productId])] : cur.filter((x) => x !== productId),
    );
    return;
  }
  if (on) await api.post('/me/wishlist', { product_id: productId });
  else await api.delete(`/me/wishlist/${productId}`);
}
