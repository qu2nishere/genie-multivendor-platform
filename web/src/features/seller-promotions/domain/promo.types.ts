// Khuyến mãi của shop (API_SPEC 10.7; prototype VendorPages.jsx Promo, VoucherFormModal).
export type DiscountType = 'PERCENTAGE' | 'FIXED' | 'FREESHIP';
export type VoucherStatus = 'ACTIVE' | 'SCHEDULED' | 'ENDED' | 'DISABLED';
export type VoucherScope = 'ALL' | 'CATEGORY' | 'PRODUCT';

export interface ShopVoucher {
  id: number;
  code: string;
  discountType: DiscountType;
  discountValue: number | null;
  maxDiscount: number | null;
  minOrderValue: number;
  usage: { used: number; limit: number };
  status: VoucherStatus;
  validTo: string;
}

export interface VoucherSummary {
  activeCount: number;
  usageThisMonth: number;
  revenueThisMonth: number;
}

/** `$ShopVoucherInput` — ngày dạng "YYYY-MM-DD" ở form, đổi sang ISO khi gửi. */
export interface VoucherInput {
  code: string;
  discountType: DiscountType;
  discountValue: number | null;
  maxDiscount: number | null;
  minOrderValue: number | null;
  usageLimit: number | null;
  usagePerUser: number | null;
  budget: number | null;
  validFrom: string;
  validTo: string;
  scope: VoucherScope;
  categoryIds: number[];
  productIds: number[];
  excludedCategoryIds: number[];
}

export interface FlashCampaign {
  id: number;
  name: string;
  startsAt: string;
  endsAt: string;
  registrationDeadline: string;
  maxItemsPerShop: number;
  registeredCount: number;
}

export interface EligibleProduct {
  productId: number;
  name: string;
  referencePrice: number;
  maxFlashPrice: number;
  stock: number;
}

export interface FlashItem {
  id: number;
  productId: number;
  name: string;
  flashPrice: number;
  flashStock: number;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
}
