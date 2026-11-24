import type {
  CatalogProduct,
  CatalogSummary,
  CategoryBase,
  CategoryAttribute,
  CategoryInput,
  CategoryProposal,
  InheritedAttribute,
  RootCategory,
} from '../domain/catalog.types';
import type {
  AttributeDto,
  CatalogProductDto,
  CatalogSummaryDto,
  CategoryDto,
  CategoryInputDto,
  InheritedAttributeDto,
  LeafCategoryDto,
  ProposalDto,
} from './catalog.dto';

/** Trường chung của ngành cấp 2 / 3; thiếu tỷ lệ thì kế thừa `parentRate`. */
const toBase = (d: LeafCategoryDto, parentRate: number): CategoryBase => ({
  id: d.id,
  name: d.name,
  slug: d.slug ?? '',
  icon: d.icon ?? 'package',
  active: d.is_active ?? true,
  requiresLicense: d.requires_business_license ?? false,
  commissionRate: d.commission_rate ?? d.effective_commission_rate ?? parentRate,
  ownCommission: d.commission_rate !== null && d.commission_rate !== undefined,
  productCount: d.product_count ?? 0,
});

export const toRoot = (d: CategoryDto): RootCategory => ({
  id: d.id,
  name: d.name,
  slug: d.slug,
  icon: d.icon,
  active: d.is_active,
  requiresLicense: d.requires_business_license ?? false,
  commissionRate: d.commission_rate ?? d.effective_commission_rate,
  ownCommission: d.commission_rate !== null && d.commission_rate !== undefined,
  productCount: d.product_count,
  shopCount: d.shop_count,
  children: d.children.map((s) => {
    const sub = toBase(s, d.commission_rate ?? d.effective_commission_rate);
    return { ...sub, children: s.children.map((l) => toBase(l, sub.commissionRate)) };
  }),
});

export const toSummary = (d: CatalogSummaryDto): CatalogSummary => ({
  rootCount: d.root_count,
  childCount: d.child_count,
  productCount: d.product_count,
  pendingProposals: d.pending_proposals,
});

export const toCategoryInputDto = (c: CategoryInput): CategoryInputDto => ({
  name: c.name,
  slug: c.slug,
  parent_id: c.parentId,
  icon: c.icon,
  ...(c.commissionRate !== null ? { commission_rate: c.commissionRate } : {}),
  is_active: c.active,
  requires_business_license: c.requiresLicense,
});

export const toAttribute = (d: AttributeDto): CategoryAttribute => ({
  code: d.code,
  name: d.name,
  dataType: d.data_type,
  values: d.values,
  unit: d.unit,
  required: d.is_required,
  filterable: d.is_filterable,
  variantAxis: d.is_variant_axis,
  active: d.is_active,
});

export const toInheritedAttribute = (d: InheritedAttributeDto): InheritedAttribute => ({
  code: d.code,
  name: d.name,
  dataType: d.data_type as InheritedAttribute['dataType'],
  required: d.is_required,
  active: d.is_active,
  categoryId: d.category_id,
  categoryName: d.category_name,
});

export const toAttributeDto = (a: CategoryAttribute): AttributeDto => ({
  code: a.code,
  name: a.name,
  data_type: a.dataType,
  values: a.values,
  unit: a.unit,
  is_required: a.required,
  is_filterable: a.filterable,
  is_variant_axis: a.variantAxis,
  is_active: a.active,
});

export const toProposal = (d: ProposalDto): CategoryProposal => ({
  id: d.id,
  name: d.name,
  shop: d.shop,
  parent: d.parent,
  reason: d.reason,
  createdAt: d.created_at,
});

export const toCatalogProduct = (d: CatalogProductDto): CatalogProduct => ({
  id: d.id,
  name: d.name,
  imageUrl: d.image_url,
  shopName: d.shop_name,
  price: d.price,
  stock: d.stock,
  status: d.status,
  locked: d.is_locked,
  reason: d.reason,
});
