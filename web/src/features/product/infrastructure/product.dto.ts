// JSON snake_case của API sản phẩm (/public/products, /public/products/{id}).

export interface ProductSummaryDto {
  id: number;
  slug: string;
  title: string;
  image: string;
  price: number;
  original_price: number | null;
  discount_pct: number;
  rating: number;
  sold_count: number;
  shop: { id: number; name: string; on_vacation: boolean };
  category_id: number;
  free_ship: boolean;
  is_authentic: boolean;
  flash: { sold: number; total: number } | null;
  preorder_days: number | null;
  brand: string;
  brand_verified: boolean;
  attribute: string | null;
}

export interface VariantGroupDto {
  name: string;
  controls_stock: boolean;
  options: { label: string; swatch?: string; price_delta: number; stock?: number }[];
}

export interface ProductDetailDto extends ProductSummaryDto {
  sku: string;
  images: string[];
  review_count: number;
  variant_groups: VariantGroupDto[];
  /** Mỗi tổ hợp phân loại là một SKU (product_variant_id), option_indexes theo thứ tự nhóm. */
  variants: { id: number; option_indexes: number[] }[];
  specs: { key: string; value: string }[];
  description_html: string;
}

// ===== API thật (API_SPEC mục 1 $ProductCard, 5.3, 5.6 — plan 2026-11-17 C07.02) =====

/** `$ProductCard` — thẻ ở mọi lưới (tìm kiếm, danh mục, shop, tương tự, gợi ý). */
export interface ProductCardDto {
  id: number;
  slug: string;
  name: string;
  image_url: string | null;
  price: number;
  original_price: number | null;
  discount_percent: number | null;
  rating_avg: number;
  sold_count: number;
  shop: { id: number; name: string; is_mall: boolean };
  is_free_shipping: boolean;
  is_pre_order: boolean;
  is_shop_on_vacation: boolean;
  flash_sale: { sold_count: number; total_count: number } | null;
}

/** `summary.facets` của `GET /public/products?include_facets=true`. */
export interface SearchFacetsDto {
  brands: { id: number; name: string; count: number }[];
  locations: { province_id: number; name: string; count: number }[];
  attribute: { code: string; name: string; values: { value: string; count: number }[] } | null;
  has_shop_voucher_supported: boolean;
}

/** `GET /public/products/{id}`. */
export interface ProductDetailApiDto {
  id: number;
  slug: string;
  name: string;
  category_path: { id: number; name: string; slug: string }[];
  brand: { id: number | null; name: string; is_verified: boolean } | null;
  images: string[];
  video_url: string | null;
  price: number;
  original_price: number | null;
  discount_percent: number | null;
  rating_avg: number;
  rating_count: number;
  sold_count: number;
  is_pre_order: boolean;
  pre_order_days: number | null;
  flash_sale: { flash_price: number; sold_count: number; total_count: number } | null;
  option_groups: { name: string; values: { value: string; image_url: string | null }[] }[];
  variants: { id: number; options: string[]; price: number; stock: number }[];
  specs: { name: string; value: string }[];
  condition: string | null;
  origin: string | null;
  ships_from: string | null;
  description_html: string | null;
  shop: {
    id: number;
    name: string;
    logo_url: string | null;
    is_mall: boolean;
    is_favorite: boolean;
    rating_avg: number;
    follower_count: number;
    province_name: string | null;
    joined_year: number;
    is_online: boolean;
    vacation: { until: string | null; message: string | null } | null;
  };
}
