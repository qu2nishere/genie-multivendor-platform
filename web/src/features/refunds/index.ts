// API công khai của feature "refunds" — nơi DUY NHẤT feature khác được import.
export type {
  RefundRequest,
  RefundStatus,
  RefundType,
  ReturnReason,
  RefundInput,
  Bank,
} from './domain/refund.types';
export { RETURN_REASONS, RETURN_WINDOW_DAYS } from './domain/refund.types';
export { needsBank, refundEstimate, evidenceMissing, isOpenRefund } from './domain/refund.rules';
export { ReturnModal, type ReturnModalProps, type ReturnOrder } from './ui/ReturnModal';
export { RefundBlock, type RefundBlockProps } from './ui/RefundBlock';
