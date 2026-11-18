// Shop công khai + theo dõi shop. Shop công khai: mock khi USE_MOCK (chờ /api/v1/public/shops). Theo dõi shop
// (API_SPEC 3.11 — UC-SOC-06): nối API thật khi PUBLIC_REAL_API có `follow`.
import { api, request } from '@/shared/api/http';
import { mockDelay, mockFail, USE_MOCK } from '@/shared/api/mock';
import { env, isMocked } from '@/shared/config/env';
import type { Shop } from '../domain/shop.types';
import type { FollowedShopDto, ShopDto } from './shop.dto';
import { fromFollowedShop, toShop } from './shop.mapper';
import { setMockFollow, shopDtos } from './shop.mock';

const baseUrl = () =>
  typeof window === 'undefined' ? (env.INTERNAL_API_URL ?? env.PUBLIC_API_URL) : env.PUBLIC_API_URL;

export async function getShop(id: number): Promise<Shop> {
  if (USE_MOCK) {
    await mockDelay();
    const s = shopDtos().find((x) => x.id === id);
    if (!s) mockFail('GNE-CAT-4041', { status: 404, message: 'Shop not found' });
    return toShop(s);
  }
  return toShop(
    (await request<ShopDto>(`/public/shops/${id}`, { auth: false, baseUrl: baseUrl() })).data,
  );
}

/** Shop đã xác minh nổi bật (trang chủ "Shop đã xác minh"). */
export async function listShops(): Promise<Shop[]> {
  if (USE_MOCK) {
    await mockDelay();
    return shopDtos().map(toShop);
  }
  return (await request<ShopDto[]>('/public/shops', { auth: false, baseUrl: baseUrl() })).data.map(
    toShop,
  );
}

const FOLLOW_MOCK = isMocked('follow');

/** "Shop đang theo dõi": tối đa 50 shop mới theo dõi nhất + tổng số cho tiêu đề (`summary.total`). */
export interface FollowedShops {
  items: Shop[];
  total: number;
}

export async function listFollowedShops(): Promise<FollowedShops> {
  if (FOLLOW_MOCK) {
    await mockDelay();
    const items = shopDtos()
      .filter((s) => s.followed)
      .map(toShop);
    return { items, total: items.length };
  }
  const res = await request<FollowedShopDto[]>('/me/following', { query: { limit: 50 } });
  return {
    items: res.data.map(fromFollowedShop),
    total: Number((res.summary ?? {}).total ?? res.data.length),
  };
}

/** GET /me/following/shop-ids — tập shop_id đang theo dõi (tô nút Theo dõi). */
export async function fetchFollowedShopIds(): Promise<number[]> {
  if (FOLLOW_MOCK) {
    return shopDtos()
      .filter((s) => s.followed)
      .map((s) => s.id);
  }
  return api.get<number[]>('/me/following/shop-ids');
}

/** PUT / DELETE /me/following/{shop_id} — idempotent; shop của chính mình → 409 GNE-SOC-4095; shop ngừng hoạt động → 404. */
export async function setFollow(id: number, on: boolean): Promise<void> {
  if (FOLLOW_MOCK) {
    await mockDelay(150);
    setMockFollow(id, on);
    return;
  }
  if (on) await api.put(`/me/following/${id}`);
  else await api.delete(`/me/following/${id}`);
}
