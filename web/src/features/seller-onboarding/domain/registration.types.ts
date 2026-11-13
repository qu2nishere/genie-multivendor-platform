// Đăng ký mở shop (plan G10.65; API_SPEC 9.1–9.2; prototype BecomeSeller.jsx).
export type BusinessType = 'INDIVIDUAL' | 'HOUSEHOLD' | 'BUSINESS';
export const BUSINESS_TYPES: BusinessType[] = ['INDIVIDUAL', 'HOUSEHOLD', 'BUSINESS'];

export interface ShopRegistration {
  shopName: string;
  ownerName: string;
  phone: string;
  email: string;
  mainCategoryId: number;
  businessType: BusinessType;
  acceptTerms: true;
}

export interface RegistrationChallenge {
  id: string;
  maskedTarget: string;
}

/** Lợi ích & các bước (nội dung trang — khóa i18n). */
export const BENEFITS = [
  { key: 'escrow', icon: 'shield-check', tint: 'text-mint-600', bg: 'bg-mint-50' },
  { key: 'logistics', icon: 'truck', tint: 'text-blue-600', bg: 'bg-blue-50' },
  { key: 'marketing', icon: 'megaphone', tint: 'text-flash-600', bg: 'bg-flash-50' },
  { key: 'analytics', icon: 'chart-column', tint: 'text-brand-600', bg: 'bg-brand-50' },
] as const;
export const STEPS = ['register', 'kyc', 'sell'] as const;
