// Sản phẩm công khai: mock khi USE_MOCK; sau này /api/v1/public/products (chữ ký giữ nguyên).
// Gọi từ frontmatter .astro (SSR) và từ island (tải thêm, sản phẩm tương tự).
import { request } from '@/shared/api/http';
import { mockDelay, mockFail, mockOk, paginatePage, USE_MOCK } from '@/shared/api/mock';
import { env } from '@/shared/config/env';
import type { Page } from '@/shared/api/types';
import { isPagePagination } from '@/shared/api/types';
import type { Product, ProductQuery, ProductSummary } from '../domain/product.types';
import type { ProductDetailDto, ProductSummaryDto } from './product.dto';
import { toProduct, toProductSummary } from './product.mapper';
import { findSeedProduct, queryProducts, toDetailDto, toSummaryDto } from './product.mock';

const isServer = () => typeof window === 'undefined';

/** SSR gọi INTERNAL_API_URL (không token), trình duyệt gọi PUBLIC_API_URL. */
function get<T>(path: string, query?: Record<string, string | number | boolean | undefined>) {
  return request<T>(path, {
    query,
    auth: false,
    baseUrl: isServer() ? (env.INTERNAL_API_URL ?? env.PUBLIC_API_URL) : env.PUBLIC_API_URL,
  });
}

const toQuery = (q: ProductQuery) => ({
  q: q.q,
  category_id: q.categoryId,
  subcategory_id: q.subcategoryId,
  shop_id: q.shopId,
  min_price: q.minPrice,
  max_price: q.maxPrice,
  brand: q.brand,
  authentic: q.authentic,
  freeship: q.freeship,
  min_rating: q.minRating,
  attribute: q.attribute,
  sort: q.sort,
  page: q.page ?? 1,
  size: q.size ?? 20,
});

/** Danh sách có phân trang số trang (danh mục, tìm kiếm, khám phá, shop). */
export async function listProducts(q: ProductQuery = {}): Promise<Page<ProductSummary>> {
  if (USE_MOCK) {
    await mockDelay();
    const page = paginatePage(queryProducts(q), { page: q.page, size: q.size });
    return { ...page, items: page.items.map((p) => toProductSummary(toSummaryDto(p))) };
  }
  const res = await get<ProductSummaryDto[]>('/public/products', toQuery(q));
  const pg = isPagePagination(res.pagination)
    ? res.pagination
    : { page: 1, size: res.data.length, total_items: res.data.length, total_pages: 1 };
  return { items: res.data.map(toProductSummary), ...pg };
}

/** Chi tiết; không có / đã gỡ → ApiError GNE-CAT-4042 (trang trả 404). */
export async function getProduct(id: number): Promise<Product> {
  if (USE_MOCK) {
    await mockDelay();
    const p = findSeedProduct(id);
    if (!p) mockFail('GNE-CAT-4042');
    return toProduct(mockOk(toDetailDto(p)).data);
  }
  return toProduct((await get<ProductDetailDto>(`/public/products/${id}`)).data);
}

/** Sản phẩm tương tự: cùng danh mục, bỏ chính nó (prototype: tối đa 6). */
export async function listRelated(id: number, limit = 6): Promise<ProductSummary[]> {
  if (USE_MOCK) {
    await mockDelay();
    const p = findSeedProduct(id);
    if (!p) return [];
    return queryProducts({ categoryId: p.category_id })
      .filter((x) => x.id !== id)
      .slice(0, limit)
      .map((x) => toProductSummary(toSummaryDto(x)));
  }
  return (await get<ProductSummaryDto[]>(`/public/products/${id}/related`, { limit })).data.map(
    toProductSummary,
  );
}

/** Nhiều sản phẩm theo id (yêu thích, gợi ý trang chủ). */
export async function listProductsByIds(ids: number[]): Promise<ProductSummary[]> {
  if (USE_MOCK) {
    await mockDelay();
    return ids
      .map((id) => findSeedProduct(id))
      .filter((p): p is NonNullable<typeof p> => !!p)
      .map((p) => toProductSummary(toSummaryDto(p)));
  }
  return (await get<ProductSummaryDto[]>('/public/products', { ids: ids.join(',') })).data.map(
    toProductSummary,
  );
}
