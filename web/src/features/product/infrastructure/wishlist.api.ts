// Sản phẩm yêu thích (API_SPEC 3.10 — UC-CAT-08): GET /me/wishlist/product-ids, PUT / DELETE /me/wishlist/{id}
// (idempotent). Nối API thật khi PUBLIC_REAL_API có `wishlist`. Mock: lưu id trong localStorage genie.wishlist
// như prototype (lz_wishlist, mặc định p2, p13).
import { api, request } from '@/shared/api/http';
import { mockDelay } from '@/shared/api/mock';
import { isMocked } from '@/shared/config/env';
import { store } from '@/shared/lib/storage';
import type { WishlistEntry } from '../domain/product.types';
import { listProductsByIds } from './product.api';
import type { WishlistItemDto } from './wishlist.dto';
import { entryFromSummary, toWishlistEntry } from './wishlist.mapper';

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

/** Trang "Sản phẩm yêu thích": dòng (mới thích trước) + tổng số cho tiêu đề (`summary.total`). */
export interface WishlistPage {
  items: WishlistEntry[];
  total: number;
}

/** GET /me/wishlist — lấy tối đa 50 dòng mới nhất (đủ cho màn Tài khoản; "xem thêm" làm khi cần). */
export async function listWishlist(): Promise<WishlistPage> {
  if (WISHLIST_MOCK) {
    const ids = readMock();
    const products = await listProductsByIds(ids);
    const byId = new Map(products.map((p) => [p.id, p]));
    const items = [...ids]
      .reverse()
      .map((id) => byId.get(id))
      .filter((p): p is NonNullable<typeof p> => !!p)
      .map(entryFromSummary);
    return { items, total: ids.length };
  }
  const res = await request<WishlistItemDto[]>('/me/wishlist', { query: { limit: 50 } });
  return {
    items: res.data.map(toWishlistEntry),
    total: Number((res.summary ?? {}).total ?? res.data.length),
  };
}
