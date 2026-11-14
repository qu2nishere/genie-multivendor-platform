// /seller/* → màn của features (plan mục 4.3, G11.01). Mỗi màn tải lười (lazy) để gói đầu nhỏ;
// đường dẫn lạ → 404 nội bộ (SellerApp).
import { lazy, type ComponentType, type LazyExoticComponent } from 'react';

export interface SellerRoute {
  path: string;
  screen: LazyExoticComponent<ComponentType>;
}

const Overview = lazy(() =>
  import('@/features/seller-dashboard').then((m) => ({ default: m.OverviewPage })),
);
const Fulfillment = lazy(() =>
  import('@/features/seller-orders').then((m) => ({ default: m.FulfillmentPage })),
);
const Returns = lazy(() =>
  import('@/features/seller-orders').then((m) => ({ default: m.ReturnsPage })),
);
const Products = lazy(() =>
  import('@/features/seller-products').then((m) => ({ default: m.ProductsPage })),
);
const Editor = lazy(() =>
  import('@/features/seller-products').then((m) => ({ default: m.ProductEditor })),
);
const Catalog = lazy(() =>
  import('@/features/seller-products').then((m) => ({ default: m.VendorCatalogPage })),
);
const Wallet = lazy(() =>
  import('@/features/seller-wallet').then((m) => ({ default: m.WalletPage })),
);
const Kyc = lazy(() =>
  import('@/features/seller-onboarding').then((m) => ({ default: m.KycPage })),
);
const Promo = lazy(() =>
  import('@/features/seller-promotions').then((m) => ({ default: m.PromoPage })),
);
const FlashSales = lazy(() =>
  import('@/features/seller-promotions').then((m) => ({ default: m.FlashSalesRoute })),
);
const Chat = lazy(() => import('@/features/chat').then((m) => ({ default: m.SellerChatPage })));
const Reviews = lazy(() =>
  import('@/features/seller-reviews').then((m) => ({ default: m.ReviewsPage })),
);
const Settings = lazy(() =>
  import('@/features/seller-settings').then((m) => ({ default: m.SettingsPage })),
);
const Stats = lazy(() =>
  import('@/features/seller-analytics').then((m) => ({ default: m.StatsPage })),
);

export const sellerRoutes: SellerRoute[] = [
  { path: '/', screen: Overview },
  { path: '/orders', screen: Fulfillment },
  { path: '/returns', screen: Returns },
  { path: '/products', screen: Products },
  { path: '/products/new', screen: Editor },
  { path: '/products/:id/edit', screen: Editor },
  { path: '/catalog', screen: Catalog },
  { path: '/wallet', screen: Wallet },
  { path: '/kyc', screen: Kyc },
  { path: '/promotions', screen: Promo },
  { path: '/flash-sales', screen: FlashSales },
  { path: '/chat', screen: Chat },
  { path: '/reviews', screen: Reviews },
  { path: '/analytics', screen: Stats },
  { path: '/settings', screen: Settings },
];
