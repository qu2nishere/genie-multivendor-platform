import { describe, expect, it } from 'vitest';
import { getProduct, listProducts, listRelated } from './product.api';

// Lớp mock phải trả đúng hình dạng như API thật (plan G08.03)
describe('product.api (mock)', () => {
  it('lọc theo danh mục, khoảng giá, Chính hãng và sắp xếp giá', async () => {
    const page = await listProducts({
      categoryId: 1,
      maxPrice: 1_000_000,
      sort: 'price_asc',
      size: 50,
    });
    expect(page.items.length).toBeGreaterThan(0);
    expect(page.items.every((p) => p.categoryId === 1 && p.price <= 1_000_000)).toBe(true);
    const prices = page.items.map((p) => p.price);
    expect(prices).toEqual([...prices].sort((a, b) => a - b));
    const auth = await listProducts({ authentic: true, size: 50 });
    expect(auth.items.every((p) => p.authentic)).toBe(true);
  });

  it('phân trang số trang', async () => {
    const p1 = await listProducts({ size: 6, page: 1 });
    const p4 = await listProducts({ size: 6, page: 4 });
    expect(p1.total_items).toBe(20);
    expect(p1.total_pages).toBe(4);
    expect(p4.items).toHaveLength(2);
  });

  it('chi tiết có phân loại, ảnh, mô tả; id lạ → GNE-CAT-4042', async () => {
    const p = await getProduct(1);
    expect(p.slug).toBe('tai-nghe-bluetooth-chong-on-anc-pro-5-pin-40-gio');
    expect(p.flash).toEqual({ sold: 45, total: 50 });
    expect(p.variantGroups).toHaveLength(2);
    expect(p.images.length).toBeGreaterThan(1);
    await expect(getProduct(999)).rejects.toMatchObject({ code: 'GNE-CAT-4042', status: 404 });
  });

  it('sản phẩm tương tự cùng danh mục, không gồm chính nó', async () => {
    const rel = await listRelated(1);
    expect(rel.length).toBeLessThanOrEqual(6);
    expect(rel.some((r) => r.id === 1)).toBe(false);
  });
});
