// Bảo mật khu tài khoản (prototype Account.jsx PasswordPanel / LoginsPanel / DevicesPanel) — plan
// G10.50–G10.52: đổi / thiết lập mật khẩu (kiểm mật khẩu hiện tại khi gõ, độ mạnh, OTP bước 2), liên kết
// Google (không gỡ được khi là cách đăng nhập duy nhất), thiết bị đăng nhập (đăng xuất từ xa).
import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { errorMessage, isApiError } from '@/shared/api/errors';
import { USE_MOCK } from '@/shared/api/mock';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatRelative } from '@/shared/lib/format';
import { zodResolver } from '@/shared/lib/zodResolver';
import {
  Badge,
  Button,
  ConfirmDialog,
  Icon,
  PanelHead,
  PasswordInput,
  Skeleton,
  toast,
  type IconName,
} from '@/shared/ui';
import {
  useChangePassword,
  useDevices,
  useLinkGoogle,
  useLoginMethods,
  useLogoutDevices,
  useRequestStepUp,
  useUnlinkGoogle,
  useVerifyPassword,
} from '../application/useAccount';
import { passwordSchema, type PasswordFormValues } from '../domain/account.schema';
import type { StepUpChallenge } from '../domain/account.types';
import { panelClass } from './ProfilePanel';
import { StepUpOtp } from './StepUpOtp';

