import { describe, expect, it } from 'vitest';
import { bannerState, dealTwo, liveBanners, localDay } from './banner.rules';
import type { Banner } from './banner.types';

const b = (id: string, over: Partial<Banner> = {}): Banner => ({
  id,
  title: id,
  image: '',
  imageMobile: null,
  href: '/',
  position: 'HOME_TOP_CAROUSEL',
  categorySlug: null,
  order: 1,
  startsAt: '2026-01-01',
  endsAt: '2030-12-31',
  on: true,
  ...over,
});

describe('banner.rules', () => {
  it('trạng thái theo ngày', () => {
    const today = '2026-11-11';
    expect(bannerState(b('a'), today)).toBe('LIVE');
    expect(bannerState(b('a', { on: false }), today)).toBe('OFF');
    expect(bannerState(b('a', { endsAt: '2026-02-15' }), today)).toBe('ENDED');
    expect(bannerState(b('a', { startsAt: '2026-12-01' }), today)).toBe('SCHEDULED');
  });

  it('lọc banner đang chạy theo vị trí, danh mục, thứ tự', () => {
    const all = [
      b('x', { order: 2 }),
      b('y', { order: 1 }),
      b('z', { position: 'CATEGORY_TOP', categorySlug: 'lam-dep' }),
      b('w', { endsAt: '2020-01-01' }),
    ];
    expect(liveBanners(all, 'HOME_TOP_CAROUSEL', '2026-11-11').map((x) => x.id)).toEqual([
      'y',
      'x',
    ]);
    expect(liveBanners(all, 'CATEGORY_TOP', '2026-11-11', 'lam-dep').map((x) => x.id)).toEqual([
      'z',
    ]);
    expect(liveBanners(all, 'CATEGORY_TOP', '2026-11-11', 'dien-tu')).toEqual([]);
  });

  it('chia hai ô xen kẽ', () => {
    expect(dealTwo([1, 2, 3, 4, 5])).toEqual([
      [1, 3, 5],
      [2, 4],
    ]);
    expect(dealTwo([1])).toEqual([[1]]);
  });

  it('localDay', () => expect(localDay(new Date(2026, 0, 5))).toBe('2026-01-05'));
});
