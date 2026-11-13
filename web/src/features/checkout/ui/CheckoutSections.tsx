// Các khối của trang Thanh toán (prototype Checkout.jsx): địa chỉ nhận, Voucher Genie, Điểm Genie,
// phương thức thanh toán, chi tiết thanh toán + đồng hồ giữ hàng — plan G10.18–G10.20.
import { useEffect, useState, type ReactNode } from 'react';
import { formatPhone, fullAddress, type Address } from '@/features/account';
import { HoldNote, PaymentLogo } from '@/features/payment';
import type { PriceBreakdown, Voucher } from '@/features/promotions';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatMmSs, formatNumber, formatVND } from '@/shared/lib/format';
import { Badge, Button, Icon, Radio, Skeleton, Switch } from '@/shared/ui';
import { isEscrowed, placeLabelKey, secondsLeft } from '../domain/checkout.rules';
import { PAYMENT_METHODS, type PaymentMethod } from '../domain/checkout.types';

export const cardClass = 'bg-surface-card rounded-lg px-[18px] py-4 mb-3.5 shadow-sm';

export function AddressCard({
  address,
  loading,
  onChange,
}: {
  address: Address | null;
  loading: boolean;
  onChange: () => void;
}) {
  const { t } = useT();
  return (
    <section className={cardClass} aria-label={t('checkout.address.title')}>
      <div className="flex gap-3 items-start">
        <Icon name="map-pin" size={20} className="text-primary mt-0.5 flex-none" />
        <div className="flex-1 min-w-0">
          {loading ? (
            <Skeleton lines={2} />
          ) : address ? (
            <>
              <div className="flex items-center gap-2.5 mb-1 flex-wrap">
                <span className="font-bold text-base text-strong">{address.recipient}</span>
                <span className="font-mono text-muted">{formatPhone(address.phone)}</span>
                <Badge variant="neutral">{t(`account.address.label.${address.label}`)}</Badge>
                {address.isDefault && (
                  <Badge variant="primary">{t('account.address.default')}</Badge>
                )}
              </div>
              <p className="m-0 text-base text-body">{fullAddress(address)}</p>
            </>
          ) : (
            <p className="m-0 text-base text-muted">{t('checkout.address.none')}</p>
          )}
        </div>
        <Button variant="ghost" iconLeft={address ? 'pencil' : 'plus'} onClick={onChange}>
          {t(address ? 'checkout.address.change' : 'checkout.address.add')}
        </Button>
      </div>
    </section>
  );
}

export function PlatformVoucherCard({
  price,
  extraSaving,
  onOpen,
  onBest,
}: {
  price: PriceBreakdown;
  extraSaving: number;
  onOpen: () => void;
  onBest: () => void;
}) {
  const { t, locale } = useT();
  const vnd = (n: number) => formatVND(n, { locale });
  const row = (badge: ReactNode, v: Voucher, text: string) => (
    <div className="flex items-center gap-2 text-sm text-body min-w-0">
      {badge}
      <span className="truncate">
        <span className="font-mono">{v.code}</span>: {text}
      </span>
    </div>
  );
  const any = price.platformVoucher || price.freeshipVoucher;
  return (
    <section className={cardClass} aria-labelledby="co-voucher">
      <div className="flex items-center gap-2 mb-3">
        <Icon name="ticket" size={18} className="text-primary" />
        <h2 id="co-voucher" className="m-0 font-bold text-base text-strong">
          {t('cart.bar.voucher')}
        </h2>
        <Badge variant="neutral">{t('checkout.voucher.one_each')}</Badge>
      </div>
      <button
        type="button"
        onClick={onOpen}
        className="w-full flex items-center gap-3 px-4 py-3 rounded-md bg-gray-25 border border-solid border-line text-left cursor-pointer hover:border-primary"
      >
        <span className="flex-1 min-w-0">
          <span className={cn('block font-semibold text-sm text-strong', any && 'mb-2')}>
            {t(any ? 'checkout.voucher.chosen' : 'checkout.voucher.choose')}
          </span>
          {any && (
            <span className="flex flex-col gap-1.5">
              {price.platformVoucher &&
                row(
                  <Badge variant="primary">{t('checkout.voucher.platform')}</Badge>,
                  price.platformVoucher,
                  t('checkout.voucher.off', { amount: vnd(price.platformDiscount) }),
                )}
              {price.freeshipVoucher &&
                row(
                  <Badge variant="mint">Freeship</Badge>,
                  price.freeshipVoucher,
                  t('checkout.voucher.ship_off', { amount: vnd(price.freeshipDiscount) }),
                )}
            </span>
          )}
        </span>
        <Icon name="chevron-right" size={20} className="text-muted" />
      </button>
      <div className="flex items-center gap-2 mt-2.5 text-xs text-muted flex-wrap">
        {extraSaving > 0 ? (
          <>
            <Icon name="zap" size={14} className="text-primary" />
            <span>{t('checkout.voucher.extra', { amount: vnd(extraSaving) })}</span>
            <Button size="sm" variant="outline" className="ml-auto" onClick={onBest}>
              {t('checkout.voucher.apply_best')}
            </Button>
          </>
        ) : (
          <>
            <Icon name="circle-check" size={14} className="text-mint-600" />
            <span>{t('checkout.voucher.is_best')}</span>
          </>
        )}
      </div>
    </section>
  );
}

