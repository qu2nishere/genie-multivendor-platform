// Dữ liệu mẫu khung Seller Center (prototype vendor/data.jsx BANK, KYC_NAME). Đơn / sản phẩm để tìm
// nhanh lấy từ kho mock dùng chung shared/api/mock/seller.ts.
import type { PayoutAccountDto } from './shop.dto';

export const PAYOUT_ACCOUNT: PayoutAccountDto = {
  bank_name: 'Vietcombank',
  account_holder: 'DANG QUOC HUY',
  account_number_masked: '0071******712',
  is_verified: true,
};

/** Tên trên CCCD đã duyệt — chủ tài khoản nhận tiền phải trùng (UC-VND-07). */
export const KYC_NAME = 'Đặng Quốc Huy';

export const BADGES = { orders_to_confirm: 2, unread_chats: 5 };
