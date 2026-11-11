// Hook i18n cho island (plan G04.08): đọc locale từ nanostore $locale (trình duyệt) hoặc từ
// request hiện tại (SSR), đồng bộ với cookie `lang` mà middleware.ts đọc.
import { useStore } from '@nanostores/react';
import { useMemo } from 'react';
import { createT, type TFunction } from '@/shared/lib/i18n';
import { $locale, currentLocale, type Locale } from '@/shared/lib/locale';

export interface UseT {
  t: TFunction;
  locale: Locale;
}

export function useT(): UseT {
  const clientLocale = useStore($locale);
  const locale = typeof window === 'undefined' ? currentLocale() : clientLocale;
  return useMemo(() => ({ t: createT(locale), locale }), [locale]);
}
