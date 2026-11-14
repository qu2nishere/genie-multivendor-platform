// Khung Seller Center (prototype VendorShell.jsx) — plan G11.03: thanh bên tối 11 mục (huy hiệu đơn chờ
// xác nhận / chat chưa đọc, mục bị khóa khi chưa KYC), chip shop + trạng thái KYC, thanh trên (tiêu đề,
// tìm nhanh, ngôn ngữ, theme, chuông, trợ giúp, tài khoản). Màn hẹp: thanh bên thành dải trên cùng,
// mục nav cuộn ngang (prototype responsive.css). Bỏ bảng "Mô phỏng KYC" (D5).
import { useEffect, type ReactNode } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import { signOut } from '@/features/auth';
import {
  kycGroup,
  navBadge,
  sectionAllowed,
  type KycGroup,
  type SellerContext,
  type SellerSection,
} from '@/features/seller-settings';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { LOCALE_LABELS, LOCALES, switchLocale, type Locale } from '@/shared/lib/locale';
import { Icon, toast, type IconName } from '@/shared/ui';
import { GenieLogo, ThemePicker } from '@/shared/ui/brand';
import { AccountMenu, NotifBell, SellerSearch } from './HeaderParts';
import { KycGate } from './KycGate';
import { NAV, sectionOf } from './nav';

const KYC_CHIP: Record<KycGroup, { icon: IconName; cls: string }> = {
  VERIFIED: { icon: 'badge-check', cls: 'text-mint-100' },
  WAITING: { icon: 'clock', cls: 'text-gold-400' },
  NOT_SUBMITTED: { icon: 'clock', cls: 'text-gold-400' },
  REJECTED: { icon: 'triangle-alert', cls: 'text-flash-100' },
  SUPPLEMENT: { icon: 'triangle-alert', cls: 'text-flash-100' },
};

const navCls =
  'flex flex-none items-center gap-[11px] px-3 h-11 rounded-md border-0 text-left text-sm no-underline hover:no-underline focus-visible:outline-none focus-visible:shadow-focus';

function Sidebar({ ctx, active }: { ctx: SellerContext; active: SellerSection | null }) {
  const { t } = useT();
  const g = kycGroup(ctx.kycStatus);
  const chip = KYC_CHIP[g];
  return (
    <aside className="bg-[var(--side-bg,var(--ink-900))] flex flex-col sticky top-0 h-screen max-md:static max-md:h-auto">
      <div className="px-[18px] pt-4 pb-3.5">
        <a href="/seller" aria-label={t('seller.shell.home')} className="inline-flex no-underline">
          <GenieLogo size={34} tone="light" sub="Seller Center" />
        </a>
      </div>
      <div className="mx-3 mb-3 px-3 py-2.5 bg-white/[.06] rounded-md flex items-center gap-2.5">
        <div className="w-[34px] h-[34px] rounded-sm bg-primary text-white grid place-items-center font-extrabold text-[15px] flex-none">
          {ctx.shop.name.trim().charAt(0).toUpperCase()}
        </div>
        <div className="min-w-0">
          <div className="font-semibold text-[13px] text-white truncate">{ctx.shop.name}</div>
          <div className={cn('inline-flex items-center gap-1 text-[11px]', chip.cls)}>
            <Icon name={chip.icon} size={12} />
            {t(`seller.shell.kyc.${g}`)}
          </div>
        </div>
      </div>
      <nav
        aria-label={t('seller.shell.nav_label')}
        className="flex-1 min-h-0 px-3 py-1 flex flex-col gap-0.5 overflow-y-auto [scrollbar-width:thin] max-md:flex-row max-md:flex-none max-md:overflow-x-auto"
      >
        {NAV.map((n) => {
          const on = n.section === active;
          const ok = sectionAllowed(n.section, ctx.kycStatus);
          const badge = navBadge(n.section, ctx);
          const label = t(`seller.shell.nav.${n.section}`);
          if (!ok)
            return (
              <button
                key={n.section}
                type="button"
                aria-disabled="true"
                onClick={() => toast.info(t('seller.shell.locked_toast'))}
                className={cn(
                  navCls,
                  'bg-transparent cursor-not-allowed text-white/30 font-medium',
                )}
              >
                <Icon name={n.icon} size={19} />
                <span className="flex-1 whitespace-nowrap">{label}</span>
                <Icon name="lock" size={14} className="opacity-70" />
              </button>
            );
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
                  : 'text-white/[.66] font-medium hover:bg-white/[.06] hover:text-white',
              )}
            >
              <Icon name={n.icon} size={19} className={on ? 'text-accent' : undefined} />
              <span className="flex-1 whitespace-nowrap">{label}</span>
              {badge > 0 && (
                <span
                  className="bg-accent text-accent-on text-[11px] font-bold min-w-[18px] h-[18px] rounded-[9px] grid place-items-center px-[5px]"
                  aria-label={t('seller.shell.badge', { count: badge })}
                >
                  {badge}
                </span>
              )}
            </NavLink>
          );
        })}
      </nav>
      <div className="p-3">
        <button
          type="button"
          onClick={() => void signOut()}
          className={cn(
            navCls,
            'w-full bg-transparent cursor-pointer text-white/50 font-medium hover:text-white',
          )}
        >
          <Icon name="log-out" size={19} />
          {t('seller.shell.sign_out')}
        </button>
      </div>
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
      {LOCALES.map((l) => (
        <option key={l} value={l} lang={l}>
          {LOCALE_LABELS[l]}
        </option>
      ))}
    </select>
  );
}

export function SellerLayout({ ctx, children }: { ctx: SellerContext; children: ReactNode }) {
  const { t } = useT();
  const { pathname } = useLocation();
  const section = sectionOf(pathname);
  const title = section ? t(`seller.shell.nav.${section}`) : t('seller.shell.not_found.title');
  const locked = section !== null && !sectionAllowed(section, ctx.kycStatus);

  useEffect(() => {
    document.title = `${title} · Seller Center · Genie`;
    window.scrollTo(0, 0);
  }, [title, pathname]);

  return (
    <div className="grid grid-cols-[232px_minmax(0,1fr)] min-h-screen bg-surface-page max-md:grid-cols-1">
      <Sidebar ctx={ctx} active={section} />
      <div className="flex flex-col min-w-0">
        <header className="h-16 bg-surface-card border-0 border-b border-solid border-line flex items-center gap-4 px-7 sticky top-0 z-sticky max-md:static max-md:h-auto max-md:flex-wrap max-md:px-3 max-md:py-2.5 max-md:gap-2.5">
          <h1 className="gn-page-title m-0 text-2xl font-bold text-strong max-md:w-full">
            {title}
          </h1>
          <div className="ml-auto flex items-center gap-2 max-md:ml-0 max-md:flex-wrap max-md:w-full">
            <SellerSearch />
            <LangSwitch />
            <ThemePicker className="gn-iconbtn" />
            <NotifBell />
            <a
              href="/policies/help-center"
              className="gn-iconbtn"
              aria-label={t('seller.shell.help')}
            >
              <Icon name="circle-help" size={20} />
            </a>
            <div className="w-px h-7 bg-line mx-1" aria-hidden="true" />
            <AccountMenu shopName={ctx.shop.name} />
          </div>
        </header>
        <main className="flex-1 px-7 pt-6 pb-[60px] min-w-0 max-md:px-3 max-md:pt-3.5 max-md:pb-10">
          {locked ? <KycGate status={ctx.kycStatus} /> : children}
        </main>
      </div>
    </div>
  );
}
