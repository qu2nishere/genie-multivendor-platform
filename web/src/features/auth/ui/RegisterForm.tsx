// Đăng ký người mua (prototype auth Register + step "otp") — plan G09.08, island client:load.
// Họ tên, email (trùng → GNE-AUTH-4091 gợi ý đăng nhập / quên mật khẩu), số điện thoại, mật khẩu có
// thanh độ mạnh, nhập lại, đồng ý điều khoản → OTP xác minh email → đăng nhập luôn.
// Sai OTP 5 lần → quay lại form với thông báo "Đăng ký thất bại".
import { useState, type FormEvent } from 'react';
import { errorMessage, toApiError } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { Providers } from '@/shared/providers/Providers';
import { Button, Checkbox, Input, PasswordInput } from '@/shared/ui';
import { useRegister } from '../application/useAuth';
import { fieldErrors, registerSchema } from '../domain/auth.schemas';
import type { OtpChallenge } from '../domain/auth.types';
import { completeLogin } from './completeLogin';
import { AuthHeading, Notice, OrDivider } from './AuthParts';
import { OtpStep } from './OtpStep';
import { SocialButtons } from './SocialButtons';

const EMPTY = { name: '', email: '', phone: '', password: '', confirm: '', agree: false };

function Form({ next }: { next: string | null }) {
  const { t } = useT();
  const [f, setF] = useState(EMPTY);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [taken, setTaken] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const [challenge, setChallenge] = useState<OtpChallenge | null>(null);
  const reg = useRegister();
  const set = <K extends keyof typeof EMPTY>(k: K, v: (typeof EMPTY)[K]) => {
    setF((s) => ({ ...s, [k]: v }));
    setErrors((e) => ({ ...e, [k]: '' }));
    if (k === 'email') setTaken(false);
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setNotice(null);
    const parsed = registerSchema.safeParse(f);
    if (!parsed.success) {
      setErrors(
        Object.fromEntries(Object.entries(fieldErrors(parsed.error)).map(([k, v]) => [k, t(v)])),
      );
      return;
    }
    setErrors({});
    reg.mutate(
      { name: f.name, email: f.email, phone: f.phone, password: f.password },
      {
        onSuccess: setChallenge,
        onError: (err) => {
          const a = toApiError(err);
          if (a.code === 'GNE-AUTH-4091') {
            setTaken(true);
            setErrors({ email: errorMessage(a, t) });
          } else setNotice(errorMessage(a, t));
        },
      },
    );
  };

  if (challenge) {
    return (
      <div className="mx-auto w-full max-w-[380px]">
        <OtpStep
          email={f.email}
          challenge={challenge}
          title={t('auth.otp.title')}
          backLabel={t('auth.otp.change_email')}
          onBack={() => setChallenge(null)}
          onVerified={(r) => {
            if (r.kind === 'session') void completeLogin(r.session, next);
          }}
          onExhausted={() => {
            setChallenge(null);
            setNotice(t('auth.register.failed_text'));
          }}
        />
      </div>
    );
  }

  return (
    <div className="mx-auto w-full max-w-[384px]">
      <AuthHeading title={t('auth.register.title')} lead={t('auth.register.lead')} />
      {notice && (
        <Notice tone="error" title={t('auth.register.failed_title')}>
          {notice}
        </Notice>
      )}
      <form className="flex flex-col gap-3.5" onSubmit={submit} noValidate>
        <Input
          label={t('auth.field.name')}
          iconLeft="user"
          name="name"
          autoComplete="name"
          value={f.name}
          maxLength={100}
          onChange={(e) => set('name', e.target.value)}
          error={errors.name || undefined}
          placeholder={t('auth.field.name_ph')}
        />
        <div>
          <Input
            label={t('auth.field.email')}
            iconLeft="mail"
            type="email"
            name="email"
            autoComplete="email"
            inputMode="email"
            value={f.email}
            onChange={(e) => set('email', e.target.value)}
            error={errors.email || undefined}
            placeholder={t('auth.field.email_ph')}
          />
          {taken && (
            <p className="m-0 mt-1.5 text-xs">
              <a
                href={`/auth/login?email=${encodeURIComponent(f.email)}`}
                className="font-semibold text-link"
              >
                {t('auth.register.go_login')}
              </a>{' '}
              ·{' '}
              <a
                href={`/auth/forgot?email=${encodeURIComponent(f.email)}`}
                className="font-semibold text-link"
              >
                {t('auth.login.forgot')}
              </a>
            </p>
          )}
        </div>
        <Input
          label={t('auth.field.phone')}
          iconLeft="phone"
          type="tel"
          name="phone"
          autoComplete="tel"
          inputMode="tel"
          value={f.phone}
          onChange={(e) => set('phone', e.target.value)}
          error={errors.phone || undefined}
          placeholder={t('auth.field.phone_ph')}
        />
        <PasswordInput
          label={t('auth.field.password')}
          name="new-password"
          autoComplete="new-password"
          showStrength
          value={f.password}
          onChange={(e) => set('password', e.target.value)}
          error={errors.password || undefined}
          placeholder={t('auth.field.password_rule')}
        />
        <PasswordInput
          label={t('auth.field.confirm')}
          name="confirm-password"
          autoComplete="new-password"
          matchWith={f.password}
          value={f.confirm}
          onChange={(e) => set('confirm', e.target.value)}
          error={errors.confirm || undefined}
          placeholder={t('auth.field.confirm_ph')}
        />
        <Checkbox
          checked={f.agree}
          onChange={() => set('agree', !f.agree)}
          aria-invalid={!!errors.agree}
          className="items-start text-xs"
        >
          <span className={errors.agree ? 'text-red-600' : 'text-muted'}>
            {t('auth.register.agree_a')}{' '}
            <a href="/policies/terms" target="_blank" rel="noopener" className="text-link">
              {t('auth.register.terms')}
            </a>{' '}
            {t('auth.register.agree_b')}{' '}
            <a href="/policies/privacy" target="_blank" rel="noopener" className="text-link">
              {t('auth.register.privacy')}
            </a>
            .
          </span>
        </Checkbox>
        <Button type="submit" block size="lg" loading={reg.isPending}>
          {t('auth.register.submit')}
        </Button>
      </form>
      <OrDivider>{t('auth.register.or')}</OrDivider>
      <SocialButtons next={next} />
      <p className="m-0 mt-4 text-center text-xs text-subtle">
        {t('auth.register.have_account')}{' '}
        <a
          href={`/auth/login${next ? `?next=${encodeURIComponent(next)}` : ''}`}
          className="font-semibold text-link"
        >
          {t('auth.login.submit')}
        </a>
      </p>
    </div>
  );
}

export default function RegisterForm({ next }: { next: string | null }) {
  return (
    <Providers>
      <Form next={next} />
    </Providers>
  );
}
