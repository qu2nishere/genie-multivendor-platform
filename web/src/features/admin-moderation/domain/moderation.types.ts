// Duyệt sản phẩm (API_SPEC 11.12; prototype admin/ProductModeration.jsx, data.jsx MODERATION).
export type ModerationStatus = 'PENDING_APPROVAL' | 'FLAGGED' | 'PUBLISHED' | 'REJECTED';
export type ModerationTab = 'PENDING' | 'PUBLISHED' | 'REJECTED';
/** Nhãn hiển thị: PENDING_APPROVAL + is_requeued → "Chờ duyệt lại". */
export type ModerationLabel = ModerationStatus | 'REQUEUED';
export type AiDecision = 'APPROVE' | 'REJECT' | 'NEED_REVIEW';

export interface ModerationRow {
  productId: number;
  name: string;
  imageUrl: string;
  shop: { id: number; name: string };
  status: ModerationStatus;
  requeued: boolean;
  assignee: { id: number; name: string } | null;
  rejectedByAi: boolean;
  submittedAt: string;
}

export interface ModerationDetail extends ModerationRow {
  ai: {
    decision: AiDecision;
    riskScore: number;
    violations: string[];
    reasoning: string | null;
  } | null;
  decisionReason: string | null;
  images: string[];
  documents: { id: number; fileName: string; url: string }[];
  categoryPath: string;
  brand: string;
  condition: 'NEW' | 'USED' | 'REFURBISHED';
  origin: string;
  descriptionHtml: string;
  weightGrams: number;
  dimensionsCm: { length: number; width: number; height: number };
  shipsFrom: string;
  shippingPayer: 'BUYER' | 'SELLER';
  variants: { sku: string; name: string; price: number; stock: number }[];
}

export interface Moderator {
  staffId: number;
  name: string;
  accepting: boolean;
}

export type ModerationCounts = Record<ModerationTab, number>;
