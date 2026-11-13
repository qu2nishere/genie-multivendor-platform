// Đăng nhập (prototype auth: step "login" / "otp" / "locked") — plan G09.05, island client:load.
// Hai chế độ Mật khẩu / Mã OTP; sai mật khẩu báo số lần còn lại; sai 5 lần → màn tạm khóa 15 phút,
// mở khóa bằng OTP qua email. Xong → gộp giỏ khách, chuyển về `next`.
import { useState, type FormEvent } from 'react';
import { errorMessage, toApiError } from '@/shared/api/errors';
import { USE_MOCK } from '@/shared/api/mock';
import type { SessionUser } from '@/shared/auth/session';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { Providers } from '@/shared/providers/Providers';
import { Button, Input, PasswordInput } from '@/shared/ui';
import { useLogin, useRequestOtp } from '../application/useAuth';
import { fieldErrors, loginSchema, otpLoginSchema } from '../domain/auth.schemas';
import type { OtpChallenge, VerifyResult } from '../domain/auth.types';
import { completeLogin } from './completeLogin';
import { AuthHeading, DemoHint, Notice, OrDivider } from './AuthParts';
import { LockedState, lockSecondsFrom } from './LockedState';
import { OtpStep } from './OtpStep';
import { SocialButtons } from './SocialButtons';

export interface LoginFormProps {
  next: string | null;
  /** Email điền sẵn (vd. từ trang đăng ký → "Đăng nhập"). */
  email?: string;
}

type Step =
  | { name: 'form' }
  | { name: 'otp'; challenge: OtpChallenge; purpose: 'LOGIN' | 'ACCOUNT_UNLOCK' }
  | { name: 'locked'; seconds: number; by: 'PASSWORD' | 'OTP' };

