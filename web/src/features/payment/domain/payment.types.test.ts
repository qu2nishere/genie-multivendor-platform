import { describe, expect, it } from 'vitest';
import { moneyChip } from './payment.types';

describe('moneyChip', () => {
  it('không hiện khi chờ thanh toán / đã hủy', () => {
    expect(moneyChip('PENDING_PAYMENT', 'VNPAY')).toBeNull();
    expect(moneyChip('CANCELLED_BY_VENDOR', 'VNPAY')).toBeNull();
    expect(moneyChip('AUTO_CANCELLED', 'VIETQR')).toBeNull();
  });
  it('COD chưa xong → trả khi nhận; xong → đã chuyển cho shop', () => {
    expect(moneyChip('DELIVERING', 'COD')).toEqual({ tone: 'muted', key: 'payment.chip.cod' });
    expect(moneyChip('COMPLETED', 'COD')).toEqual({ tone: 'done', key: 'payment.chip.released' });
  });
  it('đang giữ tiền / trả hàng', () => {
    expect(moneyChip('DELIVERING', 'VNPAY')?.key).toBe('payment.chip.holding');
    expect(moneyChip('RETURN_REQUESTED', 'VIETQR')?.key).toBe('payment.chip.return');
  });
});
