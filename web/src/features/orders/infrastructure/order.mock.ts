// Đơn mua mẫu (prototype data.jsx ORDERS — 11 đơn, đủ mọi trạng thái) dạng DTO 7.2, cộng đơn vừa đặt ở
// checkout (kho mock dùng chung `placed`). Mã cũ: "TR-20260608-000044" → id 44; shop techzone → 1…
import type { MockPlacedOrder } from '@/shared/api/mock/placed';
import { SEED_PRODUCTS, SEED_SHOPS } from '@/shared/api/mock/seed';
import { productImg } from '@/shared/lib/assets';
import { allowedActions } from '../domain/order.rules';
import type { OrderStatus, PaymentMethod } from '../domain/order.types';
import type { OrderDetailDto, OrderItemDto, ShipmentDto } from './order.dto';

const ADDRESS = {
  recipient_name: 'Trần Thị Mai',
  phone_number: '0901234567',
  full_address: '25 Nguyễn Huệ, Căn hộ 12A, P. Bến Nghé, Quận 1, TP. Hồ Chí Minh',
};
/** Phiên chờ thanh toán của nhóm đơn mẫu (prototype CS-8F21A). */
export const SEED_PENDING_SESSION = 8811;

const shop = (id: number) => {
  const s = SEED_SHOPS.find((x) => x.id === id);
  if (!s) throw new Error(`seed thiếu shop ${id}`);
  return { id: s.id, name: s.name, is_mall: s.is_authentic };
};

/** SKU mock (khớp product / cart mock: productId * 100 + màu * 10 + phiên bản); nhãn lạ → SKU mặc định. */
const COLORS = ['Đen', 'Trắng', 'Xanh navy', 'Hồng'];
const skuOf = (productId: number, label: string) =>
  productId * 100 + Math.max(0, COLORS.indexOf(label.split(' / ')[0])) * 10;

let itemSeq = 9000;
const item = (productId: number, variant: string, qty: number, price: number): OrderItemDto => {
  const p = SEED_PRODUCTS.find((x) => x.id === productId);
  if (!p) throw new Error(`seed thiếu sản phẩm ${productId}`);
  return {
    id: ++itemSeq,
    product_id: p.id,
    variant_id: skuOf(p.id, variant),
    product_slug: p.slug,
    product_name: p.title,
    image_url: productImg(p.image),
    variant_name: variant,
    quantity: qty,
    unit_price: price,
  };
};

/** "10/06 08:12" (năm 2026, giờ VN) → ISO. */
const at = (s: string): string => {
  const [d, t = '00:00'] = s.split(' ');
  const [dd, mm, yyyy = '2026'] = d.split('/');
  return `${yyyy}-${mm}-${dd}T${t}:00+07:00`;
};
const ev = (status: string, description: string, location: string, when: string) => ({
  status,
  description,
  location,
  occurred_at: at(when),
});

interface Seed {
  id: number;
  shopId: number;
  status: OrderStatus;
  pay: PaymentMethod;
  placed: string;
  items: OrderItemDto[];
  subtotal: number;
  shipping: number;
  discount: number;
  total: number;
  session?: number;
  shipment?: ShipmentDto | null;
  release?: string;
  returnClosed?: boolean;
  refund?: OrderDetailDto['refund_request'];
  cancel?: string;
  expires?: string | null;
}

const build = (s: Seed): OrderDetailDto => {
  const delivered = s.shipment?.delivered_at ?? null;
  const base = {
    status: s.status,
    paymentMethod: s.pay,
    deliveredAt: delivered,
    reviewed: false,
    returnWindowClosed: s.returnClosed ?? false,
  };
  return {
    id: s.id,
    code: `TR-20260608-${String(s.id).padStart(6, '0')}`,
    checkout_session_id: s.session ?? null,
    status: s.status,
    payment_method: s.pay,
    shop: shop(s.shopId),
    items: s.items,
    subtotal: s.subtotal,
    shipping_fee: s.shipping,
    discount_total: s.discount,
    grand_total: s.total,
    payment_expires_at: s.expires ?? null,
    delivered_at: delivered,
    is_reviewed: false,
    allowed_actions: allowedActions(base),
    placed_at: at(s.placed),
    money: {
      subtotal: s.subtotal,
      shipping_fee: s.shipping,
      discount_total: s.discount,
      coin_discount: 0,
      grand_total: s.total,
    },
    shipping_address: ADDRESS,
    shipment: s.shipment ?? null,
    escrow_release_at: s.release ? at(s.release) : null,
    return_window_closed: s.returnClosed ?? false,
    refund_request: s.refund ?? null,
    cancel_reason: s.cancel ?? null,
    note: null,
  };
};

