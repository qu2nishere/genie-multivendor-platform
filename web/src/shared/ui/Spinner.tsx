import { cn } from '@/shared/lib/cn';

export interface SpinnerProps {
  size?: number;
  className?: string;
  /** Nhãn cho trình đọc màn hình; bỏ trống khi nằm trong nút đã có aria-busy. */
  label?: string;
}

/** Vòng xoay đang tải (prototype .lz-spin) — plan G06.15. */
export function Spinner({ size = 18, className, label }: SpinnerProps) {
  return (
    <svg
      className={cn('lz-spin animate-spin', className)}
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      role={label ? 'status' : undefined}
      aria-label={label}
      aria-hidden={label ? undefined : true}
    >
      <circle cx="12" cy="12" r="9" stroke="currentColor" strokeOpacity=".25" strokeWidth="3" />
      <path d="M21 12a9 9 0 0 0-9-9" stroke="currentColor" strokeWidth="3" strokeLinecap="round" />
    </svg>
  );
}
