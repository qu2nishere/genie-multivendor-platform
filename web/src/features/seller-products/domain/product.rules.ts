// Quy tắc đăng bán (API_SPEC 10.5; prototype Products.jsx): ma trận phân loại, giới hạn, thông số bắt buộc,
// giấy tờ theo ngành, cảnh báo giá 0, cần kiểm duyệt lại khi sửa sản phẩm đang bán.
import type {
  ListingRules,
  OptionGroup,
  ProductForm,
  ProductStatus,
  SpecRow,
  VariantRow,
} from './product.types';

export const MAX_IMAGES = 8;
export const MAX_GROUPS = 2;
export const MAX_VARIANTS = 50;
export const MAX_DOCS = 4;
export const DESC_MAX = 10_000;
export const PRE_ORDER_MIN = 7;
export const PRE_ORDER_MAX = 30;

/** Tên nhóm phân loại gợi ý (prototype GROUP_NAMES) — shop vẫn tự đặt tên được. */
export const GROUP_NAMES = [
  'Màu sắc',
  'Kích thước',
  'Chất liệu',
  'Kiểu dáng',
  'Phiên bản',
  'Dung lượng',
  'Hương vị',
  'Số lượng',
  'Loại',
  'Combo',
];

export const variantKey = (options: string[]): string => options.join('|');

/** Mọi tổ hợp giá trị của các nhóm có giá trị (thứ tự nhóm → thứ tự giá trị). */
export function combos(groups: OptionGroup[]): string[][] {
  const active = groups.filter((g) => g.values.length > 0);
  if (active.length === 0) return [[]];
  return active.reduce<string[][]>(
    (acc, g) => acc.flatMap((c) => g.values.map((v) => [...c, v.value])),
    [[]],
  );
}

/** Dựng lại bảng biến thể theo nhóm, giữ giá / tồn / SKU / id của tổ hợp đã có. */
export function syncVariants(groups: OptionGroup[], current: VariantRow[]): VariantRow[] {
  const byKey = new Map(current.map((v) => [variantKey(v.options), v]));
  const single = current.length === 1 && current[0].options.length === 0 ? current[0] : null;
  return combos(groups).map((options) => {
    const hit = byKey.get(variantKey(options));
    if (hit) return hit;
    // Từ "không phân loại" sang có phân loại: mang giá / tồn đơn lẻ sang các dòng mới
    return {
      id: null,
      options,
      sku: null,
      price: single?.price ?? null,
      stock: single?.stock ?? null,
    };
  });
}

/** SKU hiển thị khi server chưa sinh (prototype: SKU·ĐE·BL). */
export const skuPreview = (options: string[]): string =>
  options.length ? `SKU·${options.map((v) => v.slice(0, 2).toUpperCase()).join('·')}` : 'SKU';

/** Số biến thể có giá 0 / trống VÀ tồn 0 / trống — hỏi lại trước khi gửi (cảnh báo phía client). */
export const zeroVariants = (variants: VariantRow[]): number =>
  variants.filter((v) => !v.price && !v.stock).length;

export const invalidPrice = (v: VariantRow): boolean => v.price !== null && v.price <= 0;

/** Thông số bắt buộc của ngành còn thiếu giá trị. */
export function missingSpecs(specs: SpecRow[], rules: ListingRules | null | undefined): string[] {
  if (!rules) return [];
  return rules.attributes
    .filter((a) => a.required)
    .map((a) => a.name)
    .filter((n) => !specs.some((s) => s.name.trim() === n && s.value.trim()));
}

/** Gợi ý thông số chưa có trong bảng. */
export const suggestedSpecs = (
  specs: SpecRow[],
  rules: ListingRules | null | undefined,
): string[] =>
  (rules?.attributes ?? [])
    .map((a) => a.name)
    .filter((n) => !specs.some((s) => s.name.trim() === n));

