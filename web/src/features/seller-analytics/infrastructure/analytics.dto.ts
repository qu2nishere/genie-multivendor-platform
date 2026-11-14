// DTO snake_case — GET /seller/analytics/* (API_SPEC 10.2, 10.9).
import type { Granularity, Outcome, TrafficSourceId } from '../domain/analytics.types';

export interface RevenueDto {
  total: number;
  previous_total: number | null;
  points: { label: string; value: number }[];
}

export interface AnalyticsOverviewDto {
  period: { from: string; to: string; comparison_label: string };
  revenue: { gross: number; previous: number };
  orders: { count: number; previous: number };
  visits: { count: number; previous: number };
  chart: { granularity: Granularity; points: { label: string; value: number }[] };
  net_revenue: {
    net: number;
    commission: number;
    commission_rate: number;
    shop_voucher_cost: number;
  };
  outcomes: Record<Outcome, number>;
  health_30d: { lost_disputes: number; auto_confirmed_orders: number };
}

export interface TrafficSourceDto {
  source: TrafficSourceId;
  visits: number;
  share_percent: number;
  conversion_rate: number;
}

export interface TopProductDto {
  product_id: number;
  name: string;
  image_url: string;
  sold: number;
  revenue: number;
  revenue_share_percent: number;
  conversion_rate: number;
}

export interface SentimentDto {
  analyzed_count: number;
  positive: number;
  neutral: number;
  negative: number;
  previous_positive: number;
  most_negative_product: { product_id: number; name: string; negative: number } | null;
}
