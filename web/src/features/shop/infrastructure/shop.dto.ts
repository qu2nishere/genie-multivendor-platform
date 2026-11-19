// GET /public/shops/{id}, /me/shop-follows
export interface ShopDto {
  id: number;
  slug: string;
  name: string;
  is_authentic: boolean;
  is_favorite: boolean;
  rating: number;
  follower_count: number;
  location: string;
  ship_fee: number;
  free_ship_from: number | null;
  logo_bg: string;
  cover_image: string;
  joined_year: number;
  response_rate: number;
  response_time: string;
  description: string;
  vacation: { until: string; message: string } | null;
  followed: boolean;
}

/** GET /me/following (API_SPEC 3.11) — một dòng "Shop đang theo dõi"; `shop.id` = vendor_profiles.id. */
export interface FollowedShopDto {
  shop: { id: number; name: string; logo_url: string | null; is_mall: boolean };
  rating_avg: number;
  follower_count: number;
}

// ===== API thật trang shop (API_SPEC 5.11, 5.12 — plan 2026-11-17 C07.06) =====

/** `GET /public/shops/{id}` */
export interface ShopPageDto {
  id: number;
  name: string;
  logo_url: string | null;
  cover_url: string | null;
  description: string | null;
  is_mall: boolean;
  is_favorite: boolean;
  rating_avg: number;
  follower_count: number;
  product_count: number;
  province_name: string | null;
  chat_response_rate: number | null;
  avg_response_minutes: number | null;
  joined_at: string | null;
  is_online: boolean;
  vacation: { until: string | null; message: string | null } | null;
  categories: { id: number; name: string }[];
}

/** `GET /public/shops?featured=true` */
export interface FeaturedShopDto {
  id: number;
  name: string;
  logo_url: string | null;
  rating_avg: number;
  follower_count: number;
  province_name: string | null;
}
