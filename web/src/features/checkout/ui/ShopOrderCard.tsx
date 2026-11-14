// Một đơn con theo shop ở trang Thanh toán (prototype Checkout.jsx "sub-orders") — plan G10.18–G10.19:
// dòng hàng (dòng hết hàng: gạch, nhãn, Bỏ món / sửa số lượng ở giỏ), voucher shop, lời nhắn,
// voucher sàn phân bổ, phí ship + freeship, tổng đơn. Màn hẹp gập còn 1 dòng + "Xem thêm".
import { useState } from 'react';
import type { CartGroup, CartLine } from '@/features/cart';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatVND } from '@/shared/lib/format';
import { Badge, Icon, Seal, StatusBadge } from '@/shared/ui';
import { NOTE_MAX } from '../domain/checkout.types';

export interface ShopOrderCardProps {
  group: CartGroup;
  index: number;
  count: number;
  /** Khóa dòng vừa hết hàng (GNE-INV-4090). */
  dead: ReadonlySet<string>;
  perLine: Record<string, number>;
  shopCut: number;
  shipCut: number;
  shipFee: number;
  freeByShop: boolean;
  note: string;
  onNote: (v: string) => void;
  onShopVoucher: () => void;
  onDrop: (l: CartLine) => void;
}

export function ShopOrderCard({
  group,
  index,
  count,
  dead,
  perLine,
  shopCut,
  shipCut,
  shipFee,
  freeByShop,
  note,
  onNote,
  onShopVoucher,
  onDrop,
}: ShopOrderCardProps) {
  const { t, locale } = useT();
  const vnd = (n: number) => formatVND(n, { locale });
  const [open, setOpen] = useState(false);
  const live = group.lines.filter((l) => !dead.has(l.key));
  const sub = live.reduce((n, l) => n + l.price * l.qty, 0);
  const platformCut = live.reduce((n, l) => n + (perLine[l.key] ?? 0), 0);
  const fee = live.length ? shipFee : 0;
  const total = Math.max(0, sub + fee - shopCut - shipCut - platformCut);

  return (
    <section
      className="bg-surface-card rounded-lg px-[18px] py-4 mb-3.5 shadow-sm"
      aria-label={group.shop.name}
    >
      <div className="flex items-center gap-2.5 pb-3 mb-1 border-0 border-b border-solid border-line-subtle">
        <Seal id={group.shop.id} name={group.shop.name} size={28} />
        <span className="font-bold text-base text-strong">{group.shop.name}</span>
        {group.shop.authentic && <Badge variant="primary">{t('common.authentic')}</Badge>}
        <span className="ml-auto inline-flex items-center gap-1.5">
          <span className="text-xs text-muted">{t('checkout.order.separate')}</span>
          <span className="font-semibold text-xs text-body bg-gray-100 px-[7px] py-0.5 rounded-sm">
            {t('checkout.order.index', { n: index, count })}
          </span>
        </span>
      </div>

      <ul className="tr-lines list-none m-0 p-0" data-open={open ? 1 : 0}>
        {group.lines.map((l) => {
          const gone = dead.has(l.key);
          return (
            <li
              key={l.key}
              className={cn(
                'flex gap-3 py-3 border-0 border-b border-solid border-line-subtle',
                gone && 'opacity-70',
              )}
            >
              <img
                src={l.image}
                alt=""
                width={56}
                height={56}
                loading="lazy"
                className={cn('w-14 h-14 rounded-md object-cover flex-none', gone && 'grayscale')}
              />
              <div className="flex-1 min-w-0">
                <p className="m-0 text-sm text-body line-clamp-1">{l.title}</p>
                <p className="m-0 mt-[3px] text-xs text-muted">
                  {t('cart.line.variant', { variant: l.variantLabel })}
                </p>
                {gone && (
                  <div className="mt-1.5 flex items-center gap-2 flex-wrap">
                    <StatusBadge
                      status="ERROR"
                      label={t('common.state.out_of_stock')}
                      dot={false}
                    />
                    <span className="text-xs text-red-600">{t('checkout.order.oos')}</span>
                    <button
                      type="button"
                      onClick={() => onDrop(l)}
                      className="border-0 bg-transparent p-0 text-xs font-semibold text-link cursor-pointer hover:underline"
                    >
                      {t('checkout.order.drop')}
                    </button>
                    <a href="/cart" className="text-xs font-semibold text-link">
                      {t('checkout.order.edit_qty')}
                    </a>
                  </div>
                )}
              </div>
              <div className="text-right flex-none">
                <div
                  className={cn(
                    'font-semibold text-sm tabular-nums',
                    gone ? 'text-disabled line-through' : 'text-body',
                  )}
                >
                  {vnd(l.price)}
                </div>
                <div className="text-xs text-muted">x{l.qty}</div>
                {!gone && (perLine[l.key] ?? 0) > 0 && (
                  <div className="text-xs text-mint-600 mt-0.5 tabular-nums">
                    −{vnd(perLine[l.key])}
                  </div>
                )}
              </div>
            </li>
          );
        })}
      </ul>
      {group.lines.length > 1 && !open && (
        <button type="button" className="tr-lines__more min-h-11" onClick={() => setOpen(true)}>
          {t('checkout.order.more', { count: group.lines.length - 1 })}
          <Icon name="chevron-down" size={15} />
        </button>
      )}

      <div className="grid grid-cols-2 gap-2.5 py-3 border-0 border-b border-solid border-line-subtle max-sm:grid-cols-1">
        <button
          type="button"
          onClick={onShopVoucher}
          className="flex items-center gap-2 bg-gray-25 rounded-md px-3 py-[9px] text-sm text-body cursor-pointer border-0 text-left min-h-11"
        >
          <Icon name="ticket" size={15} className="text-flash-500" />
          <span>
            {shopCut
              ? t('checkout.order.shop_voucher_on', { amount: vnd(shopCut) })
              : t('checkout.order.shop_voucher')}
          </span>
          <Icon name="chevron-right" size={14} className="ml-auto text-subtle" />
        </button>
        <label className="flex items-center gap-2 bg-gray-25 rounded-md px-3 py-[9px] cursor-text min-h-11">
          <Icon name="message-square" size={15} className="text-muted" />
          <input
            value={note}
            maxLength={NOTE_MAX}
            onChange={(e) => onNote(e.target.value)}
            placeholder={t('checkout.order.note')}
            aria-label={t('checkout.order.note_label', { shop: group.shop.name })}
            className="border-0 outline-none text-sm flex-1 min-w-0 bg-transparent"
          />
        </label>
      </div>

      {platformCut > 0 && (
        <div className="flex justify-between items-center pt-3 text-sm text-body">
          <span className="flex items-center gap-1.5">
            <Icon name="ticket" size={14} className="text-blue-600" />
            {t('checkout.order.platform_cut')}
          </span>
          <span className="text-blue-600 font-semibold tabular-nums">−{vnd(platformCut)}</span>
        </div>
      )}
      <div className="flex items-center gap-2.5 pt-3 flex-wrap">
        <Icon name="truck" size={16} className="text-mint-600" />
        <span className="text-sm text-body">
          {t('checkout.order.ship')} ·{' '}
          {fee === 0 && freeByShop ? t('cart.group.ship_free') : vnd(fee)}
          {shipCut > 0 && (
            <span className="text-mint-600 ml-1.5">
              ({t('checkout.order.freeship', { amount: vnd(shipCut) })})
            </span>
          )}
        </span>
        <span className="ml-auto text-sm text-muted">
          {t('checkout.order.total', { count: live.length })}{' '}
          <b className="text-price text-base tabular-nums">{vnd(total)}</b>
        </span>
      </div>
    </section>
  );
}

/** Tổng của một đơn con — cùng công thức với thẻ (dùng dựng bảng giá khi đặt hàng). */
export function orderTotal(
  lines: CartLine[],
  perLine: Record<string, number>,
  fee: number,
  shopCut: number,
  shipCut: number,
): { subtotal: number; discount: number; total: number } {
  const subtotal = lines.reduce((n, l) => n + l.price * l.qty, 0);
  const discount = shopCut + shipCut + lines.reduce((n, l) => n + (perLine[l.key] ?? 0), 0);
  return { subtotal, discount, total: Math.max(0, subtotal + fee - discount) };
}
