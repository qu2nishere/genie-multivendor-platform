// Chuông Admin (prototype AdminShell.jsx AdminBell): 5 thông báo mới nhất, "Đánh dấu đã đọc",
// bấm một dòng → đánh dấu đã đọc và mở màn liên quan trong SPA.
import { useNavigate } from 'react-router-dom';
import { badgeText, NotifPanel, useNotifications } from '@/features/notifications';
import { useT } from '@/shared/hooks/useT';
import { Icon, Popover } from '@/shared/ui';

export function AdminBell() {
  const { t } = useT();
  const notif = useNotifications();
  const navigate = useNavigate();
  const unread = notif.unread;
  return (
    <Popover
      aria-label={t('notifications.panel.title')}
      panelClassName="p-0 overflow-hidden max-md:fixed max-md:left-2 max-md:right-2 max-md:top-auto"
      trigger={(p) => (
        <button
          type="button"
          {...p}
          className="gn-iconbtn relative"
          aria-label={
            unread > 0
              ? t('admin.shell.notif_unread', { count: unread })
              : t('notifications.panel.title')
          }
        >
          <Icon name="bell" size={20} />
          {unread > 0 && (
            <span className="absolute top-1 right-1 min-w-4 h-4 px-1 rounded-lg bg-red-500 text-white text-[10px] font-bold grid place-items-center">
              {badgeText(unread)}
            </span>
          )}
        </button>
      )}
    >
      {(close) => (
        <NotifPanel
          items={notif.items}
          seeAllHref={null}
          onSelect={(n) => {
            close();
            if (n.href?.startsWith('/admin')) navigate(n.href.slice('/admin'.length) || '/');
          }}
        />
      )}
    </Popover>
  );
}
