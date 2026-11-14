// Bước nhập OTP (prototype auth: step "otp") — plan G09.06. Mã 6 số, hết hạn sau 5 phút (đếm ngược),
// gửi lại sau 60 giây, báo số lần thử còn lại; sai lần thứ 5 → onExhausted (khóa / hủy đăng ký).
import { useEffect, useState } from 'react';
import { errorMessage, toApiError, type ApiError } from '@/shared/api/errors';
import { USE_MOCK } from '@/shared/api/mock';
import type { SessionUser } from '@/shared/auth/session';
import { useCountdown } from '@/shared/hooks/useCountdown';
import { useT } from '@/shared/hooks/useT';
import { formatMmSs } from '@/shared/lib/format';
import { Button, Icon, OtpInput } from '@/shared/ui';
import { useResendOtp, useVerifyOtp } from '../application/useAuth';
import { OTP_LENGTH } from '../domain/auth.rules';
import type { OtpChallenge, VerifyResult } from '../domain/auth.types';
import { AuthHeading, BackButton, DemoHint } from './AuthParts';

export interface OtpStepProps {
  email: string;
  challenge: OtpChallenge;
  title: string;
  backLabel: string;
  onBack: () => void;
  onVerified: (r: VerifyResult<SessionUser>) => void;
  /** Sai quá 5 lần (GNE-AUTH-4221). */
  onExhausted: (e: ApiError) => void;
}

export function OtpStep({
  email,
  challenge: initial,
  title,
  backLabel,
  onBack,
  onVerified,
  onExhausted,
}: OtpStepProps) {
  const { t } = useT();
  const [challenge, setChallenge] = useState(initial);
  const [otp, setOtp] = useState('');
  const [error, setError] = useState<string | null>(null);
  const expire = useCountdown(challenge.expiresIn);
  const resend = useCountdown(challenge.resendAfter);
  const verify = useVerifyOtp();
  const again = useResendOtp();
  const expired = expire.done;

  useEffect(() => {
    expire.reset(challenge.expiresIn);
    resend.reset(challenge.resendAfter);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [challenge.id]);

  const submit = (code = otp) => {
    if (code.length < OTP_LENGTH || verify.isPending) return;
    setError(null);
    verify.mutate(
      { challengeId: challenge.id, otp: code, email },
      {
        onSuccess: onVerified,
        onError: (e) => {
          const err = toApiError(e);
          setOtp('');
          if (err.code === 'GNE-AUTH-4221') onExhausted(err);
          else setError(errorMessage(err, t));
        },
      },
    );
  };

  const doResend = () =>
    again.mutate(challenge.id, {
      onSuccess: (c) => {
        setChallenge(c);
        setOtp('');
        setError(null);
      },
      onError: (e) => setError(errorMessage(e, t)),
    });

  return (
    <div>
      <BackButton onClick={onBack}>{backLabel}</BackButton>
      <AuthHeading
        title={title}
        lead={
          <>
            {t('auth.otp.sent_a')} <b className="text-body">{email}</b>.{' '}
            {USE_MOCK && <DemoHint>{t('auth.otp.demo', { code: '123456' })}</DemoHint>}
          </>
        }
      />
      <OtpInput
        value={otp}
        onChange={(v) => {
          setOtp(v);
          setError(null);
        }}
        onComplete={(v) => submit(v)}
        disabled={expired || verify.isPending}
        invalid={!!error}
        autoFocus
        size="lg"
        aria-describedby="otp-status"
      />
      <div id="otp-status" className="mt-3 text-sm" aria-live="polite">
        {expired ? (
          <span className="text-red-600">{t('auth.otp.expired')}</span>
        ) : (
          <span className="text-muted">
            {t('auth.otp.expires_in')} <b className="code">{formatMmSs(expire.left)}</b>
          </span>
        )}
        {error && (
          <span className="mt-3.5 flex items-center gap-1.5 text-red-600" role="alert">
            <Icon name="circle-alert" size={15} className="flex-none" />
            {error}
          </span>
        )}
      </div>
      <Button
        block
        size="lg"
        className="mt-5"
        disabled={otp.length < OTP_LENGTH || expired}
        loading={verify.isPending}
        onClick={() => submit()}
      >
        {t('common.action.confirm')}
      </Button>
      <p className="m-0 mt-[18px] text-center text-sm text-muted">
        {resend.done ? (
          <button
            type="button"
            onClick={doResend}
            disabled={again.isPending}
            className="min-h-[44px] cursor-pointer border-0 bg-transparent font-semibold text-link hover:underline"
          >
            {t('auth.otp.resend')}
          </button>
        ) : (
          <>
            {t('auth.otp.resend_in')} <b className="code">{formatMmSs(resend.left)}</b>
          </>
        )}
      </p>
    </div>
  );
}
