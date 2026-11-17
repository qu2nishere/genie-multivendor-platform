import { describe, expect, it } from 'vitest';
import { PERMISSIONS } from './permissions';
import { sessionUserFromMe, type MeDto } from './session';

const base: MeDto = {
  id: 20411,
  role: 'CUSTOMER',
  full_name: 'Trần Thị Mai',
  avatar_url: null,
  email: 'mai.tran@gmail.com',
  login_methods: { password: true, google: false, zalo: false },
  shop: null,
  counters: {
    cart_items: 0,
    unread_notifications: 0,
    unread_chats: 0,
    available_vouchers: 0,
    following_shops: 0,
  },
};

describe('sessionUserFromMe (GET /me → SessionUser)', () => {
  it('người mua: email, có mật khẩu, không shop', () => {
    const u = sessionUserFromMe(base);
    expect(u).toMatchObject({ id: 20411, name: 'Trần Thị Mai', email: 'mai.tran@gmail.com' });
    expect(u.roles).toEqual(['CUSTOMER']);
    expect(u.has_password).toBe(true);
    expect(u.shop_id).toBeUndefined();
    expect(u.permissions).toEqual([]);
  });

  it('chủ shop vừa là người bán vừa là người mua, lấy shop + KYC', () => {
    const u = sessionUserFromMe({
      ...base,
      role: 'VENDOR',
      login_methods: { password: false, google: true },
      shop: { id: 15, name: 'TechZone Official', logo_url: null, kyc_status: 'MANUAL_REVIEW' },
    });
    expect(u.roles).toEqual(['CUSTOMER', 'VENDOR']);
    expect(u).toMatchObject({ shop_id: 15, shop_name: 'TechZone Official', kyc_status: 'PENDING' });
    expect(u.has_password).toBe(false);
  });

  it('Admin có đủ quyền, nhân viên chờ module admin', () => {
    expect(sessionUserFromMe({ ...base, role: 'ADMIN' }).permissions).toEqual([...PERMISSIONS]);
    expect(sessionUserFromMe({ ...base, role: 'STAFF' }).permissions).toEqual([]);
  });
});
