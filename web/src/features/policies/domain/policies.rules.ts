import type { PolicyGroup } from './policies.types';

/** Trang trước / sau theo thứ tự trong menu (điều hướng cuối trang chính sách). */
export function neighbours(groups: PolicyGroup[], slug: string) {
  const flat = groups.flatMap((g) => g.pages);
  const i = flat.findIndex((p) => p.slug === slug);
  return {
    prev: i > 0 ? flat[i - 1] : null,
    next: i >= 0 && i < flat.length - 1 ? flat[i + 1] : null,
  };
}

/** Tìm slug theo mã cũ của prototype (?screen=page&id=) để chuyển hướng 301. */
export const isKnownPolicy = (groups: PolicyGroup[], slug: string): boolean =>
  groups.some((g) => g.pages.some((p) => p.slug === slug));
