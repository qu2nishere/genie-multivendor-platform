// API công khai của feature "seller-promotions" (voucher shop, đăng ký Flash Sale).
export type { ShopVoucher, VoucherInput, DiscountType } from './domain/promo.types';
export { genCode, normalizeCode } from './domain/promo.rules';
export { default as PromoPage, FlashSalesRoute } from './ui/PromoPage';
export { VoucherFormModal, type VoucherFormTarget } from './ui/VoucherFormModal';
