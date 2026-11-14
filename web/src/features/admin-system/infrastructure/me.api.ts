// Khung Admin: GET /admin/me (11.1) — không cần mã quyền.
import { api } from '@/shared/api/http';
import { mockDelay, USE_MOCK } from '@/shared/api/mock';
import { currentUser, MOCK_STAFF } from '@/shared/auth/session';
import type { AdminMe } from '../domain/me.types';
import type { AdminMeDto } from './me.dto';
import { toAdminMe } from './me.mapper';

export async function getAdminMe(): Promise<AdminMe> {
  if (USE_MOCK) {
    await mockDelay(150);
    const u = currentUser() ?? MOCK_STAFF;
    return toAdminMe({
      id: u.id,
      full_name: u.name,
      email: u.email,
      avatar_url: u.avatar_url ?? null,
      role: u.roles.includes('ADMIN') ? 'ADMIN' : 'STAFF',
      staff_title: null,
      permissions: u.permissions,
      status: 'ACTIVE',
    });
  }
  return toAdminMe(await api.get<AdminMeDto>('/admin/me'));
}
