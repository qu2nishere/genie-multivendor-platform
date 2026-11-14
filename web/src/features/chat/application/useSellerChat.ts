// Chat phía shop (TanStack Query) — hỏi lại tin 2 giây khi đang mở (thay WebSocket mục 13 tới khi nối),
// bong bóng "Đang gửi…" như phía khách; đọc xong làm mới huy hiệu nav + Tổng quan (['seller', …]).
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback, useState } from 'react';
import { newIdempotencyKey } from '@/shared/api/idempotency';
import type { ChatMessage, OutgoingMessage } from '../domain/chat.types';
import {
  listSellerConversations,
  listSellerMessages,
  markSellerRead,
  sendSellerMessage,
} from '../infrastructure/seller-chat.api';

const KEY = ['seller', 'conversations'] as const;
const msgKey = (id: number) => [...KEY, id, 'messages'] as const;

export const useSellerConversations = () =>
  useQuery({ queryKey: KEY, queryFn: listSellerConversations, refetchInterval: 15_000 });

export const useSellerMessages = (id: number | null) =>
  useQuery({
    queryKey: msgKey(id ?? 0),
    queryFn: () => listSellerMessages(id ?? 0),
    enabled: id != null,
    refetchInterval: id != null ? 2000 : false,
  });

export function useSellerSend(id: number | null) {
  const qc = useQueryClient();
  const [pending, setPending] = useState<ChatMessage[]>([]);
  const send = useCallback(
    async (m: OutgoingMessage, order?: { id: number; code: string }) => {
      if (id == null) return;
      const clientId = newIdempotencyKey();
      setPending((p) => [
        ...p,
        {
          id: clientId,
          mine: true,
          type: m.type,
          text: m.type === 'TEXT' ? m.text : null,
          product: null,
          order: order ?? null,
          imageUrl: m.type === 'IMAGE' ? m.previewUrl : null,
          sentAt: new Date().toISOString(),
          status: 'SENDING',
          clientId,
        },
      ]);
      try {
        await sendSellerMessage(id, m, clientId, { order });
        await qc.invalidateQueries({ queryKey: msgKey(id) });
        void qc.invalidateQueries({ queryKey: KEY });
        setPending((p) => p.filter((x) => x.clientId !== clientId));
      } catch {
        setPending((p) => p.map((x) => (x.clientId === clientId ? { ...x, status: 'FAILED' } : x)));
      }
    },
    [id, qc],
  );
  return { send, pending };
}

export function useSellerMarkRead() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: markSellerRead,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['seller'] }),
  });
}
