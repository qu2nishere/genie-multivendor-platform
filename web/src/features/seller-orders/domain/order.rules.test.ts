import { describe, expect, it } from 'vitest';
import { inTab, itemCount, orderBadge, whenLabel, workCounts } from './order.rules';
import type { SellerOrder } from './order.types';

const ship = (status: NonNullable<SellerOrder['shipment']>['status']): SellerOrder['shipment'] => ({
  carrier: 'GHN',
  trackingCode: 'VN1',
  status,
  statusText: '',
});

describe('order.rules', () => {
  it('huy hiệu: trạng thái đơn trước, vận đơn sau', () => {
    expect(orderBadge({ status: 'NEW', shipment: null })).toEqual({
      tone: 'PENDING',
      key: 'seller-orders.status.NEW',
    });
    expect(orderBadge({ status: 'PREPARING', shipment: null }).tone).toBe('PAID');
    expect(orderBadge({ status: 'DELIVERING', shipment: ship('IN_TRANSIT') })).toEqual({
      tone: 'SHIPPED',
      key: 'seller-orders.shipment.IN_TRANSIT',
    });
    expect(orderBadge({ status: 'DELIVERING', shipment: ship('DELIVERY_FAILED') }).tone).toBe(
      'ERROR',
    );
    expect(orderBadge({ status: 'DELIVERED', shipment: null }).tone).toBe('DELIVERED');
  });

  it('tab gom CONFIRMED + PREPARING vào Chờ chuẩn bị', () => {
    expect(inTab({ status: 'CONFIRMED' }, 'TO_PREPARE')).toBe(true);
    expect(inTab({ status: 'PREPARING' }, 'TO_PREPARE')).toBe(true);
    expect(inTab({ status: 'NEW' }, 'TO_PREPARE')).toBe(false);
    expect(inTab({ status: 'CANCELLED' }, 'ALL')).toBe(true);
  });

  it('đếm việc + số món', () => {
    const c = workCounts([
      { status: 'NEW', shipment: null },
      { status: 'NEW', shipment: null },
      { status: 'CONFIRMED', shipment: null },
      { status: 'PREPARING', shipment: null },
      { status: 'DELIVERING', shipment: ship('DELIVERY_FAILED') },
    ]);
    expect(c).toEqual({ toConfirm: 2, toPrepare: 2, shipping: 1, failed: 1 });
    expect(
      itemCount({
        items: [
          { name: 'a', variant: '', quantity: 2, unitPrice: 1 },
          { name: 'b', variant: '', quantity: 1, unitPrice: 1 },
        ],
      }),
    ).toBe(3);
  });

  it('mốc thời gian theo giờ Việt Nam', () => {
    const now = Date.parse('2026-06-08T18:00:00+07:00');
    expect(whenLabel('2026-06-08T15:05:00+07:00', now)).toEqual({ kind: 'today', time: '15:05' });
    expect(whenLabel('2026-06-08T00:30:00+07:00', now)).toEqual({ kind: 'today', time: '00:30' });
    expect(whenLabel('2026-06-07T23:59:00+07:00', now)).toEqual({ kind: 'yesterday' });
    expect(whenLabel('2026-06-06T10:00:00+07:00', now)).toEqual({ kind: 'days', days: 2 });
    expect(whenLabel('2026-05-20T10:00:00+07:00', now)).toEqual({ kind: 'date' });
  });
});
