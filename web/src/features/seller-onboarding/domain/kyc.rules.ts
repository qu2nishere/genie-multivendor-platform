// Quy tắc hồ sơ KYC (API 10.12): giấy tờ bắt buộc theo loại hình, khi nào được sửa / gửi, nhãn băng
// trạng thái, kiểm tệp ảnh (≥ 20 KB — ảnh quá nhỏ thường mờ).
import type { BusinessType } from './registration.types';
import type { KycDocKey, KycForm, KycStatus } from './kyc.types';

export const KYC_MIN_IMAGE = 20 * 1024;
export const KYC_MAX = 10 * 1024 * 1024;

/** CCCD 2 mặt luôn cần; GPKD với Hộ KD / Doanh nghiệp; selfie luôn cần. */
export function requiredDocs(t: BusinessType): KycDocKey[] {
  return t === 'INDIVIDUAL'
    ? ['ID_FRONT', 'ID_BACK', 'SELFIE']
    : ['ID_FRONT', 'ID_BACK', 'BUSINESS_LICENSE', 'SELFIE'];
}

export const needsTaxCode = (t: BusinessType): boolean => t !== 'INDIVIDUAL';

/** Gom trạng thái hiển thị băng (PENDING / AI_PROCESSING / MANUAL_REVIEW = đang chờ). */
export type KycBanner =
  'NOT_SUBMITTED' | 'PENDING' | 'VERIFIED' | 'REJECTED' | 'SUPPLEMENT_REQUESTED';
export function bannerOf(s: KycStatus): KycBanner {
  if (s === 'AI_PROCESSING' || s === 'MANUAL_REVIEW') return 'PENDING';
  return s;
}

/** Sửa / gửi được khi chưa nộp, bị từ chối hoặc cần bổ sung (409 GNE-VND-4095 khi đang xử lý / đã duyệt). */
export const canEdit = (s: KycStatus): boolean =>
  s === 'NOT_SUBMITTED' || s === 'REJECTED' || s === 'SUPPLEMENT_REQUESTED';

export const isResubmit = (s: KycStatus): boolean =>
  s === 'REJECTED' || s === 'SUPPLEMENT_REQUESTED';

export const missingDocs = (f: Pick<KycForm, 'businessType' | 'documents'>): KycDocKey[] =>
  requiredDocs(f.businessType).filter((k) => !f.documents[k]);

export type KycFileIssue = 'type' | 'size' | 'blurry';
/** Kiểm tệp KYC: PDF / JPG / PNG, ≤ 10 MB, ảnh ≥ 20 KB; selfie chỉ nhận ảnh. */
export function kycFileIssue(
  f: { type: string; size: number },
  key: KycDocKey,
): KycFileIssue | null {
  const image = f.type === 'image/jpeg' || f.type === 'image/png';
  if (!image && (key === 'SELFIE' || f.type !== 'application/pdf')) return 'type';
  if (f.size > KYC_MAX) return 'size';
  if (image && f.size < KYC_MIN_IMAGE) return 'blurry';
  return null;
}

/** "2,1 MB" / "812 KB". */
export function sizeLabel(bytes: number, locale: 'vi' | 'en'): string {
  const mb = bytes / 1048576;
  if (mb >= 1) return `${mb.toFixed(1).replace('.', locale === 'vi' ? ',' : '.')} MB`;
  return `${Math.max(1, Math.round(bytes / 1024))} KB`;
}

/** Thử thách kiểm tra người thật khi chụp selfie (prototype CameraModal CHALLENGES). */
export const LIVENESS = ['blink', 'left', 'right'] as const;
