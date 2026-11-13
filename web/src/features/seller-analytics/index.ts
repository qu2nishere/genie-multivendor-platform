// API công khai của feature "seller-analytics" — nơi DUY NHẤT feature khác được import.
export type {
  RevenueRange,
  AnalyticsRange,
  RevenueSeries,
  AnalyticsOverview,
  Outcome,
  Outcomes,
} from './domain/analytics.types';
export {
  OUTCOME_ORDER,
  conversion,
  shopCancelRate,
  positiveRate,
  pointLabel,
  isoToVnDate,
} from './domain/analytics.rules';
export {
  useRevenue,
  useAnalyticsOverview,
  useSentiment,
  useTrafficSources,
  useTopProducts,
} from './application/useAnalytics';
export { default as StatsPage } from './ui/StatsPage';
