// Cấu hình hoa hồng (prototype admin/Finance.jsx Commission) — plan G12.27: phí mặc định (Lưu mặc định),
// hoa hồng theo ngành (tỷ lệ + công tắc; tắt = dùng mặc định), Lưu cấu hình. Tỷ lệ 0–50% (API 11.8),
// nhận số thập phân; ô sai viền đỏ và khóa nút lưu.
import { useEffect, useState } from 'react';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { Button, EmptyState, Input, Skeleton, Switch, toast } from '@/shared/ui';
import { useCommission, useSaveCategoryRates, useSaveDefaultRate } from '../application/useFinance';
import { parseRate } from '../domain/finance.rules';

interface Row {
  categoryId: number;
  name: string;
  raw: string;
  enabled: boolean;
}

export default function CommissionPage() {
  const { t } = useT();
  const q = useCommission();
  const saveDef = useSaveDefaultRate();
  const saveCats = useSaveCategoryRates();
  const [def, setDef] = useState('');
  const [rows, setRows] = useState<Row[]>([]);
  useEffect(() => {
    if (!q.data) return;
    setDef(String(q.data.defaultRate));
    setRows(
      q.data.categories.map((c) => ({
        categoryId: c.categoryId,
        name: c.name,
        raw: String(c.rate),
        enabled: c.enabled,
      })),
    );
  }, [q.data]);
  if (q.isPending) return <Skeleton lines={8} />;
  if (q.isError)
    return (
      <EmptyState
        icon="triangle-alert"
        title={t('common.state.error')}
        text={errorMessage(q.error, t)}
      />
    );
  const defRate = parseRate(def);
  const rowsOk = rows.every((r) => !r.enabled || parseRate(r.raw) !== null);
  const clean = (s: string) => s.replace(/[^\d.,]/g, '');
  const done = (key: string) => ({
    onSuccess: () => toast.success(t(key)),
    onError: (e: unknown) => toast.error(errorMessage(e, t)),
  });
  return (
    <div className="max-w-[1180px]">
      <section
        className="bg-surface-card rounded-lg shadow-sm p-[22px] mb-4"
        aria-labelledby="cm-def"
      >
        <h2 id="cm-def" className="m-0 mb-1 text-base font-bold text-strong">
          {t('admin-finance.commission.default_title')}
        </h2>
        <p className="m-0 mb-4 text-sm text-muted">{t('admin-finance.commission.order')}</p>
        <div className="flex items-end gap-3.5 flex-wrap">
          <Input
            label={t('admin-finance.commission.default_label')}
            wrapperClassName="w-[160px]"
            inputMode="decimal"
            iconRight="percent"
            value={def}
            invalid={def !== '' && defRate === null}
            error={def !== '' && defRate === null ? t('admin-finance.commission.err') : undefined}
            onChange={(e) => setDef(clean(e.target.value))}
          />
          <Button
            variant="secondary"
            disabled={defRate === null}
            loading={saveDef.isPending}
            onClick={() =>
              defRate !== null &&
              saveDef.mutate(defRate, done('admin-finance.commission.default_saved'))
            }
          >
            {t('admin-finance.commission.default_save')}
          </Button>
        </div>
      </section>
      <section
        className="bg-surface-card rounded-lg shadow-sm overflow-hidden"
        aria-labelledby="cm-cat"
      >
        <h2
          id="cm-cat"
          className="m-0 px-5 py-3.5 border-0 border-b border-solid border-line-subtle text-base font-bold text-strong"
        >
          {t('admin-finance.commission.by_category')}
        </h2>
        <ul className="list-none m-0 p-0">
          {rows.map((r, i) => {
            const bad = r.enabled && parseRate(r.raw) === null;
            return (
              <li
                key={r.categoryId}
                className="flex items-center gap-3.5 px-5 py-3 border-0 border-b border-solid border-line-subtle"
              >
                <span className={cn('flex-1 text-base', r.enabled ? 'text-body' : 'text-subtle')}>
                  {r.name}
                  {!r.enabled && (
                    <span className="block text-xs text-muted">
                      {t('admin-finance.commission.use_default', { rate: def })}
                    </span>
                  )}
                </span>
                <Input
                  aria-label={t('admin-finance.commission.rate_for', { name: r.name })}
                  wrapperClassName="w-[110px]"
                  inputMode="decimal"
                  iconRight="percent"
                  disabled={!r.enabled}
                  invalid={bad}
                  value={r.raw}
                  onChange={(e) =>
                    setRows((xs) =>
                      xs.map((x, j) => (j === i ? { ...x, raw: clean(e.target.value) } : x)),
                    )
                  }
                />
                <Switch
                  aria-label={t('admin-finance.commission.toggle', { name: r.name })}
                  checked={r.enabled}
                  onChange={() =>
                    setRows((xs) => xs.map((x, j) => (j === i ? { ...x, enabled: !x.enabled } : x)))
                  }
                />
              </li>
            );
          })}
        </ul>
        <div className="p-4 flex items-center justify-end gap-3">
          {!rowsOk && (
            <span className="text-xs text-red-600">{t('admin-finance.commission.err')}</span>
          )}
          <Button
            disabled={!rowsOk}
            loading={saveCats.isPending}
            onClick={() =>
              saveCats.mutate(
                rows.map((r) => ({
                  categoryId: r.categoryId,
                  name: r.name,
                  rate: parseRate(r.raw) ?? 0,
                  enabled: r.enabled,
                })),
                done('admin-finance.commission.saved'),
              )
            }
          >
            {t('admin-finance.commission.save')}
          </Button>
        </div>
      </section>
    </div>
  );
}
