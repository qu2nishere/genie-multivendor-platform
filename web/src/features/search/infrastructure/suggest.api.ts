// Gợi ý ô tìm kiếm — mock khi `isMocked('search')`; thật: /public/search/suggestions, /public/search/trending
// (API_SPEC 5.4, 5.4b; plan 2026-11-17 C07.01).
import { api } from '@/shared/api/http';
import { mockDelay } from '@/shared/api/mock';
import { isMocked } from '@/shared/config/env';
import type { Suggestion } from '../domain/suggest.types';
import type { SuggestionDto, SuggestionsApiDto, TrendingApiDto } from './suggest.dto';
import { fromSuggestionsDto, fromTrendingDto, trendingSuggestion } from './suggest.mapper';
import { mockSuggest, TRENDING_KEYWORDS } from './suggest.mock';

const USE_MOCK = isMocked('search');
const TRENDING_LIMIT = 5;

/** Dòng mock (định dạng prototype) → dòng dropdown. */
const fromMock = (d: SuggestionDto): Suggestion => ({
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
    : fromTrendingDto(
        await api.get<TrendingApiDto[]>('/public/search/trending', {
          query: { limit: TRENDING_LIMIT },
          auth: false,
        }),
      );
  return words.map(trendingSuggestion);
}

/** `signal` do TanStack Query truyền — gõ tiếp thì request cũ bị hủy. */
export async function fetchSuggestions(q: string, signal?: AbortSignal): Promise<Suggestion[]> {
  if (USE_MOCK) {
    await mockDelay(80);
    return mockSuggest(q).map(fromMock);
  }
  return fromSuggestionsDto(
    await api.get<SuggestionsApiDto>('/public/search/suggestions', {
      query: { q },
      auth: false,
      signal,
    }),
  );
}
