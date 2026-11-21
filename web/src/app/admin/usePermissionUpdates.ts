// Quyền nhân viên đổi trong lúc đang làm việc (plan B1 D19): phiên giữ nguyên, kênh realtime báo → tải lại
// /admin/me và dữ liệu Admin; đang ở màn vừa mất quyền thì về trang chủ được phép, kèm thông báo.
import { useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { ADMIN_ME_KEY, type AdminMe } from '@/features/admin-system';
import { useT } from '@/shared/hooks/useT';
import { PERMISSIONS_CHANGED_EVENT } from '@/shared/realtime/accountChannel';
import { toast } from '@/shared/ui';
import { allowed, homeOf, NAV } from './nav';

/** Mục thanh bên đang mở theo đường dẫn SPA (`/` = Tổng quan). */
export function sectionOf(pathname: string) {
  return (
    NAV.find((n) => n.to !== '/' && (pathname === n.to || pathname.startsWith(`${n.to}/`))) ??
    NAV.find((n) => n.to === '/' && pathname === '/')
  );
}

export function usePermissionUpdates(): void {
  const qc = useQueryClient();
  const { t } = useT();
  const navigate = useNavigate();
  const { pathname } = useLocation();

  useEffect(() => {
    const onChange = () => {
      void qc.invalidateQueries({ queryKey: ['admin'] }).then(() => {
        const me = qc.getQueryData<AdminMe>(ADMIN_ME_KEY);
        if (!me) return;
        const here = sectionOf(pathname);
        if (here && !allowed(here, me.permissions)) {
          navigate(homeOf(me.permissions), { replace: true });
          toast.info(
            t('admin.shell.permissions_lost', { screen: t(`admin.shell.nav.${here.section}`) }),
          );
        } else {
          toast.info(t('admin.shell.permissions_changed'));
        }
      });
    };
    window.addEventListener(PERMISSIONS_CHANGED_EVENT, onChange);
    return () => window.removeEventListener(PERMISSIONS_CHANGED_EVENT, onChange);
  }, [qc, t, navigate, pathname]);
}