export function PointsCard({
  balance,
  price,
  on,
  onToggle,
}: {
  balance: number;
  price: PriceBreakdown;
  on: boolean;
  onToggle: () => void;
}) {
  const { t, locale } = useT();
  return (
    <section className={cardClass} aria-labelledby="co-points">
      <div className="flex items-center gap-3">
        <span className="w-10 h-10 rounded-md bg-gold-50 text-gold-600 grid place-items-center flex-none">
          <Icon name="star" size={22} />
        </span>
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap">
            <h2 id="co-points" className="m-0 font-bold text-base text-strong">
              {t('checkout.points.title')}
            </h2>
            <Badge variant="neutral">{t('checkout.points.rate')}</Badge>
          </div>
          <p className="m-0 mt-0.5 text-xs text-muted">
            {t('checkout.points.have', { points: formatNumber(balance, { locale }) })} ·{' '}
            {t('checkout.points.cap', { amount: formatVND(price.pointsCap, { locale }) })}
          </p>
        </div>
        <Switch
          aria-label={t('cart.bar.points_toggle')}
          checked={on}
          disabled={price.subtotal <= 0 || balance <= 0}
          onChange={onToggle}
        />
      </div>
      {on && price.pointsDiscount > 0 && (
        <div className="flex items-center gap-2 mt-3 px-3 py-[9px] bg-gold-50 rounded-md text-sm text-body">
          <Icon name="circle-check" size={16} className="text-gold-600" />
          <span>
            {t('checkout.points.applied', {
              points: formatNumber(price.pointsDiscount, { locale }),
              amount: formatVND(price.pointsDiscount, { locale }),
            })}
          </span>
        </div>
      )}
    </section>
  );
}

export function PaymentMethodList({
  value,
  onChange,
}: {
  value: PaymentMethod;
  onChange: (m: PaymentMethod) => void;
}) {
  const { t } = useT();
  return (
    <section className={cardClass} aria-labelledby="co-pay">
      <h2 id="co-pay" className="m-0 mb-1 font-bold text-base text-strong">
        {t('checkout.pay.title')}
      </h2>
      <p className="m-0 mb-3 flex items-center gap-1.5 text-xs text-muted">
        <Icon name="shield-check" size={14} className="text-mint-600 flex-none" />
        {t('checkout.pay.note')}
      </p>
      <div role="radiogroup" aria-labelledby="co-pay">
        {PAYMENT_METHODS.map((m) => (
          <label
            key={m}
            className={cn(
              'flex flex-wrap items-center gap-3 px-3.5 py-3 mb-2 rounded-md cursor-pointer border-[1.5px] border-solid',
              value === m ? 'border-primary bg-primary-tint' : 'border-line bg-surface-card',
            )}
          >
            <Radio name="pay" checked={value === m} onChange={() => onChange(m)} />
            <span className="w-[30px] h-[30px] grid place-items-center flex-none">
              <PaymentLogo method={m} />
            </span>
            <span className="font-medium text-base text-strong">{t(`checkout.pay.${m}.name`)}</span>
            <span className="ml-auto text-xs text-muted text-right max-sm:basis-full">
              {t(`checkout.pay.${m}.meta`)}
            </span>
          </label>
        ))}
      </div>
    </section>
  );
}

