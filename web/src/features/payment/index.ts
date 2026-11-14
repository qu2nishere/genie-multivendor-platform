// API công khai của feature "payment" (mở rộng ở G10.23–G10.29).
export { MONEY_STEPS, coinLeftPct, fillPct, type MoneyStep } from './domain/escrow';
export { default as MoneyRail, type MoneyRailProps } from './ui/MoneyRail';
export { HoldNote } from './ui/HoldNote';
export { CodLogo, PaymentLogo } from './ui/PaymentLogos';
export { MoneyChip, VietQrPanel, PayModal, PayGateway, type PayModalProps } from './ui/PayParts';
export {
  moneyChip,
  type PayMethod,
  type VietQrView,
  type MoneyChipTone,
} from './domain/payment.types';
