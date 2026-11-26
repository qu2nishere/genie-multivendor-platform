// GET /me/cart (API_SPEC 6.1) — snake_case.

export interface CartShopDto {
  id: number;
  name: string;
  logo_url: string | null;
  is_mall: boolean;
}

export interface CartItemDto {
  id: number;
  product_id: number;
  variant_id: number;
  /**
   * Hai trường dưới chưa có trong API_SPEC 6.1 — client cần để dựng liên kết /p/{slug}-{id} (R16)
   * và tính voucher theo ngành khi chạy mock; ghi vào danh sách nối backend (G13.20).
   */
  product_slug: string;
  category_name: string | null;
  product_name: string;
  image_url: string | null;
  variant_name: string | null;
  unit_price: number;
  original_price: number | null;
  price_at_add: number | null;
  quantity: number;
  stock: number;
  is_available: boolean;
  unavailable_reason:
    'OUT_OF_STOCK' | 'SHOP_ON_VACATION' | 'PRODUCT_UNAVAILABLE' | 'FLASH_SALE_SOLD_OUT' | null;
}

export interface CartDto {
  shops: { shop: CartShopDto; items: CartItemDto[] }[];
  item_count: number;
}

/** POST /me/cart/merge (API_SPEC 6.7 — plan C2 D8). */
export interface MergeResultDto {
  merged: number;
  skipped: { variant_id: number; reason: string }[];
  adjusted: { variant_id: number; quantity: number }[];
  cart_item_count: number;
}

/** GET /public/variants?ids= (API_SPEC 5.18) — dòng hiển thị của giỏ khách. */
export interface PublicVariantDto {
  variant_id: number;
  product_id: number;
  product_slug: string;
  product_name: string;
  category_name: string | null;
  variant_name: string | null;
  image_url: string | null;
  unit_price: number;
  original_price: number | null;
  stock: number;
  is_available: boolean;
  unavailable_reason: CartItemDto['unavailable_reason'];
  shop: CartShopDto | null;
}

/** POST /me/cart/items/batch (API_SPEC 6.6). */
export interface BatchAddResultDto {
  added: number;
  skipped: { variant_id: number; reason: string }[];
  cart_item_count: number;
}

/** GET /public/products/{id}/variants (API_SPEC 5.10) — modal "Chọn phân loại" ở giỏ. */
export interface VariantChoiceDto {
  option_groups: { name: string; values: string[] }[];
  variants: {
    id: number;
    options: string[];
    price: number;
    stock: number;
    image_url: string | null;
  }[];
  image_url: string;
}
