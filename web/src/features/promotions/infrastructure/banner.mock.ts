// Banner mẫu (prototype data.jsx BANNER_SEED). Liên kết dạng ?screen= của prototype đã đổi sang
// route mới (plan mục 4.5): discover → /discover, category&cat= → /c/{slug}, shop&shop= → /shop/{id},
// product&product=p1 → /p/{slug}-1, đường dẫn trần (/sieu-sale-12-12) → /campaign/{slug}.
import { bannerImg } from '@/shared/lib/assets';
import type { BannerDto } from './banner.dto';

const B = (
  id: string,
  title: string,
  image: string,
  link: string,
  position: BannerDto['position'],
  order: number,
  extra: Partial<BannerDto> = {},
): BannerDto => ({
  id,
  title,
  image_url: image.startsWith('/') ? image : bannerImg(image),
  image_mobile_url: null,
  link,
  position,
  category_slug: null,
  sort_order: order,
  starts_at: '2026-01-01',
  ends_at: '2030-12-31',
  is_active: true,
  ...extra,
});

export const BANNER_DTOS: BannerDto[] = [
  B(
    'BN-101',
    'Nhận hàng rồi, tiền mới đến shop',
    'home-trust',
    '/discover',
    'HOME_TOP_CAROUSEL',
    1,
  ),
  B('BN-105', 'Công nghệ chính hãng', 'home-tech', '/c/dien-tu', 'HOME_TOP_CAROUSEL', 2),
  B('BN-106', 'Làm đẹp mỗi ngày', 'home-beauty', '/c/lam-dep', 'HOME_TOP_CAROUSEL', 3),
  B('BN-107', 'Thời trang mới về', 'home-fashion', '/c/thoi-trang', 'HOME_TOP_CAROUSEL', 4),
  B('BN-115', 'Siêu sale 12.12', 'camp-1212', '/campaign/sieu-sale-12-12', 'HOME_TOP_CAROUSEL', 5, {
    starts_at: '2026-11-01',
    ends_at: '2026-12-12',
  }),
  B('BN-108', 'Freeship đơn từ 99.000₫', 'side-freeship', '/#flash-sale', 'HOME_TOP_SIDE', 1),
  B('BN-109', 'Mở shop 0₫ trên Genie', 'side-seller', '/become-seller', 'HOME_TOP_SIDE', 2),
  B('BN-102', 'Nhà cửa và đời sống', 'mid-home', '/c/nha-cua', 'HOME_MIDDLE', 1),
  B('BN-110', 'Phụ kiện công nghệ', 'mid-acc', '/c/phu-kien', 'HOME_MIDDLE', 2),
  B('BN-103', 'Shop nổi bật: Beauty Box', 'cat-beauty', '/shop/5', 'CATEGORY_TOP', 1, {
    category_slug: 'lam-dep',
  }),
  B('BN-111', 'Shop nổi bật: TechZone Official', 'cat-tech', '/shop/1', 'CATEGORY_TOP', 1, {
    category_slug: 'dien-tu',
  }),
  B('BN-112', 'Shop nổi bật: Boutique Sài Gòn', 'cat-fashion', '/shop/3', 'CATEGORY_TOP', 1, {
    category_slug: 'thoi-trang',
  }),
  B('BN-113', 'Shop nổi bật: Home & Living', 'cat-home', '/shop/4', 'CATEGORY_TOP', 1, {
    category_slug: 'nha-cua',
  }),
  B('BN-114', 'Shop nổi bật: TravelGear', 'cat-acc', '/shop/6', 'CATEGORY_TOP', 1, {
    category_slug: 'phu-kien',
  }),
  B(
    'BN-116',
    'Tai nghe ANC Pro 5 chỉ 389.000₫',
    'flash-anc',
    '/p/tai-nghe-bluetooth-chong-on-anc-pro-5-pin-40-gio-1',
    'FLASH_SALE_TOP',
    1,
  ),
  B('BN-104', 'Siêu sale 12.12', 'camp-1212', '/campaign/sieu-sale-12-12', 'POPUP', 1, {
    starts_at: '2026-11-01',
    ends_at: '2026-12-12',
  }),
  B('BN-118', 'Freeship đơn từ 99.000₫', 'side-freeship', '/#flash-sale', 'POPUP', 2, {
    starts_at: '2026-11-01',
  }),
  B('BN-119', 'Mở shop 0₫ trên Genie', 'side-seller', '/become-seller', 'POPUP', 3, {
    starts_at: '2026-11-01',
  }),
  B('BN-117', 'Tết 2026 giảm sâu', '/img/products/banner-sale.jpg', '/discover', 'POPUP', 4, {
    ends_at: '2026-02-15',
  }),
];
