// Tổng quan Seller Center (prototype vendor/Overview.jsx) — plan G11.10: việc cần làm xếp đầu, banner KYC,
// Doanh thu (7 ngày / 30 ngày / 12 tháng), Tiền của shop, Đơn hàng gần đây, Đơn 30 ngày qua, Sản phẩm.
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  OUTCOME_ORDER,
  conversion,
  isoToVnDate,
  pointLabel,
  useRevenue,
  type RevenueRange,
} from '@/features/seller-analytics';
import {
  itemCount,
  orderBadge,
  OrderWhen,
  sellerNow,
  useSellerOrders,
  type SellerOrder,
} from '@/features/seller-orders';
import { kycGroup, useSellerMe } from '@/features/seller-settings';
import { useT } from '@/shared/hooks/useT';
import { formatNumber, formatPercent, formatVND } from '@/shared/lib/format';
import { Button, buttonClass, Icon, Segmented, Skeleton, StatusBadge } from '@/shared/ui';
import { BarList, Delta, SegBar, StackBar, TimeChart, WorkQueue, type Job } from '@/shared/ui/dash';
import { useDashboard } from '../application/useDashboard';
import { hoursLeft } from '../domain/dashboard.rules';
import type { Dashboard } from '../domain/dashboard.types';

const RANGES: RevenueRange[] = ['7d', '30d', '12m'];
const ghostLink = buttonClass({ variant: 'ghost', size: 'sm' });

function KycBanner() {
  const { t } = useT();
  const me = useSellerMe();
  if (!me.data) return null;
  const g = kycGroup(me.data.kycStatus);
  if (g === 'VERIFIED') return null;
  const bad = g === 'REJECTED' || g === 'SUPPLEMENT';
  const key = bad ? 'rejected' : 'waiting';
  return (
    <div
      role="status"
      className={`flex items-center gap-3 flex-wrap rounded-lg px-4 py-3 ${bad ? 'bg-red-50' : 'bg-amber-50'}`}
    >
      <Icon
        name={bad ? 'triangle-alert' : 'clock'}
        size={20}
        className={`flex-none ${bad ? 'text-red-600' : 'text-amber-600'}`}
      />
      <span className="flex-1 min-w-[200px] text-base text-body">
        <b>{t(`seller-dashboard.kyc.${key}.title`)}.</b> {t(`seller-dashboard.kyc.${key}.desc`)}
      </span>
      <Link to="/kyc" className={ghostLink}>
        {t(`seller-dashboard.kyc.${key}.cta`)}
        <Icon name="chevron-right" size={16} />
      </Link>
    </div>
  );
}

function Revenue() {
  const { t, locale } = useT();
  const [range, setRange] = useState<RevenueRange>('30d');
  const q = useRevenue(range);
  const r = q.data;
  return (
    <section className="dash-sheet">
      <div className="dash-head">
        <h2 className="dash-title">{t(`seller-dashboard.revenue.title.${range}`)}</h2>
        <div className="dash-end">
          <Segmented
            aria-label={t('seller-dashboard.revenue.range')}
            value={range}
            onChange={setRange}
            items={RANGES.map((id) => ({ id, label: t(`seller-dashboard.revenue.ranges.${id}`) }))}
          />
        </div>
      </div>
      {!r ? (
        <Skeleton lines={6} />
      ) : (
        <>
          <div className="dash-figure">{formatVND(r.total, { locale })}</div>
          <div className="dash-sub">
            {r.previous !== null ? (
              <Delta
                cur={r.total}
                prev={r.previous}
                against={t(`seller-dashboard.revenue.against.${range}`)}
              />
            ) : (
              t('seller-dashboard.revenue.against.12m')
            )}
          </div>
          <TimeChart
            kind="bar"
            name={t('seller-dashboard.revenue.series')}
            data={r.points.map((p) => ({
              label: pointLabel(p.label, r.granularity, locale),
              value: p.value,
            }))}
          />
        </>
      )}
    </section>
  );
}

