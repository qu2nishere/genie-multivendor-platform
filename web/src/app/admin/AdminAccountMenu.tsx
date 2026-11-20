// Menu tài khoản trên thanh trên Admin Console (plan B1 — 2026-11-20): ảnh + tên + chức danh của người đang đăng nhập
// (thay thẻ tài khoản ở đáy thanh bên). Bên trong: Tài khoản của tôi (/admin/account), chuyển sang trang mua sắm
// (storefront — cùng tài khoản, D16 cho phép mua hàng), Đăng xuất. Cùng kiểu AccountMenu của Seller Center.
import { useNavigate } from 'react-router-dom';
import { signOut } from '@/features/auth';
import type { AdminMe } from '@/features/admin-system';
import { useT } from '@/shared/hooks/useT';
import { Avatar, Icon, Popover } from '@/shared/ui';

const menuRow =
  'flex w-full items-center gap-2.5 px-3 py-2 rounded-sm text-left no-underline hover:no-underline hover:bg-gray-50 bg-transparent border-0 cursor-pointer text-body focus-visible:outline-none focus-visible:bg-gray-100';

export function AdminAccountMenu({ me, roleLabel }: { me: AdminMe; roleLabel: string }) {
  const { t } = useT();
  const navigate = useNavigate();
  return (
    <Popover
      role="menu"
      aria-label={t('admin.shell.account_menu')}
      panelClassName="w-[280px] p-1"
      trigger={(p) => (
        <button
          type="button"
          {...p}
          aria-label={t('admin.shell.account_menu')}
          className="flex items-center gap-2.5 h-10 pl-1 pr-2.5 rounded-full border border-solid border-line bg-surface-card cursor-pointer hover:border-primary focus-visible:outline-none focus-visible:shadow-focus"
        >
          <Avatar name={me.fullName} src={me.avatarUrl} size={32} />
          <span className="min-w-0 text-left max-md:hidden">
            <span className="block max-w-[160px] truncate text-[13px] font-semibold leading-tight text-strong">
              {me.fullName}
            </span>
            <span className="block text-[11px] leading-tight text-subtle">{roleLabel}</span>
          </span>
          <Icon name="chevron-down" size={15} className="text-muted max-md:hidden" />
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
              navigate('/account');
            }}
          >
            <Avatar name={me.fullName} src={me.avatarUrl} size={40} />
            <span className="min-w-0 flex-1">
              <span className="block truncate text-sm font-bold text-strong">{me.fullName}</span>
              <span className="block truncate text-xs text-subtle">{me.email}</span>
              <span className="mt-0.5 block text-[11px] font-semibold text-primary">
                {roleLabel}
              </span>
            </span>
          </button>
          <div className="my-1 h-px bg-line-subtle" />
          <button
            type="button"
            role="menuitem"
            className={`${menuRow} text-sm`}
            onClick={() => {
              close();
              navigate('/account');
            }}
          >
            <span className="flex w-8 justify-center">
              <Icon name="settings" size={18} />
            </span>
            {t('admin.shell.my_account')}
          </button>
          {/* Rời SPA /admin sang storefront — thẻ <a> thật (ngoài react-router basename /admin) */}
          <a role="menuitem" href="/" className={menuRow}>
            <span className="flex w-8 justify-center">
              <Icon name="store" size={18} />
            </span>
            <span className="min-w-0 flex-1">
              <span className="block text-sm font-semibold text-strong">
                {t('admin.shell.buyer_channel')}
              </span>
              <span className="block text-[11px] text-subtle">{t('admin.shell.to_store')}</span>
            </span>
            <Icon name="refresh-cw" size={14} className="text-muted" />
          </a>
          <div className="my-1 h-px bg-line-subtle" />
          <button
            type="button"
            role="menuitem"
            className={`${menuRow} !text-red-600 text-sm font-semibold`}
            onClick={() => void signOut()}
          >
            <span className="flex w-8 justify-center">
              <Icon name="log-out" size={18} />
            </span>
            {t('common.action.logout')}
          </button>
        </>
      )}
    </Popover>
  );
}
