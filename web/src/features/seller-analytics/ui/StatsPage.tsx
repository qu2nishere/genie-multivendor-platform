// Phân tích bán hàng (prototype VendorPages.jsx Stats) — plan G11.11: chọn Hôm nay / 7 ngày / 30 ngày /
// Tùy chọn ngày; 4 ô số; biểu đồ doanh thu; Tiền thực về shop; Đơn kết thúc thế nào + sức khỏe shop;
// Nguồn truy cập; Cảm xúc đánh giá (AI); Sản phẩm bán chạy; Xuất CSV.
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useT } from '@/shared/hooks/useT';
import { downloadCsv } from '@/shared/lib/csv';
import { formatMoneyShort, formatNumber, formatPercent, formatVND } from '@/shared/lib/format';
import { Button, buttonClass, Icon, Select, Skeleton } from '@/shared/ui';
import { Delta, SegBar, StatTile, TimeChart } from '@/shared/ui/dash';
import {
  useAnalyticsOverview,
  useSentiment,
  useTopProducts,
  useTrafficSources,
  type RangeQuery,
} from '../application/useAnalytics';
import {
  OUTCOME_ORDER,
  conversion,
  pointLabel,
  positiveRate,
  shopCancelRate,
} from '../domain/analytics.rules';
import type { AnalyticsRange } from '../domain/analytics.types';

const RANGES: AnalyticsRange[] = ['today', '7d', '30d', 'custom'];
const dateCls =
  'h-11 border-[1.5px] border-solid border-line rounded-md px-3 text-base text-strong bg-surface-card outline-none focus:border-primary';

