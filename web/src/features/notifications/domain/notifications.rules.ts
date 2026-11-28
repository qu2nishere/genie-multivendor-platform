import type {
  ApiCategory,
  AppNotification,
  NotificationSurface,
  NotificationType,
} from './notifications.types';

export const unreadCount = (list: AppNotification[]): number => list.filter((n) => n.unread).length;

/** Nhãn huy hiệu: quá 99 → "99+". */
export const badgeText = (n: number): string => (n > 99 ? '99+' : String(n));

export const markAllRead = (list: AppNotification[]): AppNotification[] =>
  list.map((n) => (n.unread ? { ...n, unread: false } : n));

export const markRead = (list: AppNotification[], id: string): AppNotification[] =>
  list.map((n) => (n.id === id ? { ...n, unread: false } : n));

export const byType = (
  list: AppNotification[],
  type: NotificationType | 'all',
): AppNotification[] => (type === 'all' ? list : list.filter((n) => n.type === type));

// ===== API thật (API_SPEC 3.12) — plan 2026-11-26 thông báo N05.03 =====

const CATEGORY_TO_TYPE: Record<ApiCategory, NotificationType> = {
  ORDER: 'order',
  PROMOTION: 'promo',
  SYSTEM: 'system',
  CHAT: 'chat',
};

/** Nhóm API → tab của giao diện. */
export const typeOfCategory = (c: ApiCategory): NotificationType => CATEGORY_TO_TYPE[c] ?? 'system';

/** Tab của giao diện → tham số `category` (`all` → không lọc). */
export const categoryOfType = (t: NotificationType | 'all'): ApiCategory | null =>
  t === 'all'
    ? null
    : ((Object.keys(CATEGORY_TO_TYPE) as ApiCategory[]).find((k) => CATEGORY_TO_TYPE[k] === t) ??
      null);

export interface Appearance {
  icon: string;
  tint: string;
  bg: string;
}

/** Icon + màu theo loại cụ thể (thông báo sàn dùng logo Genie). */
export function appearanceOf(
  apiType: string,
  category: ApiCategory,
  platform: boolean,
): Appearance {
  if (platform) return { icon: 'genie', tint: 'var(--brand-600)', bg: 'var(--brand-50)' };
  switch (apiType) {
    case 'NEW_DEVICE_LOGIN':
      return { icon: 'shield-check', tint: 'var(--red-600)', bg: 'var(--red-50)' };
    case 'KYC_APPROVED':
    case 'USER_APPROVAL':
      return { icon: 'user-check', tint: 'var(--brand-600)', bg: 'var(--brand-50)' };
    case 'KYC_REJECTED':
    case 'SYSTEM_ALERT':
    case 'ESCROW_DISCREPANCY':
      return { icon: 'triangle-alert', tint: 'var(--gold-700)', bg: 'var(--gold-50)' };
    case 'NEW_ORDER_FOR_VENDOR':
    case 'NEW_ORDER':
      return { icon: 'package', tint: 'var(--blue-600)', bg: 'var(--blue-50)' };
    case 'WALLET_CREDITED':
    case 'ESCROW_RELEASED':
    case 'WITHDRAW_REQUEST':
      return { icon: 'hand-coins', tint: 'var(--mint-600)', bg: 'var(--mint-50)' };
    case 'FLASH_SALE_REMINDER':
      return { icon: 'zap', tint: 'var(--flash-600)', bg: 'var(--flash-50)' };
    default:
      break;
  }
  switch (category) {
    case 'ORDER':
      return { icon: 'truck', tint: 'var(--mint-600)', bg: 'var(--mint-50)' };
    case 'PROMOTION':
      return { icon: 'ticket', tint: 'var(--flash-600)', bg: 'var(--flash-50)' };
    case 'CHAT':
      return { icon: 'message-circle', tint: 'var(--blue-600)', bg: 'var(--blue-50)' };
    default:
      return { icon: 'badge-check', tint: 'var(--brand-600)', bg: 'var(--brand-50)' };
  }
}

/**
 * `link` của API → route của web; `null` = không có đích (mở modal chi tiết). Đơn của shop (`NEW_ORDER_FOR_VENDOR`)
 * mở Seller Center; hội thoại mở khung chat của bề mặt đang dùng.
 */
export function hrefOf(
  link: { type: string; id: number | null } | null,
  apiType: string,
  surface: NotificationSurface,
): string | null {
  if (!link) return null;
  const id = link.id;
  switch (link.type) {
    case 'ORDER':
      if (apiType === 'NEW_ORDER_FOR_VENDOR' || surface === 'seller') return '/seller/orders';
      return id ? `/orders/${id}` : '/orders';
    case 'VOUCHER_WALLET':
      return '/account/vouchers';
    case 'SECURITY':
      return '/account/devices';
    case 'HOME':
      return '/';
    case 'PRODUCT':
      return id ? `/p/san-pham-${id}` : null;
    case 'KYC':
      return '/seller/kyc';
    case 'WITHDRAWAL':
      return '/seller/wallet';
    case 'CONVERSATION':
      return surface === 'seller' ? '/seller/chat' : '/chat';
    case 'ADMIN_KYC_QUEUE':
      return '/admin/kyc';
    case 'ADMIN_PRODUCT_QUEUE':
      return '/admin/product-moderation';
    case 'ADMIN_WITHDRAWALS':
      return '/admin/withdrawals';
    default:
      return null; // PLATFORM_NOTIFICATION → modal chi tiết
  }
}

/** Bề mặt theo đường dẫn hiện tại. */
export const surfaceOf = (pathname: string): NotificationSurface =>
  pathname.startsWith('/seller')
    ? 'seller'
    : pathname.startsWith('/admin')
      ? 'admin'
      : 'storefront';
