import { useState } from 'react';
import type { SessionUser } from '@/shared/auth/session';
import { useT } from '@/shared/hooks/useT';
import { Avatar, Button, ConfirmDialog, Dropdown, Icon } from '@/shared/ui';
import { signOut } from '../application/useAuth';

/**
 * Nút tài khoản trên header + menu (prototype Header.jsx UserMenu) — plan G07.16:
 * Xem hồ sơ, Tài khoản của tôi, Đơn mua, chuyển sang Kênh người bán (chủ shop), Đăng xuất (có xác nhận).
 */
export function UserMenu({ user, shopName }: { user: SessionUser; shopName?: string }) {
  const { t } = useT();
  const [confirm, setConfirm] = useState(false);
  const isVendor = user.roles.includes('VENDOR');
  const logout = () => void signOut();
  return (
    <>
      <Dropdown
        aria-label={t('auth.menu.label')}
        trigger={(p) => (
          <button type="button" className="tr-user" {...p}>
            <Avatar name={user.name} src={user.avatar_url} size={30} />
            <span className="tr-user__name whitespace-nowrap">{user.name}</span>
          </button>
        )}
        items={[
          { key: 'profile', label: t('auth.menu.profile'), icon: 'user', href: '/account/profile' },
          { key: 'orders', label: t('auth.menu.orders'), icon: 'package', href: '/orders' },
          ...(isVendor
            ? [
                {
                  key: 'seller',
                  label: (
                    <span className="flex flex-col">
                      <span className="font-semibold text-[13px] text-strong">
                        {shopName ?? t('auth.menu.seller')}
                      </span>
                      <span className="text-[11px] text-subtle">{t('auth.menu.switch')}</span>
                    </span>
                  ),
                  icon: 'store' as const,
                  href: '/seller',
                  divider: true,
                },
              ]
            : []),
          {
            key: 'logout',
            label: t('common.action.logout'),
            icon: 'log-out',
            danger: true,
            divider: true,
            onSelect: () => setConfirm(true),
          },
        ]}
      />
      <ConfirmDialog
        open={confirm}
        tone="danger"
        title={t('common.action.logout')}
        confirmLabel={
          <span className="inline-flex items-center gap-2">
            <Icon name="log-out" size={18} />
            {t('common.action.logout')}
          </span>
        }
        onCancel={() => setConfirm(false)}
        onConfirm={logout}
      >
        <div className="flex gap-3 items-start">
          <span className="w-10 h-10 rounded-full bg-red-50 text-red-600 grid place-items-center flex-none">
            <Icon name="log-out" size={20} />
          </span>
          <p className="m-0 text-base text-body leading-normal">
            {t('auth.menu.logout_confirm', { name: user.name })}
          </p>
        </div>
      </ConfirmDialog>
    </>
  );
}

/** Nút "Đăng nhập" cho khách trên header; `next` do trang truyền vào (giống nhau khi SSR và hydrate). */
export function LoginButton({ next = '/' }: { next?: string }) {
  const { t } = useT();
  return (
    <Button href={`/auth/login?next=${encodeURIComponent(next)}`}>
      {t('common.action.login')}
    </Button>
  );
}
