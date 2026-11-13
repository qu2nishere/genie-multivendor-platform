// Đặt hàng & theo dõi phiên thanh toán (plan G10.17, G10.24–G10.29).
import { useMutation, useQuery } from '@tanstack/react-query';
import {
  checkoutIdempotencyKey,
  clearCheckoutIdempotencyKey,
  newIdempotencyKey,
} from '@/shared/api/idempotency';
import type { CheckoutQuote, PaymentMethod, PlaceOrderInput } from '../domain/checkout.types';
import {
  createCheckout,
  getCheckoutSession,
  retryPayment,
  settleMockPayment,
} from '../infrastructure/checkout.api';
import { clearDraft } from '../infrastructure/draft.storage';

/**
 * Đặt hàng: dùng lại Idempotency-Key của lần đặt dở (bấm lại / tải lại không tạo đơn trùng); thành
 * công thì xóa key và nháp checkout.
 */
export function usePlaceOrder() {
  return useMutation({
    mutationFn: ({ input, quote }: { input: PlaceOrderInput; quote: CheckoutQuote }) =>
      createCheckout(input, checkoutIdempotencyKey(), quote),
    onSuccess: () => {
      clearCheckoutIdempotencyKey();
      clearDraft();
    },
  });
}

/** Phiên thanh toán; `poll` = hỏi lại mỗi 3 giây khi còn chờ (VietQR chờ SePay báo nhận tiền). */
export function useCheckoutSession(id: number | null, poll = false) {
  return useQuery({
    queryKey: ['checkout', 'session', id],
    queryFn: () => getCheckoutSession(id ?? 0),
    enabled: id != null,
    refetchInterval: (q) => (poll && q.state.data?.status === 'PAYMENT_PENDING' ? 3000 : false),
  });
}

export function useRetryPayment() {
  return useMutation({
    mutationFn: ({ id, method }: { id: number; method: PaymentMethod }) =>
      retryPayment(id, method, newIdempotencyKey()),
  });
}

/** Chỉ mock: cổng giả lập báo kết quả thanh toán. */
export function useSettleMockPayment() {
  return useMutation({
    mutationFn: ({ id, ok }: { id: number; ok: boolean }) => settleMockPayment(id, ok),
  });
}
