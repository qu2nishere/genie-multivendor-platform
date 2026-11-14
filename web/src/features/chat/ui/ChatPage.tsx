// Trang chat toàn trang (plan G10.64) — cùng thành phần với khung chat nổi: hộp thư trái, luồng tin phải;
// màn hẹp hiện lần lượt. ?shop=<id> mở (hoặc tạo) hội thoại với shop (8.2); kèm ?product= / ?order= thì
// tự gửi thẻ sản phẩm / thẻ đơn (từ "Chat ngay" ở trang sản phẩm, "Liên hệ Shop" ở đơn).
import { useEffect, useRef, useState } from 'react';
import { listProductsByIds } from '@/features/product';
import { getOrder } from '@/features/orders';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { Providers } from '@/shared/providers/Providers';
import { Badge, Icon, Seal, Skeleton, toast } from '@/shared/ui';
import { useConversations, useOpenConversation } from '../application/useChat';
import { sortConversations } from '../domain/chat.rules';
import type { Conversation } from '../domain/chat.types';
import { sendMessage } from '../application/chatSend';
import { ConversationList, Thread } from './ChatParts';

export interface ChatPageProps {
  shopId: number | null;
  productId: number | null;
  orderId: number | null;
}

function Screen({ shopId, productId, orderId }: ChatPageProps) {
  const { t } = useT();
  const q = useConversations();
  const open = useOpenConversation();
  const [activeId, setActiveId] = useState<number | null>(null);
  const [response, setResponse] = useState<string | null>(null);
  const started = useRef(false);
  const items = sortConversations(q.data ?? []);
  const active: Conversation | null = items.find((c) => c.id === activeId) ?? null;

  // Mở hội thoại theo ?shop=, gửi thẻ sản phẩm / đơn nếu có, rồi bỏ tham số khỏi URL
  useEffect(() => {
    if (!shopId || started.current) return;
    started.current = true;
    open.mutate(shopId, {
      onSuccess: async (r) => {
        setActiveId(r.conversation.id);
        if (r.responseRate != null)
          setResponse(
            t('chat.response', { rate: r.responseRate, minutes: r.avgResponseMinutes ?? 0 }),
          );
        try {
          if (productId) {
            const [p] = await listProductsByIds([productId]);
            if (p)
              await sendMessage(
                r.conversation.id,
                { type: 'PRODUCT_CARD', productId },
                {
                  product: {
                    id: p.id,
                    slug: p.slug,
                    name: p.title,
                    image_url: p.image,
                    price: p.price,
                  },
                },
              );
          }
          if (orderId) {
            const o = await getOrder(orderId);
            await sendMessage(
              r.conversation.id,
              { type: 'ORDER_CARD', orderId },
              {
                order: { id: o.id, code: o.code },
              },
            );
          }
        } catch (e) {
          toast.error(errorMessage(e, t));
        }
        window.history.replaceState(null, '', '/chat');
      },
      onError: (e) => toast.error(errorMessage(e, t)),
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [shopId]);

  return (
    <div className="mx-auto max-w-[1280px] px-6 pt-[22px] pb-14 max-md:px-3">
      <h1 className="m-0 mb-4 text-3xl font-bold text-strong">{t('chat.title')}</h1>
      <div className="grid grid-cols-[340px_minmax(0,1fr)] h-[calc(100vh-220px)] min-h-[480px] bg-surface-card rounded-lg shadow-sm overflow-hidden border border-solid border-line-subtle max-md:grid-cols-1">
        <div
          className={cn(
            'border-0 border-r border-solid border-line-subtle overflow-y-auto',
            active && 'max-md:hidden',
          )}
        >
          {q.isPending ? (
            <Skeleton lines={5} className="m-4" />
          ) : (
            <ConversationList
              items={items}
              activeId={activeId}
              onOpen={(c) => {
                setActiveId(c.id);
                setResponse(null);
              }}
            />
          )}
        </div>
        <div className={cn('flex flex-col min-h-0', !active && 'max-md:hidden')}>
          {active ? (
            <>
              <div className="flex items-center gap-2.5 px-4 py-3 border-0 border-b border-solid border-line-subtle flex-none">
                <button
                  type="button"
                  aria-label={t('common.action.back')}
                  onClick={() => setActiveId(null)}
                  className="hidden max-md:grid w-10 h-10 place-items-center border-0 bg-transparent cursor-pointer text-muted"
                >
                  <Icon name="arrow-left" size={19} />
                </button>
                <Seal id={active.shop.id} name={active.shop.name} size={36} />
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <a
                      href={`/shop/${active.shop.id}`}
                      className="font-bold text-base text-strong no-underline hover:underline truncate"
                    >
                      {active.shop.name}
                    </a>
                    {active.shop.authentic && (
                      <Badge variant="primary">{t('common.authentic')}</Badge>
                    )}
                  </div>
                  <div className="text-xs text-muted">
                    {t(active.shop.online ? 'chat.online' : 'chat.offline')}
                  </div>
                </div>
              </div>
              <Thread key={active.id} conversation={active} responseLine={response} />
            </>
          ) : (
            <div className="flex-1 grid place-items-center text-center text-subtle p-8">
              <div>
                <Icon name="message-circle" size={44} className="mx-auto mb-2" />
                <p className="m-0 text-sm">{t('chat.pick')}</p>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default function ChatPage(props: ChatPageProps) {
  return (
    <Providers>
      <Screen {...props} />
    </Providers>
  );
}
