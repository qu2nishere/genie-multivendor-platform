// Quy tắc chat — TS thuần (plan G10.62; plan 2026-11-26 chat M06.06).
import type { ChatMessage, Conversation, MessagePage, MessageType } from './chat.types';

/** API_SPEC 8.4 — 1–2.000 ký tự. */
export const MAX_TEXT = 2000;
/** "Đang gõ…" tắt sau 4 giây không nhận thêm (D14). */
export const TYPING_TTL_MS = 4000;
/** Gửi "đang gõ" tối đa 1 lần / 2 giây. */
export const TYPING_THROTTLE_MS = 2000;
/** Gửi qua WebSocket mà quá hạn này chưa có ack → gửi lại bằng REST (cùng client_message_id — không trùng tin). */
export const ACK_TIMEOUT_MS = 5000;
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
  // tin server giữ nguyên thứ tự (đã sắp theo thời điểm + id); bong bóng chưa có ack luôn ở cuối
  return [...server, ...rest.sort((a, b) => a.sentAt.localeCompare(b.sentAt))];
}

/** Gộp các trang (trang 0 = mới nhất) thành luồng cũ → mới, bỏ tin trùng id. */
export function flattenPages(pages: MessagePage[]): ChatMessage[] {
  const seen = new Set<string>();
  const out: ChatMessage[] = [];
  for (let i = pages.length - 1; i >= 0; i--) {
    for (const m of pages[i].items) {
      const k = String(m.id);
      if (!seen.has(k)) {
        seen.add(k);
        out.push(m);
      }
    }
  }
  return out;
}

/**
 * Chèn tin realtime vào trang mới nhất (tin có thể tới TRƯỚC phản hồi REST hoặc tới hai lần — ack + REST dự phòng):
 * trùng id thì giữ bản đã có nhưng gắn `clientId` nếu bản mới có.
 */
export function insertMessage(pages: MessagePage[], m: ChatMessage): MessagePage[] {
  if (!pages.length) return [{ items: [m], nextCursor: null }];
  let found = false;
  const next = pages.map((p) => ({
    ...p,
    items: p.items.map((x) => {
      if (String(x.id) !== String(m.id)) return x;
      found = true;
      return m.clientId && !x.clientId ? { ...x, clientId: m.clientId } : x;
    }),
  }));
  if (found) return next;
  const [first, ...rest] = next;
  return [{ ...first, items: [...first.items, m] }, ...rest];
}

/** Bên kia đã xem tới `upTo` → tin của mình có id ≤ upTo thành SEEN. */
export function markSeen(pages: MessagePage[], upTo: number): MessagePage[] {
  return pages.map((p) => ({
    ...p,
    items: p.items.map((m) =>
      m.mine && typeof m.id === 'number' && m.id <= upTo && m.status !== 'SEEN'
        ? { ...m, status: 'SEEN' as const }
        : m,
    ),
  }));
}

/** Ảnh chat hợp lệ trước khi nén / tải lên (UC-SOC-02 ngoại lệ — báo ngay tại chỗ): JPEG / PNG / WebP ≤ 5 MB. */
export const CHAT_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'] as const;
export const CHAT_IMAGE_MAX_BYTES = 5 * 1024 * 1024;
export const chatImageValid = (f: { type: string; size: number }): boolean =>
  (CHAT_IMAGE_TYPES as readonly string[]).includes(f.type) &&
  f.size > 0 &&
  f.size <= CHAT_IMAGE_MAX_BYTES;

/** Còn hiện "đang gõ" không. */
export const typingActive = (at: number | undefined, now: number): boolean =>
  at != null && now - at < TYPING_TTL_MS;

/** "Đang hoạt động" / "Hoạt động x phút trước" / "Ngoại tuyến" — trả khóa i18n + thời điểm. */
export function presenceOf(
  online: boolean,
  lastActiveAt: string | null,
): {
  key: 'chat.online' | 'chat.active_ago' | 'chat.offline';
  at: string | null;
} {
  if (online) return { key: 'chat.online', at: null };
  return lastActiveAt
    ? { key: 'chat.active_ago', at: lastActiveAt }
    : { key: 'chat.offline', at: null };
}

/** Nhãn trạng thái dưới tin của mình. */
export const statusKey = (m: ChatMessage): string | null =>
  m.mine ? `chat.status.${m.status}` : null;