/** Đơn mẫu; hạn thanh toán của nhóm chờ thanh toán tính từ lúc mở (để vẫn bấm "Thanh toán ngay" được). */
export function seedOrders(now = Date.now()): OrderDetailDto[] {
  itemSeq = 9000;
  const expires = new Date(now + 12 * 60_000).toISOString();
  return [
    {
      id: 44,
      shopId: 1,
      status: 'DELIVERING',
      pay: 'VNPAY',
      placed: '08/06/2026 14:22',
      items: [item(1, 'Đen', 1, 389000), item(18, 'Đen RGB', 1, 459000)],
      subtotal: 848000,
      shipping: 25000,
      discount: 60000,
      total: 813000,
      shipment: {
        carrier: 'JNT_EXPRESS',
        tracking_code: 'JT0931042771',
        estimated_delivery_at: at('11/06/2026'),
        delivered_at: null,
        events: [
          ev(
            'IN_TRANSIT',
            'Đang trung chuyển đến kho phân loại HCM',
            'Kho Sorting HCM',
            '10/06 08:12',
          ),
          ev('PICKED_UP', 'ĐVVC đã lấy hàng từ người bán', 'TP. HCM', '09/06 17:40'),
          ev(
            'AWAITING_PICKUP',
            'Người bán đã giao cho ĐVVC, chờ lấy hàng',
            'Kho TechZone, Q.7',
            '09/06 10:05',
          ),
          ev('PREPARING', 'Người bán đang chuẩn bị hàng', 'TechZone Official', '08/06 15:01'),
          ev('PAID', 'Đơn hàng đã thanh toán, Genie đang giữ tiền', 'Genie', '08/06 14:22'),
        ],
      },
    },
    {
      id: 41,
      shopId: 5,
      status: 'DELIVERED',
      pay: 'VIETQR',
      placed: '06/06/2026 09:10',
      items: [item(13, '30ml', 1, 178000), item(12, 'Full set', 1, 235000)],
      subtotal: 413000,
      shipping: 16000,
      discount: 30000,
      total: 399000,
      release: '15/06/2026 11:32',
      shipment: {
        carrier: 'GHTK',
        tracking_code: 'GHTK8841200',
        estimated_delivery_at: at('08/06/2026'),
        delivered_at: at('08/06/2026 11:32'),
        events: [
          ev(
            'DELIVERED',
            'Giao hàng thành công — người nhận đã ký nhận',
            'Q.1, TP. HCM',
            '08/06 11:32',
          ),
          ev('OUT_FOR_DELIVERY', 'Đang giao đến bạn', 'Bưu cục Q.1', '08/06 07:50'),
          ev('IN_TRANSIT', 'Đến kho phân loại', 'Kho Sorting HCM', '07/06 19:20'),
          ev('PICKED_UP', 'ĐVVC đã lấy hàng', 'TP. HCM', '06/06 16:00'),
          ev('PAID', 'Đơn hàng đã thanh toán, Genie đang giữ tiền', 'Genie', '06/06 09:10'),
        ],
      },
    },
    {
      id: 33,
      shopId: 2,
      status: 'COMPLETED',
      pay: 'VNPAY',
      placed: '28/05/2026 20:01',
      items: [item(3, 'Trắng / 42', 2, 259000)],
      subtotal: 518000,
      shipping: 18000,
      discount: 0,
      total: 536000,
      returnClosed: true,
      shipment: {
        carrier: 'VIETTEL_POST',
        tracking_code: 'VTP771903',
        estimated_delivery_at: at('31/05/2026'),
        delivered_at: at('30/05/2026 15:10'),
        events: [
          ev('DELIVERED', 'Giao hàng thành công', 'Q.1, TP. HCM', '30/05 15:10'),
          ev('PAID', 'Đơn hàng đã thanh toán', 'Genie', '28/05 20:01'),
        ],
      },
    },
    {
      id: 47,
      shopId: 1,
      status: 'CONFIRMED',
      pay: 'VIETQR',
      placed: '11/06/2026 08:15',
      items: [item(17, 'Đen', 1, 320000)],
      subtotal: 320000,
      shipping: 25000,
      discount: 0,
      total: 345000,
    },
    {
      id: 46,
      shopId: 5,
      status: 'PREPARING',
      pay: 'COD',
      placed: '10/06/2026 19:42',
      items: [item(19, '50ml', 1, 420000)],
      subtotal: 420000,
      shipping: 16000,
      discount: 0,
      total: 436000,
    },
    {
      id: 28,
      shopId: 1,
      status: 'RETURN_REQUESTED',
      pay: 'COD',
      placed: '01/06/2026 10:30',
      items: [item(18, 'Blue switch', 1, 459000)],
      subtotal: 459000,
      shipping: 25000,
      discount: 0,
      total: 484000,
      shipment: {
        carrier: 'GHN',
        tracking_code: 'VN880077120',
        estimated_delivery_at: at('04/06/2026'),
        delivered_at: at('04/06/2026 16:02'),
        events: [ev('DELIVERED', 'Giao hàng thành công', 'Q.1, TP. HCM', '04/06 16:02')],
      },
      refund: {
        id: 233,
        code: 'RFN-0233',
        status: 'REQUESTED',
        request_type: 'RETURN_AND_REFUND',
        reason_code: 'DEFECTIVE',
        refund_amount: 459000,
        shop_message: null,
        deadline_at: at('06/06/2026 10:30'),
        refund_bank_masked: 'Vietcombank ••4471',
        resolved_at: null,
      },
    },
    {
      id: 29,
      shopId: 4,
      status: 'PENDING_PAYMENT',
      pay: 'VNPAY',
      placed: '10/06/2026 09:40',
      session: SEED_PENDING_SESSION,
      items: [item(9, 'Trắng', 2, 59000)],
      subtotal: 118000,
      shipping: 30000,
      discount: 0,
      total: 148000,
      expires,
    },
    {
      id: 30,
      shopId: 6,
      status: 'PENDING_PAYMENT',
      pay: 'VNPAY',
      placed: '10/06/2026 09:40',
      session: SEED_PENDING_SESSION,
      items: [item(6, 'Đen / 25L', 1, 189000)],
      subtotal: 189000,
      shipping: 22000,
      discount: 20000,
      total: 191000,
      expires,
    },
    {
      id: 18,
      shopId: 2,
      status: 'CANCELLED',
      pay: 'VNPAY',
      placed: '12/05/2026 10:02',
      items: [item(14, 'Đen / 37', 1, 320000)],
      subtotal: 320000,
      shipping: 18000,
      discount: 0,
      total: 338000,
      cancel: 'CHANGE_OF_MIND',
    },
    {
      id: 17,
      shopId: 4,
      status: 'AUTO_CANCELLED',
      pay: 'VNPAY',
      placed: '11/05/2026 08:20',
      items: [item(10, 'Trắng', 1, 149000)],
      subtotal: 149000,
      shipping: 30000,
      discount: 0,
      total: 179000,
      cancel: 'PAYMENT_TIMEOUT',
    },
    {
      id: 21,
      shopId: 3,
      status: 'REFUNDED',
      pay: 'VNPAY',
      placed: '20/05/2026 13:27',
      items: [item(15, 'Xanh / L', 1, 245000)],
      subtotal: 245000,
      shipping: 20000,
      discount: 0,
      total: 265000,
      shipment: {
        carrier: 'GHN',
        tracking_code: 'VN880012345',
        estimated_delivery_at: null,
        delivered_at: null,
        events: [],
      },
      refund: {
        id: 212,
        code: 'RFN-0212',
        status: 'REFUNDED',
        request_type: 'RETURN_AND_REFUND',
        reason_code: 'NOT_AS_DESCRIBED',
        refund_amount: 265000,
        shop_message: null,
        deadline_at: null,
        refund_bank_masked: null,
        resolved_at: at('24/05/2026'),
      },
    },
  ].map((s) => build(s as Seed));
}

