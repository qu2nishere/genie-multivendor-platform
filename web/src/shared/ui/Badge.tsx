import type { HTMLAttributes, ReactNode } from 'react';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { Icon, type IconName } from './icon/Icon';

// Badge, StatusBadge, Tag (plan G06.10) — .lz-badge / .lz-status / .lz-tag của prototype.

export type BadgeVariant =
  | 'neutral'
  | 'primary'
  | 'solid'
  | 'flash'
  | 'success'
  | 'warning'
  | 'danger'
  | 'mint'
  | 'gold'
  | 'outline';

const BADGE: Record<BadgeVariant, string> = {
  neutral: 'bg-gray-100 text-body',
  primary: 'bg-primary-tint text-brand-700',
  solid: 'bg-primary text-white',
  flash: 'bg-flash-500 text-white',
  success: 'bg-green-50 text-green-600',
  warning: 'bg-amber-50 text-amber-600',
  danger: 'bg-red-50 text-red-600',
  mint: 'bg-mint-50 text-mint-600',
  gold: 'bg-gold-50 text-gold-600',
  outline: 'bg-transparent text-muted shadow-[inset_0_0_0_1px_var(--border-default)]',
};

export interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  variant?: BadgeVariant;
  icon?: IconName;
  children?: ReactNode;
}

export function Badge({ variant = 'neutral', icon, children, className, ...rest }: BadgeProps) {
  return (
    <span
      className={cn(
        'lz-badge inline-flex items-center gap-1 px-2 py-[3px] rounded-sm whitespace-nowrap',
        'font-semibold text-xs leading-[1.1] tracking-[.01em]',
        BADGE[variant],
        className,
      )}
      {...rest}
    >
      {icon && <Icon name={icon} size={12} strokeWidth={2.4} />}
      {children}
    </span>
  );
}

/** Trạng thái vòng đời đơn / KYC / ví ký quỹ → cặp màu token (prototype StatusBadge MAP). */
export const STATUS_TONES = {
  PENDING: ['var(--status-pending-fg)', 'var(--status-pending-bg)'],
  PAID: ['var(--status-paid-fg)', 'var(--status-paid-bg)'],
  SHIPPED: ['var(--status-shipped-fg)', 'var(--status-shipped-bg)'],
  DELIVERED: ['var(--status-delivered-fg)', 'var(--status-delivered-bg)'],
  COMPLETED: ['var(--status-completed-fg)', 'var(--status-completed-bg)'],
  CANCELLED: ['var(--status-cancelled-fg)', 'var(--status-cancelled-bg)'],
  ERROR: ['var(--status-error-fg)', 'var(--status-error-bg)'],
  VERIFIED: ['var(--kyc-verified-fg)', 'var(--kyc-verified-bg)'],
  REJECTED: ['var(--kyc-rejected-fg)', 'var(--kyc-rejected-bg)'],
  HELD: ['var(--escrow-held-fg)', 'var(--escrow-held-bg)'],
  AVAILABLE: ['var(--escrow-available-fg)', 'var(--escrow-available-bg)'],
} as const;

export type StatusTone = keyof typeof STATUS_TONES;

export interface StatusBadgeProps extends HTMLAttributes<HTMLSpanElement> {
  /** Tông màu (mã trạng thái chuẩn). Mã lạ → PENDING. */
  status?: StatusTone | string;
  /** Nhãn riêng; bỏ trống thì tra ui.status.<mã> qua t() (thay STATUS_WORDS). */
  label?: ReactNode;
  dot?: boolean;
}

export function StatusBadge({
  status = 'PENDING',
  label,
  dot = true,
  className,
  style,
  ...rest
}: StatusBadgeProps) {
  const { t } = useT();
  const key = String(status).toUpperCase();
  const tone = (key in STATUS_TONES ? key : 'PENDING') as StatusTone;
  const [fg, bg] = STATUS_TONES[tone];
  return (
    <span
      className={cn(
        'lz-status inline-flex items-center gap-1.5 px-[11px] py-[5px] rounded-pill whitespace-nowrap',
        'font-semibold text-xs leading-none tracking-[.02em]',
        className,
      )}
      style={{ color: fg, background: bg, ...style }}
      {...rest}
    >
      {dot && (
        <span aria-hidden="true" className="w-[7px] h-[7px] rounded-full bg-current flex-none" />
      )}
      {label ?? (key in STATUS_TONES ? t(`ui.status.${key}`) : key)}
    </span>
  );
}

export interface TagProps extends HTMLAttributes<HTMLSpanElement> {
  brand?: boolean;
  onRemove?: () => void;
  /** Nhãn cho nút gỡ, mặc định "Gỡ bỏ <nội dung>". */
  removeLabel?: string;
  children?: ReactNode;
}

/** Chip lọc có thể gỡ (bộ lọc đang áp dụng). Nút gỡ là <button> thật để dùng bàn phím. */
export function Tag({ brand, onRemove, removeLabel, children, className, ...rest }: TagProps) {
  const { t } = useT();
  return (
    <span
      className={cn(
        'lz-tag inline-flex items-center gap-1.5 h-[30px] pl-[11px] rounded-pill font-medium text-sm leading-none',
        onRemove ? 'pr-1.5' : 'pr-[11px]',
        brand ? 'bg-primary-tint text-brand-700' : 'bg-gray-100 text-body',
        className,
      )}
      {...rest}
    >
      {children}
      {onRemove && (
        <button
          type="button"
          onClick={onRemove}
          aria-label={
            removeLabel ??
            `${t('ui.remove')} ${typeof children === 'string' ? children : ''}`.trim()
          }
          className="inline-flex items-center justify-center w-[18px] h-[18px] rounded-full text-inherit opacity-60 hover:opacity-100 hover:bg-black/[.08] focus-visible:outline-none focus-visible:shadow-focus"
        >
          <Icon name="x" size={13} strokeWidth={2.5} />
        </button>
      )}
    </span>
  );
}
