// Quy tắc giỏ hàng — TS thuần, không React / fetch (TS 2.4; plan G10.01).
import type {
  CartGroup,
  CartLine,
  CartLineInput,
  GuestCart,
  PriceChange,
  QtyDecision,
  VariantChoice,
} from './cart.types';

/** Giỏ khách: tối đa 50 dòng, giữ 30 ngày (LUU_TRU_DU_LIEU 6.3). */
export const MAX_GUEST_LINES = 50;
export const GUEST_TTL_DAYS = 30;

/** Khóa dòng = sản phẩm + phân loại (prototype key = id|variant). */
export const lineKeyOf = (productId: number, variantLabel: string): string =>
  `${productId}|${variantLabel}`;

/** Thành tiền một dòng (prototype lineTotal cho 1 dòng). */
export const lineSubtotal = (l: Pick<CartLine, 'price' | 'qty'>): number => l.price * l.qty;

/** Tổng tiền hàng các dòng. */
export const linesSubtotal = (lines: Pick<CartLine, 'price' | 'qty'>[]): number =>
  lines.reduce((n, l) => n + lineSubtotal(l), 0);

/** Nhóm dòng theo shop, giữ thứ tự xuất hiện đầu tiên của shop. */
export function groupByShop(lines: CartLine[]): CartGroup[] {
  const groups: CartGroup[] = [];
  for (const l of lines) {
    const g = groups.find((x) => x.shop.id === l.shop.id);
    if (g) g.lines.push(l);
    else groups.push({ shop: l.shop, lines: [l] });
  }
  return groups;
}

/** Giá đổi so với lúc thêm vào giỏ. */
export function priceChange(l: Pick<CartLine, 'price' | 'snapshotPrice'>): PriceChange {
  if (l.snapshotPrice == null || l.snapshotPrice === l.price) return null;
  return l.price > l.snapshotPrice ? 'up' : 'down';
}

/** Đổi số lượng: về 0 → hỏi xóa; vượt tồn → báo giới hạn; còn lại → đặt số mới. */
export function decideQty(l: Pick<CartLine, 'stock'>, qty: number): QtyDecision {
  if (qty <= 0) return { kind: 'confirm_remove' };
  if (l.stock > 0 && qty > l.stock) return { kind: 'over_stock', stock: l.stock };
  return { kind: 'set', qty };
}

/** Dòng mua được (còn hàng, shop mở, sản phẩm còn bán). */
export const isBuyable = (l: Pick<CartLine, 'available'>): boolean => l.available;

// ---- Chọn dòng (Set khóa dòng) ----

export function toggleKey(sel: ReadonlySet<string>, key: string): Set<string> {
  const n = new Set(sel);
  if (n.has(key)) n.delete(key);
  else n.add(key);
  return n;
}

/** Chọn / bỏ cả nhóm shop: đã chọn hết → bỏ hết, còn thiếu → chọn hết. */
export function toggleGroup(sel: ReadonlySet<string>, keys: string[]): Set<string> {
  const n = new Set(sel);
  const all = keys.every((k) => n.has(k));
  for (const k of keys) {
    if (all) n.delete(k);
    else n.add(k);
  }
  return n;
}

export type CheckState = 'all' | 'some' | 'none';

export function checkState(sel: ReadonlySet<string>, keys: string[]): CheckState {
  const n = keys.filter((k) => sel.has(k)).length;
  if (n === 0) return 'none';
  return n === keys.length ? 'all' : 'some';
}

/** Bỏ khóa không còn trong giỏ (sau khi xóa / đổi phân loại / tải lại). */
export function pruneSelection(sel: ReadonlySet<string>, lines: CartLine[]): Set<string> {
  const keys = new Set(lines.map((l) => l.key));
  return new Set([...sel].filter((k) => keys.has(k)));
}

/** Lựa chọn ban đầu: "Mua ngay" chỉ chọn dòng vừa thêm; còn lại chọn mọi dòng mua được. */
export function initialSelection(lines: CartLine[], buyKey: string | null): Set<string> {
  if (buyKey && lines.some((l) => l.key === buyKey)) return new Set([buyKey]);
  return new Set(lines.filter(isBuyable).map((l) => l.key));
}

// ---- Cộng dồn / đổi phân loại ----

/** Thêm vào danh sách dòng: cùng phân loại thì cộng dồn, không vượt tồn. */
export function mergeLine(
  lines: CartLine[],
  input: CartLineInput,
  make: (input: CartLineInput) => CartLine,
): CartLine[] {
  const key = lineKeyOf(input.productId, input.variantLabel);
  const hit = lines.find((l) => l.key === key);
  if (hit) {
    return lines.map((l) =>
      l.key === key
        ? { ...l, qty: Math.min(l.qty + input.qty, Math.max(1, input.stock)), stock: input.stock }
        : l,
    );
  }
  return [...lines, make({ ...input, qty: Math.min(input.qty, Math.max(1, input.stock)) })];
}