/** Đơn vừa đặt ở checkout (kho mock dùng chung) → DTO đơn. */
export function placedToDto(o: MockPlacedOrder, sessionStatus: string | undefined): OrderDetailDto {
  const status: OrderStatus =
    o.status === 'PENDING_PAYMENT' && sessionStatus === 'COMPLETED' ? 'PAID' : o.status;
  const s = SEED_SHOPS.find((x) => x.id === o.shop.id);
  return {
    id: o.id,
    code: o.code,
    checkout_session_id: o.session_id,
    status,
    payment_method: o.payment_method,
    shop: { id: o.shop.id, name: o.shop.name, is_mall: s?.is_authentic ?? false },
    items: o.items.map((it) => ({
      id: it.cart_item_id,
      product_id: it.product_id,
      variant_id: it.variant_id,
      product_slug: it.product_slug,
      product_name: it.product_name,
      image_url: it.image_url,
      variant_name: it.variant_name,
      quantity: it.quantity,
      unit_price: it.unit_price,
    })),
    subtotal: o.subtotal,
    shipping_fee: o.shipping_fee,
    discount_total: o.discount,
    grand_total: o.grand_total,
    payment_expires_at:
      status === 'PENDING_PAYMENT'
        ? new Date(Date.parse(o.placed_at) + 15 * 60_000).toISOString()
        : null,
    delivered_at: null,
    is_reviewed: false,
    allowed_actions: allowedActions({
      status,
      paymentMethod: o.payment_method,
      deliveredAt: null,
      reviewed: false,
    }),
    placed_at: o.placed_at,
    money: {
      subtotal: o.subtotal,
      shipping_fee: o.shipping_fee,
      discount_total: o.discount,
      coin_discount: 0,
      grand_total: o.grand_total,
    },
    shipping_address: o.address,
    shipment: null,
    escrow_release_at: null,
    return_window_closed: false,
    refund_request: null,
    cancel_reason: null,
    note: o.note,
  };
}
