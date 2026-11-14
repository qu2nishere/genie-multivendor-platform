import { describe, expect, it } from 'vitest';
import {
  holdDeadline,
  isEscrowed,
  orderCode,
  outOfStockIds,
  placeLabelKey,
  resultView,
  secondsLeft,
} from './checkout.rules';

describe('checkout.rules', () => {
  it('giữ chỗ 15 phút, đếm ngược không âm', () => {
    const start = Date.parse('2026-11-13T08:00:00Z');
    const end = holdDeadline(start);
    expect(secondsLeft(end, start)).toBe(900);
    expect(secondsLeft(end, start + 62_500)).toBe(838);
    expect(secondsLeft(end, end + 5000)).toBe(0);
  });

  it('nhãn nút đặt hàng và giữ tiền theo phương thức', () => {
    expect(placeLabelKey('VNPAY')).toBe('checkout.place.vnpay');
    expect(placeLabelKey('VIETQR')).toBe('checkout.place.vietqr');
    expect(placeLabelKey('COD')).toBe('checkout.place.cod');
    expect(isEscrowed('COD')).toBe(false);
    expect(isEscrowed('VIETQR')).toBe(true);
  });

  it('đọc dòng hết hàng từ lỗi GNE-INV-4090', () => {
    expect(outOfStockIds({ out_of_stock_cart_item_ids: [7001, 'x', 7002] })).toEqual([7001, 7002]);
    expect(outOfStockIds(null)).toEqual([]);
    expect(outOfStockIds({})).toEqual([]);
  });

  it('trang kết quả chỉ đọc trạng thái phiên', () => {
    expect(resultView({ status: 'COMPLETED', paymentMethod: 'VNPAY' })).toBe('success');
    expect(resultView({ status: 'FAILED', paymentMethod: 'VNPAY' })).toBe('failed');
    expect(resultView({ status: 'EXPIRED', paymentMethod: 'VIETQR' })).toBe('failed');
    expect(resultView({ status: 'PAYMENT_PENDING', paymentMethod: 'VIETQR' })).toBe('pending');
  });

  it('mã đơn theo ngày Việt Nam', () => {
    expect(orderCode(new Date('2026-06-08T18:30:00Z'), 201)).toBe('TR-20260609-000201');
    expect(orderCode(new Date('2026-06-08T07:22:00Z'), 44)).toBe('TR-20260608-000044');
  });
});
