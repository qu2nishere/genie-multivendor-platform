import {
  useCallback,
  useEffect,
  useId,
  useRef,
  useState,
  type KeyboardEvent,
  type ReactNode,
} from 'react';
import { useClickOutside } from '@/shared/hooks/useClickOutside';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { Field, inputClass } from './Field';
import { Icon } from './icon/Icon';

export interface SelectOption<V extends string | number = string> {
  value: V;
  label: ReactNode;
  /** Chữ thuần để gõ tìm nhanh khi label là ReactNode. */
  text?: string;
  disabled?: boolean;
}

export interface SelectProps<V extends string | number = string> {
  options: SelectOption<V>[];
  value?: V | null;
  defaultValue?: V;
  onChange?: (value: V) => void;
  label?: ReactNode;
  hint?: ReactNode;
  error?: ReactNode;
  required?: boolean;
  placeholder?: string;
  disabled?: boolean;
  /** Có name → kèm input ẩn để form HTML gửi được. */
  name?: string;
  id?: string;
  className?: string;
  'aria-label'?: string;
}

/**
 * Danh sách thả theo kiểu prototype (khung trắng, mục chọn nền brand-50) nhưng đủ bàn phím (G06.08):
 * ↑ ↓ Home End để di chuyển, Enter / Space chọn, Esc đóng, gõ chữ để nhảy, bấm ngoài thì đóng.
 */
export function Select<V extends string | number = string>({
  options,
  value,
  defaultValue,
  onChange,
  label,
  hint,
  error,
  required,
  placeholder,
  disabled,
  name,
  id,
  className,
  'aria-label': ariaLabel,
}: SelectProps<V>) {
  const { t } = useT();
  const [inner, setInner] = useState<V | undefined>(defaultValue);
  const current = value !== undefined ? value : inner;
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(-1);
  const wrap = useRef<HTMLDivElement>(null);
  const button = useRef<HTMLButtonElement>(null);
  const listId = `${useId().replace(/:/g, '')}-list`;
  const selectedIndex = options.findIndex((o) => o.value === current);
  const selected = selectedIndex >= 0 ? options[selectedIndex] : undefined;

  const close = useCallback(() => setOpen(false), []);
  useClickOutside(wrap, close, open);

  useEffect(() => {
    if (open) setActive(selectedIndex >= 0 ? selectedIndex : options.findIndex((o) => !o.disabled));
  }, [open, selectedIndex, options]);

  const choose = (i: number) => {
    const o = options[i];
    if (!o || o.disabled) return;
    if (value === undefined) setInner(o.value);
    onChange?.(o.value);
    setOpen(false);
    button.current?.focus();
  };

  const move = (from: number, dir: 1 | -1) => {
    for (let k = 1; k <= options.length; k++) {
      const i = (from + dir * k + options.length) % options.length;
      if (!options[i].disabled) return i;
    }
    return from;
  };

  const typeahead = useRef({ q: '', at: 0 });
  const onKey = (e: KeyboardEvent<HTMLButtonElement>) => {
    if (disabled) return;
    switch (e.key) {
      case 'ArrowDown':
      case 'ArrowUp': {
        e.preventDefault();
        if (!open) setOpen(true);
        else setActive((a) => move(a < 0 ? 0 : a, e.key === 'ArrowDown' ? 1 : -1));
        break;
      }
      case 'Home':
      case 'End':
        if (open) {
          e.preventDefault();
          setActive(e.key === 'Home' ? move(-1 + options.length, 1) : move(0, -1));
        }
        break;
      case 'Enter':
      case ' ':
        e.preventDefault();
        if (open && active >= 0) choose(active);
        else setOpen(true);
        break;
      case 'Escape':
        if (open) {
          e.preventDefault();
          setOpen(false);
        }
        break;
      case 'Tab':
        setOpen(false);
        break;
      default:
        if (e.key.length === 1 && /\S/.test(e.key)) {
          const now = Date.now();
          const q =
            (now - typeahead.current.at < 600 ? typeahead.current.q : '') + e.key.toLowerCase();
          typeahead.current = { q, at: now };
          const i = options.findIndex(
            (o) => !o.disabled && (o.text ?? String(o.label)).toLowerCase().startsWith(q),
          );
          if (i >= 0) {
            if (open) setActive(i);
            else choose(i);
          }
        }
    }
  };

  return (
    <Field
      label={label}
      hint={hint}
      error={error}
      required={required}
      id={id}
      className={className}
    >
      {({ id: fid, describedBy, invalid }) => (
        <div ref={wrap} className="relative">
          <button
            ref={button}
            id={fid}
            type="button"
            disabled={disabled}
            aria-haspopup="listbox"
            aria-expanded={open}
            aria-controls={open ? listId : undefined}
            aria-activedescendant={open && active >= 0 ? `${listId}-${active}` : undefined}
            aria-label={ariaLabel}
            aria-invalid={invalid || undefined}
            aria-describedby={describedBy}
            onClick={() => setOpen((o) => !o)}
            onKeyDown={onKey}
            className={inputClass(
              invalid,
              cn(
                'flex items-center justify-between gap-2 text-left cursor-pointer select-none',
                open && 'border-brand-500',
              ),
            )}
          >
            <span className={cn('truncate', !selected && 'text-subtle')}>
              {selected ? selected.label : (placeholder ?? t('ui.select.placeholder'))}
            </span>
            <Icon
              name="chevron-down"
              size={16}
              className={cn('text-muted transition-transform duration-fast', open && 'rotate-180')}
            />
          </button>
          {name && <input type="hidden" name={name} value={current ?? ''} />}
          {open && (
            <ul
              id={listId}
              role="listbox"
              aria-labelledby={fid}
              className="absolute z-dropdown top-[calc(100%+4px)] left-0 right-0 max-h-[250px] overflow-y-auto p-1 bg-surface-card border border-line rounded-md shadow-md"
            >
              {options.map((o, i) => {
                const isSel = o.value === current;
                return (
                  <li
                    key={String(o.value)}
                    id={`${listId}-${i}`}
                    role="option"
                    aria-selected={isSel}
                    aria-disabled={o.disabled || undefined}
                    onMouseEnter={() => setActive(i)}
                    onMouseDown={(e) => e.preventDefault()}
                    onClick={() => choose(i)}
                    className={cn(
                      'px-3 py-2 rounded-sm cursor-pointer text-base',
                      isSel ? 'bg-brand-50 text-brand-600 font-medium' : 'text-body',
                      i === active && !isSel && 'bg-brand-50 text-brand-600',
                      o.disabled && 'opacity-[.45] cursor-not-allowed',
                    )}
                  >
                    {o.label}
                  </li>
                );
              })}
            </ul>
          )}
        </div>
      )}
    </Field>
  );
}
