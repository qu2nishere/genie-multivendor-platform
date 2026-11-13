// "Kho Voucher" (prototype Account.jsx VouchersPanel) — plan G10.55: nhập mã để lưu, tab Tất cả / Sàn /
// Shop / Freeship, lượt đã dùng, xóa khỏi kho, chi tiết; "Xem lịch sử voucher" (Hết hiệu lực / Đã dùng).
import { useState } from 'react';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatDate, formatNumber } from '@/shared/lib/format';
import { Providers } from '@/shared/providers/Providers';
import {
  Button,
  ConfirmDialog,
  EmptyState,
  Icon,
  Modal,
  PanelHead,
  Skeleton,
  Tabs,
  toast,
} from '@/shared/ui';
import { useMyVouchers, useRemoveVoucher, useSaveVoucher } from '../application/useVouchers';
import type { Voucher } from '../domain/voucher.types';

const STUB: Record<Voucher['kind'], string> = {
  platform: 'bg-brand-500',
  shop: 'bg-flash-500',
  freeship: 'bg-mint-500',
};
const KIND_ICON = { platform: 'ticket', shop: 'store', freeship: 'truck' } as const;
type Tab = 'all' | Voucher['kind'];

function usageColor(pct: number) {
  if (pct >= 100) return 'bg-gray-400';
  if (pct >= 80) return 'bg-red-500';
  if (pct >= 55) return 'bg-amber-500';
  return 'bg-mint-500';
}

