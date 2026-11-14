import { useQuery } from '@tanstack/react-query';
import { getDashboard } from '../infrastructure/dashboard.api';

export const SELLER_DASHBOARD_KEY = ['seller', 'dashboard'] as const;

export const useDashboard = () =>
  useQuery({ queryKey: SELLER_DASHBOARD_KEY, queryFn: getDashboard, refetchInterval: 60_000 });
