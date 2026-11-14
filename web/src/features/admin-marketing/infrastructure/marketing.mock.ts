// Dữ liệu marketing sàn (prototype admin/data.jsx PLATFORM_VOUCHERS, FLASH_CAMPAIGNS, PLATFORM_NOTIFS;
// banner lấy kho chung shared/api/mock/banners.ts) — hình DTO 11.14–11.17, giữ trong phiên tab.
import { mockState } from '@/shared/api/mock';
import { BANNER_SEED } from '@/shared/api/mock/banners';
import { SEED_ROOT_CATEGORIES } from '@/shared/api/mock/seed';
import { productImg } from '@/shared/lib/assets';
import type {
  AdminBannerDto,
  FlashCampaignDetailDto,
  PlatformNotificationDetailDto,
  PlatformVoucherDto,
  PlatformVoucherInputDto,
} from './marketing.dto';

// ---------- Voucher sàn ----------
type VoucherRow = PlatformVoucherDto & { input: PlatformVoucherInputDto; discount_used: number };

const voucher = (
  id: number,
  title: string,
  code: string,
  type: PlatformVoucherDto['discount_type'],
  value: number,
  cap: number,
  min: number,
  used: number,
  limit: number,
  perUser: number,
  from: string,
  to: string,
  status: PlatformVoucherDto['status'],
  funded: PlatformVoucherDto['funded_by'],
  budget: number,
  discountUsed: number,
): VoucherRow => ({
  id,
  title,
  code,
  discount_type: type,
  discount_value: value,
  min_order_value: min,
  max_discount: cap,
  usage: { used, limit },
  usage_per_user: perUser,
  valid_from: `${from}T00:00:00+07:00`,
  valid_to: `${to}T23:59:59+07:00`,
  funded_by: funded,
  status,
  discount_used: discountUsed,
  input: {
    title,
    code,
    discount_type: type,
    discount_value: value,
    max_discount: cap,
    min_order_value: min,
    usage_limit: limit,
    usage_per_user: perUser,
    budget,
    valid_from: `${from}T00:00:00+07:00`,
    valid_to: `${to}T23:59:59+07:00`,
    scope: 'ALL',
    category_ids: [],
    excluded_category_ids: [],
    funded_by: funded,
    platform_share_percent: funded === 'CO_SPONSOR' ? 50 : null,
  },
});

export const vouchersStore = mockState<VoucherRow[]>('admin.vouchers', () => [
  voucher(
    1,
    'Siêu Sale Giữa Tháng',
    'GENIE50',
    'PERCENTAGE',
    50,
    100000,
    500000,
    3420,
    10000,
    1,
    '2026-06-01',
    '2026-06-30',
    'ACTIVE',
    'PLATFORM',
    500000000,
    296400000,
  ),
  voucher(
    2,
    'Freeship toàn sàn',
    'FREESHIP30',
    'FREESHIP',
    0,
    30000,
    99000,
    8900,
    20000,
    3,
    '2026-06-01',
    '2026-07-31',
    'ACTIVE',
    'PLATFORM',
    400000000,
    160400000,
  ),
  voucher(
    3,
    'Chào bạn mới',
    'NEWUSER80',
    'FIXED_AMOUNT',
    80000,
    80000,
    300000,
    0,
    5000,
    1,
    '2026-07-01',
    '2026-07-31',
    'SCHEDULED',
    'PLATFORM',
    400000000,
    0,
  ),
  voucher(
    4,
    'Tết 2026',
    'TET2026',
    'PERCENTAGE',
    20,
    50000,
    200000,
    15000,
    15000,
    2,
    '2026-01-01',
    '2026-02-15',
    'ENDED',
    'CO_SPONSOR',
    200000000,
    0,
  ),
]);

/** Ngân sách / giảm giá đã dùng tổng (prototype ghi cứng 456.800.000₫ / 543.200.000₫). */
export const VOUCHER_TOTALS = { total_discount_used: 456800000, remaining_budget: 543200000 };

// ---------- Flash Sale ----------
const item = (
  id: number,
  productId: number,
  name: string,
  shop: string,
  price: number,
  flash: number,
  stock: number,
  status: 'PENDING' | 'APPROVED' | 'REJECTED',
) => ({
  id,
  product_id: productId,
  product_name: name,
  shop_name: shop,
  reference_price: price,
  flash_price: flash,
  flash_stock: stock,
  status,
  meets_discount_rule: flash <= price * 0.85,
});

export const flashStore = mockState<FlashCampaignDetailDto[]>('admin.flash', () => [
  {
    id: 2103,
    name: 'FLASH SALE 12.12',
    starts_at: '2026-12-12T00:00:00+07:00',
    ends_at: '2026-12-12T02:00:00+07:00',
    status: 'DRAFT',
    item_count: 3,
    max_items_per_shop: 20,
    registration_deadline: '2026-12-10',
    items: [
      item(7, 1, 'Tai nghe ANC Pro 5', 'TechZone Official', 389000, 299000, 50, 'PENDING'),
      item(8, 12, 'Serum Vitamin C 30ml', 'Beauty Box', 178000, 129000, 120, 'APPROVED'),
      item(9, 16, 'Áo thun cotton basic', 'Boutique Sài Gòn', 89000, 82000, 200, 'PENDING'),
    ],
  },
  {
    id: 2102,
    name: 'Khung giờ vàng 20h',
    starts_at: '2026-06-20T20:00:00+07:00',
    ends_at: '2026-06-20T22:00:00+07:00',
    status: 'ACTIVE',
    item_count: 1,
    max_items_per_shop: 20,
    registration_deadline: '2026-06-18',
    items: [item(5, 14, 'Cốc sứ tối giản 350ml', 'Home & Living', 59000, 39000, 80, 'APPROVED')],
  },
  {
    id: 2101,
    name: 'Sale đầu tháng 6',
    starts_at: '2026-06-01T00:00:00+07:00',
    ends_at: '2026-06-01T03:00:00+07:00',
    status: 'ENDED',
    item_count: 0,
    max_items_per_shop: 20,
    registration_deadline: '2026-05-28',
    items: [],
  },
]);

