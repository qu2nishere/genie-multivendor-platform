// Trang kết quả đặt hàng / thanh toán (prototype index.html OrderDone, PaymentFailed) — plan G10.27–G10.29.
// CHỈ hiển thị theo trạng thái phiên đọc từ GET /me/checkout-sessions/{id}; không tự đổi trạng thái đơn
// (TS 10.3 — VNPAY / SePay báo server qua IPN / webhook). Phiên còn chờ → hỏi lại mỗi 3 giây.
import { useEffect, useState } from 'react';
import { MoneyRail } from '@/features/payment';
import { useT } from '@/shared/hooks/useT';
import { formatDate, formatMmSs, formatVND } from '@/shared/lib/format';
import { Providers } from '@/shared/providers/Providers';
import { Button, Icon, Skeleton, Spinner } from '@/shared/ui';
import { useCheckoutSession } from '../application/useCheckout';
import { resultView, secondsLeft } from '../domain/checkout.rules';
import type { CheckoutSession } from '../domain/checkout.types';
import { PaymentFlow } from './PaymentFlow';

export interface CheckoutResultProps {
  sessionId: number | null;
}

function OrderDone({ s }: { s: CheckoutSession }) {
  const { t, locale } = useT();
  const vnd = (n: number) => formatVND(n, { locale });
  const cod = s.paymentMethod === 'COD';
  const kicker = 'text-xs text-muted uppercase tracking-[.05em] mb-1';
  return (
    <div className="max-w-[640px] mx-auto px-6 py-14 max-md:px-3 max-md:py-8">
      <div className="text-center mb-7">
        <span className="w-[72px] h-[72px] rounded-full bg-green-50 text-green-600 grid place-items-center mx-auto mb-4">
          <Icon name="circle-check" size={42} strokeWidth={2} />
        </span>
        <h1 className="m-0 mb-1.5 text-3xl font-bold text-strong">
          {t(cod ? 'payment.done.title_cod' : 'payment.done.title')}
        </h1>
        <p className="m-0 text-muted">
          {cod
            ? t('payment.done.text_cod')
            : s.orders.length > 1
              ? t('payment.done.text_split')
              : t('payment.done.text')}
        </p>
      </div>
      {!cod && (
        <div className="mb-4">
          <MoneyRail flat amount={s.summary.grandTotal} />
        </div>
      )}
      <div className="bg-surface-card border border-solid border-line rounded-lg px-7 py-6 shadow-sm mb-7 max-sm:px-4">
        <div className="flex justify-between gap-4 pb-[18px] mb-[18px] border-0 border-b border-dashed border-line-subtle">
          <div>
            <div className={kicker}>{t('payment.done.code')}</div>
            <div className="font-mono font-semibold text-[15px] text-strong">{s.code}</div>
          </div>
          <div className="text-right">
            <div className={kicker}>{t('payment.done.date')}</div>
            <div className="font-medium text-[15px] text-strong">
              {formatDate(s.createdAt, { locale })}
            </div>
          </div>
        </div>
        <ul className="list-none m-0 p-0 flex flex-col gap-3.5 mb-5">
          {s.orders.map((o) => (
            <li key={o.id} className="flex justify-between items-center gap-3 flex-wrap">
              <div className="min-w-0">
                <div className="font-mono font-medium text-sm text-strong mb-0.5">
                  {t('payment.done.order', { code: o.code })}
                </div>
                <div className="text-xs text-muted">
                  {t('payment.done.by', { shop: o.shop.name })}
                </div>
              </div>
              <div className="flex items-center gap-3">
                <span className="font-semibold text-[15px] text-strong tabular-nums">
                  {vnd(o.grandTotal)}
                </span>
                <Button size="sm" variant="outline" href={`/orders/${o.id}`}>
                  {t('payment.done.track')}
                </Button>
              </div>
            </li>
          ))}
        </ul>
        <dl className="m-0 pt-[18px] border-0 border-t border-solid border-line-subtle text-sm text-body">
          <div className="flex justify-between mb-3">
            <dt>{t('payment.done.subtotal')}</dt>
            <dd className="m-0 tabular-nums">{vnd(s.summary.subtotal)}</dd>
          </div>
          <div className="flex justify-between mb-3">
            <dt>{t('payment.done.shipping')}</dt>
            <dd className="m-0 tabular-nums">{vnd(s.summary.shippingTotal)}</dd>
          </div>
          {s.summary.discountTotal > 0 && (
            <div className="flex justify-between mb-3 text-mint-600">
              <dt>{t('payment.done.discount')}</dt>
              <dd className="m-0 tabular-nums">−{vnd(s.summary.discountTotal)}</dd>
            </div>
          )}
          {s.summary.coinDiscount > 0 && (
            <div className="flex justify-between mb-3 text-brand-600">
              <dt>{t('payment.done.points')}</dt>
              <dd className="m-0 tabular-nums">−{vnd(s.summary.coinDiscount)}</dd>
            </div>
          )}
          <div className="flex justify-between mt-3.5 pt-3.5 border-0 border-t border-dashed border-line-subtle font-bold text-lg text-price">
            <dt>{t(cod ? 'payment.done.collect' : 'cart.bar.total')}</dt>
            <dd className="m-0 tabular-nums">{vnd(s.summary.grandTotal)}</dd>
          </div>
        </dl>
      </div>
      <div className="flex gap-3 justify-center flex-wrap">
        <Button size="lg" variant="outline" href="/" iconLeft="house">
          {t('payment.done.home')}
        </Button>
        <Button size="lg" href="/orders" iconLeft="package">
          {t('payment.done.orders')}
        </Button>
      </div>
    </div>
  );
}

