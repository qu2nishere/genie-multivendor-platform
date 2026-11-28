// Một kết nối WebSocket / STOMP cho toàn ứng dụng (API_SPEC mục 13; plan 2026-11-26 thông báo D17, N05.02):
// mở khi đã đăng nhập, đóng khi đăng xuất; các kênh (tài khoản, thông báo, chat…) chỉ đăng ký vào hub, không tự tạo
// StompConnection. Đăng ký được giữ qua những lần đăng xuất / đăng nhập lại và kết nối lại.
import { getAccessToken, refreshAccessToken, $session } from '@/shared/auth/session';
import { env, isMocked } from '@/shared/config/env';
import { StompConnection, type MessageHandler } from './stomp';

/** `http(s)://host/api/v1` → `ws(s)://host/ws`. */
export function wsUrlOf(apiUrl: string): string {
  const u = new URL(apiUrl);
  u.protocol = u.protocol === 'https:' ? 'wss:' : 'ws:';
  u.pathname = '/ws';
  u.search = '';
  u.hash = '';
  return u.toString();
}

type StatusListener = (connected: boolean) => void;

interface Registration {
  destination: string;
  handler: MessageHandler;
  /** Hủy đăng ký trên kết nối hiện tại (nếu có). */
  off: (() => void) | null;
}

let started = false;
let conn: StompConnection | null = null;
let connected = false;
const registrations = new Set<Registration>();
const statusListeners = new Set<StatusListener>();

/** Có dùng realtime không: cần trình duyệt có WebSocket và phiên đăng nhập thật (không phải mock). */
export function realtimeEnabled(): boolean {
  return typeof window !== 'undefined' && typeof WebSocket !== 'undefined' && !isMocked('auth');
}

/** Gọi (nhiều lần cũng được) từ Providers: theo dõi phiên — đăng nhập thì mở kết nối, đăng xuất thì đóng. */
export function startRealtime(): void {
  if (started || !realtimeEnabled()) return;
  started = true;
  $session.subscribe((s) => {
    if (s.status === 'authenticated' && !conn) open();
    else if (s.status !== 'authenticated' && conn) close();
  });
}

/** Đăng ký nhận tin ở `destination`; trả hàm hủy. Gọi trước hay sau khi kết nối đều được. */
export function subscribe(destination: string, handler: MessageHandler): () => void {
  const reg: Registration = { destination, handler, off: null };
  registrations.add(reg);
  if (conn) reg.off = conn.subscribe(destination, handler);
  return () => {
    reg.off?.();
    registrations.delete(reg);
  };
}

/** Gửi lên `/app/**` qua WebSocket; `false` khi chưa / mất kết nối (nơi gọi dùng REST dự phòng). */
export function send(destination: string, body: unknown): boolean {
  return conn?.send(destination, body) ?? false;
}

export function isConnected(): boolean {
  return connected;
}

/** Theo dõi trạng thái kết nối (gọi ngay với trạng thái hiện tại); trả hàm hủy. */
export function onStatus(listener: StatusListener): () => void {
  statusListeners.add(listener);
  listener(connected);
  return () => statusListeners.delete(listener);
}

function setConnected(v: boolean): void {
  if (connected === v) return;
  connected = v;
  for (const l of statusListeners) l(v);
}

function open(): void {
  conn = new StompConnection({
    url: wsUrlOf(env.PUBLIC_API_URL),
    token: getAccessToken,
    refresh: refreshAccessToken,
    onStatus: setConnected,
  });
  for (const reg of registrations) reg.off = conn.subscribe(reg.destination, reg.handler);
  conn.connect();
}

/** Đóng kết nối (đăng xuất / bị thu hồi phiên); đăng ký vẫn giữ để mở lại lần sau. */
export function closeRealtime(): void {
  close();
}

function close(): void {
  for (const reg of registrations) reg.off = null;
  conn?.disconnect();
  conn = null;
  setConnected(false);
}
