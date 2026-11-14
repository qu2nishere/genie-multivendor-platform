// Khung Seller Center: GET /seller/me (10.1), GET /seller/search (10.1b), tài khoản nhận tiền (10.11).
import { api } from '@/shared/api/http';
import { mockDelay, USE_MOCK } from '@/shared/api/mock';
import {
  sellerChatUnreadStore,
  sellerOrdersStore,
  sellerProductsStore,
  sellerStore,
} from '@/shared/api/mock/seller';
import { searchable } from '../domain/shop.rules';
import type { PayoutAccount, SearchHit, SellerContext } from '../domain/shop.types';
import type { PayoutAccountDto, SellerMeDto, SellerSearchDto } from './shop.dto';
import { toPayoutAccount, toSearchHit, toSellerContext } from './shop.mapper';
import { BADGES, PAYOUT_ACCOUNT } from './shop.mock';

export async function getSellerMe(): Promise<SellerContext> {
  if (USE_MOCK) {
    await mockDelay(150);
    const s = sellerStore.get();
    return toSellerContext({
      shop: { id: s.id, name: s.name, logo_url: null, is_mall: s.kyc_status === 'VERIFIED' },
      kyc_status: s.kyc_status,
      badges: {
        ...BADGES,
        orders_to_confirm: sellerOrdersStore.get().filter((o) => o.status === 'NEW').length,
        unread_chats: sellerChatUnreadStore.get().count,
      },
    });
  }
  return toSellerContext(await api.get<SellerMeDto>('/seller/me'));
}

export async function searchSeller(q: string, limit = 5): Promise<SearchHit> {
  const empty: SearchHit = { orders: [], products: [] };
  if (!searchable(q)) return empty;
  if (USE_MOCK) {
    await mockDelay(120);
    if (sellerStore.get().kyc_status !== 'VERIFIED') return empty;
    const n = q.trim().toLowerCase();
    return toSearchHit({
      orders: sellerOrdersStore
        .get()
        .filter((o) => `${o.code} ${o.customer.name}`.toLowerCase().includes(n))
        .slice(0, limit)
        .map((o) => ({ id: o.id, code: o.code, customer_name: o.customer.name, status: o.status })),
      products: sellerProductsStore
        .get()
        .filter((p) => p.name.toLowerCase().includes(n))
        .slice(0, limit)
        .map((p) => ({ id: p.id, name: p.name, image_url: p.image_url, status: p.status })),
    });
  }
  return toSearchHit(
    await api.get<SellerSearchDto>('/seller/search', { query: { q: q.trim(), limit } }),
  );
}

export async function getPayoutAccount(): Promise<PayoutAccount | null> {
  if (USE_MOCK) {
    await mockDelay(120);
    return toPayoutAccount(PAYOUT_ACCOUNT);
  }
  return toPayoutAccount(await api.get<PayoutAccountDto>('/seller/payout-account'));
}
