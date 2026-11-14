// Giới hạn tải tệp theo mục đích (API_SPEC 4.1) — kiểm phía client trước khi xin URL; server kiểm lại.

export type UploadPurpose =
  | 'AVATAR'
  | 'REVIEW_IMAGE'
  | 'REVIEW_VIDEO'
  | 'REFUND_EVIDENCE'
  | 'CHAT_IMAGE'
  | 'SHOP_LOGO'
  | 'SHOP_COVER'
  | 'PRODUCT_IMAGE'
  | 'VARIANT_IMAGE'
  | 'DESCRIPTION_IMAGE'
  | 'PRODUCT_VIDEO'
  | 'PRODUCT_DOCUMENT'
  | 'KYC_DOCUMENT'
  | 'BANNER_IMAGE'
  | 'NOTIFICATION_IMAGE';

const DOCS = [
  'application/pdf',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/vnd.ms-excel',
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
];
const MEDIA = ['image/jpeg', 'image/png', 'image/webp'];

const MB = 1024 * 1024;

export const UPLOAD_LIMITS: Record<UploadPurpose, { mime: string[]; maxBytes: number }> = {
  AVATAR: { mime: ['image/jpeg', 'image/png'], maxBytes: 1 * MB },
  REVIEW_IMAGE: { mime: ['image/jpeg', 'image/png'], maxBytes: 5 * MB },
  REVIEW_VIDEO: { mime: ['video/mp4'], maxBytes: 30 * MB },
  REFUND_EVIDENCE: { mime: ['image/jpeg', 'image/png', 'video/mp4'], maxBytes: 10 * MB },
  CHAT_IMAGE: { mime: ['image/jpeg', 'image/png', 'image/webp', 'image/gif'], maxBytes: 5 * MB },
  SHOP_LOGO: { mime: ['image/jpeg', 'image/png'], maxBytes: 1 * MB },
  SHOP_COVER: { mime: ['image/jpeg', 'image/png'], maxBytes: 2 * MB },
  PRODUCT_IMAGE: { mime: MEDIA, maxBytes: 5 * MB },
  VARIANT_IMAGE: { mime: MEDIA, maxBytes: 5 * MB },
  DESCRIPTION_IMAGE: { mime: MEDIA, maxBytes: 5 * MB },
  PRODUCT_VIDEO: { mime: ['video/mp4'], maxBytes: 30 * MB },
  PRODUCT_DOCUMENT: { mime: DOCS, maxBytes: 10 * MB },
  KYC_DOCUMENT: { mime: ['application/pdf', 'image/jpeg', 'image/png'], maxBytes: 10 * MB },
  // Bucket content-assets (Admin: banner, thông báo sàn)
  BANNER_IMAGE: { mime: MEDIA, maxBytes: 5 * MB },
  NOTIFICATION_IMAGE: { mime: MEDIA, maxBytes: 5 * MB },
};

/** Ảnh sản phẩm lưu WebP (bucket product-media); KYC / giấy tờ không nén (TS 5.4). */
export const compressionOf = (purpose: UploadPurpose): 'jpeg' | 'webp' | null => {
  if (purpose === 'KYC_DOCUMENT' || purpose === 'PRODUCT_DOCUMENT') return null;
  if (
    purpose === 'PRODUCT_IMAGE' ||
    purpose === 'VARIANT_IMAGE' ||
    purpose === 'DESCRIPTION_IMAGE' ||
    purpose === 'BANNER_IMAGE' ||
    purpose === 'NOTIFICATION_IMAGE'
  )
    return 'webp';
  return 'jpeg';
};

/** Ảnh KYC phải ≥ 20 KB (API 4.1) — ảnh quá nhỏ thường mờ, AI không đọc được. */
export const KYC_MIN_IMAGE_BYTES = 20 * 1024;

export type UploadIssue = 'type' | 'size';

/** Lỗi của tệp với mục đích (null = hợp lệ). Ảnh vượt cỡ được nén trước khi kiểm (xem upload.ts). */
export function uploadIssue(
  file: { type: string; size: number },
  purpose: UploadPurpose,
): UploadIssue | null {
  const lim = UPLOAD_LIMITS[purpose];
  if (!lim.mime.includes(file.type)) return 'type';
  if (file.size > lim.maxBytes) return 'size';
  return null;
}

export const maxMb = (purpose: UploadPurpose): number => UPLOAD_LIMITS[purpose].maxBytes / MB;

export const isImage = (f: { type: string }): boolean => f.type.startsWith('image/');
export const isVideo = (f: { type: string }): boolean => f.type.startsWith('video/');
