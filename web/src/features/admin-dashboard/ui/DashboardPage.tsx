// Tổng quan Admin (prototype admin/Dashboard.jsx) — plan G12.08: việc cần xử lý đứng đầu, kỳ báo cáo
// (một bộ chọn cho mọi số tiền) + Xuất CSV, 4 ô GMV / Hoa hồng / Voucher / Lợi nhuận ròng, "Hoa hồng đi
// về đâu", biểu đồ GMV, tiền sàn đang giữ, tiền đang chờ xử lý, đơn đang chạy, xếp hạng shop / sản
// phẩm, shop tăng trưởng nhanh. Khối tiền chỉ hiện khi có VIEW_FINANCE (API 11.2).
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { listRootCategories } from '@/features/catalog';
import { usePermissions } from '@/shared/auth/usePermissions';
import { useT } from '@/shared/hooks/useT';
import { downloadCsv, type CsvCell } from '@/shared/lib/csv';
import { formatMoneyShort, formatNumber, formatVND } from '@/shared/lib/format';
import { Avatar, Button, Icon, Segmented, Select, Skeleton, TablePager } from '@/shared/ui';
import {
  BarList,
  Delta,
  Pipeline,
  SegBar,
  StackBar,
  StatTile,
  TimeChart,
  WorkQueue,
  type Job,
} from '@/shared/ui/dash';
import { useAdminQueues } from '../application/useAdminQueues';
import {
  useDashboardFinance,
  useGmvChart,
  useGrowingShops,
  useOperations,
} from '../application/useDashboard';
import {
  FINISHED_TONE,
  GMV_RANGES,
  GROWING_PAGE_SIZE,
  gmvPointLabel,
  monthLabel,
  MOVING_TONE,
  previousMonth,
} from '../domain/dashboard.rules';
import type { DashboardFinance, GmvRange, MonthKey } from '../domain/dashboard.types';

function Jobs() {
  const { t } = useT();
  const navigate = useNavigate();
  const q = useAdminQueues().data;
  if (!q) return <Skeleton lines={3} />;
  const job = (id: string, count: number | undefined, to: string, hint?: string): Job[] =>
    count === undefined
      ? []
      : [
          {
            id,
            count,
            label: t(`admin-dashboard.jobs.${id}`),
            hint: hint ?? t(`admin-dashboard.jobs.${id}_hint`),
            onClick: () => navigate(to),
          },
        ];
  const jobs = [
    ...job('kyc', q.kycPending, '/kyc'),
    ...job('products', q.productsPending, '/product-moderation'),
    ...job('disputes', q.disputesOpen, '/refunds'),
    ...job(
      'withdrawals',
      q.withdrawalsPending,
      '/withdrawals',
      t('admin-dashboard.jobs.withdrawals_hint', { count: q.withdrawalsBlocked ?? 0 }),
    ),
    ...job('cod', q.codRemittancesWaiting, '/cod-reconciliation'),
  ];
  return (
    <WorkQueue
      title={t('admin-dashboard.jobs.title')}
      jobs={jobs}
      doneText={t('admin-dashboard.jobs.done')}
    />
  );
}

function GmvChartSheet({ month }: { month: MonthKey | undefined }) {
  const { t, locale } = useT();
  const [range, setRange] = useState<GmvRange>('6m');
  const chart = useGmvChart(range, month, true).data;
  const m = monthLabel(month ?? '');
  return (
    <section className="dash-sheet">
      <div className="dash-head">
        <div>
          <h2 className="dash-title">{t('admin-dashboard.gmv.title')}</h2>
          <div className="dash-sub">
            {chart
              ? t(`admin-dashboard.gmv.chart.${chart.titleKey}`, { month: m, year: m.slice(3) })
              : ' '}
          </div>
        </div>
        <div className="dash-end">
          <Segmented
            aria-label={t('admin-dashboard.gmv.range')}
            value={range}
            onChange={setRange}
            items={GMV_RANGES.map((id) => ({ id, label: t(`admin-dashboard.gmv.ranges.${id}`) }))}
          />
        </div>
      </div>
      {chart ? (
        <TimeChart
          kind="line"
          name={t('admin-dashboard.gmv.series')}
          height={260}
          data={chart.points.map((p) => ({
            label: gmvPointLabel(p.label, range, locale),
            value: p.value,
          }))}
        />
      ) : (
        <Skeleton lines={6} />
      )}
    </section>
  );
}

