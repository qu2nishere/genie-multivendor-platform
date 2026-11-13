// Kho mock "shop của tôi" dùng chung (chỉ khi USE_MOCK) — thay cho các bảng phía server:
// - shop: tên + trạng thái KYC. Mở shop (9.2) đặt NOT_SUBMITTED, nộp KYC đặt PENDING; khung (10.1) đọc.
// - orders: đơn của shop (prototype vendor/data.jsx ORDERS) — Tổng quan, Đơn hàng, Tìm nhanh cùng đọc;
//   xác nhận / từ chối / giao hãng ghi lại để số "Việc cần làm" luôn khớp.
// - products: sản phẩm của shop (prototype Products.jsx PRODUCTS) — Tổng quan, Sản phẩm, Tìm nhanh.
import { mockState } from '../mock';

export type MockKycStatus =
  | 'NOT_SUBMITTED'
  | 'PENDING'
  | 'AI_PROCESSING'
  | 'MANUAL_REVIEW'
  | 'VERIFIED'
  | 'REJECTED'
  | 'SUPPLEMENT_REQUESTED';

export interface MockSellerShop {
  id: number;
  name: string;
  kyc_status: MockKycStatus;
}

export const sellerStore = mockState<MockSellerShop>('seller.shop', () => ({
  id: 1,
  name: 'TechZone Official',
  kyc_status: 'VERIFIED',
}));

/** Đơn của shop theo hình GET /seller/orders (API 10.3). */
export interface MockSellerOrder {
  id: number;
  code: string;
  status: 'NEW' | 'CONFIRMED' | 'PREPARING' | 'DELIVERING' | 'DELIVERED' | 'CANCELLED';
  payment_method: 'VNPAY' | 'VIETQR' | 'COD';
  customer: { id: number; name: string; avatar_url: string | null };
  ship_to: string;
  items: { product_name: string; variant_name: string; quantity: number; unit_price: number }[];
  grand_total: number;
  buyer_note: string | null;
  reject_reason: string | null;
  shipment: {
    carrier: 'GHN' | 'GHTK' | 'VIETTEL_POST' | 'JNT_EXPRESS' | 'GENIE_EXPRESS';
    tracking_code: string | null;
    status:
      | 'AWAITING_PICKUP'
      | 'IN_TRANSIT'
      | 'OUT_FOR_DELIVERY'
      | 'DELIVERY_FAILED'
      | 'DELIVERED'
      | 'CANCELLED';
    carrier_status_text: string;
  } | null;
  created_at: string;
}

type Line = [string, string, number, number];
const order = (
  id: number,
  name: string,
  custId: number,
  shipTo: string,
  lines: Line[],
  total: number,
  status: MockSellerOrder['status'],
  pay: MockSellerOrder['payment_method'],
  at: string,
  extra: Partial<MockSellerOrder> = {},
): MockSellerOrder => ({
  id,
  code: `TR-20260608-${String(id).padStart(6, '0')}`,
  status,
  payment_method: pay,
  customer: { id: custId, name, avatar_url: null },
  ship_to: shipTo,
  items: lines.map(([product_name, variant_name, quantity, unit_price]) => ({
    product_name,
    variant_name,
    quantity,
    unit_price,
  })),
  grand_total: total,
  buyer_note: null,
  reject_reason: null,
  shipment: null,
  created_at: `${at}+07:00`,
  ...extra,
});

