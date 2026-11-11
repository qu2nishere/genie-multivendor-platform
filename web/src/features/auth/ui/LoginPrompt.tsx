import { useStore } from '@nanostores/react';
import { useT } from '@/shared/hooks/useT';
import { buttonClass, Button, Icon, Modal } from '@/shared/ui';
import { $loginPrompt, closeLoginPrompt, loginHref } from '../application/loginPrompt.store';

/** Hộp nhắc đăng nhập khi khách bấm thao tác cần tài khoản (prototype index.html LoginPrompt) — G07.20. */
export function LoginPrompt() {
  const { t } = useT();
  const state = useStore($loginPrompt);
  return (
    <Modal
      open={state.open}
      onClose={closeLoginPrompt}
      aria-label={t('auth.prompt.title')}
      width={340}
    >
      <div className="text-center py-1">
        <Icon name="user" size={46} className="text-muted mx-auto mb-3.5" />
        <h2 className="m-0 mb-2 font-bold text-lg text-strong">{t('auth.prompt.title')}</h2>
        <p className="m-0 mb-[22px] text-base text-muted leading-normal">{t('auth.prompt.text')}</p>
        <div className="flex gap-3">
          <Button variant="secondary" className="flex-1" onClick={closeLoginPrompt}>
            {t('auth.prompt.later')}
          </Button>
          <a href={loginHref(state.next)} className={buttonClass({ className: 'flex-1' })}>
            {t('common.action.login')}
          </a>
        </div>
      </div>
    </Modal>
  );
}
