// Mapper hộp thư API 3.12 → miền (plan 2026-11-26 thông báo N05.08)
import { describe, expect, it } from 'vitest';
import { isNewNotification } from '../application/notificationStream';
import type { NotificationDto } from './notifications.dto';
import { toNotification } from './notifications.mapper';

const dto = (over: Partial<NotificationDto>): NotificationDto => ({
  id: 'ntf_1',
  category: 'ORDER',
  type: 'ORDER_STATUS_CHANGED',
  title: 'Đơn đang giao',
  body: 'GHN đang giao',
  image_url: null,
  created_at: '2026-06-22T08:00:00+07:00',
  is_read: false,
  link: { type: 'ORDER', id: 44 },
  ...over,
});

describe('notifications.mapper', () => {
  it('thông báo cá nhân', () => {
    const n = toNotification(dto({}), 'storefront');
    expect(n).toMatchObject({
      id: 'ntf_1',
      type: 'order',
      unread: true,
      href: '/orders/44',
      platform: false,
      icon: 'truck',
    });
  });

  it('thông báo sàn: logo Genie, mở modal, giữ HTML + ảnh', () => {
    const n = toNotification(
      dto({
        id: 'pn_1047',
        category: 'SYSTEM',
        type: 'PLATFORM_ANNOUNCEMENT',
        image_url: 'https://s3.genie.vn/content-assets/notice/1047/a.webp',
        content_html: '<p>Bảo trì</p>',
        is_read: true,
        link: { type: 'PLATFORM_NOTIFICATION', id: 1047 },
      }),
      'storefront',
    );
    expect(n).toMatchObject({
      platform: true,
      icon: 'genie',
      href: null,
      unread: false,
      type: 'system',
    });
    expect(n.contentHtml).toBe('<p>Bảo trì</p>');
    expect(n.imageUrl).toContain('notice/1047');
  });

  it('thông báo nhân viên mở hàng chờ trong Admin', () => {
    const n = toNotification(
      dto({
        id: 'an_5',
        category: 'SYSTEM',
        type: 'USER_APPROVAL',
        link: { type: 'ADMIN_KYC_QUEUE', id: null },
      }),
      'admin',
    );
    expect(n.href).toBe('/admin/kyc');
    expect(n.body).toBe('GHN đang giao');
  });

  it('body null → chuỗi rỗng', () => {
    expect(toNotification(dto({ body: null }), 'storefront').body).toBe('');
  });

  it('nhận diện gói tin realtime có thông báo mới', () => {
    expect(
      isNewNotification({ type: 'NEW_NOTIFICATION', notification: dto({}), unread_count: 1 }),
    ).toBe(true);
    expect(
      isNewNotification({ type: 'PLATFORM_NOTIFICATION', notification: dto({ id: 'pn_1' }) }),
    ).toBe(true);
    expect(isNewNotification({ type: 'NOTIFICATIONS_READ', ids: ['ntf_1'], unread_count: 0 })).toBe(
      false,
    );
    expect(isNewNotification(null)).toBe(false);
  });
});
