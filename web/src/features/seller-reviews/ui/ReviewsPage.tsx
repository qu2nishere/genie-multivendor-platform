// Đánh giá phía shop (prototype VendorPages.jsx Reviews, ReviewCard) — plan G11.42: điểm trung bình + phân bố
// sao, tab Tất cả / Chưa phản hồi / Cần chú ý (≤ 3★), thẻ đánh giá (ảnh, phân loại), Phản hồi (≤ 500 ký tự,
// không ghi SĐT), Báo cáo vi phạm (lý do + ghi chú; quyết định ẩn do kiểm duyệt viên).
import { useState } from 'react';
import { REPORT_REASONS, reportReview, type ReportReason } from '@/features/reviews';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { formatDate, formatNumber } from '@/shared/lib/format';
import {
  Avatar,
  Button,
  EmptyState,
  Icon,
  Modal,
  Rating,
  Select,
  Skeleton,
  Tabs,
  Textarea,
  toast,
} from '@/shared/ui';
import { useReplyReview, useReportReview, useShopReviews } from '../application/useShopReviews';
import { distPercent, REPLY_MAX, replyIssue } from '../domain/review.rules';
import type { ReviewTab, ShopReview } from '../domain/review.types';

function ReviewCard({ r }: { r: ShopReview }) {
  const { t, locale } = useT();
  const reply = useReplyReview();
  const report = useReportReview<ReportReason>(reportReview);
  const [open, setOpen] = useState(false);
  const [text, setText] = useState('');
  const [reportOpen, setReportOpen] = useState(false);
  const [why, setWhy] = useState<ReportReason>(REPORT_REASONS[0]);
  const [note, setNote] = useState('');
  const issue = replyIssue(text);
  return (
    <article className="bg-surface-card border border-solid border-line-subtle rounded-md px-4 py-3.5">
      <div className="flex gap-3">
        <Avatar name={r.reviewer.name} src={r.reviewer.avatarUrl} size={40} />
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="font-semibold text-sm text-strong">{r.reviewer.name}</span>
            <time className="text-xs text-subtle ml-auto" dateTime={r.createdAt}>
              {formatDate(r.createdAt, { locale })}
            </time>
            <button
              type="button"
              disabled={r.reportedByMe}
              onClick={() => setReportOpen(true)}
              className="inline-flex items-center gap-1 border-0 bg-transparent font-semibold text-xs cursor-pointer text-red-600 disabled:text-subtle disabled:cursor-default"
            >
              <Icon name="triangle-alert" size={13} />
              {t(r.reportedByMe ? 'seller-reviews.reported' : 'seller-reviews.report')}
            </button>
          </div>
          <div className="my-[3px]">
            <Rating value={r.rating} size={14} showNumber={false} />
          </div>
          <div className="text-xs text-muted mb-1.5">
            {r.productName}
            {r.variantName && ` · ${t('seller-reviews.variant', { name: r.variantName })}`}
          </div>
          <p className="m-0 text-sm text-body leading-normal whitespace-pre-wrap">{r.content}</p>
          {r.images.length > 0 && (
            <div className="flex gap-2 mt-2">
              {r.images.map((src) => (
                <a key={src} href={src} target="_blank" rel="noopener">
                  <img
                    src={src}
                    alt={t('seller-reviews.photo')}
                    width={56}
                    height={56}
                    className="w-14 h-14 rounded-sm object-cover border border-solid border-line-subtle"
                  />
                </a>
              ))}
            </div>
          )}
          {r.reply ? (
            <div className="flex gap-2 mt-2.5 px-3 py-2.5 bg-brand-50 rounded-md">
              <Icon name="message-square" size={15} className="text-brand-600 flex-none mt-0.5" />
              <p className="m-0 text-sm text-body">
                <span className="font-semibold text-xs text-brand-700">
                  {t('seller-reviews.reply_from')}{' '}
                </span>
                {r.reply.content}
              </p>
            </div>
          ) : open ? (
            <div className="mt-2.5">
              <Textarea
                aria-label={t('seller-reviews.reply_label')}
                value={text}
                maxLength={REPLY_MAX}
                showCount
                rows={2}
                onChange={(e) => setText(e.target.value)}
                placeholder={t('seller-reviews.reply_placeholder', { max: REPLY_MAX })}
                error={issue === 'phone' ? t('errors.GNE-SOC-4221') : undefined}
              />
              <div className="flex gap-2 mt-2">
                <Button
                  size="sm"
                  disabled={!!issue}
                  loading={reply.isPending}
                  onClick={() =>
                    reply.mutate(
                      { id: r.id, content: text },
                      {
                        onSuccess: () => toast.success(t('seller-reviews.replied')),
                        onError: (e) => toast.error(errorMessage(e, t)),
                      },
                    )
                  }
                >
                  {t('seller-reviews.send')}
                </Button>
                <Button size="sm" variant="ghost" onClick={() => setOpen(false)}>
                  {t('common.action.cancel')}
                </Button>
              </div>
            </div>
          ) : (
            <Button
              size="sm"
              variant="outline"
              iconLeft="message-square"
              className="mt-2.5"
              onClick={() => setOpen(true)}
            >
              {t('seller-reviews.reply')}
            </Button>
          )}
        </div>
      </div>
      <Modal
        open={reportOpen}
        onClose={() => setReportOpen(false)}
        width={440}
        title={t('seller-reviews.report_title')}
        footer={
          <>
            <Button variant="ghost" onClick={() => setReportOpen(false)}>
              {t('common.action.cancel')}
            </Button>
            <Button
              variant="danger"
              loading={report.isPending}
              onClick={() =>
                report.mutate(
                  { id: r.id, reason: why, note },
                  {
                    onSuccess: () => {
                      toast.success(t('seller-reviews.report_sent'));
                      setReportOpen(false);
                    },
                    onError: (e) => toast.error(errorMessage(e, t)),
                  },
                )
              }
            >
              {t('seller-reviews.report_send')}
            </Button>
          </>
        }
      >
        <div className="flex flex-col gap-2.5">
          <Select
            label={t('seller-reviews.report_reason')}
            value={why}
            onChange={setWhy}
            options={REPORT_REASONS.map((x) => ({
              value: x,
              label: t(`reviews.report.reasons.${x}`),
            }))}
          />
          <Textarea
            aria-label={t('seller-reviews.report_note')}
            placeholder={t('seller-reviews.report_note')}
            rows={2}
            maxLength={500}
            value={note}
            onChange={(e) => setNote(e.target.value)}
          />
          <p className="m-0 text-xs text-muted leading-normal">{t('seller-reviews.report_hint')}</p>
        </div>
      </Modal>
    </article>
  );
}

