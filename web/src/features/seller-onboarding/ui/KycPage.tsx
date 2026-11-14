// Hồ sơ KYC (prototype Onboarding.jsx) — plan G11.37: băng trạng thái (Chưa nộp / Chờ duyệt / Đã duyệt / Từ
// chối + "Yêu cầu nhân viên xem xét" / Cần bổ sung + hạn), Thông tin người bán (rhf + zod: CCCD 12 số, MST
// với Hộ KD / DN), giấy tờ CCCD 2 mặt + GPKD + selfie (tải lên hoặc chụp camera) — tệp KYC KHÔNG nén
// (TS 5.4), đồng ý sinh trắc, Lưu nháp / Gửi duyệt KYC / Gửi lại hồ sơ. Đang chờ / đã duyệt → chỉ xem.
// Bỏ bảng "Mô phỏng trạng thái KYC" của prototype (D5).
import { useEffect, useMemo, useState } from 'react';
import { useForm, useWatch } from 'react-hook-form';
import { errorMessage } from '@/shared/api/errors';
import { uploadFile } from '@/shared/api/upload';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatDateTime } from '@/shared/lib/format';
import {
  Badge,
  Button,
  Checkbox,
  Icon,
  Input,
  Select,
  Skeleton,
  Spinner,
  StatusBadge,
  toast,
  type IconName,
} from '@/shared/ui';
import {
  useKyc,
  useRequestManualReview,
  useSaveKycDraft,
  useSubmitKyc,
} from '../application/useKyc';
import {
  bannerOf,
  canEdit,
  isResubmit,
  kycFileIssue,
  missingDocs,
  requiredDocs,
  sizeLabel,
  type KycBanner,
} from '../domain/kyc.rules';
import { kycInfoSchema } from '../domain/kyc.schema';
import type { KycDocKey, KycDossier, KycForm } from '../domain/kyc.types';
import { BUSINESS_TYPES } from '../domain/registration.types';
import { CameraModal } from './CameraModal';

const BANNER: Record<
  KycBanner,
  { tone: 'amber' | 'green' | 'red'; icon: IconName; status: string }
> = {
  NOT_SUBMITTED: { tone: 'amber', icon: 'file-text', status: 'PENDING' },
  PENDING: { tone: 'amber', icon: 'clock', status: 'PENDING' },
  VERIFIED: { tone: 'green', icon: 'badge-check', status: 'VERIFIED' },
  REJECTED: { tone: 'red', icon: 'triangle-alert', status: 'REJECTED' },
  SUPPLEMENT_REQUESTED: { tone: 'amber', icon: 'file-text', status: 'PENDING' },
};
const TONE_BG = { amber: 'bg-amber-50', green: 'bg-green-50', red: 'bg-red-50' };
const TONE_FG = { amber: 'text-amber-600', green: 'text-green-600', red: 'text-red-600' };
const card = 'bg-surface-card rounded-lg shadow-sm p-[22px] mb-4 max-sm:p-4';

