import {
  forwardRef,
  useEffect,
  useImperativeHandle,
  useRef,
  type InputHTMLAttributes,
  type ReactNode,
} from 'react';
import { cn } from '@/shared/lib/cn';
import { Icon } from './icon/Icon';

// Checkbox, Radio, Switch (plan G06.07) — .lz-check / .lz-radio / .lz-switch của prototype.
// Ô input thật nằm trong nhãn (ẩn bằng sr-only, vẫn nhận bàn phím); phần hiển thị là span kế bên.

interface ChoiceBase extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type'> {
  children?: ReactNode;
  className?: string;
}

const WRAP =
  'inline-flex items-center gap-[9px] min-h-[44px] cursor-pointer text-base text-body has-[:disabled]:cursor-not-allowed';
const BOX =
  'w-5 h-5 flex-none grid place-items-center border-[1.5px] border-solid border-line-strong bg-surface-card text-white ' +
  'transition-colors duration-fast ease-out peer-focus-visible:shadow-focus peer-disabled:opacity-[.45] ' +
  'peer-checked:bg-primary peer-checked:border-primary';

export interface CheckboxProps extends ChoiceBase {
  /** Trạng thái "một phần" — ô chọn tất cả khi chỉ chọn vài dòng. */
  indeterminate?: boolean;
}

export const Checkbox = forwardRef<HTMLInputElement, CheckboxProps>(function Checkbox(
  { children, className, indeterminate = false, ...rest },
  ref,
) {
  const inner = useRef<HTMLInputElement>(null);
  useImperativeHandle(ref, () => inner.current as HTMLInputElement);
  useEffect(() => {
    if (inner.current) inner.current.indeterminate = indeterminate;
  }, [indeterminate]);
  const Tag = children ? 'label' : 'span';
  return (
    <Tag className={cn('lz-check', WRAP, className)}>
      <input
        ref={inner}
        type="checkbox"
        aria-checked={indeterminate ? 'mixed' : undefined}
        className="peer sr-only"
        {...rest}
      />
      <span
        aria-hidden="true"
        className={cn(
          BOX,
          'rounded-xs',
          indeterminate && 'bg-primary border-primary',
          '[&>svg]:opacity-0 peer-checked:[&>svg]:opacity-100',
        )}
      >
        {indeterminate ? (
          <span className="block w-2.5 h-0.5 bg-white rounded-full" />
        ) : (
          <Icon
            name="check"
            size={14}
            strokeWidth={3}
            className="transition-opacity duration-fast"
          />
        )}
      </span>
      {children && <span className="peer-disabled:opacity-[.45]">{children}</span>}
    </Tag>
  );
});

export type RadioProps = ChoiceBase;

export const Radio = forwardRef<HTMLInputElement, RadioProps>(function Radio(
  { children, className, ...rest },
  ref,
) {
  const Tag = children ? 'label' : 'span';
  return (
    <Tag className={cn('lz-radio', WRAP, className)}>
      <input ref={ref} type="radio" className="peer sr-only" {...rest} />
      <span
        aria-hidden="true"
        className={cn(BOX, 'rounded-pill [&>i]:opacity-0 peer-checked:[&>i]:opacity-100')}
      >
        <i className="block w-2 h-2 rounded-full bg-white transition-opacity duration-fast" />
      </span>
      {children && <span className="peer-disabled:opacity-[.45]">{children}</span>}
    </Tag>
  );
});

export type SwitchProps = ChoiceBase;

export const Switch = forwardRef<HTMLInputElement, SwitchProps>(function Switch(
  { children, className, ...rest },
  ref,
) {
  const Tag = children ? 'label' : 'span';
  return (
    <Tag className={cn('lz-switch', WRAP, 'gap-2.5', className)}>
      <input ref={ref} type="checkbox" role="switch" className="peer sr-only" {...rest} />
      <span
        aria-hidden="true"
        className={cn(
          'relative w-[42px] h-6 flex-none rounded-pill bg-gray-300 transition-colors duration-base ease-out',
          'peer-checked:bg-primary peer-focus-visible:shadow-focus peer-disabled:opacity-[.45]',
          'after:content-[""] after:absolute after:top-0.5 after:left-0.5 after:w-5 after:h-5 after:rounded-full after:bg-white after:shadow-sm',
          'after:transition-transform after:duration-base after:ease-out peer-checked:after:translate-x-[18px]',
        )}
      />
      {children && <span className="peer-disabled:opacity-[.45]">{children}</span>}
    </Tag>
  );
});
