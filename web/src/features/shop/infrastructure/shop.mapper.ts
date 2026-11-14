import type { Shop } from '../domain/shop.types';
import type { ShopDto } from './shop.dto';

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
