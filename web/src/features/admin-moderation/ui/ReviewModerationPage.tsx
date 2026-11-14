// Kiểm duyệt đánh giá (prototype admin/Disputes.jsx Reviews) — plan G12.16: đánh giá bị báo cáo, tiêu
// cực mạnh (PhoBERT ≥ 0,85) lên đầu rồi theo số báo cáo; Giữ hiển thị / Ẩn đánh giá / Hiện lại.
// Thêm tab Chờ xử lý / Đã ẩn / Đã xử lý theo `state` của API 11.5 (prototype gộp một danh sách).
import { useSearchParams } from 'react-router-dom';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatPercent } from '@/shared/lib/format';
import {
  Avatar,
  Badge,
  Button,
  EmptyState,
  Icon,
  Rating,
  Skeleton,
  StatusBadge,
  Tabs,
  toast,
} from '@/shared/ui';
import { useReviewReports, useReviewVisibility } from '../application/useModeration';
import { REPORT_STATES, reportActions } from '../domain/review.rules';
import type { ReportState, ReviewReport } from '../domain/review.types';

function ReportCard({ r }: { r: ReviewReport }) {
  const { t, locale } = useT();
  const act = useReviewVisibility();
  const hidden = r.state === 'HIDDEN';
  const acts = reportActions(r.state);
  const run = (visible: boolean, done: string) =>
    act.mutate(
      { reviewId: r.reviewId, visible, reason: visible ? undefined : r.topReason },
      {
        onSuccess: () => toast.success(done),
        onError: (e) => toast.error(errorMessage(e, t)),
      },
    );
  return (
    <article
      className={cn('bg-surface-card rounded-lg shadow-sm p-[18px] mb-3.5', hidden && 'opacity-70')}
      aria-labelledby={`rv-${r.reviewId}`}
    >
      <div className="flex items-center gap-2.5 mb-2 flex-wrap">
        <Avatar name={r.reviewerMasked} size={32} />
        <div>
          <h2 id={`rv-${r.reviewId}`} className="m-0 text-[13px] font-semibold text-strong">
            {r.reviewerMasked}
          </h2>
          <Rating value={r.rating} size={12} showNumber={false} />
        </div>
        <span className="ml-auto flex items-center gap-2 flex-wrap">
          {r.sentiment?.label === 'NEGATIVE' && (
            <Badge
              variant="warning"
              icon="cpu"
              title={t('admin-moderation.reviews.ai_title', {
                score: formatPercent(r.sentiment.score, { locale }),
              })}
            >
              {t('admin-moderation.reviews.ai_negative')}
            </Badge>
          )}
          <Badge variant="danger" icon="triangle-alert">
            {t('admin-moderation.reviews.reports', { count: r.reportCount })}
          </Badge>
          {hidden ? (
            <Badge variant="neutral" icon="eye-off">
              {t('admin-moderation.reviews.state.HIDDEN')}
            </Badge>
          ) : r.state === 'RESOLVED' ? (
            <StatusBadge status="COMPLETED" label={t('admin-moderation.reviews.state.RESOLVED')} />
          ) : (
            <StatusBadge status="PENDING" label={t('admin-moderation.reviews.state.PENDING')} />
          )}
        </span>
      </div>
      <div className="text-xs text-subtle mb-1">
        {t('admin-moderation.reviews.product', { name: r.productName })} ·{' '}
        <span className="font-mono">{r.code}</span>
      </div>
      <p
        className={cn(
          'm-0 text-base px-3 py-2 bg-gray-50 rounded-md border border-solid border-red-200',
          hidden ? 'text-muted' : 'text-body',
        )}
      >
        {r.content}
      </p>
      <div className="flex items-center gap-2.5 mt-3 flex-wrap">
        <span className="text-xs text-muted">
          {t('admin-moderation.reviews.flag', {
            reason: t(`admin-moderation.reviews.reasons.${r.topReason}`),
          })}
          {r.reportedByVendor && ` · ${t('admin-moderation.reviews.by_vendor')}`}
        </span>
        <span className="ml-auto flex gap-2">
          {acts.includes('unhide') && (
            <Button
              variant="ghost"
              size="sm"
              iconLeft="eye"
              loading={act.isPending}
              onClick={() => run(true, t('admin-moderation.reviews.done.unhide'))}
            >
              {t('admin-moderation.reviews.unhide')}
            </Button>
          )}
          {acts.includes('keep') && (
            <Button
              variant="secondary"
              size="sm"
              loading={act.isPending}
              onClick={() => run(true, t('admin-moderation.reviews.done.keep'))}
            >
              {t('admin-moderation.reviews.keep')}
            </Button>
          )}
          {acts.includes('hide') && (
            <Button
              variant="danger"
              size="sm"
              iconLeft="eye-off"
              loading={act.isPending}
              onClick={() => run(false, t('admin-moderation.reviews.done.hide'))}
            >
              {t('admin-moderation.reviews.hide')}
            </Button>
          )}
        </span>
      </div>
    </article>
  );
}

export default function ReviewModerationPage() {
  const { t } = useT();
  const [params, setParams] = useSearchParams();
  const raw = params.get('tab') as ReportState | null;
  const tab: ReportState = raw && REPORT_STATES.includes(raw) ? raw : 'PENDING';
  const q = useReviewReports(tab);
  const counts = q.data?.counts;
  return (
    <div className="max-w-[1180px]">
      <div className="flex gap-2 bg-amber-50 text-amber-700 rounded-md px-3.5 py-2.5 text-sm mb-4">
        <Icon name="info" size={16} className="flex-none mt-0.5" />
        {t('admin-moderation.reviews.notice')}
      </div>
      <Tabs
        size="sm"
        className="mb-4"
        value={tab}
        onChange={(v) => setParams({ tab: v }, { replace: true })}
        aria-label={t('admin-moderation.reviews.tabs_label')}
        items={REPORT_STATES.map((s) => ({
          id: s,
          label: t(`admin-moderation.reviews.tabs.${s}`),
          badge: counts && counts[s] > 0 ? counts[s] : undefined,
        }))}
      />
      {q.isPending ? (
        <Skeleton lines={8} />
      ) : q.isError ? (
        <EmptyState
          icon="triangle-alert"
          title={t('common.state.error')}
          text={errorMessage(q.error, t)}
        />
      ) : q.data.items.length === 0 ? (
        <EmptyState icon="star" title={t('admin-moderation.reviews.empty')} />
      ) : (
        q.data.items.map((r) => <ReportCard key={r.reviewId} r={r} />)
      )}
    </div>
  );
}