/** Độ dài chữ của mô tả HTML (bỏ thẻ, gộp thực thể) — giới hạn 10.000 ký tự. */
export function textLength(html: string): number {
  return html
    .replace(/<(br|\/p|\/li|\/h[1-6])\s*\/?>/gi, '\n')
    .replace(/<[^>]*>/g, '')
    .replace(/&nbsp;/g, ' ')
    .replace(/&[a-z#0-9]+;/gi, 'x')
    .trim().length;
}

export type PublishIssue =
  | 'images'
  | 'name'
  | 'category'
  | 'documents'
  | 'specs'
  | 'price'
  | 'variants_limit'
  | 'description'
  | 'weight'
  | 'pre_order';

/** Kiểm đủ điều kiện "Đăng bán / Gửi duyệt" (10.5 `submit = true`). Lưu nháp không kiểm. */
export function publishIssues(
  f: ProductForm,
  rules: ListingRules | null | undefined,
): PublishIssue[] {
  const out: PublishIssue[] = [];
  if (f.images.length === 0 || f.images.length > MAX_IMAGES) out.push('images');
  if (f.name.trim().length < 5) out.push('name');
  if (f.categoryPath.length < 3) out.push('category');
  if (rules?.requiresDocuments && f.documents.length === 0) out.push('documents');
  if (missingSpecs(f.specs, rules).length) out.push('specs');
  if (f.variants.length > MAX_VARIANTS) out.push('variants_limit');
  if (f.variants.some((v) => v.price === null || v.price <= 0 || (v.stock !== null && v.stock < 0)))
    out.push('price');
  if (textLength(f.descriptionHtml) > DESC_MAX) out.push('description');
  if (!f.weightGrams || f.weightGrams <= 0) out.push('weight');
  if (
    f.isPreOrder &&
    (f.preOrderDays === null || f.preOrderDays < PRE_ORDER_MIN || f.preOrderDays > PRE_ORDER_MAX)
  )
    out.push('pre_order');
  return out;
}

/**
 * Sửa sản phẩm đang bán mà đổi tên / ảnh / mô tả / danh mục / thương hiệu / giấy tờ → phải duyệt lại;
 * chỉ đổi giá, tồn → giữ trạng thái (10.5).
 */
export function requiresReview(
  status: ProductStatus,
  before: ProductForm,
  after: ProductForm,
): boolean {
  if (status !== 'PUBLISHED') return false;
  const keys = (x: { objectKey: string }[]) => x.map((m) => m.objectKey).join(',');
  return (
    before.name.trim() !== after.name.trim() ||
    keys(before.images) !== keys(after.images) ||
    before.descriptionHtml !== after.descriptionHtml ||
    before.categoryPath.at(-1)?.id !== after.categoryPath.at(-1)?.id ||
    (before.brand?.name ?? '') !== (after.brand?.name ?? '') ||
    keys(before.documents) !== keys(after.documents)
  );
}

/** Chỉ xem khi đang chờ duyệt (10.5). */
export const isReadOnly = (s: ProductStatus | undefined): boolean => s === 'PENDING_APPROVAL';

export const STATUS_BADGE: Record<
  ProductStatus,
  {
    variant: 'success' | 'warning' | 'danger' | 'neutral';
    icon: 'circle-check' | 'clock' | 'triangle-alert' | 'eye-off' | 'file-text';
  }
> = {
  PUBLISHED: { variant: 'success', icon: 'circle-check' },
  PENDING_APPROVAL: { variant: 'warning', icon: 'clock' },
  REJECTED: { variant: 'danger', icon: 'triangle-alert' },
  HIDDEN: { variant: 'neutral', icon: 'eye-off' },
  DRAFT: { variant: 'neutral', icon: 'file-text' },
};

/** Form trống cho "Thêm sản phẩm". */
export const emptyForm = (): ProductForm => ({
  images: [],
  video: null,
  documents: [],
  name: '',
  categoryPath: [],
  brand: null,
  condition: 'NEW',
  origin: '',
  specs: [],
  descriptionHtml: '',
  groups: [],
  variantImageGroup: null,
  variants: [{ id: null, options: [], sku: null, price: null, stock: null }],
  weightGrams: null,
  dims: { length: null, width: null, height: null },
  shippingPayer: 'BUYER',
  freeShippingThreshold: null,
  isPreOrder: false,
  preOrderDays: null,
});
