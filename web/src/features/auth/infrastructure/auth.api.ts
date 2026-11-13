// Xác thực (API_SPEC §2). Mock khi USE_MOCK. Mọi lỗi là ApiError mã GNE-AUTH-* / GNE-RAT-4290,
// UI dịch qua shared/api/errors.ts (G09.13).
import { api, request } from '@/shared/api/http';
import { mockDelay, mockFail, USE_MOCK } from '@/shared/api/mock';
import type { OtpPurpose } from '../domain/auth.rules';
import type { SessionUser } from '@/shared/auth/session';
import type { AuthSession, OtpChallenge, SocialProvider, VerifyResult } from '../domain/auth.types';
import type { AuthTokensDto, OAuthResultDto, OtpChallengeDto, ResetTokenDto } from './auth.dto';
import { toChallenge, toSession } from './auth.mapper';
import {
  EXISTING_EMAILS,
  mockChallenge,
  mockLogin,
  mockReset,
  mockResend,
  mockTokens,
  mockVerify,
} from './auth.mock';

const noAuth = { auth: false } as const;

/** POST /auth/sessions — đăng nhập bằng mật khẩu. */
export async function login(email: string, password: string): Promise<AuthSession<SessionUser>> {
  if (USE_MOCK) {
    await mockDelay(350);
    return toSession(mockLogin(email, password), email);
  }
  return toSession(
    await api.post<AuthTokensDto>('/auth/sessions', { email, password }, noAuth),
    email,
  );
}

/** POST /auth/otp-challenges — OTP đăng nhập / quên mật khẩu / mở khóa tài khoản. */
export async function requestOtp(
  email: string,
  purpose: Exclude<OtpPurpose, 'REGISTER'>,
): Promise<OtpChallenge> {
  if (USE_MOCK) {
    await mockDelay(300);
    return toChallenge(mockChallenge(email, purpose));
  }
  return toChallenge(
    await api.post<OtpChallengeDto>('/auth/otp-challenges', { purpose, email }, noAuth),
  );
}

/** POST /auth/otp-challenges/{id}/resend — 429 GNE-RAT-4290 nếu chưa đủ 60 giây. */
export async function resendOtp(challengeId: string): Promise<OtpChallenge> {
  if (USE_MOCK) {
    await mockDelay(250);
    return toChallenge(mockResend(challengeId));
  }
  return toChallenge(
    await api.post<OtpChallengeDto>(
      `/auth/otp-challenges/${encodeURIComponent(challengeId)}/resend`,
      undefined,
      noAuth,
    ),
  );
}

/** POST /auth/otp-challenges/{id}/verify — phiên (đăng nhập / đăng ký / mở khóa) hoặc reset_token. */
export async function verifyOtp(
  challengeId: string,
  otp: string,
  email = '',
): Promise<VerifyResult<SessionUser>> {
  if (USE_MOCK) {
    await mockDelay(350);
    const r = mockVerify(challengeId, otp);
    return r.kind === 'reset'
      ? { kind: 'reset', resetToken: r.reset_token }
      : { kind: 'session', session: toSession(r.tokens, email) };
  }
  const data = await api.post<AuthTokensDto | ResetTokenDto>(
    `/auth/otp-challenges/${encodeURIComponent(challengeId)}/verify`,
    { otp },
    noAuth,
  );
  return 'reset_token' in data
    ? { kind: 'reset', resetToken: data.reset_token }
    : { kind: 'session', session: toSession(data, email) };
}

export interface RegisterPayload {
  name: string;
  email: string;
  phone: string;
  password: string;
}

/** POST /auth/registrations — 202 + OTP REGISTER; 409 GNE-AUTH-4091 khi email đã có tài khoản. */
export async function register(p: RegisterPayload): Promise<OtpChallenge> {
  if (USE_MOCK) {
    await mockDelay(400);
    if (EXISTING_EMAILS.includes(p.email.trim().toLowerCase())) {
      mockFail('GNE-AUTH-4091', { status: 409 });
    }
    return toChallenge(mockChallenge(p.email, 'REGISTER'));
  }
  return toChallenge(
    await api.post<OtpChallengeDto>(
      '/auth/registrations',
      {
        full_name: p.name.trim(),
        email: p.email.trim(),
        phone_number: p.phone.replace(/\s/g, ''),
        password: p.password,
        accept_terms: true,
      },
      noAuth,
    ),
  );
}

/** POST /auth/password-resets — đặt mật khẩu mới bằng reset_token. */
export async function resetPassword(resetToken: string, newPassword: string): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(350);
    mockReset(newPassword);
    return;
  }
  await request('/auth/password-resets', {
    method: 'POST',
    body: { reset_token: resetToken, new_password: newPassword },
    auth: false,
  });
}

/** DELETE /auth/sessions/current. */
export async function logout(): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(150);
    return;
  }
  await api.delete('/auth/sessions/current');
}

/** Bắt đầu đăng nhập Google / Apple: địa chỉ trang ủy quyền của backend (302 sang nhà cung cấp). */
export function oauthAuthorizeUrl(
  provider: SocialProvider,
  apiBase: string,
  callback: string,
): string {
  return `${apiBase}/auth/oauth/${provider}/authorize?redirect_uri=${encodeURIComponent(callback)}`;
}

/** POST /auth/oauth/{provider}/token — hoàn tất ở trang callback. */
export async function completeOAuth(
  provider: SocialProvider,
  code: string,
  state: string,
  redirectUri: string,
): Promise<{ result: OAuthResultDto['result']; session: AuthSession<SessionUser> }> {
  if (USE_MOCK) {
    await mockDelay(500);
    return { result: 'LOGGED_IN', session: toSession(mockTokens()) };
  }
  const r = await api.post<OAuthResultDto>(
    `/auth/oauth/${provider}/token`,
    { code, state, redirect_uri: redirectUri },
    noAuth,
  );
  return { result: r.result, session: toSession(r.tokens) };
}
