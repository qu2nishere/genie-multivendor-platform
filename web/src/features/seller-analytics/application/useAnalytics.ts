// Phân tích bán hàng (TanStack Query).
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import type { RevenueRange } from '../domain/analytics.types';
import {
  getAnalyticsOverview,
  getRevenue,
  getSentiment,
  getTopProducts,
  getTrafficSources,
  type RangeQuery,
} from '../infrastructure/analytics.api';

const KEY = ['seller', 'analytics'] as const;

export const useRevenue = (range: RevenueRange) =>
  useQuery({
    queryKey: [...KEY, 'revenue', range],
    queryFn: () => getRevenue(range),
    placeholderData: keepPreviousData,
  });

/** Khoảng "Tùy chọn ngày" chỉ tải khi đã chọn đủ từ / đến. */
const ready = (r: RangeQuery) => r.range !== 'custom' || (!!r.from && !!r.to && r.from <= r.to);

export const useAnalyticsOverview = (r: RangeQuery) =>
  useQuery({
    queryKey: [...KEY, 'overview', r],
    queryFn: () => getAnalyticsOverview(r),
    enabled: ready(r),
    placeholderData: keepPreviousData,
  });

export const useSentiment = (r: RangeQuery) =>
  useQuery({
    queryKey: [...KEY, 'sentiment', r],
    queryFn: () => getSentiment(r),
    enabled: ready(r),
    placeholderData: keepPreviousData,
  });

export const useTrafficSources = () =>
  useQuery({ queryKey: [...KEY, 'sources'], queryFn: getTrafficSources });

export const useTopProducts = () =>
  useQuery({ queryKey: [...KEY, 'top'], queryFn: () => getTopProducts(5) });

export type { RangeQuery };
