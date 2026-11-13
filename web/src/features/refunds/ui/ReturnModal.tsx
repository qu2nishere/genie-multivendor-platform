// "Yêu cầu Trả hàng / Hoàn tiền" (prototype OrderTracking.jsx ReturnModal) — plan G10.40: phương án,
// chọn món + số lượng, lý do (16.4), bằng chứng ≥ 1 ảnh + 1 video (≤ 10 MB/tệp, ảnh được nén), tài khoản
// nhận tiền hoàn cho đơn VietQR / COD (react-hook-form + zod), số tiền hoàn dự kiến.
import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { errorMessage } from '@/shared/api/errors';
import type { UploadedFile } from '@/shared/api/upload';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatVND } from '@/shared/lib/format';
import { zodResolver } from '@/shared/lib/zodResolver';
import {
  Button,
  Checkbox,
  Icon,
  Input,
  MediaPicker,
  Modal,
  QuantityStepper,
  Radio,
  Select,
  toast,
} from '@/shared/ui';
import { useBanks, useCreateRefund } from '../application/useRefunds';
import { evidenceMissing, needsBank, refundEstimate } from '../domain/refund.rules';
import { refundBankSchema, type RefundBankValues } from '../domain/refund.schema';
import { RETURN_REASONS, type RefundType, type ReturnReason } from '../domain/refund.types';

/** Đơn tối thiểu modal cần (khớp OrderDetail của orders theo cấu trúc). */
export interface ReturnOrder {
  id: number;
  paymentMethod: string;
  subtotal: number;
  discountTotal: number;
  items: {
    id: number;
    title: string;
    image: string;
    price: number;
    paidUnitPrice: number | null;
    qty: number;
  }[];
}

export interface ReturnModalProps {
  open: boolean;
  order: ReturnOrder;
  onClose: () => void;
  onDone?: () => void;
}