export const SELLER_ORDER_SEEDS: MockSellerOrder[] = [
  order(
    146,
    'Trần Thị Mai',
    20411,
    'Q1, TP.HCM',
    [['Tai nghe ANC Pro 5', 'Đen / Pro', 1, 389000]],
    414000,
    'NEW',
    'VNPAY',
    '2026-06-08T15:05:00',
    {
      buyer_note: 'Giao giờ hành chính giúp mình nhé.',
    },
  ),
  order(
    145,
    'Đinh Gia Bảo',
    20502,
    'Thủ Đức, TP.HCM',
    [['Bàn phím cơ RGB', 'Blue switch', 1, 459000]],
    484000,
    'NEW',
    'COD',
    '2026-06-08T14:48:00',
  ),
  order(
    141,
    'Trần Thị Mai',
    20411,
    'Q1, TP.HCM',
    [
      ['Tai nghe ANC Pro 5', 'Đen / Pro', 1, 389000],
      ['Serum Vitamin C', '30ml', 1, 178000],
    ],
    778000,
    'CONFIRMED',
    'VNPAY',
    '2026-06-08T14:22:00',
    { buyer_note: 'Gói kỹ giúp mình, đồ làm quà.' },
  ),
  order(
    140,
    'Nguyễn Văn An',
    20388,
    'Cầu Giấy, Hà Nội',
    [['Đồng hồ Watch S2', 'Navy', 1, 690000]],
    690000,
    'PREPARING',
    'VIETQR',
    '2026-06-08T09:10:00',
  ),
  order(
    138,
    'Lê Hoàng Phúc',
    20377,
    'Hải Châu, Đà Nẵng',
    [['Bàn phím cơ RGB', 'Đen/Blue', 1, 459000]],
    459000,
    'DELIVERING',
    'VNPAY',
    '2026-06-07T10:00:00',
    {
      shipment: {
        carrier: 'JNT_EXPRESS',
        tracking_code: 'VN931042771',
        status: 'IN_TRANSIT',
        carrier_status_text: 'Đang trung chuyển',
      },
    },
  ),
  order(
    136,
    'Đỗ Minh Khang',
    20361,
    'Biên Hòa, Đồng Nai',
    [['Loa bluetooth mini IPX7', 'Đen', 2, 320000]],
    640000,
    'DELIVERING',
    'COD',
    '2026-06-07T08:30:00',
    {
      shipment: {
        carrier: 'GHN',
        tracking_code: 'VN931042698',
        status: 'OUT_FOR_DELIVERY',
        carrier_status_text: 'Đang giao hàng',
      },
    },
  ),
  order(
    134,
    'Bùi Thu Hằng',
    20344,
    'Ninh Kiều, Cần Thơ',
    [['Webcam Full HD', 'Đen', 1, 540000]],
    540000,
    'DELIVERING',
    'VNPAY',
    '2026-06-06T16:20:00',
    {
      shipment: {
        carrier: 'GHTK',
        tracking_code: 'VN931042544',
        status: 'DELIVERY_FAILED',
        carrier_status_text: 'Giao thất bại',
      },
    },
  ),
  order(
    139,
    'Phạm Thu Hà',
    20352,
    'Q7, TP.HCM',
    [['Máy ảnh Mirrorless X-100', 'Kit 15-45', 1, 1245000]],
    1245000,
    'DELIVERED',
    'VNPAY',
    '2026-06-06T11:00:00',
    {
      shipment: {
        carrier: 'GHN',
        tracking_code: 'VN931042500',
        status: 'DELIVERED',
        carrier_status_text: 'Giao thành công',
      },
    },
  ),
  order(
    135,
    'Vũ Minh Quân',
    20333,
    'Long Biên, Hà Nội',
    [['Sạc nhanh 65W', 'Trắng', 1, 178000]],
    178000,
    'CANCELLED',
    'VNPAY',
    '2026-06-05T09:40:00',
    {
      shipment: {
        carrier: 'GHN',
        tracking_code: null,
        status: 'CANCELLED',
        carrier_status_text: 'Đã hủy',
      },
    },
  ),
];

export const sellerOrdersStore = mockState<MockSellerOrder[]>('seller.orders', () =>
  SELLER_ORDER_SEEDS.map((o) => ({ ...o })),
);

/** Sản phẩm của shop (rút gọn — form sửa ở seller-products bổ sung chi tiết). */
export interface MockSellerProduct {
  id: number;
  name: string;
  image_url: string;
  status: 'PUBLISHED' | 'PENDING_APPROVAL' | 'REJECTED' | 'DRAFT' | 'HIDDEN';
  price: number;
  stock: number;
  sold: number;
  sku_count: number;
  reject_reason: string | null;
}

const product = (
  id: number,
  name: string,
  pic: string,
  status: MockSellerProduct['status'],
  price: number,
  stock: number,
  sold: number,
  skus: number,
  reject: string | null = null,
): MockSellerProduct => ({
  id,
  name,
  image_url: `/img/products/${pic}.jpg`,
  status,
  price,
  stock,
  sold,
  sku_count: skus,
  reject_reason: reject,
});

