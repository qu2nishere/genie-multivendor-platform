// Kho mock "hoạt động của người mua" dùng chung (chỉ khi USE_MOCK): yêu cầu trả hàng (refunds ghi, orders
// đọc để đổi trạng thái đơn) và đánh giá (reviews ghi, orders đánh dấu đã đánh giá, coins cộng +50 Điểm).
// Thay cho việc server tự cập nhật trong cùng transaction.
import { mockState } from '../mock';

export interface MockRefund {
  id: number;
  code: string;
  order_id: number;
  status: string;
  request_type: 'RETURN_AND_REFUND' | 'REFUND_ONLY';
  reason_code: string;
  refund_amount: number;
  shop_message: string | null;
  deadline_at: string | null;
  refund_bank_masked: string | null;
  resolved_at: string | null;
  evidence_count: number;
}

export interface MockReview {
  id: number;
  order_id: number;
  order_item_id: number;
  rating: number;
  with_media: boolean;
  coins_awarded: number;
  created_at: string;
}

export const activityStore = mockState<{ refunds: MockRefund[]; reviews: MockReview[] }>(
  'activity',
  () => ({ refunds: [], reviews: [] }),
);
