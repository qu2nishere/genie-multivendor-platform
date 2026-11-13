// Phần dùng chung của chat (prototype ChatWidget.jsx): hộp thư, bong bóng (chữ, ảnh, thẻ sản phẩm, thẻ
// đơn), chấm "đang gõ", tin nhanh, ô soạn (gửi ảnh ≤ 5 MB, gửi đơn với shop). Dùng cho khung chat nổi và
// trang /chat.
import { useEffect, useRef, useState } from 'react';
import { useOrders } from '@/features/orders';
import { errorMessage } from '@/shared/api/errors';
import { UploadError, uploadFile } from '@/shared/api/upload';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatDateTime, formatRelative, formatVND } from '@/shared/lib/format';
import { Icon, Seal, Spinner, toast } from '@/shared/ui';
import { isTyping, useMarkRead, useMessages, useSendMessage } from '../application/useChat';
import { mergeMessages, previewKey, QUICK_REPLIES, statusKey } from '../domain/chat.rules';
import type { ChatMessage, Conversation } from '../domain/chat.types';

export function ConversationList({
  items,
  activeId,
  onOpen,
}: {
  items: Conversation[];
  activeId?: number | null;
  onOpen: (c: Conversation) => void;
}) {
  const { t, locale } = useT();
  if (!items.length)
    return <p className="m-0 p-6 text-center text-sm text-muted">{t('chat.empty')}</p>;
  return (
    <ul className="list-none m-0 p-0">
      {items.map((c) => {
        const last = c.lastMessage;
        const text = last
          ? (last.preview ?? t(previewKey(last.type) ?? 'chat.preview.product'))
          : '';
        return (
          <li key={c.id}>
            <button
              type="button"
              onClick={() => onOpen(c)}
              aria-current={c.id === activeId ? 'true' : undefined}
              className={cn(
                'flex gap-[11px] items-center w-full px-3.5 py-[11px] border-0 border-b border-solid border-line-subtle text-left cursor-pointer',
                c.id === activeId ? 'bg-brand-50' : 'bg-surface-card hover:bg-gray-50',
              )}
            >
              <span className="relative flex-none">
                <Seal id={c.shop.id} name={c.shop.name} size={44} />
                {c.shop.online && (
                  <span className="absolute -right-px -bottom-px w-[11px] h-[11px] rounded-full bg-mint-500 border-2 border-solid border-surface-card" />
                )}
              </span>
              <span className="flex-1 min-w-0">
                <span className="flex items-center gap-1.5">
                  <span className="font-semibold text-[13px] text-strong flex-1 truncate">
                    {c.shop.name}
                  </span>
                  {c.shop.authentic && (
                    <span className="font-bold text-[8px] bg-primary text-primary-on rounded-xs px-1 py-px tracking-[.04em] uppercase">
                      {t('common.authentic')}
                    </span>
                  )}
                  {last && (
                    <span className="text-[11px] text-subtle">
                      {formatRelative(last.sentAt, { locale })}
                    </span>
                  )}
                </span>
                <span className="flex items-center gap-1.5 mt-0.5">
                  <span
                    className={cn(
                      'text-xs flex-1 truncate',
                      c.unread ? 'text-body font-semibold' : 'text-muted',
                    )}
                  >
                    {text}
                  </span>
                  {c.unread > 0 && (
                    <span className="bg-brand-900 text-inverse font-bold text-[10px] min-w-4 h-4 rounded-lg grid place-items-center px-1 flex-none">
                      {c.unread}
                    </span>
                  )}
                </span>
              </span>
            </button>
          </li>
        );
      })}
    </ul>
  );
}

