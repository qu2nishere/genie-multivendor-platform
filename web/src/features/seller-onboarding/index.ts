// API công khai của feature "seller-onboarding" — nơi DUY NHẤT feature khác được import
export { BENEFITS, STEPS, BUSINESS_TYPES, type BusinessType } from './domain/registration.types';
export { default as BecomeSeller, type BecomeSellerProps } from './ui/BecomeSeller';
export type { KycDossier, KycStatus as KycDossierStatus } from './domain/kyc.types';
export { default as KycPage } from './ui/KycPage';
