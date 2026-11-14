// JSON snake_case của API danh mục (GET /public/categories, /public/categories/{slug}).
export interface CategoryDto {
  id: number;
  slug: string;
  name: string;
  icon: string;
  description: string;
  parent_id: number | null;
}

export interface AttributeFilterDto {
  category_id: number;
  name: string;
  values: readonly string[];
}
