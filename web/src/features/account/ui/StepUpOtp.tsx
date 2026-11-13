// Ô nhập OTP bước 2 (API_SPEC 0.7) dùng chung: đổi mật khẩu, xóa tài khoản, đổi email. Báo email nhận
// mã (đã che), đếm ngược "Gửi lại mã" 60 giây, lỗi sai mã / hết hạn hiện ngay dưới ô.
import { useState } from 'react';
import { USE_MOCK } from '@/shared/api/mock';
import { errorMessage } from '@/shared/api/errors';
import { useCountdown } from '@/shared/hooks/useCountdown';
import { useT } from '@/shared/hooks/useT';
import { Icon, OtpInput, toast } from '@/shared/ui';
import { useResendStepUp } from '../application/useAccount';
import type { StepUpChallenge } from '../domain/account.types';

export interface StepUpOtpProps {
  challenge: StepUpChallenge;
  value: string;
  onChange: (v: string) => void;
  error?: string | null;
  disabled?: boolean;
  /** Đổi email: mã gửi tới email mới, không gửi lại qua 0.7. */
  resendable?: boolean;
  onResent?: (c: StepUpChallenge) => void;
}

export function StepUpOtp({
  challenge,
  value,
  onChange,
  error,
  disabled,
  resendable = true,
  onResent,
}: StepUpOtpProps) {
  const { t } = useT();
  const cd = useCountdown(60);
  const resend = useResendStepUp();
  const [id] = useState(() => `otp-${challenge.id}`);
  return (
    <div>
      <p className="m-0 mb-2.5 flex items-center gap-2 text-sm text-body">
        <Icon name="mail" size={16} className="text-brand-500 flex-none" />
        <span>
          {t('account.otp.sent', { email: challenge.maskedTarget })}
          {USE_MOCK && <span className="text-muted"> ({t('account.otp.demo')})</span>}
        </span>
      </p>
      <OtpInput
        value={value}
        onChange={onChange}
        disabled={disabled}
        invalid={!!error}
        autoFocus
        aria-describedby={error ? `${id}-err` : undefined}
      />
      <div className="flex items-center gap-3 mt-2 min-h-6">
        {error && (
          <span id={`${id}-err`} className="text-xs text-red-600" role="alert">
            {error}
          </span>
        )}
        {resendable && (
          <span className="ml-auto text-xs text-subtle">
            {cd.done ? (
              <button
                type="button"
                className="border-0 bg-transparent p-0 text-brand-600 font-semibold cursor-pointer min-h-8"
                onClick={() =>
                  resend.mutate(challenge.id, {
                    onSuccess: (c) => {
                      cd.reset();
                      onResent?.(c);
                      toast.success(t('account.otp.resent'));
                    },
                    onError: (e) => toast.error(errorMessage(e, t)),
                  })
                }
              >
                {t('account.otp.resend')}
              </button>
            ) : (
              t('account.otp.resend_in', { s: cd.left })
            )}
          </span>
        )}
      </div>
    </div>
  );
}
