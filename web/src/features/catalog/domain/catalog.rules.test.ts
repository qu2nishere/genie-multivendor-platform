import { describe, expect, it } from 'vitest';
import { buildTree, categoryHref, categoryPath } from './catalog.rules';
import type { Category } from './catalog.types';

const c = (id: number, name: string, parentId: number | null): Category => ({
  id,
  slug: name,
  name,
  icon: 'zap',
  description: '',
  parentId,
});
const ALL = [c(1, 'dien-tu', null), c(2, 'thoi-trang', null), c(10, 'laptop', 1), c(11, 'giay', 2)];

describe('catalog.rules', () => {
  it('categoryHref', () => expect(categoryHref('dien-tu')).toBe('/c/dien-tu'));
  it('buildTree gom con vào gốc', () => {
    const t = buildTree(ALL);
    expect(t.map((x) => x.slug)).toEqual(['dien-tu', 'thoi-trang']);
    expect(t[0].children.map((x) => x.slug)).toEqual(['laptop']);
  });
  it('categoryPath từ gốc tới lá, không lặp vô hạn', () => {
    expect(categoryPath(ALL, ALL[2]).map((x) => x.slug)).toEqual(['dien-tu', 'laptop']);
    const loop = [c(1, 'a', 2), c(2, 'b', 1)];
    expect(categoryPath(loop, loop[0]).length).toBeLessThanOrEqual(2);
  });
});
