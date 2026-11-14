// SEO storefront khi TẮT JavaScript (plan G08.54; R14–R19): nội dung SSR đủ cho bot đọc —
// tên + giá sản phẩm, liên kết danh mục bấm được, title / canonical đúng, JSON-LD hợp lệ,
// một <h1> mỗi trang, mã trạng thái 404 / 301.
import { expect, test, type Page } from '@playwright/test';

test.use({ javaScriptEnabled: false });

const PRODUCT = '/p/tai-nghe-bluetooth-chong-on-anc-pro-5-pin-40-gio-1';

async function jsonLd(page: Page): Promise<Record<string, unknown>[]> {
  const raw = await page.locator('script[type="application/ld+json"]').allTextContents();
  return raw.map((r) => JSON.parse(r) as Record<string, unknown>);
}

const canonical = (page: Page) => page.locator('link[rel="canonical"]').getAttribute('href');

test.describe('SEO không JavaScript', () => {
  test('trang chủ: một h1, JSON-LD Organization + WebSite có SearchAction', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('h1')).toHaveCount(1);
    const ld = await jsonLd(page);
    expect(ld.map((x) => x['@type'])).toEqual(expect.arrayContaining(['Organization', 'WebSite']));
    const site = ld.find((x) => x['@type'] === 'WebSite') as {
      potentialAction?: { target?: unknown };
    };
    expect(JSON.stringify(site.potentialAction)).toContain('/search?q=');
  });

  test('trang sản phẩm: tên, giá, title, canonical, JSON-LD Product + BreadcrumbList', async ({
    page,
  }) => {
    const res = await page.goto(PRODUCT);
    expect(res?.status()).toBe(200);
    await expect(page.locator('h1')).toHaveCount(1);
    await expect(page.locator('h1')).toContainText('Tai nghe Bluetooth chống ồn ANC Pro 5');
    await expect(page.getByText('389.000').first()).toBeVisible();
    await expect(page).toHaveTitle(/Tai nghe Bluetooth chống ồn ANC Pro 5.*\| Genie/);
    expect(await canonical(page)).toMatch(new RegExp(`${PRODUCT}$`));
    const ld = await jsonLd(page);
    const product = ld.find((x) => x['@type'] === 'Product') as {
      name: string;
      offers: { price: string; priceCurrency: string };
      aggregateRating?: unknown;
    };
    expect(product.name).toContain('ANC Pro 5');
    expect(product.offers.priceCurrency).toBe('VND');
    expect(product.offers.price).toBe('389000');
    expect(product.aggregateRating).toBeTruthy();
    expect(ld.some((x) => x['@type'] === 'BreadcrumbList')).toBe(true);
  });

  test('slug sai → 301 về URL chuẩn; sản phẩm không tồn tại → 404', async ({ request }) => {
    const moved = await request.get('/p/sai-slug-1', { maxRedirects: 0 });
    expect(moved.status()).toBe(301);
    expect(moved.headers()['location']).toContain(PRODUCT);
    expect((await request.get('/p/khong-co-99999')).status()).toBe(404);
    expect((await request.get('/c/khong-co')).status()).toBe(404);
    expect((await request.get('/shop/99999')).status()).toBe(404);
    expect((await request.get('/policies/khong-co')).status()).toBe(404);
  });

  test('danh mục: liên kết sản phẩm bấm được, canonical bỏ tham số lọc, phân trang là liên kết thật', async ({
    page,
  }) => {
    await page.goto('/c/dien-tu?brand=Sony&utm_source=x');
    expect(await canonical(page)).toMatch(/\/c\/dien-tu$/);
    await expect(page.locator('h1')).toHaveCount(1);
    const first = page.locator('ul.tr-grid a[href^="/p/"]').first();
    await expect(first).toBeVisible();
    await Promise.all([page.waitForURL(/\/p\//), first.click()]);
    await expect(page.locator('h1')).toHaveCount(1);
  });

  test('khám phá: ?page=2 có canonical riêng và liên kết trang kế', async ({ page }) => {
    await page.goto('/discover?page=2');
    expect(await canonical(page)).toMatch(/\/discover\?page=2$/);
    await expect(page.locator('a[href="/discover?page=3"]').first()).toBeVisible();
  });

  test('tìm kiếm: noindex, follow', async ({ page }) => {
    await page.goto('/search?q=tai');
    await expect(page.locator('meta[name="robots"]')).toHaveAttribute(
      'content',
      /noindex,\s*follow/,
    );
    await expect(page.locator('h1')).toHaveCount(1);
  });

  test('mỗi trang công khai có đúng một h1', async ({ page }) => {
    for (const path of [
      '/shop/1',
      '/flash-sale',
      '/campaign/sieu-sale-12-12',
      '/policies/help-center',
    ]) {
      await page.goto(path);
      await expect(page.locator('h1'), path).toHaveCount(1);
    }
  });

  test('trang cần đăng nhập, /auth/*, /seller/*, /admin/*, /dev/*: noindex, nofollow', async ({
    page,
    context,
    baseURL,
  }) => {
    for (const path of [
      '/auth/login',
      '/auth/register',
      '/auth/forgot',
      '/cart',
      '/checkout/result',
      '/dev/ui',
    ]) {
      await page.goto(path);
      await expect(page.locator('meta[name="robots"]'), path).toHaveAttribute(
        'content',
        /noindex,\s*nofollow/,
      );
    }
    await context.addCookies([
      { name: 'genie_logged_in', value: '1', url: baseURL ?? 'http://localhost:4321' },
    ]);
    for (const path of ['/seller', '/admin', '/orders', '/account/profile', '/chat', '/checkout']) {
      await page.goto(path);
      await expect(page.locator('meta[name="robots"]'), path).toHaveAttribute(
        'content',
        /noindex,\s*nofollow/,
      );
    }
  });

  test('robots.txt và sitemap.xml', async ({ request }) => {
    const robots = await (await request.get('/robots.txt')).text();
    expect(robots).toContain('Disallow: /seller');
    expect(robots).toMatch(/Sitemap: https?:\/\/.+\/sitemap\.xml/);
    const sitemap = await (await request.get('/sitemap.xml')).text();
    expect(sitemap).toContain(`${PRODUCT}</loc>`);
    expect(sitemap).toContain('/c/dien-tu</loc>');
    expect(sitemap).toContain('/policies/help-center</loc>');
  });
});
