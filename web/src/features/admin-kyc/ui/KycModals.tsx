// Hộp Từ chối / Yêu cầu bổ sung hồ sơ KYC (prototype KYCReview.jsx) — plan G12.12. react-hook-form + zod.
import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { zodResolver } from '@/shared/lib/zodResolver';
import { Button, Checkbox, Icon, Modal, Select, Textarea, toast } from '@/shared/ui';
import { useKycDecision } from '../application/useKyc';
import { REJECT_REASONS, SUPPLEMENT_DAYS, SUPPLEMENT_DOCS } from '../domain/kyc.rules';
import {
  rejectSchema,
  supplementSchema,
  type RejectValues,
  type SupplementValues,
} from '../domain/kyc.schema';
import type { KycSubmission } from '../domain/kyc.types';

const labelCls = 'block text-sm font-semibold text-strong mb-1.5';

export function RejectKycModal({
  item,
  onClose,
}: {
  item: KycSubmission | null;
  onClose: () => void;
}) {
  const { t } = useT();
  const decide = useKycDecision();
  const form = useForm<RejectValues>({
    resolver: zodResolver(rejectSchema),
    defaultValues: { reason: 'BLURRY_DOCUMENT', note: '' },
  });
  useEffect(() => {
    if (item) form.reset({ reason: 'BLURRY_DOCUMENT', note: '' });
  }, [item, form]);
  const reason = form.watch('reason');
  const err = form.formState.errors.note?.message;
  const submit = form.handleSubmit((v) => {
    if (!item) return;
    decide.mutate(
      {
        kind: 'reject',
        id: item.id,
        reason: v.reason,
        note: v.note || null,
        label: t(`admin-kyc.reasons.${v.reason}`),
      },
      {
        onSuccess: () => {
          toast.success(t('admin-kyc.reject.done', { shop: item.shop.name }));
          onClose();
        },
        onError: (e) => toast.error(errorMessage(e, t)),
      },
    );
  });
  return (
    <Modal
      open={!!item}
      onClose={onClose}
      title={item ? t('admin-kyc.reject.title', { code: item.code }) : ''}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            {t('common.action.cancel')}
          </Button>
          <Button variant="danger" loading={decide.isPending} onClick={() => void submit()}>
            {t('admin-kyc.reject.submit')}
          </Button>
        </>
      }
    >
      <form className="flex flex-col gap-3.5" onSubmit={(e) => void submit(e)} noValidate>
        <p className="m-0 text-base text-muted">
          {t('admin-kyc.reject.desc_before')}
          <b className="text-body">{item?.shop.name}</b>
          {t('admin-kyc.reject.desc_after')}
        </p>
        <Select
          label={t('admin-kyc.reject.reason')}
          value={reason}
          onChange={(v) => form.setValue('reason', v, { shouldValidate: true })}
          options={REJECT_REASONS.map((r) => ({ value: r, label: t(`admin-kyc.reasons.${r}`) }))}
        />
        <Textarea
          label={t('admin-kyc.reject.note')}
          placeholder={t('admin-kyc.reject.note_placeholder')}
          rows={3}
          maxLength={500}
          required={reason === 'OTHER'}
          error={err ? t(err) : undefined}
          {...form.register('note')}
        />
      </form>
    </Modal>
  );
}

export function SupplementModal({
  item,
  onClose,
}: {
  item: KycSubmission | null;
  onClose: () => void;
}) {
  const { t } = useT();
  const decide = useKycDecision();
  const form = useForm<SupplementValues>({
    resolver: zodResolver(supplementSchema),
    defaultValues: { documents: [], note: '' },
  });
  useEffect(() => {
    if (item) form.reset({ documents: [], note: '' });
  }, [item, form]);
  const docs = form.watch('documents');
  const docsErr = form.formState.errors.documents?.message;
  const toggle = (d: SupplementValues['documents'][number]) =>
    form.setValue('documents', docs.includes(d) ? docs.filter((x) => x !== d) : [...docs, d], {
      shouldValidate: form.formState.isSubmitted,
    });
  const submit = form.handleSubmit((v) => {
    if (!item) return;
    decide.mutate(
      { kind: 'supplement', id: item.id, documents: v.documents, note: v.note },
      {
        onSuccess: () => {
          toast.success(t('admin-kyc.supplement.done', { shop: item.shop.name }));
          onClose();
        },
        onError: (e) => toast.error(errorMessage(e, t)),
      },
    );
  });
  return (
    <Modal
      open={!!item}
      onClose={onClose}
      width={560}
      title={
        <span className="inline-flex items-center gap-2">
          <Icon name="file-text" size={18} className="text-amber-600" />
          {t('admin-kyc.supplement.title', { shop: item?.shop.name ?? '' })}
        </span>
      }
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            {t('common.action.cancel')}
          </Button>
          <Button iconLeft="send" loading={decide.isPending} onClick={() => void submit()}>
            {t('admin-kyc.supplement.submit')}
          </Button>
        </>
      }
    >
      <form onSubmit={(e) => void submit(e)} noValidate>
        <fieldset className="border-0 p-0 m-0 mb-3.5">
          <legend className={labelCls}>{t('admin-kyc.supplement.docs')}</legend>
          <div className="flex flex-col gap-1">
            {SUPPLEMENT_DOCS.map((d) => (
              <Checkbox
                key={d}
                checked={docs.includes(d)}
                onChange={() => toggle(d)}
                className={cn(
                  'px-2.5 py-2 rounded-md text-sm text-body',
                  docs.includes(d) && 'bg-amber-50',
                )}
              >
                {t(`admin-kyc.supp_docs.${d}`)}
              </Checkbox>
            ))}
          </div>
          {docsErr && (
            <p role="alert" className="m-0 mt-1.5 flex items-center gap-1 text-xs text-red-600">
              <Icon name="circle-alert" size={13} />
              {t(docsErr)}
            </p>
          )}
        </fieldset>
        <Textarea
          label={t('admin-kyc.supplement.note')}
          placeholder={t('admin-kyc.supplement.note_placeholder')}
          rows={3}
          maxLength={500}
          {...form.register('note')}
        />
        <div className="flex gap-2.5 mt-3.5 px-3 py-2.5 bg-blue-50 border border-solid border-blue-200 rounded-md">
          <Icon name="info" size={16} className="text-blue-600 flex-none mt-px" />
          <span className="text-sm text-body leading-normal">
            {t('admin-kyc.supplement.hint', { days: SUPPLEMENT_DAYS })}
          </span>
        </div>
      </form>
    </Modal>
  );
}
