// Luồng thanh toán (prototype Shared.jsx MoneyChip, VietQrPanel, PayModal, PayGateway) — plan G10.23–G10.25.
// Thành phần chỉ vẽ: dữ liệu phiên / hành động do trang Thanh toán hoặc trang kết quả truyền vào.
import { useEffect, useState } from 'react';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatMmSs, formatVND } from '@/shared/lib/format';
import { Button, Icon, Modal, QrPlaceholder, Radio, toast } from '@/shared/ui';
import { moneyChip, type PayMethod, type VietQrView } from '../domain/payment.types';
import MoneyRail from './MoneyRail';
import { PaymentLogo } from './PaymentLogos';

/** Nhãn "Genie đang giữ tiền / đã chuyển cho shop / trả khi nhận" trên thẻ đơn. */
export function MoneyChip({ status, method }: { status: string; method: PayMethod }) {
  const { t } = useT();
  const c = moneyChip(status, method);
  if (!c) return null;
  return (
    <span className={`tr-moneychip tr-moneychip--${c.tone}`}>
      <Icon name={c.tone === 'done' ? 'circle-check' : 'shield-check'} size={13} />
      {t(c.key)}
    </span>
  );
}

const useSecondsTo = (iso: string) => {
  const calc = () => Math.max(0, Math.ceil((Date.parse(iso) - Date.now()) / 1000));
  const [left, setLeft] = useState(calc);
  useEffect(() => {
    const i = window.setInterval(() => setLeft(calc()), 1000);
    return () => window.clearInterval(i);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [iso]);
  return left;
};

/** Mã VietQR giữ chỗ + thông tin chuyển khoản; chờ SePay xác nhận (trang gọi lại phiên mỗi 3 giây). */
export function VietQrPanel({ qr, onBack }: { qr: VietQrView; onBack: () => void }) {
  const { t, locale } = useT();
  const left = useSecondsTo(qr.expiresAt);
  const expired = left === 0;
  const copy = (v: string) => {
    void navigator.clipboard?.writeText(v).catch(() => undefined);
    toast.success(t('common.action.copied'));
  };
  const rows: { k: string; v: string; mono?: boolean; can?: boolean }[] = [
    { k: t('payment.qr.bank'), v: qr.bankName },
    {
      k: t('payment.qr.account'),
      v: qr.accountNumber.replace(/(\d{4})(?=\d)/g, '$1 '),
      mono: true,
      can: true,
    },
    { k: t('payment.qr.holder'), v: qr.accountName },
    { k: t('payment.qr.amount'), v: formatVND(qr.amount, { locale }) },
    { k: t('payment.qr.content'), v: qr.transferContent, mono: true, can: true },
  ];
  return (
    <div>
      <div className={cn('grid place-items-center p-3', expired && 'opacity-25')}>
        <QrPlaceholder seed={qr.qrPayload} />
      </div>
      <p
        className={cn('m-0 mb-2.5 text-center text-sm', expired ? 'text-red-600' : 'text-muted')}
        aria-live="polite"
      >
        {expired ? (
          t('payment.qr.expired')
        ) : (
          <>
            {t('payment.qr.valid')} <b className="font-mono text-amber-600">{formatMmSs(left)}</b>
          </>
        )}
      </p>
      <dl className="m-0">
        {rows.map((r) => (
          <div
            key={r.k}
            className="flex items-center gap-2 py-[7px] border-0 border-b border-solid border-line-subtle text-sm"
          >
            <dt className="text-muted w-28 flex-none">{r.k}</dt>
            <dd
              className={cn(
                'm-0 flex-1 min-w-0 font-bold text-strong tabular-nums',
                r.mono && 'font-mono',
              )}
            >
              {r.v}
            </dd>
            {r.can && (
              <button
                type="button"
                onClick={() => copy(r.v.replace(/\s/g, ''))}
                aria-label={t('payment.qr.copy', { what: r.k })}
                className="w-9 h-9 grid place-items-center border-0 bg-transparent text-brand-600 cursor-pointer rounded-md hover:bg-gray-50"
              >
                <Icon name="copy" size={15} />
              </button>
            )}
          </div>
        ))}
      </dl>
      <p className="m-0 mt-2.5 mb-3.5 text-xs text-muted leading-normal">{t('payment.qr.note')}</p>
      {!expired && (
        <p className="m-0 mb-3 flex items-center gap-2 text-sm text-brand-700">
          <Icon name="refresh-cw" size={15} className="animate-spin" />
          {t('payment.qr.waiting')}
        </p>
      )}
      <Button block variant="ghost" onClick={onBack}>
        {t(expired ? 'common.action.close' : 'payment.qr.change')}
      </Button>
    </div>
  );
}

export interface PayModalProps {
  open: boolean;
  amount: number;
  /** Có mã VietQR → hiện bước quét mã; không có → bước chọn phương thức. */
  qr: VietQrView | null;
  initial?: PayMethod;
  busy?: boolean;
  onPick: (m: PayMethod) => void;
  onBackFromQr: () => void;
  onClose: () => void;
}

/** Thanh toán lại / đổi phương thức cho phiên đang chờ (prototype PayModal). */
export function PayModal({
  open,
  amount,
  qr,
  initial = 'VNPAY',
  busy,
  onPick,
  onBackFromQr,
  onClose,
}: PayModalProps) {
  const { t, locale } = useT();
  const [method, setMethod] = useState<PayMethod>(initial);
  useEffect(() => {
    if (open) setMethod(initial);
  }, [open, initial]);
  const vnd = formatVND(amount, { locale });
  return (
    <Modal
      open={open}
      onClose={onClose}
      width={480}
      title={t(qr ? 'payment.modal.qr_title' : 'payment.modal.pick_title')}
      footer={
        qr ? undefined : (
          <>
            <Button variant="ghost" onClick={onClose}>
              {t('common.action.cancel')}
            </Button>
            <Button loading={busy} onClick={() => onPick(method)}>
              {method === 'COD'
                ? t('payment.modal.cod', { amount: vnd })
                : t('payment.modal.pay', { amount: vnd })}
            </Button>
          </>
        )
      }
    >
      {qr ? (
        <VietQrPanel qr={qr} onBack={onBackFromQr} />
      ) : (
        <div role="radiogroup" aria-label={t('payment.modal.pick_title')}>
          {(['VNPAY', 'VIETQR', 'COD'] as const).map((m) => (
            <label
              key={m}
              className={cn(
                'flex items-center gap-3 px-3.5 py-3 mb-2 rounded-md cursor-pointer border-[1.5px] border-solid',
                method === m ? 'border-primary bg-primary-tint' : 'border-line bg-surface-card',
              )}
            >
              <Radio name="repay" checked={method === m} onChange={() => setMethod(m)} />
              <span className="w-[30px] h-[30px] grid place-items-center flex-none">
                <PaymentLogo method={m} />
              </span>
              <span className="font-medium text-base text-strong">
                {t(`checkout.pay.${m}.name`)}
              </span>
              <span className="ml-auto text-xs text-muted text-right">
                {t(m === 'COD' ? 'payment.modal.cod_meta' : `checkout.pay.${m}.meta`)}
              </span>
            </label>
          ))}
        </div>
      )}
    </Modal>
  );
}

/**
 * Cổng VNPAY mô phỏng (chỉ chạy mock; thật thì trình duyệt chuyển sang payment_url của VNPAY).
 * Khoảnh khắc tiền của người mua di chuyển, hiện trên đường tiền.
 */
export function PayGateway({
  amount,
  orders,
  onPay,
  onCancel,
}: {
  amount: number;
  orders: number;
  onPay: () => void;
  onCancel: () => void;
}) {
  const { t, locale } = useT();
  const [paying, setPaying] = useState(false);
  const vnd = formatVND(amount, { locale });
  return (
    <Modal
      open
      onClose={paying ? () => undefined : onCancel}
      dismissible={!paying}
      width={520}
      title={t('payment.gateway.title')}
      footer={
        <>
          <Button variant="ghost" disabled={paying} onClick={onCancel}>
            {t('payment.gateway.back')}
          </Button>
          <Button
            iconLeft="lock"
            loading={paying}
            onClick={() => {
              setPaying(true);
              window.setTimeout(onPay, 1300);
            }}
          >
            {paying ? t('payment.gateway.moving') : t('payment.modal.pay', { amount: vnd })}
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-3.5">
        <div className="flex justify-between gap-3 text-sm text-muted">
          <span>
            {t('payment.gateway.payee')}{' '}
            <b className="text-strong">{t('payment.gateway.escrow')}</b>
          </span>
          <span>{t('payment.gateway.orders', { count: orders })}</span>
        </div>
        <div className="font-extrabold text-[30px] leading-tight text-price tabular-nums">
          {vnd}
        </div>
        <MoneyRail flat amount={amount} start={0} step={paying ? 1 : 0} />
        <p className="m-0 text-xs text-muted">{t('payment.gateway.note')}</p>
      </div>
    </Modal>
  );
}
