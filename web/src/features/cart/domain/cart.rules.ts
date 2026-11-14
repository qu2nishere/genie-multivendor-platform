// Quy tắc giỏ hàng — TS thuần, không React / fetch (TS 2.4). Mở rộng (groupByShop, tạm tính) ở G10.01.
import type { CartLine, CartLineInput, GuestCart } from './cart.types';

/** Giỏ khách: tối đa 50 dòng, giữ 30 ngày (LUU_TRU_DU_LIEU 6.3). */
export const MAX_GUEST_LINES = 50;
export const GUEST_TTL_DAYS = 30;

/** Khóa dòng = sản phẩm + phân loại (prototype key = id|variant). */
export const lineKeyOf = (productId: number, variantLabel: string): string =>
  `${productId}|${variantLabel}`;

/** Thêm vào danh sách dòng: cùng phân loại thì cộng dồn, không vượt tồn. */
export function mergeLine(lines: CartLine[], input: CartLineInput): CartLine[] {
  const key = lineKeyOf(input.productId, input.variantLabel);
  const hit = lines.find((l) => l.key === key);
  if (hit) {
    return lines.map((l) =>
      l.key === key
        ? { ...l, qty: Math.min(l.qty + input.qty, Math.max(1, input.stock)), stock: input.stock }
        : l,
    );
  }
  return [
    ...lines,
    {
      key,
      productId: input.productId,
      productSlug: input.productSlug,
      shopId: input.shopId,
      title: input.title,
      image: input.image,
      variantLabel: input.variantLabel,
      price: input.price,
      originalPrice: input.originalPrice,
      snapshotPrice: input.price,
      qty: Math.min(input.qty, Math.max(1, input.stock)),
      stock: input.stock,
    },
  ];
}

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
