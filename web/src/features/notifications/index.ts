// API công khai của feature "notifications".
export type { AppNotification, NotificationType } from './domain/notifications.types';
export { unreadCount, badgeText, byType } from './domain/notifications.rules';
export {
  useNotifications,
  useNotificationFeed,
  useMarkAllRead,
  useMarkRead,
  NOTIFICATIONS_KEY,
} from './application/useNotifications';
export { SafeHtml } from './ui/SafeHtml';
export { NotifPanel, NotifIcon } from './ui/NotifPanel';
export { default as NotificationsPanel } from './ui/NotificationsPanel';
