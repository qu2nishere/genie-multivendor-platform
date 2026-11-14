// Voucher của người dùng: kho voucher, lưu voucher. Mock khi USE_MOCK; sau này /api/v1/me/vouchers.
import { api, request } from '@/shared/api/http';
import { env } from '@/shared/config/env';
import { mockDelay, mockFail, mockState, USE_MOCK } from '@/shared/api/mock';
import { activityStore } from '@/shared/api/mock/activity';
import { formatNumber, formatVND } from '@/shared/lib/format';
import type { Voucher } from '../domain/voucher.types';
import type { VoucherDto } from './voucher.dto';
import { toVoucher } from './voucher.mapper';
import { VOUCHER_DTOS } from './voucher.mock';

// Mã đã lưu trong phiên mock (giữ qua các trang trong tab)
const savedDb = mockState<string[]>('vouchers.saved', () =>
  VOUCHER_DTOS.filter((v) => v.saved).map((v) => v.code),
);
const savedCodes = {
  has: (c: string) => savedDb.get().includes(c),
  add: (c: string) => void savedDb.set([...savedDb.get().filter((x) => x !== c), c]),
  delete: (c: string) => void savedDb.set(savedDb.get().filter((x) => x !== c)),
};
const withSaved = (d: VoucherDto): VoucherDto => ({ ...d, saved: savedCodes.has(d.code) });

/** Voucher đổi từ Điểm trong phiên mock (coins ghi vào kho "hoạt động"). */
function redeemedDtos(): VoucherDto[] {
  return (activityStore.get().redeemed ?? []).map((r) => ({
    id: r.id,
    code: r.code,
    kind: 'platform',
    shop_id: null,
    amount_label: `-${r.value / 1000}k`,
    label: `Giảm ${formatVND(r.value, { locale: 'vi' })}`,
    condition_label: 'Đơn từ 50.000₫',
    type: 'FIXED',
    value: r.value,
    cap: null,
    min_order: 50000,
    categories: null,
    excluded: null,
    valid_from: r.created_at.slice(0, 10),
    expires_at: '2026-12-31',
    scope_label: 'Toàn sàn',
    sponsor_label: 'Đổi từ Điểm Genie',
    content: `Đổi từ ${formatNumber(r.cost, { locale: 'vi' })} Điểm, dùng 1 lần.`,
    saved: true,
    status: 'active',
    used_count: 0,
    usage_limit: 1,
  }));
}

export async function listMyVouchers(): Promise<Voucher[]> {
  if (USE_MOCK) {
    await mockDelay();
    return [...redeemedDtos(), ...VOUCHER_DTOS.map(withSaved).filter((v) => v.saved)].map(
      toVoucher,
    );
  }
  return (await api.get<VoucherDto[]>('/me/vouchers')).map(toVoucher);
}

/** Voucher theo mã (để hiện trạng thái đã lưu của ticket trên trang chủ / shop / chiến dịch). */
export async function getVouchersByCodes(codes: string[]): Promise<Voucher[]> {
  if (USE_MOCK) {
    await mockDelay(80);
    return VOUCHER_DTOS.filter((v) => codes.includes(v.code))
      .map(withSaved)
      .map(toVoucher);
  }
  return (
    await api.get<VoucherDto[]>('/public/vouchers', { query: { codes: codes.join(',') } })
  ).map(toVoucher);
}

/** Lưu voucher vào Kho Voucher; đã lưu → GNE-VCH-4091, mã không tồn tại → GNE-VCH-4041. */
export async function saveVoucher(code: string): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(150);
    if (!VOUCHER_DTOS.some((v) => v.code === code)) mockFail('GNE-VCH-4041');
    if (savedCodes.has(code)) mockFail('GNE-VCH-4091');
    savedCodes.add(code);
    return;
  }
  await api.post('/me/vouchers', { code });
}

/** Voucher shop đang chạy (trang shop) — công khai, gọi được từ SSR. */
export async function listShopVouchers(shopId: number): Promise<Voucher[]> {
  if (USE_MOCK) {
    await mockDelay(80);
    return VOUCHER_DTOS.filter((v) => v.shop_id === shopId && v.status === 'active').map(toVoucher);
  }
  const baseUrl =
    typeof window === 'undefined'
      ? (env.INTERNAL_API_URL ?? env.PUBLIC_API_URL)
      : env.PUBLIC_API_URL;
  return (
    await request<VoucherDto[]>(`/public/shops/${shopId}/vouchers`, { auth: false, baseUrl })
  ).data.map(toVoucher);
}

/**
 * Tra voucher theo mã để "Áp dụng" trong Voucher Picker (POST /me/checkout/applicable-vouchers với
 * `code`, API_SPEC 6.9) — trả cả voucher chưa lưu. Không tồn tại → GNE-VCH-4041.
 */
export async function findVoucherByCode(code: string): Promise<Voucher> {
  const c = code.trim().toUpperCase();
  if (USE_MOCK) {
    await mockDelay(150);
    const d = VOUCHER_DTOS.find((v) => v.code === c);
    if (!d || d.status === 'expired' || d.status === 'used') mockFail('GNE-VCH-4041');
    return toVoucher(withSaved(d));
  }
  const list = await api.post<{ voucher: VoucherDto }[]>('/me/checkout/applicable-vouchers', {
    code: c,
    scope: 'PLATFORM',
    cart_item_ids: [],
  });
  if (!list[0]) throw new Error('GNE-VCH-4041');
  return toVoucher(list[0].voucher);
}

/** DELETE /me/vouchers/{id} — xóa khỏi Kho Voucher. */
export async function removeVoucher(v: Pick<Voucher, 'id' | 'code'>): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(150);
    savedCodes.delete(v.code);
    return;
  }
  await api.delete(`/me/vouchers/${v.id}`);
}
