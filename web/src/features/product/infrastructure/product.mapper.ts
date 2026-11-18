import type {
  CategoryCrumb,
  Product,
  ProductFacets,
  ProductSku,
  ProductSummary,
  VariantGroup,
} from '../domain/product.types';
import type {
  ProductCardDto,
  ProductDetailApiDto,
  ProductDetailDto,
  ProductSummaryDto,
  SearchFacetsDto,
  VariantGroupDto,
} from './product.dto';

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

// ===== API thật (plan 2026-11-17 C07.02) =====

/** `$ProductCard` → thẻ. Trường chỉ prototype có (danh mục, thương hiệu, thuộc tính) để trống. */
export const fromCardDto = (d: ProductCardDto): ProductSummary => ({
  id: d.id,
  slug: d.slug,
  title: d.name,
  image: d.image_url ?? '',
  price: d.price,
  originalPrice: d.original_price,
  discountPct: d.discount_percent ?? 0,
  rating: d.rating_avg,
  soldCount: d.sold_count,
  shopId: d.shop.id,
  shopName: d.shop.name,
  shopOnVacation: d.is_shop_on_vacation,
  categoryId: 0,
  freeship: d.is_free_shipping,
  authentic: d.shop.is_mall,
  flash: d.flash_sale ? { sold: d.flash_sale.sold_count, total: d.flash_sale.total_count } : null,
  // thẻ chỉ có cờ đặt trước; số ngày có ở trang chi tiết
  preorderDays: d.is_pre_order ? 1 : null,
  brand: '',
  brandVerified: false,
  attribute: null,
});

/**
 * Chi tiết API → miền: `option_groups` thành nhóm phân loại (priceDelta = 0 — giá theo SKU), `variants` thành SKU
 * mang chỉ số lựa chọn + giá + tồn thật; tồn của từng lựa chọn = tổng tồn SKU chứa nó (để luật cũ vẫn đúng).
 */
export function fromDetailDto(d: ProductDetailApiDto): Product {
  const groups: VariantGroup[] = d.option_groups.map((g, gi) => ({
    name: g.name,
    controlsStock: gi === 0,
    options: g.values.map((v) => ({
      label: v.value,
      priceDelta: 0,
      stock: d.variants
        .filter((x) => x.options[gi] === v.value)
        .reduce((n, x) => n + x.stock, 0),
    })),
  }));
  const skus: ProductSku[] = d.variants.map((v) => ({
    id: v.id,
    options: v.options.map((val, k) => d.option_groups[k]?.values.findIndex((x) => x.value === val) ?? -1),
    price: v.price,
    stock: v.stock,
  }));
  const path: CategoryCrumb[] = d.category_path;
  return {
    id: d.id,
    slug: d.slug,
    title: d.name,
    image: d.images[0] ?? '',
    price: d.price,
    originalPrice: d.original_price,
    discountPct: d.discount_percent ?? 0,
    rating: d.rating_avg,
    soldCount: d.sold_count,
    shopId: d.shop.id,
    shopName: d.shop.name,
    shopOnVacation: d.shop.vacation != null,
    categoryId: path.length ? path[path.length - 1].id : 0,
    freeship: false,
    authentic: d.shop.is_mall,
    flash: d.flash_sale ? { sold: d.flash_sale.sold_count, total: d.flash_sale.total_count } : null,
    preorderDays: d.is_pre_order ? (d.pre_order_days ?? 1) : null,
    brand: d.brand?.name ?? '',
    brandVerified: d.brand?.is_verified ?? false,
    attribute: null,
    sku: '',
    images: d.images,
    reviewCount: d.rating_count,
    variantGroups: groups,
    skus,
    specs: d.specs.map((s) => ({ key: s.name, value: s.value })),
    descriptionHtml: d.description_html ?? '',
    categoryPath: path,
  };
}

/** Facet API → giá trị lọc theo tên (UI lọc theo tên; tầng api tự đổi tên → id khi gọi). */
export const fromFacetsDto = (f: SearchFacetsDto | null | undefined, total: number): ProductFacets => ({
  brands: (f?.brands ?? []).map((b) => b.name),
  locations: (f?.locations ?? []).map((l) => l.name),
  total,
});
