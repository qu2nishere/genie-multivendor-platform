// Luồng thông báo thời gian thực (UC-NOT-01; API_SPEC 13; plan 2026-11-26 thông báo D5, D18, N05.05):
// một đăng ký cho cả trang (/user/queue/notifications + /topic/platform qua realtimeHub) — toast + âm "ting" phát MỘT lần;
// mỗi cây React (mỗi island có QueryClient riêng) chỉ nghe để làm mới cache. Đồng bộ các tab bằng BroadcastChannel
// khi chính tab này đánh dấu đã đọc (server cũng gửi NOTIFICATIONS_READ cho mọi kết nối của người dùng).
import { realtimeEnabled, subscribe } from '@/shared/realtime/realtimeHub';
import { toast } from '@/shared/ui/toast.store';
import { NOTIFICATIONS_MOCKED } from '../infrastructure/notifications.api';
import type { NotificationEventDto } from '../infrastructure/notifications.dto';

export const NOTIFICATIONS_QUEUE = '/user/queue/notifications';
export const PLATFORM_TOPIC = '/topic/platform';
const CHANNEL = 'genie.notifications';

export type StreamSignal = 'NOTIFICATION_SYNC' | 'READ_ONE' | 'READ_ALL';
type Listener = (signal: StreamSignal) => void;

const listeners = new Set<Listener>();
let started = false;
let channel: BroadcastChannel | null = null;

/** Gói tin realtime có thông báo mới không (để toast). */
export function isNewNotification(e: NotificationEventDto | null | undefined): boolean {
  return (
    !!e && (e.type === 'NEW_NOTIFICATION' || e.type === 'PLATFORM_NOTIFICATION') && !!e.notification
  );
}

function emit(signal: StreamSignal): void {
  for (const l of listeners) l(signal);
}

function handle(body: unknown): void {
  const e = body as NotificationEventDto | null;
  if (!e?.type) return;
  if (isNewNotification(e)) {
    if (
      typeof document !== 'undefined' &&
      document.visibilityState === 'visible' &&
      e.notification
    ) {
      toast.info(e.notification.title, 5000);
      ting();
    }
    emit('NOTIFICATION_SYNC');
  } else if (e.type === 'NOTIFICATIONS_READ') {
    emit(e.ids?.length ? 'READ_ONE' : 'READ_ALL');
  }
}

function start(): void {
  if (started || NOTIFICATIONS_MOCKED || !realtimeEnabled()) return;
  started = true;
  subscribe(NOTIFICATIONS_QUEUE, handle);
  subscribe(PLATFORM_TOPIC, handle);
  if (typeof BroadcastChannel !== 'undefined') {
    channel = new BroadcastChannel(CHANNEL);
    channel.onmessage = (m: MessageEvent) => {
      const s = m.data as StreamSignal;
      if (s === 'NOTIFICATION_SYNC' || s === 'READ_ONE' || s === 'READ_ALL') emit(s);
    };
  }
}

/** Nghe tín hiệu làm mới; trả hàm hủy. Lần đầu gọi thì mở đăng ký realtime. */
export function listenNotifications(listener: Listener): () => void {
  start();
  listeners.add(listener);
  return () => listeners.delete(listener);
}

/** Tab này vừa đánh dấu đã đọc → báo các tab khác của cùng trình duyệt. */
export function broadcastRead(signal: 'READ_ONE' | 'READ_ALL'): void {
  channel?.postMessage(signal);
}

let audio: AudioContext | null = null;

/** Âm "ting" ngắn bằng Web Audio (không thêm tệp âm thanh); trình duyệt chặn tự phát khi chưa tương tác → bỏ qua. */
function ting(): void {
  try {
    const Ctx =
      window.AudioContext ??
      (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
    if (!Ctx) return;
    audio ??= new Ctx();
    if (audio.state === 'suspended') void audio.resume();
    const now = audio.currentTime;
    const osc = audio.createOscillator();
    const gain = audio.createGain();
    osc.type = 'sine';
    osc.frequency.setValueAtTime(1318.5, now); // E6
    gain.gain.setValueAtTime(0.0001, now);
    gain.gain.exponentialRampToValueAtTime(0.18, now + 0.02);
    gain.gain.exponentialRampToValueAtTime(0.0001, now + 0.45);
    osc.connect(gain).connect(audio.destination);
    osc.start(now);
    osc.stop(now + 0.5);
  } catch {
    // không phát được âm — bỏ qua
  }
}
