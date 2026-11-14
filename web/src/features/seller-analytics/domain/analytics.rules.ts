// Quy tắc phân tích: tỉ lệ client tự tính (API 10.2, 10.9), thứ tự / màu kết quả đơn, nhãn mốc biểu đồ.
import type { Granularity, Outcome, Outcomes, Sentiment } from './analytics.types';

/** Thứ tự + sắc độ thanh "Đơn kết thúc thế nào" (prototype OUTCOMES). */
export const OUTCOME_ORDER: readonly [Outcome, 'ok' | 'wait' | 'bad' | 'held' | 'info'][] = [
  ['COMPLETED', 'ok'],
  ['CANCELLED_BY_CUSTOMER', 'wait'],
  ['REJECTED_BY_SHOP', 'bad'],
  ['REFUNDED', 'held'],
  ['IN_PROGRESS', 'info'],
];

/** Tỉ lệ chuyển đổi (%) = đơn / lượt truy cập. */
export const conversion = (orders: number, visits: number): number =>
  visits > 0 ? (orders / visits) * 100 : 0;

/** Tỉ lệ shop tự hủy hoặc từ chối (%). */
export const shopCancelRate = (o: Outcomes, orders: number): number =>
  orders > 0 ? (o.REJECTED_BY_SHOP / orders) * 100 : 0;

/** Tỉ lệ tích cực (%) trên số đánh giá đã phân loại. */
export function positiveRate(s: Pick<Sentiment, 'positive' | 'neutral' | 'negative'>): number {
  const total = s.positive + s.neutral + s.negative;
  return total > 0 ? (s.positive / total) * 100 : 0;
}

export const sumPoints = (points: { value: number }[]): number =>
  points.reduce((n, p) => n + p.value, 0);

/** Nhãn trục: DAY "08/06", MONTH "T6" / "Jun", HOUR "6h". */
export function pointLabel(label: string, g: Granularity, locale: 'vi' | 'en'): string {
  if (g === 'HOUR') return `${Number(label.slice(0, 2))}h`;
  if (g === 'MONTH') {
    const m = Number(label.slice(5, 7));
    if (locale === 'vi') return `T${m}`;
    return new Intl.DateTimeFormat('en-US', { month: 'short', timeZone: 'UTC' }).format(
      new Date(Date.UTC(2000, m - 1, 1)),
    );
  }
  return `${label.slice(8, 10)}/${label.slice(5, 7)}`;
}

/** "2026-06-08" → "08/06/2026". */
export const isoToVnDate = (iso: string): string =>
  `${iso.slice(8, 10)}/${iso.slice(5, 7)}/${iso.slice(0, 4)}`;
