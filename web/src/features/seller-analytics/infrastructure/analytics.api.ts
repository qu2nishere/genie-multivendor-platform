// Phân tích bán hàng (API_SPEC 10.2 revenue, 10.9). Mock: khoảng "Tùy chọn ngày" trả số 30 ngày.
import { api, request } from '@/shared/api/http';
import { mockDelay, USE_MOCK } from '@/shared/api/mock';
import type {
  AnalyticsOverview,
  AnalyticsRange,
  RevenueRange,
  RevenueSeries,
  Sentiment,
  TopProduct,
  TrafficSource,
} from '../domain/analytics.types';
import type {
  AnalyticsOverviewDto,
  RevenueDto,
  SentimentDto,
  TopProductDto,
  TrafficSourceDto,
} from './analytics.dto';
import { toOverview, toRevenue, toSentiment, toSource, toTopProduct } from './analytics.mapper';
import { OVERVIEW, REVENUE, SENTIMENT, SOURCES, TOP, TOP_TOTAL_REVENUE } from './analytics.mock';

export interface RangeQuery {
  range: AnalyticsRange;
  from?: string;
  to?: string;
}

const rangeQuery = (r: RangeQuery) => ({
  range: r.range,
  from: r.range === 'custom' ? r.from : undefined,
  to: r.range === 'custom' ? r.to : undefined,
});
const mockKey = (r: AnalyticsRange) => (r === 'custom' ? '30d' : r);

export async function getRevenue(range: RevenueRange): Promise<RevenueSeries> {
  if (USE_MOCK) {
    await mockDelay(200);
    return toRevenue(REVENUE[range], range);
  }
  return toRevenue(
    await api.get<RevenueDto>('/seller/analytics/revenue', { query: { range } }),
    range,
  );
}

export async function getAnalyticsOverview(r: RangeQuery): Promise<AnalyticsOverview> {
  if (USE_MOCK) {
    await mockDelay(220);
    return toOverview(OVERVIEW[mockKey(r.range)]);
  }
  return toOverview(
    await api.get<AnalyticsOverviewDto>('/seller/analytics/overview', { query: rangeQuery(r) }),
  );
}

export async function getTrafficSources(): Promise<TrafficSource[]> {
  if (USE_MOCK) {
    await mockDelay(160);
    return SOURCES.map(toSource);
  }
  const d = await api.get<TrafficSourceDto[]>('/seller/analytics/traffic-sources', {
    query: { range: '30d' },
  });
  return d.map(toSource);
}

export async function getTopProducts(
  limit = 5,
): Promise<{ items: TopProduct[]; totalRevenue: number }> {
  if (USE_MOCK) {
    await mockDelay(180);
    return { items: TOP.slice(0, limit).map(toTopProduct), totalRevenue: TOP_TOTAL_REVENUE };
  }
  const res = await request<TopProductDto[]>('/seller/analytics/top-products', {
    query: { range: '30d', limit },
  });
  return {
    items: res.data.map(toTopProduct),
    totalRevenue: Number(res.summary?.total_revenue ?? 0),
  };
}

export async function getSentiment(r: RangeQuery): Promise<Sentiment> {
  if (USE_MOCK) {
    await mockDelay(160);
    return toSentiment(SENTIMENT);
  }
  return toSentiment(
    await api.get<SentimentDto>('/seller/analytics/sentiment', { query: rangeQuery(r) }),
  );
}
