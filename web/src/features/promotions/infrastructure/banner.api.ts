// Banner công khai (GET /public/banners) + đếm lượt xem / bấm (POST /public/banners/{id}/events).
import { request } from '@/shared/api/http';
import { mockDelay, USE_MOCK } from '@/shared/api/mock';
import { env } from '@/shared/config/env';
import { liveBanners, localDay } from '../domain/banner.rules';
import type { Banner, BannerPosition } from '../domain/banner.types';
import type { BannerDto } from './banner.dto';
import { toBanner } from './banner.mapper';
import { BANNER_DTOS } from './banner.mock';

const baseUrl = () =>
  typeof window === 'undefined' ? (env.INTERNAL_API_URL ?? env.PUBLIC_API_URL) : env.PUBLIC_API_URL;

/** Banner đang chạy của một vị trí (lọc theo ngày ở server; mock lọc giống prototype). */
export async function listLiveBanners(
  position: BannerPosition,
  categorySlug?: string,
): Promise<Banner[]> {
  if (USE_MOCK) {
    await mockDelay();
    return liveBanners(BANNER_DTOS.map(toBanner), position, localDay(), categorySlug);
  }
  const res = await request<BannerDto[]>('/public/banners', {
    auth: false,
    baseUrl: baseUrl(),
    query: { position, category: categorySlug },
  });
  return res.data.map(toBanner);
}

/** Ghi nhận lượt xem / bấm (thống kê ở Admin → Banner). Lỗi mạng bỏ qua. */
export async function trackBanner(id: string, event: 'view' | 'click'): Promise<void> {
  if (USE_MOCK) return;
  try {
    await request(`/public/banners/${encodeURIComponent(id)}/events`, {
      method: 'POST',
      auth: false,
      body: { event },
    });
  } catch {
    /* không chặn điều hướng */
  }
}
