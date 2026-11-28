import { appearanceOf, hrefOf, typeOfCategory } from '../domain/notifications.rules';
import type { AppNotification, NotificationSurface } from '../domain/notifications.types';
import type { MockNotificationDto, NotificationDto } from './notifications.dto';

/** Đích liên kết kiểu ứng dụng của dữ liệu mock → route thật của web (mục 4.5 plan G). */
export function linkHref(link: MockNotificationDto['link']): string | null {
  if (!link) return null;
  switch (link.action) {
    case 'track':
      // Trang theo dõi dùng id đơn; mã TR-yyyyMMdd-NNNNNN mang số thứ tự ở 6 chữ số cuối (mock)
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

/** Dữ liệu mock (định dạng prototype) → miền. */
export const fromMockDto = (d: MockNotificationDto): AppNotification => ({
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
  imageUrl: null,
  contentHtml: null,
});

/** Phần tử API 3.12 → miền (plan 2026-11-26 thông báo N05.03). */
export function toNotification(d: NotificationDto, surface: NotificationSurface): AppNotification {
  const platform = d.id.startsWith('pn_');
  const look = appearanceOf(d.type, d.category, platform);
  return {
    id: d.id,
    type: typeOfCategory(d.category),
    icon: look.icon,
    tint: look.tint,
    bg: look.bg,
    title: d.title,
    body: d.body ?? '',
    createdAt: d.created_at,
    unread: !d.is_read,
    href: hrefOf(d.link, d.type, surface),
    platform,
    imageUrl: d.image_url,
    contentHtml: d.content_html ?? null,
  };
}
