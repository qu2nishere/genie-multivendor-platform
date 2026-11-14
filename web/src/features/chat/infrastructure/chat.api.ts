// Chat khách ↔ shop (API_SPEC 8.1–8.5). Thời gian thực qua WebSocket (mục 13) chưa nối — client hỏi lại
// tin mỗi vài giây khi đang mở hội thoại. Mock: 4 hội thoại mẫu của prototype, shop tự trả lời sau ~1,1 s
// (như "tự trả lời" của shop — UC-VND-06), giữ trong phiên tab.
import { api } from '@/shared/api/http';
import { mockDelay, mockFail, mockState, USE_MOCK } from '@/shared/api/mock';
import { SEED_PRODUCTS, SEED_SHOPS } from '@/shared/api/mock/seed';
import { productImg } from '@/shared/lib/assets';
import type {
  ChatMessage,
  Conversation,
  MessageType,
  OpenedConversation,
  OutgoingMessage,
} from '../domain/chat.types';

// ---- DTO ----
interface CounterpartDto {
  type: 'SHOP';
  id: number;
  name: string;
  avatar_url: string | null;
  is_mall: boolean;
  is_online: boolean;
  last_active_at: string | null;
}
interface ConversationDto {
  id: number;
  counterpart: CounterpartDto;
  last_message: { type: MessageType; preview: string | null; sent_at: string } | null;
  unread_count: number;
}
interface MessageDto {
  id: number;
  sender: 'ME' | 'THEM';
  type: MessageType;
  text: string | null;
  product: { id: number; slug?: string; name: string; image_url: string; price: number } | null;
  order: { id: number; code: string } | null;
  image_url: string | null;
  sent_at: string;
  status: 'SENT' | 'DELIVERED' | 'SEEN';
  client_message_id: string | null;
}

