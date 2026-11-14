// API công khai của feature "admin-system" — tài khoản nhân viên đang đăng nhập (khung Admin);
// nhân viên & phân quyền (/admin/staff), người dùng (/admin/users), nhật ký (/admin/audit-logs), tài khoản (/admin/account).
export type { AdminMe, StaffRole, StaffStatus } from './domain/me.types';
export { useAdminMe, ADMIN_ME_KEY } from './application/useAdminMe';
export type { StaffMember, PlatformUser, AuditEntry } from './domain/system.types';
export { default as StaffPage } from './ui/StaffPage';
export { default as UsersPage } from './ui/UsersPage';
export { default as AuditLogPage } from './ui/AuditLogPage';
export { default as AccountAdminPage } from './ui/AccountAdminPage';
