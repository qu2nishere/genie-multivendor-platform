// Duyệt KYC (prototype admin/KYCReview.jsx) — plan G12.10–G12.12: hàng chờ theo tab (?tab=) có số đếm,
// chi tiết hồ sơ (?id=): mức rủi ro, hậu kiểm ngẫu nhiên, giấy tờ đã tải (mở DocViewer), kết quả xác
// thực tự động, so sánh khuôn mặt, dữ liệu kê khai + hoa hồng riêng, lỗi phát hiện; Phê duyệt / Từ chối /
// Yêu cầu bổ sung / Đưa lại vào hàng chờ.
import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatDateTime, formatPercent } from '@/shared/lib/format';
import {
  Avatar,
  Badge,
  Button,
  EmptyState,
  Icon,
  Skeleton,
  StatusBadge,
  Tabs,
  toast,
  type BadgeVariant,
} from '@/shared/ui';
import { useKycDecision, useKycDetail, useKycQueue } from '../application/useKyc';
import {
  AI_CHECKS,
  KYC_TABS,
  kycActions,
  parseShopRate,
  statusKey,
  statusTone,
} from '../domain/kyc.rules';
import type { KycDetail, KycDocument, KycSubmission, KycTab, RiskLevel } from '../domain/kyc.types';
import { DocViewer } from './DocViewer';
import { RejectKycModal, SupplementModal } from './KycModals';

const RISK: Record<RiskLevel, BadgeVariant> = { LOW: 'success', MEDIUM: 'warning', HIGH: 'danger' };
const AI_TONE = { PASS: 'success', PARTIAL: 'warning', FAIL: 'danger' } as const;
const sheet = 'bg-surface-card rounded-lg shadow-sm';
const h3 = 'm-0 text-sm font-semibold text-strong';

function QueueItem({ k, on, onSelect }: { k: KycSubmission; on: boolean; onSelect: () => void }) {
  const { t } = useT();
  return (
    <li>
      <button
        type="button"
        onClick={onSelect}
        aria-current={on ? 'true' : undefined}
        className={cn(
          'flex gap-3 w-full text-left border-0 border-b border-l-[3px] border-solid border-b-line-subtle cursor-pointer px-4 py-3.5 focus-visible:outline-none focus-visible:shadow-focus',
          on
            ? 'bg-blue-50 border-l-blue-500'
            : 'bg-surface-card border-l-transparent hover:bg-gray-50',
        )}
      >
        <Avatar name={k.shop.name} size={40} square />
        <span className="flex-1 min-w-0">
          <span className="block text-sm font-semibold text-strong truncate">{k.shop.name}</span>
          <span className="block text-xs text-muted mt-0.5">
            {k.ownerName} · {t(`admin-kyc.business.${k.businessType}`)}
          </span>
          <span className="flex items-center gap-1.5 mt-[7px]">
            <StatusBadge
              status={statusTone(k.status)}
              label={t(`admin-kyc.status.${statusKey(k.status)}`)}
            />
            <span className="font-mono text-[11px] text-subtle">{k.code}</span>
          </span>
        </span>
      </button>
    </li>
  );
}