export const sellerProductsStore = mockState<MockSellerProduct[]>('seller.products', () => [
  product(1, 'Tai nghe Bluetooth ANC Pro 5', 'headphones', 'PUBLISHED', 389000, 38, 2100, 4),
  product(2, 'Đèn bàn LED cảm ứng 3 chế độ', 'lamp', 'PENDING_APPROVAL', 149000, 60, 0, 2),
  product(
    3,
    'Sạc nhanh 65W (thiếu giấy tờ)',
    'phone',
    'REJECTED',
    280000,
    25,
    0,
    1,
    'Thiếu chứng nhận an toàn',
  ),
  product(4, 'Đồng hồ thông minh Watch S2', 'watch', 'PUBLISHED', 690000, 12, 986, 3),
  product(5, 'Loa bluetooth mini IPX7', 'speaker', 'PUBLISHED', 320000, 0, 2600, 2),
  product(6, 'Bàn phím cơ RGB switch blue', 'keyboard', 'DRAFT', 459000, 54, 1900, 6),
  product(7, 'Camera Mirrorless X-100 (bản mới)', 'camera', 'DRAFT', 12990000, 8, 0, 1),
]);

/** Yêu cầu trả hàng gửi tới shop (API 10.4; prototype Fulfillment.jsx RETURNS). */
export interface MockSellerRefund {
  id: number;
  code: string;
  order: { id: number; code: string };
  customer_name: string;
  items: { product_name: string; variant_name: string; quantity: number }[];
  request_type: 'RETURN_AND_REFUND' | 'REFUND_ONLY';
  reason_code: string;
  evidence: { url: string; media_type: 'IMAGE' | 'VIDEO'; name: string }[];
  refund_amount: number;
  status:
    | 'REQUESTED'
    | 'AWAITING_EVIDENCE'
    | 'AWAITING_RETURN_SHIPMENT'
    | 'SHOP_REJECTED'
    | 'DISPUTED'
    | 'REFUNDED';
  deadline_at: string | null;
  evidence_requested: boolean;
}

const img = (n: string) => ({
  url: `/img/products/${n}.jpg`,
  media_type: 'IMAGE' as const,
  name: `${n}.jpg`,
});
const video = { url: '', media_type: 'VIDEO' as const, name: 'mo-hop.mp4' };

export const sellerRefundsStore = mockState<MockSellerRefund[]>('seller.refunds', () => [
  {
    id: 231,
    code: 'RFN-0231',
    order: { id: 129, code: 'TR-20260608-000129' },
    customer_name: 'Hoàng Thu Trang',
    items: [{ product_name: 'Tai nghe ANC Pro 5', variant_name: 'Trắng', quantity: 1 }],
    request_type: 'RETURN_AND_REFUND',
    reason_code: 'WRONG_SIZE_COLOR',
    evidence: [img('headphones'), img('keyboard'), video],
    refund_amount: 389000,
    status: 'REQUESTED',
    deadline_at: '2026-06-10T01:00:00+07:00',
    evidence_requested: false,
  },
  {
    id: 232,
    code: 'RFN-0232',
    order: { id: 127, code: 'TR-20260608-000127' },
    customer_name: 'Mai Anh Tuấn',
    items: [{ product_name: 'Sạc nhanh 65W', variant_name: 'Trắng', quantity: 1 }],
    request_type: 'REFUND_ONLY',
    reason_code: 'DAMAGED_IN_TRANSIT',
    evidence: [img('phone'), video],
    refund_amount: 178000,
    status: 'REQUESTED',
    deadline_at: '2026-06-09T06:00:00+07:00',
    evidence_requested: false,
  },
  {
    id: 229,
    code: 'RFN-0229',
    order: { id: 121, code: 'TR-20260608-000121' },
    customer_name: 'Bùi Đức Anh',
    items: [{ product_name: 'Loa bluetooth mini IPX7', variant_name: 'Đen', quantity: 1 }],
    request_type: 'RETURN_AND_REFUND',
    reason_code: 'DEFECTIVE',
    evidence: [img('speaker'), video],
    refund_amount: 320000,
    status: 'AWAITING_RETURN_SHIPMENT',
    deadline_at: null,
    evidence_requested: false,
  },
]);

/** Số dư ví của shop (API 10.10) — Ví ghi khi rút tiền, Tổng quan (Tiền của shop) đọc. */
export const sellerMoneyStore = mockState('seller.money', () => ({
  available: 12_450_000,
  pending_withdrawal: 5_000_000,
}));
