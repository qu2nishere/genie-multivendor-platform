// Chi tiết hồ sơ KYC (prototype admin/KYCReview.jsx: KYC, OCR theo mức rủi ro) — phần danh sách và
// trạng thái nằm ở kho dùng chung shared/api/mock/admin.ts (adminKycStore).
import { mockState } from '@/shared/api/mock';
import type { KycDetailDto, KycSubmissionDto } from './kyc.dto';

export type KycExtraDto = Omit<KycDetailDto, keyof KycSubmissionDto>;

const CARD = '/img/mock/kyc-card.svg';
const SELFIE = '/img/mock/kyc-selfie.svg';
const doc = (type: string, url = CARD) => ({ type, url, content_type: 'image/svg+xml' });

const OK = { qr_code: true, national_emblem: true, mrz: true, fingerprint: true, face_match: true };

const EXTRA: Record<number, KycExtraDto> = {
  1042: {
    risk_level: 'LOW',
    risk_score: 14,
    is_random_audit: false,
    documents: [doc('ID_FRONT'), doc('ID_BACK'), doc('BUSINESS_LICENSE'), doc('SELFIE', SELFIE)],
    ai_result: { overall: 'PASS', checks: OK, face_match_score: 0.96, errors: [] },
    declared: {
      identity_number_masked: '0790•••••042',
      representative: 'Phạm Thị Lan',
      tax_code: '8012345678',
    },
    shop_commission_rate: null,
    rejection_reason: null,
    supplement: null,
  },
  1041: {
    risk_level: 'MEDIUM',
    risk_score: 42,
    is_random_audit: true,
    documents: [
      doc('ID_FRONT'),
      doc('ID_BACK'),
      doc('BUSINESS_REGISTRATION'),
      doc('TAX_NOTICE'),
      doc('SELFIE', SELFIE),
    ],
    ai_result: {
      overall: 'PARTIAL',
      checks: { ...OK, mrz: false },
      face_match_score: 0.91,
      errors: ['MRZ mờ, không đọc được 2 ký tự cuối.'],
    },
    declared: {
      identity_number_masked: '0790•••••041',
      representative: 'Trần Quốc Bảo',
      tax_code: '0312345678',
    },
    shop_commission_rate: null,
    rejection_reason: null,
    supplement: null,
  },
  1039: {
    risk_level: 'LOW',
    risk_score: 9,
    is_random_audit: false,
    documents: [doc('ID_FRONT'), doc('ID_BACK'), doc('SELFIE', SELFIE)],
    ai_result: { overall: 'PASS', checks: OK, face_match_score: 0.94, errors: [] },
    declared: {
      identity_number_masked: '0790•••••039',
      representative: 'Nguyễn Thị Hoa',
      tax_code: null,
    },
    shop_commission_rate: null,
    rejection_reason: null,
    supplement: null,
  },
  1037: {
    risk_level: 'LOW',
    risk_score: 21,
    is_random_audit: false,
    documents: [doc('ID_FRONT'), doc('BUSINESS_LICENSE')],
    ai_result: { overall: 'PASS', checks: OK, face_match_score: 0.92, errors: [] },
    declared: {
      identity_number_masked: '0790•••••037',
      representative: 'Đỗ Minh Khang',
      tax_code: '8098765432',
    },
    shop_commission_rate: null,
    rejection_reason: null,
    supplement: {
      documents: ['ID_FRONT'],
      note: 'Đổi tài khoản nhận tiền sang tên người khác, cần bổ sung giấy tờ chủ tài khoản.',
      due_at: '2026-06-14T14:18:00+07:00',
    },
  },
  1033: {
    risk_level: 'HIGH',
    risk_score: 84,
    is_random_audit: false,
    documents: [doc('ID_FRONT')],
    ai_result: {
      overall: 'FAIL',
      checks: {
        qr_code: false,
        national_emblem: true,
        mrz: false,
        fingerprint: false,
        face_match: false,
      },
      face_match_score: 0.41,
      errors: ['Không đọc được QR trên CCCD.', 'Độ khớp khuôn mặt 41% (yêu cầu ≥ 85%).'],
    },
    declared: {
      identity_number_masked: '0790•••••033',
      representative: 'Lý Văn Tèo',
      tax_code: null,
    },
    shop_commission_rate: null,
    rejection_reason: 'Ảnh giấy tờ mờ, không đọc được số CCCD',
    supplement: null,
  },
};

/** Phần chi tiết có thể đổi khi duyệt (lý do từ chối, yêu cầu bổ sung, hoa hồng riêng). */
export const kycDetailStore = mockState<Record<number, KycExtraDto>>(
  'admin.kycDetail',
  () => EXTRA,
);
