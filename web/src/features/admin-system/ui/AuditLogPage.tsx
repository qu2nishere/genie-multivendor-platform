// Nhật ký hệ thống (prototype admin/AuditLog.jsx) — plan G12.43: "Append-only · bất biến", lọc khoảng thời
// gian (Hôm nay / 7 ngày / 30 ngày / Tuỳ chọn), nhóm hành động, tìm User / IP / mã; số lần truy cập bị từ
// chối; bảng Thời gian / User – Vai trò / Hành động / Chi tiết / IP / Kết quả, "Tải thêm" theo con trỏ;
// Xuất nhật ký CSV (mọi dòng khớp bộ lọc). Mock tính khoảng thời gian từ ngày chốt dữ liệu mẫu 22/06/2026.
import { useEffect, useMemo, useState } from 'react';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { downloadCsv } from '@/shared/lib/csv';
import { formatDateTime } from '@/shared/lib/format';
import { Badge, Button, Icon, Input, Select, Skeleton, toast, type IconName } from '@/shared/ui';
import { auditNow, listAllAuditLogs, useAuditLogs } from '../application/useSystem';
import { actionTone, AUDIT_CATEGORIES, rangeOf } from '../domain/system.rules';
import type { AuditCategory, AuditRange } from '../domain/system.types';

const th =
  'px-3.5 py-[11px] whitespace-nowrap font-semibold text-xs text-muted text-left bg-gray-25';
const td = 'px-3.5 py-3 whitespace-nowrap align-middle';
const lab = 'block text-sm font-semibold text-strong mb-1.5';
const dateBox =
  'block h-11 border-[1.5px] border-solid border-line rounded-md px-2.5 text-sm bg-surface-card text-body outline-none focus:border-primary';

const CAT_TONE: Record<AuditCategory, { fg: string; bg: string; icon: IconName }> = {
  AUTH: { fg: 'var(--blue-600)', bg: 'var(--blue-50)', icon: 'lock' },
  KYC: { fg: 'var(--brand-600)', bg: 'var(--brand-50)', icon: 'user-check' },
  FINANCE: { fg: 'var(--mint-600)', bg: 'var(--mint-50)', icon: 'banknote' },
  MODERATION: { fg: 'var(--text-muted)', bg: 'var(--gray-100)', icon: 'star' },
  SECURITY: { fg: 'var(--flash-600)', bg: 'var(--flash-50)', icon: 'shield-check' },
  CONFIG: { fg: 'var(--blue-600)', bg: 'var(--blue-50)', icon: 'sliders-horizontal' },
};

