// Kho mock "đơn vừa đặt" dùng chung (chỉ khi USE_MOCK): checkout ghi; giỏ (bỏ dòng đã đặt), Điểm (trừ
// Điểm đã dùng), Đơn mua (hiện đơn mới) đọc. Thay cho việc server tự làm các bước đó trong một
// transaction (API_SPEC 6.10). Giữ trong phiên tab như mọi trạng thái mock khác.
import { mockState } from '../mock';

export interface MockPlacedItem {
  cart_item_id: number;
  product_id: number;
  variant_id: number;
  product_slug: string;
  product_name: string;
  image_url: string;
  variant_name: string;
  unit_price: number;
  quantity: number;
}

export interface MockPlacedOrder {
  id: number;
  code: string;
  session_id: number;
  shop: { id: number; name: string };
  status: 'PENDING_PAYMENT' | 'PAID' | 'CONFIRMED' | 'CANCELLED' | 'AUTO_CANCELLED';
  payment_method: 'VNPAY' | 'VIETQR' | 'COD';
  placed_at: string;
  items: MockPlacedItem[];
  subtotal: number;
  shipping_fee: number;
  discount: number;
  grand_total: number;
  note: string | null;
  address: { recipient_name: string; phone_number: string; full_address: string };
}

export interface MockPlacedSession {
  id: number;
  code: string;
  status: 'INITIATED' | 'PAYMENT_PENDING' | 'COMPLETED' | 'FAILED' | 'EXPIRED';
  payment_method: 'VNPAY' | 'VIETQR' | 'COD';
  created_at: string;
  /** Lần đổi phương thức gần nhất (thanh toán lại). */
  updated_at?: string;
  reservation_expires_at: string;
  order_ids: number[];
  cart_item_ids: number[];
  coin_discount: number;
  /** Idempotency-Key của lần đặt — cùng key trả lại phiên cũ. */
  idempotency_key: string;
  summary: {
    subtotal: number;
    shipping_total: number;
    discount_total: number;
    coin_discount: number;
    grand_total: number;
  };
}

export const placedStore = mockState<{ sessions: MockPlacedSession[]; orders: MockPlacedOrder[] }>(
  'checkout.placed',
  () => ({ sessions: [], orders: [] }),
);
