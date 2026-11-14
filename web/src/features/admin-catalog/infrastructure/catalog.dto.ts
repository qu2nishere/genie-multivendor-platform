// DTO snake_case (API_SPEC 11.11).
export interface CategoryDto {
  id: number;
  name: string;
  slug: string;
  icon: string;
  is_active: boolean;
  commission_rate: number;
  product_count: number;
  shop_count: number;
  children: {
    id: number;
    name: string;
    commission_rate: number;
    product_count: number;
    children: { id: number; name: string }[];
  }[];
}

export interface CatalogSummaryDto {
  root_count: number;
  child_count: number;
  product_count: number;
  pending_proposals: number;
}

export interface CategoryInputDto {
  name: string;
  slug: string;
  parent_id: number | null;
  icon: string;
  commission_rate: number;
  is_active: boolean;
}

export interface AttributeDto {
  code: string;
  name: string;
  data_type: 'TEXT' | 'NUMBER' | 'BOOLEAN' | 'ENUM' | 'MULTI_ENUM' | 'DATE';
  values: string[];
  unit: string | null;
  is_required: boolean;
  is_filterable: boolean;
  is_variant_axis: boolean;
  is_active: boolean;
}

export interface ProposalDto {
  id: number;
  name: string;
  shop: { id: number; name: string };
  parent: { id: number; name: string } | null;
  reason: string;
  created_at: string;
}

export interface CatalogProductDto {
  id: number;
  name: string;
  image_url: string;
  shop_name: string;
  price: number;
  stock: number;
  status: 'PUBLISHED' | 'PENDING_APPROVAL' | 'REJECTED' | 'HIDDEN';
  is_locked: boolean;
  reason: string | null;
}
