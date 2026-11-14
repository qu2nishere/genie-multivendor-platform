// Giỏ hàng của người dùng đã đăng nhập (GET /me/cart, POST /me/cart/items). Mock khi USE_MOCK.
// Mở rộng (sửa số lượng, xóa, gộp giỏ khách) ở G10.02.
import { api } from '@/shared/api/http';
import { mockDelay, USE_MOCK } from '@/shared/api/mock';
import { mergeLine } from '../domain/cart.rules';
import type { CartLine, CartLineInput } from '../domain/cart.types';
import type { CartLineDto } from './cart.dto';
import { toCartLine } from './cart.mapper';
import { SEED_CART } from './cart.mock';

// Mock: giỏ trong bộ nhớ, bắt đầu từ giỏ mẫu của prototype
let mockLines: CartLine[] | null = null;
const mockCart = () => (mockLines ??= SEED_CART.map(toCartLine));

export async function fetchCart(): Promise<CartLine[]> {
  if (USE_MOCK) {
    await mockDelay();
    return [...mockCart()];
  }
  return (await api.get<CartLineDto[]>('/me/cart')).map(toCartLine);
}

/** Thêm SKU vào giỏ (cùng SKU cộng dồn phía server, cắt theo tồn) → trả giỏ mới. */
export async function addCartItem(input: CartLineInput): Promise<CartLine[]> {
  if (USE_MOCK) {
    await mockDelay(250);
    mockLines = mergeLine(mockCart(), input);
    return [...mockLines];
  }
  await api.post('/me/cart/items', { product_variant_id: input.variantId, quantity: input.qty });
  return fetchCart();
}