export default function AuditLogPage() {
  const { t, locale } = useT();
  const [range, setRange] = useState<AuditRange>('7D');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [cat, setCat] = useState<AuditCategory | ''>('');
  const [q, setQ] = useState('');
  const [term, setTerm] = useState('');
  const [exporting, setExporting] = useState(false);
  useEffect(() => {
    const id = setTimeout(() => setTerm(q.trim()), 300);
    return () => clearTimeout(id);
  }, [q]);
  const filter = useMemo(() => {
    const r = rangeOf(range, auditNow(), { from, to });
    return { from: r.from, to: r.to, category: cat || null, q: term };
  }, [range, from, to, cat, term]);
  const logs = useAuditLogs(filter);
  const rows = logs.data?.pages.flatMap((p) => p.items) ?? [];
  const denied = logs.data?.pages[0]?.deniedCount ?? 0;

  const exportCsv = async () => {
    setExporting(true);
    try {
      const all = await listAllAuditLogs(filter);
      downloadCsv('nhat-ky-he-thong.csv', [
        [
          t('admin-system.audit.col.time'),
          'User',
          t('admin-system.audit.col.role'),
          t('admin-system.audit.col.action'),
          t('admin-system.audit.col.detail'),
          'IP',
          t('admin-system.audit.col.result'),
        ],
        ...all.map((r) => [
          formatDateTime(r.createdAt, { locale }),
          r.actorEmail,
          r.actorRole,
          r.action,
          r.target,
          r.ip,
          r.result,
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
          {t('admin-system.audit.append_only')}
        </Badge>
        <span className="text-sm text-muted">{t('admin-system.audit.desc')}</span>
        <Button
          variant="secondary"
          size="sm"
          iconLeft="download"
          className="ml-auto"
          loading={exporting}
          onClick={() => void exportCsv()}
        >
          {t('admin-system.audit.export')}
        </Button>
      </div>
      <div className="flex items-end gap-3 bg-surface-card rounded-lg shadow-sm px-4 py-3.5 mb-4 flex-wrap">
        <Select<AuditRange>
          label={t('admin-system.audit.range')}
          className="w-[200px] max-sm:w-full"
          value={range}
          onChange={setRange}
          options={(['TODAY', '7D', '30D', 'CUSTOM'] as const).map((r) => ({
            value: r,
            label: t(`admin-system.audit.ranges.${r}`),
          }))}
        />
        {range === 'CUSTOM' && (
          <>
            <div>
              <label className={lab} htmlFor="audit-from">
                {t('admin-system.audit.from')}
              </label>
              <input
                id="audit-from"
                type="date"
                value={from}
                onChange={(e) => setFrom(e.target.value)}
                className={dateBox}
              />
            </div>
            <div>
              <label className={lab} htmlFor="audit-to">
                {t('admin-system.audit.to')}
              </label>
              <input
                id="audit-to"
                type="date"
                value={to}
                min={from || undefined}
                onChange={(e) => setTo(e.target.value)}
                className={dateBox}
              />
            </div>
          </>
        )}
        <Select<AuditCategory | ''>
          label={t('admin-system.audit.category')}
          className="w-[200px] max-sm:w-full"
          value={cat}
          onChange={setCat}
          options={[
            { value: '', label: t('admin-system.audit.all') },
            ...AUDIT_CATEGORIES.map((c) => ({
              value: c,
              label: t(`admin-system.audit.cats.${c}`),
            })),
          ]}
        />
        <Input
          label={t('admin-system.audit.search')}
          wrapperClassName="flex-1 min-w-[220px]"
          iconLeft="search"
          placeholder={t('admin-system.audit.search_placeholder')}
          value={q}
          onChange={(e) => setQ(e.target.value)}
        />
        <div className="inline-flex items-center gap-1.5 h-11 px-3 rounded-md bg-flash-50 text-flash-600 text-xs font-semibold">
          <Icon name="triangle-alert" size={15} />
          {t('admin-system.audit.denied', { count: denied })}
        </div>
      </div>
      <div className="bg-surface-card rounded-lg shadow-sm overflow-x-auto">
        {logs.isPending ? (
          <Skeleton lines={6} className="m-5" />
        ) : (
          <table className="w-full min-w-[980px] border-collapse tabular-nums">
            <thead>
              <tr>
                <th className={th}>{t('admin-system.audit.col.time')}</th>
                <th className={th}>{t('admin-system.audit.col.user')}</th>
                <th className={th}>{t('admin-system.audit.col.action')}</th>
                <th className={th}>{t('admin-system.audit.col.detail')}</th>
                <th className={th}>IP</th>
                <th className={th}>{t('admin-system.audit.col.result')}</th>
              </tr>
            </thead>
            <tbody>
              {rows.length === 0 && (
                <tr>
                  <td colSpan={6} className="p-9 text-center text-base text-muted">
                    {t('admin-system.audit.empty')}
                  </td>
                </tr>
              )}
              {rows.map((r) => {
                const tone = CAT_TONE[r.category];
                return (
                  <tr
                    key={r.id}
                    className="border-0 border-t border-solid border-line-subtle text-[13px] text-body"
                  >
                    <td className={cn(td, 'font-mono text-xs text-muted')}>
                      {formatDateTime(r.createdAt, { locale })}
                    </td>
                    <td className={td}>
                      <div className="flex items-center gap-[9px]">
                        <span
                          className="w-[30px] h-[30px] rounded-md grid place-items-center flex-none"
                          style={{ background: tone.bg, color: tone.fg }}
                          title={t(`admin-system.audit.cats.${r.category}`)}
                        >
                          <Icon name={tone.icon} size={15} />
                        </span>
                        <div className="min-w-0">
                          <div
                            className={cn(
                              'font-mono text-xs',
                              r.actorEmail === 'unknown' ? 'text-flash-600' : 'text-strong',
                            )}
                          >
                            {r.actorEmail}
                          </div>
                          <div className="text-xs text-subtle">
                            {r.actorRole === 'SYSTEM' || r.actorRole === '—'
                              ? r.actorRole === 'SYSTEM'
                                ? 'System'
                                : '—'
                              : t(`admin.perm.title.${r.actorRole}`)}
                          </div>
                        </div>
                      </div>
                    </td>
                    <td className={td}>
                      <Badge variant={actionTone(r.action)}>{r.action}</Badge>
                    </td>
                    <td className={cn(td, 'whitespace-normal max-w-[320px]')}>{r.target}</td>
                    <td className={cn(td, 'font-mono text-muted')}>{r.ip}</td>
                    <td className={td}>
                      <span
                        className={cn(
                          'inline-flex items-center gap-1 text-xs font-semibold',
                          r.result === 'SUCCESS' ? 'text-mint-600' : 'text-red-600',
                        )}
                      >
                        <Icon
                          name={
                            r.result === 'SUCCESS'
                              ? 'circle-check'
                              : r.result === 'DENIED'
                                ? 'ban'
                                : 'triangle-alert'
                          }
                          size={14}
                        />
                        {r.result}
                      </span>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>
      <div className="flex items-center gap-3 mt-3 flex-wrap">
        <span className="flex items-center gap-1.5 text-xs text-subtle">
          <Icon name="info" size={13} />
          {t('admin-system.audit.footnote', { count: rows.length })}
        </span>
        {logs.hasNextPage && (
          <Button
            variant="outline"
            size="sm"
            className="ml-auto"
            loading={logs.isFetchingNextPage}
            onClick={() => void logs.fetchNextPage()}
          >
            {t('admin-system.audit.more')}
          </Button>
        )}
      </div>
    </div>
  );
}
