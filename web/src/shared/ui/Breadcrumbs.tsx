// Breadcrumb (prototype .tr-crumbs: "Trang chủ › …") — <nav aria-label> + <ol> (R18). Mục cuối là
// trang hiện tại (aria-current), các mục trước là liên kết thật. Render tĩnh trong .astro được.
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { Icon } from './icon/Icon';

export interface Crumb {
  name: string;
  /** Bỏ trống ở mục cuối (trang hiện tại). */
  href?: string;
}

export function Breadcrumbs({ items, className }: { items: Crumb[]; className?: string }) {
  const { t } = useT();
  return (
    <nav aria-label={t('ui.breadcrumb.label')} className={cn('tr-crumbs mb-3.5', className)}>
      <ol className="m-0 flex list-none flex-wrap items-center gap-[7px] p-0 text-sm text-muted">
        {items.map((c, i) => {
          const last = i === items.length - 1;
          return (
            <li key={`${c.name}-${i}`} className="inline-flex min-w-0 items-center gap-[7px]">
              {i > 0 && <Icon name="chevron-right" size={14} aria-hidden="true" />}
              {c.href && !last ? (
                <a href={c.href} className="text-link no-underline hover:underline">
                  {c.name}
                </a>
              ) : (
                <span
                  aria-current={last ? 'page' : undefined}
                  className="truncate font-semibold text-body"
                >
                  {c.name}
                </span>
              )}
            </li>
          );
        })}
      </ol>
    </nav>
  );
}
