// Một dòng giỏ (prototype Cart.jsx .tr-line): chọn, ảnh + tên (liên kết thật — R16), cảnh báo đổi giá,
// lý do không mua được, phân loại (mở VariantModal), đơn giá, số lượng, thành tiền, xóa.
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatVND } from '@/shared/lib/format';
import { Checkbox, Icon, Price, QuantityStepper } from '@/shared/ui';
import { lineSubtotal, priceChange } from '../domain/cart.rules';
import type { CartLine } from '../domain/cart.types';

export const productHref = (l: Pick<CartLine, 'productSlug' | 'productId'>) =>
  `/p/${l.productSlug}-${l.productId}`;

export interface CartLineRowProps {
  line: CartLine;
  selected: boolean;
  /** Phần giảm của voucher sàn phân bổ về dòng (hiện khi dòng được chọn). */
  cut?: number;
  disabled?: boolean;
  onToggle: () => void;
  onQty: (qty: number) => void;
  onVariant: () => void;
  onRemove: () => void;
}

export function CartLineRow({
  line: l,
  selected,
  cut = 0,
  disabled,
  onToggle,
  onQty,
  onVariant,
  onRemove,
}: CartLineRowProps) {
  const { t, locale } = useT();
  const change = priceChange(l);
  const href = productHref(l);
  return (
    <div className="tr-line" data-available={l.available ? undefined : 'false'}>
      <span className="tr-line__check">
        <Checkbox
          aria-label={t('cart.line.select', { title: l.title })}
          checked={selected}
          disabled={!l.available}
          onChange={onToggle}
        />
      </span>
      <a
        href={href}
        className={cn('tr-line__img block overflow-hidden', !l.available && 'opacity-50')}
        tabIndex={-1}
        aria-hidden="true"
      >
        <img
          src={l.image}
          alt=""
          width={72}
          height={72}
          loading="lazy"
          className="w-full h-full object-cover"
        />
      </a>
      <div className="tr-line__info">
        <a
          href={href}
          className={cn(
            'tr-line__title no-underline hover:underline hover:text-strong',
            !l.available && 'text-subtle',
          )}
        >
          {l.title}
        </a>
        {change && l.available && (
          <span
            className={cn(
              'inline-flex items-center gap-1.5 mt-1.5 px-[9px] py-[3px] rounded-pill font-semibold text-xs',
              change === 'up' ? 'bg-red-50 text-red-600' : 'bg-mint-50 text-mint-600',
            )}
          >
            <Icon
              name={change === 'up' ? 'triangle-alert' : 'arrow-up-right'}
              size={13}
              className={change === 'down' ? 'rotate-90' : undefined}
            />
            {t(change === 'up' ? 'cart.line.price_up' : 'cart.line.price_down')}
          </span>
        )}
        {!l.available && l.unavailableReason && (
          <span className="inline-flex items-center gap-1.5 mt-1.5 px-[9px] py-[3px] rounded-pill font-semibold text-xs bg-gray-100 text-muted">
            <Icon name="circle-alert" size={13} />
            {t(`cart.line.unavailable.${l.unavailableReason}`)}
          </span>
        )}
        <button type="button" className="tr-line__variant" onClick={onVariant} disabled={disabled}>
          <span>
            {t('cart.line.variant', { variant: l.variantLabel })}
            <Icon name="chevron-down" size={12} />
          </span>
        </button>
      </div>
      <div className="tr-line__unit">
        {change ? (
          <div>
            <Price amount={l.price} size="sm" />
            <div className="text-xs text-subtle line-through mt-0.5 tabular-nums">
              {formatVND(l.snapshotPrice ?? l.price, { locale })}
            </div>
            <div className="font-semibold text-[11px] text-subtle">
              {t('cart.line.price_at_add')}
            </div>
          </div>
        ) : (
          <Price amount={l.price} original={l.originalPrice} size="sm" />
        )}
      </div>
      <span className="tr-line__qty">
        <QuantityStepper
          value={l.qty}
          min={0}
          max={Math.max(1, l.stock) + 1}
          disabled={disabled || !l.available}
          onChange={onQty}
          aria-label={t('cart.line.qty', { title: l.title })}
        />
      </span>
      <div className="tr-line__total">
        <div className="font-bold text-base text-price tabular-nums">
          {formatVND(lineSubtotal(l), { locale })}
        </div>
        {selected && cut > 0 && (
          <div className="text-xs text-mint-600 mt-[3px] tabular-nums">
            −{formatVND(cut, { locale })}
          </div>
        )}
      </div>
      <button
        type="button"
        className="tr-line__del hover:bg-gray-50 hover:text-red-600"
        onClick={onRemove}
        disabled={disabled}
        aria-label={t('cart.line.remove', { title: l.title })}
      >
        <Icon name="trash-2" size={18} />
      </button>
    </div>
  );
}
