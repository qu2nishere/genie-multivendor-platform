// Đăng nhập Google / Apple (prototype auth SocialButton, SsoDialog) — plan G09.10.
// Thật: chuyển sang GET /auth/oauth/{provider}/authorize → trang /auth/oauth-callback hoàn tất.
// Mock: hộp "Tiếp tục với …" (mô phỏng) rồi đăng nhập luôn. Không dựng bảng mô phỏng GoogleFlow (D5).
import { useState } from 'react';
import { errorMessage } from '@/shared/api/errors';
import { USE_MOCK } from '@/shared/api/mock';
import { env } from '@/shared/config/env';
import { useT } from '@/shared/hooks/useT';
import { Button, Modal, toast } from '@/shared/ui';
import { completeLogin } from './completeLogin';
import type { SocialProvider } from '../domain/auth.types';
import { finishOAuth, oauthStartUrl } from '../application/useAuth';

const LABEL: Record<SocialProvider, string> = { google: 'Google', apple: 'Apple' };

function Logo({ provider }: { provider: SocialProvider }) {
  return provider === 'google' ? (
    <svg width="18" height="18" viewBox="0 0 18 18" aria-hidden="true">
      <path
        fill="#4285F4"
        d="M17.64 9.2c0-.64-.06-1.25-.16-1.84H9v3.48h4.84a4.14 4.14 0 0 1-1.8 2.72v2.26h2.92c1.71-1.57 2.68-3.89 2.68-6.62z"
      />
      <path
        fill="#34A853"
        d="M9 18c2.43 0 4.47-.8 5.96-2.18l-2.92-2.26c-.8.54-1.84.86-3.04.86-2.34 0-4.32-1.58-5.03-3.7H.96v2.33A9 9 0 0 0 9 18z"
      />
      <path fill="#FBBC05" d="M3.97 10.72a5.4 5.4 0 0 1 0-3.44V4.95H.96a9 9 0 0 0 0 8.1l3-2.33z" />
      <path
        fill="#EA4335"
        d="M9 3.58c1.32 0 2.5.45 3.44 1.35l2.58-2.58A9 9 0 0 0 .96 4.95l3 2.33C4.68 5.16 6.66 3.58 9 3.58z"
      />
    </svg>
  ) : (
    <svg width="17" height="18" viewBox="0 0 17 18" aria-hidden="true">
      <path
        fill="currentColor"
        d="M14.07 13.9c-.26.6-.57 1.16-.93 1.67-.5.7-.9 1.18-1.22 1.45-.49.45-1.02.68-1.59.7-.4 0-.9-.12-1.46-.35-.57-.23-1.1-.34-1.58-.34-.5 0-1.04.11-1.63.34-.59.24-1.06.36-1.42.37-.55.02-1.09-.22-1.62-.71-.34-.29-.76-.79-1.27-1.5-.54-.76-.99-1.64-1.34-2.64C.92 13.05.7 11.98.7 10.95c0-1.18.26-2.2.77-3.05a4.5 4.5 0 0 1 1.6-1.62 4.32 4.32 0 0 1 2.17-.61c.42 0 .98.13 1.67.39.69.26 1.13.39 1.32.39.14 0 .63-.15 1.46-.46.78-.28 1.45-.4 1.99-.36 1.47.12 2.58.7 3.31 1.75-1.32.8-1.97 1.92-1.96 3.35.01 1.12.42 2.05 1.22 2.79.36.34.77.61 1.22.8-.1.29-.2.56-.32.83zM11.6 1.4c0 .88-.32 1.7-.96 2.46-.78.9-1.72 1.42-2.74 1.34a2.76 2.76 0 0 1-.02-.33c0-.84.37-1.74 1.02-2.48.33-.37.74-.68 1.24-.93.5-.24.97-.38 1.42-.4.02.11.04.23.04.34z"
      />
    </svg>
  );
}

export function SocialButtons({ next }: { next: string | null }) {
  const { t } = useT();
  const [sso, setSso] = useState<SocialProvider | null>(null);
  const [busy, setBusy] = useState(false);

  const pick = (p: SocialProvider) => {
    if (USE_MOCK) {
      setSso(p);
      return;
    }
    // state / nonce / PKCE do backend sinh; next đi kèm địa chỉ callback
    const callback = new URL('/auth/oauth-callback', window.location.origin);
    callback.searchParams.set('provider', p);
    if (next) callback.searchParams.set('next', next);
    window.location.assign(oauthStartUrl(p, env.PUBLIC_API_URL, callback.href));
  };

  const continueMock = async () => {
    if (!sso) return;
    setBusy(true);
    try {
      const r = await finishOAuth(sso, 'mock-code', 'mock-state', window.location.href);
      await completeLogin(r.session, next);
    } catch (e) {
      toast.error(errorMessage(e, t));
      setBusy(false);
    }
  };

  return (
    <>
      <div className="flex gap-2.5">
        {(['google', 'apple'] as const).map((p) => (
          <button
            key={p}
            type="button"
            onClick={() => pick(p)}
            className="inline-flex h-[46px] flex-1 cursor-pointer items-center justify-center gap-[9px] rounded-md border-[1.5px] border-solid border-line bg-surface-card text-sm font-semibold text-strong hover:border-line-strong focus-visible:outline-none focus-visible:shadow-focus"
          >
            <Logo provider={p} />
            {LABEL[p]}
          </button>
        ))}
      </div>
      <Modal
        open={sso !== null}
        onClose={() => setSso(null)}
        width={400}
        title={sso ? t('auth.social.title', { provider: LABEL[sso] }) : ''}
        footer={
          <>
            <Button variant="ghost" onClick={() => setSso(null)}>
              {t('common.action.cancel')}
            </Button>
            <Button loading={busy} onClick={() => void continueMock()}>
              {t('auth.social.continue')}
            </Button>
          </>
        }
      >
        <p className="m-0 text-base leading-normal text-body">
          {sso && t('auth.social.text', { provider: LABEL[sso], email: 'mai.tran@gmail.com' })}
        </p>
      </Modal>
    </>
  );
}
