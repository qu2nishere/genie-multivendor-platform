import { useEffect, useRef, type ClipboardEvent, type KeyboardEvent } from 'react';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';

export interface OtpInputProps {
  value: string;
  onChange: (value: string) => void;
  /** Đủ ký tự → gọi (vd. tự xác minh). */
  onComplete?: (value: string) => void;
  length?: number;
  /** Khóa sau 5 lần sai / đang xác minh. */
  disabled?: boolean;
  invalid?: boolean;
  autoFocus?: boolean;
  size?: 'md' | 'lg';
  'aria-describedby'?: string;
}

/**
 * Ô OTP nhiều ô (G06.16) — gộp OTPInput (auth), OtpBoxes (Account), VOtpBoxes, AOtpBoxes:
 * chỉ nhận số, tự chuyển ô, Backspace lùi ô, ← → di chuyển, dán cả chuỗi điền một lần.
 */
export function OtpInput({
  value,
  onChange,
  onComplete,
  length = 6,
  disabled,
  invalid,
  autoFocus,
  size = 'lg',
  'aria-describedby': describedBy,
}: OtpInputProps) {
  const { t } = useT();
  const refs = useRef<(HTMLInputElement | null)[]>([]);
  const digits = value.replace(/\D/g, '').slice(0, length).split('');

  useEffect(() => {
    if (!autoFocus) return;
    const boxes = refs.current.filter((el): el is HTMLInputElement => Boolean(el));
    (boxes.find((el) => !el.value) ?? boxes[boxes.length - 1])?.focus();
  }, [autoFocus]);

  const commit = (next: string) => {
    const v = next.replace(/\D/g, '').slice(0, length);
    onChange(v);
    if (v.length === length) onComplete?.(v);
  };

  const setAt = (i: number, ch: string) => {
    const arr = Array.from({ length }, (_, k) => digits[k] ?? '');
    arr[i] = ch;
    // Không để lỗ giữa chuỗi: dồn các ký tự đã có
    commit(arr.join(''));
  };

  const onInput = (i: number, raw: string) => {
    const d = raw.replace(/\D/g, '');
    if (!d) return;
    if (d.length > 1) {
      // Bàn phím gợi ý OTP của điện thoại điền cả chuỗi vào một ô
      commit((digits.slice(0, i).join('') + d).slice(0, length));
      refs.current[Math.min(i + d.length, length - 1)]?.focus();
      return;
    }
    setAt(i, d);
    if (i < length - 1) refs.current[i + 1]?.focus();
  };

  const onKey = (i: number, e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Backspace') {
      e.preventDefault();
      if (digits[i]) setAt(i, '');
      else if (i > 0) {
        setAt(i - 1, '');
        refs.current[i - 1]?.focus();
      }
    } else if (e.key === 'ArrowLeft' && i > 0) {
      e.preventDefault();
      refs.current[i - 1]?.focus();
    } else if (e.key === 'ArrowRight' && i < length - 1) {
      e.preventDefault();
      refs.current[i + 1]?.focus();
    }
  };

  const onPaste = (e: ClipboardEvent<HTMLInputElement>) => {
    const d = e.clipboardData.getData('text').replace(/\D/g, '');
    if (!d) return;
    e.preventDefault();
    commit(d);
    refs.current[Math.min(d.length, length - 1)]?.focus();
  };

  return (
    <div className="flex gap-2 md:gap-2.5" role="group" aria-label={t('ui.otp.group', { length })}>
      {Array.from({ length }, (_, i) => (
        <input
          key={i}
          ref={(el) => {
            refs.current[i] = el;
          }}
          value={digits[i] ?? ''}
          onChange={(e) => onInput(i, e.target.value)}
          onKeyDown={(e) => onKey(i, e)}
          onPaste={onPaste}
          onFocus={(e) => e.target.select()}
          disabled={disabled}
          inputMode="numeric"
          autoComplete={i === 0 ? 'one-time-code' : 'off'}
          maxLength={length}
          aria-label={t('ui.otp.digit', { n: i + 1 })}
          aria-invalid={invalid || undefined}
          aria-describedby={describedBy}
          className={cn(
            'text-center font-mono font-bold text-strong rounded-md border-[1.5px] border-solid outline-none',
            'transition-[border-color,box-shadow] duration-fast focus:border-primary focus:shadow-focus',
            size === 'lg'
              ? 'w-10 h-12 md:w-[52px] md:h-[60px] text-2xl'
              : 'w-10 h-12 md:w-11 md:h-[52px] text-[22px]',
            invalid ? 'border-red-500' : digits[i] ? 'border-primary' : 'border-line',
            disabled ? 'bg-gray-50 cursor-not-allowed' : 'bg-surface-card',
          )}
        />
      ))}
    </div>
  );
}
