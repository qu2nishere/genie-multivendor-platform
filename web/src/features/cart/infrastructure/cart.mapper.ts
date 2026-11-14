import type { CartLine } from '../domain/cart.types';
import type { CartLineDto } from './cart.dto';

export const toCartLine = (d: CartLineDto): CartLine => ({
  key: d.key,
  productId: d.product_id,
  productSlug: d.product_slug,
  shopId: d.shop_id,
  title: d.title,
  image: d.image,
  variantLabel: d.variant_label,
  price: d.unit_price,
  originalPrice: d.original_price,
  snapshotPrice: d.snapshot_price,
  qty: d.quantity,
  stock: d.stock,
});
