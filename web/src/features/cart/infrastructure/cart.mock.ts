// Giỏ mẫu của người dùng đã đăng nhập (prototype index.html seedCart), dạng DTO.
import { SEED_PRODUCTS } from '@/shared/api/mock/seed';
import { productImg } from '@/shared/lib/assets';
import type { CartLineDto } from './cart.dto';

const mk = (
  productId: number,
  variant: string,
  price: number,
  qty: number,
  stock: number,
  snapshot: number | null = null,
): CartLineDto => {
  const p = SEED_PRODUCTS.find((x) => x.id === productId);
  if (!p) throw new Error(`seed thiếu sản phẩm ${productId}`);
  return {
    key: `${productId}|${variant}`,
    product_id: p.id,
    product_slug: p.slug,
    shop_id: p.shop_id,
    title: p.title,
    image: productImg(p.image),
    variant_label: variant,
    unit_price: price,
    original_price: p.original_price,
    snapshot_price: snapshot,
    quantity: qty,
    stock,
  };
};

export const SEED_CART: CartLineDto[] = [
  mk(1, 'Đen', 389000, 1, 38),
  mk(18, 'Blue switch', 459000, 1, 25, 429000),
  mk(2, 'Đen', 690000, 1, 20),
  mk(3, 'Trắng / 42', 259000, 2, 12),
  mk(14, 'Đen / 37', 320000, 1, 18, 289000),
  mk(13, '30ml', 178000, 1, 50),
  mk(12, 'Full set', 235000, 1, 30),
  mk(19, '50ml', 420000, 1, 22),
  mk(15, 'Xanh / L', 245000, 1, 40),
  mk(16, 'Trắng / M', 89000, 2, 100),
  mk(10, 'Trắng', 149000, 1, 60),
];
