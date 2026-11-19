import { describe, expect, it } from 'vitest';
import {
  defaultAddress,
  formatPhone,
  fullAddress,
  isSameAddress,
  normalizeLine,
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
  province: { id: 79, name: 'Thành phố Hồ Chí Minh' },
  ward: { code: '26740', name: 'Phường Sài Gòn' },
  line: '25 Nguyễn Huệ',
  note: null,
  label: 'HOME',
  isDefault,
});

describe('address.rules', () => {
  it('địa chỉ đầy đủ, mặc định đứng đầu', () => {
    expect(fullAddress(a(1))).toBe('25 Nguyễn Huệ, Phường Sài Gòn, Thành phố Hồ Chí Minh');
    expect(sortAddresses([a(1), a(2, true)]).map((x) => x.id)).toEqual([2, 1]);
    expect(defaultAddress([a(1), a(2, true)])?.id).toBe(2);
    expect(defaultAddress([])).toBeNull();
    expect(withDefault([a(1, true), a(2)], 2).map((x) => x.isDefault)).toEqual([false, true]);
  });

  it('so trùng địa chỉ: bỏ dấu, hoa thường, dấu câu, khoảng trắng; khác SĐT / phường thì không trùng', () => {
    expect(normalizeLine('25 Nguyễn Huệ, Căn hộ 12A')).toBe('25 nguyen hue can ho 12a');
    const x = { provinceId: 79, wardCode: '26740', phone: '0901234567', line: '25 Nguyễn Huệ' };
    expect(isSameAddress(x, { ...x, line: '25  nguyen hue,', phone: '0901 234 567' })).toBe(true);
    expect(isSameAddress(x, { ...x, phone: '0911111111' })).toBe(false);
    expect(isSameAddress(x, { ...x, wardCode: '26743' })).toBe(false);
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
      wardCode: '',
      line: '',
      note: 'x'.repeat(201),
      label: 'HOME',
      isDefault: false,
    });
    expect(r.success).toBe(false);
    const msgs = r.success ? [] : r.error.issues.map((i) => i.message);
    expect(msgs).toContain('account.address.err.recipient');
    expect(msgs).toContain('account.address.err.phone');
    expect(msgs).toContain('account.address.err.region');
    expect(msgs).toContain('account.address.err.line');
    expect(msgs).toContain('account.address.err.note_long');
  });
});
