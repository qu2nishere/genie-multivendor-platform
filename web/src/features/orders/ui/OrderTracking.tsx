// Theo dõi đơn (prototype OrderTracking.jsx) — plan G10.36–G10.38, island client:load: thanh 7 bước,
// thẻ ĐVVC + mã vận đơn, ghi chú giữ tiền / ngày giải ngân, hành trình (mới nhất trước), địa chỉ,
// sản phẩm + tiền, nút theo trạng thái (Đã nhận hàng, Trả hàng, Đánh giá, Mua lại, Chat, Hủy).
import { useEffect, useState } from 'react';
import { formatPhone } from '@/features/account';
import { RefundBlock, ReturnModal } from '@/features/refunds';
import { ReviewModal } from '@/features/reviews';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatDateTime, formatVND } from '@/shared/lib/format';
import { Providers } from '@/shared/providers/Providers';
import { Badge, Button, Icon, Seal, Skeleton, VendorChip, type IconName } from '@/shared/ui';
import { useOrder } from '../application/useOrders';
import { STEPS, stepOf } from '../domain/order.rules';
import type { OrderDetail } from '../domain/order.types';
import { chatHref, useOrderHandlers, type OrderHandlers } from './OrderActions';
import { OrderStatus } from './OrdersPanel';

export interface OrderTrackingProps {
  id: number;
}

const EV_ICON: Record<string, IconName> = {
  PAID: 'wallet',
  PREPARING: 'package',
  AWAITING_PICKUP: 'package',
  PICKED_UP: 'truck',
  IN_TRANSIT: 'truck',
  OUT_FOR_DELIVERY: 'truck',
  DELIVERED: 'circle-check',
};
const card =
  'bg-surface-card rounded-lg border border-solid border-line-subtle shadow-sm p-5 max-sm:p-4';

function Stepper({ o }: { o: OrderDetail }) {
  const { t } = useT();
  const step = stepOf(o.status);
  const label = (key: string) =>
    key === 'paid' && o.paymentMethod === 'COD'
      ? t('orders.step.paid_cod')
      : t(`orders.step.${key}`);
  return (
    <section className={cn(card, 'px-7 pt-7 pb-6')} aria-label={t('orders.step.label')}>
      <ol className="tr-steps flex relative list-none m-0 p-0">
        {STEPS.map((s, i) => {
          const done = i < step;
          const cur = i === step;
          return (
            <li
              key={s.key}
              aria-current={cur ? 'step' : undefined}
              className="flex-1 flex flex-col items-center relative z-[1]"
            >
              {i < STEPS.length - 1 && (
                <div
                  aria-hidden="true"
                  className={cn(
                    'absolute top-[22px] left-1/2 w-full h-[3px]',
                    i < step ? 'bg-mint-500' : 'bg-gray-200',
                  )}
                />
              )}
              <div
                className={cn(
                  'w-[46px] h-[46px] rounded-full grid place-items-center relative z-[2]',
                  done
                    ? 'bg-mint-500 text-inverse'
                    : cur
                      ? 'bg-brand-500 text-inverse shadow-sm'
                      : 'bg-surface-card text-subtle border-2 border-solid border-gray-200',
                )}
              >
                <Icon
                  name={done ? 'check' : (s.icon as IconName)}
                  size={done ? 20 : 19}
                  strokeWidth={done ? 3 : 2}
                />
              </div>
              <div
                className={cn(
                  'mt-2.5 text-center text-sm',
                  cur ? 'font-bold text-strong' : done ? 'text-body' : 'text-subtle',
                )}
              >
                {label(s.key)}
              </div>
            </li>
          );
        })}
      </ol>
      <p className="tr-steps__now">
        {t('orders.step.now', { n: Math.min(step + 1, STEPS.length), count: STEPS.length })}{' '}
        <b>{label(STEPS[Math.min(step, STEPS.length - 1)].key)}</b>
      </p>
    </section>
  );
}

function EscrowNote({ o }: { o: OrderDetail }) {
  const { t, locale } = useT();
  const done = o.status === 'COMPLETED';
  const text = done
    ? t('orders.escrow.done')
    : o.paymentMethod === 'COD'
      ? t('orders.escrow.cod')
      : o.status === 'DELIVERED'
        ? t('orders.escrow.delivered', {
            date: o.escrowReleaseAt
              ? formatDateTime(o.escrowReleaseAt, { locale })
              : t('orders.escrow.after_7'),
          })
        : t('orders.escrow.holding');
  return (
    <div
      className={cn(
        'flex gap-[11px] px-4 py-3.5 rounded-md border border-solid',
        done ? 'bg-mint-50 border-mint-100' : 'bg-blue-50 border-blue-200',
      )}
    >
      <Icon
        name={done ? 'badge-check' : 'shield-check'}
        size={20}
        className={cn('flex-none mt-px', done ? 'text-mint-600' : 'text-blue-600')}
      />
      <p className="m-0 text-sm text-body leading-normal">{text}</p>
    </div>
  );
}

