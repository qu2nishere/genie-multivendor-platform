// Hồ sơ KYC của shop (API_SPEC 10.12; prototype vendor/Onboarding.jsx).
import type { BusinessType } from './registration.types';

export type KycStatus =
  | 'NOT_SUBMITTED'
  | 'PENDING'
  | 'AI_PROCESSING'
  | 'MANUAL_REVIEW'
  | 'VERIFIED'
  | 'REJECTED'
  | 'SUPPLEMENT_REQUESTED';

export type KycDocKey = 'ID_FRONT' | 'ID_BACK' | 'BUSINESS_LICENSE' | 'SELFIE';

/** Giấy tờ đã nộp: server chỉ trả tên + dung lượng (không trả URL ảnh CCCD). */
export interface KycDocInfo {
  fileName: string;
  sizeBytes: number;
  /** Tệp vừa tải trong phiên — gửi kèm khi nộp; null = giữ bản cũ. */
  objectKey: string | null;
  /** Ảnh xem trước trên máy (chỉ selfie vừa chụp / chọn). */
  previewUrl?: string | null;
}

export interface KycForm {
  shopName: string;
  businessType: BusinessType;
  identityNumber: string;
  taxCode: string;
  representative: string;
  documents: Partial<Record<KycDocKey, KycDocInfo>>;
  biometricConsent: boolean;
}

export interface KycDossier extends KycForm {
  status: KycStatus;
  rejectionReason: string | null;
  supplement: { documents: KycDocKey[]; note: string; deadlineAt: string } | null;
  manualReviewRequested: boolean;
  submittedAt: string | null;
}
