import { describe, expect, it } from 'vitest';
import {
  isEmail,
  isPhoneVN,
  LOCK_SECONDS,
  MAX_ATTEMPTS,
  OTP_TTL,
  remainingAttempts,
  RESEND_AFTER,
  safeNext,
} from './auth.rules';
import { fieldErrors, loginSchema, registerSchema, resetSchema } from './auth.schemas';

describe('auth.rules', () => {
  it('hằng số theo UC-AUTH', () => {
    expect([OTP_TTL, RESEND_AFTER, MAX_ATTEMPTS, LOCK_SECONDS]).toEqual([300, 60, 5, 900]);
  });
  it('email / số điện thoại', () => {
    expect(isEmail('ban@email.com')).toBe(true);
    expect(isEmail('ban@email')).toBe(false);
    expect(isPhoneVN('0901 234 567')).toBe(true);
    expect(isPhoneVN('901234567')).toBe(false);
  });
  it('số lần thử còn lại', () => {
    expect(remainingAttempts(3)).toBe(2);
    expect(remainingAttempts(9)).toBe(0);
  });
  it('next chỉ nhận đường dẫn nội bộ', () => {
    expect(safeNext('/checkout?x=1')).toBe('/checkout?x=1');
    expect(safeNext('https://evil.com')).toBe('/');
    expect(safeNext('//evil.com')).toBe('/');
    expect(safeNext('/auth/login')).toBe('/');
    expect(safeNext(null)).toBe('/');
  });
});

describe('auth.schemas', () => {
  it('đăng nhập', () => {
    expect(loginSchema.safeParse({ email: 'a@b.co', password: 'x' }).success).toBe(true);
    const r = loginSchema.safeParse({ email: 'sai', password: '' });
    expect(r.success).toBe(false);
    if (!r.success)
      expect(fieldErrors(r.error)).toEqual({
        email: 'auth.err.email_invalid',
        password: 'auth.err.password_required',
      });
  });

  it('đăng ký: mật khẩu mạnh, khớp, đồng ý điều khoản', () => {
    const ok = {
      name: 'Mai',
      email: 'mai@x.vn',
      phone: '0901234567',
      password: 'Genie@123',
      confirm: 'Genie@123',
      agree: true,
    };
    expect(registerSchema.safeParse(ok).success).toBe(true);
    const r = registerSchema.safeParse({ ...ok, password: 'genie123', confirm: 'x', agree: false });
    expect(r.success).toBe(false);
    if (!r.success) {
      const e = fieldErrors(r.error);
      expect(e.password).toBe('ui.password.issue.upper');
      expect(e.agree).toBe('auth.err.agree');
    }
  });

  it('đặt lại mật khẩu: nhập lại phải khớp', () => {
    const r = resetSchema.safeParse({ password: 'Genie@1234', confirm: 'Genie@123' });
    expect(r.success).toBe(false);
    if (!r.success) expect(fieldErrors(r.error).confirm).toBe('ui.password.mismatch');
  });
});
