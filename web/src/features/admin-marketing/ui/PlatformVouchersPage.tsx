// Voucher sàn (prototype admin/Vouchers.jsx) — plan G12.33: 5 chỉ số, tab trạng thái (?tab=), bảng
// Mã / Loại / Giá trị / Đơn tối thiểu / Giảm tối đa / Sử dụng / Thời gian / Nhà tài trợ / Trạng thái;
// Nhân bản / Sửa / Tắt / Huỷ lịch / Xoá vĩnh viễn (khi đã kết thúc và chưa dùng). Form tạo / sửa dùng
// lại `VoucherEditor` của seller-promotions ở chế độ `platform` (G11.40 đã chốt dùng chung).
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { listRootCategories } from '@/features/catalog';
import { EMPTY_VOUCHER, VoucherEditor, type VoucherValues } from '@/features/seller-promotions';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatDate, formatNumber, formatVND } from '@/shared/lib/format';
import {
  Badge,
  Button,
  ConfirmDialog,
  EmptyState,
  Skeleton,
  Tabs,
  toast,
  type BadgeVariant,
} from '@/shared/ui';
import { Kpi } from '@/shared/ui/dash';
import {
  useDeletePlatformVoucher,
  useDisablePlatformVoucher,
  usePlatformVoucher,
  usePlatformVouchers,
  useSavePlatformVoucher,
} from '../application/useMarketing';
import {
  inVoucherTab,
  usagePercent,
  voucherActions,
  VOUCHER_TABS,
  type VoucherTab,
} from '../domain/marketing.rules';
import type { PlatformVoucher, PlatformVoucherInput } from '../domain/marketing.types';

const th =
  'px-3.5 py-[11px] whitespace-nowrap font-semibold text-xs text-muted text-left bg-gray-25';
const td = 'px-3.5 py-3 whitespace-nowrap align-middle';
const STATUS: Record<PlatformVoucher['status'], BadgeVariant> = {
  ACTIVE: 'success',
  SCHEDULED: 'primary',
  ENDED: 'neutral',
  DISABLED: 'danger',
};

const toValues = (v: PlatformVoucherInput): VoucherValues => ({
  ...EMPTY_VOUCHER,
  code: v.code,
  discountType:
    v.discountType === 'FIXED_AMOUNT'
      ? 'FIXED'
      : v.discountType === 'FREESHIP'
        ? 'FREESHIP'
        : 'PERCENTAGE',
  discountValue: v.discountValue,
  maxDiscount: v.maxDiscount,
  minOrderValue: v.minOrderValue,
  usageLimit: v.usageLimit,
  usagePerUser: v.usagePerUser,
  budget: v.budget,
  validFrom: v.validFrom,
  validTo: v.validTo,
  scope: v.scope,
  categoryIds: v.categoryIds,
  excludedCategoryIds: v.excludedCategoryIds,
  title: v.title,
  fundedBy: v.fundedBy,
  platformSharePercent: v.platformSharePercent,
});

const toInput = (v: VoucherValues): PlatformVoucherInput => ({
  title: v.title,
  code: v.code,
  discountType:
    v.discountType === 'FIXED'
      ? 'FIXED_AMOUNT'
      : v.discountType === 'FREESHIP'
        ? 'FREESHIP'
        : 'PERCENTAGE',
  discountValue: v.discountValue,
  maxDiscount: v.maxDiscount,
  minOrderValue: v.minOrderValue,
  usageLimit: v.usageLimit,
  usagePerUser: v.usagePerUser,
  budget: v.budget,
  validFrom: v.validFrom,
  validTo: v.validTo,
  scope: v.scope === 'CATEGORY' ? 'CATEGORY' : 'ALL',
  categoryIds: v.categoryIds,
  excludedCategoryIds: v.excludedCategoryIds,
  fundedBy: v.fundedBy,
  platformSharePercent: v.platformSharePercent,
});

