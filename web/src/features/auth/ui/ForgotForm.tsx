// Quên mật khẩu (prototype auth Forgot) — plan G09.09, island client:load.
// Email → OTP (luôn báo "nếu email tồn tại…") → mật khẩu mới (khác mật khẩu gần nhất) → xong.
// Sai OTP 5 lần → tạm khóa khôi phục.
import { useState, type FormEvent } from 'react';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { Providers } from '@/shared/providers/Providers';
import { Button, Input, PasswordInput } from '@/shared/ui';
import { useForgot, useResetPassword } from '../application/useAuth';
import { fieldErrors, forgotSchema, resetSchema } from '../domain/auth.schemas';
import type { OtpChallenge } from '../domain/auth.types';
import { AuthHeading, BackButton, Notice, StateIcon } from './AuthParts';
import { OtpStep } from './OtpStep';

type Stage = 'email' | 'otp' | 'reset' | 'done' | 'blocked';

function Form({ initialEmail }: { initialEmail: string }) {
  const { t } = useT();
  const [stage, setStage] = useState<Stage>('email');
  const [email, setEmail] = useState(initialEmail);
  const [challenge, setChallenge] = useState<OtpChallenge | null>(null);
  const [resetToken, setResetToken] = useState('');
  const [pw, setPw] = useState('');
  const [pw2, setPw2] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [notice, setNotice] = useState<string | null>(null);
  const forgot = useForgot();
  const reset = useResetPassword();
  const tr = (e: Record<string, string>) =>
    Object.fromEntries(Object.entries(e).map(([k, v]) => [k, t(v)]));

  const sendCode = (e: FormEvent) => {
    e.preventDefault();
    setNotice(null);
    const parsed = forgotSchema.safeParse({ email });
    if (!parsed.success) {
      setErrors(tr(fieldErrors(parsed.error)));
      return;
    }
    setErrors({});
    forgot.mutate(parsed.data.email, {
      onSuccess: (c) => {
        setChallenge(c);
        setStage('otp');
      },
      onError: (err) => setNotice(errorMessage(err, t)),
    });
  };

  const submitReset = (e: FormEvent) => {
    e.preventDefault();
    const parsed = resetSchema.safeParse({ password: pw, confirm: pw2 });
    if (!parsed.success) {
      setErrors(tr(fieldErrors(parsed.error)));
      return;
    }
    setErrors({});
    reset.mutate(
      { resetToken, password: pw },
      {
        onSuccess: () => setStage('done'),
        onError: (err) => setErrors({ password: errorMessage(err, t) }),
      },
    );
  };

  return (
    <div className="mx-auto w-full max-w-[380px]">
      {stage === 'email' && (
        <>
          <BackButton onClick={() => window.location.assign('/auth/login')}>
            {t('auth.forgot.back')}
          </BackButton>
          <AuthHeading title={t('auth.forgot.title')} lead={t('auth.forgot.lead')} />
          {notice && <Notice tone="error">{notice}</Notice>}
          <form className="flex flex-col gap-4" onSubmit={sendCode} noValidate>
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
                setErrors({});
              }}
              error={errors.email || undefined}
              placeholder={t('auth.field.email_ph')}
            />
            <Button type="submit" block size="lg" loading={forgot.isPending}>
              {t('auth.forgot.send')}
            </Button>
          </form>
        </>
      )}

      {stage === 'otp' && challenge && (
        <OtpStep
          email={email}
          challenge={challenge}
          title={t('auth.forgot.otp_title')}
          backLabel={t('auth.otp.change_email')}
          onBack={() => setStage('email')}
          onVerified={(r) => {
            if (r.kind === 'reset') {
              setResetToken(r.resetToken);
              setStage('reset');
            }
          }}
          onExhausted={() => setStage('blocked')}
        />
      )}

      {stage === 'reset' && (
        <>
          <AuthHeading title={t('auth.forgot.reset_title')} lead={t('auth.forgot.reset_lead')} />
          <form className="flex flex-col gap-3.5" onSubmit={submitReset} noValidate>
            <PasswordInput
              label={t('auth.field.new_password')}
              name="new-password"
              autoComplete="new-password"
              showStrength
              value={pw}
              onChange={(e) => {
                setPw(e.target.value);
                setErrors({});
              }}
              error={errors.password || undefined}
              placeholder={t('auth.field.password_min')}
            />
            <PasswordInput
              label={t('auth.field.confirm_new')}
              name="confirm-password"
              autoComplete="new-password"
              matchWith={pw}
              value={pw2}
              onChange={(e) => {
                setPw2(e.target.value);
                setErrors({});
              }}
              error={errors.confirm || undefined}
              placeholder={t('auth.field.confirm_ph')}
            />
            <Button type="submit" block size="lg" loading={reset.isPending}>
              {t('auth.forgot.update')}
            </Button>
          </form>
        </>
      )}

      {stage === 'blocked' && (
        <div className="text-center">
          <StateIcon icon="lock" tone="error" />
          <AuthHeading
            center
            title={t('auth.forgot.blocked_title')}
            lead={t('auth.forgot.blocked_text')}
          />
          <Button block size="lg" variant="secondary" href="/auth/login">
            {t('auth.forgot.back')}
          </Button>
        </div>
      )}

      {stage === 'done' && (
        <div className="text-center">
          <StateIcon icon="circle-check" tone="ok" />
          <AuthHeading
            center
            title={t('auth.forgot.done_title')}
            lead={t('auth.forgot.done_text')}
          />
          <Button block size="lg" href={`/auth/login?email=${encodeURIComponent(email)}`}>
            {t('auth.forgot.login_now')}
          </Button>
        </div>
      )}
    </div>
  );
}

export default function ForgotForm({ email = '' }: { email?: string }) {
  return (
    <Providers>
      <Form initialEmail={email} />
    </Providers>
  );
}
