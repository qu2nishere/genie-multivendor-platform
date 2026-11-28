// DTO snake_case (API_SPEC 11.12).
import type { MockModerationRow } from '@/shared/api/mock/admin';

export type ModerationRowDto = Omit<MockModerationRow, 'decision_reason'>;

export interface ModerationDetailDto extends ModerationRowDto {
  ai_result: {
    decision: 'APPROVE' | 'REJECT' | 'NEED_REVIEW';
    risk_score: number;
    violations: string[];
    reasoning: string | null;
    fallback: boolean;
    created_at: string;
  } | null;
  decision_reason: string | null;
  images: string[];
  documents: { id: number; file_name: string; url: string }[];
  category_path: string | null;
  brand: string | null;
  condition: 'NEW' | 'USED' | 'REFURBISHED';
  origin: string | null;
  description_html: string | null;
  weight_grams: number | null;
  dimensions_cm: { length: number | null; width: number | null; height: number | null } | null;
  ships_from: string | null;
  shipping_payer: 'BUYER' | 'SELLER';
  variants: { sku: string; name: string; price: number; stock: number }[];
}

export interface ModeratorDto {
  staff_id: number;
  name: string;
  is_accepting: boolean;
}

export interface AiSettingsDto {
  enabled: boolean;
  mode: 'stub' | 'http';
  model: string;
  reject_threshold: number;
  configured: boolean;
  updated_at: string | null;
  updated_by: string | null;
}
