import type { Category, CategoryTree } from './catalog.types';

/** Đường dẫn trang danh mục (R12: kebab-case). */
export const categoryHref = (slug: string): string => `/c/${slug}`;

/** Gom danh mục phẳng thành cây 2 cấp (gốc → con), giữ thứ tự gốc. */
export function buildTree(all: Category[]): CategoryTree[] {
  return all
    .filter((c) => c.parentId === null)
    .map((root) => ({ ...root, children: all.filter((c) => c.parentId === root.id) }));
}

/** Chuỗi breadcrumb từ gốc tới danh mục hiện tại. */
export function categoryPath(all: Category[], current: Category): Category[] {
  const path: Category[] = [current];
  let node = current;
  const seen = new Set<number>([current.id]);
  while (node.parentId !== null) {
    const parent = all.find((c) => c.id === node.parentId);
    if (!parent || seen.has(parent.id)) break;
    seen.add(parent.id);
    path.unshift(parent);
    node = parent;
  }
  return path;
}
