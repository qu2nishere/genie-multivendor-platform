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
}
