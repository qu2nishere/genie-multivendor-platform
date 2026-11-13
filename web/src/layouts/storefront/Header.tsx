// Header storefront (prototype Header.jsx) — plan G07.12–G07.18. Island client:load.
// Thanh kính (logo, tìm kiếm, Bán hàng, ngôn ngữ, hỗ trợ, theme, chuông, giỏ, tài khoản) + hàng danh mục.
// Mọi điều hướng là <a href> thật (R16).
import { useStore } from '@nanostores/react';
import {
  useEffect,
  useRef,
  useState,
  type CSSProperties,
  type MouseEvent,
  type ReactNode,
} from 'react';
import { Providers } from '@/shared/providers/Providers';
import { LoginButton, LoginPrompt, requireLogin, UserMenu } from '@/features/auth';
import { $cartCount, $cartLines, loadCart, loadGuestCart, MiniCart } from '@/features/cart';
import { badgeText, NotifPanel, useNotifications } from '@/features/notifications';
import { SearchBox } from '@/features/search';
import { $session } from '@/shared/auth/session';
import { useT } from '@/shared/hooks/useT';
import { GenieMark, GenieWordmark } from '@/shared/ui/brand';
import { Icon, Modal, toIconName } from '@/shared/ui';
import LangSelect from './LangSelect';
import ThemePickerSlot from './ThemePickerSlot';

export interface HeaderCategory {
  slug: string;
  name: string;
  icon: string;
  children?: { slug: string; name: string }[];
}

export interface HeaderProps {
  categories: HeaderCategory[];
  /** Cờ cookie genie_logged_in khi SSR — để HTML máy chủ và lần hydrate đầu khớp nhau. */
  loggedInHint: boolean;
  /** Đường dẫn hiện tại (cho ?next= của nút Đăng nhập). */
  path: string;
  initialQuery?: string;
}

/** Hiện bảng nổi khi rê chuột (như prototype); bấm vẫn là liên kết tới trang đầy đủ. */
function HoverPanel({
  trigger,
  panel,
  enabled,
}: {
  trigger: ReactNode;
  panel: ReactNode;
  enabled: boolean;
}) {
  const [open, setOpen] = useState(false);
  return (
    <div
      className="relative"
      onMouseEnter={() => enabled && setOpen(true)}
      onMouseLeave={() => setOpen(false)}
      onKeyDown={(e) => e.key === 'Escape' && setOpen(false)}
    >
      {trigger}
      {open && (
        <div className="absolute top-full right-0 pt-2 z-dropdown cursor-default">
          <div className="bg-surface-card rounded-lg shadow-xl border border-line-subtle overflow-hidden">
            {panel}
          </div>
        </div>
      )}
    </div>
  );
}

