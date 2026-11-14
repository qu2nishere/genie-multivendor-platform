import { describe, expect, it } from 'vitest';
import { badgeText, byType, markAllRead, markRead, unreadCount } from './notifications.rules';
import type { AppNotification } from './notifications.types';

const n = (
  id: string,
  unread: boolean,
  type: AppNotification['type'] = 'order',
): AppNotification => ({
  id,
  type,
  icon: 'truck',
  tint: '',
  bg: '',
  title: id,
  body: '',
  createdAt: '2026-06-22T00:00:00Z',
  unread,
  href: null,
  platform: false,
});

describe('notifications.rules', () => {
  it('đếm chưa đọc, đánh dấu đã đọc', () => {
    const list = [n('a', true), n('b', false), n('c', true, 'promo')];
    expect(unreadCount(list)).toBe(2);
    expect(unreadCount(markAllRead(list))).toBe(0);
    expect(unreadCount(markRead(list, 'a'))).toBe(1);
    expect(byType(list, 'promo').map((x) => x.id)).toEqual(['c']);
    expect(byType(list, 'all')).toHaveLength(3);
  });
  it('badgeText', () => {
    expect(badgeText(3)).toBe('3');
    expect(badgeText(120)).toBe('99+');
  });
});
