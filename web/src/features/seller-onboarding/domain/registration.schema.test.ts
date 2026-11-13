import { describe, expect, it } from 'vitest';
import { registrationSchema } from './registration.schema';

const ok = {
  shopName: 'Góc Nhà Mai',
  ownerName: 'Trần Thị Mai',
  phone: '0901 234 567',
  email: 'Mai@Shop.vn',
  mainCategoryId: 1,
  businessType: 'INDIVIDUAL',
  acceptTerms: true,
};

describe('registrationSchema', () => {
  it('hợp lệ → chuẩn hóa SĐT, email', () => {
    const r = registrationSchema.safeParse(ok);
    expect(r.success && r.data.phone).toBe('0901234567');
    expect(r.success && r.data.email).toBe('mai@shop.vn');
  });
  it('báo lỗi theo trường', () => {
    const r = registrationSchema.safeParse({
      ...ok,
      shopName: 'A',
      phone: '12',
      acceptTerms: false,
    });
    const msgs = r.success ? [] : r.error.issues.map((i) => i.message);
    expect(msgs).toContain('seller-onboarding.landing.err.shop');
    expect(msgs).toContain('account.address.err.phone');
    expect(msgs).toContain('seller-onboarding.landing.err.terms');
  });
});
