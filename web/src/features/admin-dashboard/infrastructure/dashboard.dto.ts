// DTO snake_case (API_SPEC 11.2).
export interface ComparedDto {
  value: number;
  previous: number | null;
}

export interface DashboardFinanceDto {
  month: string;
  available_months: string[];
  gmv: ComparedDto;
  commission: ComparedDto;
  voucher_cost: ComparedDto;
  coin_cost: number;
  gateway_fee: number;
  net_profit: number;
  holdings: { escrow_held: number; wallet_balances: number };
  pending_money: {
    payable_withdrawals: number;
    blocked_withdrawals: number;
    cod_remittances: number;
    refunds_pending: number;
    open_disputes: number;
  };
  top_shops_by_gmv: { shop_id: number; name: string; gmv: number }[];
}

export interface GmvChartDto {
  title_key: 'LAST_7_DAYS' | 'WEEKS_IN_MONTH' | 'LAST_6_MONTHS';
  points: { label: string; value: number }[];
}

export interface OperationsDto {
  orders_in_progress: { group: string; count: number }[];
  orders_finished: { group: string; count: number }[];
  top_products: { product_id: number; name: string; sold: number }[];
}

export interface GrowingShopDto {
  shop: { id: number; name: string; logo_url: string | null };
  category_name: string;
  sold_count: number;
  rating_avg: number;
  growth_percent: number;
  gmv?: number;
}
