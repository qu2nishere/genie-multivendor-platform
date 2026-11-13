// Giỏ hàng của người dùng đã đăng nhập (API_SPEC 6.1–6.7). Mock khi USE_MOCK: kho dòng giỏ giữ
// trong phiên tab (mockState) để sửa ở /cart còn nguyên khi sang /checkout.
import { api } from '@/shared/api/http';
import { mockDelay, mockFail, mockState, USE_MOCK } from '@/shared/api/mock';
import type { CartLine, CartLineInput, GuestCartItem, VariantChoice } from '../domain/cart.types';
import type { BatchAddResultDto, CartDto, CartItemDto, VariantChoiceDto } from './cart.dto';
import { toCartLines, toVariantChoice } from './cart.mapper';
import {
  rowFromVariant,
  seedCartRows,
  toCartDto,
  variantChoiceDto,
  type MockCartRow,
} from './cart.mock';

const db = mockState<MockCartRow[]>('cart', seedCartRows);
const nextId = () => Math.max(7000, ...db.get().map((r) => r.item.id)) + 1;

/** Mock: thêm SKU, cùng SKU cộng dồn và chặn ở tồn kho (6.3 / 6.6 / 6.7). */
function mockAdd(variantId: number, qty: number): 'added' | 'merged' | 'skipped' {
  const rows = db.get();
  const hit = rows.find((r) => r.item.variant_id === variantId);
  if (hit) {
    db.set(
      rows.map((r) =>
        r === hit
          ? {
              ...r,
              item: {
                ...r.item,
                quantity: Math.min(r.item.quantity + qty, Math.max(1, r.item.stock)),
              },
            }
          : r,
      ),
    );
    return 'merged';
  }
  const row = rowFromVariant(variantId, qty, nextId());
  if (!row || !row.item.is_available) return 'skipped';
  db.set([...rows, row]);
  return 'added';
}

export async function fetchCart(): Promise<CartLine[]> {
  if (USE_MOCK) {
    await mockDelay();
    return toCartLines(toCartDto(db.get()));
  }
  return toCartLines(await api.get<CartDto>('/me/cart'));
}

/** POST /me/cart/items — thêm SKU (cùng SKU cộng dồn phía server, cắt theo tồn) → giỏ mới. */
export async function addCartItem(input: CartLineInput): Promise<CartLine[]> {
  if (USE_MOCK) {
    await mockDelay(250);
    if (input.stock <= 0) mockFail('GNE-INV-4094', { details: { stock: 0 } });
    mockAdd(input.variantId, input.qty);
    return fetchCart();
  }
  await api.post('/me/cart/items', { variant_id: input.variantId, quantity: input.qty });
  return fetchCart();
}

/** PATCH /me/cart/items/{id} — đổi số lượng hoặc đổi phân loại (gộp dòng trùng SKU). */
export async function updateCartItem(
  id: number,
  patch: { quantity: number } | { variant_id: number },
): Promise<CartLine[]> {
  if (USE_MOCK) {
    await mockDelay(200);
    const rows = db.get();
    const row = rows.find((r) => r.item.id === id);
    if (!row) mockFail('GNE-ORD-4041');
    if ('quantity' in patch) {
      if (patch.quantity > row.item.stock)
        mockFail('GNE-INV-4094', { details: { stock: row.item.stock } });
      db.set(
        rows.map((r) => (r === row ? { ...r, item: { ...r.item, quantity: patch.quantity } } : r)),
      );
    } else {
      const twin = rows.find((r) => r.item.variant_id === patch.variant_id && r !== row);
      const next = rowFromVariant(patch.variant_id, row.item.quantity, row.item.id);
      if (!next) mockFail('GNE-VAL-4220');
      if (!next.item.is_available) mockFail('GNE-INV-4094', { details: { stock: 0 } });
      db.set(
        twin
          ? rows
              .filter((r) => r !== row)
              .map((r) =>
                r === twin
                  ? {
                      ...r,
                      item: {
                        ...r.item,
                        quantity: Math.min(r.item.quantity + row.item.quantity, r.item.stock),
                      },
                    }
                  : r,
              )
          : rows.map((r) => (r === row ? next : r)),
      );
    }
    return fetchCart();
  }
  await api.patch<{ item: CartItemDto; merged_from_id?: number }>(`/me/cart/items/${id}`, patch);
  return fetchCart();
}

/** DELETE /me/cart/items?ids= — xóa một hoặc nhiều dòng. */
export async function removeCartItems(ids: number[]): Promise<CartLine[]> {
  if (!ids.length) return fetchCart();
  if (USE_MOCK) {
    await mockDelay(200);
    db.set(db.get().filter((r) => !ids.includes(r.item.id)));
    return fetchCart();
  }
  await api.delete(`/me/cart/items`, { query: { ids: ids.join(',') } });
  return fetchCart();
}

/** POST /me/cart/items/batch — Mua lại: thêm nhiều SKU, bỏ qua món không còn bán. */
export async function addCartItemsBatch(
  items: { variantId: number; qty: number }[],
): Promise<{ added: number; skipped: number }> {
  if (USE_MOCK) {
    await mockDelay(250);
    let added = 0;
    let skipped = 0;
    for (const it of items) {
      if (mockAdd(it.variantId, it.qty) === 'skipped') skipped += 1;
      else added += 1;
    }
    return { added, skipped };
  }
  const r = await api.post<BatchAddResultDto>('/me/cart/items/batch', {
    items: items.map((i) => ({ variant_id: i.variantId, quantity: i.qty })),
  });
  return { added: r.added, skipped: r.skipped.length };
}

/** POST /me/cart/merge — gộp giỏ khách sau đăng nhập (cùng SKU cộng dồn, chặn theo tồn). */
export async function mergeCart(items: GuestCartItem[]): Promise<{ merged: number }> {
  if (USE_MOCK) {
    await mockDelay(250);
    let merged = 0;
    for (const it of items) {
      if (mockAdd(it.product_variant_id, it.quantity) !== 'skipped') merged += 1;
    }
    return { merged };
  }
  return api.post<{ merged: number }>('/me/cart/merge', {
    items: items.map((i) => ({ variant_id: i.product_variant_id, quantity: i.quantity })),
  });
}

/**
 * Dòng hiển thị cho giỏ khách (chỉ có SKU + số lượng). API_SPEC chưa có endpoint công khai tra SKU →
 * dùng tạm GET /public/variants?ids= (ghi vào danh sách nối backend G13.20).
 */
export async function resolveGuestItems(items: GuestCartItem[]): Promise<CartLine[]> {
  if (!items.length) return [];
  if (USE_MOCK) {
    await mockDelay(150);
    const rows = items
      .map((it, i) => rowFromVariant(it.product_variant_id, it.quantity, -(i + 1)))
      .filter((r): r is MockCartRow => r !== null);
    return toCartLines(toCartDto(rows));
  }
  return toCartLines(
    await api.get<CartDto>('/public/variants', {
      query: { ids: items.map((i) => `${i.product_variant_id}:${i.quantity}`).join(',') },
    }),
  );
}

/** GET /public/products/{id}/variants — nhóm phân loại + SKU để đổi phân loại trong giỏ. */
export async function getVariantChoice(productId: number): Promise<VariantChoice> {
  if (USE_MOCK) {
    await mockDelay(120);
    const d = variantChoiceDto(productId);
    if (!d) mockFail('GNE-CAT-4041');
    return toVariantChoice(d);
  }
  return toVariantChoice(
    await api.get<VariantChoiceDto>(`/public/products/${productId}/variants`, { auth: false }),
  );
}
