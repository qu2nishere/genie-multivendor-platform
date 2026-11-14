// Khối mua (prototype ProductDetail.jsx cột thông tin) — plan G08.25, G08.26, G08.30. Island client:load.
// Tên, đánh giá, đã bán, giá theo phân loại, tiến độ Flash Sale, Đặt trước, shop tạm nghỉ, chọn
// phân loại, số lượng, tồn kho, ghi chú giữ chỗ 15 phút, Thêm vào giỏ / Mua ngay (thanh dính đáy ở
// màn hẹp — .tr-buybar), Hold Note. Liên kết chia sẻ ?v=0-1 chọn sẵn phân loại rồi rời URL.
import { useStore } from '@nanostores/react';
import { Fragment, useEffect, useState } from 'react';
import { AddToCartButton, type CartLineInput } from '@/features/cart';
import { HoldNote } from '@/features/payment';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatCompact, formatNumber, formatVND } from '@/shared/lib/format';
import { Providers } from '@/shared/providers/Providers';
import {
  Badge,
  FlashProgress,
  Icon,
  Price,
  QuantityStepper,
  Rating,
  StatusBadge,
  toast,
} from '@/shared/ui';
import { $selection } from '../application/selection.store';
import {
  decodeSelection,
  isFlashSoldOut,
  maxQty,
  missingGroups,
  priceOf,
  skuOf,
  stockOf,
  variantLabel,
  LOW_STOCK,
  type Selection,
} from '../domain/product.rules';
import type { Product } from '../domain/product.types';

export interface BuyBoxProps {
  product: Product;
  shopName: string;
  /** Shop đang tạm nghỉ: ngày mở lại (null = đang bán). */
  vacationUntil: string | null;
}

const LABEL = 'text-sm text-muted';

function chip(on: boolean, disabled: boolean) {
  return cn(
    'relative inline-flex min-h-[38px] items-center gap-[7px] rounded-md border-[1.5px] border-solid bg-surface-card px-3.5 text-sm font-medium max-md:min-h-[44px]',
    on ? 'border-primary shadow-[inset_0_0_0_1px_var(--color-primary)]' : 'border-line',
    disabled
      ? 'cursor-not-allowed text-disabled opacity-55'
      : 'cursor-pointer text-body hover:border-primary',
  );
}

