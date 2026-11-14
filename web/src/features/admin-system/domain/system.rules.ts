// Hàm thuần hệ thống (API_SPEC 11.10, 11.13, 11.18, 16.4).
import {
  PERMISSIONS,
  TITLE_PRESETS,
  type PermissionCode,
  type StaffTitle,
} from '@/shared/auth/permissions';
import type {
  AccountState,
  AuditCategory,
  AuditRange,
  PlatformUser,
  StaffMember,
  UserTab,
} from './system.types';

// ---------- Nhân viên ----------
export const EDIT_TITLES: Exclude<StaffTitle, 'ADMIN'>[] = [
  'MODERATOR',
  'CS_SPECIALIST',
  'FINANCE_OFFICER',
  'CUSTOM',
];

/** Email công việc phải có đuôi @genie.example (GNE-ADM-4221). */
export const STAFF_EMAIL = /^[^@\s]+@genie\.example$/i;

/** Bộ quyền mặc định của chức danh (CUSTOM giữ nguyên bộ đang chọn). */
export function presetOf(
  title: Exclude<StaffTitle, 'ADMIN'>,
  current: PermissionCode[],
): PermissionCode[] {
  return title === 'CUSTOM' ? current : [...TITLE_PRESETS[title]];
}

/** Chức danh tương ứng bộ quyền: trùng khớp bộ mặc định thì là chức danh đó, khác → CUSTOM. */
export function titleOf(
  title: Exclude<StaffTitle, 'ADMIN'>,
  perms: PermissionCode[],
): Exclude<StaffTitle, 'ADMIN'> {
  if (title === 'CUSTOM') return 'CUSTOM';
  const preset = TITLE_PRESETS[title];
  const same = preset.length === perms.length && preset.every((p) => perms.includes(p));
  return same ? title : 'CUSTOM';
}

/** Bật / tắt quyền cho nhân viên — MANAGE_RBAC không cấp cho Staff (GNE-SEC-4031). */
export function togglePerm(perms: PermissionCode[], code: PermissionCode): PermissionCode[] {
  if (code === 'MANAGE_RBAC') return perms;
  return perms.includes(code) ? perms.filter((p) => p !== code) : sortPerms([...perms, code]);
}

/** Giữ thứ tự 13 mã như bảng quyền. */
export const sortPerms = (perms: PermissionCode[]): PermissionCode[] =>
  PERMISSIONS.filter((p) => perms.includes(p));

export type StaffAction = 'resend' | 'edit' | 'lock' | 'unlock' | 'delete';

/** Thao tác: Admin / chính mình → không; chờ kích hoạt → gửi lại lời mời; khóa / mở khóa theo trạng thái. */
export function staffActions(s: Pick<StaffMember, 'role' | 'status' | 'isSelf'>): StaffAction[] {
  if (s.isSelf || s.role === 'ADMIN') return [];
  if (s.status === 'PENDING_ACTIVATION') return ['resend', 'edit', 'delete'];
  return ['edit', s.status === 'LOCKED' ? 'unlock' : 'lock', 'delete'];
}

export function matchesStaff(s: Pick<StaffMember, 'fullName' | 'email'>, q: string): boolean {
  const n = q.trim().toLowerCase();
  return !n || `${s.fullName} ${s.email}`.toLowerCase().includes(n);
}

// ---------- Người dùng ----------
export const USER_TABS: UserTab[] = ['ALL', 'CUSTOMER', 'VENDOR', 'LOCKED'];

/** Lý do đình chỉ (16.4). */
export const SUSPEND_REASONS = [
  'COUNTERFEIT_SELLER',
  'ABNORMAL_RETURNS',
  'REVIEW_SPAM',
  'FRAUD',
  'OTHER',
] as const;
export type SuspendReason = (typeof SUSPEND_REASONS)[number];

/** Thời hạn: 7 ngày, 30 ngày, vô thời hạn (null). */
export const DURATIONS = ['P7D', 'P30D', 'FOREVER'] as const;
export type Duration = (typeof DURATIONS)[number];

export const isRestricted = (s: AccountState): boolean => s !== 'ACTIVE';

export function inUserTab(u: Pick<PlatformUser, 'role' | 'status'>, tab: UserTab): boolean {
  if (tab === 'ALL') return true;
  if (tab === 'LOCKED') return isRestricted(u.status);
  return u.role === tab;
}

/** Tìm theo email, số điện thoại (bỏ khoảng trắng), tên, tên shop. */
export function matchesUser(
  u: Pick<PlatformUser, 'fullName' | 'email' | 'phone' | 'shopName'>,
  q: string,
): boolean {
  const n = q.trim().toLowerCase().replace(/\s+/g, '');
  if (!n) return true;
  return [u.fullName, u.email, u.phone, u.shopName ?? '']
    .join('|')
    .toLowerCase()
    .replace(/\s+/g, '')
    .includes(n);
}

/** Hạn đình chỉ từ thời hạn (null = vô thời hạn). */
export function suspendUntil(d: Duration, now: Date): string | null {
  if (d === 'FOREVER') return null;
  const days = d === 'P7D' ? 7 : 30;
  return new Date(now.getTime() + days * 864e5).toISOString();
}

// ---------- Nhật ký ----------
export const AUDIT_CATEGORIES: AuditCategory[] = [
  'AUTH',
  'KYC',
  'FINANCE',
  'MODERATION',
  'SECURITY',
  'CONFIG',
];

const day = (d: Date) =>
  new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Ho_Chi_Minh' }).format(d);

/** Khoảng thời gian → [from, to] dạng "YYYY-MM-DD" (giờ VN), tính lùi từ `now`. */
export function rangeOf(
  r: AuditRange,
  now: Date,
  custom: { from: string; to: string },
): { from: string | null; to: string | null } {
  const today = day(now);
  if (r === 'TODAY') return { from: today, to: today };
  if (r === '7D') return { from: day(new Date(now.getTime() - 7 * 864e5)), to: today };
  if (r === '30D') return { from: day(new Date(now.getTime() - 30 * 864e5)), to: today };
  return { from: custom.from || null, to: custom.to || null };
}

/** Màu nhãn hành động (prototype VARIANT). */
export function actionTone(
  action: string,
): 'danger' | 'success' | 'mint' | 'warning' | 'neutral' | 'primary' {
  if (/FAILED|DENIED|REJECTED|REVOKED|LOCKED|SUSPENDED/.test(action)) return 'danger';
  if (/APPROVED|RELEASED/.test(action)) return action.startsWith('PAYOUT') ? 'mint' : 'success';
  if (/REFUND/.test(action)) return 'warning';
  if (/HIDDEN/.test(action)) return 'neutral';
  return 'primary';
}
