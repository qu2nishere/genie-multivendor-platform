// GET /me/cart — một dòng giỏ (snake_case).
export interface CartLineDto {
  key: string;
  product_id: number;
  product_slug: string;
  shop_id: number;
  title: string;
  image: string;
  variant_label: string;
  unit_price: number;
  original_price: number | null;
  snapshot_price: number | null;
  quantity: number;
  stock: number;
}
