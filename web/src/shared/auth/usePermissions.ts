// Ẩn / hiện theo quyền (plan G05.08) — server vẫn kiểm @PreAuthorize (NFR-13).
import { useStore } from '@nanostores/react';
import { useMemo } from 'react';
import { can, canAny, type PermissionCode } from './permissions';
import { $session } from './session';

export function usePermissions() {
  const session = useStore($session);
  const perms = useMemo<PermissionCode[]>(
    () => (session.status === 'authenticated' ? session.user.permissions : []),
    [session],
  );
  return useMemo(
    () => ({
      permissions: perms,
      can: (code: PermissionCode) => can(perms, code),
      canAny: (codes: readonly PermissionCode[]) => canAny(perms, codes),
      ready: session.status !== 'unknown',
    }),
    [perms, session.status],
  );
}

export function useSession() {
  return useStore($session);
}