function MoneyBlocks({ f }: { f: DashboardFinance }) {
  const { t, locale } = useT();
  const navigate = useNavigate();
  const vnd = (v: number) => formatVND(v, { locale });
  const prev = previousMonth(f.availableMonths, f.month);
  const against = prev
    ? t('admin-dashboard.against', { month: monthLabel(prev) })
    : t('admin-dashboard.against_none');
  const m = monthLabel(f.month);
  const pm = f.pendingMoney;
  const holding = f.holdings.escrowHeld + f.holdings.walletBalances;
  const pendingTotal =
    pm.payableWithdrawals + pm.blockedWithdrawals + pm.codRemittances + pm.refundsPending;
  return (
    <>
      <div className="dash-tiles">
        <StatTile
          label={t('admin-dashboard.tiles.gmv')}
          value={vnd(f.gmv.value)}
          note={<Delta cur={f.gmv.value} prev={f.gmv.previous} against={against} />}
        />
        <StatTile
          label={t('admin-dashboard.tiles.commission')}
          value={vnd(f.commission.value)}
          note={<Delta cur={f.commission.value} prev={f.commission.previous} against={against} />}
          onClick={() => navigate('/commission')}
        />
        <StatTile
          label={t('admin-dashboard.tiles.voucher')}
          value={vnd(f.voucherCost.value)}
          note={
            <Delta
              cur={f.voucherCost.value}
              prev={f.voucherCost.previous}
              against={against}
              downIsGood
            />
          }
          onClick={() => navigate('/vouchers')}
        />
        <StatTile
          label={t('admin-dashboard.tiles.net')}
          value={vnd(f.netProfit)}
          note={t('admin-dashboard.tiles.net_note')}
          onClick={() => navigate('/ledger')}
        />
      </div>

      <section className="dash-sheet">
        <div className="dash-head">
          <div>
            <h2 className="dash-title">{t('admin-dashboard.split.title', { month: m })}</h2>
            <div className="dash-sub">{t('admin-dashboard.split.desc')}</div>
          </div>
        </div>
        <div className="dash-sub">{t('admin-dashboard.split.total')}</div>
        <div className="dash-figure mb-3.5">{vnd(f.commission.value)}</div>
        <SegBar
          label={t('admin-dashboard.split.title', { month: m })}
          format={vnd}
          parts={[
            { label: t('admin-dashboard.split.net'), value: f.netProfit, tone: 'ok' },
            { label: t('admin-dashboard.split.voucher'), value: f.voucherCost.value, tone: 'held' },
            { label: t('admin-dashboard.split.coin'), value: f.coinCost, tone: 'info' },
            { label: t('admin-dashboard.split.gateway'), value: f.gatewayFee, tone: 'wait' },
          ]}
        />
      </section>

      <GmvChartSheet month={f.month} />

      <div className="dash-cols dash-cols--even">
        <section className="dash-sheet">
          <div className="dash-head">
            <h2 className="dash-title">{t('admin-dashboard.holding.title')}</h2>
            <span className="dash-sub dash-end">{t('admin-dashboard.holding.note')}</span>
          </div>
          <div className="dash-figure mb-3">{vnd(holding)}</div>
          <StackBar
            label={t('admin-dashboard.holding.title')}
            parts={[
              {
                label: t('admin-dashboard.holding.escrow'),
                note: t('admin-dashboard.holding.escrow_note'),
                value: f.holdings.escrowHeld,
                tone: 'held',
                onClick: () => navigate('/ledger'),
              },
              {
                label: t('admin-dashboard.holding.wallet'),
                note: t('admin-dashboard.holding.wallet_note'),
                value: f.holdings.walletBalances,
                tone: 'ok',
                onClick: () => navigate('/ledger'),
              },
            ]}
          />
        </section>
        <section className="dash-sheet">
          <div className="dash-head">
            <h2 className="dash-title">{t('admin-dashboard.pending.title')}</h2>
          </div>
          <div className="dash-figure mb-3">{vnd(pendingTotal)}</div>
          <StackBar
            label={t('admin-dashboard.pending.title')}
            parts={[
              {
                label: t('admin-dashboard.pending.payable'),
                note: t('admin-dashboard.pending.payable_note'),
                value: pm.payableWithdrawals,
                tone: 'info',
                onClick: () => navigate('/withdrawals'),
              },
              {
                label: t('admin-dashboard.pending.blocked'),
                note: t('admin-dashboard.pending.blocked_note'),
                value: pm.blockedWithdrawals,
                tone: 'wait',
                onClick: () => navigate('/withdrawals'),
              },
              {
                label: t('admin-dashboard.pending.cod'),
                note: t('admin-dashboard.pending.cod_note'),
                value: pm.codRemittances,
                tone: 'held',
                onClick: () => navigate('/cod-reconciliation'),
              },
              {
                label: t('admin-dashboard.pending.refunds'),
                note: t('admin-dashboard.pending.refunds_note', { count: pm.openDisputes }),
                value: pm.refundsPending,
                tone: 'bad',
                onClick: () => navigate('/refunds'),
              },
            ]}
          />
        </section>
      </div>
    </>
  );
}

