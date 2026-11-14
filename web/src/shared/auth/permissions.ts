// 13 mã quyền + preset theo chức danh (plan G05.08; Document_Vi 3.6; prototype admin/data.jsx PERMS,
// ROLE_PERMS). Frontend chỉ dùng để ẩn / hiện — server luôn kiểm @PreAuthorize (NFR-13).

export const PERMISSIONS = [
  'VIEW_DASHBOARD',
  'REVIEW_KYC',
  'MANAGE_CATALOG',
  'MODERATE_REVIEW',
  'HANDLE_DISPUTE',
  'PROCESS_PAYOUT',
  'UPDATE_COMMISSION',
  'VIEW_FINANCE',
  'MANAGE_VOUCHER',
  'MANAGE_PLATFORM_NOTIFICATION',
  'VIEW_AUDIT_LOG',
  'MANAGE_USER',
  'MANAGE_RBAC',
] as const;

export type PermissionCode = (typeof PERMISSIONS)[number];

export const isPermission = (v: unknown): v is PermissionCode =>
  (PERMISSIONS as readonly unknown[]).includes(v);

/** Chức danh nhân viên; CUSTOM = Admin chỉnh tay từng quyền. */
export const STAFF_TITLES = [
  'ADMIN',
  'MODERATOR',
  'CS_SPECIALIST',
  'FINANCE_OFFICER',
  'CUSTOM',
] as const;
export type StaffTitle = (typeof STAFF_TITLES)[number];

/** Bộ quyền mặc định theo chức danh (prototype ROLE_PERMS). */
export const TITLE_PRESETS: Record<Exclude<StaffTitle, 'CUSTOM'>, readonly PermissionCode[]> = {
  ADMIN: PERMISSIONS,
  MODERATOR: ['VIEW_DASHBOARD', 'REVIEW_KYC', 'MANAGE_CATALOG', 'MODERATE_REVIEW'],
  CS_SPECIALIST: ['VIEW_DASHBOARD', 'MODERATE_REVIEW', 'HANDLE_DISPUTE'],
  FINANCE_OFFICER: [
    'VIEW_DASHBOARD',
    'PROCESS_PAYOUT',
    'UPDATE_COMMISSION',
    'VIEW_FINANCE',
    'MANAGE_VOUCHER',
  ],
};

export function presetFor(title: StaffTitle): PermissionCode[] {
  return title === 'CUSTOM' ? [] : [...TITLE_PRESETS[title]];
}

export type Role = 'CUSTOMER' | 'VENDOR' | 'STAFF' | 'ADMIN';

/** MANAGE_RBAC chỉ dành cho ADMIN, không cấp cho STAFF (TS 9, GNE-SEC-4031). */
export function isGrantable(code: PermissionCode, role: Role): boolean {
  return code !== 'MANAGE_RBAC' || role === 'ADMIN';
}

/** Cảnh báo tách nhiệm vụ: cùng lúc có chi tiền và đổi hoa hồng (API_SPEC SEGREGATION_OF_DUTIES). */
export function segregationWarning(perms: readonly PermissionCode[]): boolean {
  return perms.includes('PROCESS_PAYOUT') && perms.includes('UPDATE_COMMISSION');
}

export function can(perms: readonly PermissionCode[] | undefined, code: PermissionCode): boolean {
  return !!perms && perms.includes(code);
}

export function canAny(
  perms: readonly PermissionCode[] | undefined,
  codes: readonly PermissionCode[],
): boolean {
  return codes.length === 0 || codes.some((c) => can(perms, c));
}
