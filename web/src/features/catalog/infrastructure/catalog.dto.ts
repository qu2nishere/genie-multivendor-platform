// JSON snake_case của API danh mục (GET /public/categories, /public/categories/{slug}).
export interface CategoryDto {
  id: number;
  slug: string;
  name: string;
  icon: string;
  description: string;
  parent_id: number | null;
}

/** Nút cây 5.1 `GET /public/categories` — backend trả cây 3 cấp, web dàn phẳng (plan B2 C08.01). */
export interface CategoryNodeDto {
  id: number;
  name: string;
  slug: string;
  icon: string | null;
  children: CategoryNodeDto[];
}

/** 5.2 `GET /public/categories/{slug}` — đầu trang danh mục. */
export interface CategoryHeaderDto {
  id: number;
  name: string;
  slug: string;
  icon: string | null;
  description: string | null;
  parent_id: number | null;
  breadcrumb: { id: number; name: string; slug: string }[];
  children: { id: number; name: string; slug: string }[];
  product_count: number;
}

export interface AttributeFilterDto {
  category_id: number;
  name: string;
  values: readonly string[];
}
