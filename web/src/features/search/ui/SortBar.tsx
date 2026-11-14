// Thanh sắp xếp (prototype .tr-sortbar) — plan G08.19. Mỗi nút sắp xếp là liên kết thật (R16);
// bấm lại "Giá" đảo chiều. Nút "Bộ lọc" (chỉ hiện ở màn hẹp) mở ngăn kéo FilterPanel. Island client:idle.
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatNumber } from '@/shared/lib/format';
import { Icon } from '@/shared/ui';
import { openFilters } from '../application/filtersDrawer';
import {
  activeChips,
  filtersHref,
  nextSort,
  type FilterState,
  type SortId,
} from '../domain/filters';
import { sortLabel } from './labels';

export interface SortBarProps {
  path: string;
  state: FilterState;
  sorts: readonly SortId[];
  /** Tổng kết quả (null = không hiện, trang tìm kiếm đã hiện ở đầu trang). */
  total: number | null;
}

export default function SortBar({ path, state: s, sorts, total }: SortBarProps) {
  const { t, locale } = useT();
  const chips = activeChips(s).filter((c) => c.kind !== 'sub').length;
  return (
    <div className="tr-sortbar mb-3.5 flex flex-wrap items-center gap-2.5 rounded-md border border-solid border-line-subtle bg-surface-sunken px-3.5 py-[9px]">
      <button type="button" className="tr-filterbtn" onClick={openFilters} aria-controls="filters">
        <Icon name="sliders-horizontal" size={16} /> {t('search.filter.button')}
        {chips > 0 && <b>{chips}</b>}
      </button>
      <span className="tr-wide-only text-sm text-muted">{t('search.sort.label')}</span>
      <nav aria-label={t('search.sort.label')} className="contents">
        {sorts.map((id) => {
          const on = s.sort === id;
          const next = nextSort(s, id);
          return (
            <a
              key={id}
              href={filtersHref(path, s, next)}
              aria-current={on ? 'true' : undefined}
              rel="nofollow"
              className={cn(
                'inline-flex h-[34px] items-center gap-[5px] rounded-sm px-3.5 text-sm no-underline hover:no-underline max-md:h-11',
                on
                  ? 'bg-primary font-semibold text-white'
                  : 'bg-surface-card text-body shadow-[inset_0_0_0_1px_var(--border-default)] hover:text-primary',
              )}
            >
              {sortLabel(id, t)}
              {id === 'price' && (
                <>
                  <Icon name={on && s.dir === 'desc' ? 'chevron-down' : 'chevron-up'} size={14} />
                  {on && (
                    <span className="sr-only">
                      {s.dir === 'desc' ? t('search.sort.price_desc') : t('search.sort.price_asc')}
                    </span>
                  )}
                </>
              )}
            </a>
          );
        })}
      </nav>
      {total != null && (
        <span className="ml-auto text-sm text-muted">
          <b className="text-primary">{formatNumber(total, { locale })}</b>{' '}
          {t('search.result.products')}
        </span>
      )}
    </div>
  );
}
