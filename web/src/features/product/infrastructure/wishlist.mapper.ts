import type { ProductSummary, WishlistEntry } from '../domain/product.types';
import type { WishlistItemDto } from './wishlist.dto';

export const toWishlistEntry = (d: WishlistItemDto): WishlistEntry => ({
  productId: d.product_id,
  slug: d.product_slug,
  title: d.name,
  image: d.image_url,
  price: d.price,
  defaultVariantId: d.default_variant_id,
  purchasable: d.is_purchasable,
});

/** Mock: dựng dòng yêu thích từ thẻ sản phẩm (không có thông tin SKU → chỉ khóa khi shop nghỉ). */
export const entryFromSummary = (p: ProductSummary): WishlistEntry => ({
  productId: p.id,
  slug: p.slug,
  title: p.title,
  image: p.image,
  price: p.price,
  defaultVariantId: null,
  purchasable: !p.shopOnVacation,
});
