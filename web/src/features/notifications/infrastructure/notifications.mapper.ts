import type { AppNotification } from '../domain/notifications.types';
import type { NotificationDto } from './notifications.dto';

/** Đích liên kết kiểu ứng dụng → route thật của web (mục 4.5 plan). */
export function linkHref(link: NotificationDto['link']): string | null {
  if (!link) return null;
  switch (link.action) {
    case 'track':
      // Trang theo dõi dùng id đơn; mã TR-yyyyMMdd-NNNNNN mang số thứ tự ở 6 chữ số cuối (mock) —
      // API thật nên trả kèm order_id trong link (ghi G13.20)
      return `/orders/${Number(link.order_code.slice(-6))}`;
    case 'view':
      return `/account/${link.view}`;
    case 'home':
      return '/';
    case 'open':
      return link.path;
    default:
      return null;
  }
}

export const toNotification = (d: NotificationDto): AppNotification => ({
  id: d.id,
  type: d.type,
  icon: d.icon,
  tint: d.tint,
  bg: d.bg,
  title: d.title,
  body: d.body,
  createdAt: d.created_at,
  unread: d.read_at === null,
  href: linkHref(d.link),
  platform: d.is_platform,
});
