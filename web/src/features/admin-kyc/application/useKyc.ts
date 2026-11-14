// Hàng chờ + chi tiết + quyết định KYC (TanStack Query). Mỗi quyết định làm mới mọi truy vấn
// ['admin', …] để huy hiệu nav và "Việc cần xử lý" ở Tổng quan đổi theo.
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { KycRejectReason } from '../domain/kyc.rules';
import type { KycDetail, KycDocType, KycTab } from '../domain/kyc.types';
import {
  approveKyc,
  getKycSubmission,
  listKycSubmissions,
  rejectKyc,
  requestKycSupplement,
  requeueKyc,
} from '../infrastructure/kyc.api';

const ADMIN = ['admin'] as const;

export const useKycQueue = (tab: KycTab) =>
  useQuery({
    queryKey: [...ADMIN, 'kyc', 'list', tab],
    queryFn: () => listKycSubmissions(tab),
    placeholderData: keepPreviousData,
  });

export const useKycDetail = (id: number | null) =>
  useQuery({
    queryKey: [...ADMIN, 'kyc', 'detail', id],
    queryFn: () => getKycSubmission(id ?? 0),
    enabled: id != null,
  });

export type KycDecision =
  | { kind: 'approve'; id: number; shopCommissionRate: number | null }
  | { kind: 'reject'; id: number; reason: KycRejectReason; note: string | null; label: string }
  | { kind: 'supplement'; id: number; documents: KycDocType[]; note: string }
  | { kind: 'requeue'; id: number };

function decide(d: KycDecision): Promise<KycDetail> {
  switch (d.kind) {
    case 'approve':
      return approveKyc(d.id, d.shopCommissionRate);
    case 'reject':
      return rejectKyc(d.id, d.reason, d.note, d.label);
    case 'supplement':
      return requestKycSupplement(d.id, d.documents, d.note);
    case 'requeue':
      return requeueKyc(d.id);
  }
}

export function useKycDecision() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: decide,
    onSuccess: () => qc.invalidateQueries({ queryKey: ADMIN }),
  });
}
