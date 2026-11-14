// Ngữ cảnh nhân viên cho khung Admin (TanStack Query).
import { useQuery } from '@tanstack/react-query';
import { getAdminMe } from '../infrastructure/me.api';

export const ADMIN_ME_KEY = ['admin', 'me'] as const;

export const useAdminMe = () => useQuery({ queryKey: ADMIN_ME_KEY, queryFn: getAdminMe });
