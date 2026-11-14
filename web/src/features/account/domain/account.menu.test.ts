import { describe, expect, it } from 'vitest';
import { ACCOUNT_MENU, ACCOUNT_SECTIONS, isAccountSection } from './account.menu';

describe('menu khu Tài khoản', () => {
  it('Đơn mua có route riêng /orders, các mục khác ở /account/<mục>', () => {
    const all = ACCOUNT_MENU.flatMap((g) => g.items);
    expect(all.find((i) => i.id === 'orders')?.href).toBe('/orders');
    expect(all.find((i) => i.id === 'address')?.href).toBe('/account/address');
  });
  it('mục của /account/[...section] không gồm orders', () => {
    expect(ACCOUNT_SECTIONS).not.toContain('orders');
    expect(ACCOUNT_SECTIONS).toHaveLength(10);
  });
  it('isAccountSection nhận đúng mục, từ chối mục lạ (→ 404)', () => {
    expect(isAccountSection('points')).toBe(true);
    expect(isAccountSection('orders')).toBe(false);
    expect(isAccountSection('khong-co')).toBe(false);
  });
});
