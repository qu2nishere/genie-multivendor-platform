// Đặt hàng & thanh toán (API_SPEC 6.10–6.12). Mọi lệnh tạo tiền gửi Idempotency-Key (TS 8).
import { api } from '@/shared/api/http';
import { mockDelay, mockFail, USE_MOCK } from '@/shared/api/mock';
import { placedStore } from '@/shared/api/mock/placed';
import type {
  CheckoutQuote,
  CheckoutSession,
  PaymentMethod,
  PlaceOrderInput,
} from '../domain/checkout.types';
import type { CheckoutSessionDto, PaymentDto } from './checkout.dto';
import { toCheckoutSession, toPayment, toPlaceOrderDto } from './checkout.mapper';
import {
  MOCK_SEPAY_MS,
  MOCK_VNPAY_URL,
  mockPlace,
  mockRepay,
  mockSettle,
  sessionDto,
} from './checkout.mock';

export { MOCK_VNPAY_URL };

/**
 * POST /me/checkout-sessions — đặt hàng: server tính lại giá, giữ chỗ 15 phút, tách đơn theo shop,
 * xóa dòng đã đặt khỏi giỏ. Lỗi: 409 GNE-INV-4090 (hết hàng — details.out_of_stock_cart_item_ids),
 * GNE-ORD-4093 (shop nghỉ), 422 GNE-VCH-4221 (voucher hết hiệu lực), 503 GNE-SYS-5030 (thử lại cùng key).
 * `quote` chỉ dùng khi chạy mock (xem CheckoutQuote).
 */
export async function createCheckout(
  input: PlaceOrderInput,
  idempotencyKey: string,
  quote: CheckoutQuote,
): Promise<CheckoutSession> {
  if (USE_MOCK) {
    await mockDelay(700);
    if (!input.cartItemIds.length) mockFail('GNE-VAL-4220');
    // Cùng key → trả lại phiên cũ, không tạo đơn trùng
    const again = placedStore.get().sessions.find((s) => s.idempotency_key === idempotencyKey);
    if (again) return toCheckoutSession(sessionDto(again, true));
    return toCheckoutSession(sessionDto(mockPlace(input, quote, new Date(), idempotencyKey), true));
  }
  return toCheckoutSession(
    await api.post<CheckoutSessionDto>('/me/checkout-sessions', toPlaceOrderDto(input), {
      idempotencyKey,
    }),
  );
}

/** GET /me/checkout-sessions/{id} — kết quả đặt hàng / trạng thái thanh toán (trang kết quả, poll VietQR). */
export async function getCheckoutSession(id: number): Promise<CheckoutSession> {
  if (USE_MOCK) {
    await mockDelay(150);
    let s = placedStore.get().sessions.find((x) => x.id === id);
    if (!s) mockFail('GNE-ORD-4041');
    // Mô phỏng SePay báo đã nhận tiền chuyển khoản sau ~12 giây (thật: webhook SePay → server)
    if (
      s.payment_method === 'VIETQR' &&
      s.status === 'PAYMENT_PENDING' &&
      Date.now() - Date.parse(s.updated_at ?? s.created_at) > MOCK_SEPAY_MS
    )
      s = mockSettle(id, true) ?? s;
    return toCheckoutSession(sessionDto(s, s.status === 'PAYMENT_PENDING'));
  }
  return toCheckoutSession(await api.get<CheckoutSessionDto>(`/me/checkout-sessions/${id}`));
}

/** POST /me/checkout-sessions/{id}/payments — thanh toán lại / đổi phương thức. */
export async function retryPayment(
  id: number,
  method: PaymentMethod,
  idempotencyKey: string,
): Promise<CheckoutSession> {
  if (USE_MOCK) {
    await mockDelay(400);
    const s = mockRepay(id, method);
    if (!s) mockFail('GNE-ORD-4041');
    if (Date.parse(s.reservation_expires_at) < Date.now()) mockFail('GNE-INV-4093');
    return toCheckoutSession(sessionDto(s, true));
  }
  const r = await api.post<{ payment: PaymentDto }>(
    `/me/checkout-sessions/${id}/payments`,
    { payment_method: method, return_url: `${window.location.origin}/checkout/result` },
    { idempotencyKey },
  );
  return { ...(await getCheckoutSession(id)), payment: toPayment(r.payment) };
}

/**
 * Chỉ mock: cổng VNPay / SePay giả lập báo kết quả (thật do cổng gọi IPN về server — client không
 * tự đổi trạng thái).
 */
export async function settleMockPayment(id: number, ok: boolean): Promise<void> {
  if (!USE_MOCK) return;
  await mockDelay(300);
  mockSettle(id, ok);
}
