// API công khai của feature "seller-promotions" (voucher shop, đăng ký Flash Sale).
export type { ShopVoucher, VoucherInput, DiscountType } from './domain/promo.types';
export { genCode, normalizeCode } from './domain/promo.rules';
export { default as PromoPage, FlashSalesRoute } from './ui/PromoPage';
export {
  VoucherFormModal,
  VoucherEditor,
  EMPTY_VOUCHER,
  type VoucherFormTarget,
  type VoucherEditorProps,
} from './ui/VoucherFormModal';
export type { VoucherValues } from './domain/promo.schema';
