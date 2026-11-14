// Số liệu Tổng quan sàn (prototype admin/data.jsx DASHBOARD) — hình DTO 11.2. Hàng chờ và tiền đang
// chờ xử lý tính từ kho dùng chung shared/api/mock/admin.ts ở dashboard.api.ts.
import type { GmvChartDto, GrowingShopDto, OperationsDto } from './dashboard.dto';

export const MONTHS = ['2026-06', '2026-05', '2026-04'];

export interface MonthFigures {
  gmv: number;
  commission: number;
  voucher: number;
  coin: number;
  gateway: number;
}

export const MONTHLY: Record<string, MonthFigures> = {
  '2026-06': {
    gmv: 1842650000,
    commission: 92132500,
    voucher: 45680000,
    coin: 3840000,
    gateway: 5180000,
  },
  '2026-05': {
    gmv: 1687600000,
    commission: 84380000,
    voucher: 39820000,
    coin: 3120000,
    gateway: 4730000,
  },
  '2026-04': {
    gmv: 1580900000,
    commission: 79045000,
    voucher: 37210000,
    coin: 2890000,
    gateway: 4410000,
  },
};

export const HOLDINGS = { escrow_held: 326480000, wallet_balances: 118730000 };

const million = (rows: [string, number][]) =>
  rows.map(([label, m]) => ({ label, value: Math.round(m * 1e6) }));

export const CHARTS: Record<'7d' | 'weekly' | '6m', GmvChartDto> = {
  '7d': {
    title_key: 'LAST_7_DAYS',
    points: million([
      ['2026-06-24', 41],
      ['2026-06-25', 47],
      ['2026-06-26', 44],
      ['2026-06-27', 52],
      ['2026-06-28', 60],
      ['2026-06-29', 74],
      ['2026-06-30', 69],
    ]),
  },
  weekly: {
    title_key: 'WEEKS_IN_MONTH',
    points: million([
      ['2026-06-01/2026-06-07', 410],
      ['2026-06-08/2026-06-14', 450],
      ['2026-06-15/2026-06-21', 488],
      ['2026-06-22/2026-06-30', 494.65],
    ]),
  },
  '6m': {
    title_key: 'LAST_6_MONTHS',
    points: million([
      ['2026-01', 1290],
      ['2026-02', 1360],
      ['2026-03', 1470],
      ['2026-04', 1580.9],
      ['2026-05', 1687.6],
      ['2026-06', 1842.65],
    ]),
  },
};

const top = (rows: [number, string, number][]) =>
  rows.map(([product_id, name, sold]) => ({ product_id, name, sold }));

export const OPERATIONS: OperationsDto = {
  orders_in_progress: [
    { group: 'AWAITING_PAYMENT', count: 28 },
    { group: 'PAID', count: 142 },
    { group: 'PREPARING', count: 206 },
    { group: 'DELIVERING', count: 442 },
    { group: 'DELIVERED', count: 263 },
  ],
  orders_finished: [
    { group: 'COMPLETED', count: 2806 },
    { group: 'CANCELLED', count: 99 },
    { group: 'RETURNED_REFUNDED', count: 18 },
  ],
  top_products: top([
    [16, 'Áo thun cotton basic', 150],
    [21, 'Bộ mỹ phẩm 12 món', 120],
    [12, 'Serum Vitamin C 30ml', 93],
    [18, 'Kính mát UV400', 89],
    [14, 'Cốc sứ tối giản 350ml', 61],
    [10, 'Giày sneaker Air Run', 54],
    [15, 'Đèn bàn LED cảm ứng', 45],
    [13, 'Nước hoa nữ 50ml', 41],
  ]),
};

type ShopRow = [number, string, number, string, number, number, number, number];

/** Shop + mã ngành gốc (catalog) để lọc theo category_id. */
export const SHOPS: (GrowingShopDto & { category_id: number; gmv: number })[] = (
  [
    [1, 'TechZone Official', 1, 'Điện tử', 100, 4.9, 18, 45800000],
    [12, 'Beauty Box', 3, 'Làm đẹp', 263, 4.9, 38, 31890000],
    [2, 'Boutique Sài Gòn', 2, 'Thời trang', 188, 4.8, 31, 20450000],
    [14, 'Shoe Republic', 2, 'Thời trang', 96, 4.7, 27, 17620000],
    [15, 'Home & Living', 4, 'Nhà cửa', 110, 4.6, 24, 12180000],
    [9, 'TravelGear', 5, 'Phụ kiện', 49, 4.5, 21, 8430000],
    [17, 'GadgetHub VN', 1, 'Điện tử', 35, 4.6, 15, 7690000],
  ] as ShopRow[]
).map(([id, name, category_id, category_name, sold_count, rating_avg, growth_percent, gmv]) => ({
  shop: { id, name, logo_url: null },
  category_id,
  category_name,
  sold_count,
  rating_avg,
  growth_percent,
  gmv,
}));
