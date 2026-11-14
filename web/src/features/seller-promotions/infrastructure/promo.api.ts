// Khuyến mãi của shop (API_SPEC 10.7): voucher shop (danh sách + summary, xem, tạo, sửa, tắt) và đăng ký
// Flash Sale. Mock: dữ liệu prototype VendorPages.jsx VOUCHERS, FLASH_PRODUCTS; giữ trong phiên tab.
import { api, request } from '@/shared/api/http';
import { mockDelay, mockFail, mockState, USE_MOCK } from '@/shared/api/mock';
import { maxFlashPrice } from '../domain/promo.rules';
import type {
  DiscountType,
  EligibleProduct,
  FlashCampaign,
  FlashItem,
  ShopVoucher,
  VoucherInput,
  VoucherStatus,
  VoucherSummary,
} from '../domain/promo.types';

interface VoucherDto {
  id: number;
  code: string;
  discount_type: DiscountType;
  discount_value: number | null;
  max_discount: number | null;
  min_order_value: number;
  usage: { used: number; limit: number };
  status: VoucherStatus;
  valid_to: string;
}
interface VoucherInputDto {
  code: string;
  discount_type: DiscountType;
  discount_value: number | null;
  max_discount: number | null;
  min_order_value: number | null;
  usage_limit: number | null;
  usage_per_user: number | null;
  budget: number | null;
  valid_from: string;
  valid_to: string;
  scope: VoucherInput['scope'];
  category_ids: number[];
  product_ids: number[];
  excluded_category_ids: number[];
}

const toVoucher = (d: VoucherDto): ShopVoucher => ({
  id: d.id,
  code: d.code,
  discountType: d.discount_type,
  discountValue: d.discount_value,
  maxDiscount: d.max_discount,
  minOrderValue: d.min_order_value,
  usage: d.usage,
  status: d.status,
  validTo: d.valid_to,
});
const day = (s: string, end = false) => (s ? `${s}T${end ? '23:59:59' : '00:00:00'}+07:00` : '');
const toInputDto = (v: VoucherInput): VoucherInputDto => ({
  code: v.code,
  discount_type: v.discountType,
  discount_value: v.discountType === 'FREESHIP' ? null : v.discountValue,
  max_discount: v.discountType === 'PERCENTAGE' ? v.maxDiscount : null,
  min_order_value: v.minOrderValue,
  usage_limit: v.usageLimit,
  usage_per_user: v.usagePerUser,
  budget: v.budget,
  valid_from: day(v.validFrom),
  valid_to: day(v.validTo, true),
  scope: v.scope,
  category_ids: v.scope === 'CATEGORY' ? v.categoryIds : [],
  product_ids: v.scope === 'PRODUCT' ? v.productIds : [],
  excluded_category_ids: v.scope === 'PRODUCT' ? [] : v.excludedCategoryIds,
});
const fromInputDto = (d: VoucherInputDto): VoucherInput => ({
  code: d.code,
  discountType: d.discount_type,
  discountValue: d.discount_value,
  maxDiscount: d.max_discount,
  minOrderValue: d.min_order_value,
  usageLimit: d.usage_limit,
  usagePerUser: d.usage_per_user,
  budget: d.budget,
  validFrom: d.valid_from.slice(0, 10),
  validTo: d.valid_to.slice(0, 10),
  scope: d.scope,
  categoryIds: d.category_ids,
  productIds: d.product_ids,
  excludedCategoryIds: d.excluded_category_ids,
});

// ---- Mock ----
interface MockVoucher extends VoucherInputDto {
  id: number;
  used: number;
  status: VoucherStatus;
}
const mv = (
  id: number,
  code: string,
  type: DiscountType,
  value: number | null,
  cap: number | null,
  min: number,
  used: number,
  limit: number,
  status: VoucherStatus,
  from: string,
  to: string,
): MockVoucher => ({
  id,
  code,
  discount_type: type,
  discount_value: value,
  max_discount: cap,
  min_order_value: min,
  usage_limit: limit,
  usage_per_user: 1,
  budget: null,
  valid_from: `${from}T00:00:00+07:00`,
  valid_to: `${to}T23:59:59+07:00`,
  scope: 'ALL',
  category_ids: [],
  product_ids: [],
  excluded_category_ids: [],
  used,
  status,
});

