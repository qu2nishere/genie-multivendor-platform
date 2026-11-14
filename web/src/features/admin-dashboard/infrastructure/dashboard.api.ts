// Tổng quan sàn (API_SPEC 11.2). Mock: số tháng / biểu đồ / bảng shop theo prototype; tiền đang chờ
// xử lý đếm từ kho dùng chung (lệnh rút, phiên COD, tranh chấp) để khớp các màn tương ứng.
import { api, getPage } from '@/shared/api/http';
import { mockDelay, paginatePage, USE_MOCK } from '@/shared/api/mock';
import { adminCodStore, adminDisputesStore, adminWithdrawalsStore } from '@/shared/api/mock/admin';
import { can } from '@/shared/auth/permissions';
import { currentUser } from '@/shared/auth/session';
import type { Page } from '@/shared/api/types';
import { GROWING_PAGE_SIZE, netProfit } from '../domain/dashboard.rules';
import type {
  DashboardFinance,
  GmvChart,
  GmvRange,
  GrowingShop,
  MonthKey,
  Operations,
} from '../domain/dashboard.types';
import type {
  DashboardFinanceDto,
  GmvChartDto,
  GrowingShopDto,
  OperationsDto,
} from './dashboard.dto';
import { toFinance, toGmvChart, toGrowingShop, toOperations } from './dashboard.mapper';
import { CHARTS, HOLDINGS, MONTHLY, MONTHS, OPERATIONS, SHOPS } from './dashboard.mock';

const sum = (xs: number[]) => xs.reduce((n, x) => n + x, 0);

export async function getDashboardFinance(month?: MonthKey): Promise<DashboardFinance> {
  if (USE_MOCK) {
    await mockDelay();
    const m = month && MONTHLY[month] ? month : (MONTHS[0] ?? '2026-06');
    const cur = MONTHLY[m] ?? MONTHLY['2026-06']!;
    const prevKey = MONTHS[MONTHS.indexOf(m) + 1];
    const prev = prevKey ? MONTHLY[prevKey] : undefined;
    const pending = adminWithdrawalsStore.get().filter((w) => w.status === 'PENDING');
    const open = adminDisputesStore.get().filter((d) => d.status === 'OPEN');
    const dto: DashboardFinanceDto = {
      month: m,
      available_months: MONTHS,
      gmv: { value: cur.gmv, previous: prev?.gmv ?? null },
      commission: { value: cur.commission, previous: prev?.commission ?? null },
      voucher_cost: { value: cur.voucher, previous: prev?.voucher ?? null },
      coin_cost: cur.coin,
      gateway_fee: cur.gateway,
      net_profit: netProfit({
        commission: cur.commission,
        voucherCost: cur.voucher,
        coinCost: cur.coin,
        gatewayFee: cur.gateway,
      }),
      holdings: HOLDINGS,
      pending_money: {
        payable_withdrawals: sum(
          pending.filter((w) => w.shop_kyc_status === 'VERIFIED').map((w) => w.amount),
        ),
        blocked_withdrawals: sum(
          pending.filter((w) => w.shop_kyc_status !== 'VERIFIED').map((w) => w.amount),
        ),
        cod_remittances: sum(
          adminCodStore
            .get()
            .filter((c) => c.status === 'WAITING')
            .map((c) => c.received_amount),
        ),
        refunds_pending: sum(open.map((d) => d.amount)),
        open_disputes: open.length,
      },
      top_shops_by_gmv: [...SHOPS]
        .sort((a, b) => b.gmv - a.gmv)
        .slice(0, 8)
        .map((s) => ({ shop_id: s.shop.id, name: s.shop.name, gmv: s.gmv })),
    };
    return toFinance(dto);
  }
  return toFinance(
    await api.get<DashboardFinanceDto>('/admin/dashboard/finance', { query: { month } }),
  );
}

export async function getGmvChart(range: GmvRange, month?: MonthKey): Promise<GmvChart> {
  if (USE_MOCK) {
    await mockDelay(150);
    return toGmvChart(CHARTS[range]);
  }
  return toGmvChart(
    await api.get<GmvChartDto>('/admin/dashboard/gmv-chart', { query: { range, month } }),
  );
}

export async function getOperations(): Promise<Operations> {
  if (USE_MOCK) {
    await mockDelay();
    return toOperations(OPERATIONS);
  }
  return toOperations(await api.get<OperationsDto>('/admin/dashboard/operations'));
}

export interface GrowingQuery {
  categoryId?: number;
  q?: string;
  page?: number;
}

export async function listGrowingShops(g: GrowingQuery = {}): Promise<Page<GrowingShop>> {
  if (USE_MOCK) {
    await mockDelay(150);
    const seeMoney = can(currentUser()?.permissions, 'VIEW_FINANCE');
    const needle = g.q?.trim().toLowerCase() ?? '';
    const rows = SHOPS.filter((s) => !g.categoryId || s.category_id === g.categoryId)
      .filter((s) => s.shop.name.toLowerCase().includes(needle))
      .sort((a, b) => b.growth_percent - a.growth_percent)
      .map(({ category_id: _c, gmv, ...rest }): GrowingShopDto =>
        seeMoney ? { ...rest, gmv } : rest,
      );
    const page = paginatePage(rows, { page: g.page ?? 1, size: GROWING_PAGE_SIZE });
    return { ...page, items: page.items.map(toGrowingShop) };
  }
  return getPage<GrowingShopDto, GrowingShop>('/admin/dashboard/growing-shops', toGrowingShop, {
    query: {
      category_id: g.categoryId,
      q: g.q?.trim() || undefined,
      page: g.page,
      size: GROWING_PAGE_SIZE,
    },
  });
}
