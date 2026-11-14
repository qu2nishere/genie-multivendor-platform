import { useEffect, useState } from 'react';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { Icon } from './icon/Icon';

export interface QuantityStepperProps {
  value: number;
  onChange: (value: number) => void;
  min?: number;
  /** Tồn kho khả dụng / giới hạn mua. */
  max?: number;
  disabled?: boolean;
  size?: 'sm' | 'md';
  'aria-label'?: string;
  className?: string;
}

export const clampQty = (v: number, min: number, max: number) =>
  Math.max(min, Math.min(max, Math.floor(v)));

/** Bộ chọn số lượng −/+ có nhập tay, kẹp min / max theo tồn kho (.lz-qty) — G06.22. */
export function QuantityStepper({
  value,
  onChange,
  min = 1,
  max = 99,
  disabled,
  size = 'md',
  'aria-label': ariaLabel,
  className,
}: QuantityStepperProps) {
  const { t } = useT();
  // Cho phép xóa trắng tạm thời khi gõ; kẹp lại khi rời ô
  const [draft, setDraft] = useState(String(value));
  useEffect(() => setDraft(String(value)), [value]);
  const set = (v: number) => {
    const next = clampQty(v, min, max);
    setDraft(String(next));
    if (next !== value) onChange(next);
  };
  const h = size === 'sm' ? 'h-9' : 'h-11';
  const btn =
    'w-10 flex items-center justify-center bg-surface-card text-body border-0 cursor-pointer hover:bg-gray-100 ' +
    'disabled:text-disabled disabled:cursor-not-allowed disabled:hover:bg-surface-card focus-visible:outline-none focus-visible:bg-gray-100';
  return (
    <div
      className={cn(
        'lz-qty inline-flex items-stretch border-[1.5px] border-solid border-line rounded-md overflow-hidden',
        h,
        className,
      )}
    >
      <button
        type="button"
        className={btn}
        aria-label={t('ui.qty.decrease')}
        disabled={disabled || value <= min}
        onClick={() => set(value - 1)}
      >
        <Icon name="minus" size={16} />
      </button>
      <input
        type="text"
        inputMode="numeric"
        value={draft}
        disabled={disabled}
        aria-label={ariaLabel ?? t('ui.qty.label')}
        aria-valuemin={min}
        aria-valuemax={max}
        onChange={(e) => setDraft(e.target.value.replace(/\D/g, ''))}
        onBlur={() => set(parseInt(draft, 10) || min)}
        onKeyDown={(e) => {
          if (e.key === 'Enter') set(parseInt(draft, 10) || min);
          if (e.key === 'ArrowUp') {
            e.preventDefault();
            set(value + 1);
          }
          if (e.key === 'ArrowDown') {
            e.preventDefault();
            set(value - 1);
          }
        }}
        className="w-12 text-center border-0 border-x-[1.5px] border-solid border-line font-semibold text-base tabular-nums text-strong focus:outline-none disabled:bg-gray-50"
      />
      <button
        type="button"
        className={btn}
        aria-label={t('ui.qty.increase')}
        disabled={disabled || value >= max}
        onClick={() => set(value + 1)}
      >
        <Icon name="plus" size={16} />
      </button>
    </div>
  );
}
