// API công khai của feature "notifications".
export type { AppNotification, NotificationType } from './domain/notifications.types';
export { unreadCount, badgeText, byType } from './domain/notifications.rules';
export {
  useNotifications,
  useMarkAllRead,
  useMarkRead,
  NOTIFICATIONS_KEY,
} from './application/useNotifications';
export { NotifPanel, NotifIcon } from './ui/NotifPanel';
