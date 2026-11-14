import { useMutation } from '@tanstack/react-query';
import { refreshAccessToken } from '@/shared/auth/session';
import { confirmShopRegistration, registerShop } from '../infrastructure/registration.api';

export const useRegisterShop = () => useMutation({ mutationFn: registerShop });

/** Xác nhận xong làm mới token để có quyền /seller (9.2). */
export const useConfirmShop = () =>
  useMutation({
    mutationFn: async (v: { challengeId: string; otp: string }) => {
      const r = await confirmShopRegistration(v.challengeId, v.otp);
      await refreshAccessToken().catch(() => false);
      return r;
    },
  });
