// API công khai của feature "admin-marketing" — voucher sàn, Flash Sale, banner trang chủ, thông báo sàn.
export type {
  PlatformVoucher,
  FlashCampaign,
  AdminBanner,
  PlatformNotification,
} from './domain/marketing.types';
export { default as PlatformVouchersPage } from './ui/PlatformVouchersPage';
export { default as FlashSalePage } from './ui/FlashSalePage';
export { default as BannersPage } from './ui/BannersPage';
export { default as PlatformNotificationsPage } from './ui/PlatformNotificationsPage';
