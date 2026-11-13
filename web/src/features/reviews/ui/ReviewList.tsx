// Đánh giá sản phẩm (prototype ProductDetail.jsx: khối đánh giá, Review, R_FILTERS, báo cáo) —
// plan G08.33, island client:visible. Trang đầu SSR sẵn (bot đọc được); lọc theo sao / có ảnh,
// đánh giá ẩn tên, phản hồi của người bán, "Hữu ích", báo cáo đánh giá (cần đăng nhập).
import { useStore } from '@nanostores/react';
import { useState } from 'react';
import { requireLogin } from '@/features/auth';
import { $session } from '@/shared/auth/session';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatCompact, formatDate, formatNumber } from '@/shared/lib/format';
import { Providers } from '@/shared/providers/Providers';
import {
  Badge,
  Button,
  Icon,
  Modal,
  Pagination,
  Rating,
  Select,
  Textarea,
  toast,
} from '@/shared/ui';
import { useReportReview, useReviews, useToggleHelpful } from '../application/useReviews';
import { displayName } from '../domain/review.rules';
import {
  REPORT_REASONS,
  REVIEW_FILTERS,
  type ReportReason,
  type Review,
  type ReviewFilter,
  type ReviewPage,
} from '../domain/review.types';

export interface ReviewListProps {
  productId: number;
  /** Điểm và số lượt đánh giá của sản phẩm (đầu khối). */
  rating: number;
  reviewCount: number;
  initial: ReviewPage;
  initialPage?: number;
}

function ReviewItem({
  r,
  productId,
  reported,
  onReport,
}: {
  r: Review;
  productId: number;
  reported: boolean;
  onReport: () => void;
}) {
  const { t, locale } = useT();
  const session = useStore($session);
  const helpful = useToggleHelpful(productId);
  const name = displayName(r);
  const authed = session.status === 'authenticated';

  return (
    <li className="tr-review flex gap-3 border-0 border-b border-solid border-line-subtle py-[18px] last:border-b-0">
      <span
        aria-hidden="true"
        className="grid h-10 w-10 flex-none place-items-center rounded-full bg-gray-200 font-bold text-muted"
      >
        {name[0]}
      </span>
      <div className="min-w-0 flex-1">
        <div className="text-sm font-semibold text-strong">
          {name}
          {r.anonymous && (
            <span className="ml-1.5 text-xs font-normal text-subtle">
              {t('reviews.item.anonymous')}
            </span>
          )}
        </div>
        <div className="mb-1.5 mt-[3px]">
          <Rating value={r.rating} size={13} showNumber={false} />
        </div>
        <div className="mb-2 flex flex-wrap items-center gap-2">
          {r.verifiedPurchase && (
            <Badge variant="success" icon="badge-check">
              {t('reviews.item.verified')}
            </Badge>
          )}
          <span className="text-xs text-subtle">
            {formatDate(r.createdAt, { locale })} ·{' '}
            {t('reviews.item.variant', { variant: r.variantLabel })}
          </span>
        </div>
        <p className={cn('m-0 text-base text-body', r.images.length > 0 && 'mb-2.5')}>{r.text}</p>
        {r.images.length > 0 && (
          <ul className="m-0 mb-1 flex list-none gap-2 p-0">
            {r.images.map((src, i) => (
              <li key={src}>
                <img
                  src={src}
                  alt={t('reviews.item.photo', { n: i + 1 })}
                  width={64}
                  height={64}
                  loading="lazy"
                  className="h-16 w-16 rounded-sm object-cover"
                />
              </li>
            ))}
          </ul>
        )}
        {r.reply && (
          <div className="mt-2.5 rounded-md bg-gray-50 px-3 py-2.5">
            <div className="mb-1 flex items-center gap-1.5">
              <Icon name="store" size={13} className="text-primary" />
              <span className="text-xs font-semibold text-primary">{t('reviews.item.reply')}</span>
            </div>
            <p className="m-0 text-sm text-muted">{r.reply.text}</p>
          </div>
        )}
      </div>
      <div className="flex flex-none flex-col items-end gap-2 self-start">
        <button
          type="button"
          aria-pressed={r.helpfulByMe}
          disabled={helpful.isPending}
          onClick={() => (authed ? helpful.mutate(r.id) : requireLogin())}
          className={cn(
            'inline-flex min-h-[32px] cursor-pointer items-center gap-[5px] border-0 bg-transparent text-xs',
            r.helpfulByMe ? 'text-primary' : 'text-muted hover:text-body',
          )}
        >
          <Icon name="thumbs-up" size={14} />
          {t('reviews.item.helpful', { n: formatNumber(r.helpfulCount, { locale }) })}
        </button>
        <button
          type="button"
          disabled={reported}
          onClick={() => (authed ? onReport() : requireLogin())}
          className="inline-flex min-h-[32px] cursor-pointer items-center gap-[5px] border-0 bg-transparent text-xs text-subtle hover:text-body disabled:cursor-default disabled:hover:text-subtle"
        >
          <Icon name="triangle-alert" size={13} />
          {reported ? t('reviews.report.done') : t('reviews.report.action')}
        </button>
      </div>
    </li>
  );
}

