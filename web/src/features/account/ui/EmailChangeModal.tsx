// "Đổi email đăng nhập" (prototype Account.jsx EmailChangeModal) — plan G10.47: email mới → mã gửi tới
// email MỚI → xác nhận (API_SPEC 3.2). Liên kết Google giữ nguyên.
import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { errorMessage, isApiError } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { zodResolver } from '@/shared/lib/zodResolver';
import { Button, Input, Modal, toast } from '@/shared/ui';
import { useConfirmEmailChange, useRequestEmailChange } from '../application/useAccount';
import { emailChangeSchema } from '../domain/account.schema';
import type { StepUpChallenge } from '../domain/account.types';
import { StepUpOtp } from './StepUpOtp';

export interface EmailChangeModalProps {
  open: boolean;
  current: string;
  onClose: () => void;
}

export function EmailChangeModal({ open, current, onClose }: EmailChangeModalProps) {
  const { t } = useT();
  const request = useRequestEmailChange();
  const confirm = useConfirmEmailChange();
  const [challenge, setChallenge] = useState<StepUpChallenge | null>(null);
  const [code, setCode] = useState('');
  const [otpErr, setOtpErr] = useState<string | null>(null);
  const form = useForm<{ email: string }>({
    resolver: zodResolver(emailChangeSchema(current)),
    defaultValues: { email: '' },
  });
  useEffect(() => {
    if (open) {
      setChallenge(null);
      setCode('');
      setOtpErr(null);
      form.reset({ email: '' });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  const send = form.handleSubmit((v) =>
    request.mutate(v.email, {
      onSuccess: setChallenge,
      onError: (e) =>
        form.setError('email', {
          message:
            isApiError(e) && ['GNE-AUTH-4094', 'GNE-AUTH-4224', 'GNE-AUTH-4095'].includes(e.code)
              ? errorMessage(e, t)
              : errorMessage(e, t),
        }),
    }),
  );
  const ok = () =>
    challenge &&
    confirm.mutate(
      { challengeId: challenge.id, code },
      {
        onSuccess: (r) => {
          toast.success(t('account.email.done', { email: r.email }));
          onClose();
        },
        onError: (e) => {
          if (isApiError(e) && e.code === 'GNE-AUTH-4221') {
            toast.error(errorMessage(e, t));
            onClose();
            return;
          }
          setOtpErr(errorMessage(e, t));
          setCode('');
        },
      },
    );
  const msg = form.formState.errors.email?.message;

  return (
    <Modal
      open={open}
      onClose={onClose}
      width={440}
      title={t('account.email.title')}
      footer={
        challenge ? (
          <>
            <Button variant="ghost" onClick={() => setChallenge(null)}>
              {t('account.email.other')}
            </Button>
            <Button
              iconLeft="check"
              disabled={code.length < 6}
              loading={confirm.isPending}
              onClick={ok}
            >
              {t('common.action.confirm')}
            </Button>
          </>
        ) : (
          <>
            <Button variant="ghost" onClick={onClose}>
              {t('common.action.cancel')}
            </Button>
            <Button iconLeft="mail" loading={request.isPending} onClick={() => void send()}>
              {t('account.email.send')}
            </Button>
          </>
        )
      }
    >
      {challenge ? (
        <StepUpOtp
          challenge={challenge}
          value={code}
          onChange={setCode}
          error={otpErr}
          resendable={false}
        />
      ) : (
        <form className="flex flex-col gap-3" onSubmit={(e) => void send(e)} noValidate>
          <p className="m-0 text-sm text-muted">
            {t('account.email.current')} <b className="text-body">{current}</b>
          </p>
          <Input
            label={t('account.email.new')}
            type="email"
            autoComplete="email"
            placeholder="ban@email.com"
            error={msg ? (msg.includes('.') && !msg.includes(' ') ? t(msg) : msg) : undefined}
            {...form.register('email')}
          />
          <p className="m-0 text-xs text-subtle leading-normal">{t('account.email.note')}</p>
        </form>
      )}
    </Modal>
  );
}
