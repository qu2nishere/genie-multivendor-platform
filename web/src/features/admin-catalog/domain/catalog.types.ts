// Danh mục toàn sàn & bộ thuộc tính (API_SPEC 11.11; prototype admin/Catalog.jsx).
export interface LeafCategory {
  id: number;
  name: string;
}

export interface SubCategory {
  id: number;
  name: string;
  commissionRate: number;
  productCount: number;
  children: LeafCategory[];
}

export interface RootCategory {
  id: number;
  name: string;
  slug: string;
  icon: string;
  active: boolean;
  commissionRate: number;
  productCount: number;
  shopCount: number;
  children: SubCategory[];
}

export interface CatalogSummary {
  rootCount: number;
  childCount: number;
  productCount: number;
  pendingProposals: number;
}

export interface CategoryTree {
  roots: RootCategory[];
  summary: CatalogSummary;
}

export type AttributeType = 'TEXT' | 'NUMBER' | 'BOOLEAN' | 'ENUM' | 'MULTI_ENUM' | 'DATE';

export interface CategoryAttribute {
  code: string;
  name: string;
  dataType: AttributeType;
  values: string[];
  unit: string | null;
  required: boolean;
  filterable: boolean;
  variantAxis: boolean;
  active: boolean;
}

export interface CategoryProposal {
  id: number;
  name: string;
  shop: { id: number; name: string };
  parent: { id: number; name: string } | null;
  reason: string;
  createdAt: string;
}

export type CatalogProductTab = 'ALL' | 'PENDING' | 'PUBLISHED' | 'HIDDEN_OR_LOCKED';
export type CatalogProductStatus = 'PUBLISHED' | 'PENDING_APPROVAL' | 'REJECTED' | 'HIDDEN';

export interface CatalogProduct {
  id: number;
  name: string;
  imageUrl: string;
  shopName: string;
  price: number;
  stock: number;
  status: CatalogProductStatus;
  locked: boolean;
  reason: string | null;
}

/** Giá trị form Thêm / Sửa danh mục. */
export interface CategoryInput {
  name: string;
  slug: string;
  /** null = danh mục gốc. */
  parentId: number | null;
  icon: string;
  commissionRate: number;
  active: boolean;
}
