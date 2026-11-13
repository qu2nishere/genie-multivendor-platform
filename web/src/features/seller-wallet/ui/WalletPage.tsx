// Ví & Escrow (prototype Wallet.jsx) — plan G11.34: số dư khả dụng / đang giữ / doanh thu 30 ngày, chờ đối
// soát COD, đang rút, "Cách tính tiền về ví" (Flow / Op), sổ giao dịch có lọc tháng / loại / tìm, xuất CSV.
// ?withdraw=1 (nút "Rút tiền" ở Tổng quan) mở ngay hộp rút tiền.
import { useEffect, useState, type ReactNode } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { downloadCsv } from '@/shared/lib/csv';
import { formatDateTime, formatNumber, formatVND } from '@/shared/lib/format';
import { Badge, Button, Icon, Input, Select, Skeleton, StatusBadge } from '@/shared/ui';
import { Delta } from '@/shared/ui/dash';
import { useLedger, useWallet } from '../application/useWallet';
import { csvName, monthLabel, STATE_VIEW, TX_TYPES, TYPE_TONE } from '../domain/wallet.rules';
import type { TxType } from '../domain/wallet.types';
import { WithdrawModal } from './WithdrawModal';

const bal = 'rounded-lg px-5 py-[18px] shadow-sm';
const th = 'px-3 py-2.5 whitespace-nowrap';
const td = 'px-3 py-3 whitespace-nowrap';

function Flow({
  label,
  value,
  cls,
  bg,
}: {
  label: string;
  value: string;
  cls: string;
  bg?: string;
}) {
  return (
    <div className={cn('rounded-md px-3.5 py-2.5 min-w-[150px] max-sm:w-full', bg ?? 'bg-gray-50')}>
      <div className="text-xs text-muted mb-1">{label}</div>
      <div className={cn('font-bold text-lg tabular-nums', cls)}>{value}</div>
    </div>
  );
}
const Op = ({ children }: { children: ReactNode }) => (
  <span className="font-bold text-xl text-subtle max-sm:hidden" aria-hidden="true">
    {children}
  </span>
);

