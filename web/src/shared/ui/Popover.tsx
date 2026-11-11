import {
  useCallback,
  useEffect,
  useId,
  useRef,
  useState,
  type KeyboardEvent,
  type ReactNode,
  type RefObject,
} from 'react';
import { useClickOutside } from '@/shared/hooks/useClickOutside';
import { cn } from '@/shared/lib/cn';
import { Icon, type IconName } from './icon/Icon';

export interface PopoverTriggerProps {
  ref: RefObject<HTMLButtonElement | null>;
  'aria-expanded': boolean;
  'aria-controls': string;
  'aria-haspopup': 'dialog' | 'menu';
  onClick: () => void;
}

export interface PopoverProps {
  /** Hàm dựng nút mở, nhận sẵn thuộc tính ARIA + ref. */
  trigger: (p: PopoverTriggerProps) => ReactNode;
  children: ReactNode | ((close: () => void) => ReactNode);
  align?: 'start' | 'end';
  role?: 'dialog' | 'menu';
  'aria-label'?: string;
  open?: boolean;
  onOpenChange?: (open: boolean) => void;
  className?: string;
  panelClassName?: string;
}

/**
 * Bảng nổi gắn dưới nút (nền cho menu tài khoản, chuông thông báo, mini-cart) — plan G06.13.
 * Esc / bấm ngoài để đóng, trả focus về nút.
 */
export function Popover({
  trigger,
  children,
  align = 'end',
  role = 'dialog',
  'aria-label': ariaLabel,
  open: controlled,
  onOpenChange,
  className,
  panelClassName,
}: PopoverProps) {
  const [inner, setInner] = useState(false);
  const open = controlled ?? inner;
  const setOpen = useCallback(
    (v: boolean) => {
      if (controlled === undefined) setInner(v);
      onOpenChange?.(v);
    },
    [controlled, onOpenChange],
  );
  const wrap = useRef<HTMLDivElement>(null);
  const btn = useRef<HTMLButtonElement>(null);
  const panelId = useId();
  const close = useCallback(() => setOpen(false), [setOpen]);
  useClickOutside(wrap, close, open);

  useEffect(() => {
    if (!open) return undefined;
    const onKey = (e: globalThis.KeyboardEvent) => {
      if (e.key === 'Escape') {
        setOpen(false);
        btn.current?.focus();
      }
    };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [open, setOpen]);

  return (
    <div ref={wrap} className={cn('relative inline-flex', className)}>
      {trigger({
        ref: btn,
        'aria-expanded': open,
        'aria-controls': panelId,
        'aria-haspopup': role,
        onClick: () => setOpen(!open),
      })}
      {open && (
        <div
          id={panelId}
          role={role}
          aria-label={ariaLabel}
          className={cn(
            'absolute top-[calc(100%+8px)] z-dropdown min-w-[220px] bg-surface-card border border-line-subtle rounded-lg shadow-lg',
            'animate-[lzSlideDown_.18s_var(--ease-out)]',
            align === 'end' ? 'right-0' : 'left-0',
            panelClassName,
          )}
        >
          {typeof children === 'function' ? children(close) : children}
        </div>
      )}
    </div>
  );
}

export interface MenuItem {
  key: string;
  label: ReactNode;
  icon?: IconName;
  href?: string;
  onSelect?: () => void;
  danger?: boolean;
  divider?: boolean;
}

/** Danh sách mục trong Popover role="menu": ↑ ↓ Home End di chuyển, Enter chọn (G06.13). */
export function MenuList({ items, onDone }: { items: MenuItem[]; onDone?: () => void }) {
  const refs = useRef<(HTMLElement | null)[]>([]);
  useEffect(() => {
    refs.current.find(Boolean)?.focus();
  }, []);
  const onKey = (e: KeyboardEvent<HTMLUListElement>) => {
    const list = refs.current.filter((x): x is HTMLElement => Boolean(x));
    const i = list.indexOf(document.activeElement as HTMLElement);
    let n = -1;
    if (e.key === 'ArrowDown') n = (i + 1) % list.length;
    if (e.key === 'ArrowUp') n = (i - 1 + list.length) % list.length;
    if (e.key === 'Home') n = 0;
    if (e.key === 'End') n = list.length - 1;
    if (n >= 0) {
      e.preventDefault();
      list[n].focus();
    }
  };
  const cls = (danger?: boolean) =>
    cn(
      'flex w-full items-center gap-2.5 min-h-[44px] px-3.5 text-left text-base no-underline hover:no-underline rounded-sm',
      'bg-transparent border-0 cursor-pointer focus-visible:outline-none focus-visible:bg-gray-100 hover:bg-gray-100',
      danger ? 'text-red-600' : 'text-body',
    );
  return (
    <ul role="none" className="p-1.5 m-0 list-none" onKeyDown={onKey}>
      {items.map((it, idx) => (
        <li
          key={it.key}
          role="none"
          className={cn(it.divider && 'border-t border-line-subtle mt-1 pt-1')}
        >
          {it.href ? (
            <a
              ref={(el) => {
                refs.current[idx] = el;
              }}
              role="menuitem"
              href={it.href}
              tabIndex={-1}
              className={cls(it.danger)}
              onClick={() => onDone?.()}
            >
              {it.icon && <Icon name={it.icon} size={18} />}
              {it.label}
            </a>
          ) : (
            <button
              ref={(el) => {
                refs.current[idx] = el;
              }}
              type="button"
              role="menuitem"
              tabIndex={-1}
              className={cls(it.danger)}
              onClick={() => {
                it.onSelect?.();
                onDone?.();
              }}
            >
              {it.icon && <Icon name={it.icon} size={18} />}
              {it.label}
            </button>
          )}
        </li>
      ))}
    </ul>
  );
}

/** Popover dạng menu (tài khoản, thao tác "…" trên hàng). */
export function Dropdown({
  trigger,
  items,
  align,
  'aria-label': ariaLabel,
}: {
  trigger: PopoverProps['trigger'];
  items: MenuItem[];
  align?: 'start' | 'end';
  'aria-label'?: string;
}) {
  return (
    <Popover trigger={trigger} role="menu" align={align} aria-label={ariaLabel}>
      {(close) => <MenuList items={items} onDone={close} />}
    </Popover>
  );
}
