import { describe, expect, it } from 'vitest';
import { bannerOf, canEdit, kycFileIssue, missingDocs, requiredDocs, sizeLabel } from './kyc.rules';
import { kycInfoSchema } from './kyc.schema';

describe('kyc.rules', () => {
  it('giấy tờ theo loại hình', () => {
    expect(requiredDocs('INDIVIDUAL')).toEqual(['ID_FRONT', 'ID_BACK', 'SELFIE']);
    expect(requiredDocs('BUSINESS')).toContain('BUSINESS_LICENSE');
    expect(
      missingDocs({
        businessType: 'HOUSEHOLD',
        documents: { ID_FRONT: { fileName: 'a.jpg', sizeBytes: 1, objectKey: null } },
      }),
    ).toEqual(['ID_BACK', 'BUSINESS_LICENSE', 'SELFIE']);
  });

  it('trạng thái: gom chờ duyệt, sửa được khi chưa nộp / từ chối / bổ sung', () => {
    expect(bannerOf('AI_PROCESSING')).toBe('PENDING');
    expect(canEdit('PENDING')).toBe(false);
    expect(canEdit('VERIFIED')).toBe(false);
    expect(canEdit('SUPPLEMENT_REQUESTED')).toBe(true);
  });

  it('kiểm tệp KYC', () => {
    expect(
      kycFileIssue({ type: 'application/pdf', size: 3_000_000 }, 'BUSINESS_LICENSE'),
    ).toBeNull();
    expect(kycFileIssue({ type: 'application/pdf', size: 3_000_000 }, 'SELFIE')).toBe('type');
    expect(kycFileIssue({ type: 'image/webp', size: 300_000 }, 'ID_FRONT')).toBe('type');
    expect(kycFileIssue({ type: 'image/jpeg', size: 11 * 1048576 }, 'ID_FRONT')).toBe('size');
    expect(kycFileIssue({ type: 'image/png', size: 10_000 }, 'ID_BACK')).toBe('blurry');
    expect(sizeLabel(2_202_009, 'vi')).toBe('2,1 MB');
    expect(sizeLabel(812_004, 'en')).toBe('793 KB');
  });

  it('CCCD 12 số, MST với doanh nghiệp', () => {
    const v = {
      shopName: 'TechZone Official',
      businessType: 'BUSINESS' as const,
      identityNumber: '079096001234',
      taxCode: '0312345678',
      representative: 'Đặng Quốc Huy',
    };
    expect(kycInfoSchema.safeParse(v).success).toBe(true);
    expect(kycInfoSchema.safeParse({ ...v, identityNumber: '07909600123' }).success).toBe(false);
    expect(kycInfoSchema.safeParse({ ...v, taxCode: '' }).success).toBe(false);
    expect(kycInfoSchema.safeParse({ ...v, businessType: 'INDIVIDUAL', taxCode: '' }).success).toBe(
      true,
    );
  });
});
