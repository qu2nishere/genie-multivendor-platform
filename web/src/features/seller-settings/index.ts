// API công khai của feature "seller-settings" — ngữ cảnh shop dùng chung cho khung Seller Center
// (GET /seller/me, tìm nhanh, tài khoản nhận tiền); màn Cài đặt thêm ở G11.43.
export type {
  KycStatus,
  KycGroup,
  SellerContext,
  SellerSection,
  SearchHit,
  PayoutAccount,
} from './domain/shop.types';
export {
  kycGroup,
  sectionAllowed,
  navBadge,
  OPEN_WITHOUT_KYC,
  searchable,
} from './domain/shop.rules';
export {
  useSellerMe,
  usePayoutAccount,
  useSellerSearch,
  SELLER_ME_KEY,
} from './application/useSellerShop';
