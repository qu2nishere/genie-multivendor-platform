// Sản phẩm công khai — mock khi `isMocked('product')`; thật: /api/v1/public/products… (API_SPEC 5.3, 5.5–5.9;
// plan 2026-11-17 C07.02). Chữ ký hàm giữ nguyên cho UI. Gọi từ frontmatter .astro (SSR) và từ island.
import { request } from '@/shared/api/http';
import { getAccessToken } from '@/shared/auth/session';
import type { Query } from '@/shared/api/http';
import { mockDelay, mockFail, mockOk, paginatePage } from '@/shared/api/mock';
import { env, isMocked } from '@/shared/config/env';
import type { Page } from '@/shared/api/types';
import { isPagePagination } from '@/shared/api/types';
import type { ViewSource } from '../domain/product.rules';
import type {
  Product,
  ProductFacets,
  ProductQuery,
  ProductSort,
  ProductSummary,
} from '../domain/product.types';
import type { ProductCardDto, ProductDetailApiDto, SearchFacetsDto } from './product.dto';
import {
  fromCardDto,
  fromDetailDto,
  fromFacetsDto,
  toProduct,
  toProductSummary,
} from './product.mapper';
import {
  facetsOf,
  findSeedProduct,
  queryProducts,
  toDetailDto,
  toSummaryDto,
} from './product.mock';

const USE_MOCK = isMocked('product');
const isServer = () => typeof window === 'undefined';

/** SSR gọi INTERNAL_API_URL (không token), trình duyệt gọi PUBLIC_API_URL. */
function get<T>(path: string, query?: Query) {
  return request<T>(path, {
    query,
    auth: false,
    baseUrl: isServer() ? (env.INTERNAL_API_URL ?? env.PUBLIC_API_URL) : env.PUBLIC_API_URL,
  });
}

/** Sắp xếp UI → enum `sort` của API 5.3. */
const SORT: Record<ProductSort, string> = {
  rel: 'RELEVANCE',
  hot: 'BEST_SELLING',
  new: 'NEWEST',
  price_asc: 'PRICE_ASC',
  price_desc: 'PRICE_DESC',
};

/**
 * API chỉ nhận MỘT `category_id` (gồm cả danh mục con). Danh mục con thắng danh mục cha; trang tìm kiếm chọn nhiều
 * danh mục gốc → lấy danh mục đầu (giới hạn của API_SPEC 5.3 — ghi ở progress C07).
 */
const categoryOf = (q: ProductQuery): number | undefined =>
  q.subcategoryId ?? q.categoryId ?? (q.categoryIds?.length ? q.categoryIds[0] : undefined);

/** Phạm vi (từ khóa / danh mục / shop) — trước khi áp bộ lọc chi tiết. */
const scopeQuery = (q: ProductQuery): Query => ({
  q: q.q,
  category_id: categoryOf(q),
  shop_id: q.shopId,
});

/** Facet của một phạm vi: bảng tên → id cho thương hiệu, nơi bán; mã thuộc tính lọc. */
async function scopeFacets(
  q: ProductQuery,
): Promise<{ facets: SearchFacetsDto | null; total: number }> {
  const res = await get<ProductCardDto[]>('/public/products', {
    ...scopeQuery(q),
    include_facets: true,
    size: 1,
  });
  const facets = (res.summary?.facets as SearchFacetsDto | undefined) ?? null;
  return {
    facets,
    total: isPagePagination(res.pagination) ? res.pagination.total_items : res.data.length,
  };
}

/** Tên đã chọn → id; tên không có trong facet → -1 (lọc ra rỗng, không bỏ lọc). */
const idsOf = (
  names: string[] | undefined,
  table: { id: number; name: string }[],
): number[] | undefined =>
  names?.length ? names.map((n) => table.find((x) => x.name === n)?.id ?? -1) : undefined;