function GrowingShops({ seeMoney }: { seeMoney: boolean }) {
  const { t, locale } = useT();
  const [categoryId, setCategoryId] = useState(0);
  const [q, setQ] = useState('');
  const [page, setPage] = useState(1);
  const cats = useQuery({ queryKey: ['catalog', 'roots'], queryFn: listRootCategories });
  const res = useGrowingShops({ categoryId: categoryId || undefined, q, page }).data;
  return (
    <section className="dash-sheet dash-sheet--flush">
      <div className="dash-head">
        <h2 className="dash-title">{t('admin-dashboard.growing.title')}</h2>
        <div className="dash-end flex gap-2.5 flex-wrap">
          <Select<number>
            className="w-[190px]"
            aria-label={t('admin-dashboard.growing.category')}
            value={categoryId}
            onChange={(v) => {
              setCategoryId(v);
              setPage(1);
            }}
            options={[
              { value: 0, label: t('admin-dashboard.growing.all_categories') },
              ...(cats.data ?? []).map((c) => ({ value: c.id, label: c.name })),
            ]}
          />
          <div className="relative w-[220px] max-sm:w-full">
            <Icon
              name="search"
              size={16}
              className="absolute left-3 top-[14px] text-subtle pointer-events-none"
            />
            <input
              aria-label={t('admin-dashboard.growing.search')}
              value={q}
              onChange={(e) => {
                setQ(e.target.value);
                setPage(1);
              }}
              placeholder={t('admin-dashboard.growing.search_placeholder')}
              className="w-full h-11 pl-[34px] pr-3 border-[1.5px] border-solid border-line rounded-md text-sm bg-surface-card text-body outline-none focus:border-primary"
            />
          </div>
        </div>
      </div>
      {!res ? (
        <Skeleton lines={5} className="m-5" />
      ) : res.total_items === 0 ? (
        <div className="dash-empty">{t('admin-dashboard.growing.empty')}</div>
      ) : (
        <div className="dash-tablewrap">
          <table className="dash-table">
            <thead>
              <tr>
                <th>{t('admin-dashboard.growing.shop')}</th>
                <th>{t('admin-dashboard.growing.category')}</th>
                <th className="num">{t('admin-dashboard.growing.sold')}</th>
                <th>{t('admin-dashboard.growing.rating')}</th>
                <th className="num">{t('admin-dashboard.growing.growth')}</th>
                {seeMoney && <th className="num">GMV</th>}
              </tr>
            </thead>
            <tbody>
              {res.items.map((s) => (
                <tr key={s.shop.id}>
                  <td>
                    <span className="inline-flex items-center gap-2.5 font-semibold text-strong">
                      <Avatar name={s.shop.name} size={30} />
                      {s.shop.name}
                    </span>
                  </td>
                  <td>{s.categoryName}</td>
                  <td className="num">{formatNumber(s.sold, { locale })}</td>
                  <td>
                    <span className="inline-flex items-center gap-1">
                      <Icon name="star" size={14} className="text-gold-500 fill-gold-500" />
                      {formatNumber(s.rating, { locale })}
                    </span>
                  </td>
                  <td className="num font-semibold">
                    {s.growthPercent > 0 ? '+' : ''}
                    {formatNumber(s.growthPercent, { locale })}%
                  </td>
                  {seeMoney && (
                    <td className="num font-semibold">
                      {s.gmv !== null ? formatVND(s.gmv, { locale }) : '—'}
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {res && (
        <TablePager
          page={res.page}
          size={GROWING_PAGE_SIZE}
          totalItems={res.total_items}
          totalPages={res.total_pages}
          onChange={setPage}
          unit={t('admin-dashboard.growing.unit')}
        />
      )}
    </section>
  );
}

export default function DashboardPage() {
  const { t, locale } = useT();
  const { can } = usePermissions();
  const seeMoney = can('VIEW_FINANCE');
  const [month, setMonth] = useState<MonthKey | undefined>(undefined);
  const finance = useDashboardFinance(month, seeMoney);
  const ops = useOperations().data;
  const f = finance.data;

  const exportCsv = () => {
    if (!f) return;
    const rows: CsvCell[][] = [
      [t('admin-dashboard.csv.period'), monthLabel(f.month)],
      ['GMV', f.gmv.value],
      [t('admin-dashboard.tiles.commission'), f.commission.value],
      [t('admin-dashboard.csv.voucher'), f.voucherCost.value],
      [t('admin-dashboard.csv.coin'), f.coinCost],
      [t('admin-dashboard.csv.gateway'), f.gatewayFee],
      [t('admin-dashboard.tiles.net'), f.netProfit],
      [],
      [t('admin-dashboard.growing.shop'), 'GMV'],
      ...f.topShopsByGmv.map((s): CsvCell[] => [s.name, s.gmv]),
      [],
      [t('admin-dashboard.csv.status'), t('admin-dashboard.csv.count')],
      ...(ops
        ? [
            ...ops.inProgress.map((r): CsvCell[] => [
              t(`admin-dashboard.orders.${r.group}`),
              r.count,
            ]),
            ...ops.finished.map((r): CsvCell[] => [
              t(`admin-dashboard.orders.${r.group}`),
              r.count,
            ]),
          ]
        : []),
      [],
      [t('admin-dashboard.top.products'), t('admin-dashboard.csv.sold')],
      ...(ops?.topProducts ?? []).map((p): CsvCell[] => [p.name, p.sold]),
    ];
    downloadCsv(`bao-cao-${monthLabel(f.month).replace('/', '-')}.csv`, rows);
  };

  const moving = ops?.inProgress ?? [];
  const movingTotal = moving.reduce((n, r) => n + r.count, 0);

  return (
    <div className="dash">
      <Jobs />

      {seeMoney && (
        <>
          <div className="dash-toolbar">
            <label>
              {t('admin-dashboard.period')}
              <select
                value={f?.month ?? ''}
                onChange={(e) => setMonth(e.target.value)}
                disabled={!f}
              >
                {(f?.availableMonths ?? []).map((m) => (
                  <option key={m} value={m}>
                    {monthLabel(m)}
                  </option>
                ))}
              </select>
            </label>
            <div className="dash-end">
              <Button
                size="sm"
                variant="ghost"
                iconLeft="download"
                onClick={exportCsv}
                disabled={!f}
              >
                {t('admin-dashboard.export')}
              </Button>
            </div>
          </div>
          {f ? <MoneyBlocks f={f} /> : <Skeleton lines={8} />}
        </>
      )}

      <section className="dash-sheet">
        <div className="dash-head">
          <h2 className="dash-title">{t('admin-dashboard.orders.title')}</h2>
          <span className="dash-sub">
            {t('admin-dashboard.orders.sub', { count: formatNumber(movingTotal, { locale }) })}
          </span>
        </div>
        {ops ? (
          <Pipeline
            steps={moving.map((r) => [
              t(`admin-dashboard.orders.${r.group}`),
              r.count,
              MOVING_TONE[r.group],
            ])}
            outcomes={ops.finished.map((r) => [
              t(`admin-dashboard.orders.${r.group}`),
              r.count,
              FINISHED_TONE[r.group],
            ])}
          />
        ) : (
          <Skeleton lines={3} />
        )}
      </section>

      <div className={seeMoney ? 'dash-cols dash-cols--even' : 'dash'}>
        {seeMoney && (
          <section className="dash-sheet">
            <div className="dash-head">
              <h2 className="dash-title">{t('admin-dashboard.top.shops')}</h2>
            </div>
            {f ? (
              <BarList
                rows={f.topShopsByGmv.map((s) => [s.name, s.gmv])}
                format={(v) => formatMoneyShort(v, { locale })}
              />
            ) : (
              <Skeleton lines={6} />
            )}
          </section>
        )}
        <section className="dash-sheet">
          <div className="dash-head">
            <h2 className="dash-title">{t('admin-dashboard.top.products')}</h2>
          </div>
          {ops ? (
            <BarList rows={ops.topProducts.map((p) => [p.name, p.sold])} />
          ) : (
            <Skeleton lines={6} />
          )}
        </section>
      </div>

      <GrowingShops seeMoney={seeMoney} />
    </div>
  );
}
