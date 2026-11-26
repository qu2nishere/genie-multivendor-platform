import { lineKeyOf } from '../domain/cart.rules';
import type {
  CartLine,
  CartShop,
  GuestCartItem,
  MergeResult,
  VariantChoice,
} from '../domain/cart.types';
import type {
  CartDto,
  CartItemDto,
  CartShopDto,
  MergeResultDto,
  PublicVariantDto,
  VariantChoiceDto,
} from './cart.dto';

export const toCartShop = (d: CartShopDto): CartShop => ({
  id: d.id,
  name: d.name,
  authentic: d.is_mall,
});

export const toCartLine = (d: CartItemDto, shop: CartShopDto): CartLine => ({
  id: d.id,
  key: lineKeyOf(d.product_id, d.variant_name ?? ''),
  productId: d.product_id,
  variantId: d.variant_id,
  productSlug: d.product_slug,
  shop: toCartShop(shop),
  title: d.product_name,
  image: d.image_url ?? '',
  category: d.category_name,
  variantLabel: d.variant_name ?? '',
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

/** Giỏ khách: dòng công khai (5.18) + số lượng giữ ở trình duyệt; id dòng âm (chưa có cart_item_id). */
export const toGuestLines = (variants: PublicVariantDto[], items: GuestCartItem[]): CartLine[] => {
  const byId = new Map(variants.map((v) => [v.variant_id, v]));
  return items.flatMap((it, i): CartLine[] => {
    const v = byId.get(it.product_variant_id);
    if (!v) return [];
    const shop: CartShopDto = v.shop ?? { id: 0, name: '', logo_url: null, is_mall: false };
    return [
      toCartLine(
        {
          id: -(i + 1),
          product_id: v.product_id,
          variant_id: v.variant_id,
          product_slug: v.product_slug,
          category_name: v.category_name,
          product_name: v.product_name,
          image_url: v.image_url,
          variant_name: v.variant_name,
          unit_price: v.unit_price,
          original_price: v.original_price,
          price_at_add: null,
          quantity: it.quantity,
          stock: v.stock,
          is_available: v.is_available,
          unavailable_reason: v.unavailable_reason,
        },
        shop,
      ),
    ];
  });
};

export const toMergeResult = (d: MergeResultDto): MergeResult => ({
  merged: d.merged,
  skipped: d.skipped.length,
  adjusted: d.adjusted.length,
});
