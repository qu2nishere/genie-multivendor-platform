import type {
  CatalogProduct,
  CatalogSummary,
  CategoryAttribute,
  CategoryInput,
  CategoryProposal,
  RootCategory,
} from '../domain/catalog.types';
import type {
  AttributeDto,
  CatalogProductDto,
  CatalogSummaryDto,
  CategoryDto,
  CategoryInputDto,
  ProposalDto,
} from './catalog.dto';

export const toRoot = (d: CategoryDto): RootCategory => ({
  id: d.id,
  name: d.name,
  slug: d.slug,
  icon: d.icon,
  active: d.is_active,
  commissionRate: d.commission_rate,
  productCount: d.product_count,
  shopCount: d.shop_count,
  children: d.children.map((s) => ({
    id: s.id,
    name: s.name,
    commissionRate: s.commission_rate,
    productCount: s.product_count,
    children: s.children,
  })),
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
  commission_rate: c.commissionRate,
  is_active: c.active,
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
