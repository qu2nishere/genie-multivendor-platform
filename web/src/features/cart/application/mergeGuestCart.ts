// Gộp giỏ khách vào giỏ tài khoản ngay sau đăng nhập (LUU_TRU_DU_LIEU 6.3; API_SPEC 6.7), rồi xóa
// khóa genie.cart.guest. Trả số dòng đã gộp (0 = không có gì) để UI báo toast.
import { mergeCart } from '../infrastructure/cart.api';
import { clearGuestCart, readGuestCart } from '../infrastructure/guestCart.storage';
import { $guestItems, loadCart } from './cart.store';

export async function mergeGuestCart(): Promise<number> {
  const items = readGuestCart().items;
  if (!items.length) return 0;
  try {
    const { merged } = await mergeCart(items);
    clearGuestCart();
    $guestItems.set([]);
    await loadCart(true);
    return merged;
  } catch {
    // giữ giỏ khách để thử lại lần đăng nhập sau
    return 0;
  }
}
