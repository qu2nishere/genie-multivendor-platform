// DTO hộp thư (API_SPEC 3.12, 13) — snake_case. Plan 2026-11-26 thông báo N05.03.

/** Phần tử thật của `GET /me/notifications` (API 3.12). */
export interface NotificationDto {
  /** Tiền tố nguồn: `ntf_` (cá nhân), `pn_` (thông báo sàn), `an_` (nhân viên). */
  id: string;
  category: 'ORDER' | 'PROMOTION' | 'SYSTEM' | 'CHAT';
  /** `NotificationType` / `PLATFORM_ANNOUNCEMENT` / `AdminNotificationType`. */
  type: string;
  title: string;
  body: string | null;
  image_url: string | null;
  /** Chỉ có với `pn_` — HTML đã lọc ở server. */
  content_html?: string | null;
  created_at: string;
  is_read: boolean;
  link: { type: string; id: number | null } | null;
}

/** Gói tin `/user/queue/notifications` và `/topic/platform` (API 13). */
export interface NotificationEventDto {
  type: 'NEW_NOTIFICATION' | 'PLATFORM_NOTIFICATION' | 'NOTIFICATIONS_READ';
  notification?: NotificationDto;
  unread_count?: number;
  ids?: string[] | null;
  category?: string | null;
}

/** Dữ liệu mẫu của lớp mock (định dạng prototype, giữ để chạy `PUBLIC_USE_MOCK`). */
export interface MockNotificationDto {
  id: string;
  type: 'order' | 'promo' | 'system' | 'chat';
  icon: string;
  tint: string;
  bg: string;
  title: string;
  body: string;
  created_at: string;
  read_at: string | null;
  /** Đích kiểu ứng dụng: { action: 'track', order_code } | { action: 'view', view } | { action: 'home' } | { action: 'open', path } (route Seller Center). */
  link:
    | { action: 'track'; order_code: string }
    | { action: 'view'; view: string }
    | { action: 'home' }
    | { action: 'open'; path: string }
    | null;
  is_platform: boolean;
}
