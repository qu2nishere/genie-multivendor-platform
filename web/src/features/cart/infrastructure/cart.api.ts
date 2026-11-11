// Giỏ hàng của người dùng đã đăng nhập (GET /me/cart). Mock khi USE_MOCK. Mở rộng ở G10.02.
import { api } from '@/shared/api/http';
import { mockDelay, USE_MOCK } from '@/shared/api/mock';
import type { CartLine } from '../domain/cart.types';
import type { CartLineDto } from './cart.dto';
import { toCartLine } from './cart.mapper';
import { SEED_CART } from './cart.mock';

export async function fetchCart(): Promise<CartLine[]> {
  if (USE_MOCK) {
    await mockDelay();
    return SEED_CART.map(toCartLine);
  }
  return (await api.get<CartLineDto[]>('/me/cart')).map(toCartLine);
}
