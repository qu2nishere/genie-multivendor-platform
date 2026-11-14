import { describe, expect, it } from 'vitest';
import { PERMISSIONS, TITLE_PRESETS } from '@/shared/auth/permissions';
import { allowed, homeOf, NAV, navOf } from './nav';

describe('admin nav', () => {
  it('Admin thấy đủ 18 mục', () => {
    expect(NAV.filter((n) => allowed(n, PERMISSIONS))).toHaveLength(18);
  });
  it('CS Specialist chỉ thấy mục đúng quyền + Tài khoản', () => {
    const cs = NAV.filter((n) => allowed(n, TITLE_PRESETS.CS_SPECIALIST)).map((n) => n.section);
    expect(cs).toEqual(['overview', 'refunds', 'reviews', 'account']);
  });
  it('trang đầu là mục đầu tiên được phép', () => {
    expect(homeOf(PERMISSIONS)).toBe('/');
    expect(homeOf(['PROCESS_PAYOUT'])).toBe('/withdrawals');
    expect(homeOf([])).toBe('/account');
  });
  it('đường dẫn → mục nav', () => {
    expect(navOf('/')?.section).toBe('overview');
    expect(navOf('/product-moderation/1283')?.section).toBe('product_moderation');
    expect(navOf('/cod-reconciliation')?.perm).toBe('PROCESS_PAYOUT');
    expect(navOf('/khong-co')).toBeNull();
  });
});
