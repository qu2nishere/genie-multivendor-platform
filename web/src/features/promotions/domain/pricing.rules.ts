// Luật voucher & tính giá xếp chồng — TS thuần, dùng chung Giỏ hàng và Thanh toán (plan G10.09,
// G10.10; prototype data.jsx voucherLines / eligibleBase / voucherCheck / voucherDiscount / priceCart /
// bestVouchers; API_SPEC 6.8). Thứ tự: Voucher Shop → Voucher Sàn (phân bổ theo dòng) → Freeship →
// Điểm (≤ 50% tiền hàng còn lại, 1 Điểm = 1₫). Tổng giảm tầng Shop + Sàn ≤ 85% tiền hàng (LLD 9.3).
// Đây là bản tính phía client cho mock; khi có backend, server tính lại qua POST /me/checkout/preview.
import type {
  PriceBreakdown,
  PricingContext,
  PricingLine,
  VoucherCheck,
  VoucherSelection,
} from './pricing.types';
import type { Voucher } from './voucher.types';

export const POINTS_RATIO = 0.5;
export const MAX_VOUCHER_SHARE = 0.85;

export const emptySelection = (): VoucherSelection => ({
  platform: null,
  freeship: null,
  shop: {},
});

const sum = (lines: PricingLine[]) => lines.reduce((n, l) => n + l.price * l.qty, 0);
const shopsOf = (lines: PricingLine[]) => [...new Set(lines.map((l) => l.shopId))];
const byId = (ctx: PricingContext, id: string | null | undefined) =>
  id ? (ctx.vouchers.find((v) => v.id === id) ?? null) : null;

/** Dòng mà voucher áp được: đúng shop, đúng ngành, không thuộc ngành bị loại trừ. */
export function voucherLines(v: Voucher, lines: PricingLine[]): PricingLine[] {
  return lines.filter((l) => {
    if (v.shopId != null && l.shopId !== v.shopId) return false;
    if (v.excluded && l.category && v.excluded.includes(l.category)) return false;
    if (v.categories && (!l.category || !v.categories.includes(l.category))) return false;
    return true;
  });
}

/**
 * Giá trị làm căn cứ: tiền các dòng áp được; với voucher sàn, trừ trước phần voucher shop đã giảm
 * trên các dòng đó (tầng 2 tính trên số còn lại sau tầng 1).
 */
export function eligibleBase(
  v: Voucher,
  lines: PricingLine[],
  ctx: PricingContext,
  sel?: VoucherSelection,
): number {
  const hit = voucherLines(v, lines);
  let base = sum(hit);
  if (v.kind === 'platform' && sel) {
    for (const shopId of shopsOf(hit)) {
      const sv = byId(ctx, sel.shop[shopId]);
      if (!sv) continue;
      const shopLines = lines.filter((l) => l.shopId === shopId);
      const shopTotal = sum(shopLines);
      if (shopTotal === 0) continue;
      base -= Math.round(
        (voucherDiscount(sv, shopLines, ctx) * sum(hit.filter((l) => l.shopId === shopId))) /
          shopTotal,
      );
    }
  }
  return base;
}

export function voucherCheck(
  v: Voucher,
  lines: PricingLine[],
  ctx: PricingContext,
  sel?: VoucherSelection,
): VoucherCheck {
  const base = eligibleBase(v, lines, ctx, sel);
  if (v.status !== 'active') return { ok: false, base, reason: 'not_active', minOrder: v.minOrder };
  if (sum(voucherLines(v, lines)) === 0)
    return { ok: false, base, reason: 'no_items', minOrder: v.minOrder };
  if (base < v.minOrder) return { ok: false, base, reason: 'min_order', minOrder: v.minOrder };
  return { ok: true, base };
}

/** Phí ship của các shop có dòng trong `hit` (tính trên toàn bộ dòng của shop đó trong `all`). */
const shipOf = (hit: PricingLine[], all: PricingLine[], ctx: PricingContext) =>
  shopsOf(hit).reduce((n, id) => n + ctx.ship(id, all), 0);

