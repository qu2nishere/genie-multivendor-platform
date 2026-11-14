import type { Banner } from '../domain/banner.types';
import type { BannerDto } from './banner.dto';

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
