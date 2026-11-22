// Hộp chi tiết một dòng nhật ký (plan 2026-11-22 nhật ký banner N5; API_SPEC 11.10): người làm, hành động đã
// dịch, đối tượng, mã tham chiếu, IP, user agent và bảng thay đổi trước → sau (details.changes).
import type { ReactNode } from 'react';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatDateTime } from '@/shared/lib/format';
import { hasKey } from '@/shared/lib/i18n';
import { Badge, Modal } from '@/shared/ui';
import { actionTone } from '../domain/system.rules';
import type { AuditEntry } from '../domain/system.types';

/** Nhãn đã dịch theo mã; thiếu khóa (mã mới chưa dịch) → hiện chính mã. */
export function useAuditLabels() {
  const { t } = useT();
  const pick = (prefix: string, code: string) =>
    hasKey(`${prefix}.${code}`) ? t(`${prefix}.${code}`) : code;
  return {
    action: (code: string) => pick('admin-system.audit.actions', code),
    field: (code: string) => pick('admin-system.audit.fields', code),
  };
}

const dt = 'text-xs font-semibold text-muted';
const dd = 'text-sm text-body break-words min-w-0';

export function AuditLogDetail({
  entry,
  onClose,
}: {
  entry: AuditEntry | null;
  onClose: () => void;
}) {
  const { t, locale } = useT();
  const label = useAuditLabels();
  if (!entry) return null;
  const empty = <span className="text-subtle italic">{t('admin-system.audit.detail.empty')}</span>;
  const value = (v: string | null) => (v === null || v === '' || v === 'null' ? empty : v);
  const rows: [string, ReactNode][] = [
    [t('admin-system.audit.detail.time'), formatDateTime(entry.createdAt, { locale })],
    [t('admin-system.audit.detail.actor'), `${entry.actorEmail} · ${entry.actorRole}`],
    [
      t('admin-system.audit.detail.action'),
      <span key="a" className="inline-flex items-center gap-2 flex-wrap">
        <Badge variant={actionTone(entry.action)}>{label.action(entry.action)}</Badge>
        <code className="text-xs text-subtle">{entry.action}</code>
      </span>,
    ],
    [t('admin-system.audit.detail.category'), t(`admin-system.audit.cats.${entry.category}`)],
    [t('admin-system.audit.detail.target'), value(entry.target)],
    [t('admin-system.audit.detail.reference'), value(entry.referenceId)],
    [t('admin-system.audit.detail.ip'), <code key="ip">{entry.ip || '—'}</code>],
    [t('admin-system.audit.detail.user_agent'), value(entry.userAgent)],
    [t('admin-system.audit.detail.result'), entry.result],
  ];

  return (
    <Modal open onClose={onClose} title={t('admin-system.audit.detail.title')} width={640}>
      <dl className="grid grid-cols-[150px_minmax(0,1fr)] gap-x-4 gap-y-2.5 m-0 max-md:grid-cols-1 max-md:gap-y-1">
        {rows.map(([k, v]) => (
          <div key={k} className="contents">
            <dt className={dt}>{k}</dt>
            <dd className={cn(dd, 'm-0 max-md:mb-2')}>{v}</dd>
          </div>
        ))}
      </dl>

      <h3 className="mt-5 mb-2 text-sm font-bold text-strong">
        {t('admin-system.audit.detail.changes')}
      </h3>
      {entry.changes.length === 0 ? (
        <p className="m-0 text-sm text-muted">{t('admin-system.audit.detail.no_changes')}</p>
      ) : (
        <div className="overflow-x-auto border border-solid border-line rounded-md">
          <table className="w-full border-collapse text-[13px]">
            <thead>
              <tr className="bg-gray-25 text-xs text-muted text-left">
                <th className="px-3 py-2 font-semibold">{t('admin-system.audit.detail.field')}</th>
                <th className="px-3 py-2 font-semibold">{t('admin-system.audit.detail.from')}</th>
                <th className="px-3 py-2 font-semibold">{t('admin-system.audit.detail.to')}</th>
              </tr>
            </thead>
            <tbody>
              {entry.changes.map((c) => (
                <tr
                  key={c.field}
                  className="border-0 border-t border-solid border-line-subtle align-top"
                >
                  <td className="px-3 py-2 font-semibold text-strong whitespace-nowrap">
                    {label.field(c.field)}
                  </td>
                  <td className="px-3 py-2 text-red-600 break-all">{value(c.from)}</td>
                  <td className="px-3 py-2 text-mint-600 break-all">{value(c.to)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Modal>
  );
}