/** Số tiền voucher giảm được (0 nếu chưa đủ điều kiện). */
export function voucherDiscount(
  v: Voucher,
  lines: PricingLine[],
  ctx: PricingContext,
  sel?: VoucherSelection,
): number {
  const c = voucherCheck(v, lines, ctx, sel);
  if (!c.ok) return 0;
  if (v.type === 'FREESHIP')
    return Math.min(v.cap ?? Infinity, shipOf(voucherLines(v, lines), lines, ctx));
  if (v.type === 'PERCENT')
    return Math.min(Math.round((c.base * v.value) / 100), v.cap ?? Infinity);
  return Math.min(v.value, c.base);
}

/** Chia `amount` theo trọng số; phần dư do làm tròn dồn vào phần tử cuối. */
function prorate<K extends string | number>(
  amount: number,
  parts: { key: K; weight: number }[],
): Record<K, number> {
  const total = parts.reduce((n, p) => n + p.weight, 0);
  const out = {} as Record<K, number>;
  let left = amount;
  parts.forEach((p, i) => {
    const cut = i === parts.length - 1 ? left : Math.round((amount * p.weight) / (total || 1));
    out[p.key] = cut;
    left -= cut;
  });
  return out;
}

export function priceCart(
  lines: PricingLine[],
  sel: VoucherSelection,
  ctx: PricingContext,
  points: { use: boolean; balance: number },
): PriceBreakdown {
  const subtotal = sum(lines);
  const shops = shopsOf(lines);
  const shipping = shops.reduce((n, id) => n + ctx.ship(id, lines), 0);

  // Tầng 1: voucher shop
  const shopByShop: Record<number, number> = {};
  for (const id of shops) {
    const v = byId(ctx, sel.shop[id]);
    shopByShop[id] = v
      ? voucherDiscount(
          v,
          lines.filter((l) => l.shopId === id),
          ctx,
        )
      : 0;
  }
  const shopDiscount = Object.values(shopByShop).reduce((a, b) => a + b, 0);

  // Tầng 2: voucher sàn, chặn để tổng tầng 1 + 2 ≤ 85% tiền hàng
  const pv = byId(ctx, sel.platform);
  const fv = byId(ctx, sel.freeship);
  const platformDiscount = pv
    ? Math.max(
        0,
        Math.min(
          voucherDiscount(pv, lines, ctx, sel),
          Math.floor(subtotal * MAX_VOUCHER_SHARE) - shopDiscount,
        ),
      )
    : 0;
  const perLine: Record<string, number> =
    pv && platformDiscount > 0
      ? prorate(
          platformDiscount,
          voucherLines(pv, lines).map((l) => ({ key: l.key, weight: l.price * l.qty })),
        )
      : {};

  // Tầng 3: freeship, phân bổ theo phí ship từng shop
  const freeshipDiscount = fv ? voucherDiscount(fv, lines, ctx) : 0;
  const shipCutByShop: Record<number, number> =
    fv && freeshipDiscount > 0
      ? prorate(
          freeshipDiscount,
          shopsOf(voucherLines(fv, lines)).map((id) => ({ key: id, weight: ctx.ship(id, lines) })),
        )
      : {};

  // Tầng 4: Điểm
  const afterVouchers = Math.max(
    0,
    subtotal + shipping - platformDiscount - freeshipDiscount - shopDiscount,
  );
  const pointsCap = Math.floor(
    Math.max(0, subtotal - platformDiscount - shopDiscount) * POINTS_RATIO,
  );
  const pointsDiscount = points.use ? Math.min(Math.max(0, points.balance), pointsCap) : 0;

  return {
    subtotal,
    shipping,
    platformDiscount,
    freeshipDiscount,
    shopDiscount,
    shopByShop,
    shipCutByShop,
    perLine,
    pointsCap,
    pointsDiscount,
    discountTotal: platformDiscount + freeshipDiscount + shopDiscount + pointsDiscount,
    total: afterVouchers - pointsDiscount,
    platformVoucher: platformDiscount > 0 ? pv : null,
    freeshipVoucher: freeshipDiscount > 0 ? fv : null,
  };
}