function Carrier({ o }: { o: OrderDetail }) {
  const { t, locale } = useT();
  const [copied, setCopied] = useState(false);
  const sh = o.shipment;
  if (!sh) return null;
  const copy = () => {
    void navigator.clipboard?.writeText(sh.trackingCode).catch(() => undefined);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1500);
  };
  const when = sh.deliveredAt ?? sh.estimatedDeliveryAt;
  return (
    <section className={card} aria-label={t('orders.carrier.label')}>
      <div className="flex items-center gap-3 flex-wrap">
        <span className="w-[46px] h-[46px] rounded-md bg-ink-900 text-inverse grid place-items-center flex-none">
          <Icon name="truck" size={22} />
        </span>
        <div className="flex-1 min-w-0">
          <div className="font-bold text-md text-strong">{t(`orders.carrier.${sh.carrier}`)}</div>
          <div className="inline-flex items-center gap-2 mt-0.5 flex-wrap">
            <span className="text-sm text-muted">{t('orders.carrier.code')}</span>
            <span className="font-mono text-[13px] text-body">{sh.trackingCode}</span>
            <button
              type="button"
              onClick={copy}
              className="inline-flex items-center gap-1 border-0 bg-transparent text-brand-600 font-semibold text-xs cursor-pointer min-h-8"
            >
              <Icon name={copied ? 'check' : 'copy'} size={13} />
              {t(copied ? 'common.action.copied' : 'common.action.copy')}
            </button>
          </div>
        </div>
        {when && (
          <div className="text-right">
            <div className="text-xs text-muted">
              {t(sh.deliveredAt ? 'orders.carrier.delivered_at' : 'orders.carrier.eta')}
            </div>
            <div
              className={cn('font-bold text-sm', sh.deliveredAt ? 'text-mint-600' : 'text-strong')}
            >
              {formatDateTime(when, { locale })}
            </div>
          </div>
        )}
      </div>
    </section>
  );
}

function Timeline({ o }: { o: OrderDetail }) {
  const { t, locale } = useT();
  const evs = o.shipment?.events ?? [];
  if (!evs.length) return null;
  return (
    <section className={card} aria-labelledby="journey">
      <div className="flex items-center gap-2 mb-4">
        <Icon name="map-pin" size={19} className="text-brand-500" />
        <h2 id="journey" className="m-0 text-xl font-semibold text-strong">
          {t('orders.journey')}
        </h2>
      </div>
      <ol className="list-none m-0 p-0">
        {evs.map((e, i) => {
          const first = i === 0;
          return (
            <li key={`${e.status}-${e.occurredAt}`} className="flex gap-3.5">
              <div className="flex flex-col items-center flex-none">
                <span
                  className={cn(
                    'w-8 h-8 rounded-full grid place-items-center border-2 border-solid',
                    first
                      ? 'bg-mint-50 text-mint-600 border-mint-500'
                      : 'bg-surface-sunken text-subtle border-line',
                  )}
                >
                  <Icon name={EV_ICON[e.status] ?? 'package'} size={15} />
                </span>
                {i < evs.length - 1 && <span className="w-0.5 flex-1 min-h-[26px] bg-line" />}
              </div>
              <div className="pb-5 flex-1">
                <div className={cn('text-sm', first ? 'font-semibold text-strong' : 'text-body')}>
                  {e.description}
                </div>
                <div className="flex gap-3 mt-0.5 text-xs text-muted flex-wrap">
                  <span className="inline-flex items-center gap-1">
                    <Icon name="clock" size={12} />
                    {formatDateTime(e.occurredAt, { locale })}
                  </span>
                  {e.location && (
                    <span className="inline-flex items-center gap-1">
                      <Icon name="map-pin" size={12} />
                      {e.location}
                    </span>
                  )}
                </div>
              </div>
            </li>
          );
        })}
      </ol>
    </section>
  );
}

