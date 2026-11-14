// DTO snake_case (API_SPEC 11.12).
import type { MockModerationRow } from '@/shared/api/mock/admin';

export type ModerationRowDto = Omit<MockModerationRow, 'decision_reason'>;

export interface ModerationDetailDto extends ModerationRowDto {
  ai_result: {
    decision: 'APPROVE' | 'REJECT' | 'NEED_REVIEW';
    risk_score: number;
    violations: string[];
    reasoning: string | null;
  } | null;
  decision_reason: string | null;
  images: string[];
  documents: { id: number; file_name: string; url: string }[];
  category_path: string;
  brand: string;
  condition: 'NEW' | 'USED' | 'REFURBISHED';
  origin: string;
  description_html: string;
  weight_grams: number;
  dimensions_cm: { length: number; width: number; height: number };
  ships_from: string;
  shipping_payer: 'BUYER' | 'SELLER';
  variants: { sku: string; name: string; price: number; stock: number }[];
}

export interface ModeratorDto {
  staff_id: number;
  name: string;
  is_accepting: boolean;
}
