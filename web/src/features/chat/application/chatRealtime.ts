// Kênh /user/queue/chat qua hub WebSocket dùng chung (API_SPEC 13; plan 2026-11-26 chat M06.02, D14, D15):
// một đăng ký cho cả trang, phát lại cho các cây React (mỗi island có QueryClient riêng). "Đang gõ" giữ ở nanostore.
import { atom } from 'nanostores';
import { useEffect, useState } from 'react';
import {
  isConnected,
  onStatus,
  realtimeEnabled,
  send,
  subscribe,
} from '@/shared/realtime/realtimeHub';
import { CHAT_MOCKED, type MessageDto } from '../infrastructure/chat.api';

export const CHAT_QUEUE = '/user/queue/chat';

export type ChatEventDto =
  | {
      type: 'MESSAGE_CREATED';
      conversation_id: number;
      message: MessageDto;
      client_message_id: string | null;
      unread_total: number;
    }
  | { type: 'MESSAGE_SEEN'; conversation_id: number; up_to_message_id: number }
  | { type: 'TYPING'; conversation_id: number }
  | {
      type: 'MESSAGE_FAILED';
      client_message_id: string | null;
      conversation_id: number | null;
      code: string;
      message: string;
    };

type Listener = (e: ChatEventDto) => void;

const listeners = new Set<Listener>();
let started = false;

/** Hội thoại → thời điểm (ms) nhận gói "đang gõ" gần nhất. */
export const $typing = atom<Record<number, number>>({});

/** Chat dùng realtime thật không (cờ `chat` + phiên thật + trình duyệt có WebSocket). */
export const chatRealtimeOn = (): boolean => !CHAT_MOCKED && realtimeEnabled();

function handle(body: unknown): void {
  const e = body as ChatEventDto | null;
  if (!e?.type) return;
  if (e.type === 'TYPING') {
    $typing.set({ ...$typing.get(), [e.conversation_id]: Date.now() });
  } else if (e.type === 'MESSAGE_CREATED' && e.message.sender !== 'ME') {
    // tin tới thì bên kia đã gõ xong
    const { [e.conversation_id]: _, ...rest } = $typing.get();
    $typing.set(rest);
  }
  for (const l of listeners) l(e);
}

/** Nghe gói tin chat; trả hàm hủy. Đăng ký hub một lần cho cả trang. */
export function onChatEvent(listener: Listener): () => void {
  if (!started && chatRealtimeOn()) {
    started = true;
    subscribe(CHAT_QUEUE, handle);
  }
  listeners.add(listener);
  return () => listeners.delete(listener);
}

/** Gửi lên `/app/chat.*`; `false` khi không dùng được WebSocket (nơi gọi dùng REST). */
export function sendChatFrame(action: 'send' | 'read' | 'typing', body: unknown): boolean {
  return chatRealtimeOn() && isConnected() && send(`/app/chat.${action}`, body);
}

/** Trạng thái kết nối hub (để bật / tắt hỏi lại dự phòng). */
export function useRealtimeConnected(): boolean {
  const [on, setOn] = useState(() => chatRealtimeOn() && isConnected());
  useEffect(() => {
    if (!chatRealtimeOn()) return undefined;
    return onStatus(setOn);
  }, []);
  return on;
}

/** Dùng trong test. */
export function emitChatEventForTest(e: ChatEventDto): void {
  handle(e);
}
