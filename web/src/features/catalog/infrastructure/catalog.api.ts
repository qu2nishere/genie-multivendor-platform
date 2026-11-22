// Danh mục: mock khi `isMocked('catalog')`; thật: /api/v1/public/categories (cây 5.1, web dàn phẳng) và
// /public/categories/{slug} (5.2) — plan B2 C08.01. Bộ lọc thuộc tính (UC-CAT-05) chưa có backend → nhóm riêng
// `attribute-filters`, vẫn mock.
import { mockDelay, mockFail, USE_MOCK } from '@/shared/api/mock';
import { isMocked } from '@/shared/config/env';
import { SEED_VERIFIED_BRANDS } from '@/shared/api/mock/seed';
import { serverHttp } from '@/shared/api/server-http';
import type { AttributeFilter, Category } from '../domain/catalog.types';
import type { AttributeFilterDto, CategoryHeaderDto, CategoryNodeDto } from './catalog.dto';
import { flattenTree, fromHeader, toAttributeFilter, toCategory } from './catalog.mapper';
import { ATTR_FILTER_DTOS, CATEGORY_DTOS, SUB_CATEGORY_PRODUCTS } from './catalog.mock';

const CATALOG_MOCK = isMocked('catalog');
const ATTR_FILTER_MOCK = isMocked('attribute-filters');

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

export async function listAttributeFilters(categoryId: number): Promise<AttributeFilter[]> {
  if (ATTR_FILTER_MOCK) {
    await mockDelay();
    return ATTR_FILTER_DTOS.filter((a) => a.category_id === categoryId).map(toAttributeFilter);
  }
  return (
    await serverHttp<AttributeFilterDto[]>(`/public/categories/${categoryId}/attribute-filters`)
  ).map(toAttributeFilter);
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
