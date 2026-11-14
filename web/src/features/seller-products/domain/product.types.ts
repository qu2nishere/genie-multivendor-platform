// Sản phẩm của shop (API_SPEC 10.5–10.6; prototype vendor/Products.jsx, VendorCatalog.jsx).
export type ProductStatus = 'PUBLISHED' | 'DRAFT' | 'PENDING_APPROVAL' | 'REJECTED' | 'HIDDEN';
export type ProductTab = 'ALL' | ProductStatus;
export type Condition = 'NEW' | 'USED' | 'REFURBISHED';
export type ShippingPayer = 'BUYER' | 'SELLER';

export interface SellerProductRow {
  id: number;
  name: string;
  imageUrl: string;
  variantCount: number;
  priceMin: number;
  stockTotal: number;
  soldCount: number;
  status: ProductStatus;
  rejectReason: string | null;
}

export interface CategoryNode {
  id: number;
  name: string;
  children?: CategoryNode[];
}

export interface ListingRules {
  categoryId: number;
  commissionRate: number;
  requiresDocuments: boolean;
  /** Thông số gợi ý theo ngành (kế thừa ngành cha); `required` = bắt buộc khi gửi duyệt. */
  attributes: { name: string; required: boolean; values: string[] }[];
}

export interface Brand {
  id: number;
  name: string;
}

export interface MediaRef {
  objectKey: string;
  url: string;
}

export interface DocRef {
  objectKey: string;
  name: string;
}

export interface SpecRow {
  name: string;
  value: string;
}

export interface OptionValue {
  value: string;
  imageKey: string | null;
  imageUrl: string | null;
}

export interface OptionGroup {
  name: string;
  values: OptionValue[];
}

export interface VariantRow {
  id: number | null;
  options: string[];
  sku: string | null;
  /** null = chưa nhập. */
  price: number | null;
  stock: number | null;
}

/** Dữ liệu form 7 khối (10.5 `$ProductInput`). */
export interface ProductForm {
  images: MediaRef[];
  video: DocRef | null;
  documents: DocRef[];
  name: string;
  categoryPath: { id: number; name: string }[];
  brand: Brand | null;
  condition: Condition;
  origin: string;
  specs: SpecRow[];
  descriptionHtml: string;
  groups: OptionGroup[];
  variantImageGroup: string | null;
  /** Không có nhóm phân loại → đúng 1 dòng `options: []`. */
  variants: VariantRow[];
  weightGrams: number | null;
  dims: { length: number | null; width: number | null; height: number | null };
  shippingPayer: ShippingPayer;
  freeShippingThreshold: number | null;
  isPreOrder: boolean;
  preOrderDays: number | null;
}

export interface ProductDetail extends ProductForm {
  id: number;
  status: ProductStatus;
  rejectReason: string | null;
}

export interface SaveResult {
  id: number;
  status: ProductStatus;
  requiresReview: boolean;
}

/** Danh mục toàn sàn (2 cấp, có số sản phẩm + hoa hồng — 10.6). */
export interface CatalogNode {
  id: number;
  name: string;
  commissionRate: number;
  productCount: number;
  children: CatalogNode[];
}

export type ProposalStatus = 'PENDING' | 'APPROVED' | 'REJECTED';
export interface CategoryProposal {
  id: number;
  code: string;
  name: string;
  parent: { id: number; name: string } | null;
  status: ProposalStatus;
  rejectReason: string | null;
  createdAt: string;
}
