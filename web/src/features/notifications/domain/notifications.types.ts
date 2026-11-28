// Miền thông báo (prototype data.jsx NOTIFICATIONS; Notification Center).
export type NotificationType = 'order' | 'promo' | 'system' | 'chat';

export interface AppNotification {
  id: string;
  type: NotificationType;
  /** Tên icon; "genie" = logo sàn (thông báo toàn sàn). */
  icon: string;
  /** Cặp màu ô icon (token CSS). */
  tint: string;
  bg: string;
  title: string;
  body: string;
  createdAt: string;
  unread: boolean;
  /** Đích khi bấm (đơn, kho voucher…); null = chỉ đọc. */
  href: string | null;
  /** Thông báo toàn sàn do Admin phát. */
  platform: boolean;
  /** Ảnh banner / minh họa (API thật). */
  imageUrl?: string | null;
  /** HTML đã lọc của thông báo sàn — modal chi tiết (API thật). */
  contentHtml?: string | null;
}

/** Nhóm hiển thị phía API (API_SPEC 3.12 `category`). */
export type ApiCategory = 'ORDER' | 'PROMOTION' | 'SYSTEM' | 'CHAT';

/** Bề mặt đang mở — cùng một đích đổi route theo bề mặt (vd hội thoại: `/chat` hay `/seller/chat`). */
export type NotificationSurface = 'storefront' | 'seller' | 'admin';
