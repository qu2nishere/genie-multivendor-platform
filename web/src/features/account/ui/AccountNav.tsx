// Menu trái khu Tài khoản (prototype Account.jsx MENU + sidebar) — plan G10.44: người dùng, 2 nhóm mục
// (liên kết thật — R16), huy hiệu số, Đăng xuất có xác nhận. Màn hẹp: hàng cuộn ngang (account.css).
import { useStore } from '@nanostores/react';
import { useEffect, useRef, useState } from 'react';
import { signOut } from '@/features/auth';
import { useNotifications } from '@/features/notifications';
import { useMyVouchers } from '@/features/promotions';
import { $session } from '@/shared/auth/session';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { Providers } from '@/shared/providers/Providers';
import { Avatar, ConfirmDialog, Icon, type IconName } from '@/shared/ui';
import { useLoginMethods } from '../application/useAccount';
import { ACCOUNT_MENU, type AccountSection } from '../domain/account.menu';

export interface AccountNavProps {
  active: AccountSection;
}

function Nav({ active }: AccountNavProps) {
  const { t } = useT();
  const session = useStore($session);
  // Đọc phiên sau khi mount để HTML SSR và lần hydrate trùng nhau (cùng cách Header)
  const [mounted, setMounted] = useState(false);
  useEffect(() => setMounted(true), []);
  // Màn hẹp: menu là hàng cuộn ngang — cuộn tới mục đang mở (prototype)
  const navRef = useRef<HTMLElement>(null);
  useEffect(() => {
    const nav = navRef.current;
    const cur = nav?.querySelector<HTMLElement>('[aria-current="page"]');
    if (nav && cur && window.matchMedia('(max-width: 768px)').matches)
      nav.scrollLeft = cur.offsetLeft - nav.offsetLeft - 12;
  }, []);
  const user = mounted && session.status === 'authenticated' ? session.user : null;
  const notifs = useNotifications(!!user);
  const vouchers = useMyVouchers(!!user);
  const [logout, setLogout] = useState(false);
  const badge = (id: AccountSection): number => {
    if (id === 'notifications') return notifs.unread;
    if (id === 'vouchers')
      return (vouchers.data ?? []).filter((v) => v.saved && v.status === 'active').length;
    return 0;
  };
  const methods = useLoginMethods(!!user);
  const hasPw = methods.data ? methods.data.password : user?.has_password !== false;

  return (
    <aside className="tr-acct sticky top-[calc(var(--top-h,86px)+12px)] max-md:static">
      <div className="tr-acct__who flex items-center gap-3 pb-4 mb-4 border-0 border-b border-solid border-line-subtle">
        <Avatar name={user?.name ?? '…'} src={user?.avatar_url ?? null} size={48} />
        <div className="min-w-0">
          <div className="font-bold text-sm text-strong truncate">{user?.name ?? '…'}</div>
          <div className="text-xs text-muted mt-px truncate">
            {methods.data
              ? t(
                  methods.data.password && methods.data.identities.length
                    ? 'account.menu.kind_both'
                    : methods.data.identities.length
                      ? 'account.menu.kind_google'
                      : 'account.menu.kind_email',
                )
              : (user?.email ?? '')}
          </div>
        </div>
      </div>
      <nav ref={navRef} className="tr-acct__menu" aria-label={t('account.menu.label')}>
        {ACCOUNT_MENU.map((sec) => (
          <div key={sec.group} className="mb-3.5">
            {sec.group === 'mine' && (
              <div className="tr-acct__group font-bold text-[11px] tracking-[.04em] uppercase text-subtle px-2 pb-2">
                {t('account.menu.mine')}
              </div>
            )}
            {sec.items.map((m) => {
              const on = m.id === active;
              const n = badge(m.id);
              const label =
                m.id === 'password' && !hasPw
                  ? t('account.menu.password_set')
                  : t(`account.menu.${m.id}`);
              return (
                <a
                  key={m.id}
                  href={m.href}
                  aria-current={on ? 'page' : undefined}
                  className={cn(
                    'flex items-center gap-2.5 h-10 px-2 rounded-md no-underline text-sm',
                    on
                      ? 'text-brand-600 bg-brand-50 font-semibold'
                      : 'text-body hover:bg-gray-50 hover:text-strong',
                  )}
                >
                  <Icon name={m.icon as IconName} size={17} />
                  <span className="flex-1">{label}</span>
                  {n > 0 && (
                    <span className="font-bold text-[11px] text-inverse bg-brand-900 rounded-[9px] min-w-[18px] h-[18px] grid place-items-center px-[5px]">
                      {n}
                    </span>
                  )}
                </a>
              );
            })}
          </div>
        ))}
        <div className="border-0 border-t border-solid border-line-subtle mt-1.5 pt-2.5">
          <button
            type="button"
            onClick={() => setLogout(true)}
            className="w-full flex items-center gap-2.5 h-10 px-2 rounded-md border-0 bg-transparent text-left text-sm text-red-600 cursor-pointer hover:bg-red-50"
          >
            <Icon name="log-out" size={17} />
            <span className="flex-1">{t('common.action.logout')}</span>
          </button>
        </div>
      </nav>
      <ConfirmDialog
        open={logout}
        tone="danger"
        title={t('common.action.logout')}
        message={t('account.menu.logout_confirm', { name: user?.name ?? '' })}
        confirmLabel={t('common.action.logout')}
        cancelLabel={t('common.action.cancel')}
        onCancel={() => setLogout(false)}
        onConfirm={() => void signOut()}
      />
    </aside>
  );
}

export default function AccountNav(props: AccountNavProps) {
  return (
    <Providers>
      <Nav {...props} />
    </Providers>
  );
}
