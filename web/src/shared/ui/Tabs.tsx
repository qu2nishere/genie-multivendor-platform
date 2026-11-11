import { useRef, type KeyboardEvent, type ReactNode } from 'react';
import { cn } from '@/shared/lib/cn';

export interface TabItem<K extends string = string> {
  id: K;
  label: ReactNode;
  badge?: ReactNode;
  disabled?: boolean;
  /** Có href → tab là liên kết thật (lọc theo query string, R16). */
  href?: string;
}

interface TabsBase<K extends string> {
  items: TabItem<K>[];
  value: K;
  onChange?: (id: K) => void;
  size?: 'sm' | 'md';
  className?: string;
  'aria-label'?: string;
}

/** Phím mũi tên / Home / End chuyển tab, tự kích hoạt (mẫu WAI-ARIA tabs). */
function useRovingKeys<K extends string>(
  items: TabItem<K>[],
  value: K,
  onChange?: (id: K) => void,
) {
  const refs = useRef<(HTMLElement | null)[]>([]);
  const onKeyDown = (e: KeyboardEvent<HTMLElement>) => {
    const keys = ['ArrowRight', 'ArrowLeft', 'Home', 'End'];
    if (!keys.includes(e.key)) return;
    e.preventDefault();
    const enabled = items.map((it, i) => (it.disabled ? -1 : i)).filter((i) => i >= 0);
    const cur = enabled.indexOf(items.findIndex((it) => it.id === value));
    let next = cur;
    if (e.key === 'ArrowRight') next = (cur + 1) % enabled.length;
    if (e.key === 'ArrowLeft') next = (cur - 1 + enabled.length) % enabled.length;
    if (e.key === 'Home') next = 0;
    if (e.key === 'End') next = enabled.length - 1;
    const idx = enabled[next];
    refs.current[idx]?.focus();
    onChange?.(items[idx].id);
  };
  return { refs, onKeyDown };
}

/** Thanh tab gạch chân (prototype Tabs) — role tablist, phím mũi tên (G06.09). */
export function Tabs<K extends string = string>({
  items,
  value,
  onChange,
  size = 'md',
  className,
  'aria-label': ariaLabel,
}: TabsBase<K>) {
  const { refs, onKeyDown } = useRovingKeys(items, value, onChange);
  return (
    <div
      role="tablist"
      aria-label={ariaLabel}
      onKeyDown={onKeyDown}
      className={cn(
        'lz-tabs flex overflow-x-auto border-b border-line [scrollbar-width:none]',
        size === 'sm' ? 'gap-[18px]' : 'gap-[26px]',
        className,
      )}
    >
      {items.map((it, i) => {
        const on = it.id === value;
        const cls = cn(
          'relative inline-flex items-center gap-[7px] flex-none bg-transparent border-0 cursor-pointer whitespace-nowrap no-underline hover:no-underline',
          size === 'sm' ? 'py-2 text-sm' : 'py-3 text-base',
          on ? 'font-semibold text-primary' : 'font-medium text-muted hover:text-body',
          it.disabled && 'opacity-[.45] cursor-not-allowed',
          'focus-visible:outline-none focus-visible:shadow-focus rounded-xs',
        );
        const inner = (
          <>
            {it.label}
            {it.badge != null && (
              <span
                className={cn(
                  'min-w-[18px] h-[18px] px-[5px] rounded-[9px] grid place-items-center text-[11px] font-bold',
                  on ? 'bg-primary text-white' : 'bg-gray-200 text-muted',
                )}
              >
                {it.badge}
              </span>
            )}
            <span
              aria-hidden="true"
              className={cn(
                'absolute left-0 right-0 -bottom-px h-[2.5px] rounded-[2px]',
                on ? 'bg-primary' : 'bg-transparent',
              )}
            />
          </>
        );
        const common = {
          role: 'tab' as const,
          'aria-selected': on,
          tabIndex: on ? 0 : -1,
          className: cls,
        };
        return it.href ? (
          <a
            key={it.id}
            ref={(el) => {
              refs.current[i] = el;
            }}
            href={it.href}
            aria-current={on ? 'page' : undefined}
            onClick={(e) => {
              if (onChange) {
                e.preventDefault();
                onChange(it.id);
              }
            }}
            {...common}
          >
            {inner}
          </a>
        ) : (
          <button
            key={it.id}
            ref={(el) => {
              refs.current[i] = el;
            }}
            type="button"
            disabled={it.disabled}
            onClick={() => onChange?.(it.id)}
            {...common}
          >
            {inner}
          </button>
        );
      })}
    </div>
  );
}

/**
 * Nhóm nút bật / tắt dạng viên (prototype dash.jsx Segmented, .dash-seg) — chọn khoảng thời gian,
 * chế độ xem. role="group" + aria-pressed như prototype; Tab đi qua từng nút.
 */
export function Segmented<K extends string = string>({
  items,
  value,
  onChange,
  size = 'md',
  className,
  'aria-label': ariaLabel,
}: TabsBase<K>) {
  return (
    <div
      role="group"
      aria-label={ariaLabel}
      className={cn('dash-seg inline-flex p-[3px] gap-0.5 rounded-md bg-gray-100', className)}
    >
      {items.map((it) => {
        const on = it.id === value;
        return (
          <button
            key={it.id}
            type="button"
            aria-pressed={on}
            disabled={it.disabled}
            onClick={() => onChange?.(it.id)}
            className={cn(
              'inline-flex items-center gap-1.5 px-3 rounded-sm border-0 cursor-pointer whitespace-nowrap text-xs',
              size === 'sm' ? 'min-h-[30px]' : 'min-h-[34px]',
              on
                ? 'bg-surface-card text-strong font-semibold shadow-xs'
                : 'bg-transparent text-muted font-medium hover:text-body',
              'focus-visible:outline-none focus-visible:shadow-focus disabled:opacity-[.45]',
            )}
          >
            {it.label}
            {it.badge != null && <span className="text-2xs text-muted">{it.badge}</span>}
          </button>
        );
      })}
    </div>
  );
}
