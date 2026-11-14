import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { unreadCount } from '../domain/notifications.rules';
import {
  listNotifications,
  markAllNotificationsRead,
  markNotificationRead,
} from '../infrastructure/notifications.api';

export const NOTIFICATIONS_KEY = ['me', 'notifications'] as const;

/** Thông báo trang đầu + số chưa đọc (chuông header, NotificationsPanel). */
export function useNotifications(enabled = true) {
  const q = useQuery({
    queryKey: NOTIFICATIONS_KEY,
    queryFn: () => listNotifications(null, 20),
    enabled,
  });
  const items = q.data?.items ?? [];
  return { ...q, items, unread: unreadCount(items) };
}

export function useMarkAllRead() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: markAllNotificationsRead,
    onSuccess: () => qc.invalidateQueries({ queryKey: NOTIFICATIONS_KEY }),
  });
}

export function useMarkRead() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: markNotificationRead,
    onSuccess: () => qc.invalidateQueries({ queryKey: NOTIFICATIONS_KEY }),
  });
}
