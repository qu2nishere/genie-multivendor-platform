// Thanh trên Seller Center (prototype VendorShell.jsx) — plan G11.04: NotifBell (5 tin mới nhất, mở
// modal chi tiết), AccountMenu (shop → Cài đặt, Kênh người mua, Đăng xuất), SellerSearch (đơn + sản
// phẩm của shop, từ 2 ký tự, debounce 250 ms — API 10.1b).
import { useId, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { signOut } from '@/features/auth';
import {
  badgeText,
  NotifPanel,
  useNotifications,
  type AppNotification,
} from '@/features/notifications';
import { searchable, useSellerSearch } from '@/features/seller-settings';
import { useT } from '@/shared/hooks/useT';
import { formatDateTime } from '@/shared/lib/format';
import { Avatar, Button, Icon, Modal, Popover } from '@/shared/ui';

export const iconBtn = 'gn-iconbtn relative';

export function NotifBell() {
  const { t, locale } = useT();
  const notif = useNotifications();
  const [detail, setDetail] = useState<AppNotification | null>(null);
  const navigate = useNavigate();
  const unread = notif.unread;
  return (
    <>
      <Popover
        aria-label={t('notifications.panel.title')}
        panelClassName="p-0 overflow-hidden"
        trigger={(p) => (
          <button
            type="button"
            {...p}
            className={iconBtn}
            aria-label={
              unread > 0
                ? t('seller.shell.notif_unread', { count: unread })
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
              setDetail(n);
            }}
          />
        )}
      </Popover>
      <Modal
        open={!!detail}
        onClose={() => setDetail(null)}
        width={500}
        title={t('seller.shell.notif_detail')}
        footer={
          <>
            {detail?.href?.startsWith('/seller') && (
              <Button
                variant="ghost"
                onClick={() => {
                  const to = detail.href?.slice('/seller'.length) || '/';
                  setDetail(null);
                  navigate(to);
                }}
              >
                {t('seller.shell.notif_open')}
              </Button>
            )}
            <Button onClick={() => setDetail(null)}>{t('seller.shell.got_it')}</Button>
          </>
        }
      >
        {detail && (
          <div>
            <div className="text-lg font-semibold text-strong">{detail.title}</div>
            <div className="text-xs text-muted mt-1.5 mb-3.5">
              {formatDateTime(detail.createdAt, { locale })}
            </div>
            <p className="m-0 text-sm text-body leading-relaxed">{detail.body}</p>
          </div>
        )}
      </Modal>
    </>
  );
}

const menuRow =
  'flex w-full items-center gap-2.5 px-3 py-2 rounded-sm text-left no-underline hover:no-underline hover:bg-gray-50 bg-transparent border-0 cursor-pointer text-body focus-visible:outline-none focus-visible:bg-gray-100';

export function AccountMenu({ shopName }: { shopName: string }) {
  const { t } = useT();
  const navigate = useNavigate();
  return (
    <Popover
      role="menu"
      aria-label={t('seller.shell.account')}
      panelClassName="w-[260px] p-1"
      trigger={(p) => (
        <button
          type="button"
          {...p}
          aria-label={t('seller.shell.account')}
          className="border-0 bg-transparent p-0 cursor-pointer rounded-full focus-visible:outline-none focus-visible:shadow-focus"
        >
          <Avatar name={shopName} size={36} />
        </button>
      )}
    >
      {(close) => (
        <>
          <button
            type="button"
            role="menuitem"
            className={menuRow}
            onClick={() => {
              close();
              navigate('/settings');
            }}
          >
            <Avatar name={shopName} size={36} />
            <span className="min-w-0">
              <span className="block font-bold text-sm text-strong truncate">{shopName}</span>
              <span className="block text-xs text-subtle">{t('seller.shell.manage_shop')}</span>
            </span>
          </button>
          <div className="h-px bg-line-subtle my-1" />
          <a role="menuitem" href="/" className={menuRow}>
            <Avatar name={t('seller.shell.buyer_channel')} size={32} />
            <span className="flex-1 min-w-0">
              <span className="block font-semibold text-sm text-strong">
                {t('seller.shell.buyer_channel')}
              </span>
              <span className="block text-[11px] text-subtle">
                {t('seller.shell.back_to_genie')}
              </span>
            </span>
            <Icon name="refresh-cw" size={14} className="text-muted" />
          </a>
          <div className="h-px bg-line-subtle my-1" />
          <button
            type="button"
            role="menuitem"
            className={`${menuRow} !text-red-600 font-semibold text-sm`}
            onClick={() => void signOut()}
          >
            <span className="w-8 flex justify-center">
              <Icon name="log-out" size={18} />
            </span>
            {t('seller.shell.sign_out')}
          </button>
        </>
      )}
    </Popover>
  );
}

