import { describe, expect, it } from 'vitest';
import { mergeMessages, previewKey, sortConversations, statusKey, totalUnread } from './chat.rules';
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
});
