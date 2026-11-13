// Quy tắc chat — TS thuần (plan G10.62).
import type { ChatMessage, Conversation, MessageType } from './chat.types';

export const MAX_TEXT = 1000;
/** Tin nhanh (prototype QUICK) — khóa i18n. */
export const QUICK_REPLIES = ['stock', 'eta', 'discount', 'photo'] as const;

/** Khóa i18n của dòng xem trước khi tin không phải chữ (8.1: preview = null). */
export function previewKey(type: MessageType): string | null {
  if (type === 'IMAGE') return 'chat.preview.image';
  if (type === 'PRODUCT_CARD') return 'chat.preview.product';
  if (type === 'ORDER_CARD') return 'chat.preview.order';
  return null;
}

export const totalUnread = (list: Conversation[]): number => list.reduce((n, c) => n + c.unread, 0);

/** Đưa hội thoại mới cập nhật lên đầu (theo thời điểm tin cuối). */
export const sortConversations = (list: Conversation[]): Conversation[] =>
  [...list].sort((a, b) =>
    (b.lastMessage?.sentAt ?? '').localeCompare(a.lastMessage?.sentAt ?? ''),
  );

/** Ghép tin server với bong bóng đang gửi (khớp theo client_message_id), giữ thứ tự cũ → mới. */
export function mergeMessages(server: ChatMessage[], pending: ChatMessage[]): ChatMessage[] {
  const known = new Set(server.map((m) => m.clientId).filter(Boolean));
  const rest = pending.filter((p) => !p.clientId || !known.has(p.clientId));
  return [...server, ...rest].sort((a, b) => a.sentAt.localeCompare(b.sentAt));
}

/** Nhãn trạng thái dưới tin của mình. */
export const statusKey = (m: ChatMessage): string | null =>
  m.mine ? `chat.status.${m.status}` : null;
