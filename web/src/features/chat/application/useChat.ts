// Chat (TanStack Query): hộp thư, mở hội thoại, luồng tin (hỏi lại 2 giây khi đang mở — thay cho
// WebSocket ở mục 13 cho tới khi nối), gửi tin có bong bóng "Đang gửi…", đánh dấu đã xem.
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback, useState } from 'react';
import { newIdempotencyKey } from '@/shared/api/idempotency';
import type { ChatMessage, OutgoingMessage } from '../domain/chat.types';
import {
  listConversations,
  listMessages,
  markRead,
  mockTyping,
  openConversation,
  sendMessage,
} from '../infrastructure/chat.api';

export const CONVERSATIONS_KEY = ['me', 'conversations'] as const;
const messagesKey = (id: number) => [...CONVERSATIONS_KEY, id, 'messages'] as const;

export function useConversations(enabled = true) {
  return useQuery({
    queryKey: CONVERSATIONS_KEY,
    queryFn: listConversations,
    enabled,
    refetchInterval: enabled ? 15_000 : false,
  });
}

export function useOpenConversation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: openConversation,
    onSuccess: () => qc.invalidateQueries({ queryKey: CONVERSATIONS_KEY }),
  });
}

export function useMessages(conversationId: number | null) {
  return useQuery({
    queryKey: messagesKey(conversationId ?? 0),
    queryFn: () => listMessages(conversationId ?? 0),
    enabled: conversationId != null,
    refetchInterval: conversationId != null ? 2000 : false,
  });
}

export const isTyping = (conversationId: number | null): boolean =>
  conversationId != null && mockTyping(conversationId);

/** Gửi tin: hiện ngay bong bóng SENDING, xong thì tải lại luồng + hộp thư. */
export function useSendMessage(conversationId: number | null) {
  const qc = useQueryClient();
  const [pending, setPending] = useState<ChatMessage[]>([]);
  const send = useCallback(
    async (
      m: OutgoingMessage,
      preview: Partial<Pick<ChatMessage, 'text' | 'imageUrl' | 'product' | 'order'>> = {},
    ) => {
      if (conversationId == null) return;
      const clientId = newIdempotencyKey();
      const bubble: ChatMessage = {
        id: clientId,
        mine: true,
        type: m.type,
        text: m.type === 'TEXT' ? m.text : null,
        product: null,
        order: null,
        imageUrl: m.type === 'IMAGE' ? m.previewUrl : null,
        sentAt: new Date().toISOString(),
        status: 'SENDING',
        clientId,
        ...preview,
      };
      setPending((p) => [...p, bubble]);
      try {
        await sendMessage(conversationId, m, clientId, {
          product: preview.product
            ? {
                id: preview.product.id,
                slug: preview.product.slug,
                name: preview.product.name,
                image_url: preview.product.image,
                price: preview.product.price,
              }
            : undefined,
          order: preview.order ?? undefined,
        });
        await qc.invalidateQueries({ queryKey: messagesKey(conversationId) });
        void qc.invalidateQueries({ queryKey: CONVERSATIONS_KEY });
        setPending((p) => p.filter((x) => x.clientId !== clientId));
      } catch {
        setPending((p) => p.map((x) => (x.clientId === clientId ? { ...x, status: 'FAILED' } : x)));
      }
    },
    [conversationId, qc],
  );
  return { send, pending };
}

export function useMarkRead() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: markRead,
    onSuccess: () => qc.invalidateQueries({ queryKey: CONVERSATIONS_KEY }),
  });
}