/** Tham số lọc đầy đủ của API 5.3. */
export async function toApiQuery(q: ProductQuery): Promise<Query> {
  const needsFacets = !!(q.brands?.length || q.locations?.length || q.attributes?.length);
  const facets = needsFacets ? (await scopeFacets(q)).facets : null;
  const query: Query = {
    ...scopeQuery(q),
    brand_ids: idsOf(q.brands, facets?.brands ?? []),
    province_ids: idsOf(
      q.locations,
      (facets?.locations ?? []).map((l) => ({ id: l.province_id, name: l.name })),
    ),
    price_min: q.minPrice,
    price_max: q.maxPrice,
    rating_min: q.minRating,
    is_mall: q.authentic || undefined,
    free_shipping: q.freeship || undefined,
    has_shop_voucher: q.hasVoucher || undefined,
    discount_min: q.minDiscount,
    sort: q.sort ? SORT[q.sort] : undefined,
    page: q.page ?? 1,
    size: q.size ?? 20,
  };
  if (q.attributes?.length && facets?.attribute)
    query[`attr.${facets.attribute.code}`] = q.attributes;
  return query;
}

/** Danh sách có phân trang số trang (danh mục, tìm kiếm, khám phá, shop). */
export async function listProducts(q: ProductQuery = {}): Promise<Page<ProductSummary>> {
  if (USE_MOCK) {
    await mockDelay();
    const page = paginatePage(queryProducts(q), { page: q.page, size: q.size });
    return { ...page, items: page.items.map((p) => toProductSummary(toSummaryDto(p))) };
  }
  const res = await get<ProductCardDto[]>('/public/products', await toApiQuery(q));
  const pg = isPagePagination(res.pagination)
    ? res.pagination
    : { page: 1, size: res.data.length, total_items: res.data.length, total_pages: 1 };
  return { items: res.data.map(fromCardDto), ...pg };
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
    return facetsOf(queryProducts(q), q.subcategoryId ? undefined : q.categoryId);
  }
  const { facets, total } = await scopeFacets(q);
  return fromFacetsDto(facets, total);
}

/** Chi tiết; không có / đã gỡ → ApiError GNE-CAT-4042 (trang trả 404). */
export async function getProduct(id: number): Promise<Product> {
  if (USE_MOCK) {
    await mockDelay();
    const p = findSeedProduct(id);
    if (!p) mockFail('GNE-CAT-4042');
    return toProduct(mockOk(toDetailDto(p)).data);
  }
  return fromDetailDto((await get<ProductDetailApiDto>(`/public/products/${id}`)).data);
}

/** Sản phẩm tương tự (UC-CAT-07): chạy cả khi sản phẩm gốc đã ẩn — trang "không còn bán" dùng lại. */
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
  return (await get<ProductCardDto[]>(`/public/products/${id}/similar`, { limit })).data.map(
    fromCardDto,
  );
}

/** Nhiều sản phẩm theo id (yêu thích, gợi ý trang chủ). API không có lọc theo id → gọi chi tiết song song, bỏ id hết bán. */
export async function listProductsByIds(ids: number[]): Promise<ProductSummary[]> {
  if (USE_MOCK) {
    await mockDelay();
    return ids
      .map((id) => findSeedProduct(id))
      .filter((p): p is NonNullable<typeof p> => !!p)
      .map((p) => toProductSummary(toSummaryDto(p)));
  }
  const settled = await Promise.allSettled(ids.map((id) => getProduct(id)));
  return settled.flatMap((r) => (r.status === 'fulfilled' ? [r.value] : []));
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
  const res = await get<ProductCardDto[]>('/public/recommendations', { page, size });
  const pg = isPagePagination(res.pagination)
    ? res.pagination
    : { page, size, total_items: res.data.length, total_pages: 1 };
  return { items: res.data.map(fromCardDto), ...pg };
}

/**
 * Ghi lượt xem (UC-CAT-06 bước 9, UC-CAT-18): `fetch keepalive` — không chặn trang, không đọc kết quả, lỗi bỏ qua.
 * Không dùng `sendBeacon`: kiểu `application/json` khác origin bị chặn vì cần CORS preflight.
 */
export function recordView(productId: number, source: ViewSource): void {
  if (USE_MOCK || isServer()) return;
  const base = env.PUBLIC_API_URL ?? '';
  void fetch(`${base.replace(/\/$/, '')}/public/products/${productId}/views`, {
    method: 'POST',
    // có token → backend nhớ danh mục vừa xem cho "Gợi ý cho bạn" (API_SPEC 5.5)
    headers: {
      'Content-Type': 'application/json',
      ...(getAccessToken() ? { Authorization: `Bearer ${getAccessToken()}` } : {}),
    },
    body: JSON.stringify({ source }),
    keepalive: true,
    credentials: 'omit',
  }).catch(() => undefined);
}
