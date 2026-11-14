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

/** Tách "/p/tai-nghe-anc-pro-5-12" → { slug: "tai-nghe-anc-pro-5", id: 12 }. */
export function splitSlugId(param: string): { slug: string; id: number } | null {
  const m = /^(.*)-(\d+)$/.exec(param);
  if (!m) return null;
  return { slug: m[1], id: Number(m[2]) };
}

/** Ghép slug + id cho deeplink sản phẩm. */
export const slugWithId = (slug: string, id: number | string) => `${slug}-${id}`;
