// Xử lý đơn phía shop (API_SPEC 10.3): chi tiết, xác nhận, từ chối, chuẩn bị, báo giá hãng, bàn giao ĐVVC,
// phiếu gửi hàng. Mock ghi vào kho dùng chung để Tổng quan, huy hiệu nav, tìm nhanh đổi theo.
import { api } from '@/shared/api/http';
import { newIdempotencyKey } from '@/shared/api/idempotency';
import { mockDelay, mockFail, USE_MOCK } from '@/shared/api/mock';
import { sellerOrdersStore, type MockSellerOrder } from '@/shared/api/mock/seller';
import type { RejectReason, ShippingQuote } from '../domain/fulfillment.rules';
import type { Carrier, SellerOrder } from '../domain/order.types';
import type { SellerOrderDto } from './order.dto';
import { toSellerOrder } from './order.mapper';

export interface SellerOrderDetail extends SellerOrder {
  address: { recipient: string; phone: string; full: string };
}

interface DetailDto extends SellerOrderDto {
  shipping_address: { recipient_name: string; phone_number: string; full_address: string };
}

export interface ShippingLabel {
  carrier: Carrier;
  trackingCode: string;
  recipient: string;
  shipTo: string;
  itemCount: number;
  codAmount: number;
  prepaid: boolean;
}

const toDetail = (d: DetailDto): SellerOrderDetail => ({
  ...toSellerOrder(d),
  address: {
    recipient: d.shipping_address.recipient_name,
    phone: d.shipping_address.phone_number,
    full: d.shipping_address.full_address,
  },
});

function patch(id: number, f: (o: MockSellerOrder) => MockSellerOrder): MockSellerOrder {
  const all = sellerOrdersStore.get();
  const cur = all.find((o) => o.id === id);
  if (!cur) mockFail('GNE-ORD-4041');
  const next = f(cur);
  sellerOrdersStore.set(all.map((o) => (o.id === id ? next : o)));
  return next;
}

function need(id: number, status: MockSellerOrder['status']): MockSellerOrder {
  const o = sellerOrdersStore.get().find((x) => x.id === id);
  if (!o) mockFail('GNE-ORD-4041');
  if (o.status !== status) mockFail('GNE-ORD-4092');
  return o;
}

export async function getSellerOrder(id: number): Promise<SellerOrderDetail> {
  if (USE_MOCK) {
    await mockDelay(150);
    const o = sellerOrdersStore.get().find((x) => x.id === id);
    if (!o) mockFail('GNE-ORD-4041');
    return toDetail({
      ...o,
      shipping_address: {
        recipient_name: o.customer.name,
        phone_number: '09******' + String(o.customer.id).slice(-2),
        full_address: o.ship_to,
      },
    });
  }
  return toDetail(await api.get<DetailDto>(`/seller/orders/${id}`));
}

export async function confirmOrder(id: number): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(250);
    need(id, 'NEW');
    patch(id, (o) => ({ ...o, status: 'CONFIRMED' }));
    return;
  }
  await api.post(`/seller/orders/${id}/confirm`);
}

export async function rejectOrder(
  id: number,
  body: { reason: RejectReason; note: string | null; label: string },
): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(300);
    need(id, 'NEW');
    patch(id, (o) => ({
      ...o,
      status: 'CANCELLED',
      reject_reason: body.reason === 'OTHER' ? (body.note ?? '') : body.label,
      shipment: null,
    }));
    return;
  }
  await api.post(`/seller/orders/${id}/reject`, { reason_code: body.reason, note: body.note });
}

export async function prepareOrder(id: number): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(250);
    need(id, 'CONFIRMED');
    patch(id, (o) => ({ ...o, status: 'PREPARING' }));
    return;
  }
  await api.post(`/seller/orders/${id}/prepare`);
}

interface QuoteDto {
  carrier: Carrier;
  fee: number;
  estimated_days: number;
  is_selected: boolean;
}

export async function getShippingQuotes(id: number): Promise<ShippingQuote[]> {
  const map = (q: QuoteDto): ShippingQuote => ({
    carrier: q.carrier,
    fee: q.fee,
    estimatedDays: q.estimated_days,
    selected: q.is_selected,
  });
  if (USE_MOCK) {
    await mockDelay(300);
    // Hãng shop đã bật (Cài đặt → Vận chuyển: GHN, GHTK); hệ thống chọn phí thấp nhất
    return [
      { carrier: 'GHN', fee: 24000, estimated_days: 2, is_selected: false },
      { carrier: 'GHTK', fee: 22000, estimated_days: 3, is_selected: true },
    ].map((q) => map(q as QuoteDto));
  }
  return (await api.get<QuoteDto[]>(`/seller/orders/${id}/shipping-quotes`)).map(map);
}

/** 🔁 Bàn giao ĐVVC — một khóa idempotency cho mỗi lần bấm. */
export async function createShipment(
  id: number,
): Promise<{ carrier: Carrier; trackingCode: string }> {
  if (USE_MOCK) {
    await mockDelay(500);
    need(id, 'PREPARING');
    const trackingCode = `VN${931042800 + id}`;
    patch(id, (o) => ({
      ...o,
      status: 'DELIVERING',
      shipment: {
        carrier: 'GHTK',
        tracking_code: trackingCode,
        status: 'AWAITING_PICKUP',
        carrier_status_text: 'Chờ lấy hàng',
      },
    }));
    return { carrier: 'GHTK', trackingCode };
  }
  const d = await api.post<{ carrier: Carrier; tracking_code: string }>(
    `/seller/orders/${id}/shipments`,
    undefined,
    { idempotencyKey: newIdempotencyKey() },
  );
  return { carrier: d.carrier, trackingCode: d.tracking_code };
}

interface LabelDto {
  carrier: Carrier;
  tracking_code: string;
  recipient_name: string;
  ship_to: string;
  item_count: number;
  cod_amount: number;
  is_prepaid: boolean;
}

export async function getShippingLabel(id: number): Promise<ShippingLabel> {
  const map = (d: LabelDto): ShippingLabel => ({
    carrier: d.carrier,
    trackingCode: d.tracking_code,
    recipient: d.recipient_name,
    shipTo: d.ship_to,
    itemCount: d.item_count,
    codAmount: d.cod_amount,
    prepaid: d.is_prepaid,
  });
  if (USE_MOCK) {
    await mockDelay(150);
    const o = sellerOrdersStore.get().find((x) => x.id === id);
    if (!o?.shipment?.tracking_code) mockFail('GNE-ORD-4041');
    return map({
      carrier: o.shipment.carrier,
      tracking_code: o.shipment.tracking_code,
      recipient_name: o.customer.name,
      ship_to: o.ship_to,
      item_count: o.items.reduce((n, i) => n + i.quantity, 0),
      cod_amount: o.payment_method === 'COD' ? o.grand_total : 0,
      is_prepaid: o.payment_method !== 'COD',
    });
  }
  return map(await api.get<LabelDto>(`/seller/orders/${id}/shipping-label`));
}
