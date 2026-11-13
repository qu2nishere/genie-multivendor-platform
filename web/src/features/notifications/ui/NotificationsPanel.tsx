// "Thông báo" (prototype Account.jsx NotificationsPanel) — plan G10.60: tab theo loại (N_TABS), đánh dấu
// tất cả đã đọc, mở một thông báo = đánh dấu đã đọc rồi đi tới đích (đơn, voucher…); thông báo toàn sàn
// không có đích → hộp chi tiết; "Xem thêm" mỗi lần 5 mục.
import { useState } from 'react';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatDate } from '@/shared/lib/format';
import { Providers } from '@/shared/providers/Providers';
import { Button, EmptyState, Icon, Modal, PanelHead, Skeleton, Tabs } from '@/shared/ui';
import { useMarkAllRead, useMarkRead, useNotifications } from '../application/useNotifications';
import type { AppNotification, NotificationType } from '../domain/notifications.types';
import { NotifIcon } from './NotifPanel';

type Tab = 'all' | NotificationType;
const TABS: Tab[] = ['all', 'order', 'promo', 'system', 'chat'];

function Panel() {
  const { t, locale } = useT();
  const q = useNotifications();
  const markAll = useMarkAllRead();
  const mark = useMarkRead();
  const [tab, setTab] = useState<Tab>('all');
  const [limit, setLimit] = useState(5);
  const [detail, setDetail] = useState<AppNotification | null>(null);
  const all = tab === 'all' ? q.items : q.items.filter((n) => n.type === tab);
  const list = all.slice(0, limit);

  const open = (n: AppNotification) => {
    if (n.unread) mark.mutate(n.id);
    if (n.href) window.location.assign(n.href);
    else setDetail(n);
  };

  return (
    <section
      className="bg-surface-card rounded-lg shadow-sm overflow-hidden"
      aria-labelledby="notif-title"
    >
      <div className="px-[22px] pt-4 max-sm:px-4">
        <PanelHead
          id="notif-title"
          icon="bell"
          title={t('account.menu.notifications')}
          sub={
            q.unread > 0
              ? t('notifications.page.unread', { count: q.unread })
              : t('notifications.page.all_read')
          }
          action={
            <Button
              size="sm"
              variant="outline"
              iconLeft="check-check"
              disabled={q.unread === 0}
              loading={markAll.isPending}
              onClick={() => markAll.mutate()}
            >
              {t('notifications.page.mark_all')}
            </Button>
          }
        />
      </div>
      <div className="px-[22px] pt-1 max-sm:px-4">
        <Tabs<Tab>
          value={tab}
          onChange={(v) => {
            setTab(v);
            setLimit(5);
          }}
          aria-label={t('account.menu.notifications')}
          items={TABS.map((id) => ({ id, label: t(`notifications.page.tab.${id}`) }))}
        />
      </div>
      {q.isPending ? (
        <Skeleton lines={4} className="m-[22px]" />
      ) : list.length === 0 ? (
        <EmptyState icon="bell" text={t('notifications.page.empty')} />
      ) : (
        <ul className="list-none m-0 p-0">
          {list.map((n) => (
            <li key={n.id}>
              <button
                type="button"
                onClick={() => open(n)}
                className={cn(
                  'w-full flex items-center gap-3 px-[22px] py-3.5 text-left border-0 border-t border-solid border-line-subtle cursor-pointer max-sm:px-4',
                  n.unread ? 'bg-brand-50 hover:bg-brand-100' : 'bg-surface-card hover:bg-gray-50',
                )}
              >
                <span
                  className="w-10 h-10 rounded-full flex-none grid place-items-center overflow-hidden"
                  style={{ background: n.bg }}
                >
                  <NotifIcon n={n} size={40} />
                </span>
                <span className="flex-1 min-w-0">
                  <span
                    className={cn(
                      'block text-sm text-strong',
                      n.unread ? 'font-bold' : 'font-medium',
                    )}
                  >
                    {n.title}
                  </span>
                  <span className="block text-sm text-muted mt-0.5 leading-snug">{n.body}</span>
                  <span className="inline-flex items-center gap-1 text-xs text-subtle mt-1">
                    <Icon name="clock" size={12} />
                    {formatDate(n.createdAt, { locale })}
                  </span>
                </span>
                {n.unread && (
                  <span
                    className="w-[9px] h-[9px] rounded-full bg-brand-500 flex-none"
                    aria-label={t('notifications.page.new')}
                  />
                )}
              </button>
            </li>
          ))}
        </ul>
      )}
      {all.length > limit && (
        <div className="p-3.5 text-center border-0 border-t border-solid border-line-subtle">
          <Button size="sm" variant="ghost" onClick={() => setLimit((l) => l + 5)}>
            {t('notifications.page.more')}
          </Button>
        </div>
      )}
      <Modal
        open={detail !== null}
        onClose={() => setDetail(null)}
        title={t('notifications.page.detail')}
        footer={<Button onClick={() => setDetail(null)}>{t('promotions.wallet.got_it')}</Button>}
      >
        {detail && (
          <div>
            <div className="font-bold text-base text-strong">{detail.title}</div>
            <div className="text-xs text-subtle mt-1 mb-3">
              {formatDate(detail.createdAt, { locale })}
            </div>
            <p className="m-0 text-base text-body leading-relaxed">{detail.body}</p>
          </div>
        )}
      </Modal>
    </section>
  );
}

export default function NotificationsPanel() {
  return (
    <Providers>
      <Panel />
    </Providers>
  );
}
