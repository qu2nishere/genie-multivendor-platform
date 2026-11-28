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
  imageUrl: d.image_url ?? '',
  variantCount: d.variant_count,
  priceMin: d.price_min,
  stockTotal: d.stock_total,
  soldCount: d.sold_count,
  status: d.status,
  rejectReason: d.reject_reason,
});

// API thật trả null cho ô shop chưa nhập (nháp, sản phẩm bị từ chối…) — form luôn cần chuỗi / mảng
// (trình soạn mô tả, ô nhập) nên chuẩn hóa ở đây; không có biến thể → 1 dòng trống như form mới.
export const toDetail = (d: ProductDetailDto): ProductDetail => {
  const variants = (d.variants ?? []).map(({ id, options, sku, price, stock }) => ({
    id,
    options: options ?? [],
    sku,
    price,
    stock,
  }));
  return {
    id: d.id,
    version: d.version ?? null,
    isLocked: d.is_locked ?? false,
    status: d.status,
    rejectReason: d.reject_reason ?? null,
    images: (d.images ?? []).map((i) => ({ objectKey: i.object_key, url: i.url })),
    video: d.video ? { objectKey: d.video.object_key, name: d.video.file_name } : null,
    documents: (d.documents ?? []).map((x) => ({ objectKey: x.object_key, name: x.file_name })),
    name: d.name ?? '',
    categoryPath: d.category_path ?? [],
    brand: d.brand ?? null,
    condition: d.condition ?? 'NEW',
    origin: d.origin ?? '',
    specs: (d.specs ?? []).map((x) => ({ name: x.name ?? '', value: x.value ?? '' })),
    descriptionHtml: d.description_html ?? '',
    groups: (d.option_groups ?? []).map((g) => ({
      name: g.name,
      values: (g.values ?? []).map((v) => ({
        value: v.value,
        imageKey: v.image_object_key ?? null,
        imageUrl: v.image_url ?? null,
      })),
      // nhóm đang có ảnh / nhóm ảnh chính → bật "Dùng ảnh cho nhóm này"
      withImages:
        g.name === d.variant_image_group || (g.values ?? []).some((v) => !!v.image_object_key),
    })),
    variantImageGroup: d.variant_image_group ?? null,
    variants: variants.length
      ? variants
      : [{ id: null, options: [], sku: null, price: null, stock: null }],
    weightGrams: d.weight_grams ?? null,
    dims: {
      length: d.dimensions_cm?.length ?? null,
      width: d.dimensions_cm?.width ?? null,
      height: d.dimensions_cm?.height ?? null,
    },
    shippingPayer: d.shipping_payer ?? 'BUYER',
    isPreOrder: d.is_pre_order ?? false,
    preOrderDays: d.pre_order_days ?? null,
  };
};

export const toInput = (
  f: ProductForm,
  submit: boolean,
  version: number | null = null,
): ProductInputDto => ({
  submit,
  version,
  name: f.name.trim(),
  category_id: f.categoryPath.at(-1)?.id ?? null,
  brand_id: f.brand?.id ?? null,
  condition: f.condition,
  origin: f.origin.trim(),
  image_object_keys: f.images.map((i) => i.objectKey),
  video_object_key: f.video?.objectKey ?? null,
  document_object_keys: f.documents.map((x) => x.objectKey),
  document_file_names: f.documents.map((x) => x.name),
  specs: f.specs.filter((s) => s.name.trim() && s.value.trim()),
  description_html: f.descriptionHtml,
  option_groups: f.groups
    .filter((g) => g.values.length)
    .map((g) => ({
      name: g.name.trim(),
      // nhóm đã tắt "Dùng ảnh" thì không gửi ảnh (kể cả khóa còn sót trong form)
      values: g.values.map((v) => ({
        value: v.value,
        image_object_key: g.withImages ? v.imageKey : null,
      })),
    })),
  variant_image_group: f.variantImageGroup,
  variants: f.variants.map(({ id, options, sku, price, stock }) => ({
    id,
    options,
    sku,
    price,
    stock,
  })),
  weight_grams: f.weightGrams,
  dimensions_cm: f.dims,
  shipping_payer: f.shippingPayer,
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
    values: a.values ?? [],
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
