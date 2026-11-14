// "Gợi ý cho bạn" trên trang chủ (prototype Home.jsx picks) — plan G08.13, island client:visible.
// SSR trang đầu (R17); cuộn tới cuối lưới thì tải thêm (tối đa vài trang), sau đó là liên kết thật
// sang /discover?page=N (R16 — tắt JS vẫn xem tiếp được).
import { useEffect, useRef } from 'react';
import type { Page } from '@/shared/api/types';
import { useT } from '@/shared/hooks/useT';
import { buttonClass, Spinner } from '@/shared/ui';
import { Providers } from '@/shared/providers/Providers';
import { useFeed } from '../application/useFeed';
import type { ProductSummary } from '../domain/product.types';
import { ProductGridBody } from './ProductGrid';

export interface ProductFeedProps {
  initial: Page<ProductSummary>;
  size: number;
  offset?: number;
  /** Số trang tối đa tải bằng cuộn trước khi chuyển sang trang Khám phá. */
  maxPages?: number;
}

function Feed({ initial, size, offset, maxPages = 3 }: ProductFeedProps) {
  const { t } = useT();
  const q = useFeed(initial, { size, offset, maxPages });
  const items = q.data?.pages.flatMap((p) => p.items) ?? initial.items;
  const last = q.data?.pages[q.data.pages.length - 1] ?? initial;
  const sentinel = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const el = sentinel.current;
    if (!el || !q.hasNextPage || typeof IntersectionObserver === 'undefined') return undefined;
    const io = new IntersectionObserver(
      (entries) => {
        if (entries.some((e) => e.isIntersecting) && !q.isFetchingNextPage) void q.fetchNextPage();
      },
      { rootMargin: '400px 0px' },
    );
    io.observe(el);
    return () => io.disconnect();
  }, [q, q.hasNextPage, q.isFetchingNextPage]);

  return (
    <>
      <ProductGridBody items={items} priorityCount={0} aria-label={t('home.feed.title')} />
      <div ref={sentinel} aria-hidden="true" />
      {q.isFetchingNextPage && (
        <div className="flex justify-center py-6 text-muted">
          <Spinner label={t('common.state.loading')} size={24} />
        </div>
      )}
      {!q.hasNextPage && (
        <div className="flex justify-center pt-10">
          <a
            href={`/discover?page=${last.page + 1}`}
            className={buttonClass({ variant: 'outline', size: 'lg' })}
          >
            {t('home.feed.more')}
          </a>
        </div>
      )}
    </>
  );
}

export default function ProductFeed(props: ProductFeedProps) {
  return (
    <Providers>
      <Feed {...props} />
    </Providers>
  );
}
