// Đếm lượt xem / bấm banner (thống kê Admin → Banner) — island gọi qua tầng ứng dụng.
import { trackBanner } from '../infrastructure/banner.api';

export const trackBannerView = (id: string) => void trackBanner(id, 'view');
export const trackBannerClick = (id: string) => void trackBanner(id, 'click');
