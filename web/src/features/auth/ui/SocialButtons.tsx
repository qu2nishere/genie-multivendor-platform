// Đăng nhập Google / Zalo (prototype auth SocialButton, SsoDialog) — plan G09.10, A14.10.
// Thật: đánh dấu nhà cung cấp (sessionStorage) rồi chuyển sang GET /auth/oauth/{provider}/authorize → trang
// /auth/oauth-callback hoàn tất. Mock: hộp "Tiếp tục với …" (mô phỏng) rồi đăng nhập luôn (Zalo: sang bước nhập email).
import { useState } from 'react';
import { errorMessage } from '@/shared/api/errors';
import { markOAuthIntent } from '@/shared/auth/oauth-intent';
import { isMocked } from '@/shared/config/env';
import { env } from '@/shared/config/env';
import { useT } from '@/shared/hooks/useT';
import { Button, Modal, SocialLogo, toast } from '@/shared/ui';
import { completeLogin } from './completeLogin';
import type { SocialProvider } from '../domain/auth.types';
import { finishOAuth, oauthStartUrl } from '../application/useAuth';

const USE_MOCK = isMocked('auth');

export const SOCIAL_LABEL: Record<SocialProvider, string> = { google: 'Google', zalo: 'Zalo' };
const PROVIDERS: SocialProvider[] = ['google', 'zalo'];

export function SocialButtons({ next }: { next: string | null }) {
  const { t } = useT();
  const [sso, setSso] = useState<SocialProvider | null>(null);
  const [busy, setBusy] = useState(false);

  const pick = (p: SocialProvider) => {
    if (USE_MOCK) {
      setSso(p);
      return;
    }
    // state / nonce / PKCE do backend sinh; `next` backend giữ trong state; callback cố định → nhớ nhà cung cấp
    markOAuthIntent(p);
    window.location.assign(oauthStartUrl(p, env.PUBLIC_API_URL, next));
  };

  const continueMock = async () => {
    if (!sso) return;
    setBusy(true);
    try {
      const r = await finishOAuth(sso, 'mock-code', 'mock-state');
      if (r.result === 'SIGNUP_REQUIRED') {
        // mô phỏng: trang callback hỏi email
        markOAuthIntent(sso);
        window.location.assign('/auth/oauth-callback?code=mock-code&state=mock-state');
        return;
      }
      await completeLogin(r.session, next);
    } catch (e) {
      toast.error(errorMessage(e, t));
      setBusy(false);
    }
  };

  return (
    <>
      <div className="flex gap-2.5">
        {PROVIDERS.map((p) => (
          <button
            key={p}
            type="button"
            onClick={() => pick(p)}
            className="inline-flex h-[46px] flex-1 cursor-pointer items-center justify-center gap-[9px] rounded-md border-[1.5px] border-solid border-line bg-surface-card text-sm font-semibold text-strong hover:border-line-strong focus-visible:outline-none focus-visible:shadow-focus"
          >
            <SocialLogo provider={p} />
            {SOCIAL_LABEL[p]}
          </button>
        ))}
      </div>
      <Modal
        open={sso !== null}
        onClose={() => setSso(null)}
        width={400}
        title={sso ? t('auth.social.title', { provider: SOCIAL_LABEL[sso] }) : ''}
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
          {sso &&
            (sso === 'zalo'
              ? t('auth.social.text_zalo')
              : t('auth.social.text', {
                  provider: SOCIAL_LABEL[sso],
                  email: 'mai.tran@gmail.com',
                }))}
        </p>
      </Modal>
    </>
  );
}
