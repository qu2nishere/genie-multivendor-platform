// Phần dùng chung của form đăng bán (prototype Products.jsx card / cardHead / lab / hint).
import type { ReactNode } from 'react';
import type { UseFormReturn } from 'react-hook-form';
import { cn } from '@/shared/lib/cn';
import { Icon, type IconName } from '@/shared/ui';
import type { PublishIssue } from '../../domain/product.rules';
import type { ListingRules, ProductForm } from '../../domain/product.types';

export interface SectionProps {
  form: UseFormReturn<ProductForm>;
  readOnly: boolean;
  rules: ListingRules | null;
  /** Lỗi gửi duyệt đang hiện (sau lần bấm "Đăng bán" đầu tiên). */
  issues: PublishIssue[];
}

export function SectionCard({
  id,
  icon,
  title,
  right,
  error,
  children,
}: {
  id: string;
  icon: IconName;
  title: ReactNode;
  right?: ReactNode;
  error?: boolean;
  children: ReactNode;
}) {
  return (
    <section
      id={id}
      aria-labelledby={`${id}-title`}
      className={cn(
        'bg-surface-card rounded-lg shadow-sm p-[22px] mb-4 scroll-mt-24 max-sm:p-4',
        error && 'outline outline-2 outline-red-500',
      )}
    >
      <div className="flex items-center gap-2 mb-4 flex-wrap">
        <Icon name={icon} size={19} className={error ? 'text-red-600' : 'text-primary'} />
        <h2
          id={`${id}-title`}
          className={cn('m-0 font-bold text-base', error ? 'text-red-600' : 'text-strong')}
        >
          {title}
        </h2>
        {right}
      </div>
      {children}
    </section>
  );
}

export const labCls = 'block font-semibold text-sm text-strong mb-1.5';
export const hintCls = 'text-xs text-subtle mt-1.5';
export const errCls = 'flex items-center gap-1.5 text-xs text-red-600 mt-2';

/** Ô số nguyên không âm (giá, tồn, cân nặng…): bỏ ký tự không phải số, rỗng = null. */
export const toInt = (s: string): number | null => {
  const d = s.replace(/\D/g, '');
  return d === '' ? null : Number(d);
};