const db = mockState('seller.promo', () => ({
  vouchers: [
    mv(
      51,
      'SALE15',
      'PERCENTAGE',
      15,
      50000,
      250000,
      142,
      500,
      'ACTIVE',
      '2026-06-01',
      '2026-06-20',
    ),
    mv(
      52,
      'FREESHIP',
      'FREESHIP',
      null,
      null,
      99000,
      380,
      1000,
      'ACTIVE',
      '2026-06-01',
      '2026-06-30',
    ),
    mv(53, 'NEW30K', 'FIXED', 30000, null, 0, 0, 300, 'SCHEDULED', '2026-06-15', '2026-07-01'),
    mv(54, 'TET50', 'FIXED', 50000, null, 500000, 210, 210, 'ENDED', '2026-01-20', '2026-02-15'),
  ],
  flash: [] as FlashItem[],
}));

const toListDto = (v: MockVoucher): VoucherDto => ({
  id: v.id,
  code: v.code,
  discount_type: v.discount_type,
  discount_value: v.discount_value,
  max_discount: v.max_discount,
  min_order_value: v.min_order_value ?? 0,
  usage: { used: v.used, limit: v.usage_limit ?? 0 },
  status: v.status,
  valid_to: v.valid_to,
});

export async function listVouchers(): Promise<{ items: ShopVoucher[]; summary: VoucherSummary }> {
  if (USE_MOCK) {
    await mockDelay(180);
    const all = db.get().vouchers;
    return {
      items: all.map(toListDto).map(toVoucher),
      summary: {
        activeCount: all.filter((v) => v.status === 'ACTIVE').length,
        usageThisMonth: 522,
        revenueThisMonth: 8_400_000,
      },
    };
  }
  const res = await request<VoucherDto[]>('/seller/vouchers', { query: { size: 50 } });
  const s = res.summary ?? {};
  return {
    items: res.data.map(toVoucher),
    summary: {
      activeCount: Number(s.active_count ?? 0),
      usageThisMonth: Number(s.usage_this_month ?? 0),
      revenueThisMonth: Number(s.revenue_from_promotions_this_month ?? 0),
    },
  };
}

export async function getVoucher(id: number): Promise<VoucherInput> {
  if (USE_MOCK) {
    await mockDelay(120);
    const v = db.get().vouchers.find((x) => x.id === id);
    if (!v) mockFail('GNE-VCH-4041');
    return fromInputDto(v);
  }
  return fromInputDto(await api.get<VoucherInputDto>(`/seller/vouchers/${id}`));
}

/** Tạo (id = null) hoặc sửa voucher. */
export async function saveVoucher(id: number | null, v: VoucherInput): Promise<ShopVoucher> {
  const body = toInputDto(v);
  if (USE_MOCK) {
    await mockDelay(300);
    const s = db.get();
    if (s.vouchers.some((x) => x.code === v.code && x.id !== id)) mockFail('GNE-VCH-4092');
    const today = '2026-06-08';
    const status: VoucherStatus =
      v.validFrom > today ? 'SCHEDULED' : v.validTo < today ? 'ENDED' : 'ACTIVE';
    const prev = id ? s.vouchers.find((x) => x.id === id) : undefined;
    const next: MockVoucher = {
      ...body,
      id: id ?? Math.max(...s.vouchers.map((x) => x.id)) + 1,
      used: prev?.used ?? 0,
      status,
    };
    db.set({
      ...s,
      vouchers: prev ? s.vouchers.map((x) => (x.id === id ? next : x)) : [next, ...s.vouchers],
    });
    return toVoucher(toListDto(next));
  }
  return toVoucher(
    id
      ? await api.put<VoucherDto>(`/seller/vouchers/${id}`, body)
      : await api.post<VoucherDto>('/seller/vouchers', body),
  );
}

export async function disableVoucher(id: number): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(200);
    const s = db.get();
    db.set({
      ...s,
      vouchers: s.vouchers.map((x) => (x.id === id ? { ...x, status: 'DISABLED' } : x)),
    });
    return;
  }
  await api.patch(`/seller/vouchers/${id}`, { is_active: false });
}

/** Danh mục / sản phẩm của shop cho phạm vi áp dụng (mock theo prototype). */
export async function listVoucherTargets(): Promise<{
  categories: { id: number; name: string }[];
  products: { id: number; name: string }[];
}> {
  if (USE_MOCK) {
    await mockDelay(80);
    return {
      categories: [
        { id: 13, name: 'Tai nghe & Loa' },
        { id: 12, name: 'Máy tính & Laptop' },
        { id: 11, name: 'Điện thoại & Phụ kiện' },
        { id: 14, name: 'Camera' },
      ],
      products: [
        { id: 1, name: 'Tai nghe ANC Pro 5' },
        { id: 4, name: 'Watch S2 AMOLED' },
        { id: 6, name: 'Bàn phím cơ RGB' },
      ],
    };
  }
  const [cats, prods] = await Promise.all([
    api.get<{ id: number; name: string; children?: { id: number; name: string }[] }[]>(
      '/seller/categories',
    ),
    api.get<{ id: number; name: string }[]>('/seller/products', {
      query: { status: 'PUBLISHED', size: 100 },
    }),
  ]);
  return {
    categories: cats.flatMap((c) => c.children ?? []).map(({ id, name }) => ({ id, name })),
    products: prods.map(({ id, name }) => ({ id, name })),
  };
}

