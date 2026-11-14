import { isPermission } from '@/shared/auth/permissions';
import type { AdminMe } from '../domain/me.types';
import type { AdminMeDto } from './me.dto';

export const toAdminMe = (d: AdminMeDto): AdminMe => ({
  id: d.id,
  fullName: d.full_name,
  email: d.email,
  avatarUrl: d.avatar_url,
  role: d.role,
  staffTitle: d.staff_title,
  // Mã quyền lạ (server thêm sau) bị bỏ qua thay vì làm hỏng kiểu
  permissions: d.permissions.filter(isPermission),
  status: d.status,
});
