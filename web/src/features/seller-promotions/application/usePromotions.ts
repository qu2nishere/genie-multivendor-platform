import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { VoucherInput } from '../domain/promo.types';
import {
  disableVoucher,
  getVoucher,
  listEligible,
  listFlashCampaigns,
  listFlashItems,
  listVouchers,
  listVoucherTargets,
  registerFlashItem,
  saveVoucher,
} from '../infrastructure/promo.api';

const KEY = ['seller', 'promotions'] as const;

function useInvalidating<V, R>(fn: (v: V) => Promise<R>) {
  const qc = useQueryClient();
  return useMutation({ mutationFn: fn, onSuccess: () => qc.invalidateQueries({ queryKey: KEY }) });
}

export const useVouchers = () =>
  useQuery({ queryKey: [...KEY, 'vouchers'], queryFn: listVouchers });
export const useVoucher = (id: number | null) =>
  useQuery({
    queryKey: [...KEY, 'voucher', id],
    queryFn: () => getVoucher(id ?? 0),
    enabled: id != null,
    staleTime: 0,
  });
export const useVoucherTargets = () =>
  useQuery({ queryKey: [...KEY, 'targets'], queryFn: listVoucherTargets, staleTime: Infinity });
export const useSaveVoucher = () =>
  useInvalidating((v: { id: number | null; input: VoucherInput }) => saveVoucher(v.id, v.input));
export const useDisableVoucher = () => useInvalidating(disableVoucher);

export const useFlashCampaigns = () =>
  useQuery({ queryKey: [...KEY, 'flash'], queryFn: listFlashCampaigns });
export const useEligible = (id: number | null) =>
  useQuery({
    queryKey: [...KEY, 'flash', id, 'eligible'],
    queryFn: () => listEligible(id ?? 0),
    enabled: id != null,
  });
export const useFlashItems = (id: number | null) =>
  useQuery({
    queryKey: [...KEY, 'flash', id, 'items'],
    queryFn: () => listFlashItems(id ?? 0),
    enabled: id != null,
  });
export const useRegisterFlash = () =>
  useInvalidating(
    (v: { campaignId: number; productId: number; flashPrice: number; flashStock: number }) =>
      registerFlashItem(v.campaignId, v),
  );
