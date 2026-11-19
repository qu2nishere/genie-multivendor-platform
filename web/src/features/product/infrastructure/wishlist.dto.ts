// GET /me/wishlist (API_SPEC 3.10) — snake_case đúng phản hồi; product_slug là trường bổ sung (handoff mục 3).
export interface WishlistItemDto {
  product_id: number;
  product_slug: string;
  name: string;
  image_url: string | null;
  price: number;
  default_variant_id: number | null;
  is_purchasable: boolean;
}
