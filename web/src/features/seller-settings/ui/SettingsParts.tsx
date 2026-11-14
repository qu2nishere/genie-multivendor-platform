// Khối dùng chung của màn Cài đặt (prototype VendorPages.jsx Section, ToggleRow) + phần Bảo mật (G11.44:
// đăng nhập bằng gì, đặt / đổi mật khẩu với OTP — dùng lại PasswordForm của khu tài khoản vì shop chạy trên
// chính tài khoản mua hàng của chủ shop).
import type { ReactNode } from 'react';
import { PasswordForm, useLoginMethods } from '@/features/account';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { Icon, Skeleton, Switch, type IconName } from '@/shared/ui';

export function Section({
  id,
  title,
  icon,
  children,
}: {
  id: string;
  title: ReactNode;
  icon: IconName;
  children: ReactNode;
}) {
  return (
    <section
      id={id}
      aria-labelledby={`${id}-t`}
      className="bg-surface-card rounded-lg shadow-sm p-5 scroll-mt-24 max-sm:p-4"
    >
      <h2
        id={`${id}-t`}
        className="m-0 mb-4 flex items-center gap-2 font-bold text-base text-strong"
      >
        <Icon name={icon} size={18} className="text-brand-500" />
        {title}
      </h2>
      {children}
    </section>
  );
}

export function ToggleRow({
  label,
  desc,
  on,
  onToggle,
  danger,
  disabled,
}: {
  label: ReactNode;
  desc: ReactNode;
  on: boolean;
  onToggle: () => void;
  danger?: boolean;
  disabled?: boolean;
}) {
  return (
    <div className="flex items-center gap-3 py-[11px] border-0 border-b border-solid border-line-subtle">
      <div className="flex-1 min-w-0">
        <div className={cn('font-semibold text-sm', danger ? 'text-red-600' : 'text-strong')}>
          {label}
        </div>
        <div className="text-xs text-muted">{desc}</div>
      </div>
      <Switch
        checked={on}
        disabled={disabled}
        onChange={onToggle}
        aria-label={typeof label === 'string' ? label : undefined}
      />
    </div>
  );
}

export function SecuritySection() {
  const { t } = useT();
  const m = useLoginMethods();
  const has = m.data?.password ?? true;
  const google = !!m.data?.identities.some((i) => i.provider === 'GOOGLE');
  const chip = (on: boolean, icon: ReactNode, label: string) => (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 px-2.5 py-1 rounded-pill border border-solid border-line font-semibold text-xs',
        on ? 'text-strong bg-surface-card' : 'text-subtle bg-transparent',
      )}
    >
      {icon}
      {label}
      <Icon
        name={on ? 'circle-check' : 'x'}
        size={13}
        className={on ? 'text-mint-600' : 'text-subtle'}
      />
    </span>
  );
  return (
    <Section
      id="security"
      icon="shield-check"
      title={t(has ? 'seller-settings.security.change' : 'seller-settings.security.set')}
    >
      {!m.data ? (
        <Skeleton lines={4} />
      ) : (
        <>
          <div className="flex items-center gap-2 flex-wrap mb-4">
            <span className="text-sm text-muted">{t('seller-settings.security.login_with')}</span>
            {chip(has, <Icon name="mail" size={13} />, t('seller-settings.security.email'))}
            {chip(google, <span className="font-bold text-[11px] text-blue-600">G</span>, 'Google')}
            <a
              href="/account/logins"
              className="ml-auto font-semibold text-sm text-brand-600 max-sm:ml-0"
            >
              {t('seller-settings.security.manage')}
            </a>
          </div>
          <PasswordForm key={String(has)} hasPassword={has} />
          <p className="m-0 mt-3 text-xs text-muted leading-normal">
            {t(has ? 'seller-settings.security.note' : 'seller-settings.security.note_set')}
          </p>
        </>
      )}
    </Section>
  );
}
