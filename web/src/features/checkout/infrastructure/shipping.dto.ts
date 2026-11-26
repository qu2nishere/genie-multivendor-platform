// Chính sách phí ship của shop. Thật: lấy từ POST /me/checkout/preview (6.8 — base_shipping_fee, free_ship_threshold,
// is_estimated_fee — plan C2 O06.06); mock đọc ship_fee / free_ship_from của shop mẫu.
export interface ShipPolicyDto {
  shop_id: number;
  shipping_fee: number;
  free_ship_from: number | null;
}

/** Khối shop của phản hồi 6.8 (chỉ các trường trang Giỏ / Thanh toán cần cho phí ship). */
export interface PreviewShopDto {
  shop: { id: number; name: string };
  subtotal: number;
  shipping_fee: number | null;
  base_shipping_fee: number | null;
  free_ship_threshold: number | null;
  is_free_shipping_by_shop: boolean;
  is_estimated_fee: boolean;
}

export interface PreviewDto {
  shops: PreviewShopDto[];
}