function PaymentFailed({ s, onRetry }: { s: CheckoutSession; onRetry: () => void }) {
  const { t } = useT();
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const i = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(i);
  }, []);
  const left = secondsLeft(Date.parse(s.reservationExpiresAt), now);
  const expired = s.status === 'EXPIRED' || left <= 0;
  return (
    <div className="max-w-[640px] mx-auto px-6 py-16 text-center max-md:px-3 max-md:py-10">
      <span className="w-[88px] h-[88px] rounded-full bg-red-50 text-red-600 grid place-items-center mx-auto mb-[22px]">
        <Icon name="circle-alert" size={52} strokeWidth={2} />
      </span>
      <h1 className="m-0 mb-2.5 text-3xl font-bold text-strong">{t('payment.failed.title')}</h1>
      <p className="m-0 mb-1.5 text-muted">
        {t('payment.failed.text')} <b>{t('payment.failed.not_charged')}</b>
      </p>
      <p className="m-0 mb-6 text-muted">
        {t('payment.failed.waiting', { code: s.orders[0]?.code ?? s.code })}
      </p>
      {expired ? (
        <p className="inline-flex items-center gap-2 bg-red-50 text-red-600 rounded-md px-4 py-2.5 text-sm mb-7">
          <Icon name="clock" size={16} className="flex-none" />
          {t('payment.failed.expired')}
        </p>
      ) : (
        <p className="inline-flex items-center gap-2 bg-amber-50 text-amber-600 rounded-md px-4 py-2.5 text-sm mb-7">
          <Icon name="lock" size={16} className="flex-none" />
          <span>
            {t('payment.failed.hold_before')} <b>{formatMmSs(left)}</b>
            {t('payment.failed.hold_after')}
          </span>
        </p>
      )}
      <div className="flex gap-3 justify-center flex-wrap">
        {!expired && (
          <>
            <Button size="lg" iconLeft="refresh-cw" onClick={onRetry}>
              {t('payment.failed.retry')}
            </Button>
            <Button size="lg" variant="outline" iconLeft="credit-card" onClick={onRetry}>
              {t('payment.failed.change')}
            </Button>
          </>
        )}
        <Button size="lg" variant="ghost" iconLeft="shopping-cart" href="/cart">
          {t('payment.failed.cart')}
        </Button>
      </div>
    </div>
  );
}

function Pending({ s, onOpen }: { s: CheckoutSession; onOpen: () => void }) {
  const { t } = useT();
  return (
    <div className="max-w-[560px] mx-auto px-6 py-16 text-center">
      <Spinner />
      <h1 className="m-0 mt-4 mb-2 text-2xl font-semibold text-strong">
        {t('payment.pending.title')}
      </h1>
      <p className="m-0 mb-6 text-muted">{t('payment.pending.text', { code: s.code })}</p>
      {s.paymentMethod === 'VIETQR' && (
        <Button iconLeft="qr-code" onClick={onOpen}>
          {t('payment.pending.show_qr')}
        </Button>
      )}
    </div>
  );
}

function ResultScreen({ sessionId }: CheckoutResultProps) {
  const { t } = useT();
  const q = useCheckoutSession(sessionId, true);
  const [flow, setFlow] = useState<'pick' | 'resume' | null>(null);

  if (sessionId == null || q.isError)
    return (
      <div className="max-w-[560px] mx-auto px-6 py-16 text-center">
        <Icon name="receipt" size={48} className="text-subtle mx-auto mb-3" />
        <h1 className="m-0 mb-2 text-2xl font-semibold text-strong">
          {t('payment.missing.title')}
        </h1>
        <p className="m-0 mb-6 text-muted">{t('payment.missing.text')}</p>
        <Button href="/orders" iconLeft="package">
          {t('payment.done.orders')}
        </Button>
      </div>
    );
  if (!q.data)
    return (
      <div className="max-w-[640px] mx-auto px-6 py-14 grid gap-4" aria-busy="true">
        <Skeleton className="h-[72px] w-[72px] mx-auto" rounded="pill" />
        <Skeleton className="h-8 w-2/3 mx-auto" />
        <Skeleton className="h-[260px]" rounded="lg" />
      </div>
    );
  const s = q.data;
  const view = resultView(s);
  return (
    <>
      {view === 'success' && <OrderDone s={s} />}
      {view === 'failed' && <PaymentFailed s={s} onRetry={() => setFlow('pick')} />}
      {view === 'pending' && <Pending s={s} onOpen={() => setFlow('resume')} />}
      {flow && <PaymentFlow session={s} pick={flow === 'pick'} onClose={() => setFlow(null)} />}
    </>
  );
}

export default function CheckoutResult(props: CheckoutResultProps) {
  return (
    <Providers>
      <ResultScreen {...props} />
    </Providers>
  );
}
