// Đối soát COD (prototype admin/CodReconciliation.jsx; UC-LOG-03) — plan G12.30: danh sách phiên (?id=),
// chi tiết "Đã nhận … từ …", Tải bảng kê CSV (3 cột) / Dùng bảng kê mẫu → preview server, bảng Mã vận
// đơn / Đơn hàng / Bảng kê / Trên đơn / Phí / Kết quả, dải tổng (cân / lệch), Xuất kết quả CSV,
// Xác nhận đối soát (chỉ khi cân và có ≥ 1 dòng khớp). Phiên đã xác nhận hiện kết quả đã lưu.
import { useEffect, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { downloadCsv } from '@/shared/lib/csv';
import { formatDateTime, formatVND } from '@/shared/lib/format';
import { Badge, Button, EmptyState, Icon, Skeleton, toast } from '@/shared/ui';
import {
  useCodRemittance,
  useCodRemittances,
  useConfirmReconciliation,
  usePreviewReconciliation,
} from '../application/useFinance';
import { canConfirm, parseCodCsv } from '../domain/cod.rules';
import type { CodRemittanceDetail, Reconciliation, StatementLine } from '../domain/finance.types';

const th = 'px-4 py-[11px] whitespace-nowrap font-semibold text-xs text-muted text-left bg-gray-25';
const td = 'px-4 py-[11px] whitespace-nowrap';
const RESULT_TONE = { MATCH: 'success', AMOUNT_MISMATCH: 'danger', NOT_FOUND: 'danger' } as const;
const MAX_CSV = 1024 * 1024;

function Detail({ s }: { s: CodRemittanceDetail }) {
  const { t, locale } = useT();
  const vnd = (v: number) => formatVND(v, { locale });
  const preview = usePreviewReconciliation();
  const confirm = useConfirmReconciliation();
  const fileRef = useRef<HTMLInputElement>(null);
  const [draft, setDraft] = useState<{
    file: string;
    lines: StatementLine[];
    result: Reconciliation;
  } | null>(null);
  const [err, setErr] = useState('');
  useEffect(() => {
    setDraft(null);
    setErr('');
  }, [s.id]);
  const done = s.status === 'DONE';
  const result = done ? s.confirmed : (draft?.result ?? null);
  const fileName = done ? (s.statementFileName ?? '') : (draft?.file ?? '');

  const run = (file: string, lines: StatementLine[]) =>
    preview.mutate(
      { id: s.id, fileName: file, lines },
      {
        onSuccess: (r) => {
          setErr('');
          setDraft({ file, lines, result: r });
        },
        onError: (e) => toast.error(errorMessage(e, t)),
      },
    );

  const load = (f: File | undefined) => {
    if (!f) return;
    if (!/\.csv$/i.test(f.name)) return setErr(t('admin-finance.cod.err.type'));
    if (f.size > MAX_CSV) return setErr(t('admin-finance.cod.err.size'));
    const reader = new FileReader();
    reader.onload = () => {
      const parsed = parseCodCsv(String(reader.result));
      if (parsed.lines.length === 0) return setErr(t('admin-finance.cod.err.empty'));
      if (parsed.errors.length)
        return setErr(
          t('admin-finance.cod.err.rows', { rows: parsed.errors.slice(0, 5).join(', ') }),
        );
      run(f.name, parsed.lines);
    };
    reader.readAsText(f);
  };

  const gross = result?.lines.reduce((n, l) => n + l.amount, 0) ?? 0;
  const fees = result?.lines.reduce((n, l) => n + l.fee, 0) ?? 0;
  const flagged = result ? result.lines.length - result.totals.matchedCount : 0;
  const resultLabel = (r: keyof typeof RESULT_TONE) => t(`admin-finance.cod.result.${r}`);

  return (
    <section
      className="bg-surface-card rounded-lg shadow-sm overflow-hidden"
      aria-labelledby="cod-detail"
    >
      <div className="flex items-center gap-3 px-5 py-4 border-0 border-b border-solid border-line-subtle flex-wrap">
        <div className="flex-1 min-w-[220px]">
          <h2 id="cod-detail" className="m-0 text-lg font-bold text-strong">
            {t('admin-finance.cod.received', { amount: vnd(s.receivedAmount), carrier: s.carrier })}
          </h2>
          <div className="text-xs text-muted mt-0.5">
            {t('admin-finance.cod.sepay', { at: formatDateTime(s.receivedAt, { locale }) })}{' '}
            <span className="font-mono">{s.bankReference}</span>
          </div>
        </div>
        <input
          ref={fileRef}
          type="file"
          hidden
          accept=".csv,text/csv"
          onChange={(e) => {
            load(e.target.files?.[0]);
            e.target.value = '';
          }}
        />
        {!done && (
          <>
            {s.sampleLines.length > 0 && (
              <Button
                size="sm"
                variant="ghost"
                iconLeft="file-text"
                onClick={() => run('bang-ke-mau.csv', s.sampleLines)}
              >
                {t('admin-finance.cod.sample')}
              </Button>
            )}
            <Button
              size="sm"
              variant="secondary"
              iconLeft="upload"
              loading={preview.isPending}
              onClick={() => fileRef.current?.click()}
            >
              {t('admin-finance.cod.upload')}
            </Button>
          </>
        )}
      </div>
      {err && (
        <div role="alert" className="flex items-center gap-1.5 px-5 py-2.5 text-sm text-red-600">
          <Icon name="circle-alert" size={15} />
          {err}
        </div>
      )}
      {!result ? (
        <div className="px-5 py-14 text-center text-base text-muted">
          {t('admin-finance.cod.no_statement')}
        </div>
      ) : (
        <>
          <div
            className={cn(
              'flex gap-2.5 items-center px-5 py-3 text-sm text-body flex-wrap',
              result.totals.balanced ? 'bg-mint-50' : 'bg-red-50',
            )}
          >
            <Icon
              name={result.totals.balanced ? 'circle-check' : 'triangle-alert'}
              size={17}
              className={cn('flex-none', result.totals.balanced ? 'text-mint-600' : 'text-red-600')}
            />
            <span>
              {t('admin-finance.cod.totals', {
                file: fileName,
                gross: vnd(gross),
                fees: vnd(fees),
                net: vnd(result.totals.statementNet),
              })}{' '}
              {result.totals.balanced
                ? t('admin-finance.cod.balanced')
                : t('admin-finance.cod.off', {
                    diff: vnd(Math.abs(result.totals.statementNet - result.totals.receivedAmount)),
                  })}
            </span>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full min-w-[640px] border-collapse tabular-nums">
              <thead>
                <tr>
                  <th className={th}>{t('admin-finance.cod.col.tracking')}</th>
                  <th className={th}>{t('admin-finance.cod.col.order')}</th>
                  <th className={cn(th, 'text-right')}>{t('admin-finance.cod.col.statement')}</th>
                  <th className={cn(th, 'text-right')}>{t('admin-finance.cod.col.on_order')}</th>
                  <th className={cn(th, 'text-right')}>{t('admin-finance.cod.col.fee')}</th>
                  <th className={th}>{t('admin-finance.cod.col.result')}</th>
                </tr>
              </thead>
              <tbody>
                {result.lines.map((l, i) => (
                  <tr
                    key={`${l.trackingCode}-${i}`}
                    className="border-0 border-t border-solid border-line-subtle text-[13px] text-body"
                  >
                    <td className={cn(td, 'font-mono')}>{l.trackingCode}</td>
                    <td className={cn(td, 'font-mono')}>{l.orderCode ?? '—'}</td>
                    <td className={cn(td, 'text-right')}>{vnd(l.amount)}</td>
                    <td className={cn(td, 'text-right')}>
                      {l.orderAmount != null ? vnd(l.orderAmount) : '—'}
                    </td>
                    <td className={cn(td, 'text-right')}>{vnd(l.fee)}</td>
                    <td className={td}>
                      <Badge variant={RESULT_TONE[l.result]}>{resultLabel(l.result)}</Badge>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="flex items-center gap-3 px-5 py-3.5 border-0 border-t border-solid border-line-subtle flex-wrap">
            {done ? (
              <span className="inline-flex items-center gap-1.5 text-sm text-mint-600">
                <Icon name="circle-check" size={16} />
                {t('admin-finance.cod.done', { count: result.totals.matchedCount })}
              </span>
            ) : (
              <span className="text-sm text-muted">
                {t('admin-finance.cod.will_fund', { count: result.totals.matchedCount })}
                {flagged > 0 && (
                  <b className="text-red-600">
                    {' '}
                    {t('admin-finance.cod.flagged', { count: flagged })}
                  </b>
                )}
              </span>
            )}
            <span className="ml-auto flex gap-2">
              <Button
                size="sm"
                variant="ghost"
                iconLeft="download"
                onClick={() =>
                  downloadCsv(`doi-soat-${s.code}.csv`, [
                    [
                      t('admin-finance.cod.col.tracking'),
                      t('admin-finance.cod.col.order'),
                      t('admin-finance.cod.col.statement'),
                      t('admin-finance.cod.col.on_order'),
                      t('admin-finance.cod.col.fee'),
                      t('admin-finance.cod.col.result'),
                    ],
                    ...result.lines.map((l) => [
                      l.trackingCode,
                      l.orderCode,
                      l.amount,
                      l.orderAmount,
                      l.fee,
                      resultLabel(l.result),
                    ]),
                  ])
                }
              >
                {t('admin-finance.cod.export')}
              </Button>
              {!done && draft && (
                <Button
                  size="sm"
                  iconLeft="check"
                  disabled={!canConfirm(result)}
                  loading={confirm.isPending}
                  onClick={() =>
                    confirm.mutate(
                      { id: s.id, fileName: draft.file, lines: draft.lines },
                      {
                        onSuccess: (n) =>
                          toast.success(t('admin-finance.cod.confirmed', { count: n })),
                        onError: (e) => toast.error(errorMessage(e, t)),
                      },
                    )
                  }
                >
                  {t('admin-finance.cod.confirm')}
                </Button>
              )}
            </span>
          </div>
        </>
      )}
    </section>
  );
}

export default function CodReconciliationPage() {
  const { t, locale } = useT();
  const list = useCodRemittances();
  const [params, setParams] = useSearchParams();
  const rows = list.data ?? [];
  const selId = Number(params.get('id')) || rows[0]?.id || null;
  const detail = useCodRemittance(selId);
  if (list.isPending) return <Skeleton lines={8} />;
  if (list.isError)
    return (
      <EmptyState
        icon="triangle-alert"
        title={t('common.state.error')}
        text={errorMessage(list.error, t)}
      />
    );
  const waiting = rows.filter((s) => s.status === 'WAITING');
  return (
    <div className="max-w-[1180px] grid grid-cols-[340px_minmax(0,1fr)] gap-[18px] items-start max-lg:grid-cols-1">
      <section
        className="bg-surface-card rounded-lg shadow-sm overflow-hidden"
        aria-labelledby="cod-list"
      >
        <div className="px-4 py-3.5 border-0 border-b border-solid border-line-subtle">
          <h2 id="cod-list" className="m-0 text-sm font-bold text-strong">
            {t('admin-finance.cod.sessions')}
          </h2>
          <div className="text-xs text-muted mt-0.5">
            {t('admin-finance.cod.sessions_sub', {
              count: waiting.length,
              amount: formatVND(
                waiting.reduce((n, s) => n + s.receivedAmount, 0),
                { locale },
              ),
            })}
          </div>
        </div>
        <ul className="list-none m-0 p-0">
          {rows.map((s) => {
            const on = s.id === selId;
            return (
              <li key={s.id}>
                <button
                  type="button"
                  aria-current={on ? 'true' : undefined}
                  onClick={() => setParams({ id: String(s.id) }, { replace: true })}
                  className={cn(
                    'block w-full text-left border-0 border-b border-solid border-line-subtle px-4 py-3 cursor-pointer focus-visible:outline-none focus-visible:shadow-focus',
                    on ? 'bg-blue-50' : 'bg-surface-card hover:bg-gray-50',
                  )}
                >
                  <span className="flex items-center gap-2">
                    <span className="flex-1 font-mono text-[13px] font-semibold text-strong">
                      {s.code}
                    </span>
                    <Badge variant={s.status === 'DONE' ? 'success' : 'warning'}>
                      {t(`admin-finance.cod.status.${s.status}`)}
                    </Badge>
                  </span>
                  <span className="block text-xs text-muted mt-[3px]">
                    {s.carrier} · {formatVND(s.receivedAmount, { locale })} ·{' '}
                    {formatDateTime(s.receivedAt, { locale })}
                  </span>
                </button>
              </li>
            );
          })}
        </ul>
      </section>
      {selId == null ? null : detail.data ? (
        <Detail s={detail.data} />
      ) : (
        <div className="bg-surface-card rounded-lg shadow-sm p-6">
          <Skeleton lines={6} />
        </div>
      )}
    </div>
  );
}
