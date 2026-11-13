// Miền giỏ hàng (plan G10.01; prototype index.html seedCart / Cart.jsx; API_SPEC 6.1).

/** Lý do một dòng không mua được (API_SPEC 6.1 `unavailable_reason`). */
export type UnavailableReason =
  'OUT_OF_STOCK' | 'SHOP_ON_VACATION' | 'PRODUCT_UNAVAILABLE' | 'FLASH_SALE_SOLD_OUT';

/** Shop của một nhóm dòng trong giỏ. */
export interface CartShop {
  id: number;
  name: string;
  /** Shop Chính hãng (is_mall). */
  authentic: boolean;
}

export interface CartLine {
  /** Mã dòng giỏ (cart_item_id) — gửi lên API khi sửa / xóa / đặt hàng. */
  id: number;
  /** Khóa dòng = sản phẩm + phân loại (cùng SKU cộng dồn); dùng cho ?buy= và chọn dòng. */
  key: string;
  productId: number;
  /** SKU (product_variant_id). */
  variantId: number;
  productSlug: string;
  shop: CartShop;
  title: string;
  image: string;
  /** Tên danh mục gốc ("Điện tử") — voucher chỉ áp / loại trừ theo ngành. */
  category: string | null;
  /** Nhãn phân loại đã chọn ("Đen / 42"). */
  variantLabel: string;
  /** Giá hiện tại (VND). */
  price: number;
  originalPrice: number | null;
  /** Giá lúc thêm vào giỏ — khác giá hiện tại thì báo đổi giá. */
  snapshotPrice: number | null;
  qty: number;
  stock: number;
  available: boolean;
  unavailableReason: UnavailableReason | null;
}

/** Các dòng của cùng một shop — mỗi nhóm thành một đơn riêng khi đặt (tách đơn theo shop). */
export interface CartGroup {
  shop: CartShop;
  lines: CartLine[];
}

/** Giỏ khách vãng lai lưu ở trình duyệt (LUU_TRU_DU_LIEU 6.3) — chỉ id biến thể + số lượng. */
export interface GuestCartItem {
  product_variant_id: number;
  quantity: number;
  added_at: string;
}

export interface GuestCart {
  version: 1;
  expires_at: string;
  items: GuestCartItem[];
}

/** Dữ liệu nút "Thêm vào giỏ" gửi đi (trang sản phẩm dựng từ lựa chọn phân loại). */
export interface CartLineInput {
  variantId: number;
  productId: number;
  productSlug: string;
  shopId: number;
  title: string;
  image: string;
  variantLabel: string;
  price: number;
  originalPrice: number | null;
  qty: number;
  stock: number;
}

/** Kết quả đổi số lượng một dòng (prototype Cart.jsx setQty). */
export type QtyDecision =
  { kind: 'set'; qty: number } | { kind: 'confirm_remove' } | { kind: 'over_stock'; stock: number };

export type PriceChange = 'up' | 'down' | null;

/** Phân loại để đổi trong giỏ (API_SPEC 5.10). */
export interface VariantChoice {
  groups: { name: string; values: string[] }[];
  variants: { id: number; options: string[]; price: number; stock: number }[];
  image: string;
}
