// Quy tắc khung Seller Center: nhóm KYC, mục nào mở khi chưa VERIFIED (API_SPEC 0.9), huy hiệu nav.
import type { KycGroup, KycStatus, SellerContext, SellerSection } from './shop.types';

export function kycGroup(s: KycStatus): KycGroup {
  switch (s) {
    case 'VERIFIED':
      return 'VERIFIED';
    case 'NOT_SUBMITTED':
      return 'NOT_SUBMITTED';
    case 'REJECTED':
      return 'REJECTED';
    case 'SUPPLEMENT_REQUESTED':
      return 'SUPPLEMENT';
    default:
      return 'WAITING';
  }
}

/** Chưa VERIFIED thì khóa mọi mục trừ Hồ sơ KYC và Cài đặt. */
export const OPEN_WITHOUT_KYC: readonly SellerSection[] = ['kyc', 'settings'];
export const sectionAllowed = (s: SellerSection, kyc: KycStatus): boolean =>
  kyc === 'VERIFIED' || OPEN_WITHOUT_KYC.includes(s);

/** Số trên mục nav (0 hoặc mục bị khóa → không hiện). */
export function navBadge(s: SellerSection, ctx: SellerContext): number {
  if (!sectionAllowed(s, ctx.kycStatus)) return 0;
  if (s === 'orders') return ctx.badges.ordersToConfirm;
  if (s === 'chat') return ctx.badges.unreadChats;
  return 0;
}

/** Tìm nhanh chỉ chạy từ 2 ký tự (10.1b). */
export const SEARCH_MIN = 2;
export const searchable = (q: string): boolean => q.trim().length >= SEARCH_MIN;
