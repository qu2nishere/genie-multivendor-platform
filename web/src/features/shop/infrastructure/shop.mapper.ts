import type { Shop } from '../domain/shop.types';
import type { FollowedShopDto, ShopDto } from './shop.dto';

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

/**
 * Dòng "Shop đang theo dõi" của API thật → `Shop` (màn chỉ dùng id, tên, Chính hãng, điểm, số người theo dõi); các trường
 * trang shop khác để mặc định trung tính cho tới khi có `GET /public/shops/{id}`.
 */
export const fromFollowedShop = (d: FollowedShopDto): Shop => ({
  id: d.shop.id,
  slug: '',
  name: d.shop.name,
  authentic: d.shop.is_mall,
  favorite: false,
  rating: d.rating_avg,
  followerCount: d.follower_count,
  location: '',
  shipFee: 0,
  freeShipFrom: null,
  logoBg: '',
  coverImage: '',
  joinedYear: 0,
  responseRate: 0,
  responseTime: '',
  description: '',
  vacation: null,
  followed: true,
});
