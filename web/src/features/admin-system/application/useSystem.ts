// Nhân viên, người dùng, nhật ký, tài khoản (TanStack Query). Thao tác làm mới mọi truy vấn ['admin', …].
import {
  keepPreviousData,
  useInfiniteQuery,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';
import type { PermissionCode, StaffTitle } from '@/shared/auth/permissions';
import type { Duration, SuspendReason } from '../domain/system.rules';
import type { InviteInput, UserTab } from '../domain/system.types';
import {
  auditNow,
  deleteStaff,
  inviteStaff,
  listAllAuditLogs,
  listAuditLogs,
  listStaff,
  listUsers,
  lockStaff,
  reactivateUser,
  resendInvitation,
  suspendUser,
  unlockStaff,
  updateAdminMe,
  updateStaffPermissions,
  type AuditQuery,
} from '../infrastructure/system.api';

const KEY = ['admin', 'system'] as const;

function useAdminMutation<A, R>(fn: (a: A) => Promise<R>) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: fn,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin'] }),
  });
}

// Nhân viên
export const useStaff = (q: string) =>
  useQuery({
    queryKey: [...KEY, 'staff', q],
    queryFn: () => listStaff(q),
    placeholderData: keepPreviousData,
  });
export const useInviteStaff = () => useAdminMutation((v: InviteInput) => inviteStaff(v));
export const useResendInvitation = () => useAdminMutation((id: number) => resendInvitation(id));
export const useUpdatePermissions = () =>
  useAdminMutation(
    (a: { id: number; title: Exclude<StaffTitle, 'ADMIN'>; permissions: PermissionCode[] }) =>
      updateStaffPermissions(a.id, a.title, a.permissions),
  );
export const useLockStaff = () =>
  useAdminMutation((a: { id: number; reason: string }) => lockStaff(a.id, a.reason));
export const useUnlockStaff = () => useAdminMutation((id: number) => unlockStaff(id));
export const useDeleteStaff = () => useAdminMutation((id: number) => deleteStaff(id));

// Người dùng
export const useUsers = (tab: UserTab, q: string) =>
  useQuery({
    queryKey: [...KEY, 'users', tab, q],
    queryFn: () => listUsers(tab, q),
    placeholderData: keepPreviousData,
  });
export const useSuspendUser = () =>
  useAdminMutation(
    (a: {
      id: number;
      reason: SuspendReason;
      note: string | null;
      duration: Duration;
      label: string;
    }) => suspendUser(a.id, a.reason, a.note, a.duration, a.label),
  );
export const useReactivateUser = () =>
  useAdminMutation((a: { id: number; reason: string }) => reactivateUser(a.id, a.reason));

// Nhật ký
export const useAuditLogs = (f: AuditQuery) =>
  useInfiniteQuery({
    queryKey: [...KEY, 'audit', f],
    queryFn: ({ pageParam }) => listAuditLogs(f, pageParam),
    initialPageParam: null as string | null,
    getNextPageParam: (last) => (last.has_more ? last.next_cursor : null),
    placeholderData: keepPreviousData,
  });
export { listAllAuditLogs, auditNow };

// Tài khoản
export const useUpdateAdminMe = () =>
  useAdminMutation(
    (p: { fullName?: string; avatarObjectKey?: string; avatarPreview?: string | null }) =>
      updateAdminMe(p),
  );