function Form({ next, email: initialEmail = '' }: LoginFormProps) {
  const { t } = useT();
  const [step, setStep] = useState<Step>({ name: 'form' });
  const [mode, setMode] = useState<'password' | 'otp'>('password');
  const [email, setEmail] = useState(initialEmail);
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [done, setDone] = useState(false);
  const login = useLogin();
  const otp = useRequestOtp();
  const tr = (e: Record<string, string>) =>
    Object.fromEntries(Object.entries(e).map(([k, v]) => [k, t(v)]));

  const enter = (r: VerifyResult<SessionUser>) => {
    if (r.kind !== 'session') return;
    setDone(true);
    void completeLogin(r.session, next);
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setFormError(null);
    if (mode === 'password') {
      const parsed = loginSchema.safeParse({ email, password });
      if (!parsed.success) {
        setErrors(tr(fieldErrors(parsed.error)));
        return;
      }
      setErrors({});
      login.mutate(parsed.data, {
        onSuccess: (s) => {
          setDone(true);
          void completeLogin(s, next);
        },
        onError: (err) => {
          const a = toApiError(err);
          setPassword('');
          if (a.code === 'GNE-AUTH-4230') {
            setStep({ name: 'locked', seconds: lockSecondsFrom(a.details), by: 'PASSWORD' });
            return;
          }
          const remaining = a.details.remaining_attempts;
          setErrors({
            password:
              errorMessage(a, t) +
              (typeof remaining === 'number'
                ? ` ${t('auth.login.remaining', { n: remaining })}`
                : ''),
          });
        },
      });
      return;
    }
    const parsed = otpLoginSchema.safeParse({ email });
    if (!parsed.success) {
      setErrors(tr(fieldErrors(parsed.error)));
      return;
    }
    setErrors({});
    otp.mutate(
      { email: parsed.data.email, purpose: 'LOGIN' },
      {
        onSuccess: (c) => setStep({ name: 'otp', challenge: c, purpose: 'LOGIN' }),
        onError: (err) => setFormError(errorMessage(err, t)),
      },
    );
  };

  const unlock = () =>
    otp.mutate(
      { email, purpose: 'ACCOUNT_UNLOCK' },
      {
        onSuccess: (c) => setStep({ name: 'otp', challenge: c, purpose: 'ACCOUNT_UNLOCK' }),
        onError: (err) => setFormError(errorMessage(err, t)),
      },
    );

  if (step.name === 'locked') {
    return (
      <div className="mx-auto w-full max-w-[380px]">
        {formError && <Notice tone="error">{formError}</Notice>}
        <LockedState
          seconds={step.seconds}
          lockedBy={step.by}
          unlocking={otp.isPending}
          onUnlock={unlock}
        />
      </div>
    );
  }

  if (step.name === 'otp') {
    return (
      <div className="mx-auto w-full max-w-[380px]">
        <OtpStep
          email={email}
          challenge={step.challenge}
          title={
            step.purpose === 'ACCOUNT_UNLOCK' ? t('auth.otp.unlock_title') : t('auth.otp.title')
          }
          backLabel={t('auth.otp.change_email')}
          onBack={() => setStep({ name: 'form' })}
          onVerified={enter}
          onExhausted={(e) =>
            setStep({ name: 'locked', seconds: lockSecondsFrom(e.details), by: 'OTP' })
          }
        />
      </div>
    );
  }

  const tab = (id: 'password' | 'otp', label: string) => (
    <button
      type="button"
      aria-pressed={mode === id}
      onClick={() => {
        setMode(id);
        setErrors({});
        setFormError(null);
      }}
      className={cn(
        'h-[34px] flex-1 cursor-pointer rounded-sm border-0 text-sm font-semibold max-md:h-11',
        mode === id
          ? 'bg-surface-card text-strong shadow-sm'
          : 'bg-transparent text-muted hover:text-body',
      )}
    >
      {label}
    </button>
  );

  return (
    <div className="mx-auto w-full max-w-[360px]">
      <AuthHeading
        title={t('auth.login.title')}
        lead={mode === 'otp' ? t('auth.login.lead_otp') : t('auth.login.lead_password')}
      />
      {formError && <Notice tone="error">{formError}</Notice>}
      <div
        className="mb-[18px] flex gap-1 rounded-md bg-gray-100 p-1"
        role="group"
        aria-label={t('auth.login.mode')}
      >
        {tab('password', t('auth.login.mode_password'))}
        {tab('otp', t('auth.login.mode_otp'))}
      </div>
      <form className="flex flex-col gap-4" onSubmit={submit} noValidate>
        <Input
          label={t('auth.field.email')}
          iconLeft="mail"
          type="email"
          name="email"
          autoComplete="email"
          inputMode="email"
          value={email}
          onChange={(e) => {
            setEmail(e.target.value);
            setErrors((x) => ({ ...x, email: '' }));
          }}
          error={errors.email || undefined}
          placeholder={t('auth.field.email_ph')}
        />
        {mode === 'password' ? (
          <>
            <PasswordInput
              label={t('auth.field.password')}
              name="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => {
                setPassword(e.target.value);
                setErrors((x) => ({ ...x, password: '' }));
              }}
              error={errors.password || undefined}
              placeholder={t('auth.field.password_ph')}
            />
            <Button
              type="submit"
              block
              size="lg"
              disabled={!password}
              loading={login.isPending || done}
            >
              {t('auth.login.submit')}
            </Button>
            {USE_MOCK && (
              <p className="m-0 text-center">
                <DemoHint>{t('auth.login.demo', { password: 'Genie@123' })}</DemoHint>
              </p>
            )}
          </>
        ) : (
          <Button type="submit" block size="lg" loading={otp.isPending}>
            {t('auth.login.send_otp')}
          </Button>
        )}
      </form>
      <div className="mt-2.5 text-right">
        <a
          href={`/auth/forgot${email ? `?email=${encodeURIComponent(email)}` : ''}`}
          className="text-sm font-semibold text-link"
        >
          {t('auth.login.forgot')}
        </a>
      </div>
      <OrDivider>{t('auth.login.or')}</OrDivider>
      <SocialButtons next={next} />
      <p className="m-0 mt-7 text-center text-xs text-subtle">
        {t('auth.login.no_account')}{' '}
        <a
          href={`/auth/register${next ? `?next=${encodeURIComponent(next)}` : ''}`}
          className="font-semibold text-link"
        >
          {t('auth.login.register')}
        </a>{' '}
        ·{' '}
        <a href="/become-seller" className="text-link">
          {t('auth.login.open_shop')}
        </a>
      </p>
    </div>
  );
}

export default function LoginForm(props: LoginFormProps) {
  return (
    <Providers>
      <Form {...props} />
    </Providers>
  );
}
