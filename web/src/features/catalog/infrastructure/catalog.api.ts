// Danh mục: mock khi USE_MOCK, sau này gọi /api/v1/public/categories (giữ nguyên chữ ký hàm).
import { mockDelay, mockFail, USE_MOCK } from '@/shared/api/mock';
import { SEED_VERIFIED_BRANDS } from '@/shared/api/mock/seed';
import { serverHttp } from '@/shared/api/server-http';
import type { AttributeFilter, Category } from '../domain/catalog.types';
import type { AttributeFilterDto, CategoryDto } from './catalog.dto';
import { toAttributeFilter, toCategory } from './catalog.mapper';
import { ATTR_FILTER_DTOS, CATEGORY_DTOS, SUB_CATEGORY_PRODUCTS } from './catalog.mock';

/** Mọi danh mục (gốc + con), phẳng. */
export async function listCategories(): Promise<Category[]> {
  if (USE_MOCK) {
    await mockDelay();
    return CATEGORY_DTOS.map(toCategory);
  }
  return (await serverHttp<CategoryDto[]>('/public/categories')).map(toCategory);
}

export async function listRootCategories(): Promise<Category[]> {
  return (await listCategories()).filter((c) => c.parentId === null);
}

/** Danh mục theo slug; không có → lỗi GNE-CAT-4041 (trang trả 404). */
export async function getCategoryBySlug(slug: string): Promise<Category> {
  if (USE_MOCK) {
    await mockDelay();
    const hit = CATEGORY_DTOS.find((c) => c.slug === slug);
    if (!hit) mockFail('GNE-CAT-4041');
    return toCategory(hit);
  }
  return toCategory(
    await serverHttp<CategoryDto>(`/public/categories/${encodeURIComponent(slug)}`),
  );
}

export async function listAttributeFilters(categoryId: number): Promise<AttributeFilter[]> {
  if (USE_MOCK) {
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
  if (USE_MOCK) return [...SEED_VERIFIED_BRANDS];
  return serverHttp<string[]>('/public/brands/verified');
}