export default function ReviewsPage() {
  const { t, locale } = useT();
  const [tab, setTab] = useState<ReviewTab>('ALL');
  const q = useShopReviews(tab);
  const s = q.data?.stats;
  return (
    <div className="max-w-[1180px]">
      <h2 className="m-0 flex items-center gap-[9px] text-2xl font-bold text-strong">
        <Icon name="star" size={22} className="text-brand-500" />
        {t('seller-reviews.title')}
      </h2>
      <p className="m-0 mt-[5px] ml-[31px] text-sm text-muted">{t('seller-reviews.sub')}</p>
      <div className="grid grid-cols-[280px_minmax(0,1fr)] gap-[18px] mt-4 items-start max-md:grid-cols-1">
        <section
          className="bg-surface-card rounded-lg shadow-sm p-5 sticky top-[88px] max-md:static"
          aria-label={t('seller-reviews.summary')}
        >
          {!s ? (
            <Skeleton lines={6} />
          ) : (
            <>
              <div className="text-center pb-3.5 border-0 border-b border-solid border-line-subtle mb-3.5">
                <div className="font-extrabold text-[44px] text-gold-600 leading-none">
                  {formatNumber(s.avg, { locale })}
                </div>
                <div className="mt-1.5 flex justify-center">
                  <Rating value={s.avg} showNumber={false} />
                </div>
                <div className="text-xs text-muted mt-1.5">
                  {t('seller-reviews.count', { count: formatNumber(s.count, { locale }) })}
                </div>
              </div>
              <ul className="m-0 p-0 list-none">
                {([5, 4, 3, 2, 1] as const).map((star) => (
                  <li key={star} className="flex items-center gap-2 mb-[7px]">
                    <span className="inline-flex items-center gap-0.5 text-xs text-muted w-6">
                      {star}
                      <Icon name="star" size={11} className="text-gold-500" />
                    </span>
                    <div
                      className="flex-1 h-[7px] rounded bg-gray-200 overflow-hidden"
                      aria-hidden="true"
                    >
                      <div
                        className="h-full bg-gold-500"
                        style={{ width: `${distPercent(s, star)}%` }}
                      />
                    </div>
                    <span className="text-xs text-subtle w-9 text-right">
                      {formatNumber(s.distribution[star], { locale })}
                    </span>
                  </li>
                ))}
              </ul>
            </>
          )}
        </section>
        <section>
          <div className="bg-surface-card rounded-t-lg shadow-sm px-4 pt-1">
            <Tabs<ReviewTab>
              aria-label={t('seller-reviews.tabs')}
              value={tab}
              onChange={setTab}
              items={[
                { id: 'ALL', label: t('seller-reviews.tab.ALL', { count: s?.counts.ALL ?? 0 }) },
                {
                  id: 'UNREPLIED',
                  label: t('seller-reviews.tab.UNREPLIED', { count: s?.counts.UNREPLIED ?? 0 }),
                },
                { id: 'LOW_RATING', label: t('seller-reviews.tab.LOW_RATING') },
              ]}
            />
          </div>
          <div className="flex flex-col gap-3 bg-surface-sunken p-4 rounded-b-lg">
            {!q.data ? (
              <Skeleton lines={6} />
            ) : q.data.items.length === 0 ? (
              <EmptyState icon="star" text={t('seller-reviews.empty')} compact />
            ) : (
              q.data.items.map((r) => <ReviewCard key={r.id} r={r} />)
            )}
          </div>
        </section>
      </div>
    </div>
  );
}
