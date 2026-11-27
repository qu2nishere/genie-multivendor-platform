// Hộp thư thông báo (API_SPEC 3.12): GET /me/notifications, PATCH /me/notifications/{id},
// POST /me/notifications/mark-all-read. Cờ `notifications` (plan 2026-11-26 thông báo D19, N05.04) — sửa 2 endpoint lệch
// spec cũ (handoff dòng 35). Mock khi chưa bật cờ.
import { api, request } from '@/shared/api/http';
import { mockDelay, paginateCursor } from '@/shared/api/mock';
import { isCursorPagination } from '@/shared/api/types';
import { isMocked } from '@/shared/config/env';
import { categoryOfType, surfaceOf } from '../domain/notifications.rules';
import type { AppNotification, NotificationType } from '../domain/notifications.types';
import type { NotificationDto } from './notifications.dto';
import { fromMockDto, toNotification } from './notifications.mapper';
import {
  ADMIN_NOTIFICATION_DTOS,
  NOTIFICATION_DTOS,
  SELLER_NOTIFICATION_DTOS,
} from './notifications.mock';

export const NOTIFICATIONS_MOCKED = isMocked('notifications');

export interface NotificationPage {
  items: AppNotification[];
  nextCursor: string | null;
  hasMore: boolean;
  /** Tổng chưa đọc của mọi nguồn (`summary.unread_count`). */
  unread: number;
}

const currentPath = () => (typeof window !== 'undefined' ? window.location.pathname : '/');

// Trạng thái đã đọc trong phiên mock (thay cho server)
let mockReadAll = false;
const mockRead = new Set<string>();

export async function listNotifications(
  tab: NotificationType | 'all' = 'all',
  cursor?: string | null,
  limit = 20,
): Promise<NotificationPage> {
  if (NOTIFICATIONS_MOCKED) {
    await mockDelay();
    // Một endpoint cho mọi vai trò (3.12): mock chọn bộ thông báo theo bề mặt đang mở
    const path = currentPath();
    const source = path.startsWith('/seller')
      ? SELLER_NOTIFICATION_DTOS
      : path.startsWith('/admin')
        ? ADMIN_NOTIFICATION_DTOS
        : NOTIFICATION_DTOS;
    const all = source
      .map((d) =>
        mockReadAll || mockRead.has(d.id)
          ? { ...d, read_at: d.read_at ?? new Date().toISOString() }
          : d,
      )
      .map(fromMockDto);
    const rows = tab === 'all' ? all : all.filter((n) => n.type === tab);
    const page = paginateCursor(rows, { cursor, limit });
    return {
      items: page.items,
      nextCursor: page.next_cursor,
      hasMore: page.has_more,
      unread: all.filter((n) => n.unread).length,
    };
  }
  const res = await request<NotificationDto[]>('/me/notifications', {
    method: 'GET',
    query: { category: categoryOfType(tab) ?? undefined, cursor: cursor ?? undefined, limit },
  });
  const surface = surfaceOf(currentPath());
  const p = isCursorPagination(res.pagination) ? res.pagination : null;
  return {
    items: (res.data ?? []).map((d) => toNotification(d, surface)),
    nextCursor: p?.next_cursor ?? null,
    hasMore: p?.has_more ?? false,
    unread: Number(res.summary?.unread_count ?? 0),
  };
}

/** `tab = 'all'` → mọi nhóm. */
export async function markAllNotificationsRead(
  tab: NotificationType | 'all' = 'all',
): Promise<void> {
  if (NOTIFICATIONS_MOCKED) {
    await mockDelay(120);
    mockReadAll = true;
    return;
  }
  await api.post('/me/notifications/mark-all-read', { category: categoryOfType(tab) });
}

export async function markNotificationRead(id: string): Promise<void> {
  if (NOTIFICATIONS_MOCKED) {
    mockRead.add(id);
    return;
  }
  await api.patch(`/me/notifications/${encodeURIComponent(id)}`, { is_read: true });
}
