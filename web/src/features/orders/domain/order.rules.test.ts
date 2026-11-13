import { describe, expect, it } from 'vitest';
import {
  allowedActions,
  countByGroup,
  groupOrders,
  statusGroupOf,
  statusTone,
  stepOf,
  sumOrders,
} from './order.rules';
import type { OrderSummary } from './order.types';

const o = (over: Partial<OrderSummary> = {}): OrderSummary => ({
  id: 1,
  code: 'TR-20260608-000001',
  sessionId: null,
  status: 'DELIVERING',
  paymentMethod: 'VNPAY',
  shop: { id: 1, name: 'TechZone', authentic: true },
  items: [],
  subtotal: 100,
  shippingFee: 10,
  discountTotal: 5,
  grandTotal: 105,
  paymentExpiresAt: null,
  deliveredAt: null,
  reviewed: false,
  actions: [],
  ...over,
});

describe('order.rules', () => {
  it('ánh xạ tab theo 16.3', () => {
    expect(statusGroupOf(o({ status: 'PENDING_PAYMENT' }))).toBe('TO_PAY');
    expect(statusGroupOf(o({ status: 'PREPARING' }))).toBe('SHIPPING');
    expect(statusGroupOf(o({ status: 'DELIVERED' }))).toBe('TO_RECEIVE');
    expect(
      statusGroupOf(o({ status: 'PAID', paymentMethod: 'COD', deliveredAt: '2026-06-01' })),
    ).toBe('TO_RECEIVE');
    expect(statusGroupOf(o({ status: 'PAID' }))).toBe('SHIPPING');
    expect(statusGroupOf(o({ status: 'REFUNDED' }))).toBe('RETURN');
    expect(statusGroupOf(o({ status: 'AUTO_CANCELLED' }))).toBe('CANCELLED');
    const c = countByGroup([o(), o({ status: 'COMPLETED' }), o({ status: 'CANCELLED' })]);
    expect(c).toMatchObject({ SHIPPING: 1, COMPLETED: 1, CANCELLED: 1, TO_PAY: 0 });
  });

  it('bước tiến trình và tông màu', () => {
    expect(stepOf('PENDING_PAYMENT')).toBe(0);
    expect(stepOf('DELIVERING')).toBe(4);
    expect(stepOf('COMPLETED')).toBe(6);
    expect(stepOf('RETURN_REQUESTED')).toBe(5);
    expect(statusTone('PREPARING')).toBe('PENDING');
    expect(statusTone('REFUNDED')).toBe('PENDING');
    expect(statusTone('DELIVERING')).toBe('SHIPPED');
    expect(statusTone('AUTO_CANCELLED')).toBe('CANCELLED');
  });

  it('nút hợp lệ theo trạng thái', () => {
    const a = (over: Partial<Parameters<typeof allowedActions>[0]>) =>
      allowedActions({
        status: 'PAID',
        paymentMethod: 'VNPAY',
        deliveredAt: null,
        reviewed: false,
        ...over,
      });
    expect(a({ status: 'PENDING_PAYMENT' })).toEqual(['PAY', 'CANCEL']);
    expect(a({ status: 'PENDING_PAYMENT', paymentMethod: 'COD' })).toEqual(['CANCEL']);
    expect(a({ status: 'PREPARING' })).toContain('CANCEL');
    expect(a({ status: 'READY_FOR_PICKUP' })).not.toContain('CANCEL');
    expect(a({ status: 'DELIVERED' })).toEqual([
      'CONFIRM_RECEIPT',
      'REQUEST_RETURN',
      'TRACK',
      'CHAT',
    ]);
    expect(a({ status: 'DELIVERED', returnWindowClosed: true })).not.toContain('REQUEST_RETURN');
    expect(a({ status: 'COMPLETED' })).toEqual(['REVIEW', 'BUY_AGAIN', 'CHAT']);
    expect(a({ status: 'COMPLETED', reviewed: true })).toEqual(['BUY_AGAIN', 'CHAT']);
    expect(a({ status: 'RETURN_REQUESTED' })).toEqual(['VIEW_RETURN', 'CHAT']);
    expect(a({ status: 'CANCELLED' })).toEqual(['BUY_AGAIN', 'CHAT']);
  });

  it('gộp nhóm đơn cùng phiên chờ thanh toán', () => {
    const rows = groupOrders([
      o({ id: 1 }),
      o({ id: 2, sessionId: 9, status: 'PENDING_PAYMENT' }),
      o({ id: 3, sessionId: 9, status: 'PENDING_PAYMENT' }),
      o({ id: 4, sessionId: 9, status: 'PENDING_PAYMENT', paymentMethod: 'COD' }),
      o({ id: 5, sessionId: 7, status: 'CANCELLED' }),
    ]);
    expect(rows.map((r) => r.kind)).toEqual(['single', 'group', 'single', 'single']);
    const g = rows[1];
    expect(g.kind === 'group' && g.orders.map((x) => x.id)).toEqual([2, 3]);
    expect(sumOrders([o(), o()], 'grandTotal')).toBe(210);
  });
});
