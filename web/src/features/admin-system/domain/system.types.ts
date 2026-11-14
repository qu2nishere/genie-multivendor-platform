// Hệ thống: nhân viên & phân quyền, người dùng, nhật ký (API_SPEC 11.10, 11.13, 11.18; prototype
// admin/Finance.jsx Rbac / InviteStaff / RbacEditor, Users.jsx, AuditLog.jsx).
import type { PermissionCode, StaffTitle } from '@/shared/auth/permissions';
import type { StaffRole, StaffStatus } from './me.types';

// ---------- 11.18 Nhân viên ----------
export interface StaffMember {
  id: number;
  fullName: string;
  email: string;
  role: StaffRole;
  /** null với Admin ("Toàn quyền"). */
  staffTitle: Exclude<StaffTitle, 'ADMIN'> | null;
  permissions: PermissionCode[];
  status: StaffStatus;
  isSelf: boolean;
}

export interface InviteInput {
  fullName: string;
  email: string;
  staffTitle: Exclude<StaffTitle, 'ADMIN'>;
  permissions: PermissionCode[];
}

// ---------- 11.13 Người dùng ----------
export type UserTab = 'ALL' | 'CUSTOMER' | 'VENDOR' | 'LOCKED';
export type AccountState = 'ACTIVE' | 'LOCKED' | 'SUSPENDED';

export interface PlatformUser {
  id: number;
  code: string;
  fullName: string;
  shopName: string | null;
  email: string;
  phone: string;
  role: 'CUSTOMER' | 'VENDOR';
  orderCount: number;
  warningCount: number;
  status: AccountState;
  suspension: { reason: string; until: string | null } | null;
}

export type UserCounts = Record<UserTab, number>;

// ---------- 11.10 Nhật ký ----------
export type AuditCategory = 'AUTH' | 'KYC' | 'FINANCE' | 'MODERATION' | 'SECURITY' | 'CONFIG';
export type AuditResult = 'SUCCESS' | 'FAILED' | 'DENIED';

export interface AuditEntry {
  id: number;
  createdAt: string;
  actorEmail: string;
  actorRole: string;
  action: string;
  category: AuditCategory;
  target: string;
  ip: string;
  result: AuditResult;
}

export type AuditRange = 'TODAY' | '7D' | '30D' | 'CUSTOM';
