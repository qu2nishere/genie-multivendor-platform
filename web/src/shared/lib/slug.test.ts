// Slug sản phẩm phải khớp backend (SlugUtilTest) — cùng bộ mẫu docs/api-tests/slug-samples.json (plan 2026-11-17 C07.07).
import { readFileSync } from 'node:fs';
import { describe, expect, it } from 'vitest';
import { PRODUCT_SLUG_MAX, productSlug, splitSlugId } from './slug';

const samples = JSON.parse(
  readFileSync(new URL('../../../../docs/api-tests/slug-samples.json', import.meta.url), 'utf8'),
) as { name: string; slug: string }[];

describe('productSlug', () => {
  it('matches the shared samples used by the backend', () => {
    expect(samples.length).toBeGreaterThanOrEqual(10);
    for (const s of samples) expect(productSlug(s.name), s.name).toBe(s.slug);
  });

  it('never exceeds the max length or ends with a dash', () => {
    const slug = productSlug('a '.repeat(200));
    expect(slug.length).toBeLessThanOrEqual(PRODUCT_SLUG_MAX);
    expect(slug.endsWith('-')).toBe(false);
  });

  it('round-trips with splitSlugId', () => {
    expect(splitSlugId(`${productSlug('Tai nghe ANC Pro 5')}-12`)).toEqual({
      slug: 'tai-nghe-anc-pro-5',
      id: 12,
    });
  });
});
