// Tài khoản nhân viên đang đăng nhập (API_SPEC 11.1 GET /admin/me).
import type { PermissionCode, StaffTitle } from '@/shared/auth/permissions';

export type StaffRole = 'STAFF' | 'ADMIN';
/** API_SPEC 16.2 `StaffStatus` — INVITATION_EXPIRED: lời mời quá 48 giờ chưa kích hoạt (plan B1 D5). */
export type StaffStatus = 'ACTIVE' | 'LOCKED' | 'PENDING_ACTIVATION' | 'INVITATION_EXPIRED';

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
