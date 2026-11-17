// Bảo mật khu tài khoản (prototype Account.jsx PasswordPanel / LoginsPanel / DevicesPanel) — plan
// G10.50–G10.52: đổi / thiết lập mật khẩu (kiểm mật khẩu hiện tại khi gõ, độ mạnh, OTP bước 2), liên kết
// Google / Zalo (mỗi tài khoản một mạng xã hội; không gỡ được khi là cách đăng nhập duy nhất; liên kết Zalo phải nhập
// lại mật khẩu), thiết bị đăng nhập (đăng xuất từ xa).
import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { errorMessage, isApiError } from '@/shared/api/errors';
import { markOAuthLinkIntent } from '@/shared/auth/oauth-intent';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatRelative } from '@/shared/lib/format';
import { zodResolver } from '@/shared/lib/zodResolver';
import {
  Badge,
  Button,
  ConfirmDialog,
  Icon,
  Modal,
  PanelHead,
  PasswordInput,
  Skeleton,
  SocialLogo,
  toast,
  type IconName,
  type SocialLogoProvider,
} from '@/shared/ui';
import {
  useChangePassword,
  useDevices,
  useLinkIdentity,
  useLoginMethods,
  useLogoutDevices,
  useRequestStepUp,
  useUnlinkIdentity,
  useVerifyPassword,
} from '../application/useAccount';
import { passwordSchema, type PasswordFormValues } from '../domain/account.schema';
import type { IdentityProvider, StepUpChallenge } from '../domain/account.types';
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

const SOCIAL: { provider: IdentityProvider; slug: SocialLogoProvider; label: string }[] = [
  { provider: 'GOOGLE', slug: 'google', label: 'Google' },
  { provider: 'ZALO', slug: 'zalo', label: 'Zalo' },
];