/** Đồng hồ giữ tồn kho 15 phút (G10.19). Hết giờ → báo và cho giữ lại. */
export function HoldCountdown({ deadline, onRenew }: { deadline: number; onRenew: () => void }) {
  const { t } = useT();
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const i = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(i);
  }, []);
  const left = secondsLeft(deadline, now);
  if (left <= 0)
    return (
      <div className="flex items-center gap-2 bg-red-50 text-red-600 rounded-md px-3 py-[9px] text-sm mb-4">
        <Icon name="clock" size={16} />
        <span className="flex-1">{t('checkout.hold.expired')}</span>
        <Button size="sm" variant="outline" onClick={onRenew}>
          {t('checkout.hold.renew')}
        </Button>
      </div>
    );
  return (
    <div
      className="flex items-center gap-2 bg-amber-50 text-amber-600 rounded-md px-3 py-[9px] text-sm mb-4"
      role="timer"
      aria-live="off"
    >
      <Icon name="lock" size={16} />
      <span>
        {t('checkout.hold.left')} <b className="font-mono text-amber-600">{formatMmSs(left)}</b>
      </span>
    </div>
  );
}

export function CheckoutSummary({
  price,
  orderCount,
  itemCount,
  method,
  canPlace,
  placing,
  deadline,
  onRenew,
  onPlace,
}: {
  price: PriceBreakdown;
  orderCount: number;
  itemCount: number;
  method: PaymentMethod;
  canPlace: boolean;
  placing: boolean;
  deadline: number;
  onRenew: () => void;
  onPlace: () => void;
}) {
  const { t, locale } = useT();
  const vnd = (n: number) => formatVND(n, { locale });
  const row = (label: ReactNode, value: string, disc = false) => (
    <div className="flex justify-between text-sm text-muted py-1">
      <span className="inline-flex items-center gap-[5px]">{label}</span>
      <span className={cn('tabular-nums', disc && 'text-mint-600 font-semibold')}>{value}</span>
    </div>
  );
  return (
    <aside className="sticky top-[calc(var(--top-h,86px)+12px)]" aria-labelledby="co-sum">
      <div className={cardClass}>
        <HoldCountdown deadline={deadline} onRenew={onRenew} />
        <h2 id="co-sum" className="m-0 mb-3 font-bold text-base text-strong">
          {t('checkout.summary.title')}
        </h2>
        {row(t('checkout.summary.subtotal'), vnd(price.subtotal))}
        {row(t('checkout.summary.shipping'), vnd(price.shipping))}
        {price.platformDiscount > 0 &&
          row(t('checkout.summary.platform'), `−${vnd(price.platformDiscount)}`, true)}
        {price.freeshipDiscount > 0 &&
          row(t('checkout.summary.freeship'), `−${vnd(price.freeshipDiscount)}`, true)}
        {price.shopDiscount > 0 &&
          row(t('checkout.summary.shop'), `−${vnd(price.shopDiscount)}`, true)}
        {price.pointsDiscount > 0 &&
          row(
            <>
              <Icon name="star" size={13} className="text-gold-500" />
              {t('checkout.summary.points')}
            </>,
            `−${vnd(price.pointsDiscount)}`,
            true,
          )}
        <div className="border-0 border-t border-dashed border-line my-2.5" />
        <div className="flex justify-between items-baseline">
          <span className="font-semibold text-base text-strong">{t('cart.bar.total')}</span>
          <span className="font-extrabold text-[26px] text-price tabular-nums">
            {vnd(price.total)}
          </span>
        </div>
        <p className="m-0 mt-0.5 text-xs text-muted text-right">
          {t('checkout.summary.count', { orders: orderCount, items: itemCount })}
        </p>
        {isEscrowed(method) && (
          <HoldNote className="mt-3.5">
            {t('checkout.summary.hold', { amount: vnd(price.total) })}
          </HoldNote>
        )}
        <div className="tr-inline-cta mt-3.5">
          <Button block size="lg" disabled={!canPlace} loading={placing} onClick={onPlace}>
            {placing ? t('checkout.place.processing') : t(placeLabelKey(method))}
          </Button>
        </div>
        <p className="m-0 mt-2.5 text-xs text-subtle text-center">
          {t('checkout.summary.terms_before')}{' '}
          <a href="/policies/terms" target="_blank" rel="noreferrer">
            {t('checkout.summary.terms_link')}
          </a>
        </p>
      </div>
    </aside>
  );
}