function Detail({
  d,
  onView,
  onReject,
  onSupplement,
}: {
  d: KycDetail;
  onView: (doc: KycDocument, label: string) => void;
  onReject: () => void;
  onSupplement: () => void;
}) {
  const { t, locale } = useT();
  const decide = useKycDecision();
  const [rate, setRate] = useState(d.shopCommissionRate?.toString() ?? '');
  useEffect(() => setRate(d.shopCommissionRate?.toString() ?? ''), [d.id, d.shopCommissionRate]);
  const parsed = parseShopRate(rate);
  const actions = kycActions(d.status);
  const docLabel = (doc: KycDocument) => t(`admin-kyc.docs.${doc.type}`);
  const front = d.documents.find((x) => x.type === 'ID_FRONT');
  const selfie = d.documents.find((x) => x.type === 'SELFIE');
  const run = (kind: 'approve' | 'requeue') => {
    if (kind === 'approve' && !parsed.ok) return;
    decide.mutate(
      kind === 'approve'
        ? { kind, id: d.id, shopCommissionRate: parsed.ok ? parsed.value : null }
        : { kind, id: d.id },
      {
        onSuccess: () => toast.success(t(`admin-kyc.${kind}.done`, { shop: d.shop.name })),
        onError: (e) => toast.error(errorMessage(e, t)),
      },
    );
  };

  return (
    <article className={cn(sheet, 'p-6 max-md:p-4')} aria-labelledby="kyc-detail-title">
      <header className="flex items-start gap-4 max-sm:gap-3 pb-[18px] border-0 border-b border-solid border-line-subtle">
        <Avatar name={d.shop.name} size={56} square />
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2.5 flex-wrap">
            <h2 id="kyc-detail-title" className="m-0 text-xl font-bold text-strong">
              {d.shop.name}
            </h2>
            <Badge variant={RISK[d.riskLevel]}>
              {t(`admin-kyc.risk.${d.riskLevel}`, { score: d.riskScore })}
            </Badge>
            <span className="sm:hidden font-mono text-xs text-muted bg-gray-100 px-2 py-1 rounded-sm">
              {d.code}
            </span>
            {d.randomAudit && (
              <Badge variant="outline" icon="eye">
                {t('admin-kyc.random_audit')}
              </Badge>
            )}
          </div>
          <p className="m-0 mt-1 text-sm text-muted">
            {t('admin-kyc.owner')} <b className="text-body">{d.ownerName}</b> ·{' '}
            {t('admin-kyc.type')} {t(`admin-kyc.business.${d.businessType}`)} ·{' '}
            {t('admin-kyc.submitted', { at: formatDateTime(d.submittedAt, { locale }) })}
          </p>
          <div className="mt-2">
            <StatusBadge
              status={statusTone(d.status)}
              label={t(`admin-kyc.status.${statusKey(d.status)}`)}
            />
          </div>
        </div>
        <span className="font-mono text-xs text-muted bg-gray-100 px-2 py-1 rounded-sm max-sm:hidden">
          {d.code}
        </span>
      </header>

      <section className="my-[18px]" aria-labelledby="kyc-docs">
        <h3 id="kyc-docs" className={cn(h3, 'mb-2.5')}>
          {t('admin-kyc.uploaded', { count: d.documents.length })}
        </h3>
        <ul className="flex gap-3 flex-wrap list-none m-0 p-0">
          {d.documents.map((doc, i) => (
            <li key={`${doc.type}-${i}`} className="w-[150px] max-sm:w-[calc(50%-6px)]">
              <button
                type="button"
                onClick={() => onView(doc, docLabel(doc))}
                className="w-full border-0 bg-transparent p-0 text-left cursor-zoom-in rounded-md focus-visible:outline-none focus-visible:shadow-focus"
              >
                <span className="relative block h-24 rounded-md bg-gray-100 border border-solid border-line overflow-hidden">
                  <img
                    src={doc.url}
                    alt=""
                    width={150}
                    height={96}
                    className="w-full h-full object-cover opacity-80"
                  />
                  <span className="absolute bottom-1.5 right-1.5 bg-surface-card rounded-xs px-[5px] py-px font-mono text-[10px] text-muted shadow-xs">
                    {doc.contentType.includes('pdf') ? 'PDF' : 'IMG'}
                  </span>
                </span>
                <span className="flex items-center gap-1 mt-1.5 text-xs text-body">
                  <Icon name="eye" size={13} />
                  {docLabel(doc)}
                </span>
              </button>
            </li>
          ))}
        </ul>
      </section>

      <section
        className="border border-solid border-line-subtle rounded-md px-4 py-3.5 mb-[18px]"
        aria-labelledby="kyc-ai"
      >
        <div className="flex items-center gap-2 mb-3">
          <Icon name="shield-check" size={17} className="text-blue-600" />
          <h3 id="kyc-ai" className={h3}>
            {t('admin-kyc.ai.title')}
          </h3>
          <span className="ml-auto">
            {d.ai ? (
              <Badge variant={AI_TONE[d.ai.overall]}>{t(`admin-kyc.ai.${d.ai.overall}`)}</Badge>
            ) : (
              <Badge variant="neutral">{t('admin-kyc.ai.none')}</Badge>
            )}
          </span>
        </div>
        {d.ai && (
          <ul className="flex flex-wrap gap-2 mb-3.5 list-none m-0 p-0">
            {AI_CHECKS.map((c) => {
              const ok = d.ai?.checks[c];
              return (
                <li
                  key={c}
                  className={cn(
                    'inline-flex items-center gap-[5px] px-2.5 py-1 rounded-pill text-xs font-semibold',
                    ok ? 'bg-mint-50 text-mint-600' : 'bg-red-50 text-red-600',
                  )}
                >
                  <Icon name={ok ? 'check' : 'x'} size={13} />
                  {t(`admin-kyc.ai.checks.${c}`)}
                  <span className="sr-only">
                    {ok ? t('admin-kyc.ai.passed') : t('admin-kyc.ai.failed')}
                  </span>
                </li>
              );
            })}
          </ul>
        )}
        <div className="grid grid-cols-2 gap-[18px] max-md:grid-cols-1">
          <div>
            <h4 className={cn(h3, 'mb-2')}>{t('admin-kyc.face.title')}</h4>
            <div className="flex items-center gap-2.5">
              {[front, selfie].map((doc, i) => (
                <div key={i} className="contents">
                  {i > 0 && <Icon name="arrow-right" size={18} className="text-subtle" />}
                  <button
                    type="button"
                    disabled={!doc}
                    onClick={() => doc && onView(doc, docLabel(doc))}
                    className="border-0 bg-transparent p-0 text-center cursor-zoom-in disabled:cursor-default rounded-md focus-visible:outline-none focus-visible:shadow-focus"
                  >
                    <span className="w-[72px] h-[72px] rounded-md bg-gray-100 text-subtle grid place-items-center overflow-hidden">
                      {doc ? (
                        <img
                          src={doc.url}
                          alt=""
                          width={72}
                          height={72}
                          className="w-full h-full object-cover"
                        />
                      ) : (
                        <Icon name="user" size={30} />
                      )}
                    </span>
                    <span className="block text-xs text-muted mt-1">
                      {t(i === 0 ? 'admin-kyc.docs.ID_FRONT' : 'admin-kyc.docs.SELFIE')}
                    </span>
                  </button>
                </div>
              ))}
            </div>
            {d.ai && (
              <p className="m-0 mt-2 text-xs text-muted">
                {t('admin-kyc.face.score', {
                  score: formatPercent(d.ai.faceMatchScore, { locale }),
                })}
              </p>
            )}
          </div>
          <div>
            <h4 className={cn(h3, 'mb-2')}>{t('admin-kyc.declared.title')}</h4>
            <dl className="m-0 text-sm">
              {[
                [t('admin-kyc.declared.type'), t(`admin-kyc.business.${d.businessType}`)],
                [t('admin-kyc.declared.id'), d.declared.identityNumberMasked],
                [t('admin-kyc.declared.rep'), d.declared.representative],
                ...(d.declared.taxCode ? [[t('admin-kyc.declared.tax'), d.declared.taxCode]] : []),
              ].map(([k, v]) => (
                <div key={k} className="flex justify-between gap-2.5 py-[3px]">
                  <dt className="text-muted">{k}</dt>
                  <dd className="m-0 text-body font-medium">{v}</dd>
                </div>
              ))}
            </dl>
            <label className="flex justify-between items-center gap-2.5 pt-1.5 text-sm">
              <span className="text-muted">{t('admin-kyc.declared.rate')}</span>
              <span className="inline-flex items-center gap-1">
                <input
                  inputMode="decimal"
                  value={rate}
                  onChange={(e) => setRate(e.target.value.replace(/[^\d.,]/g, ''))}
                  disabled={!actions.includes('approve')}
                  placeholder={t('admin-kyc.declared.rate_placeholder')}
                  aria-invalid={!parsed.ok}
                  className={cn(
                    'w-[104px] h-[30px] border-[1.5px] border-solid rounded-sm px-2 text-sm text-right outline-none bg-surface-card text-body focus:border-primary disabled:bg-gray-50',
                    parsed.ok ? 'border-line' : 'border-red-500',
                  )}
                />
                %
              </span>
            </label>
            {!parsed.ok && (
              <p role="alert" className="m-0 mt-1 text-xs text-red-600 text-right">
                {t('admin-kyc.declared.rate_err')}
              </p>
            )}
          </div>
        </div>
        {d.ai && d.ai.errors.length > 0 && (
          <div className="mt-3">
            <h4 className="m-0 text-sm font-semibold text-red-600 mb-1">
              {t('admin-kyc.ai.errors')}
            </h4>
            <ul className="list-none m-0 p-0">
              {d.ai.errors.map((e) => (
                <li key={e} className="flex items-center gap-1.5 text-sm text-body py-0.5">
                  <Icon name="triangle-alert" size={14} className="text-red-600 flex-none" />
                  {e}
                </li>
              ))}
            </ul>
          </div>
        )}
      </section>

      {d.status === 'REJECTED' && d.rejectionReason && (
        <div className="flex gap-2 bg-red-50 text-red-600 rounded-md px-3.5 py-2.5 text-sm mb-4">
          <Icon name="triangle-alert" size={16} className="flex-none mt-0.5" />
          <span>
            <b>{t('admin-kyc.rejected_reason')}</b> {d.rejectionReason}
          </span>
        </div>
      )}
      {d.status === 'SUPPLEMENT_REQUESTED' && d.supplement && (
        <div className="flex gap-2 bg-amber-50 text-amber-700 rounded-md px-3.5 py-2.5 text-sm mb-4">
          <Icon name="file-text" size={16} className="flex-none mt-0.5" />
          <span>
            <b>{t('admin-kyc.supplement_req')}</b>{' '}
            {d.supplement.documents.map((x) => t(`admin-kyc.docs.${x}`)).join(', ')}
            {d.supplement.note && ` — ${d.supplement.note}`} ·{' '}
            {t('admin-kyc.due', { at: formatDateTime(d.supplement.dueAt, { locale }) })}
          </span>
        </div>
      )}

      <div className="flex gap-2 bg-amber-50 text-amber-700 rounded-md px-3.5 py-2.5 text-sm mb-[18px]">
        <Icon name="info" size={16} className="flex-none mt-0.5" />
        <span>
          {t('admin-kyc.notice_before')}
          <b>{t('admin-kyc.notice_strong')}</b>
          {t('admin-kyc.notice_after')}
        </span>
      </div>

      {actions.includes('approve') ? (
        <div className="flex gap-3 flex-wrap">
          <Button variant="danger" iconLeft="ban" onClick={onReject}>
            {t('admin-kyc.actions.reject')}
          </Button>
          <Button
            iconLeft="badge-check"
            loading={decide.isPending}
            disabled={!parsed.ok}
            onClick={() => run('approve')}
          >
            {t('admin-kyc.actions.approve')}
          </Button>
          <Button variant="ghost" iconLeft="message-circle" onClick={onSupplement}>
            {t('admin-kyc.actions.supplement')}
          </Button>
        </div>
      ) : (
        <div className="flex gap-2.5 items-center flex-wrap">
          {d.status === 'SUPPLEMENT_REQUESTED' && (
            <>
              <Badge variant="warning" icon="clock">
                {t('admin-kyc.status.SUPPLEMENT_REQUESTED')}
              </Badge>
              <span className="text-sm text-muted">{t('admin-kyc.waiting_seller')}</span>
            </>
          )}
          <Button
            variant="secondary"
            size={d.status === 'SUPPLEMENT_REQUESTED' ? 'sm' : 'md'}
            iconLeft="undo-2"
            className={d.status === 'SUPPLEMENT_REQUESTED' ? 'ml-auto' : undefined}
            loading={decide.isPending}
            onClick={() => run('requeue')}
          >
            {t('admin-kyc.actions.requeue')}
          </Button>
        </div>
      )}
    </article>
  );
}

