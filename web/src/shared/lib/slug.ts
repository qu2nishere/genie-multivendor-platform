// Slug tiếng Việt không dấu cho URL (plan G12.20 đưa slugify vào shared/lib; dùng sớm từ G07 cho
// /c/{slug}, /p/{slug}-{id}, /policies/{slug}). "Điện thoại & Phụ kiện" → "dien-thoai-phu-kien".

export function slugify(input: string): string {
  return input
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .replace(/-{2,}/g, '-');
}

/** Độ dài tối đa slug sản phẩm — khớp `SlugUtil.MAX_LENGTH` của backend. */
export const PRODUCT_SLUG_MAX = 80;

/**
 * Slug sản phẩm sinh từ tên — CÙNG thuật toán với `SlugUtil.slugify` (backend, plan 2026-11-17 A11, D2):
 * như {@link slugify} + cắt 80 ký tự (bỏ gạch cuối) + rỗng thì "san-pham". Hai phía test chung bộ mẫu
 * `docs/api-tests/slug-samples.json`.
 */
export function productSlug(name: string | null | undefined): string {
  let s = slugify(name ?? '');
  if (s.length > PRODUCT_SLUG_MAX) s = s.slice(0, PRODUCT_SLUG_MAX).replace(/^-+|-+$/g, '');
  return s || 'san-pham';
}

/** Tách "/p/tai-nghe-anc-pro-5-12" → { slug: "tai-nghe-anc-pro-5", id: 12 }. */
export function splitSlugId(param: string): { slug: string; id: number } | null {
  const m = /^(.*)-(\d+)$/.exec(param);
  if (!m) return null;
  return { slug: m[1], id: Number(m[2]) };
}

/** Ghép slug + id cho deeplink sản phẩm. */
export const slugWithId = (slug: string, id: number | string) => `${slug}-${id}`;
