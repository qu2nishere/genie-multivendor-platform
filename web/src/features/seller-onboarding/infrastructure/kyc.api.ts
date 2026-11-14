// Hồ sơ KYC (API_SPEC 10.12): GET /seller/kyc, PUT /seller/kyc/draft, POST /seller/kyc/submissions,
// POST /seller/kyc/manual-review-requests. Mock: hồ sơ giữ trong phiên tab; nộp → kho shop dùng chung
// chuyển PENDING (khung Seller Center hiện "KYC đang chờ", các mục khác khóa).
import { api } from '@/shared/api/http';
import { mockDelay, mockFail, mockState, USE_MOCK } from '@/shared/api/mock';
import { sellerStore } from '@/shared/api/mock/seller';
import { missingDocs } from '../domain/kyc.rules';
import type { KycDocInfo, KycDocKey, KycDossier, KycForm, KycStatus } from '../domain/kyc.types';
import type { BusinessType } from '../domain/registration.types';

interface DocDto {
  file_name: string;
  size_bytes: number;
}
interface KycDto {
  status: KycStatus;
  shop_name: string;
  business_type: BusinessType;
  identity_number: string;
  tax_code: string | null;
  representative: string;
  documents: Record<KycDocKey, DocDto | null>;
  biometric_consent: boolean;
  rejection_reason: string | null;
  supplement: { documents: KycDocKey[]; note: string; deadline_at: string } | null;
  manual_review_requested: boolean;
  submitted_at: string | null;
}

const KEYS: KycDocKey[] = ['ID_FRONT', 'ID_BACK', 'BUSINESS_LICENSE', 'SELFIE'];

const toDossier = (d: KycDto): KycDossier => ({
  status: d.status,
  shopName: d.shop_name,
  businessType: d.business_type,
  identityNumber: d.identity_number,
  taxCode: d.tax_code ?? '',
  representative: d.representative,
  documents: Object.fromEntries(
    KEYS.filter((k) => d.documents[k]).map((k) => [
      k,
      {
        fileName: d.documents[k]?.file_name ?? '',
        sizeBytes: d.documents[k]?.size_bytes ?? 0,
        objectKey: null,
      },
    ]),
  ),
  biometricConsent: d.biometric_consent,
  rejectionReason: d.rejection_reason,
  supplement: d.supplement && {
    documents: d.supplement.documents,
    note: d.supplement.note,
    deadlineAt: d.supplement.deadline_at,
  },
  manualReviewRequested: d.manual_review_requested,
  submittedAt: d.submitted_at,
});

const toBody = (f: KycForm) => ({
  shop_name: f.shopName.trim(),
  business_type: f.businessType,
  identity_number: f.identityNumber,
  tax_code: f.businessType === 'INDIVIDUAL' ? null : f.taxCode.trim(),
  representative: f.representative.trim(),
  // Chỉ gửi giấy tờ vừa tải; giấy tờ đã có từ lần trước giữ bản cũ (bỏ khóa)
  document_object_keys: Object.fromEntries(
    KEYS.flatMap((k) => (f.documents[k]?.objectKey ? [[k, f.documents[k]?.objectKey]] : [])),
  ),
  biometric_consent: f.biometricConsent,
});

// ---- Mock ----
const filed: Record<KycDocKey, DocDto> = {
  ID_FRONT: { file_name: 'cccd-truoc.jpg', size_bytes: 2_202_009 },
  ID_BACK: { file_name: 'cccd-sau.jpg', size_bytes: 1_992_294 },
  BUSINESS_LICENSE: { file_name: 'gpkd.pdf', size_bytes: 3_565_158 },
  SELFIE: { file_name: 'selfie.jpg', size_bytes: 812_004 },
};
const empty: Record<KycDocKey, null> = {
  ID_FRONT: null,
  ID_BACK: null,
  BUSINESS_LICENSE: null,
  SELFIE: null,
};

const db = mockState<KycDto | null>('seller.kyc', () => null);

/** Hồ sơ theo trạng thái của kho shop (tài khoản mẫu đã VERIFIED; shop mới mở = chưa nộp). */
function current(): KycDto {
  const shop = sellerStore.get();
  const saved = db.get();
  if (saved && saved.status === shop.kyc_status)
    return { ...saved, shop_name: saved.shop_name || shop.name };
  const done = shop.kyc_status !== 'NOT_SUBMITTED';
  return {
    status: shop.kyc_status,
    shop_name: shop.name,
    business_type: done ? 'BUSINESS' : 'INDIVIDUAL',
    identity_number: done ? '079096001234' : '',
    tax_code: done ? '0312345678' : null,
    representative: done ? 'Đặng Quốc Huy' : '',
    documents: done ? filed : empty,
    biometric_consent: done,
    rejection_reason: shop.kyc_status === 'REJECTED' ? 'Ảnh giấy tờ không hợp lệ' : null,
    supplement:
      shop.kyc_status === 'SUPPLEMENT_REQUESTED'
        ? {
            documents: ['BUSINESS_LICENSE'],
            note: 'Vui lòng cung cấp CCCD hoặc Giấy phép kinh doanh còn hạn để chúng tôi xác minh danh tính của bạn.',
            deadline_at: '2026-06-15T10:00:00+07:00',
          }
        : null,
    manual_review_requested: false,
    submitted_at: done ? '2026-06-08T10:24:00+07:00' : null,
  };
}

function merge(f: KycForm, base: KycDto, status: KycStatus): KycDto {
  const docs = { ...base.documents };
  for (const k of KEYS) {
    const d = f.documents[k];
    docs[k] = d ? { file_name: d.fileName, size_bytes: d.sizeBytes } : null;
  }
  return {
    ...base,
    status,
    shop_name: f.shopName.trim(),
    business_type: f.businessType,
    identity_number: f.identityNumber,
    tax_code: f.businessType === 'INDIVIDUAL' ? null : f.taxCode.trim(),
    representative: f.representative.trim(),
    documents: docs,
    biometric_consent: f.biometricConsent,
  };
}

export async function getKyc(): Promise<KycDossier> {
  if (USE_MOCK) {
    await mockDelay(200);
    return toDossier(current());
  }
  return toDossier(await api.get<KycDto>('/seller/kyc'));
}

export async function saveKycDraft(f: KycForm): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(250);
    const base = current();
    db.set(merge(f, base, base.status));
    return;
  }
  await api.put('/seller/kyc/draft', toBody(f));
}

/** Gửi duyệt / gửi lại → PENDING (AI Worker xử lý bất đồng bộ). */
export async function submitKyc(f: KycForm): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(500);
    const base = current();
    if (!['NOT_SUBMITTED', 'REJECTED', 'SUPPLEMENT_REQUESTED'].includes(base.status))
      mockFail('GNE-VND-4095');
    const missing = missingDocs(f);
    if (missing.length) mockFail('GNE-VND-4223', { details: { missing: missing.join(', ') } });
    const next = merge(f, base, 'PENDING');
    db.set({
      ...next,
      rejection_reason: null,
      supplement: null,
      submitted_at: new Date().toISOString(),
    });
    sellerStore.set({ ...sellerStore.get(), name: next.shop_name, kyc_status: 'PENDING' });
    return;
  }
  await api.post('/seller/kyc/submissions', toBody(f));
}

export async function requestManualReview(): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(250);
    const base = current();
    if (base.manual_review_requested) mockFail('GNE-VND-4096');
    db.set({ ...base, manual_review_requested: true });
    return;
  }
  await api.post('/seller/kyc/manual-review-requests');
}

export type { KycDocInfo };