export default function KycReviewPage() {
  const { t } = useT();
  const [params, setParams] = useSearchParams();
  const raw = params.get('tab');
  const tab: KycTab = KYC_TABS.includes(raw as KycTab) ? (raw as KycTab) : 'PENDING';
  const queue = useKycQueue(tab);
  const items = queue.data?.items ?? [];
  const idParam = Number(params.get('id')) || null;
  const selectedId = idParam ?? items[0]?.id ?? null;
  const detail = useKycDetail(selectedId);
  const [viewer, setViewer] = useState<{ doc: KycDocument; label: string } | null>(null);
  const [rejecting, setRejecting] = useState<KycSubmission | null>(null);
  const [supplementing, setSupplementing] = useState<KycSubmission | null>(null);

  const setTab = (next: KycTab) => setParams({ tab: next }, { replace: true });
  const select = (id: number) => setParams({ tab, id: String(id) }, { replace: true });
  const counts = queue.data?.counts;

  return (
    <div className="grid grid-cols-[440px_minmax(0,1fr)] gap-5 items-start max-lg:grid-cols-1">
      <section className={cn(sheet, 'overflow-hidden')} aria-label={t('admin-kyc.queue')}>
        <div className="px-3.5 pt-1 overflow-x-auto">
          <Tabs
            size="sm"
            value={tab}
            onChange={setTab}
            aria-label={t('admin-kyc.queue')}
            items={KYC_TABS.map((id) => ({
              id,
              label: t(`admin-kyc.tabs.${id}`),
              badge:
                counts && (id === 'PENDING' || id === 'SUPPLEMENT_REQUESTED') && counts[id] > 0
                  ? counts[id]
                  : undefined,
            }))}
          />
        </div>
        {queue.isPending ? (
          <Skeleton lines={5} className="m-4" />
        ) : items.length === 0 ? (
          <EmptyState icon="user-check" title={t('admin-kyc.empty')} />
        ) : (
          <ul className="list-none m-0 p-0">
            {items.map((k) => (
              <QueueItem key={k.id} k={k} on={k.id === selectedId} onSelect={() => select(k.id)} />
            ))}
          </ul>
        )}
      </section>

      {selectedId == null ? null : detail.data ? (
        <Detail
          d={detail.data}
          onView={(doc, label) => setViewer({ doc, label })}
          onReject={() => detail.data && setRejecting(detail.data)}
          onSupplement={() => detail.data && setSupplementing(detail.data)}
        />
      ) : detail.isError ? (
        <div className={cn(sheet, 'p-6')}>
          <EmptyState
            icon="triangle-alert"
            title={t('common.state.error')}
            text={errorMessage(detail.error, t)}
          />
        </div>
      ) : (
        <div className={cn(sheet, 'p-6')}>
          <Skeleton lines={10} />
        </div>
      )}

      <RejectKycModal item={rejecting} onClose={() => setRejecting(null)} />
      <SupplementModal item={supplementing} onClose={() => setSupplementing(null)} />
      {viewer && (
        <DocViewer doc={viewer.doc} label={viewer.label} onClose={() => setViewer(null)} />
      )}
    </div>
  );
}
