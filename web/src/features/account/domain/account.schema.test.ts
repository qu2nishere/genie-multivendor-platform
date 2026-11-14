import { describe, expect, it } from 'vitest';
import { emailChangeSchema, passwordSchema, profileSchema } from './account.schema';

const msgs = (r: { success: boolean; error?: { issues: { message: string }[] } }) =>
  r.success ? [] : (r.error?.issues.map((i) => i.message) ?? []);

describe('account.schema', () => {
  it('hồ sơ: tên bắt buộc, SĐT có thể trống, ngày sinh không ở tương lai', () => {
    const base = {
      fullName: 'Mai',
      phone: '',
      gender: null,
      dateOfBirth: '',
      bio: '',
      language: 'vi',
    };
    expect(profileSchema.safeParse(base).success).toBe(true);
    expect(msgs(profileSchema.safeParse({ ...base, fullName: ' ' }))).toContain(
      'account.profile.err.name',
    );
    expect(msgs(profileSchema.safeParse({ ...base, phone: '0123' }))).toContain(
      'account.address.err.phone',
    );
    expect(msgs(profileSchema.safeParse({ ...base, dateOfBirth: '2999-01-01' }))).toContain(
      'account.profile.err.dob',
    );
  });

  it('đổi email: đúng định dạng, khác email hiện tại', () => {
    const s = emailChangeSchema('mai@gmail.com');
    expect(msgs(s.safeParse({ email: 'MAI@gmail.com' }))).toContain('account.email.err.same');
    expect(msgs(s.safeParse({ email: 'abc' }))).toContain('auth.err.email_invalid');
    expect(s.safeParse({ email: 'mai.work@gmail.com' }).success).toBe(true);
  });

  it('mật khẩu: tài khoản chỉ Google không cần mật khẩu cũ', () => {
    const v = { current: '', next: 'NewPass@2026', confirm: 'NewPass@2026' };
    expect(passwordSchema(false).safeParse(v).success).toBe(true);
    expect(msgs(passwordSchema(true).safeParse(v))).toContain('account.password.err.current');
    expect(msgs(passwordSchema(true).safeParse({ ...v, current: 'NewPass@2026' }))).toContain(
      'account.password.err.same',
    );
    expect(msgs(passwordSchema(false).safeParse({ ...v, confirm: 'x' }))).toContain(
      'ui.password.mismatch',
    );
  });
});
