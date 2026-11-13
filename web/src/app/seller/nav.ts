// Thanh bên Seller Center (prototype vendor/data.jsx NAV) — 11 mục, route theo plan mục 4.3.
import type { SellerSection } from '@/features/seller-settings';
import type { IconName } from '@/shared/ui';

export interface NavItem {
  section: SellerSection;
  /** Đường dẫn trong SPA (basename /seller). */
  to: string;
  icon: IconName;
}

export const NAV: NavItem[] = [
  { section: 'overview', to: '/', icon: 'layout-dashboard' },
  { section: 'orders', to: '/orders', icon: 'package' },
  { section: 'products', to: '/products', icon: 'boxes' },
  { section: 'wallet', to: '/wallet', icon: 'wallet' },
  { section: 'kyc', to: '/kyc', icon: 'shield-check' },
  { section: 'catalog', to: '/catalog', icon: 'layers' },
  { section: 'promo', to: '/promotions', icon: 'megaphone' },
  { section: 'chat', to: '/chat', icon: 'message-circle' },
  { section: 'reviews', to: '/reviews', icon: 'star' },
  { section: 'stats', to: '/analytics', icon: 'chart-column' },
  { section: 'settings', to: '/settings', icon: 'settings' },
];

/** Đoạn đầu của đường dẫn → mục nav (null = không có màn, trả 404 nội bộ). */
const SEGMENT: Record<string, SellerSection> = {
  '': 'overview',
  orders: 'orders',
  returns: 'orders',
  products: 'products',
  wallet: 'wallet',
  kyc: 'kyc',
  catalog: 'catalog',
  promotions: 'promo',
  'flash-sales': 'promo',
  chat: 'chat',
  reviews: 'reviews',
  analytics: 'stats',
  settings: 'settings',
};

export function sectionOf(pathname: string): SellerSection | null {
  const seg = pathname.replace(/^\/+/, '').split('/')[0] ?? '';
  return SEGMENT[seg] ?? null;
}
