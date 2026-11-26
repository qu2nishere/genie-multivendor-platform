import { api } from '@/shared/api/http';
import { mockDelay } from '@/shared/api/mock';
import { SEED_SHOPS } from '@/shared/api/mock/seed';
import { isMocked } from '@/shared/config/env';
import type { ShipPolicy } from '../domain/shipping.rules';
import type { PreviewDto, PreviewShopDto, ShipPolicyDto } from './shipping.dto';

/** Giỏ / thanh toán nối API thật khi `PUBLIC_REAL_API` có `checkout` (plan C2 O06.06). */
const CHECKOUT_MOCK = isMocked('checkout');

/**
 * Voucher / Điểm chỉ tính được khi chạy mock hoàn toàn hoặc backend đã có nhóm `voucher` (chuỗi C6). Nối `checkout`
 * thật mà chưa có voucher → server không áp giảm giá (API 6.8, plan C2 D2) → giao diện ẩn ô Voucher / Điểm.
 */
export const DISCOUNTS_ENABLED = CHECKOUT_MOCK || !isMocked('voucher');

const toPolicy = (d: ShipPolicyDto): ShipPolicy => ({
  fee: d.shipping_fee,
  freeFrom: d.free_ship_from,
});

/** Khối shop 6.8 → chính sách phí: phí trước freeship + ngưỡng (web tính lại khi bỏ chọn bớt món). */
export const toPreviewPolicy = (d: PreviewShopDto): ShipPolicy => ({
  fee: d.base_shipping_fee ?? d.shipping_fee ?? 0,
  freeFrom: d.free_ship_threshold,
  estimated: d.is_estimated_fee,
});

/** Dòng giỏ cần báo phí (thật) — mọi dòng còn mua được + địa chỉ đang chọn (null = địa chỉ mặc định). */
export interface ShipQuoteScope {
  cartItemIds: number[];
  addressId: number | null;
}

/**
 * Phí ship của các shop. Mock: phí cố định của shop mẫu. Thật: POST /me/checkout/preview (6.8) với các dòng giỏ —
 * thay cho GET /public/shops/shipping-policies (đã bỏ khỏi spec, handoff dòng 44).
 */
export async function getShipPolicies(
  shopIds: number[],
  scope?: ShipQuoteScope,
): Promise<Record<number, ShipPolicy>> {
  if (!shopIds.length) return {};
  if (CHECKOUT_MOCK) {
    await mockDelay(80);
    return Object.fromEntries(
      SEED_SHOPS.filter((s) => shopIds.includes(s.id)).map((s) => [
        s.id,
        toPolicy({ shop_id: s.id, shipping_fee: s.ship_fee, free_ship_from: s.free_ship_from }),
      ]),
    );
  }
  if (!scope || !scope.cartItemIds.length) return {};
  const r = await api.post<PreviewDto>('/me/checkout/preview', {
    cart_item_ids: scope.cartItemIds,
    address_id: scope.addressId,
  });
  return Object.fromEntries(r.shops.map((d) => [d.shop.id, toPreviewPolicy(d)]));
}
