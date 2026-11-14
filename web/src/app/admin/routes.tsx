// /admin/* → màn của features (plan mục 4.4, G12.01). Mỗi route gắn mục nav (mã quyền lấy từ nav)
// và tải lười; màn chưa chuyển dùng tạm Pending (thay dần ở nhóm B–G).
import { lazy, type ComponentType, type LazyExoticComponent } from 'react';
import type { AdminSection } from './nav';

export interface AdminRoute {
  path: string;
  section: AdminSection;
  screen: LazyExoticComponent<ComponentType>;
}

const Pending = lazy(() => import('./Pending'));
const Dashboard = lazy(() =>
  import('@/features/admin-dashboard').then((m) => ({ default: m.DashboardPage })),
);
const Kyc = lazy(() => import('@/features/admin-kyc').then((m) => ({ default: m.KycReviewPage })));

const Refunds = lazy(() =>
  import('@/features/admin-disputes').then((m) => ({ default: m.RefundsPage })),
);
const Reviews = lazy(() =>
  import('@/features/admin-moderation').then((m) => ({ default: m.ReviewModerationPage })),
);
const Moderation = lazy(() =>
  import('@/features/admin-moderation').then((m) => ({ default: m.ProductModerationPage })),
);
const ModerationDetail = lazy(() =>
  import('@/features/admin-moderation').then((m) => ({ default: m.ProductModerationDetail })),
);

const Catalog = lazy(() =>
  import('@/features/admin-catalog').then((m) => ({ default: m.CatalogPage })),
);
const Attributes = lazy(() =>
  import('@/features/admin-catalog').then((m) => ({ default: m.AttributePanel })),
);
const CatProducts = lazy(() =>
  import('@/features/admin-catalog').then((m) => ({ default: m.CategoryProducts })),
);

export const adminRoutes: AdminRoute[] = [
  { path: '/', section: 'overview', screen: Dashboard },
  { path: '/kyc', section: 'kyc', screen: Kyc },
  { path: '/refunds', section: 'refunds', screen: Refunds },
  { path: '/reviews', section: 'reviews', screen: Reviews },
  { path: '/withdrawals', section: 'withdrawals', screen: Pending },
  { path: '/cod-reconciliation', section: 'cod', screen: Pending },
  { path: '/commission', section: 'commission', screen: Pending },
  { path: '/ledger', section: 'ledger', screen: Pending },
  { path: '/audit-logs', section: 'audit', screen: Pending },
  { path: '/catalog', section: 'catalog', screen: Catalog },
  { path: '/catalog/:id/attributes', section: 'catalog', screen: Attributes },
  { path: '/catalog/sub/:id/products', section: 'catalog', screen: CatProducts },
  { path: '/product-moderation', section: 'product_moderation', screen: Moderation },
  { path: '/product-moderation/:id', section: 'product_moderation', screen: ModerationDetail },
  { path: '/users', section: 'users', screen: Pending },
  { path: '/vouchers', section: 'vouchers', screen: Pending },
  { path: '/flash-sales', section: 'flash_sale', screen: Pending },
  { path: '/banners', section: 'banners', screen: Pending },
  { path: '/notifications', section: 'notifications', screen: Pending },
  { path: '/staff', section: 'rbac', screen: Pending },
  { path: '/account', section: 'account', screen: Pending },
];