function ShopMoney({ money }: { money: Dashboard['money'] }) {
  const { t, locale } = useT();
  return (
    <section className="dash-sheet">
      <h2 className="dash-title">{t('seller-dashboard.money.title')}</h2>
      <div className="dash-money">
        <div className="dash-sub mt-2.5">{t('seller-dashboard.money.available')}</div>
        <div className="dash-figure">{formatVND(money.available, { locale })}</div>
      </div>
      <Link to="/wallet?withdraw=1" className={buttonClass({ block: true })}>
        <Icon name="hand-coins" size={18} />
        {t('seller-dashboard.money.withdraw')}
      </Link>
      <div className="mt-4">
        <StackBar
          label={t('seller-dashboard.money.title')}
          parts={[
            {
              label: t('seller-dashboard.money.parts.available'),
              value: money.available,
              tone: 'ok',
            },
            {
              label: t('seller-dashboard.money.parts.held'),
              note: t('seller-dashboard.money.parts.held_note'),
              value: money.held,
              tone: 'held',
            },
            {
              label: t('seller-dashboard.money.parts.payout'),
              value: money.pendingWithdrawal,
              tone: 'info',
            },
            {
              label: t('seller-dashboard.money.parts.cod'),
              note: t('seller-dashboard.money.parts.cod_note'),
              value: money.codPending,
              tone: 'wait',
            },
          ]}
        />
      </div>
    </section>
  );
}