function Bubble({ m, shop }: { m: ChatMessage; shop: Conversation['shop'] }) {
  const { t, locale } = useT();
  const st = statusKey(m);
  const base = 'max-w-[232px] rounded-lg px-3 py-2 text-sm leading-snug';
  return (
    <div className={cn('flex flex-col mb-2.5', m.mine ? 'items-end' : 'items-start')}>
      <div className={cn('flex items-end gap-[7px]', m.mine && 'flex-row-reverse')}>
        {!m.mine && <Seal id={shop.id} name={shop.name} size={26} />}
        {m.type === 'TEXT' && (
          <div
            className={cn(
              base,
              'whitespace-pre-wrap break-words',
              m.mine
                ? 'bg-primary text-primary-on'
                : 'bg-surface-card text-body border border-solid border-line-subtle',
            )}
          >
            {m.text}
          </div>
        )}
        {m.type === 'IMAGE' && m.imageUrl && (
          <img
            src={m.imageUrl}
            alt={t('chat.image_alt')}
            width={140}
            height={140}
            className="w-[140px] h-[140px] object-cover rounded-lg border border-solid border-line-subtle"
          />
        )}
        {m.type === 'ORDER_CARD' && m.order && (
          <a
            href={`/orders/${m.order.id}`}
            className="w-[210px] bg-surface-card border border-solid border-line rounded-lg p-2.5 no-underline"
          >
            <span className="flex items-center gap-1.5 font-semibold text-sm text-strong">
              <Icon name="package" size={15} className="text-primary" />
              {t('chat.order')}
            </span>
            <span className="block font-mono text-xs text-muted mt-[3px]">{m.order.code}</span>
          </a>
        )}
        {m.type === 'PRODUCT_CARD' && m.product && (
          <div className="w-[210px] bg-surface-card border border-solid border-line rounded-lg overflow-hidden">
            <div className="flex gap-[9px] p-[9px]">
              <img
                src={m.product.image}
                alt=""
                width={48}
                height={48}
                className="w-12 h-12 rounded-sm object-cover flex-none"
              />
              <div className="min-w-0">
                <div className="text-xs text-body line-clamp-2">{m.product.name}</div>
                <div className="font-bold text-[13px] text-price mt-0.5">
                  {formatVND(m.product.price, { locale })}
                </div>
              </div>
            </div>
            <a
              href={`/p/${m.product.slug}-${m.product.id}`}
              className="block border-0 border-t border-solid border-line-subtle px-2.5 py-1.5 font-semibold text-xs text-link text-center no-underline"
            >
              {t('chat.view_product')}
            </a>
          </div>
        )}
      </div>
      <span className={cn('text-[10px] text-subtle mt-[3px]', m.mine ? 'mr-1' : 'ml-[33px]')}>
        <time dateTime={m.sentAt}>{formatDateTime(m.sentAt, { locale })}</time>
        {st && ` · ${t(st)}`}
      </span>
    </div>
  );
}

