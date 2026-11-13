import type { Product, ProductSummary, VariantGroup } from '../domain/product.types';
import type { ProductDetailDto, ProductSummaryDto, VariantGroupDto } from './product.dto';

export const toProductSummary = (d: ProductSummaryDto): ProductSummary => ({
  id: d.id,
  slug: d.slug,
  title: d.title,
  image: d.image,
  price: d.price,
  originalPrice: d.original_price,
  discountPct: d.discount_pct,
  rating: d.rating,
  soldCount: d.sold_count,
  shopId: d.shop.id,
  shopName: d.shop.name,
  shopOnVacation: d.shop.on_vacation,
  categoryId: d.category_id,
  freeship: d.free_ship,
  authentic: d.is_authentic,
  flash: d.flash,
  preorderDays: d.preorder_days,
  brand: d.brand,
  brandVerified: d.brand_verified,
  attribute: d.attribute,
});

const toVariantGroup = (g: VariantGroupDto): VariantGroup => ({
  name: g.name,
  controlsStock: g.controls_stock,
  options: g.options.map((o) => ({
    label: o.label,
    swatch: o.swatch,
    priceDelta: o.price_delta,
    stock: o.stock,
  })),
});

export const toProduct = (d: ProductDetailDto): Product => ({
  ...toProductSummary(d),
  sku: d.sku,
  images: d.images,
  reviewCount: d.review_count,
  variantGroups: d.variant_groups.map(toVariantGroup),
  skus: d.variants.map((v) => ({ id: v.id, options: v.option_indexes })),
  specs: d.specs,
  descriptionHtml: d.description_html,
});
