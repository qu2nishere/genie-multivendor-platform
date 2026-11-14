import { describe, expect, it } from 'vitest';
import { cn } from './cn';
import {
  clockVN,
  formatCompact,
  formatDate,
  formatDateTime,
  formatHhMmSs,
  formatMmSs,
  formatMonthShort,
  formatRelative,
  formatVND,
  isoDateVN,
  parseCompact,
  partsVND,
} from './format';
import { maskAccount, maskEmail, maskPhone } from './mask';

// Plan G04.05 — 4 nhóm hàm ở cả vi và en
const NBSP = new RegExp('[\u00A0\u202F]', 'g');
const norm = (s: string) => s.replace(NBSP, ' ');

describe('tiền VND', () => {
  it('vi dùng dấu chấm phân nhóm và ₫ đứng sau (khớp prototype formatVND)', () => {
    expect(formatVND(150000, { locale: 'vi' })).toBe('150.000₫');
    expect(formatVND(1250000.6, { locale: 'vi' })).toBe('1.250.001₫');
    expect(formatVND(0, { locale: 'vi' })).toBe('0₫');
  });
  it('en dùng dấu phẩy', () => {
    expect(formatVND(150000, { locale: 'en' })).toBe('150,000₫');
  });
  it('bỏ ký hiệu và tách phần', () => {
    expect(formatVND(99000, { locale: 'vi', symbol: false })).toBe('99.000');
    expect(partsVND(99000, { locale: 'vi' })).toEqual(['99.000', '₫']);
  });
  it('giá trị không phải số → 0', () => {
    expect(formatVND(Number.NaN, { locale: 'vi' })).toBe('0₫');
  });
});

describe('ngày & số', () => {
  const d = '2026-06-08T07:05:00Z'; // 14:05 giờ Việt Nam
  it('formatDate theo giờ Việt Nam', () => {
    expect(formatDate(d, { locale: 'vi' })).toBe('08/06/2026');
    expect(formatDate(d, { locale: 'en' })).toBe('06/08/2026');
  });
  it('formatDateTime', () => {
    expect(formatDateTime(d, { locale: 'vi' })).toBe('14:05 08/06/2026');
    expect(norm(formatDateTime(d, { locale: 'en' }))).toBe('06/08/2026, 2:05 PM');
  });
  it('isoDateVN lấy ngày theo giờ Việt Nam', () => {
    expect(isoDateVN('2026-06-08T16:59:00Z')).toBe('2026-06-08');
    expect(isoDateVN('2026-06-08T17:00:00Z')).toBe('2026-06-09');
  });
  it('formatMonthShort', () => {
    expect(formatMonthShort(6, { locale: 'vi' })).toBe('T6');
    expect(formatMonthShort(6, { locale: 'en' })).toBe('Jun');
  });
  it('formatRelative', () => {
    const now = Date.parse(d);
    expect(formatRelative(now - 10_000, { locale: 'vi', now })).toBe('vừa xong');
    expect(formatRelative(now - 3 * 60_000, { locale: 'vi', now })).toBe('3 phút trước');
    expect(formatRelative(now - 3 * 60_000, { locale: 'en', now })).toBe('3 minutes ago');
    expect(formatRelative(now - 24 * 3600_000, { locale: 'vi', now })).toBe('Hôm qua');
  });
  it('formatCompact / parseCompact (thay parseSold)', () => {
    expect(formatCompact(1200, { locale: 'vi' })).toBe('1,2k');
    expect(formatCompact(1200, { locale: 'en' })).toBe('1.2k');
    expect(formatCompact(980, { locale: 'vi' })).toBe('980');
    expect(formatCompact(2_500_000, { locale: 'vi' })).toBe('2,5tr');
    expect(formatCompact(2_500_000, { locale: 'en' })).toBe('2.5M');
    expect(parseCompact('1,2k')).toBe(1200);
    expect(parseCompact('12k')).toBe(12000);
    expect(parseCompact('980')).toBe(980);
    expect(parseCompact('1.250')).toBe(1250);
    expect(parseCompact('2.5M')).toBe(2_500_000);
    expect(parseCompact('abc')).toBe(0);
  });
  it('đồng hồ đếm ngược', () => {
    expect(formatMmSs(299)).toBe('04:59');
    expect(formatMmSs(-3)).toBe('00:00');
    expect(formatHhMmSs(13 * 3600 + 55 * 60 + 15)).toBe('13:55:15');
  });
});

describe('che dữ liệu', () => {
  it('maskEmail giữ 2 ký tự đầu', () => {
    expect(maskEmail('tranthimai@gmail.com')).toBe('tr****@gmail.com');
    expect(maskEmail('a@b.vn')).toBe('a****@b.vn');
    expect(maskEmail('')).toBe('');
  });
  it('maskPhone / maskAccount dạng 0792******34', () => {
    expect(maskPhone('0792 123 434')).toBe('0792****34');
    expect(maskAccount('079212345634')).toBe('0792******34');
    expect(maskAccount('1234')).toBe('1234');
  });
});

describe('cn', () => {
  it('ghép lớp có điều kiện', () => {
    expect(cn('a', false, null, undefined, '', ['b', { c: true, d: false }], 0)).toBe('a b c 0');
  });
});

describe('clockVN', () => {
  it('quy đổi sang giờ Việt Nam', () => {
    expect(clockVN(Date.parse('2026-11-13T03:04:05Z'))).toEqual({ hour: 10, minute: 4, second: 5 });
    expect(clockVN(Date.parse('2026-11-13T17:00:00Z'))).toEqual({ hour: 0, minute: 0, second: 0 });
  });
});
