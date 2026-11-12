// Yêu thích dùng chung mọi island (tim trên thẻ, trang sản phẩm, WishlistPanel) — thay LZ.wishlist.
import { atom } from 'nanostores';
import { onStorageChange } from '@/shared/lib/storage';
import { fetchWishlistIds, setWishlisted } from '../infrastructure/wishlist.api';

export const $wishlist = atom<Set<number>>(new Set());
let loaded: Promise<void> | null = null;

export function loadWishlist(): Promise<void> {
  if (loaded) return loaded;
  loaded = fetchWishlistIds()
    .then((ids) => $wishlist.set(new Set(ids)))
    .catch(() => {
      loaded = null;
    });
  if (typeof window !== 'undefined') {
    // Tab khác đổi danh sách → đồng bộ lại
    onStorageChange('wishlist', () => {
      loaded = null;
      void loadWishlist();
    });
  }
  return loaded;
}

/** Bật / tắt yêu thích; cập nhật lạc quan, lỗi thì hoàn tác. Trả trạng thái mới. */
export async function toggleWishlist(productId: number): Promise<boolean> {
  const before = $wishlist.get();
  const on = !before.has(productId);
  const next = new Set(before);
  if (on) next.add(productId);
  else next.delete(productId);
  $wishlist.set(next);
  try {
    await setWishlisted(productId, on);
    return on;
  } catch (e) {
    $wishlist.set(before);
    throw e;
  }
}
