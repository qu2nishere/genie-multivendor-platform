import type { AttributeFilter, Category } from '../domain/catalog.types';
import type { AttributeFilterDto, CategoryDto } from './catalog.dto';

export const toCategory = (d: CategoryDto): Category => ({
  id: d.id,
  slug: d.slug,
  name: d.name,
  icon: d.icon,
  description: d.description,
  parentId: d.parent_id,
});

export const toAttributeFilter = (d: AttributeFilterDto): AttributeFilter => ({
  categoryId: d.category_id,
  name: d.name,
  values: [...d.values],
});
