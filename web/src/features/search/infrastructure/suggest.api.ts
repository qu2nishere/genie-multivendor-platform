import { api } from '@/shared/api/http';
import { mockDelay, USE_MOCK } from '@/shared/api/mock';
import { searchHref, type Suggestion } from '../domain/suggest.types';
import type { SuggestionDto } from './suggest.dto';
import { mockSuggest, TRENDING_KEYWORDS } from './suggest.mock';

const toSuggestion = (d: SuggestionDto): Suggestion => ({
  key: `${d.kind}-${d.id}`,
  kind: d.kind,
  label: d.label,
  href:
    d.kind === 'category'
      ? `/c/${d.slug}`
      : d.kind === 'shop'
        ? `/shop/${d.id}`
        : `/p/${d.slug}-${d.id}`,
  icon: d.icon ?? undefined,
  image: d.image ?? undefined,
});

export async function fetchTrending(): Promise<Suggestion[]> {
  const words = USE_MOCK
    ? TRENDING_KEYWORDS
    : await api.get<string[]>('/public/search/trending', { auth: false });
  return words.map((k) => ({
    key: `k-${k}`,
    kind: 'trending',
    label: k,
    href: searchHref(k),
    icon: 'trending-up',
  }));
}

export async function fetchSuggestions(q: string): Promise<Suggestion[]> {
  if (USE_MOCK) {
    await mockDelay(80);
    return mockSuggest(q).map(toSuggestion);
  }
  return (
    await api.get<SuggestionDto[]>('/public/search/suggest', { query: { q }, auth: false })
  ).map(toSuggestion);
}
