// Form "Thông tin người bán" của hồ sơ KYC (zod): CCCD đúng 12 số, MST bắt buộc với Hộ KD / Doanh nghiệp.
import { z } from 'zod';

export const kycInfoSchema = z
  .object({
    shopName: z
      .string()
      .trim()
      .min(3, 'seller-onboarding.landing.err.shop')
      .max(60, 'seller-onboarding.landing.err.shop_long'),
    businessType: z.enum(['INDIVIDUAL', 'HOUSEHOLD', 'BUSINESS']),
    identityNumber: z.string().regex(/^\d{12}$/, 'seller-onboarding.kyc.err.cccd'),
    taxCode: z.string().trim(),
    representative: z.string().trim().min(2, 'seller-onboarding.kyc.err.rep'),
  })
  .refine((v) => v.businessType === 'INDIVIDUAL' || /^\d{10}(-\d{3})?$/.test(v.taxCode), {
    path: ['taxCode'],
    message: 'seller-onboarding.kyc.err.tax',
  });
export type KycInfoValues = z.infer<typeof kycInfoSchema>;
