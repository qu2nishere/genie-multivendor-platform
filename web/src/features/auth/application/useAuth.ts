// Tầng ứng dụng xác thực (plan G09.04): gọi API, đặt $session, gộp giỏ khách, chuyển về `next`.
import { useMutation } from '@tanstack/react-query';
import { clearSession, loadSessionUser, setSession, type SessionUser } from '@/shared/auth/session';
import { homeFor, safeNext, type OtpPurpose } from '../domain/auth.rules';
import type { AuthSession, SocialProvider } from '../domain/auth.types';
import {
  completeOAuth,
  socialSignup,
  linkIdentity,
  login,
  oauthAuthorizeUrl,
  logout,
  register,
  requestOtp,
  resendOtp,
  resetPassword,
  verifyOtp,
  requestStaffActivationOtp,
  activateStaff,
  type RegisterPayload,
  type StaffActivationPayload,
} from '../infrastructure/auth.api';

/**
 * Nhận phiên mới: nạp ngữ cảnh từ GET /me, lưu phiên, chạy `afterLogin` (UI truyền mergeGuestCart của feature cart — ghép
 * giữa các feature làm ở tầng UI theo TS 5.2), rồi điều hướng về `next`.
 */
export async function finishLogin(
  session: AuthSession<SessionUser>,
  next: string | null,
  afterLogin?: () => Promise<unknown>,
): Promise<void> {
  // $AuthTokens chỉ có id / vai trò / tên / ảnh → nạp đủ email, shop, có mật khẩu chưa từ GET /me (plan AUTH D18)
  const user = await loadSessionUser(session.accessToken, session.user);
  setSession(session.accessToken, user);
  if (afterLogin) await afterLogin();
  // Không có `next`: nhân viên / Admin vào /admin, người dùng khác về trang chủ.
  window.location.assign(next ? safeNext(next) : homeFor(user.roles));
}

/** Địa chỉ bắt đầu đăng nhập Google / Zalo (backend 302 sang nhà cung cấp). */
export const oauthStartUrl = oauthAuthorizeUrl;

/** Hoàn tất đăng nhập mạng xã hội ở trang callback. */
export const finishOAuth = (provider: SocialProvider, code: string, state: string) =>
  completeOAuth(provider, code, state);

/** Hoàn tất liên kết Google vào tài khoản đang đăng nhập (bắt đầu từ Tài khoản › Liên kết đăng nhập). */
export const finishLink = (provider: SocialProvider, code: string, state: string) =>
  linkIdentity(provider, code, state);

export function useLogin() {
  return useMutation({
    mutationFn: (v: { email: string; password: string }) => login(v.email, v.password),
  });
}

export function useRequestOtp() {
  return useMutation({
    mutationFn: (v: {
      email: string;
      purpose: Exclude<OtpPurpose, 'REGISTER' | 'OAUTH_SIGNUP' | 'STAFF_ACTIVATION'>;
    }) => requestOtp(v.email, v.purpose),
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

/** Đăng ký bằng Zalo: nhập email cho phiên đăng ký tạm → OTP. */
export function useSocialSignup() {
  return useMutation({
    mutationFn: (v: { provider: SocialProvider; token: string; email: string }) =>
      socialSignup(v.provider, v.token, v.email),
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

/** Nhân viên mở liên kết mời → xin OTP kích hoạt (UC-ADM-01). */
export function useStaffActivationOtp() {
  return useMutation({ mutationFn: (token: string) => requestStaffActivationOtp(token) });
}

/** Xác minh OTP + đặt mật khẩu → phiên nhân viên (UC-ADM-01). */
export function useActivateStaff() {
  return useMutation({ mutationFn: (v: StaffActivationPayload) => activateStaff(v) });
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