interface FormTarget {
  id: number | null;
  duplicate?: boolean;
}

function VoucherForm({ target, onClose }: { target: FormTarget; onClose: () => void }) {
  const q = usePlatformVoucher(target.id);
  const save = useSavePlatformVoucher();
  const cats = useQuery({ queryKey: ['catalog', 'roots'], queryFn: listRootCategories });
  if (target.id !== null && !q.data) return null;
  const editing = target.id !== null && !target.duplicate;
  const base = q.data ? toValues(q.data) : EMPTY_VOUCHER;
  return (
    <VoucherEditor
      mode="platform"
      editing={editing}
      initial={target.duplicate ? { ...base, code: '' } : base}
      categories={cats.data?.map((c) => ({ id: c.id, name: c.name }))}
      saving={save.isPending}
      onSave={(v) => save.mutateAsync({ id: editing ? target.id : null, input: toInput(v) })}
      onClose={onClose}
    />
  );
}

export default function PlatformVouchersPage() {
  const { t, locale } = useT();
  const [params, setParams] = useSearchParams();
  const raw = params.get('tab') as VoucherTab | null;
  const tab: VoucherTab = raw && VOUCHER_TABS.includes(raw) ? raw : 'ALL';
  const q = usePlatformVouchers(tab);
  const disable = useDisablePlatformVoucher();
  const del = useDeletePlatformVoucher();
  const [form, setForm] = useState<FormTarget | null>(null);
  const [disabling, setDisabling] = useState<PlatformVoucher | null>(null);
  const [deleting, setDeleting] = useState<PlatformVoucher | null>(null);
  const vnd = (n: number) => formatVND(n, { locale });
  const k = (n: number) =>
    n >= 1000 ? `${formatNumber(Math.round(n / 100) / 10, { locale })}k` : String(n);
  const valueLabel = (v: PlatformVoucher) =>
    v.discountType === 'PERCENTAGE'
      ? `${v.discountValue}%`
      : v.discountType === 'FIXED_AMOUNT'
        ? vnd(v.discountValue)
        : t('admin-marketing.vouchers.free_ship');

  if (q.isError)
    return (
      <EmptyState
        icon="triangle-alert"
        title={t('common.state.error')}
        text={errorMessage(q.error, t)}
      />
    );
  const s = q.data?.summary;
  const items = (q.data?.items ?? []).filter((v) => inVoucherTab(v.status, tab));
  const done = (key: string, after: () => void) => ({
    onSuccess: () => {
      toast.success(t(key));
      after();
    },
    onError: (e: unknown) => toast.error(errorMessage(e, t)),
  });

  return (
    <div className="max-w-[1180px]">
      <div className="grid grid-cols-5 gap-4 mb-[18px] max-xl:grid-cols-3 max-md:grid-cols-2">
        <Kpi
          icon="ticket"
          tone={{ bg: 'var(--mint-50)', fg: 'var(--mint-600)' }}
          label={t('admin-marketing.vouchers.kpi.active')}
          value={s ? s.activeCount : '…'}
        />
        <Kpi
          icon="chart-column"
          tone={{ bg: 'var(--blue-50)', fg: 'var(--blue-600)' }}
          label={t('admin-marketing.vouchers.kpi.usage')}
          value={s ? formatNumber(s.totalUsage, { locale }) : '…'}
        />
        <Kpi
          icon="receipt"
          tone={{ bg: 'var(--amber-50)', fg: 'var(--amber-600)' }}
          label={t('admin-marketing.vouchers.kpi.discount')}
          value={s ? vnd(s.totalDiscountUsed) : '…'}
        />
        <Kpi
          icon="calendar"
          tone={{ bg: 'var(--brand-50)', fg: 'var(--brand-600)' }}
          label={t('admin-marketing.vouchers.kpi.scheduled')}
          value={s ? s.scheduledCount : '…'}
        />
        <Kpi
          icon="banknote"
          tone={{ bg: 'var(--gray-100)', fg: 'var(--gray-600)' }}
          label={t('admin-marketing.vouchers.kpi.budget')}
          value={s ? vnd(s.remainingBudget) : '…'}
        />
      </div>
      <div className="flex items-center gap-3 mb-3.5 flex-wrap">
        <div className="flex-1 min-w-0 overflow-x-auto">
          <Tabs
            value={tab}
            onChange={(v) => setParams({ tab: v }, { replace: true })}
            aria-label={t('admin-marketing.vouchers.tabs_label')}
            items={VOUCHER_TABS.map((x) => ({
              id: x,
              label: t(`admin-marketing.vouchers.tabs.${x}`),
            }))}
          />
        </div>
        <Button iconLeft="plus" onClick={() => setForm({ id: null })}>
          {t('admin-marketing.vouchers.create')}
        </Button>
      </div>
      <div className="bg-surface-card rounded-lg shadow-sm overflow-x-auto">
        {!q.data ? (
          <Skeleton lines={5} className="m-5" />
        ) : (
          <table className="w-full min-w-[1040px] border-collapse tabular-nums">
            <thead>
              <tr>
                <th className={th}>{t('admin-marketing.vouchers.col.code')}</th>
                <th className={th}>{t('admin-marketing.vouchers.col.kind')}</th>
                <th className={cn(th, 'text-right')}>{t('admin-marketing.vouchers.col.value')}</th>
                <th className={cn(th, 'text-right')}>{t('admin-marketing.vouchers.col.min')}</th>
                <th className={cn(th, 'text-right')}>{t('admin-marketing.vouchers.col.cap')}</th>
                <th className={th}>{t('admin-marketing.vouchers.col.usage')}</th>
                <th className={th}>{t('admin-marketing.vouchers.col.period')}</th>
                <th className={th}>{t('admin-marketing.vouchers.col.sponsor')}</th>
                <th className={th}>{t('admin-marketing.vouchers.col.status')}</th>
                <th
                  className={cn(
                    th,
                    'text-right sticky right-0 shadow-[-6px_0_8px_-6px_rgba(0,0,0,.12)]',
                  )}
                >
                  {t('admin-marketing.vouchers.col.action')}
                </th>
              </tr>
            </thead>
            <tbody>
              {items.length === 0 && (
                <tr>
                  <td colSpan={10} className="p-9 text-center text-base text-muted">
                    {t('admin-marketing.vouchers.empty')}
                  </td>
                </tr>
              )}
              {items.map((v) => {
                const pct = usagePercent(v.usage);
                const acts = voucherActions(v);
                return (
                  <tr
                    key={v.id}
                    className="border-0 border-t border-solid border-line-subtle text-[13px] text-body"
                  >
                    <td className={td}>
                      <span className="font-mono font-semibold text-brand-600">{v.code}</span>
                      <span className="block text-xs text-muted max-w-[180px] truncate">
                        {v.title}
                      </span>
                    </td>
                    <td className={td}>{t(`admin-marketing.vouchers.kind.${v.discountType}`)}</td>
                    <td className={cn(td, 'text-right font-semibold')}>{valueLabel(v)}</td>
                    <td className={cn(td, 'text-right')}>{vnd(v.minOrderValue)}</td>
                    <td className={cn(td, 'text-right')}>
                      {v.maxDiscount ? vnd(v.maxDiscount) : '—'}
                    </td>
                    <td className={td}>
                      <div className="flex items-center gap-2">
                        <div
                          className="w-20 h-1.5 rounded-[3px] bg-gray-200 overflow-hidden"
                          role="progressbar"
                          aria-valuenow={pct}
                          aria-valuemin={0}
                          aria-valuemax={100}
                          aria-label={t('admin-marketing.vouchers.col.usage')}
                        >
                          <div
                            className={cn('h-full', pct >= 100 ? 'bg-gray-400' : 'bg-brand-500')}
                            style={{ width: `${pct}%` }}
                          />
                        </div>
                        <span className="text-xs text-muted">
                          {k(v.usage.used)}/{k(v.usage.limit)}
                        </span>
                      </div>
                    </td>
                    <td className={cn(td, 'text-muted text-xs')}>
                      {formatDate(v.validFrom, { locale })}
                      <br />→ {formatDate(v.validTo, { locale })}
                    </td>
                    <td className={td}>
                      <Badge variant={v.fundedBy === 'PLATFORM' ? 'primary' : 'neutral'}>
                        {v.fundedBy === 'PLATFORM' ? 'Genie' : 'Co-sponsor'}
                      </Badge>
                    </td>
                    <td className={td}>
                      <Badge variant={STATUS[v.status]}>
                        {t(`admin-marketing.vouchers.status.${v.status}`)}
                      </Badge>
                    </td>
                    <td
                      className={cn(
                        td,
                        'text-right sticky right-0 bg-surface-card shadow-[-6px_0_8px_-6px_rgba(0,0,0,.12)]',
                      )}
                    >
                      <div className="flex gap-1 justify-end">
                        {acts.includes('duplicate') && (
                          <Button
                            size="sm"
                            variant="ghost"
                            onClick={() => setForm({ id: v.id, duplicate: true })}
                          >
                            {t('admin-marketing.vouchers.duplicate')}
                          </Button>
                        )}
                        {acts.includes('edit') && (
                          <Button size="sm" variant="ghost" onClick={() => setForm({ id: v.id })}>
                            {t('admin-marketing.vouchers.edit')}
                          </Button>
                        )}
                        {(acts.includes('disable') || acts.includes('cancel')) && (
                          <Button
                            size="sm"
                            variant="ghost"
                            className="!text-red-600"
                            onClick={() => setDisabling(v)}
                          >
                            {t(
                              acts.includes('cancel')
                                ? 'admin-marketing.vouchers.cancel'
                                : 'admin-marketing.vouchers.disable',
                            )}
                          </Button>
                        )}
                        {acts.includes('delete') && (
                          <Button
                            size="sm"
                            variant="ghost"
                            className="!text-red-600"
                            onClick={() => setDeleting(v)}
                          >
                            {t('admin-marketing.vouchers.delete')}
                          </Button>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>

      {form && <VoucherForm target={form} onClose={() => setForm(null)} />}
      <ConfirmDialog
        open={!!disabling}
        tone="danger"
        title={t(
          disabling?.status === 'SCHEDULED'
            ? 'admin-marketing.vouchers.cancel_title'
            : 'admin-marketing.vouchers.disable_title',
        )}
        message={t('admin-marketing.vouchers.disable_desc', { code: disabling?.code ?? '' })}
        confirmLabel={t(
          disabling?.status === 'SCHEDULED'
            ? 'admin-marketing.vouchers.cancel'
            : 'admin-marketing.vouchers.disable',
        )}
        loading={disable.isPending}
        onCancel={() => setDisabling(null)}
        onConfirm={() =>
          disabling &&
          disable.mutate(
            disabling.id,
            done('admin-marketing.vouchers.disabled', () => setDisabling(null)),
          )
        }
      />
      <ConfirmDialog
        open={!!deleting}
        tone="danger"
        title={t('admin-marketing.vouchers.delete_title')}
        message={t('admin-marketing.vouchers.delete_desc', { code: deleting?.code ?? '' })}
        confirmLabel={t('admin-marketing.vouchers.delete_confirm')}
        loading={del.isPending}
        onCancel={() => setDeleting(null)}
        onConfirm={() =>
          deleting &&
          del.mutate(
            deleting.id,
            done('admin-marketing.vouchers.deleted', () => setDeleting(null)),
          )
        }
      />
    </div>
  );
}
