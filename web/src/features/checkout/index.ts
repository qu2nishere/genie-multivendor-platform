// API công khai của feature "checkout" — nơi DUY NHẤT feature khác được import.
export type { CheckoutDraft } from './domain/draft.types';
export { loadCheckoutDraft, saveCheckoutDraft, clearCheckoutDraft } from './application/draft';
