import type { Banner, BannerPosition } from '../domain/banner.types';
import type { BannerDto, LiveBannerDto } from './banner.dto';

export const toBanner = (d: BannerDto): Banner => ({
  id: d.id,
  title: d.title,
  image: d.image_url,
  imageMobile: d.image_mobile_url,
  href: d.link,
  position: d.position,
  categorySlug: d.category_slug,
  order: d.sort_order,
  startsAt: d.starts_at,
  endsAt: d.ends_at,
  on: d.is_active,
});

/** API thật chỉ trả banner đang LIVE đã sắp — lịch / công tắc không cần ở storefront. */
export const toLiveBanner = (
  d: LiveBannerDto,
  position: BannerPosition,
  order: number,
  categorySlug: string | null,
): Banner => ({
  id: String(d.id),
  title: d.title,
  image: d.image_url,
  imageMobile: d.mobile_image_url,
  href: d.target_url,
  position,
  categorySlug,
  order,
  startsAt: '',
  endsAt: '',
  on: true,
});
