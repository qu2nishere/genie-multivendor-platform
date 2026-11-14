import {
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
  type CSSProperties,
  type KeyboardEvent,
} from 'react';
import { createPortal } from 'react-dom';
import { useT } from '@/shared/hooks/useT';
import { onStorageChange, store } from '@/shared/lib/storage';
import { Icon } from '../icon/Icon';
import { GenieMark } from './GenieMark';
import { DEFAULT_THEME, isThemeId, THEMES, type ThemeId } from './themes';

/** Đặt theme: ghi genie.theme, đổi data-theme, phát lại hiệu ứng mở đầu (prototype GENIE_SET_THEME). */
export function applyTheme(id: ThemeId, save = true): void {
  const root = document.documentElement;
  if (save) store.set('theme', id);
  root.setAttribute('data-theme', id);
  root.classList.remove('tr-play');
  void root.offsetWidth; // ép trình duyệt tính lại để animation chạy lại
  root.classList.add('tr-play');
}

const readTheme = (): ThemeId => {
  const cur =
    typeof document !== 'undefined' ? document.documentElement.getAttribute('data-theme') : null;
  return isThemeId(cur) ? cur : DEFAULT_THEME;
};

/**
 * Nút bảng màu mở danh sách 9 giao diện (prototype brand.jsx ThemePicker) — plan G07.25.
 * Mỗi lựa chọn tự xem trước: đồng xu nằm trong data-theme riêng nên mang màu của theme đó.
 * Lưu genie.theme, đồng bộ giữa các tab (sự kiện storage), radiogroup có phím mũi tên / Home / End.
 */
export function ThemePicker({ className = 'tr-iconbtn' }: { className?: string }) {
  const { t } = useT();
  const [open, setOpen] = useState(false);
  const [cur, setCur] = useState<ThemeId>(DEFAULT_THEME);
  const btn = useRef<HTMLButtonElement>(null);
  const list = useRef<HTMLDivElement>(null);
  const [pos, setPos] = useState<{ top: number; right: number } | null>(null);

  // Đồng bộ với data-theme hiện có (ThemeBoot) và tab khác
  useEffect(() => {
    setCur(readTheme());
    const mo = new MutationObserver(() => setCur(readTheme()));
    mo.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] });
    const off = onStorageChange('theme', () => {
      const v = store.get<string>('theme');
      if (isThemeId(v)) applyTheme(v, false);
    });
    return () => {
      mo.disconnect();
      off();
    };
  }, []);

  useEffect(() => {
    if (!open) return undefined;
    (list.current?.querySelector<HTMLElement>('[aria-checked="true"]') ?? null)?.focus();
    const onKey = (e: globalThis.KeyboardEvent) => {
      if (e.key === 'Escape') {
        setOpen(false);
        btn.current?.focus();
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [open]);

  // Bảng gắn vào <body> (lớp kính backdrop-filter của thanh sẽ "nhốt" nó); màn rộng treo dưới nút.
  useLayoutEffect(() => {
    if (!open) return undefined;
    const place = () => {
      const r = btn.current?.getBoundingClientRect();
      if (r) setPos({ top: r.bottom + 10, right: Math.max(8, window.innerWidth - r.right - 6) });
    };
    place();
    window.addEventListener('resize', place);
    window.addEventListener('scroll', place, { passive: true });
    return () => {
      window.removeEventListener('resize', place);
      window.removeEventListener('scroll', place);
    };
  }, [open]);

  const pick = (id: ThemeId) => {
    applyTheme(id);
    setCur(id);
  };

  const onListKey = (e: KeyboardEvent<HTMLDivElement>) => {
    if (!['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(e.key)) return;
    e.preventDefault();
    const items = Array.from(list.current?.querySelectorAll<HTMLElement>('[role="radio"]') ?? []);
    const i = items.indexOf(document.activeElement as HTMLElement);
    const n =
      e.key === 'Home'
        ? 0
        : e.key === 'End'
          ? items.length - 1
          : (i + (e.key === 'ArrowDown' ? 1 : -1) + items.length) % items.length;
    items[n]?.focus();
  };

  const name = t(`theme.${cur}.name`);
  return (
    <div className="tr-theme">
      <button
        ref={btn}
        type="button"
        className={className}
        onClick={() => setOpen((o) => !o)}
        aria-haspopup="dialog"
        aria-expanded={open}
        aria-label={`${t('layout.header.theme')}: ${name}`}
        title={t('theme.picker.change')}
      >
        <svg
          width="22"
          height="22"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
          aria-hidden="true"
        >
          <path d="M12 22a10 10 0 1 1 10-10c0 2.2-1.8 3.5-4 3.5h-1.8a1.7 1.7 0 0 0-1.3 2.8c.4.5.6 1 .6 1.6 0 1.2-1.4 2.1-3.5 2.1Z" />
          <circle cx="7.5" cy="10.5" r="1.1" fill="currentColor" />
          <circle cx="10.5" cy="6.8" r="1.1" fill="currentColor" />
          <circle cx="15.2" cy="7.3" r="1.1" fill="currentColor" />
        </svg>
      </button>
      {open &&
        createPortal(
          <>
            <span className="tr-theme__scrim" onClick={() => setOpen(false)} />
            <div
              className="tr-theme__pop"
              role="dialog"
              aria-label={t('theme.picker.title')}
              style={
                pos
                  ? ({
                      '--pop-top': `${pos.top}px`,
                      '--pop-right': `${pos.right}px`,
                    } as CSSProperties)
                  : undefined
              }
            >
              <div className="tr-theme__head">
                <b>{t('layout.header.theme')}</b>
                <span>{t('theme.picker.saved_here')}</span>
              </div>
              <div
                ref={list}
                role="radiogroup"
                aria-label={t('layout.header.theme')}
                className="tr-theme__list"
                onKeyDown={onListKey}
              >
                {THEMES.map((th) => (
                  <button
                    key={th.id}
                    type="button"
                    role="radio"
                    aria-checked={th.id === cur}
                    tabIndex={th.id === cur ? 0 : -1}
                    className="tr-theme__opt"
                    onClick={() => pick(th.id)}
                  >
                    <span className="tr-theme__coin" data-theme={th.id}>
                      <GenieMark size={36} />
                    </span>
                    <span className="tr-theme__txt">
                      <b>
                        {t(`theme.${th.id}.name`)}
                        {th.id === cur && <Icon name="check" size={14} strokeWidth={2.6} />}
                      </b>
                      <small>{t(`theme.${th.id}.note`)}</small>
                    </span>
                    <span className="tr-theme__sw" aria-hidden="true">
                      {th.swatch.map((c) => (
                        <i key={c} style={{ background: c }} />
                      ))}
                    </span>
                  </button>
                ))}
              </div>
            </div>
          </>,
          document.body,
        )}
    </div>
  );
}
