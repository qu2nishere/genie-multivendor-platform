// Truy vấn Tổng quan sàn (TanStack Query). Khối tiền chỉ gọi khi có VIEW_FINANCE (API 11.2).
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import type { GmvRange, MonthKey } from '../domain/dashboard.types';
import {
  getDashboardFinance,
  getGmvChart,
  getOperations,
  listGrowingShops,
  type GrowingQuery,
} from '../infrastructure/dashboard.api';

export const useDashboardFinance = (month: MonthKey | undefined, enabled: boolean) =>
  useQuery({
    queryKey: ['admin', 'dashboard', 'finance', month ?? 'latest'],
    queryFn: () => getDashboardFinance(month),
    enabled,
    placeholderData: keepPreviousData,
  });

export const useGmvChart = (range: GmvRange, month: MonthKey | undefined, enabled: boolean) =>
  useQuery({
    queryKey: ['admin', 'dashboard', 'gmv', range, month ?? 'latest'],
    queryFn: () => getGmvChart(range, month),
    enabled,
    placeholderData: keepPreviousData,
  });

export const useOperations = () =>
  useQuery({ queryKey: ['admin', 'dashboard', 'operations'], queryFn: getOperations });

export const useGrowingShops = (g: GrowingQuery) =>
  useQuery({
    queryKey: ['admin', 'dashboard', 'growing', g],
    queryFn: () => listGrowingShops(g),
    placeholderData: keepPreviousData,
  });
