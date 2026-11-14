// Chọn voucher (prototype VoucherPicker.jsx) — plan G10.13: danh sách cuộn mọi voucher đã lưu theo
// nhóm; nhập mã để áp dụng; voucher chưa đủ điều kiện vẫn hiện, mờ, kèm lý do; chân hộp báo số tiền
// tiết kiệm. mode "platform": một voucher sàn + một freeship; mode "shop": một voucher của shop.
import { useEffect, useMemo, useState } from 'react';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatDate, formatVND } from '@/shared/lib/format';
import { Button, Icon, Modal, toast } from '@/shared/ui';
import { useFindVoucher } from '../application/useVouchers';
import { usableVouchers, voucherCheck, voucherDiscount } from '../domain/pricing.rules';
import type {
  IneligibleReason,
  PricingContext,
  PricingLine,
  VoucherSelection,
} from '../domain/pricing.types';
import type { Voucher } from '../domain/voucher.types';

export interface VoucherPickerProps {
  open: boolean;
  onClose: () => void;
  title: string;
  mode: 'platform' | 'shop';
  shopId?: number | null;
  /** Dòng đang chọn (mode shop: chỉ dòng của shop đó). */
  lines: PricingLine[];
  selection: VoucherSelection;
  ctx: PricingContext;
  onConfirm: (next: VoucherSelection) => void;
  /** Voucher tìm được qua ô nhập mã (chưa có trong kho) — thêm vào bộ tính giá. */
  onFound?: (v: Voucher) => void;
}

const STUB: Record<Voucher['kind'], string> = {
  platform: 'bg-brand-500',
  shop: 'bg-flash-500',
  freeship: 'bg-mint-500',
};
const KIND_ICON = { platform: 'ticket', shop: 'store', freeship: 'truck' } as const;