export function LoginsPanel() {
  const { t, locale } = useT();
  const m = useLoginMethods();
  const link = useLinkIdentity();
  const unlink = useUnlinkIdentity();
  // Hộp thoại đang mở: liên kết / gỡ liên kết của nhà cung cấp nào
  const [ask, setAsk] = useState<{ kind: 'link' | 'unlink'; provider: IdentityProvider } | null>(
    null,
  );
  // Gỡ liên kết (mọi nhà cung cấp) và liên kết Zalo là thao tác nhạy cảm → nhập lại mật khẩu hiện tại
  const [pw, setPw] = useState('');
  const [pwErr, setPwErr] = useState<string | null>(null);
  const open = (kind: 'link' | 'unlink', provider: IdentityProvider) => {
    setPw('');
    setPwErr(null);
    setAsk({ kind, provider });
  };
  const close = () => setAsk(null);
  // Quay về sau khi liên kết (/account/logins?linked=google|zalo) → báo thành công rồi bỏ tham số khỏi URL
  useEffect(() => {
    const url = new URL(window.location.href);
    if (!url.searchParams.has('linked')) return;
    toast.success(t('account.logins.linked_toast'));
    url.searchParams.delete('linked');
    window.history.replaceState(null, '', url.pathname + url.search);
  }, [t]);
  const d = m.data;
  const linked = d?.identities[0];
  const count = (d?.password ? 1 : 0) + (d?.identities.length ?? 0);
  const current = ask ? SOCIAL.find((s) => s.provider === ask.provider)! : null;
  const passwordError = (e: unknown) =>
    isApiError(e) && e.code === 'GNE-AUTH-4011'
      ? t('account.password.err.wrong')
      : errorMessage(e, t);

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

  const startLink = () => {
    if (!ask) return;
    if (ask.provider === 'ZALO' && !pw) {
      setPwErr(t('account.password.err.current'));
      return;
    }
    link.mutate(
      { provider: ask.provider, currentPassword: ask.provider === 'ZALO' ? pw : undefined },
      {
        onSuccess: (url) => {
          // API thật: sang nhà cung cấp rồi quay về /auth/oauth-callback → /account/logins; mock: đã liên kết
          if (url) {
            markOAuthLinkIntent(ask.provider.toLowerCase());
            window.location.assign(url);
            return;
          }
          close();
          toast.success(t('account.logins.linked_toast'));
        },
        onError: (e) => {
          if (ask.provider === 'ZALO') {
            setPw('');
            setPwErr(passwordError(e));
          } else {
            toast.error(errorMessage(e, t));
          }
        },
      },
    );
  };

  return (
    <section className={panelClass} aria-labelledby="logins-title">
      <PanelHead
        id="logins-title"
        icon="user-check"
        iconTile
        title={t('account.menu.logins')}
        sub={t('account.logins.sub')}
      />
      {!d ? (
        <Skeleton lines={5} className="mt-5" />
      ) : (
        <>
          {/* Dòng đầu: PanelHead đã có đường kẻ dưới → bỏ đường kẻ trên để không bị 2 vạch */}
          <div className={cn(row, '!border-t-0')}>
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
          {SOCIAL.map((s) => {
            const id = d.identities.find((i) => i.provider === s.provider);
            // Mỗi tài khoản chỉ liên kết một mạng xã hội (plan D16) → đã gắn cái khác thì khóa nút liên kết
            const other = !id && linked ? SOCIAL.find((x) => x.provider === linked.provider) : null;
            return (
              <div key={s.provider} className={row}>
                <span className={ico}>
                  <SocialLogo provider={s.slug} size={20} />
                </span>
                <div className="flex-1 min-w-0">
                  <div className="font-semibold text-sm text-strong">{s.label}</div>
                  <div className="flex gap-2.5 flex-wrap mt-[3px] text-xs text-muted">
                    {id && (id.email ?? id.displayName)}
                    {state(!!id, t(id ? 'account.logins.linked' : 'account.logins.not_linked'))}
                  </div>
                  {id && !d.password && (
                    <p className="m-0 mt-1.5 text-xs text-amber-600">
                      {t('account.logins.only_way', { provider: s.label })}
                    </p>
                  )}
                  {other && (
                    <p className="m-0 mt-1.5 text-xs text-subtle">
                      {t('account.logins.one_social', { provider: other.label })}
                    </p>
                  )}
                </div>
                {id ? (
                  <Button
                    size="sm"
                    variant="ghost"
                    disabled={!d.password}
                    onClick={() => open('unlink', s.provider)}
                  >
                    {t('account.logins.unlink')}
                  </Button>
                ) : (
                  <Button
                    size="sm"
                    variant="ghost"
                    disabled={!!other}
                    onClick={() => open('link', s.provider)}
                  >
                    {t('account.logins.link')}
                  </Button>
                )}
              </div>
            );
          })}
          <p className="m-0 border-0 border-t border-solid border-line-subtle pt-3 text-xs text-muted">
            {t('account.logins.count', { count })}
            {linked &&
              ` · ${t('account.logins.since', {
                provider: SOCIAL.find((x) => x.provider === linked.provider)?.label ?? '',
                when: formatRelative(linked.linkedAt, { locale }),
              })}`}
          </p>
        </>
      )}
      <Modal
        open={ask?.kind === 'link'}
        onClose={close}
        aria-label={
          current ? t('account.logins.link_dialog_title', { provider: current.label }) : ''
        }
        width={420}
        className="!rounded-[10px]"
        bodyClassName="relative !px-8 !py-8 flex flex-col items-center text-center"
      >
        <button
          type="button"
          onClick={close}
          aria-label={t('ui.close')}
          data-skip-autofocus
          className="absolute right-2 top-2 grid h-10 w-10 cursor-pointer place-items-center rounded-md border-0 bg-transparent text-muted hover:bg-gray-100 focus-visible:outline-none focus-visible:shadow-focus"
        >
          <Icon name="x" size={20} />
        </button>
        {current && (
          <>
            <span className="mb-4 grid h-14 w-14 place-items-center rounded-full border border-solid border-line-subtle bg-surface-card shadow-sm">
              <SocialLogo provider={current.slug} size={26} />
            </span>
            <p className="m-0 text-[17px] font-semibold leading-snug text-strong">
              {t('account.logins.link_dialog_title', { provider: current.label })}
            </p>
            {current.provider === 'GOOGLE' ? (
              <>
                <p className="m-0 mt-1.5 text-[15px] leading-relaxed text-strong">
                  {t('account.logins.link_dialog_text')}
                </p>
                <span className="mt-3 inline-block max-w-full break-all rounded-md bg-[#E8F5EF] px-3 py-1.5 text-sm font-semibold text-[#1F7356]">
                  {d?.email}
                </span>
              </>
            ) : (
              <>
                <p className="m-0 mt-1.5 text-[15px] leading-relaxed text-strong">
                  {t('account.logins.link_zalo_text')}
                </p>
                <div className="mt-4 w-full text-left">
                  <PasswordInput
                    label={t('account.password.current')}
                    name="link-current-password"
                    autoComplete="current-password"
                    value={pw}
                    onChange={(e) => {
                      setPw(e.target.value);
                      setPwErr(null);
                    }}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') startLink();
                    }}
                    error={pwErr ?? undefined}
                  />
                </div>
              </>
            )}
            <Button
              block
              size="lg"
              className="mt-6 !rounded-[8px]"
              loading={link.isPending}
              onClick={startLink}
            >
              {t('account.logins.link_continue', { provider: current.label })}
            </Button>
          </>
        )}
      </Modal>
      <ConfirmDialog
        open={ask?.kind === 'unlink'}
        tone="danger"
        title={current ? t('account.logins.unlink_title', { provider: current.label }) : ''}
        message={current ? t('account.logins.unlink_text', { provider: current.label }) : ''}
        confirmLabel={t('account.logins.unlink')}
        cancelLabel={t('account.logins.keep')}
        loading={unlink.isPending}
        onCancel={close}
        onConfirm={() => {
          if (!ask) return;
          if (!pw) {
            setPwErr(t('account.password.err.current'));
            return;
          }
          unlink.mutate(
            { provider: ask.provider, currentPassword: pw },
            {
              onSuccess: () => {
                close();
                setPw('');
                toast.success(t('account.logins.unlinked_toast'));
              },
              onError: (e) => {
                setPw('');
                setPwErr(passwordError(e));
              },
            },
          );
        }}
      >
        <div className="mt-3.5">
          <PasswordInput
            label={t('account.password.current')}
            name="unlink-current-password"
            autoComplete="current-password"
            value={pw}
            onChange={(e) => {
              setPw(e.target.value);
              setPwErr(null);
            }}
            error={pwErr ?? undefined}
          />
        </div>
      </ConfirmDialog>
    </section>
  );
}

const DEVICE_ICON: Record<string, IconName> = {
  DESKTOP: 'monitor',
  MOBILE: 'smartphone',
  TABLET: 'smartphone',
};

/** `passwordHref`: liên kết "đổi mật khẩu" trong lời nhắc (Admin trỏ tới khối đổi mật khẩu cùng trang). */
export function DevicesPanel({
  passwordHref = '/account/password',
}: { passwordHref?: string } = {}) {
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
          <a href={passwordHref} className="font-bold text-blue-600">
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
              <div className="flex-1 min-w-[180px]">
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
