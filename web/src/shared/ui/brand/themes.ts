// 9 theme của Genie (prototype ui_kits/storefront/themes.js) — quyết định D1.
// Tên / mô tả hiển thị qua i18n `theme.<id>.name|note`; ô màu dùng để xem trước trong ThemePicker.

export const THEMES = [
  { id: 'genie', swatch: ['#0F6E78', '#E2B04A', '#2BB3B1', '#F1F6F5'] },
  { id: 'hoian', swatch: ['#E8B54A', '#2C6E68', '#B8326E', '#3A2312'] },
  { id: 'dongho', swatch: ['#B5381F', '#E9B23C', '#1E1814', '#F3EDE1'] },
  { id: 'dongson', swatch: ['#8E5A17', '#E0A93B', '#2B6B5E', '#F5F1E8'] },
  { id: 'polymer', swatch: ['#1F7356', '#E26A93', '#E3C15A', '#EEF4F1'] },
  { id: 'buudien', swatch: ['#F5BE16', '#22335C', '#D9472B', '#EDEEE8'] },
  { id: 'saigon', swatch: ['#0F6E45', '#FFD024', '#D52B1E', '#FAF9F4'] },
  { id: 'sleeve', swatch: ['#5B3FD1', '#C8F03C', '#111216', '#FFFFFF'] },
  { id: 'classic', swatch: ['#1D4ED8', '#FFC02E', '#0B1F4B', '#F7F5F0'] },
] as const;

export type ThemeId = (typeof THEMES)[number]['id'];
export const THEME_IDS = THEMES.map((t) => t.id) as ThemeId[];
export const DEFAULT_THEME: ThemeId = 'genie';
/** Khóa localStorage (danh mục LUU_TRU_DU_LIEU 6.2: genie.theme). Giá trị lưu dạng JSON string. */
export const THEME_STORAGE_KEY = 'genie.theme';

export const isThemeId = (v: unknown): v is ThemeId => (THEME_IDS as unknown[]).includes(v);
