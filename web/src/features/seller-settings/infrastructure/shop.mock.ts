// Dữ liệu mẫu khung Seller Center (prototype vendor/data.jsx ORDERS, Products.jsx PRODUCTS, BANK).
import type { PayoutAccountDto, SellerSearchDto } from './shop.dto';

const img = (n: string) => `/img/products/${n}.jpg`;
const code = (id: number) => `TR-20260608-${String(id).padStart(6, '0')}`;

/** Chỉ mục tìm nhanh: đơn + sản phẩm của shop. */
export const SEARCH_ORDERS: SellerSearchDto['orders'] = (
  [
    [146, 'Trần Thị Mai', 'NEW'],
    [145, 'Đinh Gia Bảo', 'NEW'],
    [141, 'Trần Thị Mai', 'CONFIRMED'],
    [140, 'Nguyễn Văn An', 'PREPARING'],
    [138, 'Lê Hoàng Phúc', 'DELIVERING'],
    [136, 'Đỗ Minh Khang', 'DELIVERING'],
    [134, 'Bùi Thu Hằng', 'DELIVERING'],
    [139, 'Phạm Thu Hà', 'DELIVERED'],
    [135, 'Vũ Minh Quân', 'CANCELLED'],
  ] as const
).map(([id, customer_name, status]) => ({ id, code: code(id), customer_name, status }));

export const SEARCH_PRODUCTS: SellerSearchDto['products'] = (
  [
    [1, 'Tai nghe Bluetooth ANC Pro 5', 'headphones', 'PUBLISHED'],
    [2, 'Đèn bàn LED cảm ứng 3 chế độ', 'lamp', 'PENDING_APPROVAL'],
    [3, 'Sạc nhanh 65W (thiếu giấy tờ)', 'phone', 'REJECTED'],
    [4, 'Đồng hồ thông minh Watch S2', 'watch', 'PUBLISHED'],
    [5, 'Loa bluetooth mini IPX7', 'speaker', 'PUBLISHED'],
    [6, 'Bàn phím cơ RGB switch blue', 'keyboard', 'DRAFT'],
    [7, 'Camera Mirrorless X-100 (bản mới)', 'camera', 'DRAFT'],
  ] as const
).map(([id, name, pic, status]) => ({ id, name, image_url: img(pic), status }));

export const PAYOUT_ACCOUNT: PayoutAccountDto = {
  bank_name: 'Vietcombank',
  account_holder: 'DANG QUOC HUY',
  account_number_masked: '0071******712',
  is_verified: true,
};

/** Tên trên CCCD đã duyệt — chủ tài khoản nhận tiền phải trùng (UC-VND-07). */
export const KYC_NAME = 'Đặng Quốc Huy';

export const BADGES = { orders_to_confirm: 2, unread_chats: 5 };
