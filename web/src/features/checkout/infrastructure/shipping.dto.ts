// Chính sách phí ship của shop. Thật: phí nằm trong phản hồi POST /me/checkout/preview (6.8,
// shipping_fee / is_free_shipping_by_shop); mock đọc ship_fee / free_ship_from của shop mẫu.
export interface ShipPolicyDto {
  shop_id: number;
  shipping_fee: number;
  free_ship_from: number | null;
}
