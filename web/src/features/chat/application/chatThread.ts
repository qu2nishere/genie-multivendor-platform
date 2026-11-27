// Hook luồng tin dùng chung cho khách (/me) và shop (/seller) — plan 2026-11-26 chat M06.02–M06.03:
// lịch sử cursor (cuộn lên tải thêm 30 tin), tin realtime chèn thẳng vào cache, gửi qua /app/chat.send khi hub đang
// kết nối (quá hạn chưa có ack → gửi lại bằng REST cùng client_message_id — không trùng tin), đã xem, đang gõ.
// Mất kết nối → hỏi lại 30 giây; mock → hỏi lại 2 giây như trước.
import { useStore } from '@nanostores/react';
import { useInfiniteQuery, useQueryClient, type InfiniteData } from '@tanstack/react-query';
import { useCallback, useEffect, useRef, useState } from 'react';
import { ApiError } from '@/shared/api/errors';
import { newIdempotencyKey } from '@/shared/api/idempotency';
import {
  ACK_TIMEOUT_MS,
  flattenPages,
  insertMessage,
  markSeen,
  TYPING_THROTTLE_MS,
  typingActive,
} from '../domain/chat.rules';
import type { ChatMessage, MessagePage, OutgoingMessage } from '../domain/chat.types';
import { CHAT_MOCKED, mockTyping, outgoingBody, toMessage } from '../infrastructure/chat.api';
import { $typing, onChatEvent, sendChatFrame, useRealtimeConnected } from './chatRealtime';

/** Một phía của chat (khách / shop). */
export interface ChatSideApi {
  /** Khóa TanStack Query của hộp thư phía này. */
  key: readonly unknown[];
  listPage: (conversationId: number, before: string | null) => Promise<MessagePage>;
  sendRest: (conversationId: number, m: OutgoingMessage, clientId: string) => Promise<ChatMessage>;
  markReadRest: (conversationId: number) => Promise<void>;
  /** Khóa cần làm mới thêm sau khi đọc (vd. badge Seller ['seller']). */
  afterRead?: readonly unknown[];
}

const messagesKey = (side: ChatSideApi, id: number) => [...side.key, id, 'messages'] as const;

type Pages = InfiniteData<MessagePage, string | null>;

/** Khoảng hỏi lại dự phòng. */
export function pollInterval(connected: boolean): number | false {
  if (CHAT_MOCKED) return 2000;
  return connected ? false : 30_000;
}

/** Ở cấp hộp thư: tin mới / đã xem → làm mới danh sách; kết nối lại → tải lại (tin rơi khi mất mạng). */
export function useChatInboxSync(side: ChatSideApi): boolean {
  const qc = useQueryClient();
  const connected = useRealtimeConnected();
  const was = useRef(connected);
  useEffect(
    () =>
      onChatEvent((e) => {
        if (e.type === 'MESSAGE_CREATED' || e.type === 'MESSAGE_SEEN')
          void qc.invalidateQueries({ queryKey: side.key, exact: true });
      }),
    [qc, side.key],
  );
  useEffect(() => {
    if (connected && !was.current) void qc.invalidateQueries({ queryKey: side.key });
    was.current = connected;
  }, [connected, qc, side.key]);
  return connected;
}

