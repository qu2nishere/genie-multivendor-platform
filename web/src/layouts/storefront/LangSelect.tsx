import { useT } from '@/shared/hooks/useT';
import { switchLocale, type Locale } from '@/shared/lib/locale';
import { Icon, Popover } from '@/shared/ui';

const LANGS: { id: Locale; label: string }[] = [
  { id: 'vi', label: 'Tiếng Việt' },
  { id: 'en', label: 'English' },
];

/** Chọn ngôn ngữ (prototype Header.jsx LangSelect) — plan G07.12: ghi cookie `lang` rồi tải lại để SSR đúng. */
export default function LangSelect() {
  const { t, locale } = useT();
  const cur = LANGS.find((l) => l.id === locale) ?? LANGS[0];
  return (
    <Popover
      role="menu"
      aria-label={t('layout.header.language')}
      panelClassName="w-[168px] min-w-0 p-1 rounded-md shadow-xl"
      trigger={(p) => (
        <button
          type="button"
          className="tr-textbtn"
          aria-label={`${t('layout.header.language')}: ${cur.label}`}
          {...p}
        >
          <span className="tr-lang-full">{cur.label}</span>
          <span className="tr-lang-code">{cur.id.toUpperCase()}</span>
          <Icon
            name="chevron-down"
            size={12}
            className="transition-transform duration-fast"
            style={{ transform: p['aria-expanded'] ? 'rotate(180deg)' : undefined }}
          />
        </button>
      )}
    >
      {(close) => (
        <ul role="none" className="list-none m-0 p-0">
          {LANGS.map((l) => (
            <li key={l.id} role="none">
              <button
                type="button"
                role="menuitemradio"
                aria-checked={l.id === locale}
                lang={l.id}
                onClick={() => {
                  close();
                  if (l.id !== locale) switchLocale(l.id);
                }}
                className={`flex w-full items-center gap-[9px] px-2.5 py-[9px] rounded-sm border-0 cursor-pointer text-left text-[13px] ${
                  l.id === locale
                    ? 'bg-brand-50 text-brand-600 font-semibold'
                    : 'bg-transparent text-body font-medium hover:bg-gray-50'
                }`}
              >
                <span className="flex-1">{l.label}</span>
                {l.id === locale && <Icon name="check" size={15} />}
              </button>
            </li>
          ))}
        </ul>
      )}
    </Popover>
  );
}
