// Chat khách ↔ shop (plan G10.62; API_SPEC 8.1–8.5; prototype ChatWidget.jsx SEED / QUICK / REPLIES).

export type MessageType = 'TEXT' | 'IMAGE' | 'PRODUCT_CARD' | 'ORDER_CARD';
/** SENDING chỉ có ở client (bong bóng đang gửi); server trả SENT / DELIVERED / SEEN. */
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
  product: { id: number; slug: string; name: string; image: string; price: number } | null;
  order: { id: number; code: string } | null;
  imageUrl: string | null;
  sentAt: string;
  status: MessageStatus;
  clientId: string | null;
}

/** Tin gửi đi (8.4). */
export type OutgoingMessage =
  | { type: 'TEXT'; text: string }
  | { type: 'IMAGE'; imageObjectKey: string; previewUrl: string | null }
  | { type: 'PRODUCT_CARD'; productId: number }
  | { type: 'ORDER_CARD'; orderId: number };

export interface OpenedConversation {
  conversation: Conversation;
  /** "Phản hồi 98% · trong vài phút". */
  responseRate: number | null;
  avgResponseMinutes: number | null;
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