function HeaderBody({ categories, loggedInHint, path, initialQuery }: HeaderProps) {
  const { t } = useT();
  // Lần render đầu phải khớp HTML server: island khác có thể đã khởi động phiên / nạp giỏ trước khi
  // Header hydrate, nên chỉ đọc trạng thái thật sau khi mount (tránh lỗi hydration).
  const [mounted, setMounted] = useState(false);
  useEffect(() => setMounted(true), []);
  const liveSession = useStore($session);
  const session = mounted ? liveSession : ({ status: 'unknown' } as const);
  const loggedIn =
    session.status === 'authenticated' || (session.status === 'unknown' && loggedInHint);
  const user = session.status === 'authenticated' ? session.user : null;
  const lines = useStore($cartLines);
  const liveCount = useStore($cartCount);
  const cartCount = mounted ? liveCount : 0;
  const notif = useNotifications(session.status === 'authenticated');
  const [drawer, setDrawer] = useState(false);

  useEffect(() => {
    if (session.status === 'authenticated') void loadCart();
    else if (session.status === 'guest') loadGuestCart();
  }, [session.status]);

  // Thanh + hàng danh mục dính chung; khi cuộn thì lấy nền theme (--top-bg). Công bố chiều cao qua --top-h.
  const top = useRef<HTMLDivElement>(null);
  const [stuck, setStuck] = useState(false);
  useEffect(() => {
    const onScroll = () => setStuck(window.scrollY > 4);
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
    const el = top.current;
    const ro =
      el && typeof ResizeObserver !== 'undefined'
        ? new ResizeObserver(() =>
            document.documentElement.style.setProperty(
              '--top-h',
              `${Math.round(el.getBoundingClientRect().height)}px`,
            ),
          )
        : null;
    if (el && ro) ro.observe(el);
    return () => {
      window.removeEventListener('scroll', onScroll);
      ro?.disconnect();
    };
  }, []);

  const unread = loggedIn ? notif.unread : 0;
  const guard = (e: MouseEvent) => {
    if (!loggedIn) {
      e.preventDefault();
      requireLogin();
    }
  };

  return (
    <div ref={top} className="tr-top" data-stuck={stuck ? 1 : 0}>
      <header className="tr-pillwrap">
        <div className="tr-pill tr-glass">
          <a
            className="tr-logo no-underline hover:no-underline"
            href="/"
            aria-label={t('layout.header.home')}
          >
            <GenieMark size={34} />
            <GenieWordmark fontPx={34} className="tr-logo__word" />
          </a>
          <SearchBox initialQuery={initialQuery} />
          <div className="tr-actions">
            <a
              className="tr-textbtn tr-textbtn--wide no-underline hover:no-underline"
              href="/become-seller"
            >
              {t('layout.header.sell')}
            </a>
            <span className="tr-textbtn--wide inline-flex">
              <LangSelect />
            </span>
            <a
              className="tr-iconbtn tr-textbtn--wide"
              href="/policies/help-center"
              aria-label={t('layout.header.help')}
              title={t('layout.header.help')}
            >
              <Icon name="circle-help" size={22} />
            </a>
            <ThemePickerSlot />
            <HoverPanel
              enabled={loggedIn}
              trigger={
                <a
                  className="tr-iconbtn"
                  href="/account/notifications"
                  onClick={guard}
                  aria-label={
                    unread > 0
                      ? t('layout.header.notifications_unread', { count: unread })
                      : t('layout.header.notifications')
                  }
                >
                  <Icon name="bell" size={22} />
                  {unread > 0 && <span className="tr-badge">{badgeText(unread)}</span>}
                </a>
              }
              panel={<NotifPanel items={notif.items} />}
            />
            <HoverPanel
              enabled={loggedIn}
              trigger={
                <a
                  className="tr-iconbtn"
                  href="/cart"
                  onClick={guard}
                  aria-label={
                    cartCount > 0
                      ? t('layout.header.cart_count', { count: cartCount })
                      : t('layout.header.cart')
                  }
                >
                  <Icon name="shopping-cart" size={22} />
                  {cartCount > 0 && <span className="tr-badge">{cartCount}</span>}
                </a>
              }
              panel={<MiniCart lines={lines} />}
            />
            <div className="relative ml-1">
              {user ? (
                <UserMenu user={user} shopName={user.shop_name} />
              ) : loggedIn ? (
                <span className="tr-user" aria-busy="true">
                  <span className="w-[30px] h-[30px] rounded-full bg-gray-200 animate-pulse" />
                </span>
              ) : (
                <LoginButton next={path} />
              )}
            </div>
          </div>
        </div>
      </header>
      <nav className="tr-cats" aria-label={t('layout.header.categories')}>
        <button
          type="button"
          className="tr-cat md:hidden"
          onClick={() => setDrawer(true)}
          aria-haspopup="dialog"
          aria-expanded={drawer}
        >
          <Icon name="menu" size={15} /> {t('layout.header.all_categories')}
        </button>
        {categories.map((c, i) => (
          <a
            key={c.slug}
            className="tr-cat no-underline hover:no-underline"
            style={{ '--i': i } as CSSProperties}
            href={`/c/${c.slug}`}
          >
            <Icon name={toIconName(c.icon)} size={15} /> {c.name}
          </a>
        ))}
      </nav>
      <Modal
        open={drawer}
        onClose={() => setDrawer(false)}
        title={t('layout.header.all_categories')}
        width={420}
      >
        <ul className="list-none m-0 p-0 flex flex-col">
          {categories.map((c) => (
            <li key={c.slug} className="border-b border-line-subtle last:border-0">
              <a
                href={`/c/${c.slug}`}
                className="flex items-center gap-2.5 min-h-[44px] font-semibold text-base text-strong no-underline"
              >
                <Icon name={toIconName(c.icon)} size={18} className="text-brand-500" />
                {c.name}
              </a>
              {c.children && c.children.length > 0 && (
                <ul className="list-none m-0 pl-7 pb-2 flex flex-wrap gap-x-4">
                  {c.children.map((s) => (
                    <li key={s.slug}>
                      <a
                        href={`/c/${s.slug}`}
                        className="inline-flex items-center min-h-[44px] text-sm text-body"
                      >
                        {s.name}
                      </a>
                    </li>
                  ))}
                </ul>
              )}
            </li>
          ))}
        </ul>
      </Modal>
      <LoginPrompt />
    </div>
  );
}

export default function Header(props: HeaderProps) {
  return (
    <Providers>
      <HeaderBody {...props} />
    </Providers>
  );
}
