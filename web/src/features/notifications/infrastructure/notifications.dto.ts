// GET /me/notifications (cursor) — snake_case.
export interface NotificationDto {
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
