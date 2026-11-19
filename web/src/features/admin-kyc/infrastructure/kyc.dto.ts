// DTO snake_case (API_SPEC 11.3).
import type { MockAdminKyc } from '@/shared/api/mock/admin';

export type KycSubmissionDto = MockAdminKyc;

export interface KycDetailDto extends KycSubmissionDto {
  /** null khi AI chưa chấm điểm (hồ sơ vừa nộp / AI lỗi). */
  risk_level: 'LOW' | 'MEDIUM' | 'HIGH' | null;
  risk_score: number | null;
  is_random_audit: boolean;
  documents: { type: string; url: string; content_type: string }[];
  ai_result: {
    overall: 'PASS' | 'PARTIAL' | 'FAIL';
    checks: {
      qr_code: boolean | null;
      national_emblem: boolean | null;
      mrz: boolean | null;
      fingerprint: boolean | null;
      face_match: boolean | null;
    };
    face_match_score: number | null;
    errors: string[];
  } | null;
  declared: { identity_number_masked: string; representative: string; tax_code: string | null };
  shop_commission_rate: number | null;
  is_mall?: boolean;
  rejection_reason: string | null;
  /** Server trả `deadline_at` (API 11.3); mock cũ dùng `due_at`. */
  supplement: { documents: string[]; note: string; deadline_at?: string; due_at?: string } | null;
}

export interface KycCountsDto {
  PENDING: number;
  SUPPLEMENT_REQUESTED: number;
  VERIFIED: number;
  REJECTED: number;
  ALL?: number;
}