export function useThreadMessages(side: ChatSideApi, conversationId: number | null) {
  const qc = useQueryClient();
  const connected = useRealtimeConnected();
  const key = messagesKey(side, conversationId ?? 0);
  const q = useInfiniteQuery({
    queryKey: key,
    queryFn: ({ pageParam }) => side.listPage(conversationId ?? 0, pageParam),
    initialPageParam: null as string | null,
    getNextPageParam: (last) => last.nextCursor,
    enabled: conversationId != null,
    refetchInterval: conversationId != null ? pollInterval(connected) : false,
  });
  useEffect(() => {
    if (conversationId == null) return undefined;
    return onChatEvent((e) => {
      if (e.type === 'MESSAGE_CREATED' && e.conversation_id === conversationId) {
        const m = toMessage(e.message);
        qc.setQueryData<Pages>(key, (d) => (d ? { ...d, pages: insertMessage(d.pages, m) } : d));
      } else if (e.type === 'MESSAGE_SEEN' && e.conversation_id === conversationId) {
        qc.setQueryData<Pages>(key, (d) =>
          d ? { ...d, pages: markSeen(d.pages, e.up_to_message_id) } : d,
        );
      }
    });
    // key suy từ conversationId
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [conversationId, qc]);
  return {
    messages: q.data ? flattenPages(q.data.pages) : [],
    hasOlder: q.hasNextPage,
    loadingOlder: q.isFetchingNextPage,
    loadOlder: () => void q.fetchNextPage(),
    isPending: q.isPending && conversationId != null,
    isFetched: q.isFetched,
  };
}

/** Gửi tin: bong bóng "Đang gửi…" ngay, ack realtime / phản hồi REST thì bỏ bong bóng; lỗi → "Gửi lại". */
export function useThreadSend(side: ChatSideApi, conversationId: number | null) {
  const qc = useQueryClient();
  const [pending, setPending] = useState<ChatMessage[]>([]);
  const outgoing = useRef(new Map<string, { m: OutgoingMessage; timer: number | null }>());

  const settle = useCallback(
    (clientId: string, saved: ChatMessage | null, error?: string) => {
      const o = outgoing.current.get(clientId);
      if (o?.timer != null) window.clearTimeout(o.timer);
      if (saved && conversationId != null) {
        outgoing.current.delete(clientId);
        qc.setQueryData<Pages>(messagesKey(side, conversationId), (d) =>
          d ? { ...d, pages: insertMessage(d.pages, { ...saved, clientId }) } : d,
        );
        setPending((p) => p.filter((x) => x.clientId !== clientId));
        void qc.invalidateQueries({ queryKey: side.key, exact: true });
      } else {
        setPending((p) =>
          p.map((x) =>
            x.clientId === clientId ? { ...x, status: 'FAILED', text: x.text, failure: error } : x,
          ),
        );
      }
    },
    [conversationId, qc, side],
  );

  const viaRest = useCallback(
    async (clientId: string, m: OutgoingMessage) => {
      if (conversationId == null) return;
      try {
        settle(clientId, await side.sendRest(conversationId, m, clientId));
      } catch (e) {
        settle(clientId, null, e instanceof ApiError ? e.code : undefined);
      }
    },
    [conversationId, settle, side],
  );

  useEffect(
    () =>
      onChatEvent((e) => {
        const id =
          e.type === 'MESSAGE_CREATED' || e.type === 'MESSAGE_FAILED' ? e.client_message_id : null;
        if (!id || !outgoing.current.has(id)) return;
        if (e.type === 'MESSAGE_CREATED') settle(id, toMessage(e.message));
        else if (e.type === 'MESSAGE_FAILED') settle(id, null, e.code);
      }),
    [settle],
  );
  useEffect(
    () => () => {
      for (const o of outgoing.current.values()) if (o.timer != null) window.clearTimeout(o.timer);
    },
    [],
  );

  const dispatch = useCallback(
    (clientId: string, m: OutgoingMessage) => {
      if (conversationId == null) return;
      const viaSocket = sendChatFrame('send', {
        conversation_id: conversationId,
        ...outgoingBody(m, clientId),
      });
      const entry = { m, timer: null as number | null };
      outgoing.current.set(clientId, entry);
      if (viaSocket) {
        entry.timer = window.setTimeout(() => {
          entry.timer = null;
          if (outgoing.current.has(clientId)) void viaRest(clientId, m);
        }, ACK_TIMEOUT_MS);
      } else {
        void viaRest(clientId, m);
      }
    },
    [conversationId, viaRest],
  );

  const send = useCallback(
    (
      m: OutgoingMessage,
      preview: Partial<Pick<ChatMessage, 'text' | 'imageUrl' | 'product' | 'order'>> = {},
    ) => {
      if (conversationId == null) return;
      const clientId = newIdempotencyKey();
      setPending((p) => [
        ...p,
        {
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
          auto: false,
          ...preview,
        },
      ]);
      dispatch(clientId, m);
    },
    [conversationId, dispatch],
  );

  /** Gửi lại bong bóng lỗi — cùng client_message_id (server trả lại tin đã lưu nếu lần trước thực ra đã tới). */
  const retry = useCallback(
    (clientId: string) => {
      const o = outgoing.current.get(clientId);
      if (!o) return;
      setPending((p) => p.map((x) => (x.clientId === clientId ? { ...x, status: 'SENDING' } : x)));
      dispatch(clientId, o.m);
    },
    [dispatch],
  );

  return { send, retry, pending };
}

/** Đánh dấu đã xem: qua WebSocket khi được, không thì REST; làm mới hộp thư (+ badge). */
export function useThreadRead(side: ChatSideApi) {
  const qc = useQueryClient();
  return useCallback(
    async (conversationId: number) => {
      const refresh = () => {
        void qc.invalidateQueries({ queryKey: side.key, exact: true });
        if (side.afterRead) void qc.invalidateQueries({ queryKey: side.afterRead });
      };
      if (sendChatFrame('read', { conversation_id: conversationId })) {
        window.setTimeout(refresh, 400);
        return;
      }
      try {
        await side.markReadRest(conversationId);
      } finally {
        refresh();
      }
    },
    [qc, side],
  );
}

/** Bên kia đang gõ (tắt sau 4 giây) + hàm báo mình đang gõ (tối đa 1 lần / 2 giây). */
export function useTyping(conversationId: number | null, mockFetched = true) {
  const map = useStore($typing);
  const [now, setNow] = useState(() => Date.now());
  const last = useRef(0);
  const at = conversationId == null ? undefined : map[conversationId];
  useEffect(() => {
    if (at == null) return undefined;
    setNow(Date.now());
    const t = window.setTimeout(() => setNow(Date.now()), 4100);
    return () => window.clearTimeout(t);
  }, [at]);
  const active = CHAT_MOCKED
    ? conversationId != null && mockFetched && mockTyping(conversationId)
    : typingActive(at, now);
  const notify = useCallback(() => {
    if (conversationId == null || Date.now() - last.current < TYPING_THROTTLE_MS) return;
    last.current = Date.now();
    sendChatFrame('typing', { conversation_id: conversationId });
  }, [conversationId]);
  return { typing: active, notifyTyping: notify };
}