function Side({
  o,
  h,
  onReturn,
  onReview,
}: {
  o: OrderDetail;
  h: OrderHandlers;
  onReturn: () => void;
  onReview: () => void;
}) {
  const { t, locale } = useT();
  const vnd = (n: number) => formatVND(n, { locale });
  const a = new Set(o.actions);
  const row = (k: string, v: string, green = false) => (
    <div className="flex justify-between text-sm">
      <dt className="text-muted">{k}</dt>
      <dd className={cn('m-0 tabular-nums', green ? 'text-mint-600' : 'text-body')}>{v}</dd>
    </div>
  );
  return (
    <div className="flex flex-col gap-[18px]">
      <section className={card} aria-labelledby="ship-to">
        <div className="flex items-center gap-2 mb-2.5">
          <Icon name="map-pin" size={18} className="text-brand-500" />
          <h2 id="ship-to" className="m-0 font-bold text-sm text-strong">
            {t('checkout.address.title')}
          </h2>
        </div>
        <div className="font-semibold text-sm text-strong">
          {o.address.recipient} · {formatPhone(o.address.phone)}
        </div>
        <p className="m-0 text-sm text-muted mt-[3px] leading-normal">{o.address.full}</p>
      </section>

      <section className={card} aria-label={o.shop.name}>
        <div className="flex items-center gap-2.5 pb-3 mb-1 border-0 border-b border-solid border-line-subtle">
          <span className="tr-chip">
            <Seal id={o.shop.id} name={o.shop.name} size={30} />
            <VendorChip
              name={o.shop.name}
              authentic={o.shop.authentic}
              href={`/shop/${o.shop.id}`}
            />
          </span>
          {a.has('CHAT') && (
            <Button
              size="sm"
              variant="ghost"
              iconLeft="message-circle"
              className="ml-auto"
              href={chatHref(o)}
            >
              {t('common.action.chat')}
            </Button>
          )}
        </div>
        <ul className="list-none m-0 p-0">
          {o.items.map((it) => (
            <li
              key={it.id}
              className="flex gap-3 py-[11px] border-0 border-b border-solid border-line-subtle last:border-b-0"
            >
              <img
                src={it.image}
                alt=""
                width={52}
                height={52}
                loading="lazy"
                className="w-[52px] h-[52px] rounded-sm object-cover flex-none border border-solid border-line-subtle"
              />
              <div className="flex-1 min-w-0">
                <a
                  href={`/p/${it.productSlug}-${it.productId}`}
                  className="text-sm text-body line-clamp-2 no-underline hover:underline"
                >
                  {it.title}
                </a>
                <div className="text-xs text-muted mt-0.5">
                  {it.variantLabel} · x{it.qty}
                </div>
              </div>
              <div className="font-semibold text-sm text-price flex-none tabular-nums">
                {vnd(it.price)}
              </div>
            </li>
          ))}
        </ul>
        <dl className="m-0 flex flex-col gap-1.5 pt-3">
          {row(t('payment.done.subtotal'), vnd(o.subtotal))}
          {row(t('payment.done.shipping'), vnd(o.shippingFee))}
          {o.discountTotal > 0 &&
            row(t('orders.detail.discount'), `-${vnd(o.discountTotal)}`, true)}
          {o.coinDiscount > 0 && row(t('payment.done.points'), `-${vnd(o.coinDiscount)}`, true)}
          <div className="flex justify-between items-baseline pt-2 mt-1 border-0 border-t border-dashed border-line-strong">
            <dt className="font-semibold text-sm text-strong">{t('orders.card.total')}</dt>
            <dd className="m-0 font-extrabold text-[22px] text-price tabular-nums">
              {vnd(o.grandTotal)}
            </dd>
          </div>
        </dl>
      </section>

      <div className="flex flex-col gap-[9px]">
        {a.has('PAY') && o.sessionId != null && (
          <Button block size="lg" onClick={() => h.pay(o.sessionId as number)}>
            {t('orders.action.pay')}
          </Button>
        )}
        {a.has('CONFIRM_RECEIPT') && (
          <Button block size="lg" iconLeft="check" onClick={() => h.confirm(o)}>
            {t('orders.action.received')}
          </Button>
        )}
        {a.has('REQUEST_RETURN') && (
          <Button block variant="outline" iconLeft="undo-2" onClick={onReturn}>
            {t('orders.detail.request_return')}
          </Button>
        )}
        {a.has('REVIEW') && (
          <Button block size="lg" iconLeft="star" onClick={onReview}>
            {t('orders.detail.review')}
          </Button>
        )}
        {o.status === 'COMPLETED' && o.returnWindowClosed && (
          <p className="m-0 text-xs text-muted text-center">{t('orders.detail.return_closed')}</p>
        )}
        {o.status === 'DELIVERING' && (
          <Button block variant="outline" iconLeft="map-pin" disabled>
            {t('orders.detail.on_the_way')}
          </Button>
        )}
        {['PAID', 'CONFIRMED', 'PREPARING'].includes(o.status) && (
          <Button block variant="outline" iconLeft="clock" disabled>
            {t('orders.detail.preparing')}
          </Button>
        )}
        {a.has('BUY_AGAIN') && (
          <Button block variant="outline" iconLeft="shopping-cart" onClick={() => h.again(o.items)}>
            {t('orders.action.again')}
          </Button>
        )}
        {a.has('CANCEL') && (
          <Button block variant="ghost" className="text-red-600" onClick={() => h.cancel([o])}>
            {t('orders.action.cancel')}
          </Button>
        )}
      </div>
    </div>
  );
}

