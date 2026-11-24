import type { Category } from '../domain/catalog.types';
import type { CategoryDto, CategoryHeaderDto, CategoryNodeDto } from './catalog.dto';

export const toCategory = (d: CategoryDto): Category => ({
  id: d.id,
  slug: d.slug,
  name: d.name,
  icon: d.icon,
  description: d.description,
  parentId: d.parent_id,
});

/** Dàn phẳng cây 5.1 (cha đứng trước con) — mô tả không có trong cây, lấy ở 5.2 khi mở trang danh mục. */
export function flattenTree(nodes: CategoryNodeDto[], parentId: number | null = null): Category[] {
  return nodes.flatMap((n) => [
    { id: n.id, slug: n.slug, name: n.name, icon: n.icon ?? '', description: '', parentId },
    ...flattenTree(n.children ?? [], n.id),
  ]);
}

export const fromHeader = (d: CategoryHeaderDto): Category => ({
  id: d.id,
  slug: d.slug,
  name: d.name,
  icon: d.icon ?? '',
  description: d.description ?? '',
  parentId: d.parent_id,
});
