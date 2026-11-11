import { forwardRef, type ButtonHTMLAttributes, type ReactNode } from 'react';
import { cn } from '@/shared/lib/cn';
import { Icon, type IconName } from './icon/Icon';

export type IconButtonVariant = 'plain' | 'solid' | 'outline';

export interface IconButtonProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children'> {
  icon: IconName;
  /** Bắt buộc: nhãn cho trình đọc màn hình + tooltip (nút chỉ có icon). */
  label: string;
  variant?: IconButtonVariant;
  iconSize?: number;
  /** Số nhỏ góc trên (giỏ, chuông). */
  badge?: ReactNode;
}

const VARIANT: Record<IconButtonVariant, string> = {
  plain: 'bg-transparent text-body hover:bg-gray-100 active:bg-gray-200',
  solid: 'bg-primary text-white hover:bg-primary-hover',
  outline: 'bg-transparent text-body border-line hover:border-line-strong hover:bg-gray-50',
};

/** Nút vuông chỉ có icon, vùng chạm 44×44 (.lz-iconbtn, plan G06.03). */
export const IconButton = forwardRef<HTMLButtonElement, IconButtonProps>(function IconButton(
  { icon, label, variant = 'plain', iconSize = 20, badge, className, type = 'button', ...rest },
  ref,
) {
  return (
    <button
      ref={ref}
      type={type}
      aria-label={label}
      title={label}
      className={cn(
        'relative inline-flex items-center justify-center w-11 h-11 flex-none rounded-md border-[1.5px] border-solid border-transparent cursor-pointer',
        'transition-colors duration-fast ease-out focus-visible:outline-none focus-visible:shadow-focus disabled:opacity-[.45] disabled:cursor-not-allowed',
        VARIANT[variant],
        className,
      )}
      {...rest}
    >
      <Icon name={icon} size={iconSize} />
      {badge != null && badge !== 0 && (
        <span
          aria-hidden="true"
          className="absolute top-1 right-1 min-w-[18px] h-[18px] px-[5px] rounded-pill bg-flash-500 text-white text-[11px] font-bold leading-[18px] text-center"
        >
          {badge}
        </span>
      )}
    </button>
  );
});
