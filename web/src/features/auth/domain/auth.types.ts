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

/** Đăng nhập mạng xã hội (UC-AUTH-03; plan D17: Apple thay bằng Zalo). */
export type SocialProvider = 'google' | 'zalo';

/** Zalo không trả email → lần đầu phải nhập email + OTP (plan D23). */
export interface SocialSignup {
  provider: SocialProvider;
  token: string;
  /** Tên lấy từ Zalo (có thể null). */
  name: string | null;
  expiresIn: number;
}
