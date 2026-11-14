// Sổ cái tài chính (prototype admin/Finance.jsx Ledger) — plan G12.28: "Append-only · bất biến", tìm mã
// tham chiếu / tài khoản, lọc sự kiện, bảng Seq / Thời gian / Sự kiện / Tham chiếu / Tài khoản / Nợ / Có,
// "Tải thêm" theo con trỏ (API 11.9 limit 50), Xuất sổ cái CSV (mọi dòng khớp bộ lọc).
import { useEffect, useState } from 'react';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { downloadCsv } from '@/shared/lib/csv';
import { formatDateTime, formatVND } from '@/shared/lib/format';
import { Badge, Button, Icon, Input, Select, Skeleton, toast } from '@/shared/ui';
import { listAllLedger, useLedger } from '../application/useFinance';
import { EVENT_TONE, LEDGER_EVENTS } from '../domain/finance.rules';
import type { LedgerEvent } from '../domain/finance.types';

const th = 'px-3 py-[11px] whitespace-nowrap font-semibold text-xs text-muted text-left bg-gray-25';
const td = 'px-3 py-3 whitespace-nowrap';

export default function LedgerPage() {
  const { t, locale } = useT();
  const [q, setQ] = useState('');
  const [term, setTerm] = useState('');
  const [ev, setEv] = useState<LedgerEvent | ''>('');
  const [exporting, setExporting] = useState(false);
  useEffect(() => {
    const id = setTimeout(() => setTerm(q.trim()), 300);
    return () => clearTimeout(id);
  }, [q]);
  const filter = { eventType: ev || undefined, q: term || undefined };
  const ledger = useLedger(filter);
  const rows = ledger.data?.pages.flatMap((p) => p.items) ?? [];
  const money = (v: number | null) => (v ? formatVND(v, { locale }) : '—');

  const exportCsv = async () => {
    setExporting(true);
    try {
      const all = await listAllLedger(filter);
      downloadCsv('so-cai-tai-chinh.csv', [
        [
          'Seq',
          t('admin-finance.ledger.col.time'),
          t('admin-finance.ledger.col.event'),
          t('admin-finance.ledger.col.ref'),
          t('admin-finance.ledger.col.account'),
          t('admin-finance.ledger.col.debit'),
          t('admin-finance.ledger.col.credit'),
        ],
        ...all.map((r) => [
          r.seq,
          formatDateTime(r.createdAt, { locale }),
          r.eventType,
          r.reference,
          r.account,
          r.debit,
          r.credit,
        ]),
      ]);
    } catch (e) {
      toast.error(errorMessage(e, t));
    } finally {
      setExporting(false);
    }
  };

  return (
    <div className="max-w-[1180px]">
      <div className="flex items-center gap-2.5 mb-3.5 flex-wrap">
        <Badge variant="neutral" icon="lock">
          {t('admin-finance.ledger.append_only')}
        </Badge>
        <span className="text-sm text-muted">{t('admin-finance.ledger.desc')}</span>
        <Button
          variant="secondary"
          size="sm"
          iconLeft="download"
          className="ml-auto"
          loading={exporting}
          onClick={() => void exportCsv()}
        >
          {t('admin-finance.ledger.export')}
        </Button>
      </div>
      <div className="flex gap-3 mb-3 flex-wrap">
        <Input
          aria-label={t('admin-finance.ledger.search')}
          wrapperClassName="flex-1 max-w-[360px] min-w-[220px]"
          iconLeft="search"
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder={t('admin-finance.ledger.search_placeholder')}
        />
        <Select<LedgerEvent | ''>
          className="w-[240px] max-sm:w-full"
          aria-label={t('admin-finance.ledger.event_filter')}
          value={ev}
          onChange={setEv}
          options={[
            { value: '', label: t('admin-finance.ledger.all_events') },
            ...LEDGER_EVENTS.map((x) => ({
              value: x,
              label: `${x} · ${t(`admin-finance.ledger.events.${x}`)}`,
            })),
          ]}
        />
      </div>
      <div className="bg-surface-card rounded-lg shadow-sm overflow-x-auto">
        {ledger.isPending ? (
          <Skeleton lines={6} className="m-5" />
        ) : (
          <table className="w-full min-w-[900px] border-collapse tabular-nums">
            <thead>
              <tr>
                <th className={th}>Seq</th>
                <th className={th}>{t('admin-finance.ledger.col.time')}</th>
                <th className={th}>{t('admin-finance.ledger.col.event')}</th>
                <th className={th}>{t('admin-finance.ledger.col.ref')}</th>
                <th className={th}>{t('admin-finance.ledger.col.account')}</th>
                <th className={cn(th, 'text-right')}>{t('admin-finance.ledger.col.debit')}</th>
                <th className={cn(th, 'text-right')}>{t('admin-finance.ledger.col.credit')}</th>
              </tr>
            </thead>
            <tbody>
              {rows.length === 0 && (
                <tr>
                  <td colSpan={7} className="p-8 text-center text-base text-muted">
                    {t('admin-finance.ledger.empty')}
                  </td>
                </tr>
              )}
              {rows.map((r) => (
                <tr
                  key={r.seq}
                  className="border-0 border-t border-solid border-line-subtle text-[13px] text-body"
                >
                  <td className={cn(td, 'font-mono text-subtle')}>#{r.seq}</td>
                  <td className={cn(td, 'font-mono text-muted text-xs')}>
                    {formatDateTime(r.createdAt, { locale })}
                  </td>
                  <td className={td}>
                    <Badge
                      variant={EVENT_TONE[r.eventType]}
                      title={t(`admin-finance.ledger.events.${r.eventType}`)}
                    >
                      {r.eventType}
                    </Badge>
                  </td>
                  <td className={cn(td, 'font-mono text-xs')}>{r.reference}</td>
                  <td className={td}>{t(`admin-finance.ledger.accounts.${r.account}`)}</td>
                  <td
                    className={cn(
                      td,
                      'text-right',
                      r.debit ? 'text-strong font-semibold' : 'text-disabled',
                    )}
                  >
                    {money(r.debit)}
                  </td>
                  <td
                    className={cn(
                      td,
                      'text-right',
                      r.credit ? 'text-strong font-semibold' : 'text-disabled',
                    )}
                  >
                    {money(r.credit)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
      {ledger.hasNextPage && (
        <div className="text-center mt-3">
          <Button
            variant="outline"
            size="sm"
            loading={ledger.isFetchingNextPage}
            onClick={() => void ledger.fetchNextPage()}
          >
            <Icon name="chevron-down" size={16} />
            {t('admin-finance.ledger.more')}
          </Button>
        </div>
      )}
    </div>
  );
}
