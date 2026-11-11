// Trang chính sách là nội dung tĩnh trong repo (không qua API) — SSR đọc thẳng dữ liệu.
import type { PolicyGroup, PolicyPage } from '../domain/policies.types';
import { POLICY_GROUPS } from './policies.data';

export function listPolicyGroups(): PolicyGroup[] {
  return POLICY_GROUPS.map((g) => ({
    slug: g.slug,
    title: g.title,
    pages: g.pages.map((p) => ({ slug: p.slug, name: p.name })),
  }));
}

/** Trang theo slug; nhận cả mã cũ của prototype. Không có → null (trang trả 404). */
export function getPolicy(slug: string): PolicyPage | null {
  for (const g of POLICY_GROUPS) {
    const p = g.pages.find((x) => x.slug === slug || x.legacy_id === slug);
    if (p) return { slug: p.slug, name: p.name, title: p.title, html: p.html, groupSlug: g.slug };
  }
  return null;
}

export const allPolicySlugs = (): string[] =>
  POLICY_GROUPS.flatMap((g) => g.pages.map((p) => p.slug));
