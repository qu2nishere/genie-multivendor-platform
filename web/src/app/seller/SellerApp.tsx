// React SPA Seller Center (plan G11.02): Providers + BrowserRouter basename="/seller" + khung. Tải
// ngữ cảnh shop (10.1) trước; tài khoản chưa có shop → mời mở shop; lỗi khác → thử lại.
import { Suspense } from 'react';
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { useSellerMe } from '@/features/seller-settings';
import { errorMessage, isApiError } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { Button, buttonClass, EmptyState, Spinner } from '@/shared/ui';
import { Providers } from '../providers';
import NotFound from './NotFound';
import { sellerRoutes } from './routes';
import { SellerLayout } from './SellerLayout';

function Loading() {
  return (
    <div className="grid place-items-center min-h-[50vh]">
      <Spinner size={28} />
    </div>
  );
}

function Shell() {
  const { t } = useT();
  const me = useSellerMe();
  if (me.isPending) return <Loading />;
  if (me.isError) {
    const noShop = isApiError(me.error) && me.error.status === 403;
    return (
      <main className="min-h-screen grid place-items-center p-6">
        <EmptyState
          icon={noShop ? 'store' : 'triangle-alert'}
          title={noShop ? t('seller.shell.no_shop.title') : t('common.state.error')}
          text={noShop ? t('seller.shell.no_shop.desc') : errorMessage(me.error, t)}
          action={
            noShop ? (
              <a href="/become-seller" className={buttonClass({})}>
                {t('seller.shell.no_shop.cta')}
              </a>
            ) : (
              <Button onClick={() => void me.refetch()}>{t('common.action.retry')}</Button>
            )
          }
        />
      </main>
    );
  }
  return (
    <SellerLayout ctx={me.data}>
      <Suspense fallback={<Loading />}>
        <Routes>
          {sellerRoutes.map((r) => (
            <Route key={r.path} path={r.path} element={<r.screen />} />
          ))}
          <Route path="*" element={<NotFound />} />
        </Routes>
      </Suspense>
    </SellerLayout>
  );
}

export default function SellerApp() {
  return (
    <Providers>
      <BrowserRouter
        basename="/seller"
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
      >
        <Shell />
      </BrowserRouter>
    </Providers>
  );
}
