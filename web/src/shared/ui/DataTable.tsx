import type { ReactNode } from 'react';
import { cn } from '@/shared/lib/cn';
import { EmptyState, Skeleton } from './EmptyState';
import type { IconName } from './icon/Icon';

export interface Column<T> {
  key: string;
  header: ReactNode;
  cell: (row: T, index: number) => ReactNode;
  align?: 'left' | 'right' | 'center';
  /** Cho phép xuống dòng (mặc định nowrap như prototype). */
  wrap?: boolean;
  width?: string | number;
  className?: string;
}

export interface DataTableProps<T> {
  columns: Column<T>[];
  rows: T[];
  rowKey: (row: T) => string | number;
  /** Nhãn bảng cho trình đọc màn hình. */
  caption?: ReactNode;
  /** Bề rộng tối thiểu trước khi cuộn ngang (prototype: 900). */
  minWidth?: number;
  loading?: boolean;
  empty?: { icon?: IconName; text: ReactNode; action?: ReactNode };
  onRowClick?: (row: T) => void;
  /** Hàng đang chọn (tô nền). */
  activeKey?: string | number | null;
  dense?: boolean;
  className?: string;
}

/**
 * Bảng quản trị (G06.27) — thay bộ hrow / brow / th / td lặp ở 8 file admin / vendor:
 * tiêu đề xám 12px nền gray-25, hàng viền trên, ô 12px 14px nowrap, số dạng bảng, cuộn ngang trong thẻ.
 */
export function DataTable<T>({
  columns,
  rows,
  rowKey,
  caption,
  minWidth = 900,
  loading,
  empty,
  onRowClick,
  activeKey,
  dense,
  className,
}: DataTableProps<T>) {
  const pad = dense ? 'py-2.5 px-3' : 'py-3 px-3.5';
  const align = (a?: Column<T>['align']) =>
    a === 'right' ? 'text-right' : a === 'center' ? 'text-center' : 'text-left';
  return (
    <div className={cn('relative bg-surface-card rounded-lg shadow-sm overflow-x-auto', className)}>
      <table className="w-full border-collapse tabular-nums" style={{ minWidth }}>
        {caption && <caption className="sr-only">{caption}</caption>}
        <thead>
          <tr className="bg-gray-25 text-muted font-semibold text-xs text-left">
            {columns.map((c) => (
              <th
                key={c.key}
                scope="col"
                className={cn('py-[11px] px-3.5 whitespace-nowrap font-semibold', align(c.align))}
                style={c.width != null ? { width: c.width } : undefined}
              >
                {c.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {loading &&
            Array.from({ length: 5 }, (_, i) => (
              <tr key={`sk${i}`} className="border-t border-line-subtle">
                {columns.map((c) => (
                  <td key={c.key} className={pad}>
                    <Skeleton className="w-24" />
                  </td>
                ))}
              </tr>
            ))}
          {!loading &&
            rows.map((r, i) => {
              const k = rowKey(r);
              return (
                <tr
                  key={k}
                  onClick={onRowClick ? () => onRowClick(r) : undefined}
                  className={cn(
                    'border-t border-line-subtle text-[13px] text-body',
                    onRowClick && 'cursor-pointer hover:bg-gray-25',
                    activeKey === k && 'bg-brand-50 hover:bg-brand-50',
                  )}
                >
                  {columns.map((c) => (
                    <td
                      key={c.key}
                      className={cn(
                        pad,
                        'align-middle',
                        c.wrap ? 'whitespace-normal' : 'whitespace-nowrap',
                        align(c.align),
                        c.className,
                      )}
                    >
                      {c.cell(r, i)}
                    </td>
                  ))}
                </tr>
              );
            })}
        </tbody>
      </table>
      {!loading && rows.length === 0 && empty && (
        <EmptyState compact icon={empty.icon ?? 'list'} text={empty.text} action={empty.action} />
      )}
    </div>
  );
}
