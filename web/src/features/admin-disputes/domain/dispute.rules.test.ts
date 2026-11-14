import { describe, expect, it } from 'vitest';
import {
  countByStatus,
  disputeActions,
  refundActions,
  refundTransferContent,
  statusAfter,
} from './dispute.rules';
import { freezeSchema, resolveSchema } from './dispute.schema';
import type { GatewayRefund } from './dispute.types';

const refund = (over: Partial<GatewayRefund>): GatewayRefund => ({
  id: 1,
  gateway: 'VNPAY',
  originalTransactionId: 'TXN-1',
  gatewayRefundId: 'VNP-RF-1',
  amount: 100000,
  status: 'PENDING',
  createdAt: '2026-06-20T14:32:10+07:00',
  manualTransfer: null,
  ...over,
});

describe('dispute.rules', () => {
  it('trạng thái sau phán quyết', () => {
    expect(statusAfter('REFUND_NOW')).toBe('REFUNDED');
    expect(statusAfter('CARRIER_FAULT')).toBe('REFUNDED');
    expect(statusAfter('RETURN_THEN_REFUND')).toBe('AWAITING_RETURN');
    expect(statusAfter('DENY')).toBe('DENIED');
  });
  it('thao tác theo trạng thái + phong tỏa', () => {
    expect(disputeActions({ status: 'OPEN', escrow: { id: 1, status: 'DISPUTED' } })).toEqual([
      'resolve',
      'freeze',
    ]);
    expect(disputeActions({ status: 'OPEN', escrow: { id: 1, status: 'FROZEN' } })).toEqual([
      'resolve',
      'unfreeze',
    ]);
    expect(
      disputeActions({ status: 'AWAITING_RETURN', escrow: { id: 1, status: 'DISPUTED' } }),
    ).toEqual(['confirm_return']);
    expect(disputeActions({ status: 'REFUNDED', escrow: { id: 1, status: 'REFUNDED' } })).toEqual(
      [],
    );
    expect(disputeActions({ status: 'DENIED', escrow: { id: 1, status: 'HELD' } })).toEqual([
      'reopen',
    ]);
  });
  it('khối hoàn tiền: VNPay đồng bộ / thử lại, chuyển khoản tay', () => {
    expect(refundActions(refund({}))).toEqual(['sync']);
    expect(refundActions(refund({ status: 'FAILED' }))).toEqual(['retry']);
    expect(refundActions(refund({ status: 'SUCCESS' }))).toEqual([]);
    const manual = refund({
      gateway: null,
      gatewayRefundId: null,
      manualTransfer: { toAccount: 'BIDV ••0932', transferContent: 'TRRF0230' },
    });
    expect(refundActions(manual)).toEqual(['mark_transferred']);
    expect(refundActions({ ...manual, status: 'SUCCESS' })).toEqual([]);
    expect(refundTransferContent('RFN-0231')).toBe('TRRF0231');
  });
  it('đếm theo trạng thái', () => {
    expect(countByStatus([{ status: 'OPEN' }, { status: 'OPEN' }, { status: 'REFUNDED' }])).toEqual(
      {
        OPEN: 2,
        AWAITING_RETURN: 0,
        REFUNDED: 1,
        DENIED: 0,
      },
    );
  });
  it('biên bản + lý do phong tỏa bắt buộc', () => {
    expect(resolveSchema.safeParse({ outcome: 'DENY', verdict: '  ' }).success).toBe(false);
    expect(
      resolveSchema.safeParse({ outcome: 'DENY', verdict: 'Ảnh không chứng minh' }).success,
    ).toBe(true);
    expect(freezeSchema.safeParse({ reason: '' }).success).toBe(false);
  });
});
