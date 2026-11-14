// DTO snake_case (API_SPEC 10.1, 10.1b, 10.11 tài khoản nhận tiền).
import type { MockKycStatus } from '@/shared/api/mock/seller';

export interface SellerMeDto {
  shop: { id: number; name: string; logo_url: string | null; is_mall: boolean };
  kyc_status: MockKycStatus;
  badges: { orders_to_confirm: number; unread_chats: number };
}

export interface SellerSearchDto {
  orders: { id: number; code: string; customer_name: string; status: string }[];
  products: { id: number; name: string; image_url: string; status: string }[];
}

export interface PayoutAccountDto {
  bank_name: string;
  account_holder: string;
  account_number_masked: string;
  is_verified: boolean;
}
