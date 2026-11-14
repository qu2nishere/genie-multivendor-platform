import { describe, expect, it } from 'vitest';
import { allowedActions, codAmount, pickQuote } from './fulfillment.rules';
import { rejectSchema } from './fulfillment.schema';
import { NEXT_STATUS, refundActions, refundHoursLeft } from './refund.rules';

describe('fulfillment.rules', () => {
  it('thao tác theo trạng thái (chuyển NEW → CONFIRMED → PREPARING → giao hãng)', () => {
    expect(allowedActions({ status: 'NEW', shipment: null })).toEqual(['CONFIRM', 'REJECT']);
    expect(allowedActions({ status: 'CONFIRMED', shipment: null })).toEqual(['PREPARE']);
    expect(allowedActions({ status: 'PREPARING', shipment: null })).toEqual(['HAND_OVER']);
    expect(
      allowedActions({
        status: 'DELIVERING',
        shipment: { carrier: 'GHN', trackingCode: 'VN1', status: 'IN_TRANSIT', statusText: '' },
      }),
    ).toEqual(['PRINT_LABEL']);
    expect(allowedActions({ status: 'CANCELLED', shipment: null })).toEqual([]);
  });

  it('chọn hãng: theo đánh dấu, không có thì rẻ nhất', () => {
    const q = [
      { carrier: 'GHN' as const, fee: 24000, estimatedDays: 2, selected: false },
      { carrier: 'GHTK' as const, fee: 22000, estimatedDays: 3, selected: false },
    ];
    expect(pickQuote(q)?.carrier).toBe('GHTK');
    expect(pickQuote([{ ...q[0], selected: true }, q[1]])?.carrier).toBe('GHN');
    expect(pickQuote([])).toBeNull();
  });

  it('thu hộ chỉ với COD', () => {
    expect(codAmount({ payment: 'COD', total: 484000 })).toBe(484000);
    expect(codAmount({ payment: 'VNPAY', total: 414000 })).toBe(0);
  });

  it('từ chối: "Lý do khác" bắt buộc ghi chú', () => {
    expect(rejectSchema.safeParse({ reason: 'OUT_OF_STOCK', note: '' }).success).toBe(true);
    const r = rejectSchema.safeParse({ reason: 'OTHER', note: '  ' });
    expect(r.success).toBe(false);
    expect(r.success ? '' : r.error.issues[0].message).toBe('seller-orders.reject.err.note');
  });
});

describe('refund.rules', () => {
  it('nút theo trạng thái + phương án', () => {
    expect(
      refundActions({ status: 'REQUESTED', type: 'RETURN_AND_REFUND', evidenceRequested: false }),
    ).toEqual(['REQUEST_EVIDENCE', 'REJECT', 'APPROVE_WITH_RETURN', 'APPROVE_REFUND_ONLY']);
    expect(
      refundActions({ status: 'REQUESTED', type: 'REFUND_ONLY', evidenceRequested: true }),
    ).toEqual(['REJECT', 'APPROVE_REFUND_ONLY']);
    expect(
      refundActions({
        status: 'AWAITING_RETURN_SHIPMENT',
        type: 'RETURN_AND_REFUND',
        evidenceRequested: false,
      }),
    ).toEqual(['CONFIRM_RETURN_RECEIVED']);
    expect(
      refundActions({ status: 'SHOP_REJECTED', type: 'REFUND_ONLY', evidenceRequested: false }),
    ).toEqual(['ESCALATE']);
    expect(NEXT_STATUS.APPROVE_WITH_RETURN).toBe('AWAITING_RETURN_SHIPMENT');
  });
  it('giờ còn lại', () => {
    const now = Date.parse('2026-06-08T18:00:00+07:00');
    expect(refundHoursLeft('2026-06-10T01:00:00+07:00', now)).toBe(31);
    expect(refundHoursLeft(null, now)).toBeNull();
  });
});
