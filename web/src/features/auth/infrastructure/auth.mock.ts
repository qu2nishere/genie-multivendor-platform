// Mock xác thực (prototype auth/index.html): mật khẩu Genie@123, OTP 123456, email mai.tran@gmail.com
// đã có tài khoản; sai 5 lần → khóa 15 phút. Trạng thái giữ trong bộ nhớ (theo tab trình duyệt).
import { mockFail } from '@/shared/api/mock';
import { MOCK_USER } from '@/shared/auth/session';
import {
  LOCK_SECONDS,
  MAX_ATTEMPTS,
  OTP_TTL,
  RESEND_AFTER,
  type OtpPurpose,
} from '../domain/auth.rules';
import type { AuthTokensDto, OtpChallengeDto } from './auth.dto';

export const MOCK_PASSWORD = 'Genie@123';
export const MOCK_OTP = '123456';
export const EXISTING_EMAILS = ['mai.tran@gmail.com'];

interface Challenge {
  id: string;
  email: string;
  purpose: OtpPurpose;
  attempts: number;
  expiresAt: number;
  resendAt: number;
  dead: boolean;
}

const challenges = new Map<string, Challenge>();
const pwFails = new Map<string, number>();
const lockedUntil = new Map<string, number>();
let seq = 0;

const key = (email: string) => email.trim().toLowerCase();

export function maskEmail(email: string): string {
  const [name, domain] = email.split('@');
  return `${name.slice(0, 2)}****@${domain ?? ''}`;
}

export function mockTokens(): AuthTokensDto {
  return {
    access_token: 'mock-access-token',
    token_type: 'Bearer',
    expires_in: 900,
    user: { id: MOCK_USER.id, role: 'CUSTOMER', full_name: MOCK_USER.name, avatar_url: null },
  };
}

function lockError(email: string, by: 'PASSWORD' | 'OTP'): never {
  const until = lockedUntil.get(key(email)) ?? Date.now() + LOCK_SECONDS * 1000;
  mockFail('GNE-AUTH-4230', {
    status: 423,
    details: { locked_until: new Date(until).toISOString(), locked_by: by },
  });
}

export function mockLogin(email: string, password: string): AuthTokensDto {
  const k = key(email);
  if ((lockedUntil.get(k) ?? 0) > Date.now()) lockError(email, 'PASSWORD');
  if (password === MOCK_PASSWORD) {
    pwFails.delete(k);
    return mockTokens();
  }
  const n = (pwFails.get(k) ?? 0) + 1;
  pwFails.set(k, n);
  if (n >= MAX_ATTEMPTS) {
    pwFails.delete(k);
    lockedUntil.set(k, Date.now() + LOCK_SECONDS * 1000);
    lockError(email, 'PASSWORD');
  }
  mockFail('GNE-AUTH-4011', { status: 401, details: { remaining_attempts: MAX_ATTEMPTS - n } });
}

export function mockChallenge(email: string, purpose: OtpPurpose): OtpChallengeDto {
  const id = `otp_${(++seq).toString(36)}${Date.now().toString(36)}`;
  const now = Date.now();
  challenges.set(id, {
    id,
    email,
    purpose,
    attempts: 0,
    expiresAt: now + OTP_TTL * 1000,
    resendAt: now + RESEND_AFTER * 1000,
    dead: false,
  });
  return {
    challenge_id: id,
    purpose,
    email_masked: maskEmail(email),
    expires_in: OTP_TTL,
    resend_after: RESEND_AFTER,
  };
}

export function mockResend(id: string): OtpChallengeDto {
  const c = challenges.get(id);
  if (!c || c.dead) mockFail('GNE-AUTH-4101', { status: 410 });
  if (Date.now() < c.resendAt) {
    mockFail('GNE-RAT-4290', {
      status: 429,
      details: { retry_after: Math.ceil((c.resendAt - Date.now()) / 1000) },
    });
  }
  challenges.delete(id);
  return mockChallenge(c.email, c.purpose);
}

export type MockVerify =
  { kind: 'tokens'; tokens: AuthTokensDto } | { kind: 'reset'; reset_token: string };

export function mockVerify(id: string, otp: string): MockVerify {
  const c = challenges.get(id);
  if (!c || c.dead || Date.now() > c.expiresAt) mockFail('GNE-AUTH-4101', { status: 410 });
  if (otp !== MOCK_OTP) {
    c.attempts += 1;
    if (c.attempts >= MAX_ATTEMPTS) {
      c.dead = true;
      if (c.purpose === 'LOGIN' || c.purpose === 'ACCOUNT_UNLOCK') {
        lockedUntil.set(key(c.email), Date.now() + LOCK_SECONDS * 1000);
      }
      const until = new Date(Date.now() + LOCK_SECONDS * 1000).toISOString();
      mockFail('GNE-AUTH-4221', {
        status: 422,
        details: { purpose: c.purpose, locked_until: until, locked_by: 'OTP' },
      });
    }
    mockFail('GNE-AUTH-4222', {
      status: 422,
      details: { remaining_attempts: MAX_ATTEMPTS - c.attempts },
    });
  }
  challenges.delete(id);
  if (c.purpose === 'PASSWORD_RESET') return { kind: 'reset', reset_token: `rst_${id}` };
  lockedUntil.delete(key(c.email));
  pwFails.delete(key(c.email));
  if (c.purpose === 'REGISTER') EXISTING_EMAILS.push(key(c.email));
  return { kind: 'tokens', tokens: mockTokens() };
}

export function mockReset(newPassword: string): void {
  if (newPassword === MOCK_PASSWORD) mockFail('GNE-AUTH-4223', { status: 422 });
}

/** Chỉ dùng trong test: xoá trạng thái mock. */
export function resetAuthMock(): void {
  challenges.clear();
  pwFails.clear();
  lockedUntil.clear();
}
