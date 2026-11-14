// Khung Admin Portal (prototype AdminShell.jsx) — plan G12.03: thanh bên tối 18 mục lọc theo quyền,
// huy hiệu hàng chờ (G12.05), chip quyền, thẻ tài khoản ở đáy; thanh trên (tiêu đề, "Staff RBAC",
// ngôn ngữ, theme, chuông, Tài khoản). Màn hẹp: thanh bên thành dải trên, nav cuộn ngang (như Seller).
// Bỏ ô "Xem với vai trò" (PREVIEW_ROLES — D5).
import { useEffect, useRef, type ReactNode } from 'react';
import { Link, NavLink, useLocation } from 'react-router-dom';
import { badgeLabel, useAdminQueues } from '@/features/admin-dashboard';
import type { AdminMe } from '@/features/admin-system';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { switchLocale, type Locale } from '@/shared/lib/locale';
import { Avatar, Badge, Icon } from '@/shared/ui';
import { GenieLogo, ThemePicker } from '@/shared/ui/brand';
import { AdminBell } from './AdminBell';
import { allowed, NAV, navOf, type AdminNavItem } from './nav';

const navCls =
  'flex flex-none items-center gap-[11px] px-3 h-11 rounded-md border-0 text-left text-[13.5px] no-underline hover:no-underline focus-visible:outline-none focus-visible:shadow-focus';

/** Nhãn chức danh: Admin / Moderator / … (dịch qua admin.perm.title.*). */
function useRoleLabel(me: AdminMe): string {
  const { t } = useT();
  return me.role === 'ADMIN'
    ? t('admin.perm.title.ADMIN')
    : t(`admin.perm.title.${me.staffTitle ?? 'CUSTOM'}`);
}

function Sidebar({ me, active }: { me: AdminMe; active: AdminNavItem | null }) {
  const { t } = useT();
  const queues = useAdminQueues();
  const role = useRoleLabel(me);
  const items = NAV.filter((n) => allowed(n, me.permissions));
  const navRef = useRef<HTMLElement>(null);
  // Màn hẹp: nav là dải cuộn ngang → đưa mục đang mở vào khung nhìn
  useEffect(() => {
    if (!window.matchMedia('(max-width: 768px)').matches) return;
    navRef.current
      ?.querySelector('[aria-current="page"]')
      ?.scrollIntoView({ inline: 'center', block: 'nearest' });
  }, [active?.section]);
  return (
    <aside className="bg-[var(--side-bg,var(--ink-950))] flex flex-col sticky top-0 h-screen max-md:static max-md:h-auto">
      <div className="px-[18px] pt-4 pb-3.5">
        <Link to="/" aria-label={t('admin.shell.home')} className="inline-flex no-underline">
          <GenieLogo size={34} tone="light" sub="Admin Console" />
        </Link>
      </div>
      <div className="mx-3 mb-3 px-3 py-2 rounded-md flex items-center gap-2 bg-[rgba(11,127,171,.14)] border border-solid border-[rgba(11,127,171,.3)]">
        <Icon name="shield-check" size={15} className="text-blue-500" />
        <span className="text-[11px] font-semibold text-white/[.82]">
          {t('admin.shell.role_chip', { role })}
        </span>
      </div>
      <nav
        ref={navRef}
        aria-label={t('admin.shell.nav_label')}
        className="flex-1 min-h-0 px-3 py-1 flex flex-col gap-0.5 overflow-y-auto [scrollbar-width:thin] max-md:flex-row max-md:flex-none max-md:overflow-x-auto"
      >
        {items.map((n) => {
          const on = n.section === active?.section;
          const badge = n.badge ? badgeLabel(queues.data?.[n.badge]) : null;
          return (
            <NavLink
              key={n.section}
              to={n.to}
              end={n.to === '/'}
              aria-current={on ? 'page' : undefined}
              className={cn(
                navCls,
                on
                  ? 'bg-white/10 text-white font-semibold'
                  : 'text-white/[.62] font-medium hover:bg-white/[.06] hover:text-white',
              )}
            >
              <Icon name={n.icon} size={18} className={on ? 'text-accent' : undefined} />
              <span className="flex-1 max-md:whitespace-nowrap">
                {t(`admin.shell.nav.${n.section}`)}
              </span>
              {badge && (
                <span
                  className="bg-accent text-accent-on text-[11px] font-bold min-w-[18px] h-[18px] rounded-[9px] grid place-items-center px-[5px]"
                  aria-label={t('admin.shell.badge', { count: badge })}
                >
                  {badge}
                </span>
              )}
            </NavLink>
          );
        })}
      </nav>
      <Link
        to="/account"
        className="p-3 border-0 border-t border-solid border-white/[.08] flex items-center gap-2.5 no-underline hover:no-underline hover:bg-white/[.05] focus-visible:outline-none focus-visible:shadow-focus"
      >
        <Avatar name={me.fullName} size={34} />
        <span className="min-w-0">
          <span className="block text-xs font-semibold text-white truncate">{me.fullName}</span>
          <span className="block text-[11px] text-white/50">{role}</span>
        </span>
        <Icon name="settings" size={17} className="ml-auto text-white/50" />
      </Link>
    </aside>
  );
}

function LangSwitch() {
  const { t, locale } = useT();
  return (
    <select
      aria-label={t('layout.header.language')}
      value={locale}
      onChange={(e) => switchLocale(e.target.value as Locale)}
      className="h-8 border border-solid border-line rounded-sm bg-surface-card text-xs text-body px-1.5 cursor-pointer"
    >
      <option value="vi">Tiếng Việt</option>
      <option value="en">English</option>
    </select>
  );
}

export function AdminLayout({ me, children }: { me: AdminMe; children: ReactNode }) {
  const { t } = useT();
  const { pathname } = useLocation();
  const active = navOf(pathname);
  const title = active ? t(`admin.shell.nav.${active.section}`) : t('admin.shell.not_found.title');

  useEffect(() => {
    document.title = `${title} · Admin Console · Genie`;
    window.scrollTo(0, 0);
  }, [title, pathname]);

  return (
    <div className="grid grid-cols-[236px_minmax(0,1fr)] min-h-screen bg-surface-page max-md:grid-cols-1 max-md:grid-rows-[auto_1fr]">
      <Sidebar me={me} active={active} />
      <div className="flex flex-col min-w-0">
        <header className="h-16 bg-surface-card border-0 border-b border-solid border-line flex items-center gap-4 px-7 sticky top-0 z-sticky max-md:static max-md:h-auto max-md:flex-wrap max-md:px-3 max-md:py-2.5 max-md:gap-2.5">
          <h1 className="gn-page-title m-0 text-2xl font-bold text-strong">{title}</h1>
          <Badge variant="outline" className="ml-1">
            Staff RBAC
          </Badge>
          <div className="ml-auto flex items-center gap-2 max-md:ml-0 max-md:w-full">
            <LangSwitch />
            <ThemePicker className="gn-iconbtn" />
            <AdminBell />
            <Link to="/account" className="gn-iconbtn" aria-label={t('admin.shell.nav.account')}>
              <Icon name="settings" size={20} />
            </Link>
          </div>
        </header>
        <main className="flex-1 px-7 pt-6 pb-[60px] min-w-0 max-md:px-3 max-md:pt-3.5 max-md:pb-10">
          {children}
        </main>
      </div>
    </div>
  );
}
