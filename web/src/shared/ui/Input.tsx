import { forwardRef, type InputHTMLAttributes, type ReactNode } from 'react';
import { cn } from '@/shared/lib/cn';
import { Field, inputClass } from './Field';
import { Icon, type IconName } from './icon/Icon';

export interface InputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'prefix'> {
  label?: ReactNode;
  hint?: ReactNode;
  error?: ReactNode;
  iconLeft?: IconName;
  iconRight?: IconName;
  /** Chữ / nút ở đầu, cuối ô (vd. "₫", "kg", nút xóa). */
  prefix?: ReactNode;
  suffix?: ReactNode;
  invalid?: boolean;
  wrapperClassName?: string;
}

/** Ô nhập chữ có nhãn, gợi ý, lỗi, icon / tiền tố / hậu tố; forwardRef cho react-hook-form (G06.04). */
export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  {
    label,
    hint,
    error,
    required,
    iconLeft,
    iconRight,
    prefix,
    suffix,
    invalid,
    id,
    className,
    wrapperClassName,
    ...rest
  },
  ref,
) {
  return (
    <Field
      label={label}
      hint={hint}
      error={error}
      required={required}
      id={id}
      className={wrapperClassName}
    >
      {({ id: fid, describedBy, invalid: hasError }) => (
        <div className="relative flex items-center">
          {iconLeft && (
            <span className="absolute left-3 flex text-subtle pointer-events-none">
              <Icon name={iconLeft} size={18} />
            </span>
          )}
          {prefix && (
            <span className="absolute left-3 flex items-center text-muted text-sm">{prefix}</span>
          )}
          <input
            ref={ref}
            id={fid}
            required={required}
            aria-invalid={invalid || hasError || undefined}
            aria-describedby={describedBy}
            className={inputClass(
              invalid || hasError,
              cn(
                (iconLeft || prefix) && 'pl-[38px]',
                (iconRight || suffix) && 'pr-[38px]',
                className,
              ),
            )}
            {...rest}
          />
          {iconRight && (
            <span className="absolute right-3 flex text-subtle pointer-events-none">
              <Icon name={iconRight} size={18} />
            </span>
          )}
          {suffix && (
            <span className="absolute right-2 flex items-center text-muted text-sm">{suffix}</span>
          )}
        </div>
      )}
    </Field>
  );
});
