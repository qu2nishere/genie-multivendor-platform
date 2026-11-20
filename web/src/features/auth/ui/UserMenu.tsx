import { useState, type ReactNode } from 'react';
import { isInternalUser, type SessionUser } from '@/shared/auth/session';
import { useT } from '@/shared/hooks/useT';
import { Avatar, Button, ConfirmDialog, Dropdown, Icon } from '@/shared/ui';
import { signOut } from '../application/useAuth';

/**
 * Nút tài khoản trên header + menu (prototype Header.jsx UserMenu) — plan G07.16:
 * Xem hồ sơ, Tài khoản của tôi, Đơn mua, chuyển sang Kênh người bán (chủ shop), chuyển sang Admin Console
 * (ADMIN / STAFF — plan B1), Đăng xuất (có xác nhận).
 */
/** Nhãn hai dòng của mục chuyển kênh (Kênh người bán / Admin Console) + biểu tượng xoay chuyển ở cuối. */
function SwitchLabel({ title, hint }: { title: ReactNode; hint: ReactNode }) {
  return (
    <span className="flex min-w-0 flex-1 items-center gap-2.5">
      <span className="flex min-w-0 flex-1 flex-col">
        <span className="font-semibold text-[13px] text-strong">{title}</span>
        <span className="whitespace-nowrap text-[11px] text-subtle">{hint}</span>
      </span>
      <Icon name="refresh-cw" size={14} className="flex-none text-muted" />
    </span>
  );
}

export function UserMenu({ user, shopName }: { user: SessionUser; shopName?: string }) {
  const { t } = useT();
  const [confirm, setConfirm] = useState(false);
  const isVendor = user.roles.includes('VENDOR');
  // Nhân viên / Admin: lối sang Admin Console (quyền thật kiểm ở server; menu bên trong lọc theo /admin/me)
  // (tài khoản nội bộ không mua hàng — ẩn Tài khoản của tôi / Đơn mua, plan B1 D16 hướng B)
  const isStaff = isInternalUser(user);
  const logout = () => void signOut();
  return (
    <>
      <Dropdown
        aria-label={t('auth.menu.label')}
        panelClassName="min-w-[280px]"
        trigger={(p) => (
          <button type="button" className="tr-user" {...p}>
            <Avatar name={user.name} src={user.avatar_url} size={30} />
            <span className="tr-user__name whitespace-nowrap">{user.name}</span>
          </button>
        )}
        items={[
          ...(isStaff
            ? []
            : [
                {
                  key: 'profile',
                  label: t('auth.menu.profile'),
                  icon: 'user' as const,
                  href: '/account/profile',
                },
                {
                  key: 'orders',
                  label: t('auth.menu.orders'),
                  icon: 'package' as const,
                  href: '/orders',
                },
              ]),
          ...(isVendor
            ? [
                {
                  key: 'seller',
                  label: (
                    <SwitchLabel
                      title={shopName ?? t('auth.menu.seller')}
                      hint={t('auth.menu.switch')}
                    />
                  ),
                  icon: 'store' as const,
                  href: '/seller',
                  divider: true,
                },
              ]
            : []),
          ...(isStaff
            ? [
                {
                  key: 'admin',
                  label: (
                    <SwitchLabel title={t('auth.menu.admin')} hint={t('auth.menu.admin_switch')} />
                  ),
                  icon: 'layout-dashboard' as const,
                  href: '/admin',
                  // tài khoản nội bộ: đây là mục đầu tiên (không có Tài khoản của tôi / Đơn mua phía trên)
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
