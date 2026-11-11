// GET /public/search/suggest?q= và /public/search/trending
export interface SuggestionDto {
  kind: 'category' | 'shop' | 'product';
  id: number;
  label: string;
  slug: string;
  icon: string | null;
  image: string | null;
}
