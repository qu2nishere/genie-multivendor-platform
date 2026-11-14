// Hồ sơ & bảo mật (TanStack Query) — plan G10.45–G10.53.
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { ProfilePatch, StepUpProof, StepUpPurpose } from '../domain/account.types';
import {
  cancelDeletion,
  changePassword,
  confirmEmailChange,
  getLoginMethods,
  getProfile,
  linkGoogle,
  listDevices,
  logoutDevices,
  requestDeletion,
  requestEmailChange,
  requestStepUp,
  resendStepUp,
  unlinkGoogle,
  updateProfile,
  verifyPassword,
} from '../infrastructure/account.api';

export const PROFILE_KEY = ['me', 'profile'] as const;
export const METHODS_KEY = ['me', 'login-methods'] as const;
export const DEVICES_KEY = ['me', 'sessions'] as const;

export const useProfile = (enabled = true) =>
  useQuery({ queryKey: PROFILE_KEY, queryFn: getProfile, enabled });
export const useLoginMethods = (enabled = true) =>
  useQuery({ queryKey: METHODS_KEY, queryFn: getLoginMethods, enabled });
export const useDevices = () => useQuery({ queryKey: DEVICES_KEY, queryFn: listDevices });

function useInvalidating<V, R>(fn: (v: V) => Promise<R>, keys: readonly (readonly string[])[]) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: fn,
    onSuccess: () => keys.forEach((k) => void qc.invalidateQueries({ queryKey: k })),
  });
}

export const useUpdateProfile = () =>
  useInvalidating((p: ProfilePatch) => updateProfile(p), [PROFILE_KEY]);
export const useRequestEmailChange = () => useMutation({ mutationFn: requestEmailChange });
export const useConfirmEmailChange = () =>
  useInvalidating((p: StepUpProof) => confirmEmailChange(p), [PROFILE_KEY, METHODS_KEY]);
export const useLinkGoogle = () =>
  useInvalidating((code: string | null) => linkGoogle(code), [METHODS_KEY]);
export const useUnlinkGoogle = () => useInvalidating(() => unlinkGoogle(), [METHODS_KEY]);
export const useVerifyPassword = () => useMutation({ mutationFn: verifyPassword });
export const useRequestStepUp = () =>
  useMutation({ mutationFn: (purpose: StepUpPurpose) => requestStepUp(purpose) });
export const useResendStepUp = () => useMutation({ mutationFn: resendStepUp });
export const useChangePassword = () =>
  useInvalidating(
    (v: { current: string | null; next: string; proof: StepUpProof }) =>
      changePassword(v.current, v.next, v.proof),
    [METHODS_KEY, DEVICES_KEY],
  );
export const useLogoutDevices = () =>
  useInvalidating((id: number | 'others') => logoutDevices(id), [DEVICES_KEY]);
export const useRequestDeletion = () =>
  useInvalidating((p: StepUpProof) => requestDeletion(p), [PROFILE_KEY]);
export const useCancelDeletion = () => useInvalidating(() => cancelDeletion(), [PROFILE_KEY]);
