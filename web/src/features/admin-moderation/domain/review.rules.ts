// Hàm thuần kiểm duyệt đánh giá (API_SPEC 11.5).
import type { ReportState, ReviewReport } from './review.types';

export const REPORT_STATES: ReportState[] = ['PENDING', 'HIDDEN', 'RESOLVED'];

/** Ngưỡng PhoBERT để ưu tiên (UC-AI-07) — AI chỉ xếp lên đầu, không tự ẩn. */
export const NEGATIVE_PRIORITY = 0.85;

export const isStronglyNegative = (r: Pick<ReviewReport, 'sentiment'>): boolean =>
  r.sentiment?.label === 'NEGATIVE' && r.sentiment.score >= NEGATIVE_PRIORITY;

/** Tiêu cực mạnh lên đầu, rồi theo số báo cáo giảm dần. */
export function sortReports<T extends Pick<ReviewReport, 'sentiment' | 'reportCount'>>(
  rows: T[],
): T[] {
  return [...rows].sort(
    (a, b) =>
      Number(isStronglyNegative(b)) - Number(isStronglyNegative(a)) ||
      b.reportCount - a.reportCount,
  );
}

/** Thao tác: đang chờ → Giữ hiển thị / Ẩn; đã ẩn → Hiện lại; đã xử lý → Ẩn (nếu báo cáo lại). */
export function reportActions(s: ReportState): ('keep' | 'hide' | 'unhide')[] {
  if (s === 'HIDDEN') return ['unhide'];
  if (s === 'PENDING') return ['keep', 'hide'];
  return ['hide'];
}
