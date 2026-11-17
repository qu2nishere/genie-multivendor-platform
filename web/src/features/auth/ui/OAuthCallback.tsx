// Trang trả về sau Google / Zalo (plan G09.12, A14.10; API_SPEC 2.10): đang xử lý → hoàn tất (gộp giỏ, chuyển về
// `next`) hoặc lỗi. Thiếu `code` / `state`, nhà cung cấp báo lỗi, hay backend từ chối (GNE-AUTH-4001 state không hợp lệ,
// 4032 email chưa xác minh, 4031 tài khoản bị khóa) → màn lỗi.
// Callback cố định `/auth/oauth-callback` (không kèm tham số) → nhà cung cấp và ý định (đăng nhập / liên kết) đọc từ
// sessionStorage do nút bấm ghi trước khi chuyển trang; không có → Google.
// Zalo chưa có tài khoản (SIGNUP_REQUIRED) → hỏi email + OTP ngay tại trang này (SocialSignup).
// Liên kết (bắt đầu từ Tài khoản › Liên kết đăng nhập, plan A14.04) → gọi API liên kết, quay về
// /account/logins?linked={provider} (4092 đã gắn người khác → màn lỗi, nút quay lại trang liên kết).
import { useEffect, useRef, useState } from 'react';
import { errorMessage } from '@/shared/api/errors';
import { takeOAuthIntent } from '@/shared/auth/oauth-intent';
import { useT } from '@/shared/hooks/useT';
import { Providers } from '@/shared/providers/Providers';
import { Button, Spinner } from '@/shared/ui';
import { finishLink, finishOAuth } from '../application/useAuth';
import type { SocialProvider, SocialSignup as Signup } from '../domain/auth.types';
import { completeLogin } from './completeLogin';
import { AuthHeading, StateIcon } from './AuthParts';
import { SocialSignup } from './SocialSignup';

const PROVIDERS: SocialProvider[] = ['google', 'zalo'];

export default function OAuthCallback() {
  const { t } = useT();
  const [error, setError] = useState<string | null>(null);
  const [linking, setLinking] = useState(false);
  const [signup, setSignup] = useState<{ value: Signup; next: string | null } | null>(null);
  const started = useRef(false);

  useEffect(() => {
    if (started.current) return;
    started.current = true;
    const q = new URLSearchParams(window.location.search);
    const intent = takeOAuthIntent();
    const provider = (q.get('provider') ?? intent.provider ?? 'google') as SocialProvider;
    const code = q.get('code');
    const state = q.get('state');
    const link = intent.link;
    setLinking(link);
    if (q.get('error')) {
      setError(t('auth.oauth.denied'));
      return;
    }
    if (!PROVIDERS.includes(provider) || !code || !state) {
      setError(t('errors.GNE-AUTH-4001'));
      return;
    }
    if (link) {
      finishLink(provider, code, state)
        .then(() => window.location.replace(`/account/logins?linked=${provider}`))
        .catch((e: unknown) => setError(errorMessage(e, t)));
      return;
    }
    finishOAuth(provider, code, state)
      .then((r) => {
        if (r.result === 'SIGNUP_REQUIRED') {
          setSignup({ value: r.signup, next: r.next ?? q.get('next') });
          return;
        }
        return completeLogin(r.session, r.next ?? q.get('next'));
      })
      .catch((e: unknown) => setError(errorMessage(e, t)));
  }, [t]);

  if (signup) {
    return (
      <Providers>
        <SocialSignup signup={signup.value} next={signup.next} />
      </Providers>
    );
  }

  if (error) {
    return (
      <div className="mx-auto w-full max-w-[380px] text-center">
        <StateIcon icon="circle-alert" tone="error" />
        <AuthHeading
          center
          title={t(linking ? 'auth.oauth.link_error_title' : 'auth.oauth.error_title')}
          lead={error}
        />
        <Button block size="lg" href={linking ? '/account/logins' : '/auth/login'}>
          {t(linking ? 'auth.oauth.back_logins' : 'auth.forgot.back')}
        </Button>
      </div>
    );
  }
  return (
    <div
      className="mx-auto grid w-full max-w-[380px] place-items-center gap-4 text-center"
      role="status"
    >
      <Spinner size={36} label={t('auth.oauth.processing')} />
      <p className="m-0 text-base text-muted">{t('auth.oauth.processing')}</p>
    </div>
  );
}
