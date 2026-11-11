import { useId, type ReactNode } from 'react';
import { cn } from '@/shared/lib/cn';
import { Icon } from './icon/Icon';

export interface FieldProps {
  label?: ReactNode;
  hint?: ReactNode;
  error?: ReactNode;
  required?: boolean;
  /** id của ô nhập; bỏ trống thì tự sinh. */
  id?: string;
  className?: string;
  /** Hàm render nhận id + id mô tả để gắn aria-describedby. */
  children: (ids: { id: string; describedBy?: string; invalid: boolean }) => ReactNode;
}

/** Khung nhãn + gợi ý + lỗi (.lz-field) — dùng cho Input, Textarea, Select, form quản trị (G06.04, G06.28). */
export function Field({ label, hint, error, required, id, className, children }: FieldProps) {
  const auto = useId();
  const fid = id ?? `f${auto.replace(/:/g, '')}`;
  const msgId = `${fid}-msg`;
  const hasMsg = Boolean(error || hint);
  return (
    <div className={cn('lz-field flex flex-col gap-1.5', className)}>
      {label && (
        <label htmlFor={fid} className="font-semibold text-sm leading-snug text-strong">
          {label}
          {required && (
            <span className="text-flash-500 ml-0.5" aria-hidden="true">
              *
            </span>
          )}
        </label>
      )}
      {children({ id: fid, describedBy: hasMsg ? msgId : undefined, invalid: Boolean(error) })}
      {error ? (
        <span
          id={msgId}
          role="alert"
          className="flex items-center gap-1 text-xs leading-snug text-red-600"
        >
          <Icon name="circle-alert" size={13} />
          {error}
        </span>
      ) : (
        hint && (
          <span id={msgId} className="text-xs leading-snug text-muted">
            {hint}
          </span>
        )
      )}
    </div>
  );
}

/** Lớp ô nhập dùng chung (.lz-input) — Input, Textarea, Select, DateInput. */
export const inputClass = (invalid?: boolean, extra?: string) =>
  cn(
    'w-full min-h-[44px] px-[14px] rounded-md border-[1.5px] border-solid bg-surface-card text-strong font-sans text-base',
    'transition-[border-color,box-shadow] duration-fast ease-out placeholder:text-subtle',
    'hover:border-line-strong focus:outline-none focus:border-line-focus focus:shadow-focus',
    'disabled:bg-gray-50 disabled:text-disabled disabled:cursor-not-allowed',
    invalid
      ? 'border-red-500 hover:border-red-500 focus:border-red-500 focus:shadow-[0_0_0_3px_rgba(224,20,43,.18)]'
      : 'border-line',
    extra,
  );
