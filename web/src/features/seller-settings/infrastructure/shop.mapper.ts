import type { PayoutAccount, SearchHit, SellerContext } from '../domain/shop.types';
import type { PayoutAccountDto, SellerMeDto, SellerSearchDto } from './shop.dto';

export const toSellerContext = (d: SellerMeDto): SellerContext => ({
  shop: { id: d.shop.id, name: d.shop.name, logoUrl: d.shop.logo_url, authentic: d.shop.is_mall },
  kycStatus: d.kyc_status,
  badges: { ordersToConfirm: d.badges.orders_to_confirm, unreadChats: d.badges.unread_chats },
});

export const toSearchHit = (d: SellerSearchDto): SearchHit => ({
  orders: d.orders.map((o) => ({
    id: o.id,
    code: o.code,
    customerName: o.customer_name,
    status: o.status,
  })),
  products: d.products.map((p) => ({
    id: p.id,
    name: p.name,
    imageUrl: p.image_url,
    status: p.status,
  })),
});

export const toPayoutAccount = (d: PayoutAccountDto): PayoutAccount => ({
  bank: d.bank_name,
  holder: d.account_holder,
  numberMasked: d.account_number_masked,
  verified: d.is_verified,
});
