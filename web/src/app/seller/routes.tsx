// /seller/* → màn của features (plan mục 4.3, G11.01). Mỗi màn tải lười (lazy) để gói đầu nhỏ;
// màn chưa chuyển xong dùng tạm `Pending` (thay dần ở G11 nhóm B–F). Đường dẫn lạ → 404 nội bộ.
import { lazy, type ComponentType, type LazyExoticComponent } from 'react';

export interface SellerRoute {
  path: string;
  screen: LazyExoticComponent<ComponentType>;
}

const Pending = lazy(() => import('./Pending'));
const Overview = lazy(() =>
  import('@/features/seller-dashboard').then((m) => ({ default: m.OverviewPage })),
);
const Fulfillment = lazy(() =>
  import('@/features/seller-orders').then((m) => ({ default: m.FulfillmentPage })),
);
const Returns = lazy(() =>
  import('@/features/seller-orders').then((m) => ({ default: m.ReturnsPage })),
);
const Stats = lazy(() =>
  import('@/features/seller-analytics').then((m) => ({ default: m.StatsPage })),
);

export const sellerRoutes: SellerRoute[] = [
  { path: '/', screen: Overview },
  { path: '/orders', screen: Fulfillment },
  { path: '/returns', screen: Returns },
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
  { path: '/analytics', screen: Stats },
  { path: '/settings', screen: Pending },
];
