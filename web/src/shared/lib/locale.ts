// Ngôn ngữ giao diện dùng chung Astro / island (plan G04.07, G04.08).
// - Trình duyệt: nanostore $locale, khởi tạo từ <html lang> do layout đặt theo cookie `lang`.
// - Máy chủ (SSR): đọc theo từng request qua getter do middleware đăng ký (AsyncLocalStorage),
//   không dùng biến toàn cục để tránh lẫn ngôn ngữ giữa các request chạy song song.
import { atom } from 'nanostores';

export const LOCALES = ['vi', 'en'] as const;
export type Locale = (typeof LOCALES)[number];
export const DEFAULT_LOCALE: Locale = 'vi';
/** Tên ngôn ngữ viết bằng chính ngôn ngữ đó (không dịch) — ô chọn ngôn ngữ. */
export const LOCALE_LABELS: Record<Locale, string> = { vi: 'Tiếng Việt', en: 'English' };
/** Cookie thường, SameSite=Lax, 1 năm (LUU_TRU_DU_LIEU 6.2). */
export const LANG_COOKIE = 'lang';

export const isLocale = (v: unknown): v is Locale => v === 'vi' || v === 'en';
export const toLocale = (v: unknown): Locale => (isLocale(v) ? v : DEFAULT_LOCALE);

/** Thẻ BCP 47 cho Intl. */
export const intlTag = (l: Locale): string => (l === 'en' ? 'en-US' : 'vi-VN');

const initial = (): Locale =>
  typeof document !== 'undefined' ? toLocale(document.documentElement.lang) : DEFAULT_LOCALE;

export const $locale = atom<Locale>(initial());

let serverGetter: (() => Locale | undefined) | null = null;

/** Middleware gọi một lần để SSR biết ngôn ngữ của request hiện tại. */
export function registerServerLocale(getter: () => Locale | undefined): void {
  serverGetter = getter;
}

/** Ngôn ngữ hiện hành: theo request khi SSR, theo $locale khi chạy trong trình duyệt. */
export function currentLocale(): Locale {
  if (typeof window === 'undefined' && serverGetter) return serverGetter() ?? DEFAULT_LOCALE;
  return $locale.get();
}

/** Đổi ngôn ngữ: ghi cookie `lang` rồi tải lại để SSR render đúng (plan G07.12). */
export function switchLocale(next: Locale, reload = true): void {
  if (typeof document === 'undefined') return;
  document.cookie = `${LANG_COOKIE}=${next}; path=/; max-age=31536000; SameSite=Lax`;
  $locale.set(next);
  if (reload) window.location.reload();
}