export default function StatsPage() {
  const { t, locale } = useT();
  const [range, setRange] = useState<AnalyticsRange>('7d');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const rq: RangeQuery = { range, from, to };
  const ov = useAnalyticsOverview(rq);
  const sent = useSentiment(rq);
  const sources = useTrafficSources();
  const top = useTopProducts();
  const month = useAnalyticsOverview({ range: '30d' });

  const vnd = (n: number) => formatVND(n, { locale });
  const pct = (n: number) => formatPercent(n / 100, { locale, digits: 1 });
  const num = (n: number) => formatNumber(n, { locale });
  const sub = t(`seller-analytics.range_sub.${range}`);
  const against = t(`seller-analytics.against.${range}`);
  const needDates = range === 'custom' && (!from || !to || from > to);
  const d = needDates ? undefined : ov.data;

  const exportCsv = () => {
    if (!d) return;
    const rows: (string | number)[][] = [
      [t('seller-analytics.csv.metric'), t('seller-analytics.csv.value')],
      [t('seller-analytics.period'), `${d.period.from} → ${d.period.to}`],
      [t('seller-analytics.tiles.gross'), d.revenue.gross],
      [t('seller-analytics.tiles.orders'), d.orders.count],
      [t('seller-analytics.tiles.visits'), d.visits.count],
      [
        t('seller-analytics.tiles.conversion'),
        conversion(d.orders.count, d.visits.count).toFixed(2),
      ],
      [t('seller-analytics.net.net'), d.net.net],
      [t('seller-analytics.net.commission', { rate: d.net.commissionRate }), d.net.commission],
      [t('seller-analytics.net.voucher'), d.net.shopVoucherCost],
      ...OUTCOME_ORDER.map(([k]) => [t(`seller-analytics.outcome.${k}`), d.outcomes[k]]),
    ];
    if (top.data) {
      rows.push([]);
      rows.push([
        t('seller-analytics.top.product'),
        t('seller-analytics.top.sold'),
        t('seller-analytics.top.revenue'),
        t('seller-analytics.top.share'),
        t('seller-analytics.top.conversion'),
      ]);
      top.data.items.forEach((p) => rows.push([p.name, p.sold, p.revenue, p.share, p.conversion]));
    }
    downloadCsv(`phan-tich-${d.period.from}_${d.period.to}.csv`, rows);
  };

  return (
    <div className="dash">
      <div className="flex items-center gap-3 flex-wrap">
        <div>
          <h2 className="m-0 flex items-center gap-[9px] text-2xl font-bold text-strong">
            <Icon name="chart-column" size={22} className="text-brand-500" />
            {t('seller-analytics.title')}
          </h2>
          <p className="m-0 mt-[5px] ml-[31px] text-sm text-muted">
            {t('seller-analytics.subtitle', {
              sub: range === 'custom' ? t('seller-analytics.range_sub.custom') : sub,
            })}
          </p>
        </div>
        <div className="ml-auto flex items-center gap-2 flex-wrap max-md:ml-0">
          {range === 'custom' && (
            <>
              <input
                type="date"
                aria-label={t('seller-analytics.from')}
                value={from}
                max={to || undefined}
                onChange={(e) => setFrom(e.target.value)}
                className={dateCls}
              />
              <input
                type="date"
                aria-label={t('seller-analytics.to')}
                value={to}
                min={from || undefined}
                onChange={(e) => setTo(e.target.value)}
                className={dateCls}
              />
            </>
          )}
          <div className="w-[180px]">
            <Select
              aria-label={t('seller-analytics.range')}
              value={range}
              onChange={setRange}
              options={RANGES.map((r) => ({ value: r, label: t(`seller-analytics.ranges.${r}`) }))}
            />
          </div>
          <Button variant="outline" iconLeft="download" onClick={exportCsv} disabled={!d}>
            {t('seller-analytics.export')}
          </Button>
        </div>
      </div>

      {needDates ? (
        <div className="dash-sheet text-center text-muted">{t('seller-analytics.pick_dates')}</div>
      ) : !d ? (
        <Skeleton lines={10} />
      ) : (
        <>
          <div className="dash-tiles">
            <StatTile
              label={t('seller-analytics.tiles.gross')}
              value={vnd(d.revenue.gross)}
              note={<Delta cur={d.revenue.gross} prev={d.revenue.previous} against={against} />}
            />
            <StatTile
              label={t('seller-analytics.tiles.orders')}
              value={num(d.orders.count)}
              note={<Delta cur={d.orders.count} prev={d.orders.previous} against={against} />}
            />
            <StatTile
              label={t('seller-analytics.tiles.visits')}
              value={num(d.visits.count)}
              note={<Delta cur={d.visits.count} prev={d.visits.previous} against={against} />}
            />
            <StatTile
              label={t('seller-analytics.tiles.conversion')}
              value={pct(conversion(d.orders.count, d.visits.count))}
              note={
                <Delta
                  cur={conversion(d.orders.count, d.visits.count)}
                  prev={conversion(d.orders.previous, d.visits.previous)}
                  against={against}
                />
              }
            />
          </div>

          <div className="dash-cols">
            <section className="dash-sheet">
              <div className="dash-head">
                <h3 className="dash-title">
                  {t(
                    d.chart.granularity === 'HOUR'
                      ? 'seller-analytics.chart.hourly'
                      : 'seller-analytics.chart.daily',
                  )}
                </h3>
                <span className="dash-sub">{sub}</span>
              </div>
              <TimeChart
                kind="bar"
                name={t('seller-analytics.chart.series')}
                data={d.chart.points.map((p) => ({
                  label: pointLabel(p.label, d.chart.granularity, locale),
                  value: p.value,
                }))}
              />
            </section>
            <section className="dash-sheet">
              <div className="dash-head">
                <div>
                  <h3 className="dash-title">{t('seller-analytics.net.title')}</h3>
                  <div className="dash-sub">{t('seller-analytics.net.hint')}</div>
                </div>
              </div>
              <div className="dash-sub">{t('seller-analytics.net.net')}</div>
              <div className="dash-figure mb-3.5">{vnd(d.net.net)}</div>
              <SegBar
                label={t('seller-analytics.net.bar', { sub })}
                format={vnd}
                parts={[
                  { label: t('seller-analytics.net.to_shop'), value: d.net.net, tone: 'ok' },
                  {
                    label: t('seller-analytics.net.commission', {
                      rate: formatNumber(d.net.commissionRate, { locale }),
                    }),
                    value: d.net.commission,
                    tone: 'wait',
                  },
                  {
                    label: t('seller-analytics.net.voucher'),
                    value: d.net.shopVoucherCost,
                    tone: 'held',
                  },
                ]}
              />
              <div className="dash-rows mt-3.5">
                <div className="dash-row">
                  <span>{t('seller-analytics.tiles.gross')}</span>
                  <b>{vnd(d.revenue.gross)}</b>
                </div>
              </div>
            </section>
          </div>

          <div className="dash-cols dash-cols--even">
            <section className="dash-sheet">
              <div className="dash-head">
                <h3 className="dash-title">{t('seller-analytics.outcomes.title')}</h3>
                <span className="dash-sub">
                  {t('seller-analytics.outcomes.sub', { count: num(d.orders.count), sub })}
                </span>
              </div>
              <SegBar
                label={t('seller-analytics.outcomes.bar', { sub })}
                parts={OUTCOME_ORDER.map(([k, tone]) => ({
                  label: t(`seller-analytics.outcome.${k}`),
                  tone,
                  value: d.outcomes[k],
                }))}
              />
              <div className="dash-rows mt-3.5">
                <div className="dash-row">
                  <span>{t('seller-analytics.health.shop_cancel')}</span>
                  <b>{pct(shopCancelRate(d.outcomes, d.orders.count))}</b>
                </div>
                <div className="dash-row">
                  <span>{t('seller-analytics.health.lost_disputes')}</span>
                  <b>{num(d.health.lostDisputes)}</b>
                </div>
                <div className="dash-row">
                  <span>{t('seller-analytics.health.auto_confirmed')}</span>
                  <b>{num(d.health.autoConfirmedOrders)}</b>
                </div>
              </div>
            </section>
            <section className="dash-sheet">
              <div className="dash-head">
                <h3 className="dash-title">{t('seller-analytics.sources.title')}</h3>
                <span className="dash-sub">{t('seller-analytics.range_sub.30d')}</span>
              </div>
              {!sources.data ? (
                <Skeleton lines={4} />
              ) : (
                <ol className="dash-bars">
                  {sources.data.map((s, i) => {
                    const max = Math.max(...sources.data.map((x) => x.share));
                    return (
                      <li key={s.source} data-lead={i === 0 ? 1 : 0}>
                        <div className="dash-bar__top">
                          <span>{t(`seller-analytics.sources.${s.source}`)}</span>
                          <b>
                            {t('seller-analytics.sources.visits', {
                              visits: num(s.visits),
                              share: num(s.share),
                            })}
                          </b>
                        </div>
                        <div className="dash-bar__track">
                          <div
                            className="dash-bar__fill"
                            style={{ width: `${(s.share / max) * 100}%` }}
                          />
                        </div>
                        <div className="dash-sub mt-[3px]">
                          {t('seller-analytics.sources.conversion', { rate: pct(s.conversion) })}
                        </div>
                      </li>
                    );
                  })}
                </ol>
              )}
            </section>
          </div>

          {sent.data && (
            <section className="dash-sheet">
              <div className="dash-head">
                <div>
                  <h3 className="dash-title">{t('seller-analytics.sentiment.title')}</h3>
                  <div className="dash-sub">
                    {t('seller-analytics.sentiment.sub', { count: num(sent.data.analyzed), sub })}
                  </div>
                </div>
                <div className="dash-end">
                  <Link to="/reviews" className={buttonClass({ variant: 'ghost', size: 'sm' })}>
                    {t('seller-analytics.sentiment.see')}
                    <Icon name="chevron-right" size={16} />
                  </Link>
                </div>
              </div>
              <SegBar
                label={t('seller-analytics.sentiment.bar', { sub })}
                parts={[
                  {
                    label: t('seller-analytics.sentiment.positive'),
                    value: sent.data.positive,
                    tone: 'ok',
                  },
                  {
                    label: t('seller-analytics.sentiment.neutral'),
                    value: sent.data.neutral,
                    tone: 'info',
                  },
                  {
                    label: t('seller-analytics.sentiment.negative'),
                    value: sent.data.negative,
                    tone: 'bad',
                  },
                ]}
              />
              <div className="dash-rows mt-3.5">
                <div className="dash-row">
                  <span>{t('seller-analytics.sentiment.rate')}</span>
                  <b>
                    {pct(positiveRate(sent.data))}{' '}
                    <Delta cur={sent.data.positive} prev={sent.data.previousPositive} />
                  </b>
                </div>
                {sent.data.worst && (
                  <div className="dash-row max-md:flex-col max-md:gap-1">
                    <span>{t('seller-analytics.sentiment.worst')}</span>
                    <b>
                      {t('seller-analytics.sentiment.worst_value', {
                        name: sent.data.worst.name,
                        count: sent.data.worst.negative,
                      })}
                    </b>
                  </div>
                )}
              </div>
            </section>
          )}

          <section className="dash-sheet dash-sheet--flush">
            <div className="dash-head">
              <h3 className="dash-title">{t('seller-analytics.top.title')}</h3>
              {top.data && (
                <span className="dash-sub">
                  {t('seller-analytics.top.sub', {
                    total: formatMoneyShort(month.data?.revenue.gross ?? top.data.totalRevenue, {
                      locale,
                    }),
                  })}
                </span>
              )}
            </div>
            <div className="dash-tablewrap">
              {!top.data ? (
                <Skeleton lines={5} className="m-4" />
              ) : (
                <table className="dash-table">
                  <thead>
                    <tr>
                      <th>{t('seller-analytics.top.product')}</th>
                      <th className="num">{t('seller-analytics.top.sold')}</th>
                      <th className="num">{t('seller-analytics.top.revenue')}</th>
                      <th className="num">{t('seller-analytics.top.share')}</th>
                      <th className="num">{t('seller-analytics.top.conversion')}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {top.data.items.map((p) => (
                      <tr key={p.id}>
                        <td>
                          <div className="flex items-center gap-2.5">
                            <img
                              src={p.imageUrl}
                              alt=""
                              width={36}
                              height={36}
                              className="w-9 h-9 rounded-sm object-cover flex-none"
                            />
                            <span>{p.name}</span>
                          </div>
                        </td>
                        <td className="num">{num(p.sold)}</td>
                        <td className="num font-semibold">{vnd(p.revenue)}</td>
                        <td className="num">
                          <span className="dash-share">
                            <span>
                              <i style={{ width: `${p.share}%` }} />
                            </span>
                            <b>{pct(p.share)}</b>
                          </span>
                        </td>
                        <td className="num">{pct(p.conversion)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </section>
        </>
      )}
    </div>
  );
}
