// Bước thanh toán sau khi có phiên đặt hàng (plan G10.24–G10.26): VNPAY → cổng (mock: cổng giả lập),
// VietQR → mã QR + hỏi lại phiên mỗi 3 giây tới khi SePay xác nhận, COD → kết quả ngay. "Đổi phương thức"
// / "Thử thanh toán lại" → chọn phương thức rồi gọi 6.12. Xong → /checkout/result (chỉ hiển thị).
import { useEffect, useState } from 'react';
import { PayGateway, PayModal } from '@/features/payment';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { toast } from '@/shared/ui';
import {
  MOCK_VNPAY_URL,
  useCheckoutSession,
  useRetryPayment,
  useSettleMockPayment,
} from '../application/useCheckout';
import type { CheckoutSession, PaymentMethod } from '../domain/checkout.types';

export const resultHref = (id: number) => `/checkout/result?session=${id}`;

export interface PaymentFlowProps {
  session: CheckoutSession;
  /** Mở thẳng bước chọn phương thức (trang kết quả: Thử lại / Đổi phương thức). */
  pick?: boolean;
  onClose: () => void;
}

export function PaymentFlow({ session: placed, pick = false, onClose }: PaymentFlowProps) {
  const { t } = useT();
  const [session, setSession] = useState(placed);
  const [picking, setPicking] = useState(pick);
  const retry = useRetryPayment();
  const settle = useSettleMockPayment();
  const pay = session.payment;
  const waitingQr = !picking && pay?.method === 'VIETQR';
  const poll = useCheckoutSession(waitingQr ? session.id : null, true);
  const go = () => window.location.assign(resultHref(session.id));

  // Bắt đầu theo phương thức của phiên
  useEffect(() => {
    if (picking || !pay) return;
    if (pay.method === 'COD') go();
    else if (pay.method === 'VNPAY' && pay.paymentUrl !== MOCK_VNPAY_URL)
      window.location.assign(pay.paymentUrl);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pay, picking]);

  // VietQR: SePay báo đã nhận tiền → sang trang kết quả
  useEffect(() => {
    if (poll.data && poll.data.status !== 'PAYMENT_PENDING') go();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [poll.data]);

  const choose = (m: PaymentMethod) =>
    retry.mutate(
      { id: session.id, method: m },
      {
        onSuccess: (s) => {
          setSession(s);
          setPicking(false);
        },
        onError: (e) => toast.error(errorMessage(e, t)),
      },
    );

  if (!picking && pay?.method === 'VNPAY' && pay.paymentUrl === MOCK_VNPAY_URL)
    return (
      <PayGateway
        amount={session.summary.grandTotal}
        orders={session.orders.length}
        onPay={() => settle.mutate({ id: session.id, ok: true }, { onSettled: go })}
        // Thoát cổng = VNPAY trả về "khách hủy giao dịch" → trang kết quả thất bại
        onCancel={() => settle.mutate({ id: session.id, ok: false }, { onSettled: go })}
      />
    );

  return (
    <PayModal
      open={picking || waitingQr}
      amount={session.summary.grandTotal}
      qr={waitingQr && pay?.method === 'VIETQR' ? pay.vietqr : null}
      initial={session.paymentMethod}
      busy={retry.isPending}
      onPick={choose}
      onBackFromQr={() => setPicking(true)}
      onClose={onClose}
    />
  );
}
