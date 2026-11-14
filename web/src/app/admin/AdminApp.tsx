// React SPA Admin Portal (plan G12.02): Providers + BrowserRouter basename="/admin" + khung. Tải
// tài khoản nhân viên (GET /admin/me) trước; không phải nhân viên (403) → màn từ chối; lỗi khác → thử lại.
import { Suspense } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { useAdminMe } from '@/features/admin-system';
import { errorMessage, isApiError } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { Button, buttonClass, EmptyState, Spinner } from '@/shared/ui';
import { Providers } from '../providers';
import { AdminLayout } from './AdminLayout';
import { allowed, homeOf, NAV } from './nav';
import NotFound from './NotFound';
import { RequirePermission } from './RequirePermission';
import { adminRoutes } from './routes';

function Loading() {
  return (
    <div className="grid place-items-center min-h-[50vh]">
      <Spinner size={28} />
    </div>
  );
}

function Shell() {
  const { t } = useT();
  const me = useAdminMe();
  if (me.isPending) return <Loading />;
  if (me.isError) {
    const denied = isApiError(me.error) && me.error.status === 403;
    return (
      <main className="min-h-screen grid place-items-center p-6">
        <EmptyState
          icon={denied ? 'lock' : 'triangle-alert'}
          title={denied ? t('admin.shell.not_staff.title') : t('common.state.error')}
          text={denied ? t('admin.shell.not_staff.desc') : errorMessage(me.error, t)}
          action={
            denied ? (
              <a href="/" className={buttonClass({})}>
                {t('admin.shell.not_staff.cta')}
              </a>
            ) : (
              <Button onClick={() => void me.refetch()}>{t('common.action.retry')}</Button>
            )
          }
        />
      </main>
    );
  }
  const perms = me.data.permissions;
  const home = homeOf(perms);
  const overview = NAV[0];
  // Không có quyền xem Tổng quan → "/" chuyển về mục đầu tiên được phép
  const redirectHome = !!overview && !allowed(overview, perms) && home !== '/';
  return (
    <AdminLayout me={me.data}>
      <Suspense fallback={<Loading />}>
        <Routes>
          {adminRoutes.map((r) => {
            if (r.path === '/' && redirectHome)
              return <Route key={r.path} path="/" element={<Navigate to={home} replace />} />;
            const nav = NAV.find((n) => n.section === r.section);
            return (
              <Route
                key={r.path}
                path={r.path}
                element={
                  <RequirePermission
                    perm={nav?.perm}
                    perms={perms}
                    screen={t(`admin.shell.nav.${r.section}`)}
                  >
                    <r.screen />
                  </RequirePermission>
                }
              />
            );
          })}
          <Route path="*" element={<NotFound home={home} />} />
        </Routes>
      </Suspense>
    </AdminLayout>
  );
}

export default function AdminApp() {
  return (
    <Providers>
      <BrowserRouter
        basename="/admin"
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
      >
        <Shell />
      </BrowserRouter>
    </Providers>
  );
}
