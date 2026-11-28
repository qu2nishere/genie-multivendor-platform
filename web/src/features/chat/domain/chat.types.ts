// Chat khách ↔ shop (plan G10.62; API_SPEC 8.1–8.6, 13; plan 2026-11-26 chat M06; prototype ChatWidget.jsx).

export type MessageType = 'TEXT' | 'IMAGE' | 'PRODUCT_CARD' | 'ORDER_CARD';
/** SENDING / FAILED chỉ có ở client (bong bóng chưa có ack); server trả DELIVERED ("Đã nhận") / SEEN ("Đã xem"). */
export type MessageStatus = 'SENDING' | 'SENT' | 'DELIVERED' | 'SEEN' | 'FAILED';

export interface ChatShop {
  id: number;
  name: string;
  authentic: boolean;
  online: boolean;
  lastActiveAt: string | null;
}

export interface Conversation {
  id: number;
  shop: ChatShop;
  lastMessage: { type: MessageType; preview: string | null; sentAt: string } | null;
  unread: number;
}

export interface ChatMessage {
  id: number | string;
  mine: boolean;
  type: MessageType;
  text: string | null;
  /** Thông tin HIỆN TẠI của sản phẩm; `available = false` (đã ẩn / xóa) → `price = null`. */
  product: {
    id: number;
    slug: string;
    name: string;
    image: string | null;
    price: number | null;
    available: boolean;
  } | null;
  order: { id: number; code: string; status?: string | null } | null;
  imageUrl: string | null;
  sentAt: string;
  status: MessageStatus;
  clientId: string | null;
  /** Tin tự động của shop (lời chào / vắng mặt / tạm nghỉ — UC-VND-06). */
  auto: boolean;
  /** Mã lỗi khi bong bóng FAILED (chỉ phía client). */
  failure?: string;
}

/** Một trang lịch sử (8.3): tin cũ → mới; `nextCursor` để tải tin cũ hơn. */
export interface MessagePage {
  items: ChatMessage[];
  nextCursor: string | null;
}

/** Tin gửi đi (8.4). */
export type OutgoingMessage =
  | { type: 'TEXT'; text: string }
  | { type: 'IMAGE'; imageObjectKey: string; previewUrl: string | null }
  | { type: 'PRODUCT_CARD'; productId: number; variantId?: number | null }
  | { type: 'ORDER_CARD'; orderId: number };

export interface OpenedConversation {
  conversation: Conversation;
  /** "Phản hồi 98% · trong vài phút". */
  responseRate: number | null;
  avgResponseMinutes: number | null;
}

/** Một dòng ô "Gửi đơn" (API_SPEC 8.6) — đơn gần nhất giữa đúng hai bên của hội thoại. */
export interface OrderPick {
  id: number;
  code: string;
  status: string;
  total: number;
  createdAt: string;
  itemCount: number;
  thumbnailUrl: string | null;
}

/** Hội thoại phía shop (8.1 `GET /seller/conversations` — bên kia là khách hàng). */
export interface SellerConversation {
  id: number;
  customer: {
    id: number;
    name: string;
    avatarUrl: string | null;
    online: boolean;
    lastActiveAt: string | null;
  };
  lastMessage: { type: MessageType; preview: string | null; sentAt: string } | null;
  unread: number;
}
