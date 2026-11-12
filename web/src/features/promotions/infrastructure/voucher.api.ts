// Voucher của người dùng: kho voucher, lưu voucher. Mock khi USE_MOCK; sau này /api/v1/me/vouchers.
import { api } from '@/shared/api/http';
import { mockDelay, mockFail, USE_MOCK } from '@/shared/api/mock';
import type { Voucher } from '../domain/voucher.types';
import type { VoucherDto } from './voucher.dto';
import { toVoucher } from './voucher.mapper';
import { VOUCHER_DTOS } from './voucher.mock';

// Mã đã lưu trong phiên mock
const savedCodes = new Set(VOUCHER_DTOS.filter((v) => v.saved).map((v) => v.code));
const withSaved = (d: VoucherDto): VoucherDto => ({ ...d, saved: savedCodes.has(d.code) });

export async function listMyVouchers(): Promise<Voucher[]> {
  if (USE_MOCK) {
    await mockDelay();
    return VOUCHER_DTOS.map(withSaved)
      .filter((v) => v.saved)
      .map(toVoucher);
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