/** Voucher còn dùng được trong kho (đã lưu, đang hiệu lực). */
export const usableVouchers = (vs: Voucher[]): Voucher[] =>
  vs.filter((v) => v.saved && v.status === 'active');

/**
 * Bộ mã giảm nhiều nhất (prototype bestVouchers): voucher shop tốt nhất cho từng shop, rồi voucher
 * sàn và freeship tốt nhất trên phần còn lại.
 */
export function bestVouchers(lines: PricingLine[], ctx: PricingContext): VoucherSelection {
  const pool = usableVouchers(ctx.vouchers);
  const top = (list: Voucher[], ls: PricingLine[], s?: VoucherSelection) =>
    list
      .map((v) => ({ v, d: voucherDiscount(v, ls, ctx, s) }))
      .sort((a, b) => b.d - a.d)
      .find((x) => x.d > 0)?.v ?? null;
  const sel = emptySelection();
  for (const id of shopsOf(lines)) {
    const best = top(
      pool.filter((v) => v.kind === 'shop' && v.shopId === id),
      lines.filter((l) => l.shopId === id),
    );
    if (best) sel.shop[id] = best.id;
  }
  sel.platform =
    top(
      pool.filter((v) => v.kind === 'platform'),
      lines,
      sel,
    )?.id ?? null;
  sel.freeship =
    top(
      pool.filter((v) => v.kind === 'freeship'),
      lines,
      sel,
    )?.id ?? null;
  return sel;
}

/** Bỏ voucher không còn hợp lệ với các dòng hiện tại (đổi lựa chọn dòng, xóa dòng). */
export function pruneVoucherSelection(
  sel: VoucherSelection,
  lines: PricingLine[],
  ctx: PricingContext,
): VoucherSelection {
  const keep = (id: string | null, ls: PricingLine[], s?: VoucherSelection) => {
    const v = byId(ctx, id);
    return v && voucherCheck(v, ls, ctx, s).ok ? v.id : null;
  };
  const shop: Record<number, string> = {};
  for (const [k, id] of Object.entries(sel.shop)) {
    const sid = Number(k);
    const kept = keep(
      id,
      lines.filter((l) => l.shopId === sid),
    );
    if (kept) shop[sid] = kept;
  }
  const next = { platform: null, freeship: null, shop } as VoucherSelection;
  next.platform = keep(sel.platform, lines, next);
  next.freeship = keep(sel.freeship, lines);
  return next;
}

// ---- Lưu lựa chọn dạng mã (genie.checkout.draft.voucher_codes) ----

export function selectionToCodes(sel: VoucherSelection, vouchers: Voucher[]): string[] {
  return [sel.platform, sel.freeship, ...Object.values(sel.shop)]
    .map((id) => vouchers.find((v) => v.id === id)?.code)
    .filter((c): c is string => !!c);
}

export function selectionFromCodes(codes: string[], vouchers: Voucher[]): VoucherSelection {
  const sel = emptySelection();
  for (const code of codes) {
    const v = vouchers.find((x) => x.code === code);
    if (!v) continue;
    if (v.kind === 'platform') sel.platform = v.id;
    else if (v.kind === 'freeship') sel.freeship = v.id;
    else if (v.shopId != null) sel.shop[v.shopId] = v.id;
  }
  return sel;
}

/** Tiết kiệm thêm nếu đổi sang bộ mã tốt nhất (API_SPEC 6.8 best_selection.extra_saving). */
export function extraSaving(
  lines: PricingLine[],
  sel: VoucherSelection,
  ctx: PricingContext,
): { best: VoucherSelection; saving: number } {
  const best = bestVouchers(lines, ctx);
  const none = { use: false, balance: 0 };
  const now = priceCart(lines, sel, ctx, none).total;
  const then = priceCart(lines, best, ctx, none).total;
  return { best, saving: Math.max(0, now - then) };
}