// ---- Flash Sale ----
interface CampaignDto {
  id: number;
  name: string;
  starts_at: string;
  ends_at: string;
  registration_deadline: string;
  max_items_per_shop: number;
  registered_count: number;
}
const CAMPAIGN: CampaignDto = {
  id: 2103,
  name: 'FLASH SALE 12.12',
  starts_at: '2026-12-12T00:00:00+07:00',
  ends_at: '2026-12-12T02:00:00+07:00',
  registration_deadline: '2026-06-10',
  max_items_per_shop: 20,
  registered_count: 0,
};
const ELIGIBLE: EligibleProduct[] = (
  [
    [1, 'Tai nghe ANC Pro 5', 389000, 38],
    [4, 'Đồng hồ Watch S2', 690000, 12],
    [6, 'Bàn phím cơ RGB', 459000, 54],
  ] as const
).map(([productId, name, referencePrice, stock]) => ({
  productId,
  name,
  referencePrice,
  maxFlashPrice: maxFlashPrice(referencePrice),
  stock,
}));

export async function listFlashCampaigns(): Promise<FlashCampaign[]> {
  const map = (d: CampaignDto): FlashCampaign => ({
    id: d.id,
    name: d.name,
    startsAt: d.starts_at,
    endsAt: d.ends_at,
    registrationDeadline: d.registration_deadline,
    maxItemsPerShop: d.max_items_per_shop,
    registeredCount: d.registered_count,
  });
  if (USE_MOCK) {
    await mockDelay(120);
    return [map({ ...CAMPAIGN, registered_count: db.get().flash.length })];
  }
  return (
    await api.get<CampaignDto[]>('/seller/flash-sales', { query: { registrable: true } })
  ).map(map);
}

export async function listEligible(campaignId: number): Promise<EligibleProduct[]> {
  if (USE_MOCK) {
    await mockDelay(120);
    return ELIGIBLE;
  }
  const d = await api.get<
    {
      product_id: number;
      name: string;
      reference_price: number;
      max_flash_price: number;
      stock: number;
    }[]
  >(`/seller/flash-sales/${campaignId}/eligible-products`);
  return d.map((x) => ({
    productId: x.product_id,
    name: x.name,
    referencePrice: x.reference_price,
    maxFlashPrice: x.max_flash_price,
    stock: x.stock,
  }));
}

interface FlashItemDto {
  id: number;
  product_id: number;
  name: string;
  flash_price: number;
  flash_stock: number;
  status: FlashItem['status'];
}
const toItem = (d: FlashItemDto): FlashItem => ({
  id: d.id,
  productId: d.product_id,
  name: d.name,
  flashPrice: d.flash_price,
  flashStock: d.flash_stock,
  status: d.status,
});

export async function listFlashItems(campaignId: number): Promise<FlashItem[]> {
  if (USE_MOCK) {
    await mockDelay(100);
    return db.get().flash;
  }
  return (await api.get<FlashItemDto[]>(`/seller/flash-sales/${campaignId}/items`)).map(toItem);
}

/** Đăng ký (hoặc ghi đè) một sản phẩm vào chiến dịch. */
export async function registerFlashItem(
  campaignId: number,
  v: { productId: number; flashPrice: number; flashStock: number },
): Promise<FlashItem> {
  if (USE_MOCK) {
    await mockDelay(250);
    const p = ELIGIBLE.find((x) => x.productId === v.productId);
    if (!p || v.flashPrice > p.maxFlashPrice || v.flashStock < 1 || v.flashStock > p.stock)
      mockFail('GNE-PRO-4221');
    const s = db.get();
    const others = s.flash.filter((x) => x.productId !== v.productId);
    if (others.length >= CAMPAIGN.max_items_per_shop) mockFail('GNE-PRO-4223');
    const item: FlashItem = {
      id: Date.now() % 100000,
      productId: v.productId,
      name: p.name,
      flashPrice: v.flashPrice,
      flashStock: v.flashStock,
      status: 'PENDING',
    };
    db.set({ ...s, flash: [...others, item] });
    return item;
  }
  return toItem(
    await api.post<FlashItemDto>(`/seller/flash-sales/${campaignId}/items`, {
      product_id: v.productId,
      flash_price: v.flashPrice,
      flash_stock: v.flashStock,
    }),
  );
}
