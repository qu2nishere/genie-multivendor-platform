// API gợi ý → dòng dropdown ô tìm kiếm (plan 2026-11-17 C07.01). Thứ tự: danh mục → shop → sản phẩm.
import { productSlug } from '@/shared/lib/slug';
import { searchHref, type Suggestion } from '../domain/suggest.types';
import type { SuggestionsApiDto, TrendingApiDto } from './suggest.dto';

export function fromSuggestionsDto(d: SuggestionsApiDto): Suggestion[] {
  return [
    ...d.categories.map<Suggestion>((c) => ({
      key: `category-${c.id}`,
      kind: 'category',
      label: c.name,
      href: `/c/${c.slug}`,
      icon: c.icon ?? undefined,
    })),
    ...d.shops.map<Suggestion>((s) => ({
      key: `shop-${s.id}`,
      kind: 'shop',
      label: s.name,
      href: `/shop/${s.id}`,
    })),
    ...d.products.map<Suggestion>((p) => ({
      key: `product-${p.id}`,
      kind: 'product',
      label: p.name,
      href: `/p/${productSlug(p.name)}-${p.id}`,
      image: p.image_url ?? undefined,
    })),
  ];
}

export const fromTrendingDto = (rows: TrendingApiDto[]): string[] => rows.map((r) => r.keyword);

export const trendingSuggestion = (k: string): Suggestion => ({
  key: `k-${k}`,
  kind: 'trending',
  label: k,
  href: searchHref(k),
  icon: 'trending-up',
});
