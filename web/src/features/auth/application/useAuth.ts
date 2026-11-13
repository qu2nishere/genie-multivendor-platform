// Tầng ứng dụng xác thực (plan G09.04): gọi API, đặt $session, gộp giỏ khách, chuyển về `next`.
import { useMutation } from '@tanstack/react-query';
import { clearSession, setSession, type SessionUser } from '@/shared/auth/session';
import { safeNext, type OtpPurpose } from '../domain/auth.rules';
import type { AuthSession, SocialProvider } from '../domain/auth.types';
import {
  completeOAuth,
  login,
  oauthAuthorizeUrl,
  logout,
  register,
  requestOtp,
  resendOtp,
  resetPassword,
  verifyOtp,
  type RegisterPayload,
} from '../infrastructure/auth.api';

/**
 * Nhận phiên mới: lưu phiên, chạy `afterLogin` (UI truyền mergeGuestCart của feature cart — ghép
 * giữa các feature làm ở tầng UI theo TS 5.2), rồi điều hướng về `next`.
 */
export async function finishLogin(
  session: AuthSession<SessionUser>,
  next: string | null,
  afterLogin?: () => Promise<unknown>,
): Promise<void> {
  setSession(session.accessToken, session.user);
  if (afterLogin) await afterLogin();
  window.location.assign(safeNext(next));
}

/** Địa chỉ bắt đầu đăng nhập Google / Apple (backend 302 sang nhà cung cấp). */
export const oauthStartUrl = oauthAuthorizeUrl;

/** Hoàn tất đăng nhập mạng xã hội ở trang callback. */
export const finishOAuth = (
  provider: SocialProvider,
  code: string,
  state: string,
  redirectUri: string,
) => completeOAuth(provider, code, state, redirectUri);

export function useLogin() {
  return useMutation({
    mutationFn: (v: { email: string; password: string }) => login(v.email, v.password),
  });
}

export function useRequestOtp() {
  return useMutation({
    mutationFn: (v: { email: string; purpose: Exclude<OtpPurpose, 'REGISTER'> }) =>
      requestOtp(v.email, v.purpose),
  });
}

export function useResendOtp() {
  return useMutation({ mutationFn: resendOtp });
}

export function useVerifyOtp() {
  return useMutation({
    mutationFn: (v: { challengeId: string; otp: string; email: string }) =>
      verifyOtp(v.challengeId, v.otp, v.email),
  });
}

export function useRegister() {
  return useMutation({ mutationFn: (v: RegisterPayload) => register(v) });
}

export function useForgot() {
  return useMutation({ mutationFn: (email: string) => requestOtp(email, 'PASSWORD_RESET') });
}

export function useResetPassword() {
  return useMutation({
    mutationFn: (v: { resetToken: string; password: string }) =>
      resetPassword(v.resetToken, v.password),
  });
}

/** Đăng xuất: báo server (lỗi vẫn xóa phiên phía client), rồi về trang chủ. */
export async function signOut(): Promise<void> {
  try {
    await logout();
  } finally {
    clearSession();
    window.location.assign('/');
  }
}
