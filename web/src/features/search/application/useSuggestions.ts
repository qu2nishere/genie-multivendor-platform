import { useQuery } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { SUGGEST_DEBOUNCE_MS, SUGGEST_MIN_CHARS, type Suggestion } from '../domain/suggest.types';
import { fetchSuggestions, fetchTrending } from '../infrastructure/suggest.api';

/** Ô trống → từ khóa thịnh hành; gõ ≥ 2 ký tự → gợi ý (trễ 250 ms); 1 ký tự → không gợi ý. */
export function useSuggestions(q: string): Suggestion[] {
  const [debounced, setDebounced] = useState(q);
  useEffect(() => {
    const h = setTimeout(() => setDebounced(q), SUGGEST_DEBOUNCE_MS);
    return () => clearTimeout(h);
  }, [q]);
  const term = debounced.trim();
  const trending = useQuery({
    queryKey: ['search', 'trending'],
    queryFn: fetchTrending,
    staleTime: 10 * 60_000,
  });
  const suggest = useQuery({
    queryKey: ['search', 'suggest', term.toLowerCase()],
    queryFn: () => fetchSuggestions(term),
    enabled: term.length >= SUGGEST_MIN_CHARS,
    staleTime: 60_000,
  });
  if (!q.trim()) return trending.data ?? [];
  if (term.length < SUGGEST_MIN_CHARS) return [];
  return suggest.data ?? [];
}
