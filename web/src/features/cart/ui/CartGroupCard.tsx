// Một shop trong giỏ (prototype Cart.jsx: .tr-vhead + dòng + .tr-vfoot) — mỗi shop thành một đơn riêng.
import type { ReactNode } from 'react';
import { useT } from '@/shared/hooks/useT';
import { formatVND } from '@/shared/lib/format';
import { Badge, Checkbox, Icon, Seal } from '@/shared/ui';
import { checkState, isBuyable } from '../domain/cart.rules';
import type { CartGroup, CartLine } from '../domain/cart.types';
import { CartLineRow } from './CartLineRow';

export interface CartGroupCardProps {
  group: CartGroup;
  selected: ReadonlySet<string>;
  /** "Phí ship 18.000₫" / "Shop miễn phí vận chuyển" — có khi đã tính phí ship. */
  shipLabel?: string;
  /** Giảm của voucher shop cho nhóm. */
  shopCut?: number;
  /** Giảm voucher sàn phân bổ theo dòng. */
  lineCuts?: Record<string, number>;
  /** Nút "Thêm voucher của Shop" (nhóm B). */
  voucherSlot?: ReactNode;
  busy?: boolean;
  onToggleGroup: () => void;
  onToggle: (l: CartLine) => void;
  onQty: (l: CartLine, qty: number) => void;
  onVariant: (l: CartLine) => void;
  onRemove: (l: CartLine) => void;
}

export function CartGroupCard({
  group,
  selected,
  shipLabel,
  shopCut = 0,
  lineCuts = {},
  voucherSlot,
  busy,
  onToggleGroup,
  onToggle,
  onQty,
  onVariant,
  onRemove,
}: CartGroupCardProps) {
  const { t, locale } = useT();
  const { shop, lines } = group;
  const buyable = lines.filter(isBuyable).map((l) => l.key);
  const state = checkState(selected, buyable);
  const picked = lines.filter((l) => selected.has(l.key));
  const sub = picked.reduce((n, l) => n + l.price * l.qty, 0);
  const lineCut = picked.reduce((n, l) => n + (lineCuts[l.key] ?? 0), 0);

  return (
    <section
      className="bg-surface-card rounded-lg shadow-sm mb-4 overflow-hidden"
      aria-label={shop.name}
    >
      <div className="tr-vhead">
        <Checkbox
          aria-label={t('cart.group.select_all', { shop: shop.name })}
          checked={state === 'all'}
          indeterminate={state === 'some'}
          disabled={buyable.length === 0}
          onChange={onToggleGroup}
        />
        <Seal id={shop.id} name={shop.name} size={28} />
        <a href={`/shop/${shop.id}`} className="tr-vhead__name no-underline hover:underline">
          {shop.name}
        </a>
        {shop.authentic && <Badge variant="primary">{t('common.authentic')}</Badge>}
        <a
          href={`/chat?shop=${shop.id}`}
          className="inline-flex items-center gap-1 min-h-9 text-xs text-muted no-underline hover:text-primary"
        >
          <Icon name="message-circle" size={14} />
          {t('common.action.chat')}
        </a>
        <span className="tr-vhead__meta">
          {t('cart.group.separate')}
          {shipLabel ? ` · ${shipLabel}` : ''}
        </span>
      </div>
      {lines.map((l) => (
        <CartLineRow
          key={l.key}
          line={l}
          selected={selected.has(l.key)}
          cut={lineCuts[l.key]}
          disabled={busy}
          onToggle={() => onToggle(l)}
          onQty={(q) => onQty(l, q)}
          onVariant={() => onVariant(l)}
          onRemove={() => onRemove(l)}
        />
      ))}
      <div className="tr-vfoot">
        {voucherSlot}
        <span className="tr-vfoot__sum">
          {t('cart.group.subtotal', { count: picked.length })}{' '}
          <b className="text-price tabular-nums">
            {formatVND(Math.max(0, sub - shopCut - lineCut), { locale })}
          </b>
        </span>
      </div>
    </section>
  );
}
