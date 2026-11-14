// /admin/* → màn của features (plan mục 4.4, G12.01). Mỗi route gắn mục nav (mã quyền lấy từ nav)
// và tải lười.
import { lazy, type ComponentType, type LazyExoticComponent } from 'react';
import type { AdminSection } from './nav';

export interface AdminRoute {
  path: string;
  section: AdminSection;
  screen: LazyExoticComponent<ComponentType>;
}

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

const Withdrawals = lazy(() =>
  import('@/features/admin-finance').then((m) => ({ default: m.WithdrawalsPage })),
);
const Commission = lazy(() =>
  import('@/features/admin-finance').then((m) => ({ default: m.CommissionPage })),
);
const Ledger = lazy(() =>
  import('@/features/admin-finance').then((m) => ({ default: m.LedgerPage })),
);
const Cod = lazy(() =>
  import('@/features/admin-finance').then((m) => ({ default: m.CodReconciliationPage })),
);

const Vouchers = lazy(() =>
  import('@/features/admin-marketing').then((m) => ({ default: m.PlatformVouchersPage })),
);
const FlashSale = lazy(() =>
  import('@/features/admin-marketing').then((m) => ({ default: m.FlashSalePage })),
);
const Banners = lazy(() =>
  import('@/features/admin-marketing').then((m) => ({ default: m.BannersPage })),
);
const PlatformNotifs = lazy(() =>
  import('@/features/admin-marketing').then((m) => ({ default: m.PlatformNotificationsPage })),
);

const Staff = lazy(() => import('@/features/admin-system').then((m) => ({ default: m.StaffPage })));
const Users = lazy(() => import('@/features/admin-system').then((m) => ({ default: m.UsersPage })));
const Audit = lazy(() =>
  import('@/features/admin-system').then((m) => ({ default: m.AuditLogPage })),
);
const Account = lazy(() =>
  import('@/features/admin-system').then((m) => ({ default: m.AccountAdminPage })),
);

export const adminRoutes: AdminRoute[] = [
  { path: '/', section: 'overview', screen: Dashboard },
  { path: '/kyc', section: 'kyc', screen: Kyc },
  { path: '/refunds', section: 'refunds', screen: Refunds },
  { path: '/reviews', section: 'reviews', screen: Reviews },
  { path: '/withdrawals', section: 'withdrawals', screen: Withdrawals },
  { path: '/cod-reconciliation', section: 'cod', screen: Cod },
  { path: '/commission', section: 'commission', screen: Commission },
  { path: '/ledger', section: 'ledger', screen: Ledger },
  { path: '/audit-logs', section: 'audit', screen: Audit },
  { path: '/catalog', section: 'catalog', screen: Catalog },
  { path: '/catalog/:id/attributes', section: 'catalog', screen: Attributes },
  { path: '/catalog/sub/:id/products', section: 'catalog', screen: CatProducts },
  { path: '/product-moderation', section: 'product_moderation', screen: Moderation },
  { path: '/product-moderation/:id', section: 'product_moderation', screen: ModerationDetail },
  { path: '/users', section: 'users', screen: Users },
  { path: '/vouchers', section: 'vouchers', screen: Vouchers },
  { path: '/flash-sales', section: 'flash_sale', screen: FlashSale },
  { path: '/banners', section: 'banners', screen: Banners },
  { path: '/notifications', section: 'notifications', screen: PlatformNotifs },
  { path: '/staff', section: 'rbac', screen: Staff },
  { path: '/account', section: 'account', screen: Account },
];
