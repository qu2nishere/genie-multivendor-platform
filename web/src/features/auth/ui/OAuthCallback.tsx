// Trang trả về sau Google / Apple (plan G09.12; API_SPEC 2.10): đang xử lý → hoàn tất (gộp giỏ,
// chuyển về `next`) hoặc lỗi. Thiếu `code` / `state`, nhà cung cấp báo lỗi, hay backend từ chối
// (GNE-AUTH-4001 state không hợp lệ, 4032 email chưa xác minh, 4031 tài khoản bị khóa) → màn lỗi.
import { useEffect, useRef, useState } from 'react';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { Button, Spinner } from '@/shared/ui';
import { finishOAuth } from '../application/useAuth';
import type { SocialProvider } from '../domain/auth.types';
import { completeLogin } from './completeLogin';
import { AuthHeading, StateIcon } from './AuthParts';

const PROVIDERS: SocialProvider[] = ['google', 'apple'];

export default function OAuthCallback() {
  const { t } = useT();
  const [error, setError] = useState<string | null>(null);
  const started = useRef(false);

  useEffect(() => {
    if (started.current) return;
    started.current = true;
    const q = new URLSearchParams(window.location.search);
    const provider = q.get('provider') as SocialProvider | null;
    const code = q.get('code');
    const state = q.get('state');
    if (q.get('error')) {
      setError(t('auth.oauth.denied'));
      return;
    }
    if (!provider || !PROVIDERS.includes(provider) || !code || !state) {
      setError(t('errors.GNE-AUTH-4001'));
      return;
    }
    const redirect = new URL(window.location.href);
    ['code', 'state', 'scope', 'authuser', 'prompt'].forEach((k) =>
      redirect.searchParams.delete(k),
    );
    finishOAuth(provider, code, state, redirect.href)
      .then((r) => completeLogin(r.session, q.get('next')))
      .catch((e: unknown) => setError(errorMessage(e, t)));
  }, [t]);

  if (error) {
    return (
      <div className="mx-auto w-full max-w-[380px] text-center">
        <StateIcon icon="circle-alert" tone="error" />
        <AuthHeading center title={t('auth.oauth.error_title')} lead={error} />
        <Button block size="lg" href="/auth/login">
          {t('auth.forgot.back')}
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
