import {
  forwardRef,
  type AnchorHTMLAttributes,
  type ButtonHTMLAttributes,
  type ReactNode,
  type Ref,
} from 'react';
import { cn } from '@/shared/lib/cn';
import { Icon, type IconName } from './icon/Icon';
import { Spinner } from './Spinner';

export type ButtonVariant = 'primary' | 'secondary' | 'outline' | 'ghost' | 'subtle' | 'danger';
export type ButtonSize = 'sm' | 'md' | 'lg';

// .lz-btn của prototype viết lại bằng utility (plan G06.02). Mọi cỡ giữ vùng chạm ≥ 44 px.
const BASE =
  'inline-flex items-center justify-center gap-2 min-h-[44px] border-[1.5px] border-solid rounded-md ' +
  'font-sans font-semibold leading-none tracking-[-0.01em] cursor-pointer select-none whitespace-nowrap no-underline ' +
  'transition-[background-color,transform,box-shadow,border-color,color] duration-fast ease-out ' +
  'hover:no-underline active:translate-y-px focus-visible:outline-none focus-visible:shadow-focus ' +
  'disabled:opacity-[.45] disabled:pointer-events-none aria-disabled:opacity-[.45] aria-disabled:pointer-events-none';

const VARIANT: Record<ButtonVariant, string> = {
  primary:
    'bg-primary text-primary-on border-transparent hover:bg-primary-hover active:bg-primary-active',
  secondary:
    'bg-surface-card text-primary border-primary hover:bg-primary-tint active:bg-brand-100',
  outline:
    'bg-surface-card text-body border-line-strong hover:bg-gray-50 hover:border-primary hover:text-primary active:bg-gray-100',
  ghost: 'bg-transparent text-body border-transparent hover:bg-gray-100 active:bg-gray-200',
  subtle: 'bg-gray-100 text-body border-transparent hover:bg-gray-200',
  danger: 'bg-red-500 text-white border-transparent hover:bg-red-600 active:bg-red-600',
};

const SIZE: Record<ButtonSize, string> = {
  sm: 'px-[14px] text-sm',
  md: 'px-5 text-base',
  lg: 'min-h-[52px] px-7 text-lg',
};

interface CommonProps {
  variant?: ButtonVariant;
  size?: ButtonSize;
  block?: boolean;
  iconLeft?: IconName;
  iconRight?: IconName;
  loading?: boolean;
  children?: ReactNode;
  className?: string;
}

export type ButtonProps = CommonProps &
  (
    | ({ href?: undefined } & ButtonHTMLAttributes<HTMLButtonElement>)
    | ({ href: string } & AnchorHTMLAttributes<HTMLAnchorElement>)
  );

export function buttonClass({
  variant = 'primary',
  size = 'md',
  block = false,
  className,
}: Pick<CommonProps, 'variant' | 'size' | 'block' | 'className'> = {}): string {
  return cn(BASE, VARIANT[variant], SIZE[size], block && 'flex w-full', className);
}

/** Nút hành động; truyền `href` để thành liên kết thật (R16). */
export const Button = forwardRef<HTMLButtonElement | HTMLAnchorElement, ButtonProps>(
  function Button(
    {
      variant,
      size = 'md',
      block,
      iconLeft,
      iconRight,
      loading = false,
      children,
      className,
      ...rest
    },
    ref,
  ) {
    const isz = size === 'lg' ? 20 : 18;
    const content = (
      <>
        {loading ? <Spinner size={isz} /> : iconLeft && <Icon name={iconLeft} size={isz} />}
        {children != null && <span>{children}</span>}
        {iconRight && !loading && <Icon name={iconRight} size={isz} />}
      </>
    );
    const cls = buttonClass({ variant, size, block, className });

    if ('href' in rest && rest.href !== undefined) {
      const { href, ...a } = rest as AnchorHTMLAttributes<HTMLAnchorElement> & { href: string };
      return (
        <a
          ref={ref as Ref<HTMLAnchorElement>}
          href={href}
          className={cls}
          aria-busy={loading || undefined}
          {...a}
        >
          {content}
        </a>
      );
    }
    const { type = 'button', disabled, ...b } = rest as ButtonHTMLAttributes<HTMLButtonElement>;
    return (
      <button
        ref={ref as Ref<HTMLButtonElement>}
        type={type}
        className={cls}
        disabled={disabled || loading}
        aria-busy={loading || undefined}
        {...b}
      >
        {content}
      </button>
    );
  },
);
