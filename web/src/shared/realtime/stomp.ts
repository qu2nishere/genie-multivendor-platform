// Client STOMP 1.2 tối giản trên WebSocket thuần (API_SPEC mục 13) — đủ cho kênh server → client: CONNECT kèm
// access token, SUBSCRIBE, nhận MESSAGE, tự kết nối lại. Không dùng thư viện ngoài; gửi lên /app/** (chat) thêm sau.

export interface StompFrame {
  command: string;
  headers: Record<string, string>;
  body: string;
}

const NUL = '\0';

/** Mã hóa một khung: `COMMAND\nkhóa:giá trị\n…\n\nthân\0`. */
export function encodeFrame(command: string, headers: Record<string, string>, body = ''): string {
  const lines = Object.entries(headers).map(([k, v]) => `${k}:${v}`);
  const head = lines.length ? `${command}\n${lines.join('\n')}` : command;
  return `${head}\n\n${body}${NUL}`;
}

/** Tách dữ liệu nhận được thành các khung (một gói có thể chứa nhiều khung; dòng trống = nhịp tim, bỏ qua). */
export function parseFrames(data: string): StompFrame[] {
  const frames: StompFrame[] = [];
  for (const raw of data.split(NUL)) {
    const text = raw.replace(/^[\r\n]+/, '');
    if (!text) continue;
    const sep = text.indexOf('\n\n');
    const head = (sep < 0 ? text : text.slice(0, sep)).split(/\r?\n/);
    const body = sep < 0 ? '' : text.slice(sep + 2);
    const command = head.shift() ?? '';
    const headers: Record<string, string> = {};
    for (const line of head) {
      const i = line.indexOf(':');
      if (i > 0 && !(line.slice(0, i) in headers))
        headers[line.slice(0, i)] = unescape(line.slice(i + 1));
    }
    frames.push({ command, headers, body });
  }
  return frames;
}

/** STOMP 1.2: `\\c` → `:`, `\\n` → xuống dòng, `\\r`, `\\\\`. */
function unescape(v: string): string {
  return v.replace(/\\(.)/g, (_, c: string) =>
    c === 'c' ? ':' : c === 'n' ? '\n' : c === 'r' ? '\r' : c === '\\' ? '\\' : c,
  );
}

export type MessageHandler = (body: unknown) => void;

export interface StompOptions {
  url: string;
  /** Access token hiện tại — đọc lại mỗi lần kết nối. */
  token: () => string | null;
  /** Server từ chối CONNECT (token hết hạn) → thử làm mới; `false` = hết phiên, dừng kết nối lại. */
  refresh: () => Promise<boolean>;
}

/** Kết nối STOMP giữ các đăng ký qua những lần kết nối lại (chờ 2 → 4 → … tối đa 30 giây). */
export class StompConnection {
  private ws: WebSocket | null = null;
  private subs = new Map<string, { destination: string; handler: MessageHandler }>();
  private nextId = 0;
  private stopped = false;
  private retry = 0;
  private timer: ReturnType<typeof setTimeout> | null = null;
  private rejected = false;

  constructor(private readonly opts: StompOptions) {}

  subscribe(destination: string, handler: MessageHandler): () => void {
    const id = `sub-${this.nextId++}`;
    this.subs.set(id, { destination, handler });
    if (this.ws?.readyState === WebSocket.OPEN) this.send('SUBSCRIBE', { id, destination });
    return () => {
      this.subs.delete(id);
      if (this.ws?.readyState === WebSocket.OPEN) this.send('UNSUBSCRIBE', { id });
    };
  }

  connect(): void {
    const token = this.opts.token();
    if (this.stopped || !token) return;
    const ws = new WebSocket(this.opts.url);
    this.ws = ws;
    this.rejected = false;
    ws.onopen = () => {
      this.send('CONNECT', {
        'accept-version': '1.2',
        host: new URL(this.opts.url).host,
        'heart-beat': '0,0',
        Authorization: `Bearer ${token}`,
      });
    };
    ws.onmessage = (e: MessageEvent) => {
      if (typeof e.data !== 'string') return;
      for (const f of parseFrames(e.data)) this.handle(f);
    };
    ws.onclose = () => {
      if (this.ws === ws) this.ws = null;
      if (!this.stopped) void this.reconnect();
    };
  }

  disconnect(): void {
    this.stopped = true;
    if (this.timer) clearTimeout(this.timer);
    const ws = this.ws;
    this.ws = null;
    if (ws && ws.readyState === WebSocket.OPEN) {
      try {
        ws.send(encodeFrame('DISCONNECT', {}));
      } catch {
        // bỏ qua
      }
    }
    ws?.close();
  }

  private handle(f: StompFrame): void {
    if (f.command === 'CONNECTED') {
      this.retry = 0;
      for (const [id, s] of this.subs) this.send('SUBSCRIBE', { id, destination: s.destination });
    } else if (f.command === 'MESSAGE') {
      const sub = this.subs.get(f.headers.subscription ?? '');
      if (!sub) return;
      let body: unknown = f.body;
      try {
        body = JSON.parse(f.body);
      } catch {
        // thân không phải JSON — giữ chuỗi
      }
      sub.handler(body);
    } else if (f.command === 'ERROR') {
      this.rejected = true; // server đóng kết nối ngay sau khung ERROR
    }
  }

  private async reconnect(): Promise<void> {
    if (this.rejected && !(await this.opts.refresh())) {
      this.stopped = true; // không làm mới được phiên → đã là khách
      return;
    }
    const delay = Math.min(30_000, 2_000 * 2 ** this.retry++);
    this.timer = setTimeout(() => this.connect(), delay);
  }

  private send(command: string, headers: Record<string, string>): void {
    this.ws?.send(encodeFrame(command, headers));
  }
}
