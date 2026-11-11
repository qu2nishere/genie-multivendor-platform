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
  specs: { key: string; value: string }[];
  description_html: string;
}
