// Phân tích bán hàng của shop (API_SPEC 10.2 revenue, 10.9; prototype vendor/data.jsx REVENUE, PERF,
// OUTCOMES, HEALTH, COMMISSION_RATE; VendorPages.jsx TOP, SOURCES, SENTIMENT).

/** Biểu đồ Doanh thu ở Tổng quan: 7 ngày / 30 ngày / 12 tháng đã chốt. */
export type RevenueRange = '7d' | '30d' | '12m';
/** Bộ chọn ở Phân tích. */
export type AnalyticsRange = 'today' | '7d' | '30d' | 'custom';
export type Granularity = 'HOUR' | 'DAY' | 'MONTH';

/** Nhãn mốc: "2026-06-08" (DAY), "2026-06" (MONTH), "06:00" (HOUR). */
export interface SeriesPoint {
  label: string;
  value: number;
}

export interface RevenueSeries {
  total: number;
  /** null với 12 tháng (không có kỳ so sánh). */
  previous: number | null;
  granularity: Granularity;
  points: SeriesPoint[];
}

/** Kết quả đơn trong kỳ (UC-VND-08: hoàn tất, khách hủy, shop từ chối, trả hàng, đang xử lý). */
export type Outcome =
  'COMPLETED' | 'CANCELLED_BY_CUSTOMER' | 'REJECTED_BY_SHOP' | 'REFUNDED' | 'IN_PROGRESS';
export type Outcomes = Record<Outcome, number>;

export interface AnalyticsOverview {
  period: { from: string; to: string };
  revenue: { gross: number; previous: number };
  orders: { count: number; previous: number };
  visits: { count: number; previous: number };
  chart: { granularity: Granularity; points: SeriesPoint[] };
  net: { net: number; commission: number; commissionRate: number; shopVoucherCost: number };
  outcomes: Outcomes;
  health: { lostDisputes: number; autoConfirmedOrders: number };
}

export type TrafficSourceId = 'SEARCH' | 'HOME' | 'PROMOTION' | 'EXTERNAL';
export interface TrafficSource {
  source: TrafficSourceId;
  visits: number;
  share: number;
  conversion: number;
}

export interface TopProduct {
  id: number;
  name: string;
  imageUrl: string;
  sold: number;
  revenue: number;
  share: number;
  conversion: number;
}

export interface Sentiment {
  analyzed: number;
  positive: number;
  neutral: number;
  negative: number;
  previousPositive: number;
  worst: { productId: number; name: string; negative: number } | null;
}
