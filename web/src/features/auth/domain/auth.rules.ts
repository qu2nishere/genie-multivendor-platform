// Quy tắc xác thực (prototype auth/index.html; UC-AUTH-01..05) — plan G09.01. TS thuần, có test.
export { passwordIssue, passwordStrength } from '@/shared/lib/password';
export type { PasswordIssue, PasswordStrength } from '@/shared/lib/password';

/** OTP 6 chữ số, hiệu lực 5 phút, gửi lại sau 60 giây. */
export const OTP_LENGTH = 6;
export const OTP_TTL = 300;
export const RESEND_AFTER = 60;
/** Sai 5 lần (mật khẩu hoặc OTP) → khóa 15 phút. */
export const MAX_ATTEMPTS = 5;
export const LOCK_SECONDS = 900;

/** Mục đích của một lần gửi OTP (POST /auth/otp-challenges). */
export type OtpPurpose = 'LOGIN' | 'REGISTER' | 'ACCOUNT_UNLOCK' | 'PASSWORD_RESET';

export const isEmail = (v: string): boolean => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v.trim());

/** Số điện thoại Việt Nam: 0 + 9–10 chữ số (prototype /^0\d{8,10}$/, bỏ khoảng trắng). */
export const isPhoneVN = (v: string): boolean => /^0\d{8,10}$/.test(v.replace(/\s/g, ''));

export const remainingAttempts = (fails: number): number => Math.max(0, MAX_ATTEMPTS - fails);

/** Đích sau đăng nhập: chỉ nhận đường dẫn nội bộ (chặn open redirect), mặc định trang chủ. */
export function safeNext(next: string | null | undefined): string {
  // chặn "//evil.com" và "/\evil.com" (trình duyệt coi như URL tuyệt đối)
  if (!next || !next.startsWith('/') || next.startsWith('//') || next.startsWith('/\\')) return '/';
  if (next.startsWith('/auth/')) return '/';
  return next;
}
