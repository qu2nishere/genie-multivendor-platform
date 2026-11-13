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
export type {
  PaymentMethod,
  CheckoutSession,
  CheckoutStatus,
  CheckoutQuote,
  PlaceOrderInput,
  PaymentInstruction,
  VietQrInfo,
} from './domain/checkout.types';
export { PAYMENT_METHODS, HOLD_MINUTES, NOTE_MAX } from './domain/checkout.types';
export {
  holdDeadline,
  secondsLeft,
  placeLabelKey,
  isEscrowed,
  outOfStockIds,
  resultView,
} from './domain/checkout.rules';
export {
  usePlaceOrder,
  useCheckoutSession,
  useRetryPayment,
  useSettleMockPayment,
} from './application/useCheckout';
export { MOCK_VNPAY_URL } from './infrastructure/checkout.api';
export { default as CheckoutPage } from './ui/CheckoutPage';
export { default as CheckoutResult, type CheckoutResultProps } from './ui/CheckoutResult';
