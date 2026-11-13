// API công khai của feature "checkout" — nơi DUY NHẤT feature khác được import.
export type { CheckoutDraft } from './domain/draft.types';
export { loadCheckoutDraft, saveCheckoutDraft, clearCheckoutDraft } from './application/draft';
export {
  shipFee,
  shipOf,
  makeShipFn,
  isFreeByShop,
  shopSubtotal,
  type ShipPolicy,
} from './domain/shipping.rules';
export { useShipPolicies } from './application/useShipping';
