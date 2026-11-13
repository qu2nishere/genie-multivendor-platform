// Gửi tin ngoài luồng soạn (thẻ sản phẩm / đơn tự gửi khi mở chat từ trang sản phẩm / đơn — 8.2).
import { newIdempotencyKey } from '@/shared/api/idempotency';
import type { OutgoingMessage } from '../domain/chat.types';
import { sendMessage as send } from '../infrastructure/chat.api';

export function sendMessage(
  conversationId: number,
  m: OutgoingMessage,
  card: Parameters<typeof send>[3],
) {
  return send(conversationId, m, newIdempotencyKey(), card);
}