function Box({ product: p, shopName, vacationUntil }: BuyBoxProps) {
  const { t, locale } = useT();
  const sel = useStore($selection);
  const [qty, setQty] = useState(1);

  // Lần đầu: lựa chọn trống; liên kết chia sẻ chọn sẵn rồi bỏ tham số khỏi URL (prototype)
  useEffect(() => {
    const u = new URL(window.location.href);
    const shared = u.searchParams.get('v');
    $selection.set(decodeSelection(p.variantGroups, shared));
    if (shared || u.searchParams.get('src')) {
      u.searchParams.delete('v');
      u.searchParams.delete('src');
      window.history.replaceState(window.history.state, '', u);
    }
    return () => $selection.set({});
  }, [p.id, p.variantGroups]);

  const pick = (group: string, i: number) => {
    $selection.set({ ...sel, [group]: i } satisfies Selection);
    setQty(1);
  };

  const price = priceOf(p, sel);
  const stock = stockOf(p.variantGroups, sel);
  const soldOut = stock === 0 && !p.preorderDays;
  const flashOut = isFlashSoldOut(p);
  const vacation = vacationUntil !== null;
  const limit = maxQty(p, stock);

  const showVariants = () => {
    if (window.matchMedia('(max-width: 768px)').matches)
      document.getElementById('tr-variants')?.scrollIntoView({ block: 'start' });
  };

  const getLine = (): CartLineInput | null => {
    if (flashOut) {
      toast.error(t('product.buy.flash_out'));
      return null;
    }
    const missing = missingGroups(p.variantGroups, sel);
    if (missing.length) {
      toast.error(
        missing.length === p.variantGroups.length
          ? t('product.buy.pick_variant')
          : t('product.buy.pick_all'),
      );
      showVariants();
      return null;
    }
    const variantId = skuOf(p, sel);
    if (variantId == null) {
      toast.error(t('product.buy.pick_all'));
      return null;
    }
    return {
      variantId,
      productId: p.id,
      productSlug: p.slug,
      shopId: p.shopId,
      title: p.title,
      image: p.images[0] ?? p.image,
      variantLabel: variantLabel(p.variantGroups, sel),
      price,
      originalPrice: p.originalPrice,
      qty,
      stock: Number.isFinite(stock) ? stock : 999,
    };
  };

  return (
    <div className="min-w-0">
      <div className="mb-2 flex flex-wrap gap-2">
        {p.authentic && <Badge variant="primary">{t('product.badge.authentic')}</Badge>}
        {p.flash && (
          <Badge variant="flash" icon="zap">
            {t('product.badge.flash')}
          </Badge>
        )}
        {p.preorderDays && (
          <Badge variant="primary" icon="clock">
            {t('common.state.preorder')}
          </Badge>
        )}
        {!p.authentic && <Badge variant="outline">{t('product.badge.genuine')}</Badge>}
      </div>
      <h1 className="m-0 mb-2.5 text-[22px] font-semibold leading-[1.3] text-strong">{p.title}</h1>
      <div className="mb-4 flex flex-wrap items-center gap-4 border-0 border-b border-solid border-line-subtle pb-4">
        <a href="#reviews" className="text-inherit no-underline hover:no-underline">
          <Rating value={p.rating} count={p.reviewCount} />
        </a>
        <span className="text-line" aria-hidden="true">
          |
        </span>
        <span className="text-sm text-muted">
          {t('product.sold')} <b className="text-body">{formatCompact(p.soldCount, { locale })}</b>
        </span>
      </div>

      <div className="mb-[18px] flex items-center gap-3.5" aria-live="polite">
        <Price amount={price} original={p.originalPrice} size="lg" />
        {p.discountPct > 0 && <Badge variant="flash">-{p.discountPct}%</Badge>}
      </div>

      {p.flash && (
        <div className="mb-[18px]">
          <FlashProgress sold={p.flash.sold} total={p.flash.total} />
          <div className={cn('mt-[5px] text-xs', flashOut ? 'text-red-600' : 'text-muted')}>
            {flashOut ? t('product.buy.flash_out') : t('product.buy.flash_limit')}
          </div>
        </div>
      )}
      {p.preorderDays && (
        <div className="mb-[18px] flex items-center gap-2 rounded-md bg-blue-50 px-3 py-2 text-sm text-body">
          <Icon name="clock" size={15} className="text-blue-600" />
          {t('product.buy.preorder_note', { days: p.preorderDays })}
        </div>
      )}
      {vacation && (
        <div
          role="status"
          className="mb-[18px] flex items-center gap-2 rounded-md bg-gray-100 px-3 py-2.5 text-sm text-body"
        >
          <Icon name="clock" size={15} />
          {t('product.buy.vacation', { until: vacationUntil })}
        </div>
      )}

      <div
        id="tr-variants"
        className="mb-[18px] grid scroll-mt-[130px] grid-cols-[90px_1fr] items-center gap-x-4 gap-y-3.5 max-md:grid-cols-1 max-md:gap-y-2"
      >
        {p.variantGroups.map((g, gi) => (
          <Fragment key={g.name}>
            <span className={LABEL} id={`vg-${gi}`}>
              {g.name}
            </span>
            <div className="flex flex-wrap gap-2" role="group" aria-labelledby={`vg-${gi}`}>
              {g.options.map((o, i) => {
                const out = g.controlsStock && (o.stock ?? 0) === 0;
                const on = sel[g.name] === i;
                return (
                  <button
                    key={o.label}
                    type="button"
                    disabled={out}
                    aria-pressed={on}
                    onClick={() => pick(g.name, i)}
                    className={chip(on, out)}
                  >
                    {o.swatch && (
                      <span
                        aria-hidden="true"
                        className="h-3.5 w-3.5 rounded-full shadow-[inset_0_0_0_1px_rgba(0,0,0,.12)]"
                        style={{ background: o.swatch }}
                      />
                    )}
                    {o.label}
                    {/* prototype: chỉ nhóm không quyết định tồn (Phiên bản) ghi chênh giá */}
                    {o.priceDelta && !g.controlsStock
                      ? ` (+${formatVND(o.priceDelta, { locale })})`
                      : ''}
                    {out && <span className="sr-only"> — {t('common.state.out_of_stock')}</span>}
                  </button>
                );
              })}
            </div>
          </Fragment>
        ))}
        <span className={LABEL} id="qty-label">
          {t('common.label.quantity')}
        </span>
        <div className="flex flex-wrap items-center gap-3.5">
          <QuantityStepper
            value={qty}
            max={limit}
            onChange={setQty}
            aria-label={t('common.label.quantity')}
            disabled={soldOut}
          />
          {soldOut ? (
            <StatusBadge status="ERROR" label={t('common.state.out_of_stock')} dot={false} />
          ) : (
            Number.isFinite(stock) && (
              <span className="text-sm text-muted">
                {t('product.buy.stock_a')}{' '}
                <b className={stock <= LOW_STOCK ? 'text-red-600' : 'text-body'}>
                  {formatNumber(stock, { locale })}
                </b>{' '}
                {t('product.buy.stock_b')}
              </span>
            )
          )}
          {stock > 0 && stock <= LOW_STOCK && (
            <Badge variant="warning">
              <Icon name="clock" size={11} /> {t('product.buy.low')}
            </Badge>
          )}
        </div>
      </div>

      <div className="mb-[18px] flex items-center gap-2 rounded-md bg-amber-50 px-3 py-2 text-sm text-amber-600">
        <Icon name="lock" size={15} />
        <span>
          {t('product.buy.hold_a')} <b>{t('product.buy.hold_b')}</b> {t('product.buy.hold_c')}
        </span>
      </div>

      <div className="tr-buybar">
        <AddToCartButton
          mode="add"
          variant="secondary"
          iconLeft="shopping-cart"
          label={t('product.buy.add')}
          disabled={soldOut || vacation}
          getLine={getLine}
        />
        <AddToCartButton
          mode="buy"
          label={p.preorderDays ? t('product.buy.preorder') : t('product.buy.now')}
          disabled={soldOut || vacation}
          getLine={getLine}
        />
      </div>
      <HoldNote className="mt-4">
        {t('product.buy.hold_note_a')} <b>{shopName}</b> {t('product.buy.hold_note_b')}
      </HoldNote>
    </div>
  );
}

export default function BuyBox(props: BuyBoxProps) {
  return (
    <Providers>
      <Box {...props} />
    </Providers>
  );
}
