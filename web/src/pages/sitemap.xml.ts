// sitemap.xml cho storefront (plan G08.44; TECH_STACK 5.4): trang chủ, khám phá, Flash Sale, danh mục
// gốc, mọi sản phẩm, shop, chính sách, chiến dịch. Chỉ URL chuẩn (không tham số lọc / sắp xếp).
import type { APIRoute } from 'astro';
import { categoryHref, listRootCategories } from '@/features/catalog';
import { policyHref, allPolicySlugs } from '@/features/policies';
import { listProducts, productHref } from '@/features/product';
import { listCampaignSlugs } from '@/features/promotions';
import { listShops, shopHref } from '@/features/shop';
import { env } from '@/shared/config/env';

const xmlEscape = (s: string) =>
  s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');

async function allProductPaths(): Promise<string[]> {
  const out: string[] = [];
  for (let page = 1; page <= 200; page++) {
    const res = await listProducts({ page, size: 60 });
    out.push(...res.items.map(productHref));
    if (page >= res.total_pages) break;
  }
  return out;
}

export const GET: APIRoute = async ({ site }) => {
  const base = (site?.toString() ?? env.PUBLIC_WEB_URL).replace(/\/$/, '');
  const [roots, products, shops, campaigns] = await Promise.all([
    listRootCategories(),
    allProductPaths(),
    listShops(),
    listCampaignSlugs(),
  ]);
  const paths = [
    '/',
    '/discover',
    '/flash-sale',
    ...roots.map((c) => categoryHref(c.slug)),
    ...products,
    ...shops.map((s) => shopHref(s.id)),
    ...campaigns.map((s) => `/campaign/${s}`),
    ...allPolicySlugs().map(policyHref),
  ];
  const body = `<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
${Array.from(new Set(paths))
  .map((p) => `  <url><loc>${xmlEscape(base + p)}</loc></url>`)
  .join('\n')}
</urlset>
`;
  return new Response(body, {
    headers: {
      'Content-Type': 'application/xml; charset=utf-8',
      'Cache-Control': 'public, max-age=3600',
    },
  });
};
