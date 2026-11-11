import type { ReactNode } from 'react';
import { cn } from '@/shared/lib/cn';
import { Icon, type IconName } from './icon/Icon';

export interface EmptyStateProps {
  icon?: IconName;
  title?: ReactNode;
  /** Dòng mô tả (prototype Empty: icon 40 + chữ thân xám). */
  text?: ReactNode;
  action?: ReactNode;
  className?: string;
  compact?: boolean;
}

/** Trạng thái rỗng (prototype Account Empty) — plan G06.15. */
export function EmptyState({
  icon = 'package',
  title,
  text,
  action,
  className,
  compact,
}: EmptyStateProps) {
  return (
    <div
      className={cn(
        'grid place-items-center gap-2.5 text-center text-subtle',
        compact ? 'py-6' : 'py-12',
        className,
      )}
    >
      <Icon name={icon} size={compact ? 32 : 40} />
      {title && <p className="m-0 font-semibold text-md text-strong">{title}</p>}
      {text && <p className="m-0 text-base text-muted max-w-[42ch]">{text}</p>}
      {action && <div className="mt-1">{action}</div>}
    </div>
  );
}

export interface SkeletonProps {
  className?: string;
  /** Vẽ nhiều dòng chữ giả. */
  lines?: number;
  rounded?: 'sm' | 'md' | 'lg' | 'pill';
}

const RADIUS = {
  sm: 'rounded-sm',
  md: 'rounded-md',
  lg: 'rounded-lg',
  pill: 'rounded-pill',
} as const;

/** Khối xám nhấp nháy giữ chỗ khi đang tải — plan G06.15. */
export function Skeleton({ className, lines, rounded = 'sm' }: SkeletonProps) {
  const base = cn('block bg-gray-100 animate-pulse', RADIUS[rounded]);
  if (lines && lines > 1) {
    return (
      <span className={cn('flex flex-col gap-2', className)} aria-hidden="true">
        {Array.from({ length: lines }, (_, i) => (
          <span key={i} className={cn(base, 'h-3', i === lines - 1 && 'w-2/3')} />
        ))}
      </span>
    );
  }
  return <span aria-hidden="true" className={cn(base, 'h-4', className)} />;
}
