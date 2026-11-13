// Sản phẩm công khai: mock khi USE_MOCK; sau này /api/v1/public/products (chữ ký giữ nguyên).
// Gọi từ frontmatter .astro (SSR) và từ island (tải thêm, sản phẩm tương tự).
import { request } from '@/shared/api/http';
import { mockDelay, mockFail, mockOk, paginatePage, USE_MOCK } from '@/shared/api/mock';
import { env } from '@/shared/config/env';
import type { Page } from '@/shared/api/types';
import { isPagePagination } from '@/shared/api/types';
import type { Product, ProductFacets, ProductQuery, ProductSummary } from '../domain/product.types';
import type { ProductDetailDto, ProductSummaryDto } from './product.dto';
import { toProduct, toProductSummary } from './product.mapper';
import {
  facetsOf,
  findSeedProduct,
  queryProducts,
  toDetailDto,
  toSummaryDto,
} from './product.mock';

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
  category_ids: q.categoryIds?.join(','),
  subcategory_id: q.subcategoryId,
  shop_id: q.shopId,
  min_price: q.minPrice,
  max_price: q.maxPrice,
  brands: q.brands?.join(','),
  locations: q.locations?.join(','),
  authentic: q.authentic,
  freeship: q.freeship,
  has_voucher: q.hasVoucher,
  min_rating: q.minRating,
  attributes: q.attributes?.join(','),
  flash: q.flash,
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

/**
 * Giá trị lọc của một phạm vi (danh mục / từ khóa / shop) TRƯỚC khi áp bộ lọc chi tiết —
 * để bộ lọc "Thương hiệu", "Nơi bán" không tự co lại khi đang chọn.
 */
export async function getFacets(
  q: Pick<ProductQuery, 'q' | 'categoryId' | 'subcategoryId' | 'shopId'>,
): Promise<ProductFacets> {
  if (USE_MOCK) {
    await mockDelay();
    return facetsOf(queryProducts(q));
  }
  return (await get<ProductFacets>('/public/products/facets', toQuery(q))).data;
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

/**
 * "Gợi ý hôm nay" (trang chủ, /discover): danh sách gợi ý có phân trang. Mock giống prototype —
 * xoay vòng 20 sản phẩm mẫu thành 240 mục (8 trang × 30); `offset` để trang chủ bắt đầu từ mục 6.
 */
export async function listFeed(
  opts: { page?: number; size?: number; offset?: number } = {},
): Promise<Page<ProductSummary>> {
  const page = Math.max(1, opts.page ?? 1);
  const size = opts.size ?? 30;
  if (USE_MOCK) {
    await mockDelay();
    const all = queryProducts({});
    const total = 240;
    const start = (page - 1) * size;
    const n = Math.max(0, Math.min(size, total - start));
    const items = Array.from({ length: n }, (_, i) =>
      toProductSummary(toSummaryDto(all[(start + i + (opts.offset ?? 0)) % all.length])),
    );
    return { items, page, size, total_items: total, total_pages: Math.ceil(total / size) };
  }
  const res = await get<ProductSummaryDto[]>('/public/feed', { page, size });
  const pg = isPagePagination(res.pagination)
    ? res.pagination
    : { page, size, total_items: res.data.length, total_pages: 1 };
  return { items: res.data.map(toProductSummary), ...pg };
}
