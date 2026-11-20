// Kích hoạt tài khoản nhân viên từ liên kết email mời (UC-ADM-01 bước 5; API_SPEC 2.11; plan B1 R08.02, mục 3.3, 3.7).
// Mở trang → tự xin OTP gửi tới email được mời → nhập OTP + mật khẩu mới (một lần gửi, vì API nhận cả hai) →
// đăng nhập và chuyển sang /admin. Lời mời sai / hết hạn / đã dùng (GNE-AUTH-4103) → màn "Lời mời đã hết hạn";
// sai OTP 5 lần (GNE-AUTH-4221) → màn tạm khóa, cho xin mã mới.
import { useCallback, useEffect, useRef, useState, type FormEvent, type ReactNode } from 'react';
import { errorMessage, toApiError } from '@/shared/api/errors';
import { isMocked } from '@/shared/config/env';
import { useCountdown } from '@/shared/hooks/useCountdown';
import { useT } from '@/shared/hooks/useT';
import { formatMmSs } from '@/shared/lib/format';
import { Providers } from '@/shared/providers/Providers';
import { Button, Icon, OtpInput, PasswordInput, Spinner } from '@/shared/ui';
import {
  finishLogin,
  useActivateStaff,
  useResendOtp,
  useStaffActivationOtp,
} from '../application/useAuth';
import { isInvitationToken, OTP_LENGTH } from '../domain/auth.rules';
import { fieldErrors, staffActivationSchema } from '../domain/auth.schemas';
import type { OtpChallenge } from '../domain/auth.types';
import { AuthHeading, DemoHint, Notice, StateIcon } from './AuthParts';

const USE_MOCK = isMocked('auth');
/** Trang đích sau khi kích hoạt: Admin Portal. */
const AFTER_ACTIVATION = '/admin';

type Stage = 'loading' | 'form' | 'expired' | 'blocked' | 'error' | 'done';

