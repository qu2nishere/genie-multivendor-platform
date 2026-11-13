// Kiểu miền xác thực.
import type { OtpPurpose } from './auth.rules';

export interface OtpChallenge {
  id: string;
  purpose: OtpPurpose;
  emailMasked: string;
  /** Giây còn hiệu lực / giây đến khi được gửi lại. */
  expiresIn: number;
  resendAfter: number;
}

/** Phiên sau đăng nhập; `U` là kiểu người dùng của tầng phiên (shared/auth SessionUser). */
export interface AuthSession<U = unknown> {
  accessToken: string;
  user: U;
}

export type VerifyResult<U = unknown> =
  { kind: 'session'; session: AuthSession<U> } | { kind: 'reset'; resetToken: string };

export type SocialProvider = 'google' | 'apple';