export default function WalletPage() {
  const { t, locale } = useT();
  const [params, setParams] = useSearchParams();
  const wallet = useWallet();
  const [open, setOpen] = useState(params.get('withdraw') === '1');
  const [filterOpen, setFilterOpen] = useState(false);
  const [month, setMonth] = useState('2026-06');
  const [type, setType] = useState<TxType | null>(null);
  const [q, setQ] = useState('');
  const ledger = useLedger({ month, type, q });
  const vnd = (n: number) => formatVND(n, { locale });

  useEffect(() => {
    if (params.get('withdraw')) setParams({}, { replace: true });
  }, [params, setParams]);

  const w = wallet.data;
  if (!w) return <Skeleton lines={10} />;
  const ex = w.commissionExample;
  const rate = formatNumber(ex.rate, { locale });
  const rows = ledger.data?.items ?? [];
  const months = ledger.data?.months ?? [month];

  const exportCsv = () =>
    downloadCsv(csvName(month), [
      [
        t('seller-wallet.ledger.col.time'),
        t('seller-wallet.ledger.col.ref'),
        t('seller-wallet.ledger.col.type'),
        t('seller-wallet.ledger.col.desc'),
        'Gross',
        t('seller-wallet.ledger.col.commission'),
        'Net',
      ],
      ...rows.map((r) => [
        formatDateTime(r.createdAt, { locale }),
        r.reference,
        t(`seller-wallet.type.${r.type}`),
        r.description,
        r.gross,
        r.commission,
        r.net,
      ]),
    ]);

  return (
    <div className="max-w-[1180px]">
      <div className="grid grid-cols-3 gap-4 mb-4 max-lg:grid-cols-1">
        <div
          className={cn(
            bal,
            'bg-[linear-gradient(135deg,var(--ink-800),var(--ink-900))] text-white',
          )}
        >
          <div className="flex items-center gap-2">
            <Icon name="wallet" size={18} className="text-mint-500" />
            <span className="font-semibold text-[13px] text-white/80">
              {t('seller-wallet.balance.available')}
            </span>
          </div>
          <div className="font-extrabold text-[30px] mt-3 mb-1 tabular-nums max-sm:text-2xl">
            {vnd(w.available)}
          </div>
          <div className="text-xs text-white/60">{t('seller-wallet.balance.available_hint')}</div>
          <Button block className="mt-4" iconLeft="hand-coins" onClick={() => setOpen(true)}>
            {t('seller-wallet.withdraw.title')}
          </Button>
        </div>
        <div className={cn(bal, 'bg-amber-50 border border-solid border-[#FCE3B0]')}>
          <div className="flex items-center gap-2">
            <Icon name="lock" size={18} className="text-amber-600" />
            <span className="font-semibold text-[13px] text-amber-600">
              {t('seller-wallet.balance.held')}
            </span>
          </div>
          <div className="font-extrabold text-[30px] mt-3 mb-1 text-strong tabular-nums max-sm:text-2xl">
            {vnd(w.held)}
          </div>
          <div className="text-xs text-muted">{t('seller-wallet.balance.held_hint')}</div>
          <div className="mt-4 flex items-center gap-1.5 text-xs text-amber-600">
            <Icon name="clock" size={14} /> {t('seller-wallet.balance.held_auto')}
          </div>
        </div>
        <div className={cn(bal, 'bg-surface-card border border-solid border-line-subtle')}>
          <div className="flex items-center gap-2">
            <Icon name="chart-column" size={18} className="text-primary" />
            <span className="font-semibold text-[13px] text-muted">
              {t('seller-wallet.balance.revenue')}
            </span>
          </div>
          <div className="font-extrabold text-[30px] mt-3 mb-1 text-strong tabular-nums max-sm:text-2xl">
            {vnd(w.revenue30d.value)}
          </div>
          <div className="text-xs text-muted">
            <Delta
              cur={w.revenue30d.value}
              prev={w.revenue30d.previous}
              against={t('seller-wallet.balance.revenue_against')}
            />
          </div>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-4 mb-4 max-md:grid-cols-1">
        <div className={cn(bal, 'bg-surface-card border border-solid border-line-subtle')}>
          <div className="font-semibold text-[13px] text-muted">
            {t('seller-wallet.balance.cod')}
          </div>
          <div className="font-extrabold text-2xl mt-2 mb-0.5 text-strong tabular-nums">
            {vnd(w.codPending)}
          </div>
          <div className="text-xs text-muted">{t('seller-wallet.balance.cod_hint')}</div>
        </div>
        <div className={cn(bal, 'bg-surface-card border border-solid border-line-subtle')}>
          <div className="font-semibold text-[13px] text-muted">
            {t('seller-wallet.balance.pending')}
          </div>
          <div className="font-extrabold text-2xl mt-2 mb-0.5 text-strong tabular-nums">
            {vnd(w.pendingWithdrawal)}
          </div>
          <div className="text-xs text-muted">{t('seller-wallet.balance.pending_hint')}</div>
        </div>
      </div>

      <section
        className="bg-surface-card rounded-lg shadow-sm px-5 py-[18px] mb-4"
        aria-labelledby="wallet-how"
      >
        <div className="flex items-center gap-2 mb-3.5 flex-wrap">
          <Icon name="receipt" size={18} className="text-primary" />
          <h2 id="wallet-how" className="m-0 font-bold text-base text-strong">
            {t('seller-wallet.how.title')}
          </h2>
          <Badge variant="outline">{t('seller-wallet.how.rate', { rate })}</Badge>
        </div>
        <div className="flex items-center gap-3.5 flex-wrap">
          <Flow label={t('seller-wallet.how.gross')} value={vnd(ex.gross)} cls="text-strong" />
          <Op>−</Op>
          <Flow
            label={t('seller-wallet.how.commission', { rate })}
            value={vnd(ex.commission)}
            cls="text-gray-500"
          />
          <Op>=</Op>
          <Flow
            label={t('seller-wallet.how.net')}
            value={vnd(ex.net)}
            cls="text-amber-600"
            bg="bg-amber-50"
          />
          <Op>→</Op>
          <Flow
            label={t('seller-wallet.how.release')}
            value={vnd(ex.net)}
            cls="text-green-600"
            bg="bg-green-50"
          />
        </div>
      </section>

      <section
        className="bg-surface-card rounded-lg shadow-sm overflow-hidden"
        aria-labelledby="wallet-ledger"
      >
        <div className="flex items-center gap-2.5 px-5 py-4 border-0 border-b border-solid border-line-subtle flex-wrap">
          <h2 id="wallet-ledger" className="m-0 font-bold text-base text-strong">
            {t('seller-wallet.ledger.title')}
          </h2>
          <Badge variant="neutral" icon="lock">
            {t('seller-wallet.ledger.append_only')}
          </Badge>
          <span className="text-xs text-muted">{monthLabel(month)}</span>
          <div className="ml-auto flex gap-2">
            <Button
              variant="ghost"
              size="sm"
              iconLeft="sliders-horizontal"
              aria-expanded={filterOpen}
              onClick={() => setFilterOpen((o) => !o)}
            >
              {t('seller-wallet.ledger.filter')}
            </Button>
            <Button
              variant="secondary"
              size="sm"
              iconLeft="download"
              disabled={!rows.length}
              onClick={exportCsv}
            >
              {t('seller-wallet.ledger.export')}
            </Button>
          </div>
        </div>
        {filterOpen && (
          <div className="flex gap-3 px-5 py-3 border-0 border-b border-solid border-line-subtle bg-gray-25 flex-wrap">
            <div className="w-[130px]">
              <Select
                aria-label={t('seller-wallet.ledger.month')}
                value={month}
                onChange={setMonth}
                options={months.map((m) => ({ value: m, label: monthLabel(m) }))}
              />
            </div>
            <div className="w-[200px]">
              <Select
                aria-label={t('seller-wallet.ledger.type')}
                value={type ?? 'ALL'}
                onChange={(v) => setType(v === 'ALL' ? null : (v as TxType))}
                options={[
                  { value: 'ALL', label: t('seller-wallet.ledger.all') },
                  ...TX_TYPES.map((x) => ({ value: x, label: t(`seller-wallet.type.${x}`) })),
                ]}
              />
            </div>
            <div className="flex-1 max-w-[320px] min-w-[200px]">
              <Input
                iconLeft="search"
                aria-label={t('seller-wallet.ledger.search')}
                value={q}
                onChange={(e) => setQ(e.target.value)}
                placeholder={t('seller-wallet.ledger.search')}
              />
            </div>
          </div>
        )}
        <div className="overflow-x-auto">
          <table className="w-full min-w-[880px] border-collapse tabular-nums">
            <thead>
              <tr className="text-xs font-semibold text-muted text-left bg-gray-25">
                <th scope="col" className={th}>
                  {t('seller-wallet.ledger.col.time')}
                </th>
                <th scope="col" className={th}>
                  {t('seller-wallet.ledger.col.ref')}
                </th>
                <th scope="col" className={th}>
                  {t('seller-wallet.ledger.col.type')}
                </th>
                <th scope="col" className={th}>
                  {t('seller-wallet.ledger.col.desc')}
                </th>
                <th scope="col" className={`${th} text-right`}>
                  Gross
                </th>
                <th scope="col" className={`${th} text-right`}>
                  {t('seller-wallet.ledger.col.commission')}
                </th>
                <th scope="col" className={`${th} text-right`}>
                  Net
                </th>
                <th scope="col" className={`${th} text-center`}>
                  {t('seller-wallet.ledger.col.state')}
                </th>
              </tr>
            </thead>
            <tbody>
              {!ledger.data ? (
                <tr>
                  <td colSpan={8} className="p-4">
                    <Skeleton lines={5} />
                  </td>
                </tr>
              ) : rows.length === 0 ? (
                <tr>
                  <td colSpan={8} className="p-9 text-center text-base text-muted">
                    {t('seller-wallet.ledger.empty')}
                  </td>
                </tr>
              ) : (
                rows.map((r) => {
                  const neg = r.net < 0;
                  const sv = STATE_VIEW[r.state];
                  return (
                    <tr
                      key={r.id}
                      className="border-0 border-t border-solid border-line-subtle text-[13px] text-body"
                    >
                      <td className={`${td} text-muted text-xs`}>
                        {formatDateTime(r.createdAt, { locale })}
                      </td>
                      <td className={td}>
                        <span className="code text-xs">{r.reference}</span>
                      </td>
                      <td className={td}>
                        <Badge variant={TYPE_TONE[r.type]}>
                          {t(`seller-wallet.type.${r.type}`)}
                        </Badge>
                      </td>
                      <td className={`${td} text-muted`}>{r.description}</td>
                      <td className={`${td} text-right font-medium`}>
                        {r.gross ? vnd(r.gross) : '—'}
                      </td>
                      <td className={`${td} text-right text-gray-500`}>
                        {r.commission ? vnd(-Math.abs(r.commission)) : '—'}
                      </td>
                      <td
                        className={cn(
                          td,
                          'text-right font-bold',
                          neg ? 'text-red-600' : 'text-green-600',
                        )}
                      >
                        {neg ? '' : '+'}
                        {vnd(r.net)}
                      </td>
                      <td className={`${td} text-center`}>
                        {sv.kind === 'status' ? (
                          <StatusBadge
                            status={sv.status}
                            label={t(`seller-wallet.state.${r.state}`)}
                            dot={false}
                          />
                        ) : (
                          <Badge variant={sv.variant}>{t(`seller-wallet.state.${r.state}`)}</Badge>
                        )}
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </section>
      {open && <WithdrawModal wallet={w} onClose={() => setOpen(false)} />}
    </div>
  );
}
