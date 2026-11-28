import { describe, expect, it } from 'vitest';
import {
  flattenPages,
  insertMessage,
  markSeen,
  mergeMessages,
  presenceOf,
  previewKey,
  sortConversations,
  statusKey,
  totalUnread,
  typingActive,
} from './chat.rules';
import type { ChatMessage, Conversation } from './chat.types';

const m = (over: Partial<ChatMessage>): ChatMessage => ({
  id: 1,
  mine: true,
  type: 'TEXT',
  text: 'a',
  product: null,
  order: null,
  imageUrl: null,
  sentAt: '2026-06-08T14:00:00Z',
  status: 'SENT',
  clientId: null,
  auto: false,
  ...over,
});
const c = (id: number, at: string | null, unread = 0): Conversation => ({
  id,
  shop: { id, name: `S${id}`, authentic: false, online: true, lastActiveAt: null },
  lastMessage: at ? { type: 'TEXT', preview: 'x', sentAt: at } : null,
  unread,
});

describe('chat.rules', () => {
  it('dòng xem trước của tin không phải chữ', () => {
    expect(previewKey('IMAGE')).toBe('chat.preview.image');
    expect(previewKey('ORDER_CARD')).toBe('chat.preview.order');
    expect(previewKey('TEXT')).toBeNull();
  });

  it('tổng chưa đọc, sắp xếp theo tin cuối', () => {
    const list = [c(1, '2026-06-01T00:00:00Z', 2), c(2, '2026-06-08T00:00:00Z', 1), c(3, null)];
    expect(totalUnread(list)).toBe(3);
    expect(sortConversations(list).map((x) => x.id)).toEqual([2, 1, 3]);
  });

  it('ghép bong bóng đang gửi với tin server theo client_message_id', () => {
    const server = [m({ id: 1, clientId: 'a', sentAt: '2026-06-08T14:00:00Z' })];
    const pending = [
      m({ id: 'a', clientId: 'a', status: 'SENDING', sentAt: '2026-06-08T14:00:01Z' }),
      m({ id: 'b', clientId: 'b', status: 'SENDING', sentAt: '2026-06-08T14:00:02Z' }),
    ];
    const out = mergeMessages(server, pending);
    expect(out.map((x) => x.id)).toEqual([1, 'b']);
    expect(statusKey(out[1])).toBe('chat.status.SENDING');
    expect(statusKey(m({ mine: false }))).toBeNull();
  });

  it('gộp trang (trang 0 mới nhất) thành luồng cũ → mới, bỏ trùng', () => {
    const pages = [
      { items: [m({ id: 3 }), m({ id: 4 })], nextCursor: 'c1' },
      { items: [m({ id: 1 }), m({ id: 2 }), m({ id: 3 })], nextCursor: null },
    ];
    expect(flattenPages(pages).map((x) => x.id)).toEqual([1, 2, 3, 4]);
  });

  it('tin realtime tới trước / tới hai lần (ack + REST dự phòng) không bị nhân đôi', () => {
    const pages = [{ items: [m({ id: 1 })], nextCursor: null }];
    const once = insertMessage(pages, m({ id: 2, clientId: null }));
    const twice = insertMessage(once, m({ id: 2, clientId: 'x' }));
    expect(flattenPages(twice).map((x) => x.id)).toEqual([1, 2]);
    expect(flattenPages(twice)[1].clientId).toBe('x');
    expect(insertMessage([], m({ id: 9 }))[0].items).toHaveLength(1);
  });

  it('đã xem: chỉ tin của mình có id ≤ up_to', () => {
    const pages = [
      {
        items: [
          m({ id: 1, status: 'DELIVERED' }),
          m({ id: 2, mine: false, status: 'DELIVERED' }),
          m({ id: 3, status: 'DELIVERED' }),
        ],
        nextCursor: null,
      },
    ];
    const out = flattenPages(markSeen(pages, 2));
    expect(out.map((x) => x.status)).toEqual(['SEEN', 'DELIVERED', 'DELIVERED']);
  });

  it('đang gõ tắt sau 4 giây; trạng thái hoạt động', () => {
    expect(typingActive(1000, 4999)).toBe(true);
    expect(typingActive(1000, 5000)).toBe(false);
    expect(typingActive(undefined, 1)).toBe(false);
    expect(presenceOf(true, '2026-06-08T00:00:00Z')).toEqual({ key: 'chat.online', at: null });
    expect(presenceOf(false, '2026-06-08T00:00:00Z').key).toBe('chat.active_ago');
    expect(presenceOf(false, null).key).toBe('chat.offline');
  });
});
