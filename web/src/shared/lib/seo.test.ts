import { describe, expect, it } from 'vitest';
import {
  breadcrumbLd,
  buildMeta,
  clampDescription,
  formatTitle,
  productLd,
  serializeJsonLd,
  websiteLd,
} from './seo';

const SITE = 'https://genie.vn';

describe('seo', () => {
  it('mẫu tiêu đề %s | Genie', () => {
    expect(formatTitle('Tai nghe ANC')).toBe('Tai nghe ANC | Genie');
    expect(formatTitle('Genie')).toBe('Genie');
    expect(formatTitle('Giỏ | Genie')).toBe('Giỏ | Genie');
  });

  it('buildMeta: canonical tuyệt đối, robots, Open Graph', () => {
    const m = buildMeta(
      { title: 'Điện thoại', description: 'Mô tả', canonical: '/c/dien-thoai' },
      SITE,
    );
    expect(m.canonical).toBe('https://genie.vn/c/dien-thoai');
    expect(m.robots).toContain('index, follow');
    expect(m.og['og:url']).toBe(m.canonical);
    expect(m.og['og:locale']).toBe('vi_VN');
    expect(m.og['og:image'].startsWith('https://genie.vn/')).toBe(true);
    expect(buildMeta({ title: 'Tìm', noindex: true, follow: true }, SITE).robots).toBe(
      'noindex, follow',
    );
  });

  it('cắt mô tả ≤ 160 ký tự', () => {
    const d = clampDescription('a '.repeat(200)) ?? '';
    expect(d.length).toBeLessThanOrEqual(160);
    expect(d.endsWith('…')).toBe(true);
  });

  it('JSON-LD Product có offers VND và tình trạng hàng', () => {
    const ld = productLd(
      {
        name: 'Tai nghe',
        url: '/p/tai-nghe-p1',
        images: ['/img/products/headphones.jpg'],
        price: 389000,
        inStock: false,
        rating: { value: 4.8, count: 120 },
      },
      SITE,
    );
    const offers = ld.offers as Record<string, string>;
    expect(offers.priceCurrency).toBe('VND');
    expect(offers.price).toBe('389000');
    expect(offers.availability).toBe('https://schema.org/OutOfStock');
    expect(ld.aggregateRating).toBeDefined();
  });

  it('BreadcrumbList, WebSite SearchAction', () => {
    const b = breadcrumbLd(
      [
        { name: 'Genie', url: '/' },
        { name: 'Điện thoại', url: '/c/dien-thoai' },
      ],
      SITE,
    );
    expect((b.itemListElement as { position: number }[])[1].position).toBe(2);
    expect(JSON.stringify(websiteLd(SITE))).toContain('/search?q={search_term_string}');
  });

  it('serializeJsonLd chặn thoát khỏi thẻ script', () => {
    expect(serializeJsonLd({ name: '</script><b>' })).not.toContain('</script>');
  });
});
