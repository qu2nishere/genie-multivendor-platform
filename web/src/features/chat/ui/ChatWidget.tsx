// Khung chat nổi góc phải mọi trang storefront (prototype ChatWidget.jsx) — plan G10.63, client:only:
// nút mở (số tin chưa đọc), hộp thư nhiều shop → luồng tin theo shop. Khách bấm thì nhắc đăng nhập.
// Trên trang /chat (bản toàn trang) không hiện.
import { useStore } from '@nanostores/react';
import { useEffect, useState } from 'react';
import { requireLogin } from '@/features/auth';
import { $session } from '@/shared/auth/session';
import { useT } from '@/shared/hooks/useT';
import { Providers } from '@/shared/providers/Providers';
import { Icon, Seal } from '@/shared/ui';
import { useConversations } from '../application/useChat';
import { sortConversations, totalUnread } from '../domain/chat.rules';
import type { Conversation } from '../domain/chat.types';
import { ConversationList, Thread } from './ChatParts';

const hdrBtn =
  'w-[30px] h-[30px] rounded-md border-0 bg-transparent text-inverse cursor-pointer grid place-items-center flex-none hover:bg-white/10';

function Widget() {
  const { t } = useT();
  const session = useStore($session);
  const authed = session.status === 'authenticated';
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState<Conversation | null>(null);
  const q = useConversations(authed);
  const items = sortConversations(q.data ?? []);
  const unread = totalUnread(items);
  useEffect(() => {
    if (!open) setActive(null);
  }, [open]);
  const cur = active ? (items.find((c) => c.id === active.id) ?? active) : null;

  return (
    <div className="tr-chatdock fixed right-5 bottom-5 z-[850] font-sans">
      {open && authed && (
        <div
          role="dialog"
          aria-label={t('chat.title')}
          className="absolute right-0 bottom-[76px] w-[348px] max-w-[calc(100vw-24px)] h-[496px] max-h-[calc(100vh-120px)] bg-surface-page rounded-lg shadow-xl flex flex-col overflow-hidden border border-solid border-line-subtle"
        >
          {cur ? (
            <>
              <div className="flex items-center gap-[9px] px-3 py-2.5 bg-brand-900 text-inverse flex-none">
                <button
                  type="button"
                  className={hdrBtn}
                  aria-label={t('common.action.back')}
                  onClick={() => setActive(null)}
                >
                  <Icon name="arrow-left" size={19} />
                </button>
                <Seal id={cur.shop.id} name={cur.shop.name} size={36} />
                <div className="flex-1 min-w-0">
                  <div className="font-bold text-sm truncate">{cur.shop.name}</div>
                  <div className="text-[11px] opacity-85">
                    {t(cur.shop.online ? 'chat.online' : 'chat.offline')}
                  </div>
                </div>
                <a
                  href={`/chat?shop=${cur.shop.id}`}
                  className={hdrBtn}
                  aria-label={t('chat.open_full')}
                >
                  <Icon name="external-link" size={16} />
                </a>
                <button
                  type="button"
                  className={hdrBtn}
                  aria-label={t('common.action.close')}
                  onClick={() => setOpen(false)}
                >
                  <Icon name="x" size={18} />
                </button>
              </div>
              <Thread conversation={cur} compact />
            </>
          ) : (
            <>
              <div className="flex items-center gap-2.5 px-3.5 py-[13px] bg-brand-900 text-inverse flex-none">
                <Icon name="message-circle" size={20} />
                <span className="font-bold text-[15px] flex-1">{t('chat.title')}</span>
                <button
                  type="button"
                  className={hdrBtn}
                  aria-label={t('common.action.close')}
                  onClick={() => setOpen(false)}
                >
                  <Icon name="x" size={18} />
                </button>
              </div>
              <div className="flex-1 overflow-y-auto bg-surface-card">
                <ConversationList items={items} onOpen={setActive} />
              </div>
            </>
          )}
        </div>
      )}
      <button
        type="button"
        onClick={() => (authed ? setOpen((o) => !o) : requireLogin())}
        aria-expanded={open}
        aria-label={
          !open && unread > 0 ? t('chat.launcher_unread', { count: unread }) : t('chat.launcher')
        }
        className="relative w-[50px] h-[50px] rounded-full border-0 bg-brand-900 text-inverse cursor-pointer grid place-items-center shadow-[0_0_0_3px_var(--surface-card),var(--shadow-lg)] transition-transform hover:-translate-y-0.5 hover:bg-brand-800"
      >
        <Icon name={open ? 'chevron-down' : 'message-circle'} size={26} />
        {!open && unread > 0 && (
          <span className="absolute -top-0.5 -right-0.5 min-w-5 h-5 rounded-[10px] bg-accent text-accent-on font-bold text-[11px] grid place-items-center px-[5px] border-2 border-solid border-surface-card">
            {unread}
          </span>
        )}
      </button>
    </div>
  );
}

export default function ChatWidget() {
  return (
    <Providers>
      <Widget />
    </Providers>
  );
}
