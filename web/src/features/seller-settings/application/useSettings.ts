// Cài đặt Shop (TanStack Query). Ghi xong làm mới mọi truy vấn ['seller', …] (tên shop ở khung, ví…).
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { PickupAddress, SettingsPatch } from '../domain/settings.types';
import {
  deletePayoutAccount,
  endVacation,
  getSettings,
  listBanks,
  savePayoutAccount,
  savePickupAddress,
  startVacation,
  verifyPayoutAccount,
  patchSettings,
  type PayoutInput,
} from '../infrastructure/settings.api';

const KEY = ['seller', 'settings'] as const;

function useSellerMutation<V, R>(fn: (v: V) => Promise<R>) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: fn,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['seller'] }),
  });
}

export const useShopSettings = () => useQuery({ queryKey: KEY, queryFn: getSettings });
export const usePatchSettings = () => useSellerMutation((p: SettingsPatch) => patchSettings(p));
export const useSavePickup = () => useSellerMutation((a: PickupAddress) => savePickupAddress(a));
export const useBanks = () =>
  useQuery({ queryKey: ['public', 'banks'], queryFn: listBanks, staleTime: Infinity });
export const useVerifyPayout = () => useMutation({ mutationFn: verifyPayoutAccount });
export const useSavePayout = () =>
  useSellerMutation((v: { input: PayoutInput; proof: { challengeId: string; code: string } }) =>
    savePayoutAccount(v.input, v.proof),
  );
export const useDeletePayout = () =>
  useSellerMutation((proof: { challengeId: string; code: string }) => deletePayoutAccount(proof));
export const useStartVacation = () =>
  useSellerMutation((v: { endsAt: string; message: string }) => startVacation(v.endsAt, v.message));
export const useEndVacation = () => useSellerMutation(() => endVacation());

export type { PayoutInput };
