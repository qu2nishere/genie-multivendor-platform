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
  /** Hạn lời mời — chỉ có khi PENDING_ACTIVATION / INVITATION_EXPIRED. */
  invitationExpiresAt: string | null;
  isSelf: boolean;
}

export interface InviteInput {
  fullName: string;
  email: string;
  staffTitle: Exclude<StaffTitle, 'ADMIN'>;
  permissions: PermissionCode[];
}

/** Vai trò tài khoản đang giữ email (OTHER: tài khoản đang chờ xóa / không rõ vai trò). */
export type EmailOwner = 'CUSTOMER' | 'VENDOR' | 'STAFF' | 'ADMIN' | 'OTHER';

/** Kết quả kiểm tra email trước khi mời — GET /admin/staff/email-check (cảnh báo tức thời ở form). */
export interface StaffEmailCheck {
  email: string;
  available: boolean;
  reason: 'INVALID' | 'DOMAIN' | 'TAKEN' | null;
  takenBy: EmailOwner | null;
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
  id: string;
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