export function SellerSearch() {
  const { t } = useT();
  const navigate = useNavigate();
  const [q, setQ] = useState('');
  const [open, setOpen] = useState(false);
  const listId = useId();
  const search = useSellerSearch(q);
  const hits = search.data;
  const show = open && searchable(q);
  const go = (to: string) => {
    setOpen(false);
    setQ('');
    navigate(to);
  };
  const row =
    'flex items-center gap-2.5 w-full border-0 bg-transparent px-3 py-2 cursor-pointer text-left text-sm text-body rounded-sm hover:bg-gray-50 focus-visible:outline-none focus-visible:bg-gray-100';
  const head = 'text-[11px] font-bold tracking-[.04em] uppercase text-subtle px-3 pt-2 pb-1';
  return (
    <div className="relative flex items-center max-md:w-full">
      <Icon name="search" size={16} className="absolute left-3 text-subtle pointer-events-none" />
      <input
        value={q}
        onChange={(e) => {
          setQ(e.target.value);
          setOpen(true);
        }}
        onFocus={() => setOpen(true)}
        onBlur={() => setTimeout(() => setOpen(false), 150)}
        onKeyDown={(e) => e.key === 'Escape' && setOpen(false)}
        role="combobox"
        aria-expanded={show}
        aria-controls={listId}
        aria-autocomplete="list"
        aria-label={t('seller.shell.search')}
        placeholder={t('seller.shell.search_placeholder')}
        className="w-[260px] max-md:w-full h-10 pl-9 pr-3 border-[1.5px] border-solid border-line rounded-md text-sm bg-surface-card text-body outline-none focus:border-primary"
      />
      {show && (
        <div
          id={listId}
          role="listbox"
          className="absolute top-[calc(100%+6px)] left-0 w-[340px] max-w-[calc(100vw-24px)] bg-surface-card border border-solid border-line rounded-md shadow-xl z-dropdown p-1"
        >
          {search.isFetching && !hits ? (
            <div className="px-3 py-3.5 text-sm text-muted">{t('common.state.loading')}</div>
          ) : !hits || (hits.orders.length === 0 && hits.products.length === 0) ? (
            <div className="px-3 py-3.5 text-sm text-muted">{t('seller.shell.search_empty')}</div>
          ) : (
            <>
              {hits.orders.length > 0 && (
                <div className={head}>{t('seller.shell.search_orders')}</div>
              )}
              {hits.orders.map((o) => (
                <button
                  key={o.id}
                  type="button"
                  role="option"
                  aria-selected={false}
                  className={row}
                  onMouseDown={(e) => e.preventDefault()}
                  onClick={() => go(`/orders?q=${encodeURIComponent(o.code)}`)}
                >
                  <Icon name="package" size={16} className="text-muted flex-none" />
                  <span className="font-mono text-xs">{o.code}</span>
                  <span className="ml-auto text-muted truncate">{o.customerName}</span>
                </button>
              ))}
              {hits.products.length > 0 && (
                <div className={head}>{t('seller.shell.search_products')}</div>
              )}
              {hits.products.map((p) => (
                <button
                  key={p.id}
                  type="button"
                  role="option"
                  aria-selected={false}
                  className={row}
                  onMouseDown={(e) => e.preventDefault()}
                  onClick={() => go(`/products/${p.id}/edit`)}
                >
                  <img
                    src={p.imageUrl}
                    alt=""
                    width={28}
                    height={28}
                    className="w-7 h-7 rounded-sm object-cover flex-none"
                  />
                  <span className="flex-1 min-w-0 truncate">{p.name}</span>
                </button>
              ))}
            </>
          )}
        </div>
      )}
    </div>
  );
}
