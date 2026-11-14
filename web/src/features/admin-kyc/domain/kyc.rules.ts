// Hàm thuần của Duyệt KYC (API_SPEC 11.3, 16.4).
import type { AiCheck, KycDocType, KycStatus, KycTab, RiskLevel } from './kyc.types';

export const KYC_TABS: KycTab[] = [
  'PENDING',
  'SUPPLEMENT_REQUESTED',
  'VERIFIED',
  'REJECTED',
  'ALL',
];

/** Mã lý do từ chối KYC (16.4). */
export const REJECT_REASONS = [
  'BLURRY_DOCUMENT',
  'INFO_MISMATCH',
  'EXPIRED_DOCUMENT',
  'SUSPECTED_FORGERY',
  'MISSING_LICENSE',
  'OTHER',
] as const;
export type KycRejectReason = (typeof REJECT_REASONS)[number];

/** Giấy tờ có thể yêu cầu bổ sung (KycDocumentType). */
export const SUPPLEMENT_DOCS: KycDocType[] = [
  'ID_FRONT',
  'ID_BACK',
  'BUSINESS_LICENSE',
  'BUSINESS_REGISTRATION',
  'SELFIE',
  'TAX_NOTICE',
];

export const AI_CHECKS: AiCheck[] = [
  'qr_code',
  'national_emblem',
  'mrz',
  'fingerprint',
  'face_match',
];

/** Hạn bổ sung hồ sơ (API 11.3 request-supplement). */
export const SUPPLEMENT_DAYS = 7;

/** Hoa hồng riêng 0–50% (cùng giới hạn cấu hình hoa hồng 11.8). */
export const MAX_COMMISSION_RATE = 50;

const WAITING: readonly KycStatus[] = ['PENDING', 'AI_PROCESSING', 'MANUAL_REVIEW'];

export const isWaiting = (s: KycStatus): boolean => WAITING.includes(s);

export function inTab(s: KycStatus, tab: KycTab): boolean {
  if (tab === 'ALL') return true;
  if (tab === 'PENDING') return isWaiting(s);
  return s === tab;
}

/** Mức rủi ro theo điểm: LOW < 30, MEDIUM 30–69, HIGH ≥ 70. */
export function riskOf(score: number): RiskLevel {
  if (score >= 70) return 'HIGH';
  if (score >= 30) return 'MEDIUM';
  return 'LOW';
}

/** Thao tác được phép theo trạng thái (prototype: chờ duyệt → 3 nút; còn lại → đưa lại vào hàng chờ). */
export function kycActions(s: KycStatus): ('approve' | 'reject' | 'supplement' | 'requeue')[] {
  return isWaiting(s) ? ['reject', 'approve', 'supplement'] : ['requeue'];
}

/** Tông nhãn trạng thái (StatusBadge): chờ bổ sung dùng tông chờ. */
export function statusTone(s: KycStatus): 'PENDING' | 'VERIFIED' | 'REJECTED' {
  if (s === 'VERIFIED') return 'VERIFIED';
  if (s === 'REJECTED') return 'REJECTED';
  return 'PENDING';
}

/** Nhóm hiển thị trạng thái (gom các trạng thái chờ). */
export const statusKey = (s: KycStatus): Exclude<KycTab, 'ALL'> =>
  isWaiting(s) ? 'PENDING' : (s as Exclude<KycTab, 'ALL' | 'PENDING'>);

/** Ô "hoa hồng riêng": rỗng = theo ngành (null); ngoài 0–50 → không hợp lệ. */
export function parseShopRate(raw: string): { ok: true; value: number | null } | { ok: false } {
  const s = raw.trim().replace(',', '.');
  if (!s) return { ok: true, value: null };
  const n = Number(s);
  if (!Number.isFinite(n) || n < 0 || n > MAX_COMMISSION_RATE) return { ok: false };
  return { ok: true, value: Math.round(n * 100) / 100 };
}
