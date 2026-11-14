import { useInfiniteQuery } from '@tanstack/react-query';
import type { Page } from '@/shared/api/types';
import type { ProductSummary } from '../domain/product.types';
import { listFeed } from '../infrastructure/product.api';

/** Gợi ý hôm nay tải thêm theo trang; trang đầu do SSR đưa vào (initial). */
export function useFeed(
  initial: Page<ProductSummary>,
  opts: { size: number; offset?: number; maxPages: number },
) {
  return useInfiniteQuery({
    queryKey: ['feed', opts.size, opts.offset ?? 0],
    queryFn: ({ pageParam }) => listFeed({ page: pageParam, size: opts.size, offset: opts.offset }),
    initialPageParam: initial.page,
    initialData: { pages: [initial], pageParams: [initial.page] },
    getNextPageParam: (last) =>
      last.page < Math.min(last.total_pages, opts.maxPages) ? last.page + 1 : undefined,
    staleTime: Infinity,
  });
}
