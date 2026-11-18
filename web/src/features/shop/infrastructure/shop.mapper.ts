import { slugify } from '@/shared/lib/slug';
import type { Shop } from '../domain/shop.types';
import type { FeaturedShopDto, ShopDto, ShopPageDto } from './shop.dto';

export const toShop = (d: ShopDto): Shop => ({
  id: d.id,
  slug: d.slug,
  name: d.name,
  authentic: d.is_authentic,
  favorite: d.is_favorite,
  rating: d.rating,
  followerCount: d.follower_count,
  location: d.location,
  shipFee: d.ship_fee,
  freeShipFrom: d.free_ship_from,
  logoBg: d.logo_bg,
  coverImage: d.cover_image,
  joinedYear: d.joined_year,
  responseRate: d.response_rate,
  responseTime: d.response_time,
  description: d.description,
  vacation: d.vacation,
  followed: d.followed,
});

// ===== API thật (plan 2026-11-17 C07.06) =====

/** "2026-07-15" → "15/07/2026" (định dạng hiển thị của miền ShopVacation). */
const isoToVn = (iso: string | null): string => {
  const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(iso ?? '');
  return m ? `${m[3]}/${m[2]}/${m[1]}` : '';
};

const BASE: Omit<Shop, 'id' | 'slug' | 'name'> = {
  authentic: false,
  favorite: false,
  rating: 0,
  followerCount: 0,
  location: '',
  // phí ship / ngưỡng freeship không có ở API trang shop — tính ở checkout (API 6.8)
  shipFee: 0,
  freeShipFrom: null,
  logoBg: '',
  coverImage: '',
  joinedYear: 0,
  responseRate: 0,
  responseTime: '',
  description: '',
  vacation: null,
  // trạng thái theo dõi lấy từ /me/following/shop-ids (plan Theo dõi gian hàng)
  followed: false,
};

export const fromShopPageDto = (d: ShopPageDto): Shop => ({
  ...BASE,
  id: d.id,
  slug: slugify(d.name),
  name: d.name,
  authentic: d.is_mall,
  favorite: d.is_favorite,
  rating: d.rating_avg,
  followerCount: d.follower_count,
  location: d.province_name ?? '',
  coverImage: d.cover_url ?? '',
  joinedYear: d.joined_at ? Number(d.joined_at.slice(0, 4)) : 0,
  responseRate: d.chat_response_rate ?? 0,
  description: d.description ?? '',
  vacation: d.vacation
    ? { until: isoToVn(d.vacation.until), message: d.vacation.message ?? '' }
    : null,
  responseMinutes: d.avg_response_minutes,
  categories: d.categories,
  logoUrl: d.logo_url,
});

export const fromFeaturedShopDto = (d: FeaturedShopDto): Shop => ({
  ...BASE,
  id: d.id,
  slug: slugify(d.name),
  name: d.name,
  rating: d.rating_avg,
  followerCount: d.follower_count,
  location: d.province_name ?? '',
  logoUrl: d.logo_url,
});
