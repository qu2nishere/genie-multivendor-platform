// Shop công khai + theo dõi shop. Mock khi USE_MOCK; sau này /api/v1/public/shops, /me/shop-follows.
import { api, request } from '@/shared/api/http';
import { mockDelay, mockFail, USE_MOCK } from '@/shared/api/mock';
import { env } from '@/shared/config/env';
import type { Shop } from '../domain/shop.types';
import type { ShopDto } from './shop.dto';
import { toShop } from './shop.mapper';
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

export async function listFollowedShops(): Promise<Shop[]> {
  if (USE_MOCK) {
    await mockDelay();
    return shopDtos()
      .filter((s) => s.followed)
      .map(toShop);
  }
  return (await api.get<ShopDto[]>('/me/shop-follows')).map(toShop);
}

export async function setFollow(id: number, on: boolean): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(150);
    setMockFollow(id, on);
    return;
  }
  if (on) await api.post(`/me/shop-follows`, { shop_id: id });
  else await api.delete(`/me/shop-follows/${id}`);
}
