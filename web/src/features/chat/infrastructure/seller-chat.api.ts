// Chat phía shop (API_SPEC 8 — cùng DTO, đường dẫn /seller/conversations/**; bên kia là CUSTOMER; không có
// 8.2 "mở hội thoại"). Mock: 3 hội thoại của prototype vendor/Chat.jsx; tin của shop "Đã nhận" rồi "Đã xem"
// sau ~1,2 s; số chưa đọc ghi vào kho dùng chung để huy hiệu nav + Tổng quan đổi theo.
import { api } from '@/shared/api/http';
import { mockDelay, mockFail, mockState, USE_MOCK } from '@/shared/api/mock';
import { sellerChatUnreadStore } from '@/shared/api/mock/seller';
import { productImg } from '@/shared/lib/assets';
import type {
  ChatMessage,
  MessageType,
  OutgoingMessage,
  SellerConversation,
} from '../domain/chat.types';
import { toMessage, type MessageDto } from './chat.api';

interface CustomerDto {
  type: 'CUSTOMER';
  id: number;
  name: string;
  avatar_url: string | null;
  is_online: boolean;
  last_active_at: string | null;
}
interface SellerConversationDto {
  id: number;
  counterpart: CustomerDto;
  last_message: { type: MessageType; preview: string | null; sent_at: string } | null;
  unread_count: number;
}

const toConversation = (d: SellerConversationDto): SellerConversation => ({
  id: d.id,
  customer: {
    id: d.counterpart.id,
    name: d.counterpart.name,
    avatarUrl: d.counterpart.avatar_url,
    online: d.counterpart.is_online,
    lastActiveAt: d.counterpart.last_active_at,
  },
  lastMessage: d.last_message && {
    type: d.last_message.type,
    preview: d.last_message.preview,
    sentAt: d.last_message.sent_at,
  },
  unread: d.unread_count,
});

// ---- Mock ----
const SEEN_MS = 1200;
let seq = 1;
const at = (d: number, h: number, m: number) =>
  `2026-06-${String(d).padStart(2, '0')}T${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:00+07:00`;
const m = (
  sender: 'ME' | 'THEM',
  sent: string,
  body: Partial<MessageDto> & { type: MessageType },
): MessageDto => ({
  id: 70000 + seq++,
  sender,
  text: null,
  product: null,
  order: null,
  image_url: null,
  sent_at: sent,
  status: 'SEEN',
  client_message_id: null,
  ...body,
});

interface Thread {
  id: number;
  customer: CustomerDto;
  unread: number;
  messages: MessageDto[];
}
const cust = (id: number, name: string, online: boolean): CustomerDto => ({
  type: 'CUSTOMER',
  id,
  name,
  avatar_url: null,
  is_online: online,
  last_active_at: online ? null : at(8, 12, 10),
});

const db = mockState<Thread[]>('seller.chat', () => [
  {
    id: 4101,
    customer: cust(20411, 'Trần Thị Mai', true),
    unread: 2,
    messages: [
      m('THEM', at(8, 14, 20), {
        type: 'TEXT',
        text: 'Chào shop, tai nghe ANC Pro 5 còn hàng màu đen không ạ?',
      }),
      m('THEM', at(8, 14, 20), {
        type: 'PRODUCT_CARD',
        product: {
          id: 1,
          slug: 'tai-nghe-bluetooth-anc-pro-5',
          name: 'Tai nghe Bluetooth ANC Pro 5',
          image_url: productImg('headphones'),
          price: 389000,
        },
      }),
      m('ME', at(8, 14, 24), {
        type: 'TEXT',
        text: 'Dạ chào bạn, sản phẩm còn đủ màu đen nhé. Bên mình freeship đơn từ 199k ạ.',
      }),
      m('THEM', at(8, 14, 26), {
        type: 'TEXT',
        text: 'Mình muốn xem thực tế sản phẩm được không shop?',
      }),
      m('ME', at(8, 14, 28), { type: 'IMAGE', image_url: productImg('headphones') }),
      m('ME', at(8, 14, 28), {
        type: 'TEXT',
        text: 'Đây là ảnh thật bên mình chụp ạ. Bạn đặt mình gói cẩn thận gửi liền trong hôm nay nhé!',
      }),
      m('THEM', at(8, 14, 32), { type: 'TEXT', text: 'Ok shop, mình đặt luôn đây 🧡' }),
      m('THEM', at(8, 14, 32), { type: 'TEXT', text: 'Sản phẩm còn màu đen không shop?' }),
    ],
  },
  {
    id: 4102,
    customer: cust(20388, 'Nguyễn Văn An', false),
    unread: 0,
    messages: [m('THEM', at(8, 12, 10), { type: 'TEXT', text: 'Đã nhận hàng, cảm ơn shop!' })],
  },
  {
    id: 4103,
    customer: cust(20377, 'Lê Hoàng Phúc', true),
    unread: 3,
    messages: [
      m('THEM', at(7, 16, 2), { type: 'TEXT', text: 'Shop ơi bàn phím bị kẹt một phím.' }),
      m('THEM', at(7, 16, 3), { type: 'IMAGE', image_url: productImg('keyboard') }),
      m('THEM', at(7, 16, 3), { type: 'TEXT', text: 'Shop xem giúp mình với ạ.' }),
    ],
  },
]);

