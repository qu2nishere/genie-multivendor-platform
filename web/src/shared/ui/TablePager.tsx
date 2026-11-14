import { useT } from '@/shared/hooks/useT';
import { formatNumber } from '@/shared/lib/format';
import { cn } from '@/shared/lib/cn';
import { Button } from './Button';

// Chân bảng quản trị (prototype admin: "Hiển thị 1 - 5 trong tổng số 7 shop · Trước · Trang 1 / 2 · Sau").

export interface TablePagerProps {
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
  onChange: (page: number) => void;
  /** Đơn vị đếm đã dịch: "shop", "hồ sơ"… */
  unit: string;
  className?: string;
}

export function TablePager({
  page,
  size,
  totalItems,
  totalPages,
  onChange,
  unit,
  className,
}: TablePagerProps) {
  const { t, locale } = useT();
  const from = totalItems ? (page - 1) * size + 1 : 0;
  const to = Math.min(page * size, totalItems);
  const pages = Math.max(1, totalPages);
  return (
    <div
      className={cn(
        'flex items-center gap-2.5 flex-wrap px-5 py-3 border-0 border-t border-solid border-line-subtle text-xs text-muted',
        className,
      )}
    >
      <span>
        {t('ui.pager.showing', {
          from: formatNumber(from, { locale }),
          to: formatNumber(to, { locale }),
          total: formatNumber(totalItems, { locale }),
          unit,
        })}
      </span>
      <Button
        size="sm"
        variant="outline"
        className="ml-auto"
        disabled={page <= 1}
        onClick={() => onChange(page - 1)}
      >
        {t('ui.pager.prev')}
      </Button>
      <span aria-live="polite">{t('ui.pager.page', { page, pages })}</span>
      <Button
        size="sm"
        variant="outline"
        disabled={page >= pages}
        onClick={() => onChange(page + 1)}
      >
        {t('ui.pager.next')}
      </Button>
    </div>
  );
}
