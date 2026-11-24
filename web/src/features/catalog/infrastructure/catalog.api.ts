// Danh mục: mock khi `isMocked('catalog')`; thật: /api/v1/public/categories (cây 5.1, web dàn phẳng) và
// /public/categories/{slug} (5.2) — plan B2 C08.01. Bộ lọc thuộc tính ngành (UC-CAT-05) lấy từ
// `summary.facets.attribute` của 5.3 qua `getFacets` (feature product) — plan 2026-11-24 D9, không có endpoint riêng.
import { mockDelay, mockFail, USE_MOCK } from '@/shared/api/mock';
import { isMocked } from '@/shared/config/env';
import { SEED_VERIFIED_BRANDS } from '@/shared/api/mock/seed';
import { serverHttp } from '@/shared/api/server-http';
import type { Category } from '../domain/catalog.types';
import type { CategoryHeaderDto, CategoryNodeDto } from './catalog.dto';
import { flattenTree, fromHeader, toCategory } from './catalog.mapper';
import { CATEGORY_DTOS, SUB_CATEGORY_PRODUCTS } from './catalog.mock';

const CATALOG_MOCK = isMocked('catalog');

/** Mọi danh mục đang hiển thị (gốc + con), phẳng. */
export async function listCategories(): Promise<Category[]> {
  if (CATALOG_MOCK) {
    await mockDelay();
    return CATEGORY_DTOS.map(toCategory);
  }
  return flattenTree(await serverHttp<CategoryNodeDto[]>('/public/categories'));
}

export async function listRootCategories(): Promise<Category[]> {
  return (await listCategories()).filter((c) => c.parentId === null);
}

/** Danh mục theo slug; không có → lỗi GNE-CAT-4041 (trang trả 404). */
export async function getCategoryBySlug(slug: string): Promise<Category> {
  if (CATALOG_MOCK) {
    await mockDelay();
    const hit = CATEGORY_DTOS.find((c) => c.slug === slug);
    if (!hit) mockFail('GNE-CAT-4041');
    return toCategory(hit);
  }
  return fromHeader(
    await serverHttp<CategoryHeaderDto>(`/public/categories/${encodeURIComponent(slug)}`),
  );
}

/** Mock: id sản phẩm thuộc danh mục con (prototype nhóm theo ảnh). Backend lọc thẳng theo category_id. */
export function mockProductIdsOfSubcategory(categoryId: number): readonly number[] | undefined {
  return SUB_CATEGORY_PRODUCTS[categoryId];
}

/** Thương hiệu đã xác minh (prototype VERIFIED_BRANDS) — bộ lọc "Thương hiệu" hiện dấu tích. */
export async function listVerifiedBrands(): Promise<string[]> {
  if (USE_MOCK) return [...SEED_VERIFIED_BRANDS]; // chưa có backend — giữ theo mock toàn cục
  return serverHttp<string[]>('/public/brands/verified');
}
