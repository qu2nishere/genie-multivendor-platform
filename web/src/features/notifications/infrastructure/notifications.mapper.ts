import type { AppNotification } from '../domain/notifications.types';
import type { NotificationDto } from './notifications.dto';

/** Đích liên kết kiểu ứng dụng → route thật của web (mục 4.5 plan). */
export function linkHref(link: NotificationDto['link']): string | null {
  if (!link) return null;
  switch (link.action) {
    case 'track':
      return `/orders/${encodeURIComponent(link.order_code)}`;
    case 'view':
      return `/account/${link.view}`;
    case 'home':
      return '/';
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
