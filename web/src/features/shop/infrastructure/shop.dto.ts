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
