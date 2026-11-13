// DTO snake_case — GET /seller/orders (API_SPEC 10.3).
export interface SellerOrderDto {
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

/** summary.counts theo tab (badge "Mới", "Chờ chuẩn bị"…). */
export type OrderCountsDto = Record<
  'ALL' | 'NEW' | 'TO_PREPARE' | 'DELIVERING' | 'DELIVERED' | 'CANCELLED',
  number
>;
