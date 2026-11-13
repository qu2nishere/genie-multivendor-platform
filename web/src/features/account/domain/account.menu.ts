// Mục của khu Tài khoản (prototype Account.jsx MENU) và đường dẫn tương ứng (plan 4.5).

export type AccountSection =
  | 'profile'
  | 'address'
  | 'password'
  | 'logins'
  | 'devices'
  | 'orders'
  | 'notifications'
  | 'vouchers'
  | 'points'
  | 'wishlist'
  | 'following';

export interface AccountMenuItem {
  id: AccountSection;
  icon: string;
  href: string;
}

const item = (id: AccountSection, icon: string): AccountMenuItem => ({
  id,
  icon,
  href: id === 'orders' ? '/orders' : `/account/${id}`,
});

export const ACCOUNT_MENU: { group: 'mine' | 'shopping'; items: AccountMenuItem[] }[] = [
  {
    group: 'mine',
    items: [
      item('profile', 'user'),
      item('address', 'map-pin'),
      item('password', 'shield-check'),
      item('logins', 'user-check'),
      item('devices', 'lock'),
    ],
  },
  {
    group: 'shopping',
    items: [
      item('orders', 'package'),
      item('notifications', 'bell'),
      item('vouchers', 'ticket'),
      item('points', 'star'),
      item('wishlist', 'heart'),
      item('following', 'store'),
    ],
  },
];

/** Mục ở /account/[...section] (đơn mua có route riêng /orders). */
export const ACCOUNT_SECTIONS: AccountSection[] = ACCOUNT_MENU.flatMap((g) =>
  g.items.map((i) => i.id),
).filter((id) => id !== 'orders');

export const isAccountSection = (s: string): s is AccountSection =>
  (ACCOUNT_SECTIONS as string[]).includes(s);
