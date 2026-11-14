// Thanh tổng dính đáy của giỏ (prototype Cart.jsx .tr-cartbar) — plan G10.06: chọn tất cả, xóa nhiều,
// voucher sàn + Điểm (slot), "Genie giữ … đến khi bạn nhận hàng", tổng thanh toán, nút Mua hàng.
// Màn hẹp: .tr-cartbar thành display:contents — công cụ thành thẻ riêng, phần trả tiền dính đáy.
import type { ReactNode } from 'react';
import { useT } from '@/shared/hooks/useT';
import { formatNumber, formatVND } from '@/shared/lib/format';
import { Button, Checkbox, Icon } from '@/shared/ui';
import type { CheckState } from '../domain/cart.rules';

export interface CartBarProps {
  lineCount: number;
  selectedCount: number;
  allState: CheckState;
  /** Số đơn sẽ tách (số shop có dòng được chọn). */
  orderCount: number;
  total: number;
  /** Phí ship + tổng giảm — có khi đã tính giá đầy đủ (voucher, Điểm). */
  shipping?: number;
  discount?: number;
  /** Nút Voucher Genie, công tắc Điểm (nhóm B). */
  tools?: ReactNode;
  busy?: boolean;
  onToggleAll: () => void;
  onRemoveSelected: () => void;
  onCheckout: () => void;
}

export function CartBar({
  lineCount,
  selectedCount,
  allState,
  orderCount,
  total,
  shipping,
  discount = 0,
  tools,
  busy,
  onToggleAll,
  onRemoveSelected,
  onCheckout,
}: CartBarProps) {
  const { t, locale } = useT();
  const vnd = (n: number) => formatVND(n, { locale });
  const info =
    shipping == null
      ? t('cart.bar.orders', { orders: orderCount })
      : t('cart.bar.info', { orders: orderCount, ship: vnd(shipping) }) +
        (discount > 0 ? t('cart.bar.info_discount', { amount: vnd(discount) }) : '');

  return (
    <div className="tr-cartbar">
      <div className="tr-cart-tools">
        <Checkbox
          checked={allState === 'all'}
          indeterminate={allState === 'some'}
          onChange={onToggleAll}
        >
          {t('cart.bar.select_all', { count: formatNumber(lineCount, { locale }) })}
        </Checkbox>
        {selectedCount > 0 && (
          <button
            type="button"
            className="tr-cart-tools__btn text-red-600 font-medium"
            onClick={onRemoveSelected}
            disabled={busy}
          >
            {t('cart.bar.remove_selected', { count: selectedCount })}
          </button>
        )}
        {tools}
        <div className="tr-cart-info--phone gap-[5px] text-brand-700 font-semibold">
          <Icon name="shield-check" size={14} />
          {t('cart.bar.hold_short')}
        </div>
        <div className="tr-cart-info--phone">{info}</div>
      </div>
      <div className="tr-cart-pay">
        <div className="tr-cart-pay__text">
          <div className="tr-wide-only inline-flex items-center gap-[5px] text-xs text-brand-700 font-semibold">
            <Icon name="shield-check" size={14} />
            {t('cart.bar.hold', { amount: vnd(total) })}
          </div>
          <div className="tr-cart-pay__info text-xs text-muted">{info}</div>
          <div className="text-sm text-body">
            <span className="tr-cart-pay__label">
              {t('cart.bar.total')}{' '}
              <span className="tr-wide-only">
                {t('cart.bar.total_count', { count: selectedCount })}
              </span>
            </span>{' '}
            <span className="tr-cart-pay__total align-middle ml-1.5 font-extrabold text-[26px] text-price tabular-nums">
              {vnd(total)}
            </span>
          </div>
        </div>
        <span className="tr-cart-pay__cta">
          <Button size="lg" block disabled={selectedCount === 0 || busy} onClick={onCheckout}>
            {t('cart.bar.checkout', { count: selectedCount })}
          </Button>
        </span>
      </div>
    </div>
  );
}
