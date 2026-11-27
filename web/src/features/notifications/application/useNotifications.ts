// Hộp thư (TanStack Query) — chuông header, trang Tài khoản › Thông báo (plan 2026-11-26 thông báo N05.05).
import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import type { NotificationType } from '../domain/notifications.types';
import {
  listNotifications,
  markAllNotificationsRead,
  markNotificationRead,
} from '../infrastructure/notifications.api';
import { broadcastRead, listenNotifications } from './notificationStream';

export const NOTIFICATIONS_KEY = ['me', 'notifications'] as const;
const BELL_KEY = [...NOTIFICATIONS_KEY, 'bell'] as const;
const feedKey = (tab: NotificationType | 'all') => [...NOTIFICATIONS_KEY, 'feed', tab] as const;

/** Làm mới cache hộp thư khi có thông báo mới / đã đọc ở nơi khác (realtime, tab khác). */
function useLiveRefresh(enabled: boolean) {
  const qc = useQueryClient();
  useEffect(() => {
    if (!enabled) return undefined;
    return listenNotifications(() => {
      void qc.invalidateQueries({ queryKey: NOTIFICATIONS_KEY });
    });
  }, [enabled, qc]);
}

/** Thông báo trang đầu + số chưa đọc (chuông header Storefront / Seller / Admin, menu tài khoản). */
export function useNotifications(enabled = true) {
  useLiveRefresh(enabled);
  const q = useQuery({
    queryKey: BELL_KEY,
    queryFn: () => listNotifications('all', null, 20),
    enabled,
  });
  return { ...q, items: q.data?.items ?? [], unread: q.data?.unread ?? 0 };
}

/** Danh sách theo tab, "Xem thêm" theo cursor (trang Tài khoản › Thông báo). */
export function useNotificationFeed(tab: NotificationType | 'all', pageSize = 10) {
  useLiveRefresh(true);
  const q = useInfiniteQuery({
    queryKey: feedKey(tab),
    queryFn: ({ pageParam }) => listNotifications(tab, pageParam, pageSize),
    initialPageParam: null as string | null,
    getNextPageParam: (last) => (last.hasMore ? last.nextCursor : undefined),
  });
  const pages = q.data?.pages ?? [];
  return {
    ...q,
    items: pages.flatMap((p) => p.items),
    unread: pages[0]?.unread ?? 0,
  };
}

export function useMarkAllRead() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (tab: NotificationType | 'all') => markAllNotificationsRead(tab),
    onSuccess: () => {
      broadcastRead('READ_ALL');
      return qc.invalidateQueries({ queryKey: NOTIFICATIONS_KEY });
    },
  });
}

export function useMarkRead() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: markNotificationRead,
    onSuccess: () => {
      broadcastRead('READ_ONE');
      return qc.invalidateQueries({ queryKey: NOTIFICATIONS_KEY });
    },
  });
}
