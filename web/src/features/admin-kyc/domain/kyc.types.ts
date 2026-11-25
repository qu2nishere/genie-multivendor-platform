// Duyệt KYC (API_SPEC 11.3; prototype admin/KYCReview.jsx, data.jsx KYC).
export type KycStatus =
  'PENDING' | 'AI_PROCESSING' | 'MANUAL_REVIEW' | 'SUPPLEMENT_REQUESTED' | 'VERIFIED' | 'REJECTED';

/** Tab hàng chờ — PENDING gom cả AI_PROCESSING / MANUAL_REVIEW (API 11.3). */
export type KycTab = 'PENDING' | 'SUPPLEMENT_REQUESTED' | 'VERIFIED' | 'REJECTED' | 'ALL';

export type BusinessType = 'INDIVIDUAL' | 'HOUSEHOLD' | 'BUSINESS';
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH';
export type KycDocType =
  'ID_FRONT' | 'ID_BACK' | 'BUSINESS_LICENSE' | 'BUSINESS_REGISTRATION' | 'SELFIE' | 'TAX_NOTICE';

export interface KycSubmission {
  id: number;
  code: string;
  shop: { id: number; name: string; logoUrl: string | null };
  ownerName: string;
  businessType: BusinessType;
  status: KycStatus;
  submittedAt: string;
}

export type AiCheck = 'qr_code' | 'national_emblem' | 'mrz' | 'fingerprint' | 'face_match';

export interface AiResult {
  overall: 'PASS' | 'PARTIAL' | 'FAIL';
  /** `null` = chưa kiểm (vd quốc huy / vân tay chưa làm) — không phải "không đạt". */
  checks: Record<AiCheck, boolean | null>;
  /** `null` = chưa so khớp khuôn mặt (người bán không đồng ý sinh trắc / AI không thấy mặt). */
  faceMatchScore: number | null;
  errors: string[];
}

/** Tín hiệu rủi ro đã cộng điểm (ENTITY 1.6) — vì sao hồ sơ vào hàng chờ (UC-VND-03 bước 5). */
export interface RiskSignal {
  code: string;
  points: number;
  /** Nhãn tiếng Việt do server gửi — dùng khi web chưa có bản dịch cho mã. */
  label: string;
}

export interface KycDocument {
  type: KycDocType;
  url: string;
  contentType: string;
}

export interface KycDetail extends KycSubmission {
  riskLevel: RiskLevel;
  riskScore: number;
  randomAudit: boolean;
  documents: KycDocument[];
  ai: AiResult | null;
  riskSignals: RiskSignal[];
  declared: { identityNumberMasked: string; representative: string; taxCode: string | null };
  shopCommissionRate: number | null;
  /** Huy hiệu "Chính hãng" (UC-VND-04 bước 8). */
  isMall: boolean;
  rejectionReason: string | null;
  supplement: { documents: KycDocType[]; note: string; dueAt: string } | null;
}

export type KycCounts = Record<KycTab, number>;
