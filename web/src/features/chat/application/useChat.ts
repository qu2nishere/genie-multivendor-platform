// Chat phía khách (TanStack Query): hộp thư, mở hội thoại, luồng tin realtime qua hub WebSocket (API_SPEC 13;
// plan 2026-11-26 chat M06) — hỏi lại dự phòng khi mất kết nối, bong bóng "Đang gửi…", đã xem, đang gõ.
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { ChatMessage, OutgoingMessage } from '../domain/chat.types';
import {
  listConversations,
  listMessagesPage,
  markRead,
  openConversation,
  sendMessage,
} from '../infrastructure/chat.api';
import {
  pollInterval,
  useChatInboxSync,
  useThreadMessages,
  useThreadRead,
  useThreadSend,
  type ChatSideApi,
} from './chatThread';

export const CONVERSATIONS_KEY = ['me', 'conversations'] as const;

export const BUYER_CHAT: ChatSideApi = {
  key: CONVERSATIONS_KEY,
  listPage: listMessagesPage,
  sendRest: (id: number, m: OutgoingMessage, clientId: string): Promise<ChatMessage> =>
    sendMessage(id, m, clientId),
  markReadRest: markRead,
};

export function useConversations(enabled = true) {
  const connected = useChatInboxSync(BUYER_CHAT);
  const poll = pollInterval(connected);
  return useQuery({
    queryKey: CONVERSATIONS_KEY,
    queryFn: listConversations,
    enabled,
    refetchInterval: enabled && poll ? Math.max(poll, 15_000) : false,
  });
}

export function useOpenConversation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: openConversation,
    onSuccess: () => qc.invalidateQueries({ queryKey: CONVERSATIONS_KEY }),
  });
}

export const useMessages = (conversationId: number | null) =>
  useThreadMessages(BUYER_CHAT, conversationId);

export const useSendMessage = (conversationId: number | null) =>
  useThreadSend(BUYER_CHAT, conversationId);

export const useMarkRead = () => useThreadRead(BUYER_CHAT);
