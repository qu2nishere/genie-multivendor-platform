// Miền danh mục (plan G08.04; prototype data.jsx CATEGORIES, CATEGORY_META, SUBCATEGORIES, ATTR_FILTERS).

export interface Category {
  id: number;
  slug: string;
  name: string;
  /** Tên icon trong bộ ICONS (UI tự rơi về "package" nếu không có). */
  icon: string;
  description: string;
  parentId: number | null;
}

export interface CategoryTree extends Category {
  children: Category[];
}
