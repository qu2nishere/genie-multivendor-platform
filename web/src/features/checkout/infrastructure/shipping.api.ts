import { api } from '@/shared/api/http';
import { mockDelay, USE_MOCK } from '@/shared/api/mock';
import { SEED_SHOPS } from '@/shared/api/mock/seed';
import type { ShipPolicy } from '../domain/shipping.rules';
import type { ShipPolicyDto } from './shipping.dto';

const toPolicy = (d: ShipPolicyDto): ShipPolicy => ({
  fee: d.shipping_fee,
  freeFrom: d.free_ship_from,
});

/**
 * Phí ship mặc định của các shop (giao tới địa chỉ mặc định). API_SPEC chưa tách endpoint này —
 * dùng tạm GET /public/shops/shipping-policies?ids= (ghi vào danh sách nối backend G13.20).
 */
export async function getShipPolicies(shopIds: number[]): Promise<Record<number, ShipPolicy>> {
  if (!shopIds.length) return {};
  if (USE_MOCK) {
    await mockDelay(80);
    return Object.fromEntries(
      SEED_SHOPS.filter((s) => shopIds.includes(s.id)).map((s) => [
        s.id,
        toPolicy({ shop_id: s.id, shipping_fee: s.ship_fee, free_ship_from: s.free_ship_from }),
      ]),
    );
  }
  const list = await api.get<ShipPolicyDto[]>('/public/shops/shipping-policies', {
    auth: false,
    query: { ids: shopIds.join(',') },
  });
  return Object.fromEntries(list.map((d) => [d.shop_id, toPolicy(d)]));
}
