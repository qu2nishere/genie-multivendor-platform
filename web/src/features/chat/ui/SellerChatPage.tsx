// Chat của shop (prototype vendor/Chat.jsx) — plan G11.41: hộp thư khách hàng (chưa đọc, trực tuyến), luồng
// tin dùng chung `Bubble` với phía khách (văn bản, ảnh, thẻ sản phẩm, thẻ đơn), gửi ảnh, gửi thẻ đơn của chính
// khách đó (lấy từ đơn của shop), trạng thái Đang gửi / Đã nhận / Đã xem. Màn hẹp: danh sách → luồng tin.
// Tin tự động (lời chào / vắng mặt) do server gửi theo Cài đặt — không có nút ở đây.
import { useEffect, useMemo, useRef, useState } from 'react';
import { useSellerOrders } from '@/features/seller-orders';
import { errorMessage } from '@/shared/api/errors';
import { UploadError, uploadFile } from '@/shared/api/upload';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatRelative } from '@/shared/lib/format';
import { Avatar, Button, Icon, Skeleton, Spinner, toast } from '@/shared/ui';
import {
  useSellerConversations,
  useSellerMarkRead,
  useSellerMessages,
  useSellerSend,
} from '../application/useSellerChat';
import { mergeMessages, previewKey } from '../domain/chat.rules';
import type { SellerConversation } from '../domain/chat.types';
import { Bubble } from './ChatParts';

