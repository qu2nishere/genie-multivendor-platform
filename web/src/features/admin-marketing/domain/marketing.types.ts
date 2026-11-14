// Marketing sàn (API_SPEC 11.14–11.17; prototype admin/Vouchers.jsx, FlashSale.jsx, Banners.jsx,
// PlatformNotifications.jsx).

// ---------- 11.14 Voucher sàn ----------
export type ManagedVoucherStatus = 'ACTIVE' | 'SCHEDULED' | 'ENDED' | 'DISABLED';
export type PlatformDiscountType = 'PERCENTAGE' | 'FIXED_AMOUNT' | 'FREESHIP';

export interface PlatformVoucher {
  id: number;
  title: string;
  code: string;
  discountType: PlatformDiscountType;
  discountValue: number;
  minOrderValue: number;
  maxDiscount: number | null;
  usage: { used: number; limit: number };
  usagePerUser: number;
  validFrom: string;
  validTo: string;
  fundedBy: 'PLATFORM' | 'CO_SPONSOR';
  status: ManagedVoucherStatus;
}

export interface PlatformVoucherSummary {
  activeCount: number;
  totalUsage: number;
  totalDiscountUsed: number;
  scheduledCount: number;
  remainingBudget: number;
}

/** `$PlatformVoucherInput` — ngày dạng "YYYY-MM-DD" ở form, đổi sang ISO khi gửi. */
export interface PlatformVoucherInput {
  title: string;
  code: string;
  discountType: PlatformDiscountType;
  discountValue: number | null;
  maxDiscount: number | null;
  minOrderValue: number | null;
  usageLimit: number | null;
  usagePerUser: number | null;
  budget: number | null;
  validFrom: string;
  validTo: string;
  scope: 'ALL' | 'CATEGORY';
  categoryIds: number[];
  excludedCategoryIds: number[];
  fundedBy: 'PLATFORM' | 'CO_SPONSOR';
  platformSharePercent: number | null;
}

// ---------- 11.15 Flash Sale ----------
export type FlashStatus = 'DRAFT' | 'SCHEDULED' | 'ACTIVE' | 'ENDED';
export type FlashItemStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface FlashCampaign {
  id: number;
  name: string;
  startsAt: string;
  endsAt: string;
  status: FlashStatus;
  itemCount: number;
}

export interface FlashItem {
  id: number;
  productId: number;
  productName: string;
  shopName: string;
  referencePrice: number;
  flashPrice: number;
  flashStock: number;
  status: FlashItemStatus;
  meetsDiscountRule: boolean;
}

export interface FlashCampaignDetail extends FlashCampaign {
  maxItemsPerShop: number;
  registrationDeadline: string;
  items: FlashItem[];
}

export interface FlashCampaignInput {
  name: string;
  startsAt: string;
  endsAt: string;
  maxItemsPerShop: number;
  registrationDeadline: string;
}

// ---------- 11.16 Banner ----------
export type BannerPosition =
  | 'HOME_TOP_CAROUSEL'
  | 'HOME_TOP_SIDE'
  | 'HOME_MIDDLE'
  | 'CATEGORY_TOP'
  | 'FLASH_SALE_TOP'
  | 'POPUP';
export type BannerState = 'LIVE' | 'SCHEDULED' | 'ENDED' | 'OFF';

export interface AdminBanner {
  id: string;
  title: string;
  imageUrl: string;
  mobileImageUrl: string | null;
  targetUrl: string;
  position: BannerPosition;
  category: { id: number; name: string; slug: string } | null;
  sortOrder: number;
  startsAt: string;
  endsAt: string;
  active: boolean;
  state: BannerState;
  impressions: number;
  clicks: number;
}

export interface BannerInput {
  title: string;
  /** Ảnh mới đã tải (object key); null khi sửa mà không đổi ảnh. */
  imageObjectKey: string | null;
  mobileImageObjectKey: string | null;
  /** Ảnh xem trước (mock dùng làm ảnh hiển thị). */
  imagePreview: string | null;
  mobilePreview: string | null;
  targetUrl: string;
  position: BannerPosition;
  categoryId: number | null;
  sortOrder: number;
  startsAt: string;
  endsAt: string;
  active: boolean;
}

// ---------- 11.17 Thông báo sàn ----------
export type NotificationTarget = 'ALL' | 'CUSTOMER' | 'VENDOR' | 'SPECIFIC_USERS';

export interface PlatformNotification {
  id: number;
  code: string;
  title: string;
  contentPreview: string;
  imageUrl: string | null;
  target: NotificationTarget;
  createdBy: string;
  sendAt: string;
  scheduled: boolean;
  readCount: number;
  reachCount: number;
}

export interface PlatformNotificationDetail extends PlatformNotification {
  contentHtml: string;
  recipientEmails: string[];
}

export interface NotificationSummary {
  sentThisMonth: number;
  totalReach: number;
  totalReads: number;
}

export interface NotificationInput {
  title: string;
  contentHtml: string;
  imageObjectKey: string | null;
  imagePreview: string | null;
  target: NotificationTarget;
  recipientEmails: string[];
  /** null = phát ngay. */
  sendAt: string | null;
}
