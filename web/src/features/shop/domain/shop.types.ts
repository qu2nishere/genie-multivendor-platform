// Miền shop (plan G08.05; prototype data.jsx V, FOLLOWS; Shop.jsx).

export interface ShopVacation {
  /** Ngày mở lại (dd/MM/yyyy như prototype; backend trả ISO). */
  until: string;
  message: string;
}

export interface Shop {
  id: number;
  slug: string;
  name: string;
  /** Shop Chính hãng (isMall trong DATN). */
  authentic: boolean;
  /** Huy hiệu "Shop Yêu Thích". */
  favorite: boolean;
  rating: number;
  followerCount: number;
  location: string;
  /** Phí ship mặc định của shop và ngưỡng shop tự miễn phí. */
  shipFee: number;
  freeShipFrom: number | null;
  logoBg: string;
  coverImage: string;
  joinedYear: number;
  responseRate: number;
  responseTime: string;
  description: string;
  vacation: ShopVacation | null;
  /** Người dùng hiện tại đang theo dõi. */
  followed: boolean;
  /** API thật: số phút phản hồi chat trung bình — trang tự dịch "trong vài phút / vài giờ" (mock dùng responseTime). */
  responseMinutes?: number | null;
  /** API thật: danh mục gốc của sản phẩm đang bán (tab danh mục — API_SPEC 5.11). */
  categories?: { id: number; name: string }[];
  /** API thật: ảnh đại diện shop. */
  logoUrl?: string | null;
}
