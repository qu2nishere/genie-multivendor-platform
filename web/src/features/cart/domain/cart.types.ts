// Miền giỏ hàng (TS 2.4; prototype index.html seedCart / Cart.jsx). Mở rộng đầy đủ ở G10.01.

export interface CartLine {
  /** Khóa dòng = sản phẩm + phân loại (cùng SKU cộng dồn). */
  key: string;
  productId: number;
  productSlug: string;
  shopId: number;
  title: string;
  image: string;
  /** Nhãn phân loại đã chọn ("Đen / 42"). */
  variantLabel: string;
  /** Giá hiện tại (VND). */
  price: number;
  originalPrice: number | null;
  /** Giá lúc thêm vào giỏ — khác giá hiện tại thì báo đổi giá. */
  snapshotPrice: number | null;
  qty: number;
  stock: number;
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