const toConversation = (d: ConversationDto): Conversation => ({
  id: d.id,
  shop: {
    id: d.counterpart.id,
    name: d.counterpart.name,
    authentic: d.counterpart.is_mall,
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
const toMessage = (d: MessageDto): ChatMessage => ({
  id: d.id,
  mine: d.sender === 'ME',
  type: d.type,
  text: d.text,
  product: d.product && {
    id: d.product.id,
    slug: d.product.slug ?? 'san-pham',
    name: d.product.name,
    image: d.product.image_url,
    price: d.product.price,
  },
  order: d.order,
  imageUrl: d.image_url,
  sentAt: d.sent_at,
  status: d.status,
  clientId: d.client_message_id,
});

// ---- Mock ----
const REPLIES = [
  'Dạ shop hỗ trợ bạn ngay ạ 🧡',
  'Sản phẩm còn hàng, bên mình đóng gói gửi liền trong hôm nay nhé!',
  'Đơn của bạn dự kiến giao trong 2–3 ngày, freeship toàn quốc ạ.',
  'Bạn để lại số điện thoại, shop tư vấn kỹ hơn nha!',
];
const REPLY_MS = 1100;
const today = (h: number, m: number) => {
  const d = new Date();
  d.setHours(h, m, 0, 0);
  return d.toISOString();
};
const daysAgo = (n: number, h = 9) => {
  const d = new Date(Date.now() - n * 86_400_000);
  d.setHours(h, 0, 0, 0);
  return d.toISOString();
};
const shopOf = (id: number) => SEED_SHOPS.find((s) => s.id === id);
const productCard = (id: number) => {
  const p = SEED_PRODUCTS.find((x) => x.id === id);
  return p
    ? { id: p.id, slug: p.slug, name: p.title, image_url: productImg(p.image), price: p.price }
    : null;
};
let seq = 99000;
const msg = (
  o: Partial<MessageDto> & Pick<MessageDto, 'sender' | 'type' | 'sent_at'>,
): MessageDto => ({
  id: ++seq,
  text: null,
  product: null,
  order: null,
  image_url: null,
  status: 'SEEN',
  client_message_id: null,
  ...o,
});

interface MockThread {
  id: number;
  shopId: number;
  online: boolean;
  unread: number;
  messages: MessageDto[];
  /** Tin trả lời đang chờ tới hạn (mô phỏng shop gõ). */
  pending: { at: number; text: string } | null;
}

const seed = (): { threads: MockThread[]; replyIdx: number } => ({
  replyIdx: 0,
  threads: [
    {
      id: 3301,
      shopId: 1,
      online: true,
      unread: 1,
      pending: null,
      messages: [
        msg({
          sender: 'THEM',
          type: 'TEXT',
          text: 'Chào bạn 🧡 TechZone có thể giúp gì cho bạn hôm nay ạ?',
          sent_at: today(14, 20),
        }),
        msg({
          sender: 'THEM',
          type: 'PRODUCT_CARD',
          product: productCard(1),
          sent_at: today(14, 20),
        }),
        msg({
          sender: 'ME',
          type: 'TEXT',
          text: 'Sản phẩm còn màu đen không shop?',
          sent_at: today(14, 31),
        }),
        msg({
          sender: 'THEM',
          type: 'TEXT',
          text: 'Dạ còn đủ màu đen bạn nhé! Freeship cho đơn từ 199k ạ.',
          sent_at: today(14, 32),
        }),
      ],
    },
    {
      id: 3302,
      shopId: 5,
      online: true,
      unread: 2,
      pending: null,
      messages: [
        msg({
          sender: 'THEM',
          type: 'TEXT',
          text: 'Beauty Box chào bạn ạ! Bạn cần tư vấn sản phẩm nào ạ?',
          sent_at: today(10, 2),
        }),
        msg({
          sender: 'ME',
          type: 'TEXT',
          text: 'Serum Vitamin C còn hàng không shop?',
          sent_at: today(10, 5),
        }),
        msg({
          sender: 'THEM',
          type: 'TEXT',
          text: 'Dạ còn nha, bên mình đang có deal hoàn 50k điểm cho đơn từ 300k ạ 🧡',
          sent_at: today(10, 6),
        }),
      ],
    },
    {
      id: 3303,
      shopId: 2,
      online: false,
      unread: 0,
      pending: null,
      messages: [
        msg({
          sender: 'THEM',
          type: 'TEXT',
          text: 'Shoe Republic xin chào! Bạn đi size bao nhiêu để mình tư vấn ạ?',
          sent_at: daysAgo(1, 9),
        }),
        msg({
          sender: 'ME',
          type: 'TEXT',
          text: 'Mình mang size 42, đôi Air Run còn không ạ?',
          sent_at: daysAgo(1, 10),
        }),
        msg({
          sender: 'THEM',
          type: 'TEXT',
          text: 'Còn size 42 nha bạn, ship trong 2-3 ngày ạ.',
          sent_at: daysAgo(1, 11),
        }),
      ],
    },
    {
      id: 3304,
      shopId: 3,
      online: true,
      unread: 0,
      pending: null,
      messages: [
        msg({
          sender: 'THEM',
          type: 'TEXT',
          text: 'Boutique Sài Gòn chào bạn 🧡',
          sent_at: daysAgo(3, 9),
        }),
        msg({
          sender: 'THEM',
          type: 'IMAGE',
          image_url: productImg('jacket'),
          sent_at: daysAgo(3, 9),
        }),
        msg({
          sender: 'THEM',
          type: 'TEXT',
          text: 'Áo khoác denim mẫu mới về bạn nhé, form rộng phong cách Hàn ạ.',
          sent_at: daysAgo(3, 10),
        }),
      ],
    },
  ],
});
const db = mockState('chat', seed);

/** Đưa tin trả lời tới hạn vào luồng, đánh dấu tin của mình là đã xem. */
function settle(t: MockThread): MockThread {
  if (!t.pending || Date.now() < t.pending.at) return t;
  return {
    ...t,
    pending: null,
    messages: [
      ...t.messages.map((m) => (m.sender === 'ME' ? { ...m, status: 'SEEN' as const } : m)),
      msg({
        sender: 'THEM',
        type: 'TEXT',
        text: t.pending.text,
        sent_at: new Date(t.pending.at).toISOString(),
      }),
    ],
  };
}
function threads(): MockThread[] {
  const s = db.get();
  const next = s.threads.map(settle);
  db.set({ ...s, threads: next });
  return next;
}
function convDto(t: MockThread): ConversationDto {
  const sh = shopOf(t.shopId);
  const last = t.messages[t.messages.length - 1];
  return {
    id: t.id,
    counterpart: {
      type: 'SHOP',
      id: t.shopId,
      name: sh?.name ?? `Shop ${t.shopId}`,
      avatar_url: null,
      is_mall: sh?.is_authentic ?? false,
      is_online: t.online,
      last_active_at: last?.sent_at ?? null,
    },
    last_message: last
      ? { type: last.type, preview: last.type === 'TEXT' ? last.text : null, sent_at: last.sent_at }
      : null,
    unread_count: t.unread,
  };
}

// ---- API ----
export async function listConversations(): Promise<Conversation[]> {
  if (USE_MOCK) {
    await mockDelay(120);
    return threads().map(convDto).map(toConversation);
  }
  return (await api.get<ConversationDto[]>('/me/conversations', { query: { limit: 50 } })).map(
    toConversation,
  );
}

/** POST /me/conversations — mở hoặc lấy hội thoại với shop. */
export async function openConversation(shopId: number): Promise<OpenedConversation> {
  if (USE_MOCK) {
    await mockDelay(150);
    const sh = shopOf(shopId);
    if (!sh) mockFail('GNE-VAL-4220');
    const s = db.get();
    let t = s.threads.find((x) => x.shopId === shopId);
    if (!t) {
      t = {
        id: 3300 + s.threads.length + 10,
        shopId,
        online: true,
        unread: 0,
        pending: null,
        messages: [],
      };
      db.set({ ...s, threads: [t, ...s.threads] });
    }
    return {
      conversation: toConversation(convDto(t)),
      responseRate: sh.response_rate,
      avgResponseMinutes: sh.response_rate > 95 ? 5 : 60,
    };
  }
  const r = await api.post<
    ConversationDto & { shop_stats: { chat_response_rate: number; avg_response_minutes: number } }
  >('/me/conversations', { shop_id: shopId });
  return {
    conversation: toConversation(r),
    responseRate: r.shop_stats.chat_response_rate,
    avgResponseMinutes: r.shop_stats.avg_response_minutes,
  };
}

export async function listMessages(conversationId: number): Promise<ChatMessage[]> {
  if (USE_MOCK) {
    await mockDelay(80);
    const t = threads().find((x) => x.id === conversationId);
    if (!t) mockFail('GNE-SOC-4031');
    return t.messages.map(toMessage);
  }
  const r = await api.get<MessageDto[]>(`/me/conversations/${conversationId}/messages`, {
    query: { limit: 30 },
  });
  return r.map(toMessage).reverse();
}

/** Shop đang gõ (mock: còn tin trả lời chờ tới hạn). Thật: sự kiện WebSocket. */
export function mockTyping(conversationId: number): boolean {
  if (!USE_MOCK) return false;
  return !!db.get().threads.find((x) => x.id === conversationId)?.pending;
}

export async function sendMessage(
  conversationId: number,
  m: OutgoingMessage,
  clientId: string,
  card?: { product?: MessageDto['product']; order?: MessageDto['order'] },
): Promise<ChatMessage> {
  if (USE_MOCK) {
    await mockDelay(250);
    const s = db.get();
    const t = s.threads.find((x) => x.id === conversationId);
    if (!t) mockFail('GNE-SOC-4031');
    if (m.type === 'TEXT' && !m.text.trim()) mockFail('GNE-VAL-4220');
    const dto = msg({
      sender: 'ME',
      type: m.type,
      text: m.type === 'TEXT' ? m.text : null,
      product: m.type === 'PRODUCT_CARD' ? (card?.product ?? productCard(m.productId)) : null,
      order: m.type === 'ORDER_CARD' ? (card?.order ?? null) : null,
      image_url: m.type === 'IMAGE' ? (m.previewUrl ?? productImg('jacket')) : null,
      sent_at: new Date().toISOString(),
      status: 'DELIVERED',
      client_message_id: clientId,
    });
    const text = REPLIES[s.replyIdx % REPLIES.length];
    db.set({
      replyIdx: s.replyIdx + 1,
      threads: s.threads.map((x) =>
        x.id === conversationId
          ? { ...x, messages: [...x.messages, dto], pending: { at: Date.now() + REPLY_MS, text } }
          : x,
      ),
    });
    return toMessage(dto);
  }
  const body =
    m.type === 'TEXT'
      ? { type: 'TEXT', text: m.text }
      : m.type === 'IMAGE'
        ? { type: 'IMAGE', image_object_key: m.imageObjectKey }
        : m.type === 'PRODUCT_CARD'
          ? { type: 'PRODUCT_CARD', product_id: m.productId }
          : { type: 'ORDER_CARD', order_id: m.orderId };
  return toMessage(
    await api.post<MessageDto>(`/me/conversations/${conversationId}/messages`, {
      ...body,
      client_message_id: clientId,
    }),
  );
}

export async function markRead(conversationId: number): Promise<void> {
  if (USE_MOCK) {
    const s = db.get();
    db.set({
      ...s,
      threads: s.threads.map((x) => (x.id === conversationId ? { ...x, unread: 0 } : x)),
    });
    return;
  }
  await api.post(`/me/conversations/${conversationId}/read`);
}
