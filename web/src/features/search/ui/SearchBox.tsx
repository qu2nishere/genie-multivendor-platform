import { useEffect, useId, useState } from 'react';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { Icon, toIconName } from '@/shared/ui';
import { useSuggestions } from '../application/useSuggestions';
import { searchHref } from '../domain/suggest.types';

/**
 * Ô tìm kiếm header (prototype Header.jsx .tr-search) — plan G07.14: combobox có danh sách gợi ý,
 * ↑ ↓ chọn, Enter mở, Esc đóng; ô trống hiện từ khóa thịnh hành. Là <form action="/search"> thật để
 * tắt JS vẫn tìm được.
 */
export function SearchBox({ initialQuery = '' }: { initialQuery?: string }) {
  const { t } = useT();
  const [q, setQ] = useState(initialQuery);
  const [open, setOpen] = useState(false);
  const [act, setAct] = useState(-1);
  const sug = useSuggestions(q);
  const listId = `${useId().replace(/:/g, '')}-sug`;
  useEffect(() => setAct(-1), [sug]);
  const show = open && sug.length > 0;
  const go = (href: string) => {
    setOpen(false);
    window.location.href = href;
  };
  const hint = (kind: string) =>
    kind === 'trending'
      ? t('search.suggest.trending')
      : kind === 'category'
        ? t('search.suggest.category')
        : kind === 'shop'
          ? t('search.suggest.shop')
          : '';

  return (
    <form
      action="/search"
      method="get"
      role="search"
      className="tr-search"
      onSubmit={(e) => {
        if (show && act >= 0 && sug[act]) {
          e.preventDefault();
          go(sug[act].href);
          return;
        }
        if (!q.trim()) {
          e.preventDefault();
          return;
        }
        e.preventDefault();
        go(searchHref(q));
      }}
    >
      {show && (
        <ul
          role="listbox"
          id={listId}
          aria-label={t('search.suggest.label')}
          className="tr-sug list-none m-0"
        >
          {sug.map((s, i) => (
            <li
              key={s.key}
              id={`${listId}-${i}`}
              role="option"
              aria-selected={i === act}
              onMouseDown={(e) => {
                e.preventDefault();
                go(s.href);
              }}
              onMouseEnter={() => setAct(i)}
              className={cn(
                'flex items-center gap-2.5 px-2.5 py-2 rounded-md cursor-pointer text-body',
                i === act ? 'bg-brand-50' : 'hover:bg-gray-50',
              )}
            >
              {s.image ? (
                <img
                  src={s.image}
                  alt=""
                  width={32}
                  height={32}
                  className="w-8 h-8 rounded-[6px] object-cover flex-none"
                />
              ) : (
                <Icon
                  name={toIconName(s.icon ?? 'search')}
                  size={18}
                  className="text-muted flex-none"
                />
              )}
              <span className="flex-1 min-w-0 text-sm truncate">{s.label}</span>
              {hint(s.kind) && <span className="text-xs text-subtle">{hint(s.kind)}</span>}
            </li>
          ))}
        </ul>
      )}
      <input
        name="q"
        type="search"
        value={q}
        autoComplete="off"
        aria-label={t('common.action.search')}
        role="combobox"
        aria-autocomplete="list"
        aria-controls={listId}
        aria-expanded={show}
        aria-activedescendant={show && act >= 0 ? `${listId}-${act}` : undefined}
        placeholder={t('search.box.placeholder')}
        onChange={(e) => {
          setQ(e.target.value);
          setOpen(true);
        }}
        onFocus={() => setOpen(true)}
        onBlur={() => setOpen(false)}
        onKeyDown={(e) => {
          if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
            e.preventDefault();
            setOpen(true);
            const step = e.key === 'ArrowDown' ? 1 : -1;
            setAct((a) => {
              if (!sug.length) return -1;
              const n = a + step;
              return n < 0 ? sug.length - 1 : n >= sug.length ? 0 : n;
            });
          }
          if (e.key === 'Escape') setOpen(false);
        }}
      />
      <button type="submit" className="tr-search__go" aria-label={t('search.box.submit')}>
        <Icon name="search" size={18} strokeWidth={2.4} />
      </button>
    </form>
  );
}
