// Giới hạn tải tệp theo mục đích (API_SPEC 4.1) — kiểm phía client trước khi xin URL; server kiểm lại.

export type UploadPurpose =
  'AVATAR' | 'REVIEW_IMAGE' | 'REVIEW_VIDEO' | 'REFUND_EVIDENCE' | 'CHAT_IMAGE';

const MB = 1024 * 1024;

export const UPLOAD_LIMITS: Record<UploadPurpose, { mime: string[]; maxBytes: number }> = {
  AVATAR: { mime: ['image/jpeg', 'image/png'], maxBytes: 1 * MB },
  REVIEW_IMAGE: { mime: ['image/jpeg', 'image/png'], maxBytes: 5 * MB },
  REVIEW_VIDEO: { mime: ['video/mp4'], maxBytes: 30 * MB },
  REFUND_EVIDENCE: { mime: ['image/jpeg', 'image/png', 'video/mp4'], maxBytes: 10 * MB },
  CHAT_IMAGE: { mime: ['image/jpeg', 'image/png', 'image/webp', 'image/gif'], maxBytes: 5 * MB },
};

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
