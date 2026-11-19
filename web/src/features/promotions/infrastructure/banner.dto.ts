// GET /public/banners?position=&category= — snake_case.
export interface BannerDto {
  id: string;
  title: string;
  image_url: string;
  image_mobile_url: string | null;
  link: string;
  position:
    | 'HOME_TOP_CAROUSEL'
    | 'HOME_TOP_SIDE'
    | 'HOME_MIDDLE'
    | 'CATEGORY_TOP'
    | 'FLASH_SALE_TOP'
    | 'POPUP';
  category_slug: string | null;
  sort_order: number;
  starts_at: string;
  ends_at: string;
  is_active: boolean;
}

/** Một banner đang hiển thị — `GET /public/banners?positions=…&category_id=…` trả map vị trí → mảng (API_SPEC 5.14). */
export interface LiveBannerDto {
  id: number;
  title: string;
  image_url: string;
  mobile_image_url: string | null;
  target_url: string;
}
