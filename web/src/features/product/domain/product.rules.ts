import type {
  Product,
  ProductBadge,
  ProductSku,
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

/**
 * Giá theo lựa chọn phân loại. API thật (SKU có giá): chọn đủ → giá của SKU, chưa đủ → giá thấp nhất (p.price).
 * Mock prototype: giá gốc + chênh của từng nhóm đã chọn.
 */
export function priceOf(
  p: Pick<Product, 'price' | 'variantGroups'> & Partial<Pick<Product, 'skus'>>,
  sel: Selection,
): number {
  if (p.skus?.length && p.skus.every((s) => s.price !== undefined)) {
    const id = skuOf({ variantGroups: p.variantGroups, skus: p.skus }, sel);
    return p.skus.find((s) => s.id === id)?.price ?? p.price;
  }
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

// ---- Ma trận SKU thật (API_SPEC 5.6; plan 2026-11-17 A7 — cùng luật với VariantMatrix.selectableValues của backend) ----

/** SKU mang tồn thật (API) — mock prototype không có, dùng luật nhóm quyết định tồn. */
export const hasSkuStock = (p: Pick<Product, 'skus'>): boolean =>
  p.skus.length > 0 && p.skus.every((s) => s.stock !== undefined);

/** SKU khớp lựa chọn hiện tại (nhóm chưa chọn = mọi giá trị). */
export function matchingSkus(
  p: Pick<Product, 'variantGroups' | 'skus'>,
  sel: Selection,
): ProductSku[] {
  return p.skus.filter((s) =>
    p.variantGroups.every((g, k) => sel[g.name] == null || s.options[k] === sel[g.name]),
  );
}

/** Tồn theo lựa chọn: SKU thật → tổng tồn các SKU khớp; mock → {@link stockOf}. */
export function stockFor(p: Pick<Product, 'variantGroups' | 'skus'>, sel: Selection): number {
  if (!hasSkuStock(p)) return stockOf(p.variantGroups, sel);
  return matchingSkus(p, sel).reduce((n, s) => n + (s.stock ?? 0), 0);
}

/**
 * Lựa chọn `optionIndex` của nhóm `groupIndex` có bị khóa không: không còn SKU nào còn hàng khớp
 * (các nhóm khác đang chọn + lựa chọn này). Mock → nhóm quyết định tồn có tồn 0.
 */
export function isOptionOut(
  p: Pick<Product, 'variantGroups' | 'skus'>,
  sel: Selection,
  groupIndex: number,
  optionIndex: number,
): boolean {
  const g = p.variantGroups[groupIndex];
  if (!g) return true;
  if (!hasSkuStock(p)) return g.controlsStock && (g.options[optionIndex]?.stock ?? 0) === 0;
  return !p.skus.some(
    (s) =>
      (s.stock ?? 0) > 0 &&
      s.options[groupIndex] === optionIndex &&
      p.variantGroups.every(
        (x, k) => k === groupIndex || sel[x.name] == null || s.options[k] === sel[x.name],
      ),
  );
}

/** Tham số `v` của deeplink (UC-CAT-18): id SKU khi đã chọn đủ phân loại, ngược lại không gắn. */
export function shareVariantParam(
  p: Pick<Product, 'variantGroups' | 'skus'>,
  sel: Selection,
): string | null {
  const id = skuOf(p, sel);
  return id == null ? null : String(id);
}

/**
 * Đọc `?v=` của deeplink: id SKU (chuẩn UC-CAT-18) → chọn sẵn đúng phân loại (bỏ qua nếu SKU hết hàng);
 * không khớp SKU nào → thử định dạng cũ "0-1" (chỉ số theo nhóm).
 */
export function decodeShared(
  p: Pick<Product, 'variantGroups' | 'skus'>,
  code: string | null,
): Selection {
  if (code && /^\d+$/.test(code)) {
    const sku = p.skus.find((s) => s.id === Number(code));
    if (sku && (sku.stock === undefined || sku.stock > 0)) {
      return Object.fromEntries(p.variantGroups.map((g, k) => [g.name, sku.options[k] ?? null]));
    }
  }
  return decodeSelection(p.variantGroups, code);
}

/** Nguồn lượt xem (API_SPEC 5.7 `source`). */
export type ViewSource = 'SEARCH' | 'HOME_FEED' | 'PROMOTION' | 'SHOP_PAGE' | 'EXTERNAL_LINK';

/**
 * Đoán nguồn truy cập trang sản phẩm (UC-CAT-06 bước 9, UC-CAT-18 bước 5): `?src=share` → EXTERNAL_LINK; referrer cùng
 * site theo đường dẫn (/search, /c/ → SEARCH; /shop/ → SHOP_PAGE; /campaign, /flash-sale → PROMOTION; còn lại HOME_FEED);
 * referrer site khác → EXTERNAL_LINK; không có referrer → HOME_FEED.
 */
export function viewSourceOf(src: string | null, referrer: string, origin: string): ViewSource {
  if (src === 'share') return 'EXTERNAL_LINK';
  if (!referrer) return 'HOME_FEED';
  let ref: URL;
  try {
    ref = new URL(referrer);
  } catch {
    return 'HOME_FEED';
  }
  if (ref.origin !== origin) return 'EXTERNAL_LINK';
  const path = ref.pathname;
  if (path.startsWith('/search') || path.startsWith('/c/')) return 'SEARCH';
  if (path.startsWith('/shop/')) return 'SHOP_PAGE';
  if (path.startsWith('/campaign') || path.startsWith('/flash-sale')) return 'PROMOTION';
  return 'HOME_FEED';
}