function RecentOrders() {
  const { t, locale } = useT();
  const navigate = useNavigate();
  const q = useSellerOrders({ size: 5 });
  const rows: SellerOrder[] = q.data?.items ?? [];
  return (
    <section className="dash-sheet dash-sheet--flush">
      <div className="dash-head">
        <h2 className="dash-title">{t('seller-dashboard.recent.title')}</h2>
        <div className="dash-end">
          <Link to="/orders" className={ghostLink}>
            {t('seller-dashboard.see_all')}
            <Icon name="chevron-right" size={16} />
          </Link>
        </div>
      </div>
      <div className="dash-tablewrap">
        {q.isPending ? (
          <Skeleton lines={5} className="m-4" />
        ) : (
          <table className="dash-table">
            <thead>
              <tr>
                <th>{t('seller-dashboard.recent.code')}</th>
                <th>{t('seller-dashboard.recent.customer')}</th>
                <th>{t('seller-dashboard.recent.items')}</th>
                <th className="num">{t('seller-dashboard.recent.total')}</th>
                <th>{t('seller-dashboard.recent.time')}</th>
                <th>{t('seller-dashboard.recent.status')}</th>
                <th className="num">{t('seller-dashboard.recent.action')}</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((o) => {
                const b = orderBadge(o);
                return (
                  <tr key={o.id}>
                    <td>
                      <span className="code text-xs">{o.code}</span>
                    </td>
                    <td>{o.customer.name}</td>
                    <td>{itemCount(o)}</td>
                    <td className="num font-semibold">{formatVND(o.total, { locale })}</td>
                    <td className="text-muted">
                      <OrderWhen iso={o.createdAt} />
                    </td>
                    <td>
                      <StatusBadge status={b.tone} label={t(b.key)} />
                    </td>
                    <td className="num">
                      {o.status === 'NEW' ? (
                        <Button size="sm" onClick={() => navigate('/orders?tab=NEW')}>
                          {t('seller-dashboard.recent.confirm')}
                        </Button>
                      ) : (
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => navigate(`/orders?q=${encodeURIComponent(o.code)}`)}
                        >
                          {t('seller-dashboard.recent.detail')}
                        </Button>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>
    </section>
  );
}

function Orders30({ d }: { d: Dashboard }) {
  const { t, locale } = useT();
  const o = d.orders30;
  return (
    <section className="dash-sheet">
      <div className="dash-head">
        <h2 className="dash-title">{t('seller-dashboard.orders30.title')}</h2>
        <span className="dash-sub">
          {t('seller-dashboard.orders30.as_of', { date: isoToVnDate(d.asOf) })}
        </span>
        <div className="dash-end">
          <Link to="/analytics" className={ghostLink}>
            {t('seller-dashboard.orders30.analytics')}
            <Icon name="chevron-right" size={16} />
          </Link>
        </div>
      </div>
      <div className="dash-figure">
        {t('seller-dashboard.orders30.count', { count: formatNumber(o.count, { locale }) })}
      </div>
      <div className="dash-sub mb-3.5">
        <Delta
          cur={o.count}
          prev={o.previous}
          against={t('seller-dashboard.revenue.against.30d')}
        />
      </div>
      <SegBar
        label={t('seller-dashboard.orders30.outcomes')}
        parts={OUTCOME_ORDER.map(([k, tone]) => ({
          label: t(`seller-analytics.outcome.${k}`),
          tone,
          value: o.outcomes[k],
        }))}
      />
      <div className="dash-rows mt-3.5">
        <div className="dash-row">
          <span>{t('seller-dashboard.orders30.visits')}</span>
          <b>
            {formatNumber(o.visits, { locale })} <Delta cur={o.visits} prev={o.previousVisits} />
          </b>
        </div>
        <div className="dash-row">
          <span>{t('seller-dashboard.orders30.conversion')}</span>
          <b>{formatPercent(conversion(o.count, o.visits) / 100, { locale, digits: 1 })}</b>
        </div>
      </div>
    </section>
  );
}

function Products({ p }: { p: Dashboard['products'] }) {
  const { t } = useT();
  return (
    <section className="dash-sheet">
      <div className="dash-head">
        <h2 className="dash-title">{t('seller-dashboard.products.title')}</h2>
        <div className="dash-end">
          <Link to="/products" className={ghostLink}>
            {t('seller-dashboard.products.manage')}
            <Icon name="chevron-right" size={16} />
          </Link>
        </div>
      </div>
      <BarList
        rows={[
          [t('seller-dashboard.products.PUBLISHED'), p.PUBLISHED],
          [t('seller-dashboard.products.PENDING_APPROVAL'), p.PENDING_APPROVAL],
          [t('seller-dashboard.products.DRAFT'), p.DRAFT],
          [t('seller-dashboard.products.REJECTED'), p.REJECTED],
        ]}
      />
    </section>
  );
}

export default function OverviewPage() {
  const { t } = useT();
  const navigate = useNavigate();
  const q = useDashboard();
  const d = q.data;
  if (!d) return <Skeleton lines={10} />;
  const left = hoursLeft(d.work.returns.nearestDeadlineAt, sellerNow());
  const jobs: Job[] = [
    {
      id: 'new',
      count: d.work.toConfirm,
      label: t('seller-dashboard.work.new'),
      hint: t('seller-dashboard.work.new_hint'),
      onClick: () => navigate('/orders?tab=NEW'),
    },
    {
      id: 'prep',
      count: d.work.toPrepare,
      label: t('seller-dashboard.work.prep'),
      hint: t('seller-dashboard.work.prep_hint'),
      onClick: () => navigate('/orders?tab=TO_PREPARE'),
    },
    {
      id: 'fail',
      count: d.work.deliveryFailed,
      label: t('seller-dashboard.work.fail'),
      hint: t('seller-dashboard.work.fail_hint'),
      onClick: () => navigate('/orders?tab=DELIVERING'),
    },
    {
      id: 'return',
      count: d.work.returns.count,
      label: t('seller-dashboard.work.return'),
      hint: left !== null ? t('seller-dashboard.work.return_hint', { hours: left }) : '',
      onClick: () => navigate('/returns'),
    },
    {
      id: 'chat',
      count: d.work.unreadChats,
      label: t('seller-dashboard.work.chat'),
      hint: t('seller-dashboard.work.chat_hint'),
      onClick: () => navigate('/chat'),
    },
  ];
  return (
    <div className="dash">
      <KycBanner />
      <WorkQueue
        title={t('seller-dashboard.work.title')}
        jobs={jobs}
        doneText={t('seller-dashboard.work.done')}
      />
      <div className="dash-cols">
        <Revenue />
        <ShopMoney money={d.money} />
      </div>
      <RecentOrders />
      <div className="dash-cols">
        <Orders30 d={d} />
        <Products p={d.products} />
      </div>
    </div>
  );
}
