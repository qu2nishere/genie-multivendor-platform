import type {
  DashboardFinance,
  FinishedGroup,
  GmvChart,
  GrowingShop,
  MovingGroup,
  Operations,
} from '../domain/dashboard.types';
import type {
  DashboardFinanceDto,
  GmvChartDto,
  GrowingShopDto,
  OperationsDto,
} from './dashboard.dto';

export const toFinance = (d: DashboardFinanceDto): DashboardFinance => ({
  month: d.month,
  availableMonths: d.available_months,
  gmv: d.gmv,
  commission: d.commission,
  voucherCost: d.voucher_cost,
  coinCost: d.coin_cost,
  gatewayFee: d.gateway_fee,
  netProfit: d.net_profit,
  holdings: { escrowHeld: d.holdings.escrow_held, walletBalances: d.holdings.wallet_balances },
  pendingMoney: {
    payableWithdrawals: d.pending_money.payable_withdrawals,
    blockedWithdrawals: d.pending_money.blocked_withdrawals,
    codRemittances: d.pending_money.cod_remittances,
    refundsPending: d.pending_money.refunds_pending,
    openDisputes: d.pending_money.open_disputes,
  },
  topShopsByGmv: d.top_shops_by_gmv.map((s) => ({ shopId: s.shop_id, name: s.name, gmv: s.gmv })),
});

export const toGmvChart = (d: GmvChartDto): GmvChart => ({
  titleKey: d.title_key,
  points: d.points,
});

const MOVING: readonly MovingGroup[] = [
  'AWAITING_PAYMENT',
  'PAID',
  'PREPARING',
  'DELIVERING',
  'DELIVERED',
];
const FINISHED: readonly FinishedGroup[] = ['COMPLETED', 'CANCELLED', 'RETURNED_REFUNDED'];
const isMoving = (g: string): g is MovingGroup => (MOVING as readonly string[]).includes(g);
const isFinished = (g: string): g is FinishedGroup => (FINISHED as readonly string[]).includes(g);

export const toOperations = (d: OperationsDto): Operations => ({
  inProgress: d.orders_in_progress.flatMap((r) =>
    isMoving(r.group) ? [{ group: r.group, count: r.count }] : [],
  ),
  finished: d.orders_finished.flatMap((r) =>
    isFinished(r.group) ? [{ group: r.group, count: r.count }] : [],
  ),
  topProducts: d.top_products.map((p) => ({ productId: p.product_id, name: p.name, sold: p.sold })),
});

export const toGrowingShop = (d: GrowingShopDto): GrowingShop => ({
  shop: { id: d.shop.id, name: d.shop.name, logoUrl: d.shop.logo_url },
  categoryName: d.category_name,
  sold: d.sold_count,
  rating: d.rating_avg,
  growthPercent: d.growth_percent,
  gmv: d.gmv ?? null,
});
