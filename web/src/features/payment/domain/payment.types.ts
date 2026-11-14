// Kiểu hiển thị của luồng thanh toán (plan G10.23–G10.26). Feature payment chỉ vẽ; tạo phiên / gọi cổng
// do checkout làm và truyền dữ liệu vào (luật phụ thuộc chéo feature — TS 5.2).

export type PayMethod = 'VNPAY' | 'VIETQR' | 'COD';

/** Thông tin chuyển khoản VietQR ($CheckoutSession.payment.vietqr). */
export interface VietQrView {
  bankName: string;
  accountNumber: string;
  accountName: string;
  amount: number;
  transferContent: string;
  qrPayload: string;
  expiresAt: string;
}

/** Trạng thái đơn để chọn nhãn "Genie giữ tiền" (prototype MoneyChip). */
export type MoneyChipTone = 'hold' | 'done' | 'muted';

export function moneyChip(
  status: string,
  method: PayMethod,
): { tone: MoneyChipTone; key: string } | null {
  if (status === 'PENDING_PAYMENT' || status.startsWith('CANCELLED') || status === 'AUTO_CANCELLED')
    return null;
  if (status === 'REFUNDED') return null;
  if (method === 'COD' && status !== 'COMPLETED') return { tone: 'muted', key: 'payment.chip.cod' };
  if (status === 'COMPLETED') return { tone: 'done', key: 'payment.chip.released' };
  if (status === 'RETURN_REQUESTED') return { tone: 'hold', key: 'payment.chip.return' };
  return { tone: 'hold', key: 'payment.chip.holding' };
}
