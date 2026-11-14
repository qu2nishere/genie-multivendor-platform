// Thông báo của người dùng (GET /me/notifications, POST /me/notifications/read-all). Mock khi USE_MOCK.
import { api } from '@/shared/api/http';
import { mockDelay, paginateCursor, USE_MOCK } from '@/shared/api/mock';
import type { CursorPage } from '@/shared/api/types';
import type { AppNotification } from '../domain/notifications.types';
import type { NotificationDto } from './notifications.dto';
import { toNotification } from './notifications.mapper';
import {
  ADMIN_NOTIFICATION_DTOS,
  NOTIFICATION_DTOS,
  SELLER_NOTIFICATION_DTOS,
} from './notifications.mock';

// Trạng thái đã đọc trong phiên mock (thay cho server)
let mockReadAll = false;
const mockRead = new Set<string>();

export async function listNotifications(
  cursor?: string | null,
  limit = 20,
): Promise<CursorPage<AppNotification>> {
  if (USE_MOCK) {
    await mockDelay();
    // Một endpoint cho mọi vai trò (3.12): mock chọn bộ thông báo theo bề mặt đang mở
    const path = typeof window !== 'undefined' ? window.location.pathname : '/';
    const source = path.startsWith('/seller')
      ? SELLER_NOTIFICATION_DTOS
      : path.startsWith('/admin')
        ? ADMIN_NOTIFICATION_DTOS
        : NOTIFICATION_DTOS;
    const rows = source.map((d) =>
      mockReadAll || mockRead.has(d.id)
        ? { ...d, read_at: d.read_at ?? new Date().toISOString() }
        : d,
    );
    const page = paginateCursor(rows, { cursor, limit });
    return { ...page, items: page.items.map(toNotification) };
  }
  const page = await api.get<CursorPage<NotificationDto>>('/me/notifications', {
    query: { cursor, limit },
  });
  return { ...page, items: page.items.map(toNotification) };
}

export async function markAllNotificationsRead(): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(120);
    mockReadAll = true;
    return;
  }
  await api.post('/me/notifications/read-all');
}

export async function markNotificationRead(id: string): Promise<void> {
  if (USE_MOCK) {
    mockRead.add(id);
    return;
  }
  await api.post(`/me/notifications/${encodeURIComponent(id)}/read`);
}
