// Kiểm duyệt đánh giá (API_SPEC 11.5): GET /admin/review-reports, PATCH /admin/reviews/{id}.
import { api, getPage } from '@/shared/api/http';
import { mockDelay, mockFail, USE_MOCK } from '@/shared/api/mock';
import { REPORT_STATES, sortReports } from '../domain/review.rules';
import type { ReportCounts, ReportState, ReviewReport } from '../domain/review.types';
import type { ReviewReportDto } from './review.dto';
import { reviewReportsStore } from './review.mock';

export const toReviewReport = (d: ReviewReportDto): ReviewReport => ({
  reviewId: d.review_id,
  code: d.code,
  productName: d.product_name,
  reviewerMasked: d.reviewer_masked,
  rating: d.rating,
  content: d.content,
  reportCount: d.report_count,
  topReason: d.top_reason,
  reportedByVendor: d.reported_by_vendor,
  state: d.state,
  sentiment: d.sentiment,
});

export interface ReportList {
  items: ReviewReport[];
  counts: ReportCounts;
}

export async function listReviewReports(state: ReportState): Promise<ReportList> {
  if (USE_MOCK) {
    await mockDelay(180);
    const all = reviewReportsStore.get().map(toReviewReport);
    const counts = Object.fromEntries(
      REPORT_STATES.map((s) => [s, all.filter((r) => r.state === s).length]),
    ) as ReportCounts;
    return { items: sortReports(all.filter((r) => r.state === state)), counts };
  }
  const res = await getPage<ReviewReportDto, ReviewReport>(
    '/admin/review-reports',
    toReviewReport,
    {
      query: { state, size: 50 },
    },
  );
  const c = (res.summary.counts ?? {}) as Partial<ReportCounts>;
  return {
    items: res.items,
    counts: { PENDING: c.PENDING ?? 0, HIDDEN: c.HIDDEN ?? 0, RESOLVED: c.RESOLVED ?? 0 },
  };
}

/** Ẩn (kèm lý do) hoặc hiện — "Giữ hiển thị" đóng các báo cáo (RESOLVED). */
export async function setReviewVisibility(
  reviewId: number,
  visible: boolean,
  reason?: string,
): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(220);
    const rows = reviewReportsStore.get();
    if (!rows.some((r) => r.review_id === reviewId)) mockFail('GNE-SYS-4040', { status: 404 });
    reviewReportsStore.set(
      rows.map((r) =>
        r.review_id === reviewId ? { ...r, state: visible ? 'RESOLVED' : 'HIDDEN' } : r,
      ),
    );
    return;
  }
  await api.patch(
    `/admin/reviews/${reviewId}`,
    visible ? { is_visible: true } : { is_visible: false, reason },
  );
}
