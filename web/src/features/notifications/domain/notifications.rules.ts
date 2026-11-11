import type { AppNotification, NotificationType } from './notifications.types';

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
