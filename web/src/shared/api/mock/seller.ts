// Kho mock "shop của tôi" dùng chung (chỉ khi USE_MOCK): trạng thái KYC + tên shop. Mở shop (9.2) đặt
// NOT_SUBMITTED, nộp KYC đặt PENDING; khung Seller Center (10.1) đọc. Thay cho bảng shops phía server.
import { mockState } from '../mock';

export type MockKycStatus =
  | 'NOT_SUBMITTED'
  | 'PENDING'
  | 'AI_PROCESSING'
  | 'MANUAL_REVIEW'
  | 'VERIFIED'
  | 'REJECTED'
  | 'SUPPLEMENT_REQUESTED';

export interface MockSellerShop {
  id: number;
  name: string;
  kyc_status: MockKycStatus;
}

export const sellerStore = mockState<MockSellerShop>('seller.shop', () => ({
  id: 1,
  name: 'TechZone Official',
  kyc_status: 'VERIFIED',
}));
