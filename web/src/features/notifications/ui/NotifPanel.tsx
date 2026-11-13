import logomark from '@/assets/brand/logomark.svg';
import { useT } from '@/shared/hooks/useT';
import { formatDate } from '@/shared/lib/format';
import { Icon, toIconName } from '@/shared/ui';
import { useMarkAllRead, useMarkRead } from '../application/useNotifications';
import type { AppNotification } from '../domain/notifications.types';

/** Ô icon thông báo (dùng chung NotifPanel + NotificationsPanel). */
export function NotifIcon({ n, size = 38 }: { n: AppNotification; size?: number }) {
  if (n.icon === 'genie') {
    return (
      <img
        src={logomark.src}
        alt=""
        width={size * 0.68}
        height={size * 0.68}
        className="flex-none"
      />
    );
  }
  return (
    <span
      className="grid place-items-center rounded-md flex-none"
      style={{ width: size, height: size, background: n.bg, color: n.tint }}
      aria-hidden="true"
    >
      <Icon name={toIconName(n.icon, 'bell')} size={19} />
    </span>
  );
}

/**
 * Bảng thông báo rút gọn dưới chuông header (prototype Header.jsx NotifPanel) — plan G07.17:
 * 5 thông báo mới nhất, "Đánh dấu đã đọc", mở đúng đích, "Xem tất cả thông báo".
 */
export function NotifPanel({
  items,
  onNavigate,
  onSelect,
  seeAllHref = '/account/notifications',
}: {
  items: AppNotification[];
  onNavigate?: () => void;
  /** Có thì mọi dòng là nút: đánh dấu đã đọc rồi gọi (Seller Center mở modal chi tiết). */
  onSelect?: (n: AppNotification) => void;
  /** null = ẩn dòng "Xem tất cả thông báo". */
  seeAllHref?: string | null;
}) {
  const { t, locale } = useT();
  const markAll = useMarkAllRead();
  const markOne = useMarkRead();
  const top = items.slice(0, 5);
  return (
    <div className="w-[360px] max-w-[calc(100vw-16px)] overflow-hidden rounded-lg text-body">
      <div className="flex items-center px-4 py-[13px] border-b border-line-subtle">
        <span className="font-bold text-sm text-strong">{t('notifications.panel.title')}</span>
        <button
          type="button"
          onClick={() => markAll.mutate()}
          className="ml-auto text-xs font-semibold text-brand-600 bg-transparent border-0 cursor-pointer hover:underline focus-visible:outline-none focus-visible:shadow-focus rounded-xs"
        >
          {t('notifications.panel.mark_all')}
        </button>
      </div>
      <ul className="max-h-[360px] overflow-y-auto list-none m-0 p-0">
        {top.map((n) => {
          const inner = (
            <>
              <NotifIcon n={n} />
              <span className="min-w-0">
                <span className="block font-semibold text-sm text-strong">{n.title}</span>
                <span className="block text-xs text-muted mt-px leading-[1.35] line-clamp-3">
                  {n.body}
                </span>
                <span className="block text-xs text-subtle mt-[3px]">
                  {formatDate(n.createdAt, { locale })}
                </span>
              </span>
            </>
          );
          const cls = `flex gap-[11px] px-4 py-3 border-b border-line-subtle no-underline hover:no-underline text-[inherit] ${
            n.unread ? 'bg-brand-50' : 'bg-surface-card hover:bg-gray-50'
          }`;
          return (
            <li key={n.id}>
              {onSelect ? (
                <button
                  type="button"
                  className={`${cls} w-full text-left border-x-0 border-t-0 cursor-pointer`}
                  onClick={() => {
                    if (n.unread) markOne.mutate(n.id);
                    onSelect(n);
                  }}
                >
                  {inner}
                </button>
              ) : n.href ? (
                <a
                  href={n.href}
                  className={cls}
                  onClick={() => {
                    if (n.unread) markOne.mutate(n.id);
                    onNavigate?.();
                  }}
                >
                  {inner}
                </a>
              ) : (
                <div className={cls}>{inner}</div>
              )}
            </li>
          );
        })}
        {top.length === 0 && (
          <li className="p-5 text-center text-muted">{t('notifications.panel.empty')}</li>
        )}
      </ul>
      {seeAllHref && (
        <a
          href={seeAllHref}
          onClick={onNavigate}
          className="block px-4 py-[11px] text-center font-semibold text-sm text-brand-600 no-underline hover:underline"
        >
          {t('notifications.panel.see_all')}
        </a>
      )}
    </div>
  );
}