function Form({ token }: { token: string }) {
  const { t } = useT();
  const [stage, setStage] = useState<Stage>(isInvitationToken(token) ? 'loading' : 'expired');
  const [challenge, setChallenge] = useState<OtpChallenge | null>(null);
  const [otp, setOtp] = useState('');
  const [pw, setPw] = useState('');
  const [pw2, setPw2] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [notice, setNotice] = useState<string | null>(null);
  const requestOtp = useStaffActivationOtp();
  const resend = useResendOtp();
  const activate = useActivateStaff();
  const expire = useCountdown(challenge?.expiresIn ?? 0, !!challenge);
  const resendIn = useCountdown(challenge?.resendAfter ?? 0, !!challenge);
  const started = useRef(false);
  const tr = (e: Record<string, string>) =>
    Object.fromEntries(Object.entries(e).map(([k, v]) => [k, t(v)]));

  const begin = useCallback(
    (c: OtpChallenge) => {
      setChallenge(c);
      setOtp('');
      setErrors({});
      expire.reset(c.expiresIn);
      resendIn.reset(c.resendAfter);
      setStage('form');
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [],
  );

  const ask = useCallback(() => {
    setStage('loading');
    setNotice(null);
    requestOtp.mutate(token, {
      onSuccess: begin,
      onError: (e) => {
        const err = toApiError(e);
        if (err.code === 'GNE-AUTH-4103') setStage('expired');
        else {
          setNotice(errorMessage(err, t));
          setStage('error');
        }
      },
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token, begin, t]);

  // Xin OTP một lần khi mở trang (StrictMode gọi effect hai lần ở dev).
  useEffect(() => {
    if (started.current || !isInvitationToken(token)) return;
    started.current = true;
    ask();
  }, [ask, token]);

  const doResend = () => {
    if (!challenge) return;
    resend.mutate(challenge.id, {
      onSuccess: begin,
      onError: (e) => setErrors({ otp: errorMessage(e, t) }),
    });
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    if (!challenge || activate.isPending) return;
    const parsed = staffActivationSchema.safeParse({ otp, password: pw, confirm: pw2 });
    if (!parsed.success) {
      setErrors(tr(fieldErrors(parsed.error)));
      return;
    }
    setErrors({});
    activate.mutate(
      { invitationToken: token, challengeId: challenge.id, otp, password: pw },
      {
        onSuccess: (session) => {
          setStage('done');
          void finishLogin(session, AFTER_ACTIVATION);
        },
        onError: (err) => {
          const ae = toApiError(err);
          if (ae.code === 'GNE-AUTH-4103') setStage('expired');
          else if (ae.code === 'GNE-AUTH-4221') setStage('blocked');
          else if (ae.code.startsWith('GNE-AUTH-4')) {
            setOtp('');
            setErrors({ otp: errorMessage(ae, t) });
          } else setErrors({ password: errorMessage(ae, t) });
        },
      },
    );
  };

  if (stage === 'loading' || stage === 'done') {
    const label = t(
      stage === 'done' ? 'auth.staff_activation.signing_in' : 'auth.staff_activation.sending',
    );
    return (
      <div
        className="mx-auto grid w-full max-w-[380px] place-items-center gap-4 text-center"
        role="status"
      >
        <Spinner size={36} label={label} />
        <p className="m-0 text-base text-muted">{label}</p>
      </div>
    );
  }

  if (stage === 'expired') {
    return (
      <div className="mx-auto w-full max-w-[380px] text-center">
        <StateIcon icon="clock" tone="error" />
        <AuthHeading
          center
          title={t('auth.staff_activation.expired_title')}
          lead={t('auth.staff_activation.expired_text')}
        />
        <Button block size="lg" variant="secondary" href="/auth/login">
          {t('auth.forgot.back')}
        </Button>
      </div>
    );
  }

  if (stage === 'blocked' || stage === 'error') {
    const blocked = stage === 'blocked';
    return (
      <div className="mx-auto w-full max-w-[380px] text-center">
        <StateIcon icon={blocked ? 'lock' : 'circle-alert'} tone="error" />
        <AuthHeading
          center
          title={t(
            blocked ? 'auth.staff_activation.blocked_title' : 'auth.staff_activation.error_title',
          )}
          lead={blocked ? t('auth.staff_activation.blocked_text') : notice}
        />
        <Button block size="lg" onClick={ask} loading={requestOtp.isPending}>
          {t('auth.staff_activation.new_code')}
        </Button>
      </div>
    );
  }

  // stage === 'form'
  const expired = expire.done;
  return (
    <div className="mx-auto w-full max-w-[400px]">
      <AuthHeading
        center
        title={t('auth.staff_activation.title')}
        lead={t('auth.staff_activation.lead')}
      />

      {/* Email được mời — mã gửi về đây (một dòng, màu xanh) */}
      <p className="m-0 -mt-2 mb-6 text-center text-sm font-medium text-mint-600">
        {t('auth.staff_activation.sent_to')}{' '}
        <b className="font-semibold">{challenge?.emailMasked}</b>
        {USE_MOCK && (
          <>
            {' '}
            <DemoHint>{t('auth.otp.demo', { code: '123456' })}</DemoHint>
          </>
        )}
      </p>

      {notice && <Notice tone="error">{notice}</Notice>}
      <form className="flex flex-col gap-6" onSubmit={submit} noValidate>
        {/* Bước 1 — mã xác thực */}
        <section aria-labelledby="staff-step-otp">
          <StepTitle id="staff-step-otp" n={1}>
            {t('auth.staff_activation.step_otp')}
          </StepTitle>
          <OtpInput
            value={otp}
            onChange={(v) => {
              setOtp(v);
              setErrors((e) => ({ ...e, otp: '' }));
            }}
            disabled={expired || activate.isPending}
            invalid={!!errors.otp}
            autoFocus
            aria-describedby="staff-otp-status"
          />
          <div
            id="staff-otp-status"
            className="mt-2.5 flex items-center justify-between gap-3 text-sm"
            aria-live="polite"
          >
            {expired ? (
              <span className="text-red-600">{t('auth.otp.expired')}</span>
            ) : (
              <span className="text-muted">
                {t('auth.otp.expires_in')}{' '}
                <b className="code text-body">{formatMmSs(expire.left)}</b>
              </span>
            )}
            {resendIn.done ? (
              <button
                type="button"
                onClick={doResend}
                disabled={resend.isPending}
                className="inline-flex min-h-[32px] flex-none cursor-pointer items-center gap-1 border-0 bg-transparent p-0 font-semibold text-link hover:underline"
              >
                <Icon name="refresh-cw" size={14} />
                {t('auth.staff_activation.resend')}
              </button>
            ) : (
              <span className="flex-none text-subtle">
                {t('auth.staff_activation.resend_in', { time: formatMmSs(resendIn.left) })}
              </span>
            )}
          </div>
          {errors.otp && (
            <span className="mt-2 flex items-center gap-1.5 text-sm text-red-600" role="alert">
              <Icon name="circle-alert" size={15} className="flex-none" />
              {errors.otp}
            </span>
          )}
        </section>

        <div className="h-px bg-line-subtle" aria-hidden="true" />

        {/* Bước 2 — mật khẩu */}
        <section aria-labelledby="staff-step-password" className="flex flex-col gap-3.5">
          <StepTitle id="staff-step-password" n={2}>
            {t('auth.staff_activation.step_password')}
          </StepTitle>
          <PasswordInput
            label={t('auth.field.new_password')}
            name="new-password"
            autoComplete="new-password"
            showStrength
            value={pw}
            onChange={(e) => {
              setPw(e.target.value);
              setErrors((x) => ({ ...x, password: '' }));
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
              setErrors((x) => ({ ...x, confirm: '' }));
            }}
            error={errors.confirm || undefined}
            placeholder={t('auth.field.confirm_ph')}
          />
        </section>

        <Button
          type="submit"
          block
          size="lg"
          iconLeft="arrow-right"
          disabled={otp.length < OTP_LENGTH || expired}
          loading={activate.isPending}
        >
          {t('auth.staff_activation.submit')}
        </Button>
      </form>
    </div>
  );
}

/** Tiêu đề bước có số thứ tự tròn. */
function StepTitle({ id, n, children }: { id: string; n: number; children: ReactNode }) {
  return (
    <h2 id={id} className="m-0 mb-3 flex items-center gap-2 text-base font-semibold text-strong">
      <span className="grid h-6 w-6 flex-none place-items-center rounded-full bg-primary text-xs font-bold text-primary-on">
        {n}
      </span>
      {children}
    </h2>
  );
}

export default function StaffActivationForm({ token = '' }: { token?: string }) {
  return (
    <Providers>
      <Form token={token} />
    </Providers>
  );
}