function save(threads: Thread[]) {
  db.set(threads);
  sellerChatUnreadStore.set({ count: threads.reduce((n, t) => n + t.unread, 0) });
}

/** Tin của shop đã gửi quá 1,2 s → "Đã xem" (bên kia đang mở). */
const settle = (d: MessageDto): MessageDto =>
  d.sender === 'ME' && d.status !== 'SEEN' && Date.now() - Date.parse(d.sent_at) > SEEN_MS
    ? { ...d, status: 'SEEN' }
    : d;

const preview = (d: MessageDto) => (d.type === 'TEXT' ? d.text : null);

export async function listSellerConversations(): Promise<SellerConversation[]> {
  if (USE_MOCK) {
    await mockDelay(150);
    return db.get().map((t) => {
      const last = t.messages.at(-1);
      return toConversation({
        id: t.id,
        counterpart: t.customer,
        last_message: last
          ? { type: last.type, preview: preview(last), sent_at: last.sent_at }
          : null,
        unread_count: t.unread,
      });
    });
  }
  return (
    await api.get<SellerConversationDto[]>('/seller/conversations', { query: { limit: 50 } })
  ).map(toConversation);
}

export async function listSellerMessages(id: number): Promise<ChatMessage[]> {
  if (USE_MOCK) {
    await mockDelay(120);
    const t = db.get().find((x) => x.id === id);
    if (!t) mockFail('GNE-SOC-4031');
    return t.messages.map(settle).map(toMessage);
  }
  const r = await api.get<MessageDto[]>(`/seller/conversations/${id}/messages`, {
    query: { limit: 30 },
  });
  return r.map(toMessage).reverse();
}

export async function sendSellerMessage(
  id: number,
  out: OutgoingMessage,
  clientId: string,
  card?: { order?: { id: number; code: string } },
): Promise<ChatMessage> {
  if (USE_MOCK) {
    await mockDelay(220);
    const all = db.get();
    const t = all.find((x) => x.id === id);
    if (!t) mockFail('GNE-SOC-4031');
    if (out.type === 'TEXT' && !out.text.trim()) mockFail('GNE-VAL-4220');
    const dto: MessageDto = {
      ...m('ME', new Date().toISOString(), { type: out.type }),
      text: out.type === 'TEXT' ? out.text : null,
      image_url: out.type === 'IMAGE' ? (out.previewUrl ?? productImg('jacket')) : null,
      order: out.type === 'ORDER_CARD' ? (card?.order ?? null) : null,
      status: 'DELIVERED',
      client_message_id: clientId,
    };
    save(all.map((x) => (x.id === id ? { ...x, messages: [...x.messages, dto] } : x)));
    return toMessage(dto);
  }
  const body =
    out.type === 'TEXT'
      ? { type: 'TEXT', text: out.text }
      : out.type === 'IMAGE'
        ? { type: 'IMAGE', image_object_key: out.imageObjectKey }
        : out.type === 'PRODUCT_CARD'
          ? { type: 'PRODUCT_CARD', product_id: out.productId }
          : { type: 'ORDER_CARD', order_id: out.orderId };
  return toMessage(
    await api.post<MessageDto>(`/seller/conversations/${id}/messages`, {
      ...body,
      client_message_id: clientId,
    }),
  );
}

export async function markSellerRead(id: number): Promise<void> {
  if (USE_MOCK) {
    save(db.get().map((x) => (x.id === id ? { ...x, unread: 0 } : x)));
    return;
  }
  await api.post(`/seller/conversations/${id}/read`);
}
