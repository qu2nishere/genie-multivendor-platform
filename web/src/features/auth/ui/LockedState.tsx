// Màn "Tài khoản tạm khoá" (prototype auth: step "locked") — plan G09.07. Đếm ngược 15 phút
// (theo locked_until server trả), "Xác minh qua email" (OTP ACCOUNT_UNLOCK), "Liên hệ hỗ trợ".
import { useT } from '@/shared/hooks/useT';
import { useCountdown } from '@/shared/hooks/useCountdown';
import { formatMmSs } from '@/shared/lib/format';
import { Button, Icon } from '@/shared/ui';
import { AuthHeading, StateIcon } from './AuthParts';

export interface LockedStateProps {
  /** Giây còn khóa. */
  seconds: number;
  lockedBy: 'PASSWORD' | 'OTP';
  unlocking?: boolean;
  onUnlock: () => void;
}

export function LockedState({ seconds, lockedBy, unlocking, onUnlock }: LockedStateProps) {
  const { t } = useT();
  const { left } = useCountdown(seconds);
  return (
    <div className="text-center">
      <StateIcon icon="lock" tone="error" />
      <AuthHeading
        center
        title={t('auth.locked.title')}
        lead={
          <>
            {t('auth.locked.text_a', { by: t(`auth.locked.by_${lockedBy.toLowerCase()}`) })}{' '}
            <b className="text-red-600">{t('auth.locked.times')}</b>. {t('auth.locked.text_b')}
          </>
        }
      />
      <div
        role="timer"
        className="mb-6 inline-flex items-center gap-2 rounded-md bg-red-50 px-4 py-2.5 text-base font-semibold text-red-600"
      >
        <Icon name="clock" size={18} /> {t('auth.locked.retry_in')}{' '}
        <span className="code">{formatMmSs(left)}</span>
      </div>
      <div className="flex flex-col gap-2.5">
        <Button
          block
          size="lg"
          variant="secondary"
          iconLeft="shield-check"
          loading={unlocking}
          onClick={onUnlock}
        >
          {t('auth.locked.unlock')}
        </Button>
        <Button block variant="ghost" iconLeft="circle-help" href="/policies/help-center">
          {t('auth.locked.support')}
        </Button>
      </div>
    </div>
  );
}

/** Giây còn khóa từ details.locked_until (ISO); thiếu → 15 phút. */
export function lockSecondsFrom(details: Record<string, unknown>, fallback = 900): number {
  const until = typeof details.locked_until === 'string' ? Date.parse(details.locked_until) : NaN;
  return Number.isFinite(until) ? Math.max(0, Math.round((until - Date.now()) / 1000)) : fallback;
}
