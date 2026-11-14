// GET /admin/queues (11.1). Mock: đếm từ kho dùng chung shared/api/mock/admin.ts và bỏ khóa người
// đang đăng nhập không có quyền (như server).
import { api } from '@/shared/api/http';
import { mockDelay, USE_MOCK } from '@/shared/api/mock';
import { mockAdminQueues } from '@/shared/api/mock/admin';
import { can, type PermissionCode } from '@/shared/auth/permissions';
import { currentUser } from '@/shared/auth/session';
import type { AdminQueues } from '../domain/queues.types';
import type { AdminQueuesDto } from './queues.dto';
import { toAdminQueues } from './queues.mapper';

const NEED: Record<keyof AdminQueuesDto, PermissionCode> = {
  kyc_pending: 'REVIEW_KYC',
  products_pending: 'MANAGE_CATALOG',
  disputes_open: 'HANDLE_DISPUTE',
  withdrawals_pending: 'PROCESS_PAYOUT',
  withdrawals_blocked: 'PROCESS_PAYOUT',
  cod_remittances_waiting: 'PROCESS_PAYOUT',
};

export async function getAdminQueues(): Promise<AdminQueues> {
  if (USE_MOCK) {
    await mockDelay(120);
    const perms = currentUser()?.permissions ?? [];
    const all = mockAdminQueues();
    const dto: AdminQueuesDto = {};
    for (const k of Object.keys(all) as (keyof AdminQueuesDto)[]) {
      if (can(perms, NEED[k])) dto[k] = all[k];
    }
    return toAdminQueues(dto);
  }
  return toAdminQueues(await api.get<AdminQueuesDto>('/admin/queues'));
}