function Tracking({ id }: OrderTrackingProps) {
  const { t } = useT();
  const q = useOrder(id);
  const { handlers, dialogs } = useOrderHandlers();
  // ?return=1 / ?review=1 từ nút trên thẻ đơn → mở hộp tương ứng khi đơn cho phép
  const [modal, setModal] = useState<'return' | 'review' | null>(null);
  useEffect(() => {
    const p = new URLSearchParams(window.location.search);
    const want = p.get('return') ? 'return' : p.get('review') ? 'review' : null;
    if (!want || !q.data) return;
    if (q.data.actions.includes(want === 'return' ? 'REQUEST_RETURN' : 'REVIEW')) setModal(want);
    p.delete('return');
    p.delete('review');
    const qs = p.toString();
    window.history.replaceState(
      null,
      '',
      window.location.pathname + (qs ? `?${qs}` : '') + window.location.hash,
    );
  }, [q.data]);
  if (q.isError)
    return (
      <div className="max-w-[560px] mx-auto px-6 py-16 text-center">
        <h1 className="m-0 mb-2 text-2xl font-semibold text-strong">{t('errors.GNE-ORD-4041')}</h1>
        <Button href="/orders" iconLeft="arrow-left">
          {t('orders.title')}
        </Button>
      </div>
    );
  const o = q.data;
  return (
    <div className="mx-auto max-w-[1100px] px-6 pt-5 pb-14 max-md:px-3">
      <div className="flex items-center gap-3 mb-4 flex-wrap">
        <a
          href="/orders"
          className="inline-flex items-center gap-1.5 text-sm text-muted no-underline hover:text-strong"
        >
          <Icon name="arrow-left" size={18} /> {t('orders.title')}
        </a>
        <span className="w-px h-4 bg-line-strong" aria-hidden="true" />
        <h1 className="m-0 font-semibold text-md text-strong">
          {t('orders.detail.code')}{' '}
          <span className="font-mono font-normal text-sm text-body">{o?.code ?? ''}</span>
        </h1>
        {o && (
          <span className="ml-auto flex items-center gap-2">
            <Badge variant="neutral">{t(`orders.pay.${o.paymentMethod}`)}</Badge>
            <OrderStatus o={o} />
          </span>
        )}
      </div>
      {!o ? (
        <div className="grid gap-4" aria-busy="true">
          <Skeleton className="h-[140px]" rounded="lg" />
          <Skeleton className="h-[320px]" rounded="lg" />
        </div>
      ) : (
        <>
          <Stepper o={o} />
          <div className="grid grid-cols-[minmax(0,1fr)_380px] gap-[18px] mt-[18px] items-start max-[900px]:grid-cols-1">
            <div className="flex flex-col gap-[18px] min-w-0">
              <Carrier o={o} />
              <EscrowNote o={o} />
              {o.refund && (
                <RefundBlock
                  refund={{ ...o.refund, orderId: o.id }}
                  paymentMethod={o.paymentMethod}
                />
              )}
              <Timeline o={o} />
            </div>
            <Side
              o={o}
              h={handlers}
              onReturn={() => setModal('return')}
              onReview={() => setModal('review')}
            />
          </div>
        </>
      )}
      {dialogs}
      {o && (
        <>
          <ReturnModal open={modal === 'return'} order={o} onClose={() => setModal(null)} />
          <ReviewModal open={modal === 'review'} order={o} onClose={() => setModal(null)} />
        </>
      )}
    </div>
  );
}

export default function OrderTracking(props: OrderTrackingProps) {
  return (
    <Providers>
      <Tracking {...props} />
    </Providers>
  );
}