export function VoucherPicker({
  open,
  onClose,
  title,
  mode,
  shopId = null,
  lines,
  selection,
  ctx,
  onConfirm,
  onFound,
}: VoucherPickerProps) {
  const { t, locale } = useT();
  const [draft, setDraft] = useState<VoucherSelection>(selection);
  const [code, setCode] = useState('');
  const find = useFindVoucher();

  useEffect(() => {
    if (open) {
      setDraft(selection);
      setCode('');
    }
    // chỉ chụp lựa chọn lúc mở hộp
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  const pool = useMemo(
    () =>
      usableVouchers(ctx.vouchers).filter((v) =>
        mode === 'shop' ? v.kind === 'shop' && v.shopId === shopId : v.kind !== 'shop',
      ),
    [ctx.vouchers, mode, shopId],
  );
  const q = code.trim().toLowerCase();
  const shown = q ? pool.filter((v) => `${v.code} ${v.label}`.toLowerCase().includes(q)) : pool;

  const isOn = (v: Voucher) =>
    v.kind === 'freeship'
      ? draft.freeship === v.id
      : v.kind === 'shop'
        ? draft.shop[v.shopId ?? -1] === v.id
        : draft.platform === v.id;
  const toggle = (v: Voucher) =>
    setDraft((d) => {
      const on = isOn(v);
      if (v.kind === 'freeship') return { ...d, freeship: on ? null : v.id };
      if (v.kind === 'platform') return { ...d, platform: on ? null : v.id };
      const shop = { ...d.shop };
      if (on) delete shop[v.shopId ?? -1];
      else shop[v.shopId ?? -1] = v.id;
      return { ...d, shop };
    });

  const reasonText = (r: IneligibleReason, min: number) =>
    t(`promotions.picker.reason.${r}`, { amount: formatVND(min, { locale }) });

  const apply = async () => {
    if (!code.trim()) return;
    try {
      const v = await find.mutateAsync(code);
      const fits = mode === 'shop' ? v.kind === 'shop' && v.shopId === shopId : v.kind !== 'shop';
      const c = voucherCheck(v, lines, { ...ctx, vouchers: [...ctx.vouchers, v] }, selection);
      if (!fits || v.status !== 'active') {
        toast.error(t('errors.GNE-VCH-4041'));
        return;
      }
      if (!c.ok) {
        toast.error(t('promotions.picker.not_eligible'));
        return;
      }
      onFound?.(v);
      if (!isOn(v)) toggle(v);
      setCode('');
      toast.success(t('promotions.picker.applied'));
    } catch (e) {
      toast.error(errorMessage(e, t));
    }
  };

  const chosen = [draft.platform, draft.freeship, ...Object.values(draft.shop)]
    .map((id) => ctx.vouchers.find((v) => v.id === id))
    .filter((v): v is Voucher => !!v && (mode === 'shop' ? v.kind === 'shop' : v.kind !== 'shop'));
  const saving = chosen
    .filter((v) => v.kind !== 'freeship')
    .reduce((n, v) => n + voucherDiscount(v, lines, ctx, draft), 0);
  const hasFreeship = chosen.some((v) => v.kind === 'freeship');

  const section = (head: string, list: Voucher[]) =>
    list.length === 0 ? null : (
      <section className="mb-5">
        <h3 className="m-0 mb-2.5 font-bold text-sm text-strong">{head}</h3>
        <ul className="list-none m-0 p-0 flex flex-col gap-2.5">
          {list.map((v) => {
            const c = voucherCheck(v, lines, ctx, draft);
            const on = isOn(v);
            return (
              <li key={v.id}>
                <button
                  type="button"
                  aria-pressed={on}
                  disabled={!c.ok}
                  onClick={() => toggle(v)}
                  className={cn(
                    'w-full flex flex-col text-left bg-surface-card rounded-md overflow-hidden p-0',
                    'border-[1.5px] border-solid cursor-pointer disabled:cursor-not-allowed',
                    on
                      ? 'border-primary shadow-[0_0_0_3px_var(--color-primary-tint)]'
                      : 'border-line-subtle',
                    !c.ok && 'opacity-[.72]',
                  )}
                >
                  <span className="flex min-h-[92px] w-full">
                    <span
                      className={cn(
                        'w-24 flex-none flex flex-col items-center justify-center gap-1 text-inverse',
                        c.ok ? STUB[v.kind] : 'bg-gray-400',
                      )}
                    >
                      <Icon name={KIND_ICON[v.kind]} size={22} />
                      <span className="font-extrabold text-[17px]">{v.amountLabel}</span>
                    </span>
                    <span className="flex-1 min-w-0 px-3.5 py-2.5">
                      <span className="block font-bold text-sm text-strong">{v.label}</span>
                      <span className="block text-xs text-muted mt-0.5">
                        {v.conditionLabel} · {v.scopeLabel}
                      </span>
                      {v.categories && (
                        <span className="inline-block mt-[5px] px-[7px] py-px rounded-sm bg-mint-50 text-mint-600 font-semibold text-[11px]">
                          {t('promotions.picker.only', { list: v.categories.join(', ') })}
                        </span>
                      )}
                      {v.excluded && (
                        <span className="inline-block mt-[5px] px-[7px] py-px rounded-sm bg-brand-50 text-brand-600 font-semibold text-[11px]">
                          {t('promotions.picker.except', { list: v.excluded.join(', ') })}
                        </span>
                      )}
                      <span className="flex items-center gap-2 mt-1.5">
                        <span className="font-mono font-semibold text-[11px] text-body bg-gray-100 px-1.5 py-px rounded-sm">
                          {v.code}
                        </span>
                        <span className="text-xs text-flash-600">
                          {t('promotions.picker.expiry', {
                            date: formatDate(v.expiresAt, { locale }),
                          })}
                        </span>
                      </span>
                    </span>
                    <span className="grid place-items-center px-3.5 flex-none" aria-hidden="true">
                      <span
                        className={cn(
                          'w-5 h-5 rounded-full grid place-items-center text-inverse',
                          on
                            ? 'bg-primary'
                            : cn(
                                'border-[1.5px] border-solid border-line-strong',
                                c.ok ? 'bg-surface-card' : 'bg-gray-100',
                              ),
                        )}
                      >
                        {on && <Icon name="check" size={14} />}
                      </span>
                    </span>
                  </span>
                  {!c.ok && (
                    <span className="flex items-center gap-1.5 bg-amber-50 text-amber-600 px-3 py-1.5 text-xs">
                      <Icon name="info" size={13} />
                      {reasonText(c.reason, c.minOrder)}
                    </span>
                  )}
                </button>
              </li>
            );
          })}
        </ul>
      </section>
    );

  return (
    <Modal open={open} onClose={onClose} width={620} title={title} bodyClassName="!p-0">
      <div className="flex flex-col h-[68vh] max-h-[640px]">
        <form
          className="flex gap-2.5 px-5 py-3.5 bg-surface-sunken border-0 border-b border-solid border-line-subtle"
          onSubmit={(e) => {
            e.preventDefault();
            void apply();
          }}
        >
          <label className="flex-1 flex items-center gap-2.5 bg-surface-card border-[1.5px] border-solid border-line rounded-md px-3 focus-within:border-primary">
            <span className="text-sm text-muted whitespace-nowrap">
              {t('promotions.picker.code_label')}
            </span>
            <input
              value={code}
              onChange={(e) => setCode(e.target.value)}
              placeholder={t('promotions.picker.code_placeholder')}
              className="flex-1 min-w-0 border-0 outline-none h-10 bg-transparent text-base"
            />
          </label>
          <Button
            type="submit"
            variant="secondary"
            disabled={!code.trim()}
            loading={find.isPending}
          >
            {t('common.action.apply')}
          </Button>
        </form>
        <div className="flex-1 overflow-y-auto px-5 py-4">
          {shown.length === 0 ? (
            <div className="grid place-items-center gap-2.5 py-12 text-subtle">
              <Icon name="ticket" size={40} />
              <span>{t('promotions.picker.empty')}</span>
            </div>
          ) : mode === 'shop' ? (
            section(t('promotions.picker.shop_group'), shown)
          ) : (
            <>
              {section(
                t('promotions.picker.platform_group'),
                shown.filter((v) => v.kind === 'platform'),
              )}
              {section(
                t('promotions.picker.freeship_group'),
                shown.filter((v) => v.kind === 'freeship'),
              )}
            </>
          )}
        </div>
        <div className="flex items-center gap-2.5 px-5 py-3.5 border-0 border-t border-solid border-line-subtle">
          <div className="flex-1 min-w-0 text-sm text-muted">
            {chosen.length === 0 ? (
              t('promotions.picker.none')
            ) : (
              <>
                {t('promotions.picker.chosen', { count: chosen.length })}{' '}
                <b className="text-primary">
                  {t('promotions.picker.saving')}{' '}
                  {[
                    saving > 0 ? formatVND(saving, { locale }) : '',
                    hasFreeship ? t('promotions.picker.shipping_fee') : '',
                  ]
                    .filter(Boolean)
                    .join(' + ')}
                </b>
              </>
            )}
          </div>
          <Button variant="ghost" onClick={onClose}>
            {t('cart.variant.back')}
          </Button>
          <Button
            onClick={() => {
              onConfirm(draft);
              onClose();
            }}
          >
            {t('cart.confirm.ok')}
          </Button>
        </div>
      </div>
    </Modal>
  );
}
