import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatVND, partsVND } from '@/shared/lib/format';

export interface PriceProps {
  /** Giá hiện tại (VND số nguyên). */
  amount: number;
  /** Giá gốc để gạch; chỉ hiện khi lớn hơn giá hiện tại. */
  original?: number | null;
  size?: 'sm' | 'md' | 'lg';
  /** Hiện nhãn -x% cạnh giá gạch. */
  showPercent?: boolean;
  className?: string;
}

const NOW: Record<NonNullable<PriceProps['size']>, string> = {
  sm: 'text-lg',
  md: 'text-2xl',
  lg: 'text-4xl',
};

/** Phần trăm giảm làm tròn: (gốc − hiện tại) / gốc. */
export function discountPercent(amount: number, original?: number | null): number {
  if (!original || original <= amount) return 0;
  return Math.round(((original - amount) / original) * 100);
}

/**
 * Giá VND: ₫ nhỏ, nâng cao, gạch chân; giá gốc gạch ngang (prototype PriceVND / .lz-price) — G06.20.
 * Chuỗi đọc cho trình đọc màn hình đầy đủ "150.000₫, giá gốc 299.000₫".
 */
export function Price({ amount, original, size = 'md', showPercent, className }: PriceProps) {
  const { t, locale } = useT();
  const [now, sym] = partsVND(amount, { locale });
  const hasWas = original != null && original > amount;
  const pct = discountPercent(amount, original);
  return (
    <span
      className={cn(
        'lz-price inline-flex items-baseline flex-wrap gap-x-2 gap-y-0.5 tabular-nums',
        className,
      )}
    >
      <span className="sr-only">
        {formatVND(amount, { locale })}
        {hasWas && `, ${t('ui.price.original')} ${formatVND(original, { locale })}`}
      </span>
      <span
        aria-hidden="true"
        className={cn('lz-price__now font-bold leading-none text-price', NOW[size])}
      >
        {now}
        <span className="text-[.62em] align-[.42em] underline underline-offset-1 font-bold">
          {sym}
        </span>
      </span>
      {hasWas && (
        <span aria-hidden="true" className="lz-price__was text-sm text-subtle line-through">
          {formatVND(original, { locale, symbol: false })}
          {sym}
        </span>
      )}
      {showPercent && pct > 0 && (
        <span aria-hidden="true" className="text-xs font-bold text-flash-500">
          -{pct}%
        </span>
      )}
    </span>
  );
}
