// Bản nhập dở của form đăng bán, giữ ở localStorage để F5 / đóng tab không mất dữ liệu (yêu cầu chủ dự án
// 2026-11-27; LUU_TRU_DU_LIEU mục 6). Hàm thuần — đọc / ghi trình duyệt ở application/useProductDraft.ts.
import { PRODUCT_DRAFT_PREFIX } from '@/shared/lib/storage';
import type { ProductForm } from './product.types';

/** Đổi cấu trúc `ProductForm` thì tăng số này để bỏ các bản cũ. */
export const DRAFT_SCHEMA = 2;
/** Bản nhập dở quá 7 ngày thì bỏ. */
export const DRAFT_TTL_MS = 7 * 24 * 60 * 60 * 1000;
/** Tệp tải lên nằm ở `tmp/` — kho tự xóa sau 1 ngày; chừa 4 giờ cho an toàn. */
export const TMP_UPLOAD_TTL_MS = 20 * 60 * 60 * 1000;

export interface ProductDraft {
  schema: number;
  savedAt: number;
  /** `version` của sản phẩm lúc bắt đầu sửa (null = sản phẩm mới). */
  productVersion: number | null;
  form: ProductForm;
}

/** Khóa localStorage (sau `genie.`) — `clearUserData` xóa hết theo tiền tố khi đăng xuất. */
export const draftKey = (userId: number, productId: number | null): string =>
  `${PRODUCT_DRAFT_PREFIX}${userId}.${productId ?? 'new'}`;

/** Form có gì đáng giữ không (form trống thì không lưu / không gợi ý khôi phục). */
export function hasContent(f: ProductForm): boolean {
  return (
    f.name.trim() !== '' ||
    f.images.length > 0 ||
    f.video !== null ||
    f.documents.length > 0 ||
    f.categoryPath.length > 0 ||
    f.brand !== null ||
    f.origin.trim() !== '' ||
    f.specs.some((s) => s.name.trim() || s.value.trim()) ||
    f.descriptionHtml.replace(/<[^>]*>/g, '').trim() !== '' ||
    f.groups.some((g) => g.name.trim() || g.values.length) ||
    f.variants.some((v) => v.price !== null || v.stock !== null) ||
    f.weightGrams !== null
  );
}

/**
 * Bản này còn gợi ý khôi phục được không: đúng cấu trúc, chưa quá 7 ngày, có nội dung; khi sửa sản phẩm đã có thì
 * `version` lúc lưu phải bằng `version` hiện tại (sản phẩm vừa được duyệt / sửa ở nơi khác → bỏ, tránh đè dữ liệu mới).
 */
export function draftUsable(
  d: ProductDraft | null,
  now: number,
  productVersion: number | null,
): d is ProductDraft {
  if (!d || d.schema !== DRAFT_SCHEMA || typeof d.savedAt !== 'number' || !d.form) return false;
  if (now - d.savedAt > DRAFT_TTL_MS || d.savedAt > now + 60_000) return false;
  if (d.productVersion !== productVersion) return false;
  return hasContent(d.form);
}

const isTmp = (key: string | null | undefined) => !!key && key.startsWith('tmp/');

/**
 * Tệp vừa tải (`tmp/…`) đã quá hạn kho tự xóa → bỏ khỏi form khi khôi phục (gửi lên server sẽ lỗi "tệp không hợp lệ").
 * Tệp đã thuộc sản phẩm (khóa chính thức) giữ nguyên. `dropped` = có bỏ gì không, để báo người bán tải lại.
 */
export function dropExpiredUploads(
  f: ProductForm,
  savedAt: number,
  now: number,
): { form: ProductForm; dropped: boolean } {
  if (now - savedAt <= TMP_UPLOAD_TTL_MS) return { form: f, dropped: false };
  let dropped = false;
  const keep = <T>(x: T, key: string | null | undefined): boolean => {
    if (isTmp(key)) dropped = true;
    return !isTmp(key) && x !== null;
  };
  const images = f.images.filter((i) => keep(i, i.objectKey));
  const documents = f.documents.filter((d) => keep(d, d.objectKey));
  const video = f.video && keep(f.video, f.video.objectKey) ? f.video : null;
  const groups = f.groups.map((g) => ({
    ...g,
    values: g.values.map((v) => {
      if (!isTmp(v.imageKey)) return v;
      dropped = true;
      return { ...v, imageKey: null, imageUrl: null };
    }),
  }));
  return { form: { ...f, images, documents, video, groups }, dropped };
}
