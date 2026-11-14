// Dữ liệu mẫu danh mục dạng DTO (từ bộ seed chung — prototype data.jsx).
import {
  SEED_ATTR_FILTERS,
  SEED_ROOT_CATEGORIES,
  SEED_SUB_CATEGORIES,
} from '@/shared/api/mock/seed';
import type { AttributeFilterDto, CategoryDto } from './catalog.dto';

export const CATEGORY_DTOS: CategoryDto[] = [
  ...SEED_ROOT_CATEGORIES.map((c) => ({ ...c })),
  ...SEED_SUB_CATEGORIES.map(({ product_ids: _p, ...c }) => ({ ...c })),
];

export const ATTR_FILTER_DTOS: AttributeFilterDto[] = SEED_ATTR_FILTERS.map((a) => ({ ...a }));

/** Sản phẩm thuộc danh mục con (prototype nhóm theo ảnh) — dùng để lọc mock ở product.api. */
export const SUB_CATEGORY_PRODUCTS: Record<number, readonly number[]> = Object.fromEntries(
  SEED_SUB_CATEGORIES.map((s) => [s.id, s.product_ids]),
);
