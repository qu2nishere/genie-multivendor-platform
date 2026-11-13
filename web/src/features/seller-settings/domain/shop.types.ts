// Ngữ cảnh shop của Seller Center (API_SPEC 10.1, 10.1b; prototype vendor/data.jsx SHOP, BANK, KYC_NAME).
export type KycStatus =
  | 'NOT_SUBMITTED'
  | 'PENDING'
  | 'AI_PROCESSING'
  | 'MANUAL_REVIEW'
  | 'VERIFIED'
  | 'REJECTED'
  | 'SUPPLEMENT_REQUESTED';

/** Nhóm hiển thị: client gom PENDING / AI_PROCESSING / MANUAL_REVIEW thành "đang chờ". */
export type KycGroup = 'VERIFIED' | 'WAITING' | 'NOT_SUBMITTED' | 'REJECTED' | 'SUPPLEMENT';

export interface SellerContext {
  shop: { id: number; name: string; logoUrl: string | null; authentic: boolean };
  kycStatus: KycStatus;
  badges: { ordersToConfirm: number; unreadChats: number };
}

export type SellerSection =
  | 'overview'
  | 'orders'
  | 'products'
  | 'wallet'
  | 'kyc'
  | 'catalog'
  | 'promo'
  | 'chat'
  | 'reviews'
  | 'stats'
  | 'settings';

export interface SearchHit {
  orders: { id: number; code: string; customerName: string; status: string }[];
  products: { id: number; name: string; imageUrl: string; status: string }[];
}

/** Tài khoản nhận tiền (Cài đặt ↔ Rút tiền). */
export interface PayoutAccount {
  bank: string;
  holder: string;
  /** Số tài khoản đã che. */
  numberMasked: string;
  verified: boolean;
}
