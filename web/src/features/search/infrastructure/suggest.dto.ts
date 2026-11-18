// GET /public/search/suggest?q= và /public/search/trending
export interface SuggestionDto {
  kind: 'category' | 'shop' | 'product';
  id: number;
  label: string;
  slug: string;
  icon: string | null;
  image: string | null;
}

// ===== API thật (API_SPEC 5.4, 5.4b — plan 2026-11-17 C07.01) =====

/** `GET /public/search/suggestions?q=` — tối đa 2 danh mục, 2 shop, 5 sản phẩm. */
export interface SuggestionsApiDto {
  categories: { id: number; name: string; slug: string; icon: string | null }[];
  shops: { id: number; name: string }[];
  products: { id: number; name: string; image_url: string | null }[];
}

/** `GET /public/search/trending?limit=` */
export interface TrendingApiDto {
  keyword: string;
}
