import { forwardRef, useState, type InputHTMLAttributes, type ReactNode } from 'react';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { passwordStrength } from '@/shared/lib/password';
import { Field, inputClass } from './Field';
import { Icon } from './icon/Icon';

export interface PasswordInputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type'> {
  label?: ReactNode;
  hint?: ReactNode;
  error?: ReactNode;
  /** Hiện thanh độ mạnh dưới ô (đăng ký, đặt lại / đổi mật khẩu). */
  showStrength?: boolean;
  /** So khớp với ô "Nhập lại mật khẩu". */
  matchWith?: string;
  wrapperClassName?: string;
}

const STRENGTH_COLOR = [
  'var(--red-500)',
  'var(--red-500)',
  'var(--gold-500)',
  'var(--mint-500)',
  'var(--mint-600)',
];

/**
 * Ô mật khẩu có nút hiện / ẩn + thanh độ mạnh (G06.06) — gộp PasswordInput (auth), PwField (Account),
 * VPwField (vendor), APwField (admin).
 */
export const PasswordInput = forwardRef<HTMLInputElement, PasswordInputProps>(
  function PasswordInput(
    {
      label,
      hint,
      error,
      showStrength,
      matchWith,
      id,
      className,
      wrapperClassName,
      value,
      ...rest
    },
    ref,
  ) {
    const { t } = useT();
    const [show, setShow] = useState(false);
    const pw = typeof value === 'string' ? value : '';
    const s = passwordStrength(pw);
    return (
      <Field
        label={label}
        hint={hint}
        error={error}
        id={id}
        required={rest.required}
        className={wrapperClassName}
      >
        {({ id: fid, describedBy, invalid }) => (
          <>
            <div className="relative">
              <input
                ref={ref}
                id={fid}
                type={show ? 'text' : 'password'}
                value={value}
                autoComplete={rest.autoComplete ?? 'current-password'}
                aria-invalid={invalid || undefined}
                aria-describedby={describedBy}
                className={inputClass(invalid, cn('pr-[44px]', className))}
                {...rest}
              />
              <button
                type="button"
                aria-label={show ? t('ui.password.hide') : t('ui.password.show')}
                aria-pressed={show}
                onClick={() => setShow((v) => !v)}
                className="absolute right-1 top-1/2 -translate-y-1/2 w-9 h-9 grid place-items-center rounded-sm text-subtle hover:text-body focus-visible:outline-none focus-visible:shadow-focus"
              >
                <Icon name={show ? 'eye-off' : 'eye'} size={18} />
              </button>
            </div>
            {showStrength && pw && (
              <div className="flex items-center gap-2 mt-0.5" aria-live="polite">
                <div className="flex-1 h-[5px] rounded-[3px] bg-gray-200 overflow-hidden">
                  <div
                    className="h-full transition-[width] duration-base"
                    style={{ width: `${(s / 4) * 100}%`, background: STRENGTH_COLOR[s] }}
                  />
                </div>
                <span className="text-xs font-semibold" style={{ color: STRENGTH_COLOR[s] }}>
                  {t(`ui.password.strength.${s}`)}
                </span>
              </div>
            )}
            {matchWith !== undefined && pw && !error && (
              <span className={cn('text-xs', pw === matchWith ? 'text-mint-600' : 'text-red-600')}>
                {pw === matchWith ? t('ui.password.match') : t('ui.password.mismatch')}
              </span>
            )}
          </>
        )}
      </Field>
    );
  },
);