// ---------- Banner ----------
/** Lượt hiển thị / bấm mẫu theo mã banner (prototype views / clicks). */
const STATS: Record<string, [number, number]> = {
  'BN-101': [48200, 2210],
  'BN-105': [31800, 1630],
  'BN-106': [29400, 1720],
  'BN-107': [27100, 1180],
  'BN-115': [12800, 940],
  'BN-108': [22400, 860],
  'BN-109': [18900, 1290],
  'BN-102': [15600, 520],
  'BN-110': [14100, 470],
  'BN-116': [9800, 610],
  'BN-104': [41200, 3100],
};

const catOf = (slug: string | null) => {
  const c = SEED_ROOT_CATEGORIES.find((x) => x.slug === slug);
  return c ? { id: c.id, name: c.name, slug: c.slug } : null;
};

export type BannerRow = Omit<AdminBannerDto, 'state'>;

export const bannersStore = mockState<BannerRow[]>('admin.banners', () =>
  BANNER_SEED.map((b) => {
    const [impressions, clicks] = STATS[b.id] ?? [0, 0];
    return {
      id: b.id,
      title: b.title,
      image_url: b.image_url,
      mobile_image_url: b.image_mobile_url,
      target_url: b.link,
      position: b.position,
      category: catOf(b.category_slug),
      sort_order: b.sort_order,
      starts_at: b.starts_at,
      ends_at: b.ends_at,
      is_active: b.is_active,
      impressions,
      clicks,
    };
  }),
);

// ---------- Thông báo sàn ----------
const notif = (
  id: number,
  title: string,
  content: string,
  target: PlatformNotificationDetailDto['target'],
  by: string,
  at: string,
  reads: number,
  reach: number,
  image: string | null,
): PlatformNotificationDetailDto => ({
  id,
  code: `PN-${id}`,
  title,
  content_preview: content,
  content_html: `<p>${content}</p>`,
  image_url: image,
  target,
  created_by: by,
  send_at: `${at}:00+07:00`,
  is_scheduled: false,
  read_count: reads,
  reach_count: reach,
  recipient_emails: [],
});

export const notificationsStore = mockState<PlatformNotificationDetailDto[]>(
  'admin.platformNotifs',
  () => [
    notif(
      1048,
      'Siêu sale 12.12 giảm tới 50%',
      'Chương trình Siêu sale 12.12 bắt đầu từ 00:00. Lưu voucher ngay để không bỏ lỡ ưu đãi lớn nhất năm.',
      'CUSTOMER',
      'Admin (1)',
      '2026-06-08T09:00',
      48210,
      120400,
      '/img/products/banner-sale.jpg',
    ),
    notif(
      1047,
      'Nâng cấp hệ thống 02:00 – 04:00',
      'Một số dịch vụ có thể gián đoạn trong khung giờ bảo trì. Đơn hàng và ví Escrow không bị ảnh hưởng.',
      'ALL',
      'Admin (1)',
      '2026-06-07T09:00',
      73560,
      248300,
      null,
    ),
    notif(
      1046,
      'Cập nhật biểu phí hoa hồng từ 01/07',
      'Phí hoa hồng ngành Làm đẹp điều chỉnh từ 6% lên 7%. Xem chi tiết tại Bảng hoa hồng & Phí dịch vụ.',
      'VENDOR',
      'Admin (2)',
      '2026-06-05T16:30',
      2140,
      3120,
      null,
    ),
    notif(
      1045,
      'Đăng ký Flash Sale khung giờ vàng',
      'Người bán đăng ký tối đa 20 sản phẩm tham gia Flash Sale trước ngày 10/06.',
      'VENDOR',
      'Admin (2)',
      '2026-06-03T10:00',
      1860,
      3120,
      productImg('banner-tech'),
    ),
    notif(
      1044,
      'Điểm danh nhận Điểm mỗi ngày',
      'Điểm danh 7 ngày liên tiếp để nhận tới 1.600 Điểm Genie.',
      'CUSTOMER',
      'Admin (1)',
      '2026-06-01T08:00',
      35420,
      118900,
      null,
    ),
    notif(
      1043,
      'Chính sách đổi trả mới',
      'Genie cập nhật chính sách đổi trả và hoàn tiền áp dụng cho toàn sàn.',
      'ALL',
      'Admin (1)',
      '2026-05-28T14:00',
      61200,
      240100,
      null,
    ),
  ],
);

/** Số người nhận ước tính theo đối tượng (prototype REACH). */
export const REACH: Record<'ALL' | 'CUSTOMER' | 'VENDOR', number> = {
  ALL: 248300,
  CUSTOMER: 120400,
  VENDOR: 3120,
};

/** Email có tài khoản (mock kiểm `details.unknown_emails` — GNE-NOT-4221). */
export const KNOWN_EMAILS = [
  'mai.tran@gmail.com',
  'an.nguyen@gmail.com',
  'phuc.le@yahoo.com',
  'shop@techzone.vn',
  'lan@boutiquesg.vn',
];