function Form({ dossier }: { dossier: KycDossier }) {
  const { t, locale } = useT();
  const save = useSaveKycDraft();
  const submit = useSubmitKyc();
  const review = useRequestManualReview();
  const initial = useMemo<KycForm>(
    () => ({
      shopName: dossier.shopName,
      businessType: dossier.businessType,
      identityNumber: dossier.identityNumber,
      taxCode: dossier.taxCode,
      representative: dossier.representative,
      documents: dossier.documents,
      biometricConsent: dossier.biometricConsent,
    }),
    [dossier],
  );
  const form = useForm<KycForm>({ defaultValues: initial });
  useEffect(() => form.reset(initial), [initial, form]);
  const type = useWatch({ control: form.control, name: 'businessType' });
  const docs = useWatch({ control: form.control, name: 'documents' });
  const consent = useWatch({ control: form.control, name: 'biometricConsent' });
  const [busy, setBusy] = useState<KycDocKey | null>(null);
  const [note, setNote] = useState<string | null>(null);
  const [cam, setCam] = useState(false);
  const [sent, setSent] = useState<string | null>(null);
  const { errors } = form.formState;
  const editable = canEdit(dossier.status);
  const b = bannerOf(dossier.status);
  const meta = BANNER[b];
  const individual = type === 'INDIVIDUAL';

  const attach = async (key: KycDocKey, f: File | undefined) => {
    if (!f) return;
    const issue = kycFileIssue(f, key);
    if (issue) return setNote(t(`seller-onboarding.kyc.err.file_${issue}`));
    setNote(null);
    setBusy(key);
    try {
      const u = await uploadFile(f, 'KYC_DOCUMENT');
      form.setValue(
        'documents',
        {
          ...form.getValues('documents'),
          [key]: {
            fileName: u.name,
            sizeBytes: f.size,
            objectKey: u.objectKey,
            previewUrl: u.previewUrl,
          },
        },
        { shouldDirty: true },
      );
    } catch (e) {
      setNote(errorMessage(e, t));
    } finally {
      setBusy(null);
    }
  };

  const validInfo = (): boolean => {
    const v = form.getValues();
    const r = kycInfoSchema.safeParse(v);
    form.clearErrors();
    if (!r.success) {
      r.error.issues.forEach((i) =>
        form.setError(i.path[0] as 'shopName' | 'identityNumber' | 'taxCode' | 'representative', {
          message: i.message,
        }),
      );
      return false;
    }
    return true;
  };

  const doSubmit = () => {
    if (!validInfo()) return setNote(t('seller-onboarding.kyc.err.info'));
    const miss = missingDocs(form.getValues());
    if (miss.length)
      return setNote(
        t('seller-onboarding.kyc.err.missing', {
          list: miss.map((k) => t(`seller-onboarding.kyc.docs.${k}`)).join(', '),
        }),
      );
    setNote(null);
    submit.mutate(form.getValues(), {
      onSuccess: () => {
        setSent(t(consent ? 'seller-onboarding.kyc.sent' : 'seller-onboarding.kyc.sent_manual'));
        toast.success(t('seller-onboarding.kyc.sent_toast'));
      },
      onError: (e) => setNote(errorMessage(e, t)),
    });
  };

  const docRows: KycDocKey[] = requiredDocs(type).filter((k) => k !== 'SELFIE');
  const selfie = docs.SELFIE;
  const err = (m?: string) => (m ? t(m) : undefined);

  return (
    <div className="max-w-[1180px]">
      <div
        className={cn(
          'flex gap-3 items-start rounded-lg px-[18px] py-4 mb-[18px]',
          TONE_BG[meta.tone],
        )}
        role="status"
      >
        <Icon name={meta.icon} size={22} className={cn('mt-px flex-none', TONE_FG[meta.tone])} />
        <div className="flex-1">
          <div className="flex items-center gap-2.5 flex-wrap">
            <h2 className="m-0 font-bold text-base text-strong">
              {t(`seller-onboarding.kyc.banner.${b}.title`)}
            </h2>
            <StatusBadge status={meta.status} label={t(`seller-onboarding.kyc.banner.${b}.chip`)} />
          </div>
          <p className="m-0 mt-1 text-sm text-body">
            {b === 'REJECTED'
              ? t('seller-onboarding.kyc.banner.REJECTED.desc', {
                  reason: dossier.rejectionReason ?? '',
                })
              : b === 'SUPPLEMENT_REQUESTED' && dossier.supplement
                ? dossier.supplement.note
                : t(`seller-onboarding.kyc.banner.${b}.desc`)}
          </p>
          {b === 'SUPPLEMENT_REQUESTED' && dossier.supplement && (
            <p className="m-0 mt-1 text-xs text-amber-600">
              {t('seller-onboarding.kyc.banner.SUPPLEMENT_REQUESTED.deadline', {
                date: formatDateTime(dossier.supplement.deadlineAt, { locale }),
              })}
            </p>
          )}
          {b === 'REJECTED' && (
            <Button
              size="sm"
              variant="secondary"
              className="mt-2.5"
              loading={review.isPending}
              disabled={dossier.manualReviewRequested}
              onClick={() =>
                review.mutate(undefined, {
                  onSuccess: () => toast.success(t('seller-onboarding.kyc.review_sent')),
                  onError: (e) => toast.error(errorMessage(e, t)),
                })
              }
            >
              {t(
                dossier.manualReviewRequested
                  ? 'seller-onboarding.kyc.review_done'
                  : 'seller-onboarding.kyc.review',
              )}
            </Button>
          )}
        </div>
      </div>

      <form onSubmit={(e) => e.preventDefault()} noValidate>
        <section className={card} aria-labelledby="kyc-info">
          <h2 id="kyc-info" className="m-0 mb-4 font-bold text-base text-strong">
            {t('seller-onboarding.kyc.info')}
          </h2>
          <div className="flex flex-col gap-3.5">
            <div className="grid grid-cols-2 gap-3.5 max-md:grid-cols-1">
              <Input
                label={t('seller-onboarding.landing.shop_name')}
                required
                disabled={!editable}
                error={err(errors.shopName?.message)}
                {...form.register('shopName')}
              />
              <Select
                label={t('seller-onboarding.landing.type')}
                disabled={!editable}
                value={type}
                onChange={(v) => form.setValue('businessType', v, { shouldDirty: true })}
                options={BUSINESS_TYPES.map((x) => ({
                  value: x,
                  label: t(`seller-onboarding.landing.types.${x}`),
                }))}
              />
            </div>
            <div
              className={cn(
                'grid gap-3.5 max-md:grid-cols-1',
                individual ? 'grid-cols-2' : 'grid-cols-3',
              )}
            >
              <Input
                label={t('seller-onboarding.kyc.cccd')}
                iconLeft="badge-check"
                inputMode="numeric"
                maxLength={12}
                required
                disabled={!editable}
                placeholder={t('seller-onboarding.kyc.cccd_placeholder')}
                error={err(errors.identityNumber?.message)}
                {...form.register('identityNumber', {
                  onChange: (e: { target: HTMLInputElement }) => {
                    e.target.value = e.target.value.replace(/\D/g, '').slice(0, 12);
                  },
                })}
              />
              {!individual && (
                <Input
                  label={t('seller-onboarding.kyc.tax')}
                  iconLeft="file-text"
                  required
                  disabled={!editable}
                  placeholder="VD: 0312xxxxxx"
                  error={err(errors.taxCode?.message)}
                  {...form.register('taxCode')}
                />
              )}
              <Input
                label={t('seller-onboarding.kyc.rep')}
                required
                disabled={!editable}
                error={err(errors.representative?.message)}
                {...form.register('representative')}
              />
            </div>
            <p className="m-0 flex items-center gap-[7px] text-xs text-subtle">
              <Icon name="info" size={14} />
              {t(
                individual
                  ? 'seller-onboarding.kyc.hint_individual'
                  : 'seller-onboarding.kyc.hint_business',
              )}
            </p>
          </div>
        </section>

        <section className={card} aria-labelledby="kyc-docs">
          <div className="flex items-center gap-2 mb-1.5 flex-wrap">
            <h2 id="kyc-docs" className="m-0 font-bold text-base text-strong">
              {t('seller-onboarding.kyc.docs_title')}
            </h2>
            <Badge variant="neutral">{t('seller-onboarding.kyc.docs_rule')}</Badge>
          </div>
          <ul className="m-0 p-0 list-none flex flex-col gap-3 mt-3">
            {docRows.map((k) => {
              const d = docs[k];
              return (
                <li
                  key={k}
                  className={cn(
                    'flex items-center gap-3.5 rounded-md px-4 py-3 border-[1.5px] border-dashed flex-wrap',
                    d ? 'border-mint-500 bg-mint-50' : 'border-line-strong bg-gray-25',
                  )}
                >
                  <Icon
                    name={d ? 'circle-check' : 'upload'}
                    size={22}
                    className={d ? 'text-mint-600' : 'text-subtle'}
                  />
                  <div className="flex-1 min-w-[180px]">
                    <div className="font-semibold text-sm text-strong">
                      {t(`seller-onboarding.kyc.docs_long.${k}`)}
                    </div>
                    {d ? (
                      <div className="code text-xs text-muted mt-0.5">
                        {d.fileName} · {sizeLabel(d.sizeBytes, locale)}
                      </div>
                    ) : (
                      <div className="text-xs text-subtle mt-0.5">
                        {t('seller-onboarding.kyc.pick_hint')}
                      </div>
                    )}
                  </div>
                  {editable && (
                    <label
                      className={cn(
                        'inline-flex items-center gap-1.5 h-8 px-3 rounded-md font-semibold text-[13px] cursor-pointer focus-within:shadow-focus',
                        d ? 'text-body' : 'bg-brand-50 text-brand-700',
                      )}
                    >
                      <input
                        type="file"
                        className="sr-only"
                        accept=".pdf,image/jpeg,image/png"
                        disabled={busy !== null}
                        aria-label={t('seller-onboarding.kyc.upload_for', {
                          doc: t(`seller-onboarding.kyc.docs.${k}`),
                        })}
                        onChange={(e) => {
                          const f = e.target.files?.[0];
                          e.target.value = '';
                          void attach(k, f);
                        }}
                      />
                      {busy === k ? (
                        <Spinner size={14} />
                      ) : (
                        <Icon name={d ? 'refresh-cw' : 'upload'} size={14} />
                      )}
                      {t(d ? 'seller-onboarding.kyc.change' : 'seller-onboarding.kyc.choose')}
                    </label>
                  )}
                </li>
              );
            })}
          </ul>

          <div className="mt-4">
            <div className="flex items-center gap-2 mb-2">
              <span className="font-semibold text-sm text-strong">
                {t('seller-onboarding.kyc.docs.SELFIE')}
              </span>
              {!selfie && <Badge variant="danger">{t('common.state.required')}</Badge>}
            </div>
            <p className="m-0 mb-2.5 flex items-center gap-1.5 text-xs text-subtle">
              <Icon name="info" size={14} />
              {t('seller-onboarding.kyc.selfie_hint')}
            </p>
            <div
              className={cn(
                'flex items-center gap-3.5 rounded-md px-4 py-3 border-[1.5px] border-dashed flex-wrap',
                selfie ? 'border-mint-500 bg-mint-50' : 'border-line-strong bg-gray-25',
              )}
            >
              {selfie?.previewUrl ? (
                <img
                  src={selfie.previewUrl}
                  alt={t('seller-onboarding.kyc.docs.SELFIE')}
                  width={44}
                  height={44}
                  className="w-11 h-11 rounded-sm object-cover"
                />
              ) : (
                <Icon
                  name={selfie ? 'circle-check' : 'camera'}
                  size={22}
                  className={selfie ? 'text-mint-600' : 'text-subtle'}
                />
              )}
              <div
                className={cn(
                  'flex-1 min-w-[160px] text-sm',
                  selfie ? 'text-mint-600' : 'text-muted',
                )}
              >
                {selfie
                  ? t('seller-onboarding.kyc.selfie_done')
                  : t('seller-onboarding.kyc.selfie_empty')}
              </div>
              {editable && (
                <>
                  <label className="inline-flex items-center gap-1.5 h-8 px-3 font-semibold text-[13px] text-body cursor-pointer rounded-md focus-within:shadow-focus">
                    <input
                      type="file"
                      className="sr-only"
                      accept="image/jpeg,image/png"
                      disabled={busy !== null}
                      aria-label={t('seller-onboarding.kyc.upload_for', {
                        doc: t('seller-onboarding.kyc.docs.SELFIE'),
                      })}
                      onChange={(e) => {
                        const f = e.target.files?.[0];
                        e.target.value = '';
                        void attach('SELFIE', f);
                      }}
                    />
                    {busy === 'SELFIE' ? <Spinner size={14} /> : <Icon name="upload" size={14} />}
                    {t('seller-onboarding.kyc.choose')}
                  </label>
                  <Button
                    size="sm"
                    variant="secondary"
                    iconLeft="camera"
                    disabled={busy !== null}
                    onClick={() => setCam(true)}
                  >
                    {t('seller-onboarding.kyc.camera.open')}
                  </Button>
                </>
              )}
            </div>
          </div>
          {note && (
            <p className="m-0 mt-2.5 flex items-center gap-1.5 text-xs text-red-600" role="alert">
              <Icon name="circle-alert" size={13} />
              {note}
            </p>
          )}
        </section>

        <div className="bg-surface-card rounded-lg shadow-sm px-[18px] py-3.5 mb-4">
          <Checkbox
            checked={consent}
            disabled={!editable}
            onChange={(e) =>
              form.setValue('biometricConsent', e.target.checked, { shouldDirty: true })
            }
            className="items-start text-sm"
          >
            <span className="text-body leading-normal">{t('seller-onboarding.kyc.consent')}</span>
          </Checkbox>
        </div>

        {sent && (
          <p className="m-0 mb-3 flex items-center gap-[7px] text-mint-600 text-sm" role="status">
            <Icon name="circle-check" size={16} />
            {sent}
          </p>
        )}
        {editable && (
          <div className="flex justify-end gap-3 flex-wrap">
            <Button
              variant="ghost"
              loading={save.isPending}
              onClick={() =>
                save.mutate(form.getValues(), {
                  onSuccess: () => toast.success(t('seller-onboarding.kyc.draft_saved')),
                  onError: (e) => toast.error(errorMessage(e, t)),
                })
              }
            >
              {t('seller-onboarding.kyc.save_draft')}
            </Button>
            <Button
              iconLeft="send"
              loading={submit.isPending}
              disabled={busy !== null}
              onClick={doSubmit}
            >
              {t(
                isResubmit(dossier.status)
                  ? 'seller-onboarding.kyc.resubmit'
                  : 'seller-onboarding.kyc.submit',
              )}
            </Button>
          </div>
        )}
      </form>
      {cam && (
        <CameraModal
          onClose={() => setCam(false)}
          onShot={(f) => {
            setCam(false);
            void attach('SELFIE', f);
          }}
        />
      )}
    </div>
  );
}

export default function KycPage() {
  const q = useKyc();
  if (!q.data) return <Skeleton lines={10} />;
  return <Form dossier={q.data} />;
}