function Thread({ c, onBack }: { c: SellerConversation; onBack: () => void }) {
  const { t } = useT();
  const q = useSellerMessages(c.id);
  const { send, pending } = useSellerSend(c.id);
  const read = useSellerMarkRead();
  const orders = useSellerOrders({ size: 50 });
  const [draft, setDraft] = useState('');
  const [menu, setMenu] = useState(false);
  const [uploading, setUploading] = useState(false);
  const body = useRef<HTMLDivElement>(null);
  const img = useRef<HTMLInputElement>(null);
  const list = mergeMessages(q.data ?? [], pending);
  const theirOrders = (orders.data?.items ?? []).filter((o) => o.customer.id === c.customer.id);

  useEffect(() => {
    if (c.unread > 0) read.mutate(c.id);
    // chỉ khi mở hội thoại
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [c.id]);
  useEffect(() => {
    if (body.current) body.current.scrollTop = body.current.scrollHeight;
  }, [list.length]);

  const sendText = () => {
    const v = draft.trim();
    if (!v) return;
    void send({ type: 'TEXT', text: v });
    setDraft('');
  };
  const pick = async (f: File | undefined) => {
    if (!f) return;
    setUploading(true);
    try {
      const u = await uploadFile(f, 'CHAT_IMAGE');
      void send({ type: 'IMAGE', imageObjectKey: u.objectKey, previewUrl: u.previewUrl });
    } catch (e) {
      toast.error(e instanceof UploadError ? t('chat.image_invalid') : errorMessage(e, t));
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="flex flex-col min-w-0 min-h-0 h-full">
      <div className="flex items-center gap-2.5 px-[18px] py-3 border-0 border-b border-solid border-line-subtle flex-none max-md:px-3">
        <button
          type="button"
          aria-label={t('common.action.back')}
          onClick={onBack}
          className="hidden max-md:grid w-10 h-10 place-items-center border-0 bg-transparent cursor-pointer text-muted"
        >
          <Icon name="arrow-left" size={19} />
        </button>
        <Avatar
          name={c.customer.name}
          src={c.customer.avatarUrl}
          size={36}
          online={c.customer.online}
        />
        <div className="min-w-0">
          <div className="font-semibold text-sm text-strong truncate">{c.customer.name}</div>
          <div className={cn('text-[11px]', c.customer.online ? 'text-mint-600' : 'text-subtle')}>
            {c.customer.online ? `● ${t('chat.online')}` : t('chat.offline')}
          </div>
        </div>
        <div className="relative ml-auto">
          <Button
            variant="ghost"
            size="sm"
            iconLeft="package"
            aria-expanded={menu}
            onClick={() => setMenu((o) => !o)}
          >
            {t('seller-chat.orders')}
          </Button>
          {menu && (
            <ul className="list-none m-0 absolute right-0 top-[calc(100%+4px)] w-[230px] bg-surface-card border border-solid border-line rounded-md shadow-md z-10 p-1">
              {theirOrders.length === 0 && (
                <li className="px-2.5 py-2 text-xs text-muted">{t('seller-chat.no_orders')}</li>
              )}
              {theirOrders.map((o) => (
                <li key={o.id}>
                  <button
                    type="button"
                    className="w-full text-left px-2.5 py-2 rounded-sm font-mono text-xs text-body border-0 bg-transparent cursor-pointer hover:bg-gray-50"
                    onClick={() => {
                      void send({ type: 'ORDER_CARD', orderId: o.id }, { id: o.id, code: o.code });
                      setMenu(false);
                    }}
                  >
                    {t('seller-chat.send_card', { code: o.code })}
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
      <div
        ref={body}
        className="flex-1 overflow-y-auto p-[18px] bg-gray-50"
        aria-live="polite"
        aria-busy={q.isPending}
      >
        {q.isPending && (
          <div className="grid place-items-center py-6">
            <Spinner />
          </div>
        )}
        {list.map((m) => (
          <Bubble
            key={String(m.id)}
            m={m}
            avatar={<Avatar name={c.customer.name} src={c.customer.avatarUrl} size={28} />}
            orderHref={(o) => `/seller/orders?q=${encodeURIComponent(o.code)}`}
          />
        ))}
      </div>
      <form
        className="flex items-center gap-2.5 px-4 py-3 border-0 border-t border-solid border-line flex-none"
        onSubmit={(e) => {
          e.preventDefault();
          sendText();
        }}
      >
        <input
          ref={img}
          type="file"
          hidden
          accept="image/*"
          onChange={(e) => {
            void pick(e.target.files?.[0]);
            e.target.value = '';
          }}
        />
        <button
          type="button"
          aria-label={t('chat.send_image')}
          disabled={uploading}
          onClick={() => img.current?.click()}
          className="w-[42px] h-[42px] rounded-full border-0 bg-transparent text-muted cursor-pointer grid place-items-center flex-none hover:bg-gray-100"
        >
          {uploading ? <Spinner size={16} /> : <Icon name="image" size={20} />}
        </button>
        <input
          value={draft}
          maxLength={1000}
          onChange={(e) => setDraft(e.target.value)}
          placeholder={t('chat.placeholder')}
          aria-label={t('chat.placeholder')}
          className="flex-1 min-w-0 h-[42px] border-[1.5px] border-solid border-line rounded-pill px-4 text-base outline-none bg-surface-card focus:border-primary"
        />
        <button
          type="submit"
          aria-label={t('chat.send')}
          className="w-[42px] h-[42px] rounded-full border-0 bg-primary text-primary-on cursor-pointer grid place-items-center flex-none"
        >
          <Icon name="send" size={20} />
        </button>
      </form>
    </div>
  );
}

export default function SellerChatPage() {
  const { t, locale } = useT();
  const q = useSellerConversations();
  const items = useMemo(() => q.data ?? [], [q.data]);
  const [activeId, setActiveId] = useState<number | null>(null);
  // Màn rộng mở sẵn hội thoại đầu (như prototype); màn hẹp chờ chọn
  useEffect(() => {
    if (activeId === null && items[0] && window.matchMedia('(min-width: 768px)').matches)
      setActiveId(items[0].id);
  }, [activeId, items]);
  const active = items.find((c) => c.id === activeId) ?? null;

  return (
    <div className="grid grid-cols-[300px_minmax(0,1fr)] grid-rows-[100%] h-[620px] max-h-[calc(100vh-140px)] min-h-[440px] bg-surface-card rounded-lg shadow-sm overflow-hidden max-w-[1180px] max-md:grid-cols-1">
      <div
        className={cn(
          'border-0 border-r border-solid border-line flex flex-col min-h-0',
          active && 'max-md:hidden',
        )}
      >
        <h2 className="m-0 p-3.5 border-0 border-b border-solid border-line-subtle font-bold text-base text-strong">
          {t('seller-chat.inbox')}
        </h2>
        {q.isPending ? (
          <Skeleton lines={4} className="m-3.5" />
        ) : items.length === 0 ? (
          <p className="m-0 p-6 text-center text-sm text-muted">{t('seller-chat.empty')}</p>
        ) : (
          <ul className="m-0 p-0 list-none overflow-y-auto">
            {items.map((c) => {
              const last = c.lastMessage;
              const text = last
                ? (last.preview ?? t(previewKey(last.type) ?? 'chat.preview.image'))
                : '';
              return (
                <li key={c.id}>
                  <button
                    type="button"
                    aria-current={c.id === activeId ? 'true' : undefined}
                    onClick={() => setActiveId(c.id)}
                    className={cn(
                      'flex gap-2.5 items-center w-full px-3.5 py-3 border-0 border-b border-solid border-line-subtle cursor-pointer text-left',
                      c.id === activeId ? 'bg-primary-tint' : 'bg-surface-card hover:bg-gray-50',
                    )}
                  >
                    <Avatar
                      name={c.customer.name}
                      src={c.customer.avatarUrl}
                      size={42}
                      online={c.customer.online}
                    />
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <span className="font-semibold text-[13px] text-strong flex-1 truncate">
                          {c.customer.name}
                        </span>
                        {last && (
                          <span className="text-[11px] text-subtle flex-none">
                            {formatRelative(last.sentAt, { locale })}
                          </span>
                        )}
                      </div>
                      <div className="flex items-center gap-1.5 mt-0.5">
                        <span className="text-xs text-muted flex-1 truncate">{text}</span>
                        {c.unread > 0 && (
                          <span className="bg-flash-500 text-white text-[10px] font-bold min-w-4 h-4 rounded-lg grid place-items-center px-1">
                            {c.unread}
                          </span>
                        )}
                      </div>
                    </div>
                  </button>
                </li>
              );
            })}
          </ul>
        )}
      </div>
      <div className={cn('min-w-0 min-h-0', !active && 'max-md:hidden')}>
        {active ? (
          <Thread key={active.id} c={active} onBack={() => setActiveId(null)} />
        ) : (
          <div className="h-full grid place-items-center text-center text-subtle p-8">
            <div>
              <Icon name="message-circle" size={44} className="mx-auto mb-2" />
              <p className="m-0 text-sm">{t('chat.pick')}</p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
