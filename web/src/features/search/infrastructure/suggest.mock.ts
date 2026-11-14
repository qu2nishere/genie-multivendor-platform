// Gợi ý mẫu (prototype: TRENDING + lọc danh mục / shop / sản phẩm theo chữ gõ) — từ bộ seed chung.
import { SEED_PRODUCTS, SEED_ROOT_CATEGORIES, SEED_SHOPS } from '@/shared/api/mock/seed';
import { productImg } from '@/shared/lib/assets';
import type { SuggestionDto } from './suggest.dto';

/** Từ khóa nhiều người tìm 24 giờ qua (search_keyword_stats). */
export const TRENDING_KEYWORDS = [
  'tai nghe chống ồn',
  'serum vitamin c',
  'giày sneaker',
  'bàn phím cơ',
  'áo khoác denim',
];

export function mockSuggest(q: string): SuggestionDto[] {
  const t = q.trim().toLowerCase();
  const hit = (s: string) => s.toLowerCase().includes(t);
  return [
    ...SEED_ROOT_CATEGORIES.filter((c) => hit(c.name))
      .slice(0, 2)
      .map((c) => ({
        kind: 'category' as const,
        id: c.id,
        label: c.name,
        slug: c.slug,
        icon: c.icon,
        image: null,
      })),
    ...SEED_SHOPS.filter((s) => hit(s.name))
      .slice(0, 2)
      .map((s) => ({
        kind: 'shop' as const,
        id: s.id,
        label: s.name,
        slug: s.slug,
        icon: 'store',
        image: null,
      })),
    ...SEED_PRODUCTS.filter((p) => hit(p.title))
      .slice(0, 5)
      .map((p) => ({
        kind: 'product' as const,
        id: p.id,
        label: p.title,
        slug: p.slug,
        icon: null,
        image: productImg(p.image),
      })),
  ];
}
