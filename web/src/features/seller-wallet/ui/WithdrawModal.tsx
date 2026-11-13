// "Yêu cầu rút tiền" (prototype Wallet.jsx WithdrawModal) — plan G11.35: số tiền (tối thiểu 100.000₫, không
// vượt khả dụng, "Rút toàn bộ"), tài khoản nhận đã xác minh, OTP bước 2 `WITHDRAWAL` (API 0.7) → POST
// /seller/withdrawals. Chưa có tài khoản nhận → dẫn sang Cài đặt.
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { StepUpOtp, useRequestStepUp, type StepUpChallenge } from '@/features/account';
import { errorMessage, isApiError } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { formatVND } from '@/shared/lib/format';
import { Badge, Button, Icon, Input, Modal, toast } from '@/shared/ui';
import { useWithdraw } from '../application/useWallet';
import { withdrawIssue } from '../domain/wallet.rules';
import type { Wallet } from '../domain/wallet.types';

export function WithdrawModal({ wallet, onClose }: { wallet: Wallet; onClose: () => void }) {
  const { t, locale } = useT();
  const [amount, setAmount] = useState<number | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [challenge, setChallenge] = useState<StepUpChallenge | null>(null);
  const [otp, setOtp] = useState('');
  const [otpErr, setOtpErr] = useState<string | null>(null);
  const stepUp = useRequestStepUp();
  const withdraw = useWithdraw();
  const acc = wallet.payoutAccount;
  const vnd = (n: number) => formatVND(n, { locale });

  const next = () => {
    const n = amount ?? 0;
    const issue = withdrawIssue(n, wallet);
    if (issue === 'min') return setErr(t('errors.GNE-WAL-4221'));
    if (issue === 'over') return setErr(t('errors.GNE-WAL-4001'));
    setErr(null);
    stepUp.mutate('WITHDRAWAL', {
      onSuccess: (c) => {
        setOtp('');
        setOtpErr(null);
        setChallenge(c);
      },
      onError: (e) => setErr(errorMessage(e, t)),
    });
  };

  const confirm = () => {
    if (!challenge || amount === null) return;
    withdraw.mutate(
      { amount, proof: { challengeId: challenge.id, code: otp } },
      {
        onSuccess: (w) => {
          toast.success(t('seller-wallet.withdraw.done', { code: w.code }), 6000);
          onClose();
        },
        onError: (e) => {
          if (isApiError(e) && e.code.startsWith('GNE-AUTH')) setOtpErr(errorMessage(e, t));
          else {
            setChallenge(null);
            setErr(errorMessage(e, t));
          }
        },
      },
    );
  };

  return (
    <Modal
      open
      onClose={onClose}
      width={460}
      title={t('seller-wallet.withdraw.title')}
      dismissible={!withdraw.isPending}
      footer={
        !acc?.verified ? (
          <Button variant="ghost" onClick={onClose}>
            {t('common.action.close')}
          </Button>
        ) : !challenge ? (
          <>
            <Button variant="ghost" onClick={onClose}>
              {t('common.action.cancel')}
            </Button>
            <Button loading={stepUp.isPending} disabled={!amount} onClick={next}>
              {t('seller-wallet.withdraw.next')}
            </Button>
          </>
        ) : (
          <>
            <Button
              variant="ghost"
              disabled={withdraw.isPending}
              onClick={() => setChallenge(null)}
            >
              {t('common.action.back')}
            </Button>
            <Button
              iconLeft="check"
              loading={withdraw.isPending}
              disabled={otp.length < 6}
              onClick={confirm}
            >
              {t('seller-wallet.withdraw.confirm')}
            </Button>
          </>
        )
      }
    >
      {!acc?.verified ? (
        <p className="m-0 text-base text-body leading-normal">
          {t('seller-wallet.withdraw.no_account')}{' '}
          <Link to="/settings#payout" onClick={onClose} className="text-brand-600 font-semibold">
            {t('seller-wallet.withdraw.add_account')}
          </Link>
        </p>
      ) : (
        <div className="flex flex-col gap-3.5">
          <div className="flex justify-between text-sm">
            <span className="text-muted">{t('seller-wallet.balance.available')}</span>
            <b className="tabular-nums">{vnd(wallet.available)}</b>
          </div>
          <div className="flex items-center gap-2.5 px-3 py-2.5 border border-solid border-line-subtle rounded-md">
            <Icon name="credit-card" size={18} className="text-blue-600" />
            <div className="flex-1 min-w-0">
              <div className="font-semibold text-sm text-strong">
                {acc.bank} · {acc.holder}
              </div>
              <div className="code text-xs text-muted">{acc.numberMasked}</div>
            </div>
            <Badge variant="mint" icon="badge-check">
              {t('seller-wallet.withdraw.verified')}
            </Badge>
          </div>
          {!challenge ? (
            <div>
              <Input
                label={t('seller-wallet.withdraw.amount')}
                inputMode="numeric"
                autoFocus
                value={amount === null ? '' : formatVND(amount, { locale, symbol: false })}
                onChange={(e) => {
                  const d = e.target.value.replace(/\D/g, '');
                  setAmount(d ? Number(d) : null);
                  setErr(null);
                }}
                error={err ?? undefined}
                placeholder={t('seller-wallet.withdraw.placeholder', {
                  min: vnd(wallet.minWithdrawal),
                })}
                suffix="₫"
              />
              <button
                type="button"
                onClick={() => setAmount(wallet.available)}
                className="mt-1.5 border-0 p-0 bg-transparent text-brand-600 text-xs font-semibold cursor-pointer"
              >
                {t('seller-wallet.withdraw.all')}
              </button>
            </div>
          ) : (
            <div>
              <p className="m-0 mb-2 text-sm text-body">
                {t('seller-wallet.withdraw.otp_for', { amount: vnd(amount ?? 0) })}
              </p>
              <StepUpOtp
                challenge={challenge}
                value={otp}
                onChange={(v) => {
                  setOtp(v);
                  setOtpErr(null);
                }}
                error={otpErr}
                disabled={withdraw.isPending}
                onResent={setChallenge}
              />
            </div>
          )}
        </div>
      )}
    </Modal>
  );
}
