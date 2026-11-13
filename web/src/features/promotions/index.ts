// API công khai của feature "promotions" — nơi DUY NHẤT feature khác được import.
export type { Banner, BannerPosition, BannerState } from './domain/banner.types';
export { localDay, bannerState, liveBanners, dealTwo, SLIDE_MS } from './domain/banner.rules';
export type { Voucher, VoucherKind, VoucherType, VoucherStatus } from './domain/voucher.types';
export { listLiveBanners, trackBanner } from './infrastructure/banner.api';
export {
  listMyVouchers,
  getVouchersByCodes,
  saveVoucher,
  listShopVouchers,
} from './infrastructure/voucher.api';
export { useVouchersByCodes, useMyVouchers, useSaveVoucher } from './application/useVouchers';
export {
  default as HomeBanners,
  BannerCarousel,
  BannerPair,
  BannerSlot,
  BannerLink,
  BannerPicture,
} from './ui/banner/Banners';
export { default as BannerPopup } from './ui/banner/BannerPopup';
export { default as SaveVoucher, type SaveVoucherItem } from './ui/SaveVoucher';
export {
  FLASH_SLOT_HOURS,
  flashSlots,
  liveSlot,
  pickSlot,
  secondsUntilHour,
  slotLabel,
  type FlashSlot,
  type FlashSlotState,
} from './domain/flash.rules';
export type { Campaign } from './domain/campaign.types';
export { getCampaign, listCampaignSlugs } from './infrastructure/campaign.api';
export type {
  PricingLine,
  PricingContext,
  PriceBreakdown,
  ShipFn,
  VoucherSelection,
  IneligibleReason,
} from './domain/pricing.types';
export {
  priceCart,
  bestVouchers,
  voucherCheck,
  voucherDiscount,
  voucherLines,
  eligibleBase,
  emptySelection,
  pruneVoucherSelection,
  selectionToCodes,
  selectionFromCodes,
  extraSaving,
  usableVouchers,
  POINTS_RATIO,
} from './domain/pricing.rules';
export { findVoucherByCode } from './infrastructure/voucher.api';
export { useFindVoucher } from './application/useVouchers';
export { VoucherPicker, type VoucherPickerProps } from './ui/VoucherPicker';
export { default as VouchersPanel } from './ui/VouchersPanel';
export { useRemoveVoucher } from './application/useVouchers';