/**
 * Đổi phân loại của dòng `fromKey` thành `next` (prototype VariantModal onConfirm): nếu giỏ đã có
 * dòng cùng phân loại mới → gộp số lượng vào dòng đó và bỏ dòng cũ (API_SPEC 6.4 merged_from_id).
 */
export function changeVariant(
  lines: CartLine[],
  fromKey: string,
  next: Pick<CartLine, 'variantId' | 'variantLabel' | 'price' | 'stock'>,
): { lines: CartLine[]; key: string } {
  const from = lines.find((l) => l.key === fromKey);
  if (!from) return { lines, key: fromKey };
  const key = lineKeyOf(from.productId, next.variantLabel);
  const twin = lines.find((l) => l.key === key && l.key !== fromKey);
  if (twin) {
    return {
      key,
      lines: lines
        .filter((l) => l.key !== fromKey)
        .map((l) =>
          l.key === key ? { ...l, qty: Math.min(l.qty + from.qty, Math.max(1, l.stock)) } : l,
        ),
    };
  }
  return {
    key,
    lines: lines.map((l) =>
      l.key === fromKey
        ? {
            ...l,
            key,
            variantId: next.variantId,
            variantLabel: next.variantLabel,
            price: next.price,
            stock: next.stock,
            qty: Math.min(l.qty, Math.max(1, next.stock)),
            snapshotPrice: null,
          }
        : l,
    ),
  };
}

// ---- Giỏ khách ----

export function emptyGuestCart(now: Date): GuestCart {
  return {
    version: 1,
    expires_at: new Date(now.getTime() + GUEST_TTL_DAYS * 86_400_000).toISOString(),
    items: [],
  };
}

export const isGuestCartExpired = (c: GuestCart, now: Date): boolean =>
  Date.parse(c.expires_at) <= now.getTime();

export type GuestAddResult =
  { cart: GuestCart; ok: true } | { cart: GuestCart; ok: false; reason: 'full' };

/**
 * Thêm SKU vào giỏ khách: cùng SKU cộng dồn (không vượt `maxQty`), giỏ đầy 50 dòng thì từ chối;
 * mỗi lần thêm gia hạn thêm 30 ngày.
 */
export function addGuestItem(
  cart: GuestCart,
  variantId: number,
  qty: number,
  now: Date,
  maxQty = Number.POSITIVE_INFINITY,
): GuestAddResult {
  const base = isGuestCartExpired(cart, now) ? emptyGuestCart(now) : cart;
  const expires_at = emptyGuestCart(now).expires_at;
  const hit = base.items.find((i) => i.product_variant_id === variantId);
  if (hit) {
    return {
      ok: true,
      cart: {
        ...base,
        expires_at,
        items: base.items.map((i) =>
          i.product_variant_id === variantId
            ? { ...i, quantity: Math.min(i.quantity + qty, maxQty) }
            : i,
        ),
      },
    };
  }
  if (base.items.length >= MAX_GUEST_LINES) return { ok: false, reason: 'full', cart: base };
  return {
    ok: true,
    cart: {
      ...base,
      expires_at,
      items: [
        ...base.items,
        {
          product_variant_id: variantId,
          quantity: Math.min(qty, maxQty),
          added_at: now.toISOString(),
        },
      ],
    },
  };
}

/** Đổi số lượng / bỏ một SKU trong giỏ khách (qty 0 = bỏ). */
export function setGuestQty(cart: GuestCart, variantId: number, qty: number): GuestCart {
  return {
    ...cart,
    items:
      qty <= 0
        ? cart.items.filter((i) => i.product_variant_id !== variantId)
        : cart.items.map((i) => (i.product_variant_id === variantId ? { ...i, quantity: qty } : i)),
  };
}

/** Lựa chọn ban đầu của modal đổi phân loại từ nhãn hiện tại ("Đen / Bản Pro"); giá trị lạ → chưa chọn. */
export function pickFromLabel(choice: VariantChoice, label: string): (string | null)[] {
  const parts = label.split(' / ');
  return choice.groups.map((g, i) => (g.values.includes(parts[i]) ? parts[i] : null));
}

/** SKU ứng với lựa chọn đủ mọi nhóm; thiếu nhóm → null. */
export function variantOfPick(
  choice: VariantChoice,
  pick: (string | null)[],
): VariantChoice['variants'][number] | null {
  if (pick.some((p) => p == null)) return null;
  return choice.variants.find((v) => v.options.every((o, i) => o === pick[i])) ?? null;
}
