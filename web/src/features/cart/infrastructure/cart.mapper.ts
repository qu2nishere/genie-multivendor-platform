import { lineKeyOf } from '../domain/cart.rules';
import type { CartLine, CartShop, VariantChoice } from '../domain/cart.types';
import type { CartDto, CartItemDto, CartShopDto, VariantChoiceDto } from './cart.dto';

export const toCartShop = (d: CartShopDto): CartShop => ({
  id: d.id,
  name: d.name,
  authentic: d.is_mall,
});

export const toCartLine = (d: CartItemDto, shop: CartShopDto): CartLine => ({
  id: d.id,
  key: lineKeyOf(d.product_id, d.variant_name),
  productId: d.product_id,
  variantId: d.variant_id,
  productSlug: d.product_slug,
  shop: toCartShop(shop),
  title: d.product_name,
  image: d.image_url,
  category: d.category_name,
  variantLabel: d.variant_name,
  price: d.unit_price,
  originalPrice: d.original_price,
  snapshotPrice: d.price_at_add,
  qty: d.quantity,
  stock: d.stock,
  available: d.is_available,
  unavailableReason: d.unavailable_reason,
});

/** Giỏ nhóm theo shop (6.1) → danh sách dòng phẳng (UI tự nhóm lại bằng groupByShop). */
export const toCartLines = (d: CartDto): CartLine[] =>
  d.shops.flatMap((g) => g.items.map((i) => toCartLine(i, g.shop)));

export const toVariantChoice = (d: VariantChoiceDto): VariantChoice => ({
  groups: d.option_groups,
  variants: d.variants.map((v) => ({
    id: v.id,
    options: v.options,
    price: v.price,
    stock: v.stock,
  })),
  image: d.image_url,
});
