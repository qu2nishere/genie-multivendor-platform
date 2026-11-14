import type {
  AnalyticsOverview,
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

export const toRevenue = (d: RevenueDto, range: RevenueRange): RevenueSeries => ({
  total: d.total,
  previous: d.previous_total,
  granularity: range === '12m' ? 'MONTH' : 'DAY',
  points: d.points,
});

export const toOverview = (d: AnalyticsOverviewDto): AnalyticsOverview => ({
  period: { from: d.period.from, to: d.period.to },
  revenue: d.revenue,
  orders: d.orders,
  visits: d.visits,
  chart: d.chart,
  net: {
    net: d.net_revenue.net,
    commission: d.net_revenue.commission,
    commissionRate: d.net_revenue.commission_rate,
    shopVoucherCost: d.net_revenue.shop_voucher_cost,
  },
  outcomes: d.outcomes,
  health: {
    lostDisputes: d.health_30d.lost_disputes,
    autoConfirmedOrders: d.health_30d.auto_confirmed_orders,
  },
});

export const toSource = (d: TrafficSourceDto): TrafficSource => ({
  source: d.source,
  visits: d.visits,
  share: d.share_percent,
  conversion: d.conversion_rate,
});

export const toTopProduct = (d: TopProductDto): TopProduct => ({
  id: d.product_id,
  name: d.name,
  imageUrl: d.image_url,
  sold: d.sold,
  revenue: d.revenue,
  share: d.revenue_share_percent,
  conversion: d.conversion_rate,
});

export const toSentiment = (d: SentimentDto): Sentiment => ({
  analyzed: d.analyzed_count,
  positive: d.positive,
  neutral: d.neutral,
  negative: d.negative,
  previousPositive: d.previous_positive,
  worst: d.most_negative_product && {
    productId: d.most_negative_product.product_id,
    name: d.most_negative_product.name,
    negative: d.most_negative_product.negative,
  },
});
