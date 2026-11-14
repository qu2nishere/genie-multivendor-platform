import { useRef, useState } from 'react';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatNumber } from '@/shared/lib/format';
import { Icon } from './icon/Icon';

export interface RatingProps {
  value: number;
  /** Số lượt đánh giá, hiện "(120)". */
  count?: number;
  /** Đã bán (chuỗi đã định dạng gọn, vd. "1,2k"). */
  sold?: string;
  showNumber?: boolean;
  size?: number;
  className?: string;
}

const starStyle = (on: boolean) => ({
  fill: on ? 'var(--gold-500)' : 'var(--gray-200)',
  stroke: 'none',
});

/** Sao hiển thị có nửa sao (prototype Rating / .lz-rating) — G06.21. */
export function Rating({
  value,
  count,
  sold,
  showNumber = true,
  size = 14,
  className,
}: RatingProps) {
  const { t, locale } = useT();
  const full = Math.floor(value);
  const half = value - full >= 0.5;
  return (
    <span className={cn('lz-rating inline-flex items-center gap-[3px]', className)}>
      <span className="sr-only">{t('ui.rating.value', { value: value.toFixed(1) })}</span>
      <span className="inline-flex" aria-hidden="true">
        {[0, 1, 2, 3, 4].map((i) => {
          const isHalf = i === full && half;
          return (
            <span key={i} className="relative inline-flex">
              <Icon name="star" size={size} strokeWidth={1.5} style={starStyle(i < full)} />
              {isHalf && (
                <Icon
                  name="star-half"
                  size={size}
                  strokeWidth={1.5}
                  className="absolute inset-0"
                  style={starStyle(true)}
                />
              )}
            </span>
          );
        })}
      </span>
      {showNumber && (
        <span aria-hidden="true" className="ml-[3px] font-semibold text-sm leading-none text-body">
          {value.toFixed(1)}
        </span>
      )}
      {count != null && (
        <span className="text-xs text-muted">({formatNumber(count, { locale })})</span>
      )}
      {sold != null && (
        <span className="text-xs text-muted">· {t('common.label.sold', { count: sold })}</span>
      )}
    </span>
  );
}

export interface RatingInputProps {
  value: number;
  onChange: (value: number) => void;
  size?: number;
  name?: string;
  disabled?: boolean;
  'aria-label'?: string;
}

/** Chọn số sao cho form đánh giá: radiogroup, phím mũi tên đổi sao (G06.21, G10.42). */
export function RatingInput({
  value,
  onChange,
  size = 32,
  disabled,
  'aria-label': ariaLabel,
}: RatingInputProps) {
  const { t } = useT();
  const [hover, setHover] = useState(0);
  const shown = hover || value;
  const refs = useRef<(HTMLButtonElement | null)[]>([]);
  // Phím mũi tên: đổi sao VÀ chuyển focus sang sao mới (mẫu radiogroup WAI-ARIA, G13.14)
  const move = (n: number) => {
    onChange(n);
    refs.current[n - 1]?.focus();
  };
  return (
    <div
      role="radiogroup"
      aria-label={ariaLabel ?? t('ui.rating.choose')}
      className="inline-flex gap-1"
      onMouseLeave={() => setHover(0)}
    >
      {[1, 2, 3, 4, 5].map((n) => (
        <button
          key={n}
          ref={(el) => {
            refs.current[n - 1] = el;
          }}
          type="button"
          role="radio"
          aria-checked={value === n}
          aria-label={t('ui.rating.stars', { n })}
          tabIndex={value === n || (value === 0 && n === 1) ? 0 : -1}
          disabled={disabled}
          onMouseEnter={() => setHover(n)}
          onClick={() => onChange(n)}
          onKeyDown={(e) => {
            if (e.key === 'ArrowRight' || e.key === 'ArrowUp') {
              e.preventDefault();
              move(Math.min(5, (value || 0) + 1));
            }
            if (e.key === 'ArrowLeft' || e.key === 'ArrowDown') {
              e.preventDefault();
              move(Math.max(1, (value || 2) - 1));
            }
          }}
          className="p-0.5 rounded-sm bg-transparent border-0 cursor-pointer focus-visible:outline-none focus-visible:shadow-focus disabled:cursor-not-allowed"
        >
          <Icon name="star" size={size} strokeWidth={1.5} style={starStyle(n <= shown)} />
        </button>
      ))}
    </div>
  );
}
