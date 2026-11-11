// Tiện ích meta, Open Graph, JSON-LD cho trang storefront SSR (plan G04.12; TS 5.4, R14).
// Hàm thuần: nhận dữ liệu đã có, trả object để SeoHead.astro render. Không gọi API.

export const SITE_NAME = 'Genie';
export const TITLE_TEMPLATE = '%s | Genie';
export const DEFAULT_OG_IMAGE = '/img/banners/home-trust.jpg';

export type JsonLd = Record<string, unknown>;

export interface MetaInput {
  title: string;
  description?: string;
  /** Đường dẫn hoặc URL tuyệt đối của trang chuẩn. */
  canonical?: string;
  image?: string;
  imageAlt?: string;
  noindex?: boolean;
  /** Không index nhưng vẫn theo liên kết (trang /search). */
  follow?: boolean;
  type?: 'website' | 'product' | 'article';
  locale?: 'vi' | 'en';
  jsonLd?: JsonLd[];
}

export interface Meta {
  title: string;
  description?: string;
  canonical?: string;
  robots: string;
  og: Record<string, string>;
  twitter: Record<string, string>;
  jsonLd: JsonLd[];
}

/** Ghép URL tuyệt đối từ gốc site (PUBLIC_WEB_URL). */
export function absoluteUrl(pathOrUrl: string, site: string): string {
  try {
    return new URL(pathOrUrl, site).toString();
  } catch {
    return pathOrUrl;
  }
}

/** "Áo khoác" → "Áo khoác | Genie"; tiêu đề đã có "Genie" ở cuối thì giữ nguyên. */
export function formatTitle(title: string): string {
  const t = title.trim();
  if (!t || t === SITE_NAME) return SITE_NAME;
  return / \| Genie$/.test(t) ? t : TITLE_TEMPLATE.replace('%s', t);
}

/** Mô tả meta tối đa ~160 ký tự, cắt ở ranh giới từ. */
export function clampDescription(text: string | undefined, max = 160): string | undefined {
  if (!text) return undefined;
  const s = text.replace(/\s+/g, ' ').trim();
  if (s.length <= max) return s;
  const cut = s.slice(0, max - 1);
  return cut.slice(0, Math.max(cut.lastIndexOf(' '), max - 20)).trimEnd() + '…';
}

export function buildMeta(input: MetaInput, site: string): Meta {
  const title = formatTitle(input.title);
  const description = clampDescription(input.description);
  const canonical = input.canonical ? absoluteUrl(input.canonical, site) : undefined;
  const image = absoluteUrl(input.image ?? DEFAULT_OG_IMAGE, site);
  const robots = input.noindex
    ? `noindex, ${input.follow ? 'follow' : 'nofollow'}`
    : 'index, follow, max-image-preview:large';

  const og: Record<string, string> = {
    'og:site_name': SITE_NAME,
    'og:type': input.type === 'product' ? 'product' : (input.type ?? 'website'),
    'og:title': title,
    'og:image': image,
    'og:locale': input.locale === 'en' ? 'en_US' : 'vi_VN',
  };
  if (description) og['og:description'] = description;
  if (canonical) og['og:url'] = canonical;
  if (input.imageAlt) og['og:image:alt'] = input.imageAlt;

  const twitter: Record<string, string> = {
    'twitter:card': 'summary_large_image',
    'twitter:title': title,
    'twitter:image': image,
  };
  if (description) twitter['twitter:description'] = description;

  return { title, description, canonical, robots, og, twitter, jsonLd: input.jsonLd ?? [] };
}

/** Chuỗi JSON an toàn để nhúng trong <script type="application/ld+json"> (chặn </script>). */
export function serializeJsonLd(data: JsonLd): string {
  return JSON.stringify(data)
    .replace(/</g, '\\u003c')
    .replace(/>/g, '\\u003e')
    .replace(/&/g, '\\u0026');
}

// ---------------------------------------------------------------- JSON-LD

export interface Crumb {
  name: string;
  url: string;
}

export function breadcrumbLd(items: Crumb[], site: string): JsonLd {
  return {
    '@context': 'https://schema.org',
    '@type': 'BreadcrumbList',
    itemListElement: items.map((c, i) => ({
      '@type': 'ListItem',
      position: i + 1,
      name: c.name,
      item: absoluteUrl(c.url, site),
    })),
  };
}

export interface ProductLdInput {
  name: string;
  url: string;
  images: string[];
  description?: string;
  sku?: string;
  brand?: string;
  /** Giá VND số nguyên. */
  price: number;
  inStock: boolean;
  preorder?: boolean;
  sellerName?: string;
  rating?: { value: number; count: number };
  reviews?: { author: string; rating: number; body: string; date: string }[];
}

export function productLd(p: ProductLdInput, site: string): JsonLd {
  const availability = p.preorder
    ? 'https://schema.org/PreOrder'
    : p.inStock
      ? 'https://schema.org/InStock'
      : 'https://schema.org/OutOfStock';
  const ld: JsonLd = {
    '@context': 'https://schema.org',
    '@type': 'Product',
    name: p.name,
    image: p.images.map((i) => absoluteUrl(i, site)),
    url: absoluteUrl(p.url, site),
    offers: {
      '@type': 'Offer',
      url: absoluteUrl(p.url, site),
      priceCurrency: 'VND',
      price: String(Math.round(p.price)),
      availability,
      itemCondition: 'https://schema.org/NewCondition',
      ...(p.sellerName ? { seller: { '@type': 'Organization', name: p.sellerName } } : {}),
    },
  };
  if (p.description) ld.description = clampDescription(p.description, 500);
  if (p.sku) ld.sku = p.sku;
  if (p.brand) ld.brand = { '@type': 'Brand', name: p.brand };
  if (p.rating && p.rating.count > 0) {
    ld.aggregateRating = {
      '@type': 'AggregateRating',
      ratingValue: p.rating.value,
      reviewCount: p.rating.count,
      bestRating: 5,
      worstRating: 1,
    };
  }
  if (p.reviews?.length) {
    ld.review = p.reviews.map((r) => ({
      '@type': 'Review',
      author: { '@type': 'Person', name: r.author },
      datePublished: r.date,
      reviewBody: r.body,
      reviewRating: { '@type': 'Rating', ratingValue: r.rating, bestRating: 5, worstRating: 1 },
    }));
  }
  return ld;
}

export function organizationLd(site: string): JsonLd {
  return {
    '@context': 'https://schema.org',
    '@type': 'Organization',
    name: SITE_NAME,
    url: absoluteUrl('/', site),
    logo: absoluteUrl('/favicon.svg', site),
  };
}

export function websiteLd(site: string): JsonLd {
  return {
    '@context': 'https://schema.org',
    '@type': 'WebSite',
    name: SITE_NAME,
    url: absoluteUrl('/', site),
    potentialAction: {
      '@type': 'SearchAction',
      target: {
        '@type': 'EntryPoint',
        urlTemplate: absoluteUrl('/search?q={search_term_string}', site),
      },
      'query-input': 'required name=search_term_string',
    },
  };
}

export function itemListLd(items: { name: string; url: string }[], site: string): JsonLd {
  return {
    '@context': 'https://schema.org',
    '@type': 'ItemList',
    itemListElement: items.map((it, i) => ({
      '@type': 'ListItem',
      position: i + 1,
      url: absoluteUrl(it.url, site),
      name: it.name,
    })),
  };
}
