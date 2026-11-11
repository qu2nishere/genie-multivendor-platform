import type { ElementType, HTMLAttributes, ReactNode } from 'react';
import { cn } from '@/shared/lib/cn';
import { Icon, type IconName } from './icon/Icon';

// Card, PanelHead, SectionHead (plan G06.19) — khung thẻ và tiêu đề khối dùng lặp ở nhiều màn.

export interface CardProps extends HTMLAttributes<HTMLElement> {
  as?: ElementType;
  /** Đệm trong; mặc định 22px như thẻ của Account prototype. */
  padding?: 'none' | 'sm' | 'md' | 'lg';
}

const PAD = { none: '', sm: 'p-3.5', md: 'p-[22px]', lg: 'p-7' } as const;

/** Thẻ nền trắng (.lz-card): viền mảnh, bo 16px, đổ bóng nhẹ. */
export function Card({ as: Tag = 'div', padding = 'md', className, children, ...rest }: CardProps) {
  return (
    <Tag
      className={cn(
        'lz-card bg-surface-card border border-line-subtle rounded-lg shadow-sm',
        PAD[padding],
        className,
      )}
      {...rest}
    >
      {children}
    </Tag>
  );
}

export interface PanelHeadProps {
  icon?: IconName;
  title: ReactNode;
  sub?: ReactNode;
  /** Cấp tiêu đề — giữ đúng thứ tự trên trang (R18). */
  level?: 2 | 3;
  action?: ReactNode;
  id?: string;
}

/** Tiêu đề panel trong trang tài khoản (prototype PanelHead): icon xanh + h4, dòng phụ gạch dưới. */
export function PanelHead({ icon, title, sub, level = 2, action, id }: PanelHeadProps) {
  const H = `h${level}` as 'h2' | 'h3';
  return (
    <div className={cn(sub && 'pb-3 mb-1 border-b border-line-subtle')}>
      <div className="flex items-center gap-2">
        {icon && <Icon name={icon} size={20} className="text-brand-500" />}
        <H id={id} className="m-0 flex-1 font-semibold text-xl leading-snug text-strong">
          {title}
        </H>
        {action}
      </div>
      {sub && <p className={cn('mt-1 mb-0 text-sm text-muted', icon && 'ml-7')}>{sub}</p>}
    </div>
  );
}

export interface SectionHeadProps {
  title: ReactNode;
  sub?: ReactNode;
  /** Liên kết "Xem tất cả ›" bên phải. */
  moreHref?: string;
  moreLabel?: ReactNode;
  level?: 1 | 2 | 3;
  /** Vạch màu bên trái (đầu trang Gợi ý hôm nay / danh mục). */
  accent?: boolean;
  aside?: ReactNode;
  id?: string;
  className?: string;
}

/** Tiêu đề khối trên trang storefront: tiêu đề + mô tả + "Xem thêm ›". */
export function SectionHead({
  title,
  sub,
  moreHref,
  moreLabel,
  level = 2,
  accent,
  aside,
  id,
  className,
}: SectionHeadProps) {
  const H = `h${level}` as 'h1' | 'h2' | 'h3';
  return (
    <div
      className={cn(
        'flex items-end justify-between gap-3 flex-wrap',
        accent && 'border-l-4 border-primary pl-3.5',
        className,
      )}
    >
      <div className="min-w-0">
        <H
          id={id}
          className={cn(
            'm-0 text-strong',
            level === 1 ? 'font-bold text-3xl leading-snug' : 'font-bold text-xl leading-snug',
          )}
        >
          {title}
        </H>
        {sub && <p className="mt-1 mb-0 text-sm text-muted">{sub}</p>}
      </div>
      {aside}
      {moreHref && (
        <a
          href={moreHref}
          className="inline-flex items-center gap-1 text-sm font-semibold text-link"
        >
          {moreLabel}
          <Icon name="chevron-right" size={14} />
        </a>
      )}
    </div>
  );
}
