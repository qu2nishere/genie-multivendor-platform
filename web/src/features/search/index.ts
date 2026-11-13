// API công khai của feature "search" (mở rộng ở G08.17).
export type { Suggestion, SuggestionKind } from './domain/suggest.types';
export { searchHref } from './domain/suggest.types';
export { SearchBox } from './ui/SearchBox';
export {
  parseFilters,
  serializeFilters,
  filtersHref,
  priceRange,
  sortParam,
  nextSort,
  activeChips,
  withoutChip,
  clearedFilters,
  toggleValue,
  EMPTY_FILTERS,
  CATEGORY_SORTS,
  SEARCH_SORTS,
  CATEGORY_BANDS,
  SEARCH_BANDS,
  RATINGS,
  type FilterState,
  type FilterChip,
  type PriceBand,
  type SortId,
  type SortDir,
  type SearchScope,
} from './domain/filters';
export { bandLabel, chipLabel, sortLabel } from './ui/labels';
export { default as FilterPanel, type FilterPanelProps } from './ui/FilterPanel';
export { default as SortBar, type SortBarProps } from './ui/SortBar';
