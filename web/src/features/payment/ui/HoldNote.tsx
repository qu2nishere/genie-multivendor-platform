import type { ReactNode } from 'react';
import { cn } from '@/shared/lib/cn';
import { Icon } from '@/shared/ui';

/** Lời hứa giữ tiền đặt cạnh quyết định mua (prototype HoldNote, DESIGN.md "Hold Note") — G10.23. */
export function HoldNote({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <div className={cn('tr-hold', className)}>
      <Icon name="shield-check" size={18} />
      <span>{children}</span>
    </div>
  );
}
