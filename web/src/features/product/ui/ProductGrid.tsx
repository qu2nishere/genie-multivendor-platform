// Lưới thẻ sản phẩm (prototype .tr-grid + CardX). Island: SSR đủ thẻ + liên kết (R17), hydrate để
// tim yêu thích hoạt động. Danh sách là <ul> (R18).
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { ProductCard } from '@/shared/ui';
import { Providers } from '@/shared/providers/Providers';
import type { ProductSummary } from '../domain/product.types';
import { toCardData } from './toCard';
import { WishHeart } from './WishHeart';

export interface ProductGridProps {
  items: ProductSummary[];
  flashBar?: boolean;
  /** Số thẻ đầu tải ảnh sớm (màn hình đầu). */
  priorityCount?: number;
  className?: string;
  'aria-label'?: string;
}

export function ProductGridBody({
  items,
  flashBar,
  priorityCount = 0,
  className,
  'aria-label': label,
}: ProductGridProps) {
  const { t } = useT();
  const labels = { vacation: t('product.card.vacation'), preorder: t('common.state.preorder') };
  return (
    <ul className={cn('tr-grid list-none m-0 p-0', className)} aria-label={label}>
      {items.map((p, i) => (
        <li key={`${p.id}-${i}`} className="min-w-0">
          <ProductCard
            product={toCardData(p, labels)}
            flashBar={flashBar}
            priority={i < priorityCount}
            heart={<WishHeart productId={p.id} />}
          />
        </li>
      ))}
    </ul>
  );
}

export default function ProductGrid(props: ProductGridProps) {
  return (
    <Providers>
      <ProductGridBody {...props} />
    </Providers>
  );
}