function ReportDialog({
  reviewId,
  onClose,
  onDone,
}: {
  reviewId: string | null;
  onClose: () => void;
  onDone: (id: string) => void;
}) {
  const { t } = useT();
  const [reason, setReason] = useState<ReportReason>(REPORT_REASONS[0]);
  const [note, setNote] = useState('');
  const report = useReportReview();
  const submit = () => {
    if (!reviewId) return;
    report.mutate(
      { reviewId, reason, note },
      {
        onSuccess: () => {
          onDone(reviewId);
          setNote('');
          toast.success(t('reviews.report.sent'));
        },
        onError: (e) => toast.error(errorMessage(e, t)),
      },
    );
  };
  return (
    <Modal
      open={reviewId !== null}
      onClose={onClose}
      width={440}
      title={t('reviews.report.title')}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            {t('common.action.cancel')}
          </Button>
          <Button variant="danger" loading={report.isPending} onClick={submit}>
            {t('reviews.report.submit')}
          </Button>
        </>
      }
    >
      <Select<ReportReason>
        label={t('reviews.report.reason')}
        value={reason}
        onChange={setReason}
        options={REPORT_REASONS.map((r) => ({ value: r, label: t(`reviews.report.reasons.${r}`) }))}
      />
      <Textarea
        aria-label={t('reviews.report.note')}
        placeholder={t('reviews.report.note_ph')}
        rows={2}
        value={note}
        maxLength={500}
        onChange={(e) => setNote(e.target.value)}
        className="mt-2.5"
      />
    </Modal>
  );
}

function List({ productId, rating, reviewCount, initial, initialPage = 1 }: ReviewListProps) {
  const { t, locale } = useT();
  const [filter, setFilter] = useState<ReviewFilter>('all');
  const [page, setPage] = useState(initialPage);
  const [reporting, setReporting] = useState<string | null>(null);
  const [reported, setReported] = useState<Set<string>>(() => new Set());
  const q = useReviews(productId, filter, page, {
    filter: 'all',
    page: initialPage,
    data: initial,
  });
  const data = q.data ?? initial;
  const counts = data.summary.counts;

  const pick = (f: ReviewFilter) => {
    setFilter(f);
    setPage(1);
  };

  return (
    <section
      id="reviews"
      aria-labelledby="reviews-title"
      className="mb-4 scroll-mt-[140px] rounded-lg bg-surface-card px-6 py-5 shadow-sm max-md:px-4"
    >
      <div className="mb-2 flex flex-wrap items-center gap-6 gap-y-1">
        <h2 id="reviews-title" className="m-0 text-xl font-bold text-strong">
          {t('reviews.title')}
        </h2>
        <div className="flex items-baseline gap-1.5">
          <span className="text-[32px] font-extrabold text-gold-600">
            {formatNumber(rating, { locale })}
          </span>
          <span className="text-muted">
            {t('reviews.summary', { n: formatCompact(reviewCount, { locale }) })}
          </span>
        </div>
      </div>
      <div
        className="mb-1.5 flex flex-wrap gap-2"
        role="group"
        aria-label={t('reviews.filter.label')}
      >
        {REVIEW_FILTERS.map((f) => {
          const on = f === filter;
          return (
            <button
              key={f}
              type="button"
              aria-pressed={on}
              onClick={() => pick(f)}
              className={cn(
                'h-8 cursor-pointer rounded-sm border border-solid px-3.5 text-sm max-md:h-11',
                on
                  ? 'border-primary bg-primary-tint text-brand-700'
                  : 'border-line bg-surface-card text-body hover:border-primary',
              )}
            >
              {t(`reviews.filter.${f}`)} ({formatNumber(counts[f], { locale })})
            </button>
          );
        })}
      </div>

      <div aria-busy={q.isFetching} className={cn(q.isPlaceholderData && 'opacity-60')}>
        {data.items.length > 0 ? (
          <ul className="m-0 list-none p-0">
            {data.items.map((r) => (
              <ReviewItem
                key={r.id}
                r={r}
                productId={productId}
                reported={reported.has(r.id)}
                onReport={() => setReporting(r.id)}
              />
            ))}
          </ul>
        ) : (
          <p className="m-0 py-8 text-center text-base text-muted">{t('reviews.empty')}</p>
        )}
      </div>
      <Pagination
        page={data.page}
        total={data.total_pages}
        onChange={setPage}
        hrefFor={(n) => `?review_page=${n}#reviews`}
        className="mt-4"
      />

      <ReportDialog
        reviewId={reporting}
        onClose={() => setReporting(null)}
        onDone={(id) => {
          setReported((s) => new Set(s).add(id));
          setReporting(null);
        }}
      />
    </section>
  );
}

export default function ReviewList(props: ReviewListProps) {
  return (
    <Providers>
      <List {...props} />
    </Providers>
  );
}
