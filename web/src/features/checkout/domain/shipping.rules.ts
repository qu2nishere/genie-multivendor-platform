// Phí vận chuyển hiển thị (plan G10.11; prototype data.jsx shipFee / shipOf). Mỗi shop một phí cố
// định; shop tự miễn phí khi tiền hàng của shop đạt ngưỡng (shop chịu phí) — khác voucher Freeship.

export interface ShipPolicy {
  fee: number;
  /** Ngưỡng shop tự miễn phí vận chuyển; null = không có. */
  freeFrom: number | null;
}

interface ShipLine {
  shopId: number;
  price: number;
  qty: number;
}

export const shopSubtotal = (shopId: number, lines: ShipLine[]): number =>
  lines.filter((l) => l.shopId === shopId).reduce((n, l) => n + l.price * l.qty, 0);

/** Phí ship của một shop với tiền hàng `subtotal` của shop đó. */
export function shipFee(policy: ShipPolicy | undefined, subtotal: number): number {
  if (!policy) return 0;
  return policy.freeFrom != null && subtotal >= policy.freeFrom ? 0 : policy.fee;
}

/** Shop có đang tự miễn phí vận chuyển cho các dòng này (is_free_shipping_by_shop). */
export const isFreeByShop = (policy: ShipPolicy | undefined, subtotal: number): boolean =>
  !!policy && policy.fee > 0 && shipFee(policy, subtotal) === 0;

/** Hàm phí ship theo shop cho bộ tính giá (ShipFn của promotions). */
export const makeShipFn =
  (policies: Record<number, ShipPolicy>) =>
  (shopId: number, lines: ShipLine[]): number =>
    shipFee(policies[shopId], shopSubtotal(shopId, lines));

/** Tổng phí ship của các shop có dòng trong `lines` (prototype shipOf). */
export function shipOf(lines: ShipLine[], policies: Record<number, ShipPolicy>): number {
  const fn = makeShipFn(policies);
  return [...new Set(lines.map((l) => l.shopId))].reduce((n, id) => n + fn(id, lines), 0);
}
