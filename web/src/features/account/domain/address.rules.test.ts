import { describe, expect, it } from 'vitest';
import {
  defaultAddress,
  formatPhone,
  fullAddress,
  normalizePhone,
  PHONE_RE,
  sortAddresses,
  withDefault,
} from './address.rules';
import { addressSchema } from './address.schema';
import type { Address } from './address.types';

const a = (id: number, isDefault = false): Address => ({
  id,
  recipient: 'Mai',
  phone: '0901234567',
  province: { id: 1, name: 'TP. Hồ Chí Minh' },
  district: { id: 2, name: 'Quận 1' },
  ward: { code: '20308', name: 'P. Bến Nghé' },
  line: '25 Nguyễn Huệ',
  label: 'HOME',
  isDefault,
});

describe('address.rules', () => {
  it('địa chỉ đầy đủ, mặc định đứng đầu', () => {
    expect(fullAddress(a(1))).toBe('25 Nguyễn Huệ, P. Bến Nghé, Quận 1, TP. Hồ Chí Minh');
    expect(sortAddresses([a(1), a(2, true)]).map((x) => x.id)).toEqual([2, 1]);
    expect(defaultAddress([a(1), a(2, true)])?.id).toBe(2);
    expect(defaultAddress([])).toBeNull();
    expect(withDefault([a(1, true), a(2)], 2).map((x) => x.isDefault)).toEqual([false, true]);
  });

  it('SĐT', () => {
    expect(formatPhone('0901234567')).toBe('0901 234 567');
    expect(PHONE_RE.test(normalizePhone('0901 234 567'))).toBe(true);
    expect(PHONE_RE.test('090123')).toBe(false);
  });

  it('lược đồ form: thiếu cấp hành chính, SĐT sai', () => {
    const r = addressSchema.safeParse({
      recipient: ' ',
      phone: '123',
      provinceId: 0,
      districtId: 0,
      wardCode: '',
      line: '',
      label: 'HOME',
      isDefault: false,
    });
    expect(r.success).toBe(false);
    const msgs = r.success ? [] : r.error.issues.map((i) => i.message);
    expect(msgs).toContain('account.address.err.recipient');
    expect(msgs).toContain('account.address.err.phone');
    expect(msgs).toContain('account.address.err.region');
    expect(msgs).toContain('account.address.err.line');
  });
});
