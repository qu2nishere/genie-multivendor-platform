// DTO snake_case — /seller/products, /seller/categories, /seller/brands, /seller/category-proposals
// (API_SPEC 10.5–10.6).
import type {
  Condition,
  ProductStatus,
  ProposalStatus,
  ShippingPayer,
} from '../domain/product.types';

export interface ProductRowDto {
  id: number;
  name: string;
  image_url: string;
  variant_count: number;
  price_min: number;
  stock_total: number;
  sold_count: number;
  status: ProductStatus;
  reject_reason: string | null;
}

export interface ProductDetailDto {
  id: number;
  status: ProductStatus;
  reject_reason: string | null;
  images: { object_key: string; url: string }[];
  video: { object_key: string; file_name: string } | null;
  documents: { id: number; file_name: string; object_key: string; url: string }[];
  name: string;
  category_path: { id: number; name: string }[];
  brand: { id: number; name: string } | null;
  condition: Condition;
  origin: string;
  specs: { name: string; value: string }[];
  description_html: string;
  option_groups: {
    name: string;
    values: { value: string; image_object_key?: string | null; image_url?: string | null }[];
  }[];
  variant_image_group: string | null;
  variants: {
    id: number | null;
    options: string[];
    sku: string | null;
    price: number;
    stock: number;
  }[];
  weight_grams: number | null;
  dimensions_cm: { length: number | null; width: number | null; height: number | null };
  shipping_payer: ShippingPayer;
  free_shipping_threshold: number | null;
  is_pre_order: boolean;
  pre_order_days: number | null;
}

export interface ProductInputDto {
  submit: boolean;
  name: string;
  category_id: number | null;
  brand_id: number | null;
  condition: Condition;
  origin: string;
  image_object_keys: string[];
  video_object_key: string | null;
  document_object_keys: string[];
  specs: { name: string; value: string }[];
  description_html: string;
  option_groups: { name: string; values: { value: string; image_object_key: string | null }[] }[];
  variant_image_group: string | null;
  variants: {
    id: number | null;
    options: string[];
    sku: string | null;
    price: number | null;
    stock: number | null;
  }[];
  weight_grams: number | null;
  dimensions_cm: { length: number | null; width: number | null; height: number | null };
  shipping_payer: ShippingPayer;
  free_shipping_threshold: number | null;
  is_pre_order: boolean;
  pre_order_days: number | null;
}

export interface ListingRulesDto {
  category_id: number;
  commission_rate: number;
  requires_documents: boolean;
  attributes: {
    code: string;
    name: string;
    data_type: 'TEXT' | 'ENUM' | 'NUMBER';
    values: string[];
    unit: string | null;
    is_required: boolean;
    is_variant_axis: boolean;
  }[];
}

export interface CatalogNodeDto {
  id: number;
  name: string;
  commission_rate: number;
  product_count: number;
  children?: CatalogNodeDto[];
}

export interface ProposalDto {
  id: number;
  code: string;
  name: string;
  parent: { id: number; name: string } | null;
  status: ProposalStatus;
  reject_reason: string | null;
  created_at: string;
}
