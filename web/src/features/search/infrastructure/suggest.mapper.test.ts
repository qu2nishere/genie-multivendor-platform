// Mapper gợi ý API thật (plan 2026-11-17 C07.07).
import { describe, expect, it } from 'vitest';
import { fromSuggestionsDto, fromTrendingDto } from './suggest.mapper';

describe('suggest mapper', () => {
  it('maps categories, shops then products with real links', () => {
    const rows = fromSuggestionsDto({
      categories: [
        { id: 13, name: 'Thiết bị âm thanh', slug: 'thiet-bi-am-thanh', icon: 'headphones' },
      ],
      shops: [{ id: 15, name: 'TechZone Official' }],
      products: [{ id: 1, name: 'Tai nghe ANC Pro 5', image_url: null }],
    });
    expect(rows.map((r) => r.kind)).toEqual(['category', 'shop', 'product']);
    expect(rows.map((r) => r.href)).toEqual([
      '/c/thiet-bi-am-thanh',
      '/shop/15',
      '/p/tai-nghe-anc-pro-5-1',
    ]);
    expect(rows[2].image).toBeUndefined();
  });

  it('reads trending keywords', () => {
    expect(fromTrendingDto([{ keyword: 'tai nghe chống ồn' }])).toEqual(['tai nghe chống ồn']);
  });
});
