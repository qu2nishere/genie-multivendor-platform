// Chặn route theo quyền (plan G12.04; prototype AdminShell "denied"): thiếu mã quyền → màn 403.
// Chỉ ẩn / hiện ở client — server luôn kiểm @PreAuthorize (TS 5.4, NFR-13).
import type { ReactNode } from 'react';
import type { PermissionCode } from '@/shared/auth/permissions';
import { useT } from '@/shared/hooks/useT';
import { Icon } from '@/shared/ui';

export function Forbidden({ screen, perm }: { screen: string; perm: PermissionCode }) {
  const { t } = useT();
  return (
    <div className="grid place-items-center min-h-[420px] text-center">
      <div>
        <div className="w-[84px] h-[84px] rounded-full bg-red-50 text-red-600 grid place-items-center mx-auto mb-5">
          <Icon name="lock" size={42} />
        </div>
        <h2 className="m-0 mb-2.5 text-2xl font-bold text-strong">
          {t('admin.shell.forbidden.title')}
        </h2>
        <p className="m-0 mx-auto max-w-[420px] text-base text-muted leading-relaxed">
          {t('admin.shell.forbidden.desc_before', { screen })}
          <code className="font-mono text-sm text-body">{perm}</code>
          {t('admin.shell.forbidden.desc_after')}
        </p>
      </div>
    </div>
  );
}

export function RequirePermission({
  perm,
  perms,
  screen,
  children,
}: {
  perm: PermissionCode | undefined;
  perms: readonly PermissionCode[];
  screen: string;
  children: ReactNode;
}) {
  if (perm && !perms.includes(perm)) return <Forbidden screen={screen} perm={perm} />;
  return <>{children}</>;
}
