import { describe, expect, it, vi as vitest } from 'vitest';
import { createT, hasKey, interpolate, lookup, t, translate, type Messages } from './i18n';

// Plan G04.09 — khóa lồng, nội suy, rơi về vi, khóa không tồn tại
const dicts: Record<'vi' | 'en', Messages> = {
  vi: { a: { b: { c: 'Xin chào {name}' } }, only_vi: 'Chỉ có tiếng Việt', n: 'Đã bán {count}' },
  en: { a: { b: { c: 'Hello {name}' } }, n: '{count} sold' },
};

describe('i18n', () => {
  it('tra khóa lồng nhiều cấp', () => {
    expect(lookup(dicts.vi, 'a.b.c')).toBe('Xin chào {name}');
    expect(lookup(dicts.vi, 'a.b')).toBeUndefined();
    expect(lookup(dicts.vi, 'a.b.c.d')).toBeUndefined();
  });

  it('nội suy tham số, giữ nguyên tham số thiếu', () => {
    expect(interpolate('Xin chào {name}', { name: 'Mai' })).toBe('Xin chào Mai');
    expect(interpolate('{a} và {b}', { a: 1 })).toBe('1 và {b}');
    expect(translate(dicts, 'en', 'n', { count: '1,2k' })).toBe('1,2k sold');
  });

  it('thiếu khóa ở en thì rơi về vi và cảnh báo một lần', () => {
    const warn = vitest.spyOn(console, 'warn').mockImplementation(() => undefined);
    expect(translate(dicts, 'en', 'only_vi')).toBe('Chỉ có tiếng Việt');
    translate(dicts, 'en', 'only_vi');
    expect(warn).toHaveBeenCalledTimes(1);
    warn.mockRestore();
  });

  it('khóa không tồn tại ở cả hai → trả lại chính khóa', () => {
    const warn = vitest.spyOn(console, 'warn').mockImplementation(() => undefined);
    expect(translate(dicts, 'vi', 'khong.ton.tai')).toBe('khong.ton.tai');
    warn.mockRestore();
  });

  it('dùng từ điển thật: t(), createT(), hasKey()', () => {
    expect(t('common.action.save', undefined, 'vi')).toBe('Lưu');
    expect(t('common.action.save', undefined, 'en')).toBe('Save');
    expect(createT('en')('common.label.sold', { count: 12 })).toBe('12 sold');
    expect(hasKey('common.points')).toBe(true);
    expect(hasKey('common.xu')).toBe(false);
  });

  it('từ ngữ thương hiệu đúng R11', () => {
    expect(t('common.points', undefined, 'vi')).toBe('Điểm Genie');
    expect(t('common.authentic', undefined, 'vi')).toBe('Chính hãng');
  });
});
