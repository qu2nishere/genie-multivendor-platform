import { forwardRef, type InputHTMLAttributes, type ReactNode } from 'react';
import { cn } from '@/shared/lib/cn';
import { Field, inputClass } from './Field';
import { Icon, type IconName } from './icon/Icon';

// FormSection, DateInput (plan G06.28) — thay Section, lab, dateInput lặp ở form quản trị / người bán.
// Nhãn + ô nhập dùng Field (đã có ở G06.04).

export interface FormSectionProps {
  title: ReactNode;
  icon?: IconName;
  description?: ReactNode;
  action?: ReactNode;
  children: ReactNode;
  className?: string;
  id?: string;
}

/** Khối form nền trắng, tiêu đề có icon xanh (prototype VendorPages Section). */
export function FormSection({
  title,
  icon,
  description,
  action,
  children,
  className,
  id,
}: FormSectionProps) {
  return (
    <section
      aria-labelledby={id}
      className={cn('bg-surface-card rounded-lg shadow-sm p-5', className)}
    >
      <div className="flex items-center gap-2 mb-4">
        {icon && <Icon name={icon} size={18} className="text-brand-500" />}
        <h2 id={id} className="m-0 flex-1 font-bold text-base text-strong">
          {title}
        </h2>
        {action}
      </div>
      {description && <p className="-mt-2 mb-4 text-sm text-muted">{description}</p>}
      {children}
    </section>
  );
}

export interface DateInputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type'> {
  label?: ReactNode;
  hint?: ReactNode;
  error?: ReactNode;
  /** date | datetime-local | time (chọn giờ Flash Sale). */
  kind?: 'date' | 'datetime-local' | 'time';
  wrapperClassName?: string;
}

/** Ô chọn ngày / giờ native, cùng khung với Input (prototype dateInput: cao 44, viền 1.5). */
export const DateInput = forwardRef<HTMLInputElement, DateInputProps>(function DateInput(
  { label, hint, error, kind = 'date', id, className, wrapperClassName, required, ...rest },
  ref,
) {
  return (
    <Field
      label={label}
      hint={hint}
      error={error}
      id={id}
      required={required}
      className={wrapperClassName}
    >
      {({ id: fid, describedBy, invalid }) => (
        <input
          ref={ref}
          id={fid}
          type={kind}
          required={required}
          aria-invalid={invalid || undefined}
          aria-describedby={describedBy}
          className={inputClass(invalid, cn('px-3', className))}
          {...rest}
        />
      )}
    </Field>
  );
});

/** Lưới 2 cột cho form dài, thu 1 cột ở màn hẹp. */
export function FormGrid({ children, className }: { children: ReactNode; className?: string }) {
  return <div className={cn('grid grid-cols-1 md:grid-cols-2 gap-4', className)}>{children}</div>;
}