/** Form đặt / đổi mật khẩu (dùng ở Tài khoản và Cài đặt Seller Center — cùng tài khoản đăng nhập). */
export function PasswordForm({ hasPassword }: { hasPassword: boolean }) {
  const { t } = useT();
  const verify = useVerifyPassword();
  const step = useRequestStepUp();
  const change = useChangePassword();
  const [challenge, setChallenge] = useState<StepUpChallenge | null>(null);
  const [code, setCode] = useState('');
  const [otpErr, setOtpErr] = useState<string | null>(null);
  const [curState, setCurState] = useState<'checking' | 'ok' | 'bad' | null>(null);
  const [done, setDone] = useState(false);
  const form = useForm<PasswordFormValues>({
    resolver: zodResolver(passwordSchema(hasPassword)),
    defaultValues: { current: '', next: '', confirm: '' },
  });
  const cur = form.watch('current');
  const next = form.watch('next');
  const { errors } = form.formState;
  const err = (m?: string) => (m ? t(m) : undefined);

  // Kiểm mật khẩu hiện tại khi đang gõ (3.4 /me/password/verify), chờ 500 ms sau phím cuối
  useEffect(() => {
    if (!hasPassword || !cur) {
      setCurState(null);
      return undefined;
    }
    setCurState('checking');
    const id = window.setTimeout(
      () => verify.mutate(cur, { onSuccess: (ok) => setCurState(ok ? 'ok' : 'bad') }),
      500,
    );
    return () => window.clearTimeout(id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [cur, hasPassword]);

  const submit = form.handleSubmit(() =>
    step.mutate('PASSWORD_CHANGE', {
      onSuccess: (c) => {
        setChallenge(c);
        setCode('');
        setOtpErr(null);
      },
      onError: (e) => toast.error(errorMessage(e, t)),
    }),
  );
  const confirm = () =>
    challenge &&
    change.mutate(
      {
        current: hasPassword ? form.getValues('current') : null,
        next: form.getValues('next'),
        proof: { challengeId: challenge.id, code },
      },
      {
        onSuccess: () => {
          setChallenge(null);
          setDone(true);
          form.reset();
          toast.success(t(hasPassword ? 'account.password.changed' : 'account.password.set_done'));
          // Lần thiết lập đầu: menu (island khác) đổi sang "Đổi mật khẩu" sau khi đọc thông báo (prototype)
          if (!hasPassword) window.setTimeout(() => window.location.reload(), 2000);
        },
        onError: (e) => {
          if (isApiError(e) && e.code === 'GNE-AUTH-4011') {
            setChallenge(null);
            form.setError('current', { message: 'account.password.err.wrong' });
            return;
          }
          if (isApiError(e) && e.code === 'GNE-AUTH-4221') setChallenge(null);
          setOtpErr(errorMessage(e, t));
          setCode('');
        },
      },
    );

  return (
    <div className="grid grid-cols-[minmax(0,1fr)_280px] gap-7 mt-[18px] items-start max-md:grid-cols-1">
      <form className="flex flex-col gap-4" onSubmit={(e) => void submit(e)} noValidate>
        {hasPassword && (
          <div>
            <PasswordInput
              label={t('account.password.current')}
              autoComplete="current-password"
              error={err(errors.current?.message)}
              {...form.register('current')}
            />
            {curState && !errors.current && (
              <p
                className={cn(
                  'm-0 mt-1.5 text-xs',
                  curState === 'ok'
                    ? 'text-mint-600'
                    : curState === 'bad'
                      ? 'text-red-600'
                      : 'text-subtle',
                )}
                aria-live="polite"
              >
                {t(`account.password.check.${curState}`)}
              </p>
            )}
          </div>
        )}
        <PasswordInput
          label={t('account.password.new')}
          autoComplete="new-password"
          showStrength
          hint={t('account.password.hint')}
          error={err(errors.next?.message)}
          {...form.register('next')}
        />
        <PasswordInput
          label={t('account.password.confirm')}
          autoComplete="new-password"
          matchWith={next}
          error={err(errors.confirm?.message)}
          {...form.register('confirm')}
        />
        {!challenge && (
          <div>
            <Button type="submit" iconLeft="check" loading={step.isPending}>
              {t(hasPassword ? 'account.password.submit' : 'account.password.set_submit')}
            </Button>
          </div>
        )}
        {challenge && (
          <div className="border-0 border-t border-solid border-line-subtle pt-4">
            <StepUpOtp
              challenge={challenge}
              value={code}
              onChange={setCode}
              error={otpErr}
              onResent={setChallenge}
            />
            <div className="flex items-center gap-3 mt-4 flex-wrap">
              <Button
                iconLeft="check"
                disabled={code.length < 6}
                loading={change.isPending}
                onClick={confirm}
              >
                {t('account.password.confirm_otp')}
              </Button>
              <Button variant="ghost" onClick={() => setChallenge(null)}>
                {t('common.action.cancel')}
              </Button>
            </div>
          </div>
        )}
        {done && (
          <p className="m-0 inline-flex items-center gap-2 self-start text-mint-600 font-semibold text-sm bg-mint-50 rounded-md px-3.5 py-2.5">
            <Icon name="circle-check" size={18} />
            {t('account.password.done_note')}
          </p>
        )}
      </form>
      <aside className="bg-surface-sunken rounded-md px-[18px] py-4">
        <h3 className="m-0 mb-3 flex gap-2 items-center font-bold text-sm text-strong">
          <Icon name="shield-check" size={18} className="text-mint-600" />
          {t('account.password.tips_title')}
        </h3>
        <ul className="list-none m-0 p-0">
          {[1, 2, 3, 4].map((i) => (
            <li key={i} className="flex gap-2 items-start mb-2 text-sm text-muted leading-normal">
              <Icon name="check" size={15} className="text-mint-600 flex-none mt-0.5" />
              {t(`account.password.tips.${i}`)}
            </li>
          ))}
        </ul>
      </aside>
    </div>
  );
}

export function PasswordPanel() {
  const { t } = useT();
  const m = useLoginMethods();
  const has = m.data?.password ?? true;
  return (
    <section className={panelClass} aria-labelledby="pw-title">
      <PanelHead
        id="pw-title"
        icon="shield-check"
        title={t(has ? 'account.menu.password' : 'account.menu.password_set')}
        sub={t(has ? 'account.password.sub' : 'account.password.sub_set')}
      />
      {m.data ? (
        <PasswordForm key={String(has)} hasPassword={has} />
      ) : (
        <Skeleton lines={4} className="mt-5" />
      )}
    </section>
  );
}

export function LoginsPanel() {
  const { t, locale } = useT();
  const m = useLoginMethods();
  const link = useLinkGoogle();
  const unlink = useUnlinkGoogle();
  const [ask, setAsk] = useState<'link' | 'unlink' | null>(null);
  const d = m.data;
  const google = d?.identities.find((i) => i.provider === 'GOOGLE');
  const count = (d?.password ? 1 : 0) + (google ? 1 : 0);
  const state = (ok: boolean, text: string) => (
    <span
      className={cn(
        'inline-flex items-center gap-1 font-semibold text-xs',
        ok ? 'text-mint-600' : 'text-subtle',
      )}
    >
      <Icon name={ok ? 'circle-check' : 'minus-circle'} size={14} />
      {text}
    </span>
  );
  const row =
    'flex items-center gap-3.5 py-4 border-0 border-t border-solid border-line-subtle flex-wrap';
  const ico = 'w-10 h-10 rounded-md bg-gray-50 grid place-items-center flex-none text-body';
  return (
    <section className={panelClass} aria-labelledby="logins-title">
      <PanelHead
        id="logins-title"
        icon="user-check"
        title={t('account.menu.logins')}
        sub={t('account.logins.sub')}
      />
      {!d ? (
        <Skeleton lines={4} className="mt-5" />
      ) : (
        <>
          <p className="m-0 mt-4 mb-1.5 flex gap-2.5 items-start px-[13px] py-[11px] bg-blue-50 rounded-md text-sm text-body leading-normal">
            <Icon name="info" size={16} className="text-blue-600 flex-none mt-0.5" />
            <span>{t('account.logins.info', { email: d.email })}</span>
          </p>
          <div className={row}>
            <span className={ico}>
              <Icon name="mail" size={19} />
            </span>
            <div className="flex-1 min-w-0">
              <div className="font-semibold text-sm text-strong">
                {t('account.logins.password')}
              </div>
              <div className="flex gap-2.5 flex-wrap mt-[3px] text-xs text-muted">
                {d.email}
                {state(
                  d.password,
                  t(d.password ? 'account.logins.has_pw' : 'account.logins.no_pw'),
                )}
              </div>
            </div>
            <Button size="sm" variant={d.password ? 'ghost' : 'primary'} href="/account/password">
              {t(d.password ? 'account.menu.password' : 'account.menu.password_set')}
            </Button>
          </div>
          <div className={row}>
            <span className={ico}>
              <Icon name="user-check" size={19} />
            </span>
            <div className="flex-1 min-w-0">
              <div className="font-semibold text-sm text-strong">Google</div>
              <div className="flex gap-2.5 flex-wrap mt-[3px] text-xs text-muted">
                {google ? google.email : t('account.logins.not_linked')}
                {state(!!google, t(google ? 'account.logins.linked' : 'account.logins.not_linked'))}
              </div>
              {google && !d.password && (
                <p className="m-0 mt-1.5 text-xs text-amber-600">{t('account.logins.only_way')}</p>
              )}
            </div>
            {google ? (
              <Button
                size="sm"
                variant="ghost"
                disabled={!d.password}
                onClick={() => setAsk('unlink')}
              >
                {t('account.logins.unlink')}
              </Button>
            ) : (
              <Button size="sm" variant="ghost" onClick={() => setAsk('link')}>
                {t('account.logins.link')}
              </Button>
            )}
          </div>
          <p className="m-0 border-0 border-t border-solid border-line-subtle pt-3 text-xs text-muted">
            {t('account.logins.count', { count })}
            {google &&
              ` · ${t('account.logins.since', { when: formatRelative(google.linkedAt, { locale }) })}`}
          </p>
        </>
      )}
      <ConfirmDialog
        open={ask === 'link'}
        title={t('account.logins.link_title')}
        message={t('account.logins.link_text', { email: d?.email ?? '' })}
        confirmLabel={t('account.logins.link')}
        loading={link.isPending}
        onCancel={() => setAsk(null)}
        onConfirm={() => {
          // Thật: chuyển sang Google OAuth (intent=link) rồi quay về /account/logins với code
          if (!USE_MOCK) {
            window.location.assign(
              `/auth/oauth/google/authorize?intent=link&next=%2Faccount%2Flogins`,
            );
            return;
          }
          link.mutate(null, {
            onSuccess: () => {
              setAsk(null);
              toast.success(t('account.logins.linked_toast'));
            },
            onError: (e) => toast.error(errorMessage(e, t)),
          });
        }}
      />
      <ConfirmDialog
        open={ask === 'unlink'}
        tone="danger"
        title={t('account.logins.unlink_title')}
        message={t('account.logins.unlink_text')}
        confirmLabel={t('account.logins.unlink')}
        cancelLabel={t('account.logins.keep')}
        loading={unlink.isPending}
        onCancel={() => setAsk(null)}
        onConfirm={() =>
          unlink.mutate(undefined, {
            onSuccess: () => {
              setAsk(null);
              toast.success(t('account.logins.unlinked_toast'));
            },
            onError: (e) => toast.error(errorMessage(e, t)),
          })
        }
      />
    </section>
  );
}

const DEVICE_ICON: Record<string, IconName> = {
  DESKTOP: 'monitor',
  MOBILE: 'smartphone',
  TABLET: 'smartphone',
};

export function DevicesPanel() {
  const { t, locale } = useT();
  const q = useDevices();
  const out = useLogoutDevices();
  const list = q.data ?? [];
  const others = list.filter((d) => !d.current).length;
  const run = (id: number | 'others') =>
    out.mutate(id, {
      onSuccess: () =>
        toast.success(t(id === 'others' ? 'account.devices.out_all' : 'account.devices.out_one')),
      onError: (e) => toast.error(errorMessage(e, t)),
    });
  return (
    <section className={panelClass} aria-labelledby="dev-title">
      <PanelHead
        id="dev-title"
        icon="lock"
        title={t('account.menu.devices')}
        sub={t('account.devices.sub')}
        action={
          <Button
            size="sm"
            variant="outline"
            iconLeft="log-out"
            disabled={others === 0}
            onClick={() => run('others')}
          >
            {t('account.devices.logout_others')}
          </Button>
        }
      />
      <p className="m-0 my-4 flex gap-2.5 items-center bg-blue-50 border border-solid border-blue-200 rounded-md px-3.5 py-[11px] text-sm text-body">
        <Icon name="shield-check" size={18} className="text-blue-600 flex-none" />
        <span>
          {t('account.devices.tip_before')}{' '}
          <a href="/account/password" className="font-bold text-blue-600">
            {t('account.devices.tip_link')}
          </a>
          . {t('account.devices.count', { count: list.length })}
        </span>
      </p>
      {q.isPending ? (
        <Skeleton lines={4} />
      ) : (
        <ul className="list-none m-0 p-0 flex flex-col gap-2.5">
          {list.map((d) => (
            <li
              key={d.id}
              className={cn(
                'flex items-center gap-3.5 rounded-md px-4 py-3.5 border border-solid flex-wrap',
                d.current ? 'border-mint-100 bg-mint-50' : 'border-line-subtle bg-surface-card',
              )}
            >
              <span
                className={cn(
                  'w-11 h-11 rounded-md grid place-items-center flex-none',
                  d.current ? 'bg-mint-500 text-inverse' : 'bg-surface-sunken text-muted',
                )}
              >
                <Icon name={DEVICE_ICON[d.type] ?? 'lock'} size={22} />
              </span>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="font-semibold text-sm text-strong">{d.name}</span>
                  {d.current && (
                    <Badge variant="mint" icon="badge-check">
                      {t('account.devices.current')}
                    </Badge>
                  )}
                </div>
                <div className="flex flex-wrap gap-x-3.5 gap-y-0.5 mt-1 text-xs text-muted">
                  <span className="inline-flex items-center gap-1">
                    <Icon name="map-pin" size={12} />
                    {d.location}
                  </span>
                  <span className="font-mono">IP {d.ip}</span>
                  <span className="inline-flex items-center gap-1">
                    <Icon name="clock" size={12} />
                    {d.current
                      ? t('account.devices.active')
                      : formatRelative(d.lastActiveAt, { locale })}
                  </span>
                </div>
              </div>
              {d.current ? (
                <span className="text-xs text-mint-600 font-semibold">
                  {t('account.devices.this')}
                </span>
              ) : (
                <Button size="sm" variant="ghost" iconLeft="log-out" onClick={() => run(d.id)}>
                  {t('common.action.logout')}
                </Button>
              )}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
