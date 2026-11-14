import type { ReactNode } from 'react';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatVND } from '@/shared/lib/format';
import { Icon } from './icon/Icon';
import { Seal } from './Seal';

/** Dữ liệu tối thiểu để vẽ thẻ — mỗi feature tự ánh xạ từ kiểu domain của mình. */
export interface ProductCardData {
  id: string | number;
  /** Liên kết thật /p/{slug}-{id} (R16). */
  href: string;
  title: string;
  image: string;
  price: number;
  original?: number;
  discountPct?: number;
  rating?: number;
  shopId: string | number;
  shopName: string;
  freeship?: boolean;
  /** Nhãn góc dưới trái: "Đặt trước", "Shop đang tạm nghỉ", "Chính hãng"… */
  flag?: string;
  soldOut?: boolean;
  /** Tiến độ Flash Sale. */
  flash?: { sold: number; total: number };
}

export interface ProductCardProps {
  product: ProductCardData;
  /** Ô tim yêu thích — feature truyền island WishlistButton vào (plan G06.23). */
  heart?: ReactNode;
  /** Hiện thanh tiến độ Flash Sale dưới thẻ. */
  flashBar?: boolean;
  /** Ảnh nằm trên màn hình đầu → tải sớm (G08.51). */
  priority?: boolean;
  className?: string;
}

/**
 * Thẻ sản phẩm storefront (prototype Shared.jsx CardX, lớp .tr-card*) — G06.23.
 * Giữ tên lớp tr-card* để 9 theme (styles/themes.css) đè được; phần nhìn mặc định viết bằng utility,
 * khớp mức "Marketplace density" của storefront.css (thân 13px, giá 15px, tim 32px).
 */
export function ProductCard({
  product: p,
  heart,
  flashBar,
  priority,
  className,
}: ProductCardProps) {
  const { t, locale } = useT();
  const off = p.discountPct ?? 0;
  const flag = p.soldOut ? t('common.state.out_of_stock') : p.flag;
  return (
    <div className={cn('tr-card relative flex flex-col gap-1.5 min-w-0', className)}>
      <a
        href={p.href}
        className="tr-card__main group flex-1 flex flex-col w-full p-0 border-0 rounded-xl bg-surface-card text-left text-[inherit] no-underline hover:no-underline overflow-hidden focus-visible:outline-none focus-visible:shadow-focus"
      >
        <span className="tr-card__media relative block aspect-square overflow-hidden bg-gray-100">
          <img
            src={p.image}
            alt={p.title}
            width={400}
            height={400}
            loading={priority ? 'eager' : 'lazy'}
            fetchPriority={priority ? 'high' : undefined}
            decoding="async"
            className={cn(
              'w-full h-full object-cover transition-transform duration-slow ease-out group-hover:scale-[1.04]',
              p.soldOut && 'opacity-60',
            )}
          />
          {flag && (
            <span className="tr-card__flag absolute left-2 bottom-2 px-[9px] py-[3px] rounded-pill text-white bg-[rgba(11,31,75,.82)] font-semibold text-[11px] leading-4">
              {flag}
            </span>
          )}
          {off > 0 && (
            <span className="tr-card__off absolute right-1.5 bottom-1.5 px-1.5 py-0.5 rounded-pill bg-flash-500 text-white font-bold text-[11px] leading-[15px]">
              -{off}%
            </span>
          )}
        </span>
        <span className="tr-card__body flex flex-col gap-[3px] pt-2 px-2.5 pb-2.5">
          <span className="tr-card__title text-[13px] leading-[1.4] text-body line-clamp-2 min-h-[2.8em]">
            {p.title}
          </span>
          <span className="tr-card__price flex items-baseline flex-wrap gap-x-[7px] gap-y-0.5 tabular-nums">
            <span className="tr-card__now font-extrabold text-[15px] leading-[1.2] tracking-[-.01em] text-price">
              {formatVND(p.price, { locale })}
            </span>
            {p.original != null && p.original > p.price && (
              <span className="tr-card__was text-xs leading-snug text-subtle line-through">
                {formatVND(p.original, { locale })}
              </span>
            )}
          </span>
          <span className="tr-card__meta flex items-start md:items-center gap-1 md:gap-1.5 min-w-0 text-xs leading-snug text-muted">
            <Seal id={p.shopId} name={p.shopName} size={16} />
            <span className="tr-card__shop flex-1 min-w-0 whitespace-normal md:truncate">
              {p.shopName}
            </span>
            {p.rating != null && (
              <span className="tr-card__rate flex-none inline-flex items-center gap-px md:gap-0.5">
                <Icon
                  name="star"
                  size={12}
                  className="text-gold-500"
                  style={{ fill: 'currentColor' }}
                />
                <span className="sr-only">
                  {t('ui.rating.value', { value: p.rating.toFixed(1) })}
                </span>
                <span aria-hidden="true">{p.rating}</span>
              </span>
            )}
          </span>
          {p.freeship && (
            <span className="tr-card__meta tr-card__ship text-xs leading-snug text-mint-600 font-semibold">
              {t('common.label.free_ship')}
            </span>
          )}
        </span>
      </a>
      {heart && <div className="tr-card__heart-slot absolute top-1.5 right-1.5">{heart}</div>}
      {flashBar && p.flash && <FlashProgress sold={p.flash.sold} total={p.flash.total} />}
    </div>
  );
}

/** Thanh tiến độ Flash Sale (prototype FlashBar, .tr-bar). */
export function FlashProgress({ sold, total }: { sold: number; total: number }) {
  const { t } = useT();
  const pct = total > 0 ? Math.min(100, Math.round((sold / total) * 100)) : 0;
  const out = sold >= total;
  return (
    <div
      className="tr-bar relative h-[18px] rounded-pill bg-flash-50 overflow-hidden"
      role="progressbar"
      aria-valuemin={0}
      aria-valuemax={total}
      aria-valuenow={Math.min(sold, total)}
      aria-label={t('ui.flash.progress')}
    >
      <i className="block h-full bg-flash-100" style={{ width: `${pct}%` }} />
      <span className="absolute inset-0 grid place-items-center font-bold text-[11px] text-flash-600">
        {out ? t('ui.flash.sold_out') : t('ui.flash.sold', { sold, total })}
      </span>
    </div>
  );
}

/** Nút tim tròn 32px (vùng chạm 44px) đặt vào ô `heart` của ProductCard (.tr-card__heart). */
export function HeartToggle({
  liked,
  onToggle,
  disabled,
}: {
  liked: boolean;
  onToggle: () => void;
  disabled?: boolean;
}) {
  const { t } = useT();
  return (
    <button
      type="button"
      onClick={onToggle}
      disabled={disabled}
      aria-pressed={liked}
      aria-label={liked ? t('ui.wishlist.remove') : t('ui.wishlist.add')}
      className={cn(
        'tr-card__heart relative w-8 h-8 grid place-items-center rounded-full bg-white border border-white/70 shadow-sm cursor-pointer',
        'after:content-[""] after:absolute after:-inset-1.5',
        'focus-visible:outline-none focus-visible:shadow-focus',
        liked ? 'text-flash-500' : 'text-gray-700',
      )}
    >
      <Icon name="heart" size={17} style={{ fill: liked ? 'currentColor' : 'none' }} />
    </button>
  );
}
