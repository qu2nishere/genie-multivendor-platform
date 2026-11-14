// Thanh bên Admin Portal (prototype admin/data.jsx NAV) — 18 mục, route + mã quyền theo plan mục 4.4.
// Huy hiệu lấy từ GET /admin/queues (prototype ghi cứng trong NAV) — plan G12.05.
import type { QueueKey } from '@/features/admin-dashboard';
import type { PermissionCode } from '@/shared/auth/permissions';
import type { IconName } from '@/shared/ui';

export type AdminSection =
  | 'overview'
  | 'kyc'
  | 'refunds'
  | 'reviews'
  | 'withdrawals'
  | 'cod'
  | 'commission'
  | 'ledger'
  | 'audit'
  | 'catalog'
  | 'product_moderation'
  | 'users'
  | 'vouchers'
  | 'flash_sale'
  | 'banners'
  | 'notifications'
  | 'rbac'
  | 'account';

export interface AdminNavItem {
  section: AdminSection;
  /** Đường dẫn trong SPA (basename /admin). */
  to: string;
  icon: IconName;
  /** Không có = mọi nhân viên (Tài khoản). */
  perm?: PermissionCode;
  badge?: QueueKey;
}

export const NAV: AdminNavItem[] = [
  { section: 'overview', to: '/', icon: 'layout-dashboard', perm: 'VIEW_DASHBOARD' },
  { section: 'kyc', to: '/kyc', icon: 'user-check', perm: 'REVIEW_KYC', badge: 'kycPending' },
  {
    section: 'refunds',
    to: '/refunds',
    icon: 'undo-2',
    perm: 'HANDLE_DISPUTE',
    badge: 'disputesOpen',
  },
  { section: 'reviews', to: '/reviews', icon: 'star', perm: 'MODERATE_REVIEW' },
  {
    section: 'withdrawals',
    to: '/withdrawals',
    icon: 'hand-coins',
    perm: 'PROCESS_PAYOUT',
    badge: 'withdrawalsPending',
  },
  {
    section: 'cod',
    to: '/cod-reconciliation',
    icon: 'receipt',
    perm: 'PROCESS_PAYOUT',
    badge: 'codRemittancesWaiting',
  },
  { section: 'commission', to: '/commission', icon: 'percent', perm: 'UPDATE_COMMISSION' },
  { section: 'ledger', to: '/ledger', icon: 'scale', perm: 'VIEW_FINANCE' },
  { section: 'audit', to: '/audit-logs', icon: 'file-text', perm: 'VIEW_AUDIT_LOG' },
  { section: 'catalog', to: '/catalog', icon: 'boxes', perm: 'MANAGE_CATALOG' },
  {
    section: 'product_moderation',
    to: '/product-moderation',
    icon: 'clipboard-check',
    perm: 'MANAGE_CATALOG',
    badge: 'productsPending',
  },
  { section: 'users', to: '/users', icon: 'user', perm: 'MANAGE_USER' },
  { section: 'vouchers', to: '/vouchers', icon: 'ticket', perm: 'MANAGE_VOUCHER' },
  { section: 'flash_sale', to: '/flash-sales', icon: 'zap', perm: 'MANAGE_VOUCHER' },
  { section: 'banners', to: '/banners', icon: 'image', perm: 'MANAGE_VOUCHER' },
  {
    section: 'notifications',
    to: '/notifications',
    icon: 'bell',
    perm: 'MANAGE_PLATFORM_NOTIFICATION',
  },
  { section: 'rbac', to: '/staff', icon: 'shield-check', perm: 'MANAGE_RBAC' },
  { section: 'account', to: '/account', icon: 'settings' },
];

/** Đoạn đầu đường dẫn → mục nav (null = không có màn, 404 nội bộ). */
const SEGMENT: Record<string, AdminSection> = Object.fromEntries(
  NAV.map((n) => [n.to.replace(/^\//, ''), n.section]),
);

export function navOf(pathname: string): AdminNavItem | null {
  const seg = pathname.replace(/^\/+/, '').split('/')[0] ?? '';
  const section = SEGMENT[seg];
  return section ? (NAV.find((n) => n.section === section) ?? null) : null;
}

/** Mục được phép: không yêu cầu quyền hoặc có mã quyền (chỉ ẩn / hiện — server mới kiểm, TS 5.4). */
export const allowed = (n: AdminNavItem, perms: readonly PermissionCode[]): boolean =>
  !n.perm || perms.includes(n.perm);

/** Trang mở đầu: mục đầu tiên người dùng có quyền (Tổng quan với Admin). */
export const homeOf = (perms: readonly PermissionCode[]): string =>
  NAV.find((n) => allowed(n, perms))?.to ?? '/account';
