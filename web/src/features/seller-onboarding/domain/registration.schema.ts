// Form "Đăng ký mở Shop" (zod) — thông điệp là khóa i18n.
import { z } from 'zod';

const PHONE_RE = /^0\d{9,10}$/;
export const registrationSchema = z.object({
  shopName: z
    .string()
    .trim()
    .min(3, 'seller-onboarding.landing.err.shop')
    .max(60, 'seller-onboarding.landing.err.shop_long'),
  ownerName: z.string().trim().min(2, 'seller-onboarding.landing.err.owner'),
  phone: z
    .string()
    .transform((p) => p.replace(/[\s.-]/g, ''))
    .refine((p) => PHONE_RE.test(p), 'account.address.err.phone'),
  email: z.string().trim().toLowerCase().email('auth.err.email_invalid'),
  mainCategoryId: z.number().int().positive('seller-onboarding.landing.err.category'),
  businessType: z.enum(['INDIVIDUAL', 'HOUSEHOLD', 'BUSINESS']),
  acceptTerms: z.literal(true, {
    errorMap: () => ({ message: 'seller-onboarding.landing.err.terms' }),
  }),
});
export type RegistrationValues = z.infer<typeof registrationSchema>;
