// Đổi phân loại ngay trong giỏ (prototype Cart.jsx VariantModal) — plan G10.05.
// Nhóm phân loại lấy từ API 5.10; lựa chọn hết hàng bị khóa; "Xác nhận" khi đã chọn đủ mọi nhóm.
import { useEffect, useState } from 'react';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatVND } from '@/shared/lib/format';
import { Button, Modal, Spinner } from '@/shared/ui';
import { pickFromLabel, variantOfPick } from '../domain/cart.rules';
import type { CartLine } from '../domain/cart.types';
import { useVariantChoice } from '../application/useVariantChoice';

export interface VariantModalProps {
  line: CartLine | null;
  onClose: () => void;
  onConfirm: (line: CartLine, next: { id: number; label: string; stock: number }) => void;
  busy?: boolean;
}

export function VariantModal({ line, onClose, onConfirm, busy }: VariantModalProps) {
  const { t, locale } = useT();
  const q = useVariantChoice(line?.productId ?? null);
  const [pick, setPick] = useState<(string | null)[]>([]);
  useEffect(() => {
    if (q.data && line) setPick(pickFromLabel(q.data, line.variantLabel));
  }, [q.data, line]);

  const variant = q.data ? variantOfPick(q.data, pick) : null;
  const stockOfValue = (gi: number, value: string) =>
    q.data?.variants
      .filter((v) => v.options[gi] === value)
      .reduce((n, v) => Math.max(n, v.stock), 0) ?? 0;

  return (
    <Modal
      open={line != null}
      onClose={onClose}
      title={t('cart.variant.title')}
      width={460}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            {t('cart.variant.back')}
          </Button>
          <Button
            disabled={!variant || variant.stock <= 0}
            loading={busy}
            onClick={() =>
              line &&
              variant &&
              onConfirm(line, {
                id: variant.id,
                label: variant.options.join(' / '),
                stock: variant.stock,
              })
            }
          >
            {t('cart.variant.confirm')}
          </Button>
        </>
      }
    >
      {line && (
        <div>
          <div className="flex gap-4 mb-5">
            <img
              src={line.image}
              alt=""
              width={80}
              height={80}
              className="w-20 h-20 object-cover rounded-md flex-none"
            />
            <div>
              <p className="m-0 font-semibold text-lg text-price tabular-nums">
                {formatVND(variant?.price ?? line.price, { locale })}
              </p>
              <p className="m-0 text-sm text-muted">
                {t('cart.variant.stock', { stock: variant?.stock ?? line.stock })}
              </p>
            </div>
          </div>
          {q.isPending && (
            <div className="grid place-items-center py-6">
              <Spinner />
            </div>
          )}
          {q.data?.groups.map((g, gi) => (
            <fieldset key={g.name} className="mb-4 border-0 p-0 m-0">
              <legend className="font-medium text-sm text-body mb-2 p-0">{g.name}:</legend>
              <div className="flex gap-2 flex-wrap">
                {g.values.map((v) => {
                  const on = pick[gi] === v;
                  const soldOut = stockOfValue(gi, v) <= 0;
                  return (
                    <button
                      key={v}
                      type="button"
                      aria-pressed={on}
                      disabled={soldOut}
                      onClick={() =>
                        setPick((p) => p.map((x, i) => (i === gi ? (on ? null : v) : x)))
                      }
                      className={cn(
                        'min-h-9 px-3 py-1.5 rounded-sm border border-solid text-sm cursor-pointer',
                        'disabled:cursor-not-allowed disabled:text-disabled disabled:line-through',
                        on
                          ? 'bg-primary-tint border-primary text-primary'
                          : 'bg-gray-50 border-line text-body hover:border-primary',
                      )}
                    >
                      {v}
                    </button>
                  );
                })}
              </div>
            </fieldset>
          ))}
        </div>
      )}
    </Modal>
  );
}
