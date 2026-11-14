// Số liệu mẫu (prototype vendor/data.jsx DAILY, MONTHS, PERF, HEALTH; VendorPages.jsx TOP, SOURCES,
// SENTIMENT). Mọi con số suy ra từ cùng một chuỗi doanh thu theo ngày nên các màn không lệch nhau.
import type {
  AnalyticsOverviewDto,
  RevenueDto,
  SentimentDto,
  TopProductDto,
  TrafficSourceDto,
} from './analytics.dto';

const M = 1_000_000;
/** Ngày chốt số liệu của bản mẫu. */
export const AS_OF = '2026-06-08';

const isoDay = (back: number): string => {
  const d = new Date(Date.UTC(2026, 5, 8 - back));
  return d.toISOString().slice(0, 10);
};

/** Doanh thu mỗi ngày (trước hoa hồng) của 30 ngày kết thúc AS_OF. */
export const DAILY = [
  1.0, 1.3, 1.0, 1.4, 1.2, 1.5, 1.6, 1.2, 1.4, 1.1, 1.3, 1.6, 1.7, 1.5, 1.3, 1.4, 1.5, 1.2, 1.6,
  1.8, 1.7, 1.6, 1.6, 1.7, 1.4, 1.6, 1.9, 2.2, 2.0, 2.5,
].map((v) => Math.round(v * M));
const PREV_30_DAYS = 38_680_000;
const sum = (a: number[]) => a.reduce((n, v) => n + v, 0);
const dayPoints = (n: number) =>
  DAILY.slice(-n).map((value, i) => ({ label: isoDay(n - 1 - i), value }));

/** Các tháng đã chốt 06/2025 → 05/2026. */
const MONTHS = [18, 21, 19, 24, 23, 27, 29, 34, 26, 31, 35, 41].map((v, i) => {
  const m = ((i + 5) % 12) + 1;
  const y = i < 7 ? 2025 : 2026;
  return { label: `${y}-${String(m).padStart(2, '0')}`, value: v * M };
});

const HOURS_TODAY = (
  [
    ['06:00', 0.1],
    ['09:00', 0.3],
    ['12:00', 0.6],
    ['15:00', 0.4],
    ['18:00', 0.5],
    ['21:00', 0.6],
  ] as const
).map(([label, v]) => ({ label, value: Math.round(v * M) }));

export const REVENUE: Record<'7d' | '30d' | '12m', RevenueDto> = {
  '7d': {
    total: sum(DAILY.slice(-7)),
    previous_total: sum(DAILY.slice(-14, -7)),
    points: dayPoints(7),
  },
  '30d': { total: sum(DAILY), previous_total: PREV_30_DAYS, points: dayPoints(30) },
  '12m': { total: sum(MONTHS.map((m) => m.value)), previous_total: null, points: MONTHS },
};

const COMMISSION_RATE = 5;
const overview = (
  from: string,
  granularity: 'HOUR' | 'DAY',
  points: { label: string; value: number }[],
  gross: number,
  prevGross: number,
  orders: [number, number],
  visits: [number, number],
  shopVoucher: number,
  out: [number, number, number, number, number],
): AnalyticsOverviewDto => {
  const commission = Math.round((gross * COMMISSION_RATE) / 100);
  return {
    period: { from, to: AS_OF, comparison_label: 'PREVIOUS_PERIOD' },
    revenue: { gross, previous: prevGross },
    orders: { count: orders[0], previous: orders[1] },
    visits: { count: visits[0], previous: visits[1] },
    chart: { granularity, points },
    net_revenue: {
      net: gross - commission - shopVoucher,
      commission,
      commission_rate: COMMISSION_RATE,
      shop_voucher_cost: shopVoucher,
    },
    outcomes: {
      COMPLETED: out[0],
      CANCELLED_BY_CUSTOMER: out[1],
      REJECTED_BY_SHOP: out[2],
      REFUNDED: out[3],
      IN_PROGRESS: out[4],
    },
    health_30d: { lost_disputes: 1, auto_confirmed_orders: 2 },
  };
};

export const OVERVIEW: Record<'today' | '7d' | '30d', AnalyticsOverviewDto> = {
  today: overview(
    AS_OF,
    'HOUR',
    HOURS_TODAY,
    DAILY[29],
    DAILY[28],
    [5, 4],
    [140, 120],
    52_000,
    [0, 0, 0, 0, 5],
  ),
  '7d': overview(
    isoDay(6),
    'DAY',
    dayPoints(7),
    sum(DAILY.slice(-7)),
    sum(DAILY.slice(-14, -7)),
    [29, 24],
    [860, 760],
    296_000,
    [17, 1, 1, 0, 10],
  ),
  '30d': overview(
    isoDay(29),
    'DAY',
    dayPoints(30),
    sum(DAILY),
    PREV_30_DAYS,
    [100, 92],
    [3120, 2786],
    1_284_000,
    [81, 4, 2, 2, 11],
  ),
};

const VISITS_30D = 3120;
export const SOURCES: TrafficSourceDto[] = (
  [
    ['SEARCH', 46, 3.9],
    ['HOME', 28, 2.6],
    ['PROMOTION', 16, 4.4],
    ['EXTERNAL', 10, 1.2],
  ] as const
).map(([source, share, conv]) => ({
  source,
  visits: Math.round((VISITS_30D * share) / 100),
  share_percent: share,
  conversion_rate: conv,
}));

const REV_30D = sum(DAILY);
export const TOP: TopProductDto[] = (
  [
    [1, 'Tai nghe ANC Pro 5', 'headphones', 38, 14_782_000, 8.2],
    [4, 'Watch S2 AMOLED', 'watch', 14, 9_660_000, 5.4],
    [6, 'Bàn phím cơ RGB', 'keyboard', 18, 8_262_000, 6.1],
    [5, 'Loa bluetooth mini', 'speaker', 12, 3_840_000, 4.8],
    [8, 'Webcam Full HD', 'camera', 5, 2_700_000, 2.1],
  ] as const
).map(([id, name, pic, sold, revenue, conv]) => ({
  product_id: id,
  name,
  image_url: `/img/products/${pic}.jpg`,
  sold,
  revenue,
  revenue_share_percent: Math.round((revenue / REV_30D) * 1000) / 10,
  conversion_rate: conv,
}));
export const TOP_TOTAL_REVENUE = REV_30D;

export const SENTIMENT: SentimentDto = {
  analyzed_count: 64,
  positive: 47,
  neutral: 9,
  negative: 8,
  previous_positive: 41,
  most_negative_product: { product_id: 6, name: 'Bàn phím cơ RGB', negative: 4 },
};
