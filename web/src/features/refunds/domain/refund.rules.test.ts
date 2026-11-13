import { describe, expect, it } from 'vitest';
import { evidenceMissing, isOpenRefund, maskBank, needsBank, refundEstimate } from './refund.rules';
import { refundBankSchema } from './refund.schema';

describe('refund.rules', () => {
  it('tiền hoàn theo đơn giá đã trả, nếu không có thì chia giảm giá theo tỉ lệ', () => {
    expect(
      refundEstimate([{ price: 389000, paidUnitPrice: 360000, qty: 1 }], {
        subtotal: 848000,
        discountTotal: 60000,
      }),
    ).toBe(360000);
    // 413.000 giảm 30.000 → tỉ lệ trả 383/413
    expect(
      refundEstimate([{ price: 178000, paidUnitPrice: null, qty: 1 }], {
        subtotal: 413000,
        discountTotal: 30000,
      }),
    ).toBe(165070);
    expect(
      refundEstimate([{ price: 100, paidUnitPrice: null, qty: 2 }], {
        subtotal: 0,
        discountTotal: 0,
      }),
    ).toBe(200);
  });

  it('bằng chứng tối thiểu 1 ảnh + 1 video', () => {
    expect(evidenceMissing([])).toEqual(['IMAGE', 'VIDEO']);
    expect(evidenceMissing([{ mediaType: 'IMAGE' }])).toEqual(['VIDEO']);
    expect(evidenceMissing([{ mediaType: 'IMAGE' }, { mediaType: 'VIDEO' }])).toEqual([]);
  });

  it('tài khoản hoàn tiền cho đơn không phải VNPAY', () => {
    expect(needsBank('VNPAY')).toBe(false);
    expect(needsBank('COD')).toBe(true);
    expect(maskBank('Vietcombank', '0071000006712')).toBe('Vietcombank ••6712');
    expect(isOpenRefund('REQUESTED')).toBe(true);
    expect(isOpenRefund('REFUNDED')).toBe(false);
    expect(
      refundBankSchema.safeParse({ code: 'VCB', accountNumber: '12a', holder: 'A' }).success,
    ).toBe(false);
    expect(
      refundBankSchema.safeParse({
        code: 'VCB',
        accountNumber: '0071000006712',
        holder: 'TRAN THI MAI',
      }).success,
    ).toBe(true);
  });
});
