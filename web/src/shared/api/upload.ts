// Tải tệp lên MinIO bằng URL ký sẵn (API_SPEC 4.1): nén ảnh (browser-image-compression) → kiểm MIME /
// kích thước → POST /uploads/presigned-urls → PUT thẳng lên MinIO → trả object_key để gửi kèm yêu cầu.
// Ảnh nén ra JPEG cho mục đích chỉ nhận jpeg/png (đánh giá, trả hàng, ảnh đại diện…), WebP cho ảnh sản
// phẩm; giấy tờ sản phẩm và hồ sơ KYC giữ nguyên tệp gốc (không nén — TS 5.4).
import {
  compressionOf,
  isImage,
  isVideo,
  uploadIssue,
  type UploadIssue,
  type UploadPurpose,
} from '@/shared/lib/upload.rules';
import { api } from './http';
import { mockDelay, USE_MOCK } from './mock';

export interface UploadedFile {
  objectKey: string;
  mediaType: 'IMAGE' | 'VIDEO' | 'DOCUMENT';
  name: string;
  /** URL xem trước trên máy (ảnh) — thu hồi bằng URL.revokeObjectURL khi bỏ. */
  previewUrl: string | null;
}

export class UploadError extends Error {
  constructor(readonly issue: UploadIssue) {
    super(issue);
  }
}

/** Nén ảnh ≤ 1920px, ~1 MB (JPEG hoặc WebP theo mục đích); tệp khác / mục đích không nén giữ nguyên. */
async function compress(file: File, purpose: UploadPurpose): Promise<File> {
  const to = compressionOf(purpose);
  if (!to || !isImage(file) || file.type === 'image/gif') return file;
  const { default: imageCompression } = await import('browser-image-compression');
  const type = to === 'webp' ? 'image/webp' : 'image/jpeg';
  const out = await imageCompression(file, {
    maxSizeMB: 1,
    maxWidthOrHeight: 1920,
    fileType: type,
    useWebWorker: true,
  });
  const name = file.name.replace(/\.[^.]+$/, '') + (to === 'webp' ? '.webp' : '.jpg');
  return new File([out], name, { type });
}

interface PresignedDto {
  upload_url: string;
  method: 'PUT';
  headers: Record<string, string>;
  object_key: string;
  expires_at: string;
}

export async function uploadFile(source: File, purpose: UploadPurpose): Promise<UploadedFile> {
  const file = await compress(source, purpose);
  const issue = uploadIssue(file, purpose);
  if (issue) throw new UploadError(issue);
  const mediaType = isImage(file) ? 'IMAGE' : isVideo(file) ? 'VIDEO' : 'DOCUMENT';
  const previewUrl = mediaType === 'IMAGE' ? URL.createObjectURL(file) : null;
  if (USE_MOCK) {
    await mockDelay(300);
    const ext = file.name.split('.').pop() ?? 'bin';
    return {
      objectKey: `tmp/${purpose.toLowerCase()}/${crypto.randomUUID()}.${ext}`,
      mediaType,
      name: file.name,
      previewUrl,
    };
  }
  const p = await api.post<PresignedDto>('/uploads/presigned-urls', {
    purpose,
    file_name: file.name,
    content_type: file.type,
    content_length: file.size,
  });
  const res = await fetch(p.upload_url, { method: p.method, headers: p.headers, body: file });
  if (!res.ok) throw new Error(`upload ${res.status}`);
  return { objectKey: p.object_key, mediaType, name: file.name, previewUrl };
}
