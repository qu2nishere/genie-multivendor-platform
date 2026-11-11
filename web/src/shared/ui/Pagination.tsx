import type { ReactNode } from 'react';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { Icon } from './icon/Icon';

/** Danh sách số trang có dấu …: 1 … (p-1) p (p+1) … last (prototype Discover pageList). */
export function pageList(page: number, total: number): (number | '…')[] {
  const out: (number | '…')[] = [1];
  if (page > 3) out.push('…');
  for (let p = Math.max(2, page - 1); p <= Math.min(total - 1, page + 1); p++) out.push(p);
  if (page < total - 2) out.push('…');
  if (total > 1) out.push(total);
  return out;
}

export interface PaginationProps {
  page: number;
  total: number;
  /** Liên kết thật cho từng trang (?page=N) — bấm được khi tắt JS, bot đọc được (R16, G08.50). */
  hrefFor?: (page: number) => string;
  /** Bắt sự kiện để chuyển trang không tải lại (island). */
  onChange?: (page: number) => void;
  className?: string;
}

const BTN =
  'inline-flex items-center justify-center min-w-[40px] h-10 gap-1 rounded-md border-[1.5px] border-solid ' +
  'font-semibold text-sm no-underline hover:no-underline cursor-pointer focus-visible:outline-none focus-visible:shadow-focus';
const IDLE = 'border-line bg-surface-card text-body hover:border-line-strong';
const ACTIVE = 'bg-primary border-primary text-white';
const OFF = 'opacity-40 cursor-not-allowed pointer-events-none';

/** Phân trang số (prototype Discover: Trước · 1 2 3 … · Sau) — plan G06.18. */
export function Pagination({ page, total, hrefFor, onChange, className }: PaginationProps) {
  const { t } = useT();
  if (total <= 1) return null;

  const Item = ({
    to,
    disabled,
    current,
    label,
    children,
    wide,
  }: {
    to: number;
    disabled?: boolean;
    current?: boolean;
    label?: string;
    children: ReactNode;
    wide?: boolean;
  }) => {
    const cls = cn(BTN, wide ? 'px-3' : 'px-0', current ? ACTIVE : IDLE, disabled && OFF);
    if (disabled) {
      return (
        <span className={cls} aria-disabled="true">
          {children}
        </span>
      );
    }
    if (hrefFor) {
      return (
        <a
          href={hrefFor(to)}
          className={cls}
          aria-current={current ? 'page' : undefined}
          aria-label={label}
          rel={
            label === t('common.label.prev_page')
              ? 'prev'
              : label === t('common.label.next_page')
                ? 'next'
                : undefined
          }
          onClick={(e) => {
            if (onChange) {
              e.preventDefault();
              if (!current) onChange(to);
            }
          }}
        >
          {children}
        </a>
      );
    }
    return (
      <button
        type="button"
        className={cls}
        aria-current={current ? 'page' : undefined}
        aria-label={label}
        onClick={() => !current && onChange?.(to)}
      >
        {children}
      </button>
    );
  };

  return (
    <nav aria-label={t('ui.pagination.label')} className={cn('flex justify-center', className)}>
      <ul className="flex flex-wrap justify-center items-center gap-1.5 list-none m-0 p-0">
        <li>
          <Item to={page - 1} disabled={page <= 1} label={t('common.label.prev_page')} wide>
            <Icon name="chevron-left" size={16} />
            <span className="hidden sm:inline">{t('ui.pagination.prev')}</span>
          </Item>
        </li>
        {pageList(page, total).map((p, i) =>
          p === '…' ? (
            <li key={`e${i}`} aria-hidden="true" className="w-6 text-center text-subtle">
              …
            </li>
          ) : (
            <li key={p}>
              <Item to={p} current={p === page} label={t('common.label.page', { page: p })}>
                {p}
              </Item>
            </li>
          ),
        )}
        <li>
          <Item to={page + 1} disabled={page >= total} label={t('common.label.next_page')} wide>
            <span className="hidden sm:inline">{t('ui.pagination.next')}</span>
            <Icon name="chevron-right" size={16} />
          </Item>
        </li>
      </ul>
    </nav>
  );
}
