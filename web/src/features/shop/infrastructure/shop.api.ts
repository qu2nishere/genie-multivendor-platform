// Shop công khai + theo dõi shop. Trang shop: mock khi `isMocked('shop')`, thật /api/v1/public/shops (API_SPEC 5.11,
// 5.12; plan 2026-11-17 C07.06). Theo dõi shop: mock khi USE_MOCK — thuộc plan Theo dõi gian hàng.
import { api, request } from '@/shared/api/http';
import { mockDelay, mockFail, USE_MOCK } from '@/shared/api/mock';
import { env, isMocked } from '@/shared/config/env';
import type { Shop } from '../domain/shop.types';
import type { FeaturedShopDto, ShopDto, ShopPageDto } from './shop.dto';
import { fromFeaturedShopDto, fromShopPageDto, toShop } from './shop.mapper';
import { setMockFollow, shopDtos } from './shop.mock';

const baseUrl = () =>
  typeof window === 'undefined' ? (env.INTERNAL_API_URL ?? env.PUBLIC_API_URL) : env.PUBLIC_API_URL;

const SHOP_MOCK = isMocked('shop');

export async function getShop(id: number): Promise<Shop> {
  if (SHOP_MOCK) {
    await mockDelay();
    const s = shopDtos().find((x) => x.id === id);
    if (!s) mockFail('GNE-VND-4041', { status: 404, message: 'Shop not found' });
    return toShop(s);
  }
  return fromShopPageDto(
    (await request<ShopPageDto>(`/public/shops/${id}`, { auth: false, baseUrl: baseUrl() })).data,
  );
}

/** Shop đã xác minh nổi bật (trang chủ "Shop đã xác minh"). */
export async function listShops(): Promise<Shop[]> {
  if (SHOP_MOCK) {
    await mockDelay();
    return shopDtos().map(toShop);
  }
  return (
    await request<FeaturedShopDto[]>('/public/shops', {
      auth: false,
      baseUrl: baseUrl(),
      query: { featured: true, limit: 24 },
    })
  ).data.map(fromFeaturedShopDto);
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
