// Đơn hàng của shop (API_SPEC 10.3). Mock đọc kho dùng chung shared/api/mock/seller.ts.
import { request } from '@/shared/api/http';
import { mockDelay, paginatePage, USE_MOCK } from '@/shared/api/mock';
import { sellerOrdersStore } from '@/shared/api/mock/seller';
import type { Page } from '@/shared/api/types';
import { inTab } from '../domain/order.rules';
import type { OrderListQuery, OrderTab, SellerOrder } from '../domain/order.types';
import type { OrderCountsDto, SellerOrderDto } from './order.dto';
import { toSellerOrder } from './order.mapper';

export interface SellerOrderPage extends Page<SellerOrder> {
  counts: Record<OrderTab, number>;
}

/** "Bây giờ" để tính "hôm nay / hôm qua": mock đứng ở ngày chốt số liệu của bản mẫu (08/06/2026). */
export const sellerNow = (): number =>
  USE_MOCK ? Date.parse('2026-06-08T18:00:00+07:00') : Date.now();

const TABS: OrderTab[] = ['ALL', 'NEW', 'TO_PREPARE', 'DELIVERING', 'DELIVERED', 'CANCELLED'];

export async function listSellerOrders(q: OrderListQuery = {}): Promise<SellerOrderPage> {
  const tab = q.tab ?? 'ALL';
  if (USE_MOCK) {
    await mockDelay(200);
    const all = sellerOrdersStore.get().map(toSellerOrder);
    const needle = q.q?.trim().toLowerCase() ?? '';
    const found = needle
      ? all.filter((o) => `${o.code} ${o.customer.name}`.toLowerCase().includes(needle))
      : all;
    const sorted = [...found].sort((a, b) =>
      q.sort === 'OLDEST'
        ? a.createdAt.localeCompare(b.createdAt)
        : b.createdAt.localeCompare(a.createdAt),
    );
    const page = paginatePage(
      sorted.filter((o) => inTab(o, tab)),
      { page: q.page ?? 1, size: q.size ?? 20 },
    );
    const counts = Object.fromEntries(
      TABS.map((t) => [t, found.filter((o) => inTab(o, t)).length]),
    ) as Record<OrderTab, number>;
    return { ...page, counts };
  }
  const res = await request<SellerOrderDto[]>('/seller/orders', {
    query: {
      tab: tab === 'ALL' ? undefined : tab,
      q: q.q?.trim() || undefined,
      sort: q.sort,
      page: q.page,
      size: q.size,
    },
  });
  const p = res.pagination && 'page' in res.pagination ? res.pagination : null;
  const counts = (res.summary?.counts ?? {}) as Partial<OrderCountsDto>;
  return {
    items: res.data.map(toSellerOrder),
    page: p?.page ?? 1,
    size: p?.size ?? res.data.length,
    total_items: p?.total_items ?? res.data.length,
    total_pages: p?.total_pages ?? 1,
    counts: Object.fromEntries(TABS.map((t) => [t, counts[t] ?? 0])) as Record<OrderTab, number>,
  };
}