export function Thread({
  conversation: c,
  responseLine,
  compact,
}: {
  conversation: Conversation;
  responseLine?: string | null;
  compact?: boolean;
}) {
  const { t } = useT();
  const q = useMessages(c.id);
  const { send, pending } = useSendMessage(c.id);
  const read = useMarkRead();
  const orders = useOrders();
  const [draft, setDraft] = useState('');
  const [orderMenu, setOrderMenu] = useState(false);
  const [uploading, setUploading] = useState(false);
  const body = useRef<HTMLDivElement>(null);
  const imgRef = useRef<HTMLInputElement>(null);
  const list = mergeMessages(q.data ?? [], pending);
  const typing = isTyping(c.id) && q.isFetched;
  const shopOrders = (orders.data?.items ?? []).filter((o) => o.shop.id === c.shop.id).slice(0, 8);

  useEffect(() => {
    if (c.unread > 0) read.mutate(c.id);
    // chỉ khi mở hội thoại
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [c.id]);
  useEffect(() => {
    if (body.current) body.current.scrollTop = body.current.scrollHeight;
  }, [list.length, typing]);

  const sendText = (text: string) => {
    const v = text.trim();
    if (!v) return;
    void send({ type: 'TEXT', text: v });
    setDraft('');
  };
  const pickImage = async (f: File | undefined) => {
    if (!f) return;
    setUploading(true);
    try {
      const up = await uploadFile(f, 'CHAT_IMAGE');
      void send({ type: 'IMAGE', imageObjectKey: up.objectKey, previewUrl: up.previewUrl });
    } catch (e) {
      toast.error(e instanceof UploadError ? t('chat.image_invalid') : errorMessage(e, t));
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="flex flex-col h-full min-h-0">
      <div
        ref={body}
        className={cn(
          'flex-1 overflow-y-auto bg-gray-50',
          compact ? 'px-3.5 pt-3.5 pb-1.5' : 'p-5',
        )}
        aria-live="polite"
        aria-busy={q.isPending}
      >
        {responseLine && (
          <p className="m-0 mb-3 text-center text-[11px] text-subtle">{responseLine}</p>
        )}
        {q.isPending && (
          <div className="grid place-items-center py-6">
            <Spinner />
          </div>
        )}
        {list.map((m) => (
          <Bubble key={String(m.id)} m={m} shop={c.shop} />
        ))}
        {typing && (
          <div className="flex items-center gap-[7px] mb-2.5" aria-label={t('chat.typing')}>
            <Seal id={c.shop.id} name={c.shop.name} size={26} />
            <span className="bg-surface-card border border-solid border-line-subtle rounded-lg px-3.5 py-2.5 flex gap-1">
              {[0, 1, 2].map((n) => (
                <span
                  key={n}
                  className="lz-typing-dot w-1.5 h-1.5 rounded-full bg-gray-400"
                  style={{ animationDelay: `${n * 0.15}s` }}
                />
              ))}
            </span>
          </div>
        )}
      </div>
      <div className="flex gap-[7px] px-3 pt-2 overflow-x-auto flex-none bg-surface-page">
        {QUICK_REPLIES.map((k) => (
          <button
            key={k}
            type="button"
            onClick={() => sendText(t(`chat.quick.${k}`))}
            className="flex-none text-xs text-primary bg-surface-card border border-solid border-brand-200 rounded-pill px-3 py-1.5 cursor-pointer whitespace-nowrap min-h-8"
          >
            {t(`chat.quick.${k}`)}
          </button>
        ))}
      </div>
      <form
        className="flex items-center gap-2 px-3 py-2.5 bg-surface-page flex-none"
        onSubmit={(e) => {
          e.preventDefault();
          sendText(draft);
        }}
      >
        <input
          ref={imgRef}
          type="file"
          hidden
          accept="image/*"
          onChange={(e) => {
            void pickImage(e.target.files?.[0]);
            e.target.value = '';
          }}
        />
        <button
          type="button"
          aria-label={t('chat.send_image')}
          disabled={uploading}
          onClick={() => imgRef.current?.click()}
          className="w-10 h-10 rounded-full border-0 bg-transparent text-muted cursor-pointer grid place-items-center flex-none hover:bg-gray-100"
        >
          {uploading ? <Spinner size={16} /> : <Icon name="image" size={19} />}
        </button>
        <div className="relative flex-none">
          <button
            type="button"
            aria-label={t('chat.send_order')}
            aria-expanded={orderMenu}
            onClick={() => setOrderMenu((o) => !o)}
            className="w-10 h-10 rounded-full border-0 bg-transparent text-muted cursor-pointer grid place-items-center hover:bg-gray-100"
          >
            <Icon name="package" size={19} />
          </button>
          {orderMenu && (
            <ul className="list-none m-0 absolute left-0 bottom-[calc(100%+6px)] w-[220px] bg-surface-card border border-solid border-line rounded-md shadow-md p-1 z-10">
              {shopOrders.length === 0 && (
                <li className="px-2.5 py-2 text-xs text-muted">{t('chat.no_orders')}</li>
              )}
              {shopOrders.map((o) => (
                <li key={o.id}>
                  <button
                    type="button"
                    onClick={() => {
                      void send(
                        { type: 'ORDER_CARD', orderId: o.id },
                        { order: { id: o.id, code: o.code } },
                      );
                      setOrderMenu(false);
                    }}
                    className="w-full text-left px-2.5 py-2 rounded-sm font-mono text-xs text-body border-0 bg-transparent cursor-pointer hover:bg-gray-50"
                  >
                    {o.code}
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
        <input
          value={draft}
          maxLength={1000}
          onChange={(e) => setDraft(e.target.value)}
          placeholder={t('chat.placeholder')}
          aria-label={t('chat.placeholder')}
          className="flex-1 min-w-0 h-10 border-[1.5px] border-solid border-line rounded-pill px-[15px] text-sm outline-none bg-surface-card focus:border-primary"
        />
        <button
          type="submit"
          aria-label={t('chat.send')}
          className="w-10 h-10 rounded-full border-0 bg-primary text-primary-on cursor-pointer grid place-items-center flex-none"
        >
          <Icon name="send" size={18} />
        </button>
      </form>
    </div>
  );
}
