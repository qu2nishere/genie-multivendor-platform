import { KYC_TABS, SUPPLEMENT_DOCS } from '../domain/kyc.rules';
import type { KycCounts, KycDetail, KycDocType, KycSubmission } from '../domain/kyc.types';
import type { KycCountsDto, KycDetailDto, KycSubmissionDto } from './kyc.dto';

const isDocType = (s: string): s is KycDocType => (SUPPLEMENT_DOCS as string[]).includes(s);

export const toKycSubmission = (d: KycSubmissionDto): KycSubmission => ({
  id: d.id,
  code: d.code,
  shop: { id: d.shop.id, name: d.shop.name, logoUrl: d.shop.logo_url },
  ownerName: d.owner_name,
  businessType: d.business_type,
  status: d.status,
  submittedAt: d.submitted_at,
});

export const toKycDetail = (d: KycDetailDto): KycDetail => ({
  ...toKycSubmission(d),
  riskLevel: d.risk_level,
  riskScore: d.risk_score,
  randomAudit: d.is_random_audit,
  documents: d.documents.flatMap((x) =>
    isDocType(x.type) ? [{ type: x.type, url: x.url, contentType: x.content_type }] : [],
  ),
  ai: d.ai_result
    ? {
        overall: d.ai_result.overall,
        checks: d.ai_result.checks,
        faceMatchScore: d.ai_result.face_match_score,
        errors: d.ai_result.errors,
      }
    : null,
  declared: {
    identityNumberMasked: d.declared.identity_number_masked,
    representative: d.declared.representative,
    taxCode: d.declared.tax_code,
  },
  shopCommissionRate: d.shop_commission_rate,
  rejectionReason: d.rejection_reason,
  supplement: d.supplement
    ? {
        documents: d.supplement.documents.filter(isDocType),
        note: d.supplement.note,
        dueAt: d.supplement.due_at,
      }
    : null,
});

export const toKycCounts = (d: Partial<KycCountsDto>): KycCounts =>
  Object.fromEntries(
    KYC_TABS.map((t) => [
      t,
      t === 'ALL'
        ? (d.ALL ??
          (d.PENDING ?? 0) + (d.SUPPLEMENT_REQUESTED ?? 0) + (d.VERIFIED ?? 0) + (d.REJECTED ?? 0))
        : (d[t] ?? 0),
    ]),
  ) as KycCounts;
