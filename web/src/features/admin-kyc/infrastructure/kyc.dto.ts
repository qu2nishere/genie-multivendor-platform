// DTO snake_case (API_SPEC 11.3).
import type { MockAdminKyc } from '@/shared/api/mock/admin';

export type KycSubmissionDto = MockAdminKyc;

export interface KycDetailDto extends KycSubmissionDto {
  risk_level: 'LOW' | 'MEDIUM' | 'HIGH';
  risk_score: number;
  is_random_audit: boolean;
  documents: { type: string; url: string; content_type: string }[];
  ai_result: {
    overall: 'PASS' | 'PARTIAL' | 'FAIL';
    checks: {
      qr_code: boolean;
      national_emblem: boolean;
      mrz: boolean;
      fingerprint: boolean;
      face_match: boolean;
    };
    face_match_score: number;
    errors: string[];
  } | null;
  declared: { identity_number_masked: string; representative: string; tax_code: string | null };
  shop_commission_rate: number | null;
  rejection_reason: string | null;
  supplement: { documents: string[]; note: string; due_at: string } | null;
}

export interface KycCountsDto {
  PENDING: number;
  SUPPLEMENT_REQUESTED: number;
  VERIFIED: number;
  REJECTED: number;
  ALL?: number;
}