function Panel() {
  const { t, locale } = useT();
  const q = useMyVouchers();
  const [mode, setMode] = useState<'live' | 'history'>('live');
  const [tab, setTab] = useState<Tab>('all');
  const [hist, setHist] = useState<'expired' | 'used'>('expired');
  const [code, setCode] = useState('');
  const [detail, setDetail] = useState<Voucher | null>(null);
  const [del, setDel] = useState<Voucher | null>(null);
  const save = useSaveVoucher(
    () => {
      setCode('');
      toast.success(t('promotions.wallet.saved'));
    },
    (e) => toast.error(errorMessage(e, t)),
  );
  const remove = useRemoveVoucher();
  const all = q.data ?? [];
  const live = all.filter((v) => v.status === 'active' || v.status === 'upcoming');
  const count = (k: Voucher['kind']) => live.filter((v) => v.kind === k).length;
  const list =
    mode === 'history'
      ? all.filter((v) => v.status === hist)
      : tab === 'all'
        ? live
        : live.filter((v) => v.kind === tab);
  const rule = (v: Voucher) =>
    v.categories
      ? {
          text: t('promotions.wallet.only', { list: v.categories.join(', ') }),
          cls: 'text-mint-600',
        }
      : v.excluded
        ? {
            text: t('promotions.wallet.except', { list: v.excluded.join(', ') }),
            cls: 'text-red-600',
          }
        : null;
  const date = (d: string) => formatDate(d, { locale });

  return (
    <section
      className="bg-surface-card rounded-lg shadow-sm px-[22px] py-5 max-sm:px-4"
      aria-labelledby="vw-title"
    >
      <div className="flex items-start gap-1.5">
        {mode === 'history' && (
          <button
            type="button"
            aria-label={t('common.action.back')}
            onClick={() => setMode('live')}
            className="border-0 bg-transparent cursor-pointer p-1 text-muted grid place-items-center w-11 h-11"
          >
            <Icon name="arrow-left" size={20} />
          </button>
        )}
        <div className="flex-1">
          <PanelHead
            id="vw-title"
            icon="ticket"
            title={t(mode === 'history' ? 'promotions.wallet.history' : 'account.menu.vouchers')}
            sub={
              mode === 'history'
                ? t('promotions.wallet.history_sub')
                : t('promotions.wallet.sub', { count: live.length })
            }
            action={
              mode === 'live' && (
                <button
                  type="button"
                  onClick={() => setMode('history')}
                  className="border-0 bg-transparent p-0 text-sm font-semibold text-brand-600 cursor-pointer hover:underline min-h-11"
                >
                  {t('promotions.wallet.view_history')}
                </button>
              )
            }
          />
        </div>
      </div>
      {mode === 'live' && (
        <form
          className="flex gap-2.5 items-center bg-surface-sunken rounded-md px-3.5 py-2.5 mt-3.5 mb-1 flex-wrap"
          onSubmit={(e) => {
            e.preventDefault();
            if (code.trim()) save.mutate(code.trim().toUpperCase());
          }}
        >
          <label htmlFor="vw-code" className="text-sm text-muted whitespace-nowrap">
            {t('promotions.picker.code_label')}
          </label>
          <input
            id="vw-code"
            value={code}
            onChange={(e) => setCode(e.target.value)}
            placeholder={t('promotions.wallet.code_placeholder')}
            className="flex-1 min-w-[140px] h-9 border-[1.5px] border-solid border-line rounded-sm px-2.5 text-sm outline-none bg-surface-card focus:border-primary"
          />
          <Button type="submit" size="sm" disabled={!code.trim()} loading={save.isPending}>
            {t('common.action.save')}
          </Button>
        </form>
      )}
      <div className="mt-1">
        {mode === 'live' ? (
          <Tabs<Tab>
            value={tab}
            onChange={setTab}
            aria-label={t('account.menu.vouchers')}
            items={[
              { id: 'all', label: `${t('common.action.all')} (${live.length})` },
              {
                id: 'platform',
                label: `${t('promotions.wallet.platform')} (${count('platform')})`,
              },
              { id: 'shop', label: `${t('promotions.wallet.shop')} (${count('shop')})` },
              { id: 'freeship', label: `Freeship (${count('freeship')})` },
            ]}
          />
        ) : (
          <Tabs<'expired' | 'used'>
            value={hist}
            onChange={setHist}
            aria-label={t('promotions.wallet.history')}
            items={[
              { id: 'expired', label: t('promotions.wallet.expired') },
              { id: 'used', label: t('promotions.wallet.used') },
            ]}
          />
        )}
      </div>
      {q.isPending && <Skeleton lines={4} className="mt-4" />}
      {!q.isPending && list.length === 0 && (
        <EmptyState icon="ticket" text={t('promotions.wallet.empty')} />
      )}
      <ul className="list-none m-0 p-0 grid grid-cols-2 gap-3.5 mt-4 max-md:grid-cols-1">
        {list.map((v) => {
          const pct = v.usageLimit
            ? Math.min(100, Math.round((v.usedCount / v.usageLimit) * 100))
            : 0;
          const old = mode === 'history';
          const r = rule(v);
          return (
            <li
              key={v.id}
              className={cn(
                'flex border border-solid border-line-subtle rounded-md overflow-hidden bg-surface-card',
                old && 'opacity-[.72]',
              )}
            >
              <button
                type="button"
                onClick={() => setDetail(v)}
                aria-label={t('promotions.wallet.detail_of', { label: v.label })}
                className={cn(
                  'relative w-24 flex-none flex flex-col items-center justify-center gap-1 text-inverse border-0 cursor-pointer',
                  old ? 'bg-gray-400' : STUB[v.kind],
                )}
              >
                {old && (
                  <span className="absolute top-1 left-1 px-1 py-0.5 rounded-xs bg-black/30 font-semibold text-[10px]">
                    {t(
                      v.status === 'used'
                        ? 'promotions.wallet.used_tag'
                        : 'promotions.wallet.expired_tag',
                    )}
                  </span>
                )}
                <Icon name={KIND_ICON[v.kind]} size={22} />
                <span className="font-extrabold text-[17px]">{v.amountLabel}</span>
              </button>
              <div className="flex-1 min-w-0 px-3.5 py-3">
                <div className="font-bold text-sm text-strong">{v.label}</div>
                <div className="text-xs text-muted mt-0.5">
                  {v.conditionLabel} · {v.scopeLabel}
                </div>
                {r && <div className={cn('font-semibold text-[11px] mt-0.5', r.cls)}>{r.text}</div>}
                <div className="flex items-center gap-2 mt-[9px] flex-wrap">
                  <span
                    className={cn(
                      'text-xs',
                      v.status === 'upcoming'
                        ? 'text-amber-600'
                        : old
                          ? 'text-muted'
                          : 'text-flash-600',
                    )}
                  >
                    {v.status === 'upcoming'
                      ? t('promotions.wallet.from', { date: date(v.validFrom) })
                      : t('promotions.picker.expiry', { date: date(v.expiresAt) })}
                  </span>
                  {!old && (
                    <span className="ml-auto inline-flex items-center gap-2">
                      <button
                        type="button"
                        aria-label={t('promotions.wallet.remove', { label: v.label })}
                        onClick={() => setDel(v)}
                        className="border-0 bg-transparent text-muted cursor-pointer grid place-items-center w-9 h-9 rounded-md hover:bg-gray-50"
                      >
                        <Icon name="trash-2" size={16} />
                      </button>
                      {v.status === 'upcoming' ? (
                        <Button size="sm" disabled>
                          {t('promotions.wallet.not_yet')}
                        </Button>
                      ) : (
                        <Button size="sm" href="/cart">
                          {t('promotions.wallet.use')}
                        </Button>
                      )}
                    </span>
                  )}
                </div>
                {!old && (
                  <div className="mt-1.5">
                    <div
                      className="h-1 rounded-pill bg-gray-200 overflow-hidden"
                      aria-hidden="true"
                    >
                      <div className={cn('h-full', usageColor(pct))} style={{ width: `${pct}%` }} />
                    </div>
                    <div className="mt-[3px] text-xs text-subtle">
                      {t('promotions.wallet.usage', {
                        used: formatNumber(v.usedCount, { locale }),
                        limit: v.usageLimit ? formatNumber(v.usageLimit, { locale }) : '∞',
                      })}
                    </div>
                  </div>
                )}
              </div>
            </li>
          );
        })}
      </ul>
      <Modal
        open={detail !== null}
        onClose={() => setDetail(null)}
        title={t('promotions.wallet.detail')}
        footer={<Button onClick={() => setDetail(null)}>{t('promotions.wallet.got_it')}</Button>}
      >
        {detail && (
          <dl className="m-0 grid gap-3">
            {(
              [
                ['code', detail.code],
                ['offer', detail.label],
                ['sponsor', detail.sponsorLabel],
                ['condition', detail.conditionLabel],
                ['scope', rule(detail)?.text ?? detail.scopeLabel],
                ['valid', `${date(detail.validFrom)} - ${date(detail.expiresAt)}`],
              ] as const
            ).map(([k, val]) => (
              <div key={k} className="grid grid-cols-[120px_1fr] gap-3 text-sm">
                <dt className="text-muted">{t(`promotions.wallet.field.${k}`)}</dt>
                <dd className="m-0 text-strong font-semibold">{val}</dd>
              </div>
            ))}
            <div className="grid gap-1.5 text-sm">
              <dt className="text-muted">{t('promotions.wallet.field.content')}</dt>
              <dd className="m-0 text-strong leading-relaxed">{detail.content}</dd>
            </div>
          </dl>
        )}
      </Modal>
      <ConfirmDialog
        open={del !== null}
        tone="danger"
        title={t('promotions.wallet.remove_title')}
        message={t('promotions.wallet.remove_text', { label: del?.label ?? '' })}
        confirmLabel={t('common.action.delete')}
        loading={remove.isPending}
        onCancel={() => setDel(null)}
        onConfirm={() =>
          del &&
          remove.mutate(del, {
            onSuccess: () => {
              setDel(null);
              toast.success(t('promotions.wallet.removed'));
            },
            onError: (e) => toast.error(errorMessage(e, t)),
          })
        }
      />
    </section>
  );
}

export default function VouchersPanel() {
  return (
    <Providers>
      <Panel />
    </Providers>
  );
}
