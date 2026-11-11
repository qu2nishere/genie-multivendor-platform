import { forwardRef, useState, type ReactNode, type TextareaHTMLAttributes } from 'react';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { Field, inputClass } from './Field';

export interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: ReactNode;
  hint?: ReactNode;
  error?: ReactNode;
  /** Hiện bộ đếm ký tự khi có maxLength. */
  showCount?: boolean;
  wrapperClassName?: string;
}

/** Ô nhập nhiều dòng (.lz-textarea) có đếm ký tự (G06.05). */
export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(function Textarea(
  {
    label,
    hint,
    error,
    required,
    showCount = true,
    maxLength,
    id,
    className,
    wrapperClassName,
    onChange,
    value,
    defaultValue,
    ...rest
  },
  ref,
) {
  const { t } = useT();
  const [len, setLen] = useState(() => String(value ?? defaultValue ?? '').length);
  const count = value !== undefined ? String(value).length : len;
  return (
    <Field
      label={label}
      hint={hint}
      error={error}
      required={required}
      id={id}
      className={wrapperClassName}
    >
      {({ id: fid, describedBy, invalid }) => (
        <div className="relative">
          <textarea
            ref={ref}
            id={fid}
            required={required}
            maxLength={maxLength}
            value={value}
            defaultValue={defaultValue}
            aria-invalid={invalid || undefined}
            aria-describedby={describedBy}
            onChange={(e) => {
              setLen(e.target.value.length);
              onChange?.(e);
            }}
            className={inputClass(
              invalid,
              cn('min-h-[88px] py-[11px] resize-y leading-normal', className),
            )}
            {...rest}
          />
          {showCount && maxLength != null && (
            <span
              className="absolute right-3 bottom-2 text-2xs text-subtle pointer-events-none"
              aria-live="polite"
            >
              {t('ui.char_count', { count, max: maxLength })}
            </span>
          )}
        </div>
      )}
    </Field>
  );
});
