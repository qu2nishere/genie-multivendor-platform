// Banner do Admin quản lý (prototype data.jsx BANNER_SEED, banners.live) — plan G08.07.
export type BannerPosition =
  | 'HOME_TOP_CAROUSEL'
  | 'HOME_TOP_SIDE'
  | 'HOME_MIDDLE'
  | 'CATEGORY_TOP'
  | 'FLASH_SALE_TOP'
  | 'POPUP';

export type BannerState = 'OFF' | 'SCHEDULED' | 'LIVE' | 'ENDED';

export interface Banner {
  id: string;
  title: string;
  image: string;
  /** Ảnh riêng cho màn ≤768px (Admin tải lên), rỗng thì dùng ảnh chính. */
  imageMobile: string | null;
  /** Đích khi bấm — route của web (/c/…, /p/…, /campaign/…). */
  href: string;
  position: BannerPosition;
  /** Banner đầu trang danh mục: slug danh mục áp dụng. */
  categorySlug: string | null;
  order: number;
  /** Ngày bắt đầu / kết thúc dạng YYYY-MM-DD (so sánh theo ngày địa phương như prototype). */
  startsAt: string;
  endsAt: string;
  on: boolean;
}
