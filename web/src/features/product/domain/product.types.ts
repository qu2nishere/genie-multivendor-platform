// Miền sản phẩm (plan G08.01; prototype data.jsx P/PRODUCTS/VARIANT_GROUPS, ProductDetail.jsx COLORS/SIZES).

/** Một lựa chọn phân loại: "Đen", "Bản Pro (+150.000₫)". */
export interface VariantOption {
  label: string;
  /** Màu mẫu cho nhóm màu. */
  swatch?: string;
  /** Chênh giá so với giá gốc của sản phẩm (VND). */
  priceDelta: number;
  /** Tồn kho theo lựa chọn (chỉ nhóm quyết định tồn — prototype: màu). */
  stock?: number;
}

export interface VariantGroup {
  name: string;
  options: VariantOption[];
  /** Nhóm quyết định tồn kho. */
  controlsStock: boolean;
}

export interface FlashInfo {
  sold: number;
  total: number;
}

/** Dữ liệu cho thẻ / danh sách (GET /public/products). */
export interface ProductSummary {
  id: number;
  slug: string;
  title: string;
  image: string;
  price: number;
  originalPrice: number | null;
  discountPct: number;
  rating: number;
  soldCount: number;
  shopId: number;
  shopName: string;
  shopOnVacation: boolean;
  categoryId: number;
  freeship: boolean;
  /** Sản phẩm thuộc shop Chính hãng (isMall trong DATN). */
  authentic: boolean;
  flash: FlashInfo | null;
  preorderDays: number | null;
  brand: string;
  brandVerified: boolean;
  /** Giá trị thuộc tính lọc theo ngành (Bảo hành 12 tháng…). */
  attribute: string | null;
}

export interface ProductSku {
  id: number;
  /** Chỉ số lựa chọn theo thứ tự nhóm phân loại. */
  options: number[];
  /** Giá bán của SKU (API thật — API_SPEC 5.6); mock prototype không có → tính theo priceDelta. */
  price?: number;
  /** Tồn khả dụng của SKU (stock − reserved); mock không có → theo nhóm quyết định tồn. */
  stock?: number;
}

/** Một cấp danh mục trên đường dẫn gốc → lá (API_SPEC 5.6 category_path). */
export interface CategoryCrumb {
  id: number;
  name: string;
  slug: string;
}

/** Khóa thông số thương hiệu trong dữ liệu người bán — bảng thông số hiện riêng hàng này ở đầu. */
export const BRAND_SPEC_KEY = 'Thương hiệu';

export interface ProductSpec {
  key: string;
  value: string;
}

/** Chi tiết sản phẩm (GET /public/products/{id}). */
export interface Product extends ProductSummary {
  sku: string;
  images: string[];
  reviewCount: number;
  variantGroups: VariantGroup[];
  /** SKU theo tổ hợp phân loại (product_variant_id dùng cho giỏ / đơn). */
  skus: ProductSku[];
  specs: ProductSpec[];
  /** Mô tả HTML do người bán soạn (đã làm sạch phía server). */
  descriptionHtml: string;
  /** Danh mục gốc → lá (API thật); mock không có → trang tự dựng từ categoryId. */
  categoryPath?: CategoryCrumb[];
}

export type ProductBadge = 'authentic' | 'flash' | 'preorder' | 'out_of_stock';

export type StockState = 'in_stock' | 'low' | 'out_of_stock' | 'preorder';

export type ProductSort = 'rel' | 'hot' | 'new' | 'price_asc' | 'price_desc';

/** Tham số lấy danh sách (danh mục, tìm kiếm, shop, khám phá). */
export interface ProductQuery {
  q?: string;
  categoryId?: number;
  /** Tìm kiếm: lọc nhiều danh mục gốc cùng lúc. */
  categoryIds?: number[];
  /** Danh mục con (mock lọc theo nhóm ảnh như prototype). */
  subcategoryId?: number;
  shopId?: number;
  minPrice?: number;
  maxPrice?: number;
  brands?: string[];
  /** Nơi bán (tỉnh / thành của shop). */
  locations?: string[];
  /** Chính hãng: shop Chính hãng hoặc thương hiệu đã xác minh. */
  authentic?: boolean;
  freeship?: boolean;
  /** Chỉ shop đang có voucher. */
  hasVoucher?: boolean;
  minRating?: number;
  /** Giá trị thuộc tính lọc theo ngành (Bảo hành 12 tháng…). */
  attributes?: string[];
  /** Chỉ sản phẩm đang Flash Sale. */
  flash?: boolean;
  /** Giảm giá tối thiểu (%) — sản phẩm tham gia chiến dịch. */
  minDiscount?: number;
  sort?: ProductSort;
  page?: number;
  size?: number;
}

/** Thuộc tính lọc theo ngành (`is_filterable` đầu tiên của danh mục — API 5.3 `summary.facets.attribute`). */
export interface FacetAttribute {
  /** Mã thuộc tính — gửi lọc dạng `attr.<code>=<giá trị>`. */
  code: string;
  name: string;
  values: string[];
}

/** Giá trị lọc có trong một tập kết quả (thương hiệu, nơi bán) — backend trả kèm danh sách. */
export interface ProductFacets {
  brands: string[];
  locations: string[];
  /** Chỉ có khi phạm vi có đúng một danh mục; ngược lại `null` (plan 2026-11-24 D9). */
  attribute: FacetAttribute | null;
  /** Backend đã lọc được "Shop có voucher" (`has_shop_voucher_supported`) — false thì ẩn ô lọc (plan 2026-11-24 D7). */
  voucherFilterSupported: boolean;
  total: number;
}

/** Một dòng Tài khoản › Sản phẩm yêu thích (API_SPEC 3.10 — GET /me/wishlist). */
export interface WishlistEntry {
  productId: number;
  slug: string;
  title: string;
  image: string | null;
  price: number;
  /** SKU mặc định cho "Thêm vào giỏ" — null khi chưa biết (mock) / không có SKU. */
  defaultVariantId: number | null;
  /** false khi sản phẩm không còn bán / hết hàng / shop đang nghỉ → khóa nút "Thêm vào giỏ". */
  purchasable: boolean;
}
