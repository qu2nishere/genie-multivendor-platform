// Gợi ý ô tìm kiếm header (UC-CAT-04; prototype Header.jsx TRENDING + gợi ý theo chữ gõ).
export type SuggestionKind = 'trending' | 'category' | 'shop' | 'product';

export interface Suggestion {
  key: string;
  kind: SuggestionKind;
  label: string;
  /** Đích: /search?q= (thịnh hành), /c/{slug}, /shop/{id}, /p/{slug}-{id}. */
  href: string;
  icon?: string;
  image?: string;
}

/** Gõ tối thiểu 2 ký tự mới gợi ý (prototype). */
export const SUGGEST_MIN_CHARS = 2;
export const SUGGEST_DEBOUNCE_MS = 250;

export const searchHref = (q: string): string => `/search?q=${encodeURIComponent(q.trim())}`;
