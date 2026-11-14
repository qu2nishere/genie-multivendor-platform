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
