import { describe, expect, it } from 'vitest';
import {
  appearanceOf,
  badgeText,
  byType,
  categoryOfType,
  hrefOf,
  markAllRead,
  markRead,
  surfaceOf,
  typeOfCategory,
  unreadCount,
} from './notifications.rules';
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

// ===== API thật (plan 2026-11-26 thông báo N05.08) =====

describe('notifications.rules — API 3.12', () => {
  it('đổi nhóm API ⇄ tab', () => {
    expect(typeOfCategory('ORDER')).toBe('order');
    expect(typeOfCategory('PROMOTION')).toBe('promo');
    expect(typeOfCategory('SYSTEM')).toBe('system');
    expect(typeOfCategory('CHAT')).toBe('chat');
    expect(categoryOfType('promo')).toBe('PROMOTION');
    expect(categoryOfType('all')).toBeNull();
  });

  it('route theo link và bề mặt', () => {
    expect(hrefOf({ type: 'ORDER', id: 44 }, 'ORDER_STATUS_CHANGED', 'storefront')).toBe(
      '/orders/44',
    );
    expect(hrefOf({ type: 'ORDER', id: 44 }, 'NEW_ORDER_FOR_VENDOR', 'storefront')).toBe(
      '/seller/orders',
    );
    expect(hrefOf({ type: 'CONVERSATION', id: 7 }, 'CHAT_MESSAGE', 'seller')).toBe('/seller/chat');
    expect(hrefOf({ type: 'CONVERSATION', id: 7 }, 'CHAT_MESSAGE', 'storefront')).toBe('/chat');
    expect(hrefOf({ type: 'KYC', id: null }, 'KYC_APPROVED', 'seller')).toBe('/seller/kyc');
    expect(hrefOf({ type: 'SECURITY', id: null }, 'NEW_DEVICE_LOGIN', 'storefront')).toBe(
      '/account/devices',
    );
    expect(hrefOf({ type: 'ADMIN_KYC_QUEUE', id: null }, 'USER_APPROVAL', 'admin')).toBe(
      '/admin/kyc',
    );
    expect(
      hrefOf({ type: 'PLATFORM_NOTIFICATION', id: 1 }, 'PLATFORM_ANNOUNCEMENT', 'storefront'),
    ).toBeNull();
    expect(hrefOf(null, 'SYSTEM_ALERT', 'admin')).toBeNull();
  });

  it('bề mặt theo đường dẫn', () => {
    expect(surfaceOf('/seller/orders')).toBe('seller');
    expect(surfaceOf('/admin')).toBe('admin');
    expect(surfaceOf('/account/notifications')).toBe('storefront');
  });

  it('thông báo sàn dùng logo Genie, loại riêng có icon riêng', () => {
    expect(appearanceOf('PLATFORM_ANNOUNCEMENT', 'SYSTEM', true).icon).toBe('genie');
    expect(appearanceOf('NEW_DEVICE_LOGIN', 'SYSTEM', false).icon).toBe('shield-check');
    expect(appearanceOf('CHAT_MESSAGE', 'CHAT', false).icon).toBe('message-circle');
    expect(appearanceOf('ORDER_STATUS_CHANGED', 'ORDER', false).icon).toBe('truck');
  });
});
