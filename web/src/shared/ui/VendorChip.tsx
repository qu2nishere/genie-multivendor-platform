import type { ReactNode } from 'react';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { Icon } from './icon/Icon';

export interface VendorChipProps {
  name: string;
  logo?: string | null;
  initial?: string;
  /** Shop Chính hãng / đã xác minh → dấu tích xanh. */
  authentic?: boolean;
  online?: boolean;
  meta?: ReactNode;
  size?: 'md' | 'lg';
  href?: string;
  className?: string;
}

/** Danh tính shop: logo / chữ đầu, tên + dấu Chính hãng, dòng phụ (prototype VendorChip, .lz-vendor) — G06.24. */
export function VendorChip({
  name,
  logo,
  initial,
  authentic,
  online,
  meta,
  size = 'md',
  href,
  className,
}: VendorChipProps) {
  const { t } = useT();
  const box = size === 'lg' ? 'w-10 h-10' : 'w-8 h-8';
  const NameTag = href ? 'a' : 'span';
  return (
    <span className={cn('lz-vendor inline-flex items-center gap-2', className)}>
      <span
        className={cn(
          box,
          'lz-vendor__logo rounded-sm bg-gray-100 grid place-items-center text-muted font-bold flex-none overflow-hidden',
        )}
      >
        {logo ? (
          <img src={logo} alt="" width={40} height={40} className="w-full h-full object-cover" />
        ) : (
          (initial ?? (name || '?')[0])
        )}
      </span>
      <span className="flex flex-col gap-px min-w-0">
        <NameTag
          {...(href ? { href } : {})}
          className="flex items-center gap-1 font-semibold text-sm leading-[1.2] text-strong no-underline hover:underline"
        >
          <span className="truncate">{name}</span>
          {authentic && (
            <Icon
              name="badge-check"
              size={14}
              className="text-primary"
              title={t('common.authentic')}
            />
          )}
        </NameTag>
        {(meta || online) && (
          <span className="text-xs text-muted">
            {online && <span className="text-mint-600">● Online · </span>}
            {meta}
          </span>
        )}
      </span>
    </span>
  );
}
