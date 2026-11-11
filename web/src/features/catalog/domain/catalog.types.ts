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

/** Một thuộc tính lọc theo ngành (UC-CAT-05): "Bảo hành" 12 / 24 tháng… */
export interface AttributeFilter {
  categoryId: number;
  name: string;
  values: string[];
}

export interface CategoryTree extends Category {
  children: Category[];
}
