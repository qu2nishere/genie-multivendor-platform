// /seller/* → màn của features (plan mục 4.3, G11.01). Mỗi màn tải lười (lazy) để gói đầu nhỏ;
// màn chưa chuyển xong dùng tạm `Pending` (thay dần ở G11 nhóm B–F). Đường dẫn lạ → 404 nội bộ.
import { lazy, type ComponentType, type LazyExoticComponent } from 'react';

export interface SellerRoute {
  path: string;
  screen: LazyExoticComponent<ComponentType>;
}

const Pending = lazy(() => import('./Pending'));

export const sellerRoutes: SellerRoute[] = [
  { path: '/', screen: Pending },
  { path: '/orders', screen: Pending },
  { path: '/returns', screen: Pending },
  { path: '/products', screen: Pending },
  { path: '/products/new', screen: Pending },
  { path: '/products/:id/edit', screen: Pending },
  { path: '/catalog', screen: Pending },
  { path: '/wallet', screen: Pending },
  { path: '/kyc', screen: Pending },
  { path: '/promotions', screen: Pending },
  { path: '/flash-sales', screen: Pending },
  { path: '/chat', screen: Pending },
  { path: '/reviews', screen: Pending },
  { path: '/analytics', screen: Pending },
  { path: '/settings', screen: Pending },
];
