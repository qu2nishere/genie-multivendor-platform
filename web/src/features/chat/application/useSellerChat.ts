// Chat phía shop (TanStack Query) — cùng hook luồng tin realtime với phía khách (plan 2026-11-26 chat M06);
// đọc xong làm mới huy hiệu nav + Tổng quan (['seller', …]).
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import {
  listSellerConversations,
  listSellerMessagesPage,
  markSellerRead,
  sendSellerMessage,
} from '../infrastructure/seller-chat.api';
import {
  pollInterval,
  useChatInboxSync,
  useThreadMessages,
  useThreadRead,
  useThreadSend,
  type ChatSideApi,
} from './chatThread';
import { onChatEvent } from './chatRealtime';

const KEY = ['seller', 'conversations'] as const;

export const SELLER_CHAT: ChatSideApi = {
  key: KEY,
  listPage: listSellerMessagesPage,
  sendRest: (id, m, clientId) => sendSellerMessage(id, m, clientId),
  markReadRest: markSellerRead,
  afterRead: ['seller'],
};

export function useSellerConversations() {
  const connected = useChatInboxSync(SELLER_CHAT);
  const poll = pollInterval(connected);
  return useQuery({
    queryKey: KEY,
    queryFn: listSellerConversations,
    refetchInterval: poll ? Math.max(poll, 15_000) : false,
  });
}

/** Khung Seller Center: tin mới / đã đọc ở bất kỳ màn nào → làm mới badge nav Chat (`GET /seller/me` → `badges`). */
export function useSellerChatBadgeSync(): void {
  const qc = useQueryClient();
  useEffect(
    () =>
      onChatEvent((e) => {
        if (e.type === 'MESSAGE_CREATED') void qc.invalidateQueries({ queryKey: ['seller', 'me'] });
      }),
    [qc],
  );
}

export const useSellerMessages = (id: number | null) => useThreadMessages(SELLER_CHAT, id);

export const useSellerSend = (id: number | null) => useThreadSend(SELLER_CHAT, id);

export const useSellerMarkRead = () => useThreadRead(SELLER_CHAT);
