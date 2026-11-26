// Gộp giỏ khách vào giỏ tài khoản ngay sau đăng nhập (LUU_TRU_DU_LIEU 6.3; API_SPEC 6.7), rồi xóa
// khóa genie.cart.guest. Trang chuyển hướng ngay sau đăng nhập nên kết quả được giữ ở sessionStorage
// (genie.cart.merge_notice) để header báo toast ở trang kế tiếp (plan C2 O03.02). Trả số dòng đã gộp.
import { store } from '@/shared/lib/storage';
import type { MergeResult } from '../domain/cart.types';
import { mergeCart } from '../infrastructure/cart.api';
import { clearGuestCart, readGuestCart } from '../infrastructure/guestCart.storage';
import { $guestItems, loadCart } from './cart.store';

export async function mergeGuestCart(): Promise<number> {
  const items = readGuestCart().items;
  if (!items.length) return 0;
  try {
    const result = await mergeCart(items);
    clearGuestCart();
    $guestItems.set([]);
    if (result.merged + result.skipped > 0) store.set('cartMergeNotice', result);
    await loadCart(true);
    return result.merged;
  } catch {
    // giữ giỏ khách để thử lại lần đăng nhập sau
    return 0;
  }
}

/** Lấy (và xóa) kết quả gộp giỏ đang chờ báo; không có → null. */
export function takeMergeNotice(): MergeResult | null {
  const n = store.get<MergeResult>('cartMergeNotice');
  if (n) store.remove('cartMergeNotice');
  return n && typeof n.merged === 'number' ? n : null;
}
