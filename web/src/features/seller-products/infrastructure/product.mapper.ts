import type {
  CatalogNode,
  CategoryProposal,
  ListingRules,
  ProductDetail,
  ProductForm,
  SellerProductRow,
} from '../domain/product.types';
import type {
  CatalogNodeDto,
  ListingRulesDto,
  ProductDetailDto,
  ProductInputDto,
  ProductRowDto,
  ProposalDto,
} from './product.dto';

export const toRow = (d: ProductRowDto): SellerProductRow => ({
  id: d.id,
  name: d.name,
  imageUrl: d.image_url,
  variantCount: d.variant_count,
  priceMin: d.price_min,
  stockTotal: d.stock_total,
  soldCount: d.sold_count,
  status: d.status,
  rejectReason: d.reject_reason,
});

export const toDetail = (d: ProductDetailDto): ProductDetail => ({
  id: d.id,
  status: d.status,
  rejectReason: d.reject_reason,
  images: d.images.map((i) => ({ objectKey: i.object_key, url: i.url })),
  video: d.video && { objectKey: d.video.object_key, name: d.video.file_name },
  documents: d.documents.map((x) => ({ objectKey: x.object_key, name: x.file_name })),
  name: d.name,
  categoryPath: d.category_path,
  brand: d.brand,
  condition: d.condition,
  origin: d.origin,
  specs: d.specs,
  descriptionHtml: d.description_html,
  groups: d.option_groups.map((g) => ({
    name: g.name,
    values: g.values.map((v) => ({
      value: v.value,
      imageKey: v.image_object_key ?? null,
      imageUrl: v.image_url ?? null,
    })),
  })),
  variantImageGroup: d.variant_image_group,
  variants: d.variants.map((v) => ({ ...v })),
  weightGrams: d.weight_grams,
  dims: d.dimensions_cm,
  shippingPayer: d.shipping_payer,
  freeShippingThreshold: d.free_shipping_threshold,
  isPreOrder: d.is_pre_order,
  preOrderDays: d.pre_order_days,
});

export const toInput = (f: ProductForm, submit: boolean): ProductInputDto => ({
  submit,
  name: f.name.trim(),
  category_id: f.categoryPath.at(-1)?.id ?? null,
  brand_id: f.brand?.id ?? null,
  condition: f.condition,
  origin: f.origin.trim(),
  image_object_keys: f.images.map((i) => i.objectKey),
  video_object_key: f.video?.objectKey ?? null,
  document_object_keys: f.documents.map((x) => x.objectKey),
  specs: f.specs.filter((s) => s.name.trim() && s.value.trim()),
  description_html: f.descriptionHtml,
  option_groups: f.groups
    .filter((g) => g.values.length)
    .map((g) => ({
      name: g.name.trim(),
      values: g.values.map((v) => ({ value: v.value, image_object_key: v.imageKey })),
    })),
  variant_image_group: f.variantImageGroup,
  variants: f.variants.map((v) => ({ ...v })),
  weight_grams: f.weightGrams,
  dimensions_cm: f.dims,
  shipping_payer: f.shippingPayer,
  free_shipping_threshold: f.shippingPayer === 'SELLER' ? f.freeShippingThreshold : null,
  is_pre_order: f.isPreOrder,
  pre_order_days: f.isPreOrder ? f.preOrderDays : null,
});

export const toRules = (d: ListingRulesDto): ListingRules => ({
  categoryId: d.category_id,
  commissionRate: d.commission_rate,
  requiresDocuments: d.requires_documents,
  attributes: d.attributes.map((a) => ({
    name: a.name,
    required: a.is_required,
    values: a.values,
  })),
});

export const toCatalog = (d: CatalogNodeDto): CatalogNode => ({
  id: d.id,
  name: d.name,
  commissionRate: d.commission_rate,
  productCount: d.product_count,
  children: (d.children ?? []).map(toCatalog),
});

export const toProposal = (d: ProposalDto): CategoryProposal => ({
  id: d.id,
  code: d.code,
  name: d.name,
  parent: d.parent,
  status: d.status,
  rejectReason: d.reject_reason,
  createdAt: d.created_at,
});
