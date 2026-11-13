import { describe, expect, it } from 'vitest';
import { kycGroup, navBadge, searchable, sectionAllowed } from './shop.rules';
import type { SellerContext } from './shop.types';

const ctx = (kyc: SellerContext['kycStatus']): SellerContext => ({
  shop: { id: 1, name: 'TechZone Official', logoUrl: null, authentic: true },
  kycStatus: kyc,
  badges: { ordersToConfirm: 2, unreadChats: 5 },
});

describe('shop.rules', () => {
  it('gom trạng thái chờ', () => {
    expect(kycGroup('PENDING')).toBe('WAITING');
    expect(kycGroup('AI_PROCESSING')).toBe('WAITING');
    expect(kycGroup('MANUAL_REVIEW')).toBe('WAITING');
    expect(kycGroup('SUPPLEMENT_REQUESTED')).toBe('SUPPLEMENT');
  });
  it('chưa VERIFIED chỉ mở KYC + Cài đặt', () => {
    expect(sectionAllowed('orders', 'PENDING')).toBe(false);
    expect(sectionAllowed('kyc', 'REJECTED')).toBe(true);
    expect(sectionAllowed('settings', 'NOT_SUBMITTED')).toBe(true);
    expect(sectionAllowed('wallet', 'VERIFIED')).toBe(true);
  });
  it('huy hiệu nav', () => {
    expect(navBadge('orders', ctx('VERIFIED'))).toBe(2);
    expect(navBadge('chat', ctx('VERIFIED'))).toBe(5);
    expect(navBadge('chat', ctx('PENDING'))).toBe(0);
    expect(navBadge('products', ctx('VERIFIED'))).toBe(0);
  });
  it('tìm từ 2 ký tự', () => {
    expect(searchable(' a ')).toBe(false);
    expect(searchable('TR')).toBe(true);
  });
});
