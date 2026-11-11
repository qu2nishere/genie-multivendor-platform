// Hàm t(key) dùng chung Astro / React (plan G04.07; TS 3.4, 5.4, 11).
// Khóa: <feature>.<màn>.<ý nghĩa>; tra khóa lồng nhau; nội suy {name}; thiếu khóa → rơi về vi
// (cảnh báo ở DEV); thiếu cả vi → trả lại chính khóa để dễ phát hiện trên giao diện.
import en from '@/i18n/en.json';
import vi from '@/i18n/vi.json';
import { currentLocale, DEFAULT_LOCALE, type Locale } from './locale';

export type Messages = { [key: string]: string | Messages };
export type TParams = Record<string, string | number>;
export type TFunction = (key: string, params?: TParams) => string;

const DICTS: Record<Locale, Messages> = { vi: vi as Messages, en: en as Messages };

const warned = new Set<string>();
const isDev = (): boolean => {
  try {
    return Boolean(import.meta.env?.DEV);
  } catch {
    return false;
  }
};

/** Tra khóa dạng "a.b.c" trong từ điển lồng nhau; chỉ nhận giá trị là chuỗi. */
export function lookup(dict: Messages, key: string): string | undefined {
  let node: string | Messages | undefined = dict;
  for (const part of key.split('.')) {
    if (node === undefined || typeof node === 'string') return undefined;
    node = node[part];
  }
  return typeof node === 'string' ? node : undefined;
}

/** Thay {name} bằng params.name; tham số không có thì giữ nguyên {name}. */
export function interpolate(template: string, params?: TParams): string {
  if (!params) return template;
  return template.replace(/\{(\w+)\}/g, (whole, name: string) =>
    Object.prototype.hasOwnProperty.call(params, name) ? String(params[name]) : whole,
  );
}

export function translate(
  dicts: Record<Locale, Messages>,
  locale: Locale,
  key: string,
  params?: TParams,
): string {
  const hit = lookup(dicts[locale], key);
  if (hit !== undefined) return interpolate(hit, params);
  const fallback = lookup(dicts[DEFAULT_LOCALE], key);
  if (isDev() && !warned.has(locale + key)) {
    warned.add(locale + key);
    console.warn(`[i18n] thiếu khóa "${key}" cho "${locale}"`);
  }
  return interpolate(fallback ?? key, params);
}

/** t(key, params?, locale?) — locale bỏ trống: theo request khi SSR, theo $locale trên trình duyệt. */
export function t(key: string, params?: TParams, locale?: Locale): string {
  return translate(DICTS, locale ?? currentLocale(), key, params);
}

/** Dùng trong frontmatter .astro: const t = createT(Astro.locals.lang). */
export function createT(locale: Locale): TFunction {
  return (key, params) => translate(DICTS, locale, key, params);
}

/** Kiểm khóa có tồn tại (vd. errors.GNE-*) trước khi dùng thông điệp mặc định. */
export function hasKey(key: string, locale: Locale = DEFAULT_LOCALE): boolean {
  return lookup(DICTS[locale], key) !== undefined;
}
