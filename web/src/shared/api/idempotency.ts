// Idempotency-Key UUIDv4 (plan G05.05; TS mục 8) — bắt buộc cho tạo phiên checkout, thanh toán,
// rút tiền, hoàn tiền. Key của lần đặt hàng giữ trong sessionStorage `genie.checkout.idem`
// tới khi đặt thành công, để bấm lại / tải lại trang không tạo đơn trùng (LUU_TRU_DU_LIEU 6.2).
import { store } from '@/shared/lib/storage';

export const IDEMPOTENCY_HEADER = 'Idempotency-Key';

/** UUID v4 ngẫu nhiên; dùng crypto.randomUUID khi có, nếu không tự dựng từ getRandomValues. */
export function newIdempotencyKey(): string {
  const c = globalThis.crypto;
  if (c && typeof c.randomUUID === 'function') return c.randomUUID();
  const b = new Uint8Array(16);
  if (c && typeof c.getRandomValues === 'function') c.getRandomValues(b);
  else for (let i = 0; i < 16; i++) b[i] = Math.floor(Math.random() * 256);
  b[6] = (b[6] & 0x0f) | 0x40;
  b[8] = (b[8] & 0x3f) | 0x80;
  const h = Array.from(b, (x) => x.toString(16).padStart(2, '0')).join('');
  return `${h.slice(0, 8)}-${h.slice(8, 12)}-${h.slice(12, 16)}-${h.slice(16, 20)}-${h.slice(20)}`;
}

export const UUID_V4 = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

/** Key của lần đặt hàng đang dở: dùng lại nếu đã có, chưa có thì tạo và lưu. */
export function checkoutIdempotencyKey(): string {
  const saved = store.get<string>('checkoutIdem');
  if (saved && UUID_V4.test(saved)) return saved;
  const key = newIdempotencyKey();
  store.set('checkoutIdem', key);
  return key;
}

/** Gọi sau khi đặt hàng thành công. */
export function clearCheckoutIdempotencyKey(): void {
  store.remove('checkoutIdem');
}
