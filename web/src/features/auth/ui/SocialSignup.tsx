// Hoàn tất đăng ký bằng Zalo (UC-AUTH-03, plan AUTH A14.10, D23). Zalo không cung cấp email → hỏi email, gửi OTP xác minh
// (màn OTP dùng chung), mã đúng → tạo tài khoản gắn Zalo và đăng nhập luôn.
// Email đã có tài khoản Genie → KHÔNG tự gộp (Zalo không chứng minh được quyền sở hữu email): hộp thoại hướng dẫn đăng
// nhập rồi liên kết Zalo ở Tài khoản › Liên kết đăng nhập.
import { useState, type FormEvent } from 'react';
import { errorMessage, toApiError } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { Button, Icon, Input, Modal, SocialLogo } from '@/shared/ui';
import { useSocialSignup } from '../application/useAuth';
import { isEmail } from '../domain/auth.rules';
import type { OtpChallenge, SocialSignup as Signup } from '../domain/auth.types';
import { completeLogin } from './completeLogin';
import { AuthHeading, Notice } from './AuthParts';
import { OtpStep } from './OtpStep';
import { SOCIAL_LABEL } from './SocialButtons';

export function SocialSignup({ signup, next }: { signup: Signup; next: string | null }) {
  const { t } = useT();
  const label = SOCIAL_LABEL[signup.provider];
  const [email, setEmail] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [taken, setTaken] = useState(false);
  const [expired, setExpired] = useState(false);
  const [challenge, setChallenge] = useState<OtpChallenge | null>(null);
  const send = useSocialSignup();

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setNotice(null);
    if (!email.trim()) return setError(t('auth.err.email_required'));
    if (!isEmail(email)) return setError(t('auth.err.email_invalid'));
    setError(null);
    send.mutate(
      { provider: signup.provider, token: signup.token, email },
      {
        onSuccess: setChallenge,
        onError: (err) => {
          const a = toApiError(err);
          if (a.code === 'GNE-AUTH-4091') setTaken(true);
          else if (a.code === 'GNE-AUTH-4001') setExpired(true);
          else setNotice(errorMessage(a, t));
        },
      },
    );
  };

  if (expired) {
    return (
      <div className="mx-auto w-full max-w-[380px] text-center">
        <AuthHeading
          center
          title={t('auth.social_signup.expired_title')}
          lead={t('auth.social_signup.expired_text', { provider: label })}
        />
        <Button block size="lg" href="/auth/login">
          {t('auth.forgot.back')}
        </Button>
      </div>
    );
  }

  if (challenge) {
    return (
      <div className="mx-auto w-full max-w-[380px]">
        <OtpStep
          email={email}
          challenge={challenge}
          title={t('auth.otp.title')}
          backLabel={t('auth.otp.change_email')}
          onBack={() => setChallenge(null)}
          onVerified={(r) => {
            if (r.kind === 'session') void completeLogin(r.session, next);
          }}
          onExhausted={() => setExpired(true)}
        />
      </div>
    );
  }

  return (
    <div className="mx-auto w-full max-w-[384px]">
      <div className="mb-4 flex justify-center">
        <span className="grid h-14 w-14 place-items-center rounded-full border border-solid border-line-subtle bg-surface-card shadow-sm">
          <SocialLogo provider={signup.provider} size={28} />
        </span>
      </div>
      <AuthHeading
        center
        title={
          signup.name
            ? t('auth.social_signup.title_named', { name: signup.name })
            : t('auth.social_signup.title')
        }
        lead={t('auth.social_signup.lead', { provider: label })}
      />
      {notice && <Notice tone="error">{notice}</Notice>}
      <form className="flex flex-col gap-3.5" onSubmit={submit} noValidate>
        <Input
          label={t('auth.field.email')}
          iconLeft="mail"
          type="email"
          name="email"
          autoComplete="email"
          placeholder={t('auth.field.email_ph')}
          value={email}
          maxLength={255}
          onChange={(e) => {
            setEmail(e.target.value);
            setError(null);
            setTaken(false);
          }}
          error={error ?? undefined}
        />
        <Button type="submit" block size="lg" loading={send.isPending}>
          {t('auth.social_signup.submit')}
        </Button>
      </form>
      <p className="m-0 mt-4 text-center text-xs leading-relaxed text-muted">
        {t('auth.social_signup.hint', { provider: label })}
      </p>
      <Modal
        open={taken}
        onClose={() => setTaken(false)}
        aria-label={t('auth.social_signup.taken_title')}
        width={420}
        className="!rounded-[10px]"
        bodyClassName="relative !px-8 !py-8 flex flex-col items-center text-center"
      >
        <button
          type="button"
          onClick={() => setTaken(false)}
          aria-label={t('ui.close')}
          data-skip-autofocus
          className="absolute right-2 top-2 grid h-10 w-10 cursor-pointer place-items-center rounded-md border-0 bg-transparent text-muted hover:bg-gray-100 focus-visible:outline-none focus-visible:shadow-focus"
        >
          <Icon name="x" size={20} />
        </button>
        <span className="mb-4 grid h-14 w-14 place-items-center rounded-full bg-[#E8F5EF] text-[#1F7356]">
          <Icon name="user-check" size={26} />
        </span>
        <p className="m-0 text-[17px] font-semibold leading-snug text-strong">
          {t('auth.social_signup.taken_title')}
        </p>
        <p className="m-0 mt-1.5 text-[15px] leading-relaxed text-strong">
          {t('auth.social_signup.taken_text', { provider: label })}
        </p>
        <Button
          block
          size="lg"
          className="mt-6 !rounded-[8px]"
          href={`/auth/login?email=${encodeURIComponent(email.trim())}&next=${encodeURIComponent('/account/logins')}`}
        >
          {t('auth.register.go_login')}
        </Button>
      </Modal>
    </div>
  );
}
