// Góc trên phải trang đăng nhập (prototype auth Panel): nút đổi theme + chọn ngôn ngữ (plan G07.25).
import { useT } from '@/shared/hooks/useT';
import { LOCALE_LABELS, LOCALES, switchLocale, toLocale } from '@/shared/lib/locale';
import { ThemePicker } from '@/shared/ui/brand';

export default function AuthTools() {
  const { t, locale } = useT();
  return (
    <>
      <ThemePicker className="gn-iconbtn" />
      <select
        aria-label={t('layout.header.language')}
        value={locale}
        onChange={(e) => switchLocale(toLocale(e.target.value))}
        className="h-8 border border-line rounded-sm bg-surface-card text-xs text-body px-1.5"
      >
        {LOCALES.map((l) => (
          <option key={l} value={l} lang={l}>
            {LOCALE_LABELS[l]}
          </option>
        ))}
      </select>
    </>
  );
}
