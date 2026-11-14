// API công khai của feature "admin-system" — tài khoản nhân viên đang đăng nhập (khung Admin);
// nhân viên & phân quyền, người dùng, nhật ký, tài khoản thêm ở G12 nhóm G.
export type { AdminMe, StaffRole, StaffStatus } from './domain/me.types';
export { useAdminMe, ADMIN_ME_KEY } from './application/useAdminMe';
