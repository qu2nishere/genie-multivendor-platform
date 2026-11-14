import { describe, expect, it } from 'vitest';
import {
  autoReplyValid,
  canRename,
  holderMatches,
  normalizeHolder,
  toggleCarrier,
  vacationRange,
} from './settings.rules';

describe('settings.rules', () => {
  it('tên chủ tài khoản so không dấu', () => {
    expect(normalizeHolder('  Đặng  Quốc Huy ')).toBe('DANG QUOC HUY');
    expect(holderMatches('dang quoc huy', 'Đặng Quốc Huy')).toBe(true);
    expect(holderMatches('DANG QUOC HUNG', 'Đặng Quốc Huy')).toBe(false);
  });
  it('phải còn ít nhất 1 hãng', () => {
    const c = {
      GHN: true,
      GHTK: false,
      VIETTEL_POST: false,
      JNT_EXPRESS: false,
      GENIE_EXPRESS: false,
    };
    expect(toggleCarrier(c, 'GHN')).toBeNull();
    expect(toggleCarrier(c, 'GHTK')?.GHTK).toBe(true);
  });
  it('tự trả lời cần ít nhất 1 nội dung', () => {
    expect(autoReplyValid({ enabled: true, greeting: ' ', away: '' })).toBe(false);
    expect(autoReplyValid({ enabled: false, greeting: '', away: '' })).toBe(true);
  });
  it('khoảng ngày nghỉ + đổi tên', () => {
    const now = Date.parse('2026-06-08T18:00:00+07:00');
    expect(vacationRange(now)).toEqual({ min: '2026-06-08', max: '2026-07-08' });
    expect(canRename(null, now)).toBe(true);
    expect(canRename('2026-06-20T00:00:00+07:00', now)).toBe(false);
  });
});
