// Mock đặt hàng (API_SPEC 6.10–6.12): dựng phiên + N đơn con từ bảng giá client, ghi vào kho
// "đơn vừa đặt" dùng chung (giỏ bỏ dòng đã đặt, Điểm bị trừ, Đơn mua hiện đơn mới).
import {
  placedStore,
  type MockPlacedOrder,
  type MockPlacedSession,
} from '@/shared/api/mock/placed';
import { holdDeadline, orderCode } from '../domain/checkout.rules';
import type { CheckoutQuote, PlaceOrderInput } from '../domain/checkout.types';
import type { CheckoutSessionDto, PaymentDto } from './checkout.dto';

/** Tài khoản nhận VietQR của sàn (Genie giữ tiền). */
const BANK = {
  bank_name: 'MB Bank',
  account_number: '000088662024',
  account_name: 'GENIE GIU TIEN',
};
/** VNPay mô phỏng: client nhận dạng tiền tố này để mở cổng giả lập thay vì chuyển trang. */
export const MOCK_VNPAY_URL = 'mock:vnpay';
/** SePay mô phỏng xác nhận chuyển khoản sau khoảng thời gian này. */
export const MOCK_SEPAY_MS = 12_000;

export function sessionDto(s: MockPlacedSession, withPayment: boolean): CheckoutSessionDto {
  const orders = placedStore.get().orders.filter((o) => s.order_ids.includes(o.id));
  const first = orders[0];
  const payment: PaymentDto | undefined = !withPayment
    ? undefined
    : s.payment_method === 'VNPAY'
      ? { method: 'VNPAY', payment_url: MOCK_VNPAY_URL }
      : s.payment_method === 'VIETQR'
        ? {
            method: 'VIETQR',
            vietqr: {
              ...BANK,
              amount: s.summary.grand_total,
              transfer_content: first ? first.code.replace(/-/g, '') : s.code,
              qr_payload: `00020101021238${s.code}${s.summary.grand_total}`,
              expires_at: s.reservation_expires_at,
            },
          }
        : { method: 'COD' };
  return {
    id: s.id,
    code: s.code,
    status: s.status,
    payment_method: s.payment_method,
    created_at: s.created_at,
    reservation_expires_at: s.reservation_expires_at,
    orders: orders.map((o) => ({
      id: o.id,
      code: o.code,
      shop: o.shop,
      grand_total: o.grand_total,
    })),
    summary: s.summary,
    ...(payment ? { payment } : {}),
  };
}

export function mockPlace(
  input: PlaceOrderInput,
  quote: CheckoutQuote,
  now: Date,
  idempotencyKey: string,
): MockPlacedSession {
  const db = placedStore.get();
  const sid = 8800 + db.sessions.length + 1;
  const firstOrderId = 200 + db.orders.length + 1;
  const status: MockPlacedSession['status'] =
    input.paymentMethod === 'COD' ? 'COMPLETED' : 'PAYMENT_PENDING';
  const orders: MockPlacedOrder[] = quote.orders.map((o, i) => ({
    id: firstOrderId + i,
    code: orderCode(now, firstOrderId + i),
    session_id: sid,
    shop: o.shop,
    status: input.paymentMethod === 'COD' ? 'PAID' : 'PENDING_PAYMENT',
    payment_method: input.paymentMethod,
    placed_at: now.toISOString(),
    items: o.items.map((it) => ({
      cart_item_id: it.cartItemId,
      product_id: it.productId,
      variant_id: it.variantId,
      product_slug: it.productSlug,
      product_name: it.title,
      image_url: it.image,
      variant_name: it.variantLabel,
      unit_price: it.price,
      quantity: it.qty,
    })),
    subtotal: o.subtotal,
    shipping_fee: o.shipping,
    discount: o.discount,
    grand_total: o.total,
    note: input.notes.find((n) => n.shopId === o.shop.id)?.note ?? null,
    address: {
      recipient_name: quote.address.recipient,
      phone_number: quote.address.phone,
      full_address: quote.address.full,
    },
  }));
  const session: MockPlacedSession = {
    id: sid,
    code: `CS-${sid.toString(36).toUpperCase()}`,
    status,
    payment_method: input.paymentMethod,
    created_at: now.toISOString(),
    reservation_expires_at: new Date(holdDeadline(now.getTime())).toISOString(),
    order_ids: orders.map((o) => o.id),
    cart_item_ids: input.cartItemIds,
    coin_discount: quote.summary.coinDiscount,
    idempotency_key: idempotencyKey,
    summary: {
      subtotal: quote.summary.subtotal,
      shipping_total: quote.summary.shippingTotal,
      discount_total: quote.summary.discountTotal,
      coin_discount: quote.summary.coinDiscount,
      grand_total: quote.summary.grandTotal,
    },
  };
  placedStore.set({ sessions: [...db.sessions, session], orders: [...db.orders, ...orders] });
  return session;
}

/** Cổng thanh toán mô phỏng trả kết quả: thành công → đơn PAID; thất bại → phiên FAILED, giữ chỗ còn. */
export function mockSettle(sessionId: number, ok: boolean): MockPlacedSession | null {
  const db = placedStore.get();
  const s = db.sessions.find((x) => x.id === sessionId);
  if (!s) return null;
  const next: MockPlacedSession = { ...s, status: ok ? 'COMPLETED' : 'FAILED' };
  placedStore.set({
    sessions: db.sessions.map((x) => (x.id === sessionId ? next : x)),
    orders: db.orders.map((o) => (o.session_id === sessionId && ok ? { ...o, status: 'PAID' } : o)),
  });
  return next;
}

/** Thanh toán lại / đổi phương thức (6.12). */
export function mockRepay(sessionId: number, method: MockPlacedSession['payment_method']) {
  const db = placedStore.get();
  const s = db.sessions.find((x) => x.id === sessionId);
  if (!s) return null;
  const next: MockPlacedSession = {
    ...s,
    payment_method: method,
    updated_at: new Date().toISOString(),
    status: method === 'COD' ? 'COMPLETED' : 'PAYMENT_PENDING',
  };
  placedStore.set({
    sessions: db.sessions.map((x) => (x.id === sessionId ? next : x)),
    orders: db.orders.map((o) =>
      o.session_id === sessionId
        ? { ...o, payment_method: method, status: method === 'COD' ? 'PAID' : o.status }
        : o,
    ),
  });
  return next;
}