export function ReturnModal({ open, order, onClose, onDone }: ReturnModalProps) {
  const { t, locale } = useT();
  const bankNeeded = needsBank(order.paymentMethod);
  const banks = useBanks(open && bankNeeded);
  const create = useCreateRefund();
  const [type, setType] = useState<RefundType>('RETURN_AND_REFUND');
  const [reason, setReason] = useState<ReturnReason>('DAMAGED_IN_TRANSIT');
  const [picked, setPicked] = useState<Record<number, number>>({});
  const [files, setFiles] = useState<UploadedFile[]>([]);
  const form = useForm<RefundBankValues>({
    resolver: zodResolver(refundBankSchema),
    defaultValues: { code: 'VCB', accountNumber: '', holder: '' },
  });

  useEffect(() => {
    if (!open) return;
    setType('RETURN_AND_REFUND');
    setReason('DAMAGED_IN_TRANSIT');
    setPicked(Object.fromEntries(order.items.map((it) => [it.id, it.qty])));
    setFiles([]);
    form.reset();
    // chỉ đặt lại khi mở hộp
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  const lines = order.items
    .filter((it) => picked[it.id])
    .map((it) => ({ ...it, qty: picked[it.id] }));
  const amount = refundEstimate(lines, order);
  const missing = evidenceMissing(files);

  const send = (bank: RefundBankValues | null) =>
    create.mutate(
      {
        input: {
          orderId: order.id,
          requestType: type,
          reasonCode: reason,
          items: lines.map((l) => ({ orderItemId: l.id, qty: l.qty })),
          evidence: files.map((f) => ({ objectKey: f.objectKey, mediaType: f.mediaType })),
          bank: bank && { code: bank.code, accountNumber: bank.accountNumber, holder: bank.holder },
        },
        amount,
      },
      {
        onSuccess: () => {
          toast.success(t('refunds.form.sent'));
          onClose();
          onDone?.();
        },
        onError: (e) => toast.error(errorMessage(e, t)),
      },
    );
  const submit = () => {
    if (bankNeeded) void form.handleSubmit((b) => send(b))();
    else send(null);
  };
  const err = (m?: string) => (m ? t(m) : undefined);
  const lab = 'block font-semibold text-sm text-strong';

  return (
    <Modal
      open={open}
      onClose={onClose}
      width={620}
      dismissible={!create.isPending}
      title={t('refunds.form.title')}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            {t('common.action.cancel')}
          </Button>
          <Button
            iconLeft="undo-2"
            loading={create.isPending}
            disabled={lines.length === 0 || missing.length > 0}
            onClick={submit}
          >
            {t('refunds.form.submit')}
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-4">
        <fieldset className="border-0 p-0 m-0">
          <legend className={cn(lab, 'p-0')}>{t('refunds.form.type')}</legend>
          <div className="grid grid-cols-2 gap-2 mt-2 max-sm:grid-cols-1">
            {(['RETURN_AND_REFUND', 'REFUND_ONLY'] as const).map((id) => (
              <label
                key={id}
                className={cn(
                  'flex gap-[9px] px-3 py-2.5 rounded-md cursor-pointer border-[1.5px] border-solid',
                  type === id ? 'border-primary bg-primary-tint' : 'border-line bg-surface-card',
                )}
              >
                <Radio name="refund-type" checked={type === id} onChange={() => setType(id)} />
                <span>
                  <span className="block font-semibold text-sm text-strong">
                    {t(`refunds.type.${id}`)}
                  </span>
                  <span className="text-xs text-muted">{t(`refunds.type_hint.${id}`)}</span>
                </span>
              </label>
            ))}
          </div>
        </fieldset>

        <fieldset className="border-0 p-0 m-0">
          <legend className={cn(lab, 'p-0')}>{t('refunds.form.items')}</legend>
          <div className="flex flex-col gap-2 mt-2">
            {order.items.map((it) => {
              const on = !!picked[it.id];
              return (
                <div
                  key={it.id}
                  className="flex items-center gap-[11px] flex-wrap border border-solid border-line rounded-md px-3 py-2.5"
                >
                  <Checkbox
                    checked={on}
                    aria-label={it.title}
                    onChange={() =>
                      setPicked((p) => {
                        const n = { ...p };
                        if (on) delete n[it.id];
                        else n[it.id] = it.qty;
                        return n;
                      })
                    }
                  />
                  <img
                    src={it.image}
                    alt=""
                    width={40}
                    height={40}
                    className="w-10 h-10 rounded-sm object-cover border border-solid border-line-subtle"
                  />
                  <span className="flex-1 min-w-[120px] text-sm text-body line-clamp-1">
                    {it.title}
                  </span>
                  {it.qty > 1 && on && (
                    <QuantityStepper
                      size="sm"
                      value={picked[it.id]}
                      min={1}
                      max={it.qty}
                      onChange={(q) => setPicked((p) => ({ ...p, [it.id]: q }))}
                      aria-label={t('cart.line.qty', { title: it.title })}
                    />
                  )}
                  <span className="font-semibold text-sm text-price tabular-nums">
                    {formatVND(it.price, { locale })}
                  </span>
                </div>
              );
            })}
          </div>
        </fieldset>

        <Select
          label={t('refunds.form.reason')}
          value={reason}
          onChange={(v) => setReason(v as ReturnReason)}
          options={RETURN_REASONS.map((r) => ({ value: r, label: t(`refunds.reason.${r}`) }))}
        />

        <div>
          <MediaPicker
            id="refund-evidence"
            label={t('refunds.form.evidence')}
            hint={t('refunds.form.evidence_hint')}
            value={files}
            onChange={setFiles}
            accept="image/jpeg,image/png,video/mp4"
            max={6}
            purposeOf={() => 'REFUND_EVIDENCE'}
          />
          {files.length > 0 && missing.length > 0 && (
            <p className="m-0 mt-1.5 text-xs text-amber-600">
              {t('refunds.form.missing', {
                what: missing.map((m) => t(`refunds.form.missing_${m}`)).join(', '),
              })}
            </p>
          )}
        </div>

        {bankNeeded && (
          <fieldset className="border-0 p-0 m-0">
            <legend className={cn(lab, 'p-0')}>{t('refunds.form.bank')}</legend>
            <p className="m-0 mt-1 mb-2 text-xs text-muted">
              {t('refunds.form.bank_hint', { method: t(`orders.pay.${order.paymentMethod}`) })}
            </p>
            <div className="grid grid-cols-3 gap-2 max-sm:grid-cols-1">
              <Select
                aria-label={t('refunds.form.bank_name')}
                value={form.watch('code')}
                onChange={(v) => form.setValue('code', v)}
                options={(banks.data ?? []).map((b) => ({ value: b.code, label: b.name }))}
              />
              <Input
                placeholder={t('refunds.form.account')}
                aria-label={t('refunds.form.account')}
                inputMode="numeric"
                error={err(form.formState.errors.accountNumber?.message)}
                {...form.register('accountNumber', {
                  onChange: (e) =>
                    form.setValue('accountNumber', e.target.value.replace(/\D/g, '')),
                })}
              />
              <Input
                placeholder={t('refunds.form.holder')}
                aria-label={t('refunds.form.holder')}
                error={err(form.formState.errors.holder?.message)}
                {...form.register('holder')}
              />
            </div>
          </fieldset>
        )}

        <div className="flex justify-between items-center px-3.5 py-3 bg-brand-50 rounded-md">
          <span className="text-sm text-body">{t('refunds.form.amount')}</span>
          <span className="font-extrabold text-xl text-price tabular-nums">
            {formatVND(amount, { locale })}
          </span>
        </div>
        <p className="m-0 flex gap-[9px] text-xs text-muted leading-normal">
          <Icon name="shield-check" size={16} className="text-blue-600 flex-none mt-px" />
          {t('refunds.form.note')}
        </p>
      </div>
    </Modal>
  );
}
