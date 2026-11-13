import type {
  Product,
  ProductBadge,
  ProductSummary,
  StockState,
  VariantGroup,
} from './product.types';

/** Ngưỡng "Sắp hết" (prototype: còn ≤ 5). */
export const LOW_STOCK = 5;

/** Deeplink sản phẩm /p/{slug}-{id} (R12). */
export const productHref = (p: Pick<ProductSummary, 'slug' | 'id'>): string =>
  `/p/${p.slug}-${p.id}`;

/** Phần trăm giảm làm tròn từ giá gốc. */
export function discountPct(price: number, original: number | null): number {
  if (!original || original <= price) return 0;
  return Math.round(((original - price) / original) * 100);
}

export const isFlashSoldOut = (p: Pick<ProductSummary, 'flash'>): boolean =>
  !!p.flash && p.flash.sold >= p.flash.total;

/** Nhãn hiển thị trên trang sản phẩm (prototype: Chính hãng / FLASH SALE / Đặt trước). */
export function badges(p: ProductSummary, totalStock?: number): ProductBadge[] {
  const out: ProductBadge[] = [];
  if (p.authentic) out.push('authentic');
  if (p.flash) out.push('flash');
  if (p.preorderDays) out.push('preorder');
  if (totalStock === 0) out.push('out_of_stock');
  return out;
}

export type Selection = Record<string, number | null>;

/** Giá theo lựa chọn phân loại: giá gốc + chênh của từng nhóm đã chọn. */
export function priceOf(p: Pick<Product, 'price' | 'variantGroups'>, sel: Selection): number {
  return p.variantGroups.reduce((sum, g) => {
    const i = sel[g.name];
    return sum + (i != null && g.options[i] ? g.options[i].priceDelta : 0);
  }, p.price);
}

/** Tồn theo lựa chọn: chưa chọn nhóm quyết định tồn → tổng tồn mọi lựa chọn. */
export function stockOf(groups: VariantGroup[], sel: Selection): number {
  const g = groups.find((x) => x.controlsStock);
  if (!g) return Number.POSITIVE_INFINITY;
  const i = sel[g.name];
  if (i != null && g.options[i]) return g.options[i].stock ?? 0;
  return g.options.reduce((n, o) => n + (o.stock ?? 0), 0);
}

export function stockState(stock: number, preorderDays: number | null): StockState {
  if (preorderDays) return 'preorder';
  if (stock <= 0) return 'out_of_stock';
  if (stock <= LOW_STOCK) return 'low';
  return 'in_stock';
}

/** Đã chọn đủ mọi nhóm phân loại chưa (prototype: báo "Vui lòng chọn đầy đủ Phân loại hàng"). */
export function missingGroups(groups: VariantGroup[], sel: Selection): string[] {
  return groups.filter((g) => sel[g.name] == null).map((g) => g.name);
}

export function variantLabel(groups: VariantGroup[], sel: Selection): string {
  return groups
    .map((g) => {
      const i = sel[g.name];
      return i != null ? g.options[i]?.label : null;
    })
    .filter(Boolean)
    .join(' / ');
}

/** Số lượng tối đa được mua: Flash Sale 1 suất / tài khoản, ngược lại theo tồn. */
export function maxQty(p: Pick<ProductSummary, 'flash'>, stock: number): number {
  if (p.flash) return 1;
  return Math.max(1, Number.isFinite(stock) ? stock : 99);
}

/** SKU (product_variant_id) của lựa chọn đầy đủ; thiếu nhóm nào → null. */
export function skuOf(p: Pick<Product, 'variantGroups' | 'skus'>, sel: Selection): number | null {
  const idx = p.variantGroups.map((g) => sel[g.name]);
  if (idx.some((i) => i == null)) return null;
  return p.skus.find((s) => s.options.every((o, k) => o === idx[k]))?.id ?? null;
}

/** Slug trên URL khác slug chuẩn → cần chuyển hướng 301 (R19). */
export const needsCanonicalRedirect = (urlSlug: string, p: Pick<ProductSummary, 'slug'>): boolean =>
  urlSlug !== p.slug;

/** Mã phân loại trong liên kết chia sẻ: "?v=0-1" (chỉ số lựa chọn theo nhóm). */
export function encodeSelection(groups: VariantGroup[], sel: Selection): string {
  return groups.map((g) => (sel[g.name] == null ? '' : String(sel[g.name]))).join('-');
}

export function decodeSelection(groups: VariantGroup[], code: string | null): Selection {
  const parts = (code ?? '').split('-');
  const sel: Selection = {};
  groups.forEach((g, k) => {
    const n = parts[k] === '' || parts[k] === undefined ? NaN : Number(parts[k]);
    const opt = g.options[n];
    sel[g.name] =
      Number.isInteger(n) && opt && (!g.controlsStock || (opt.stock ?? 0) > 0) ? n : null;
  });
  return sel;
}
