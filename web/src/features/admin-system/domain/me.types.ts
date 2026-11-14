// Tài khoản nhân viên đang đăng nhập (API_SPEC 11.1 GET /admin/me).
import type { PermissionCode, StaffTitle } from '@/shared/auth/permissions';

export type StaffRole = 'STAFF' | 'ADMIN';
export type StaffStatus = 'ACTIVE' | 'LOCKED' | 'PENDING_ACTIVATION';

export interface AdminMe {
  id: number;
  fullName: string;
  email: string;
  avatarUrl: string | null;
  role: StaffRole;
  /** null với Admin (giao diện hiện "Toàn quyền"). */
  staffTitle: Exclude<StaffTitle, 'ADMIN'> | null;
  permissions: PermissionCode[];
  status: StaffStatus;
}
