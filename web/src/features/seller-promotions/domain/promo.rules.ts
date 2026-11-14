// Quy tắc khuyến mãi: sinh mã, giá Flash tối đa (thấp hơn ít nhất 15% giá 30 ngày), kiểm suất Flash,
// thao tác theo trạng thái voucher.
import type { EligibleProduct, ShopVoucher, VoucherStatus } from './promo.types';

const CODE_CHARS = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
/** Mã 8 ký tự, bỏ ký tự dễ nhầm (O/0, I/1) — nút "Tự tạo" (prototype genCode). */
export function genCode(rand: () => number = Math.random): string {
  let s = '';
  for (let i = 0; i < 8; i += 1) s += CODE_CHARS[Math.floor(rand() * CODE_CHARS.length)];
  return s;
}

export const CODE_RE = /^[A-Z0-9]{6,12}$/;
export const normalizeCode = (v: string): string =>
  v
    .toUpperCase()
    .replace(/[^A-Z0-9]/g, '')
    .slice(0, 12);

/** Giá Flash tối đa = 85% giá tham chiếu (làm tròn xuống). */
export const maxFlashPrice = (referencePrice: number): number => Math.floor(referencePrice * 0.85);

export type FlashIssue = 'price' | 'stock';
export function flashIssues(
  p: Pick<EligibleProduct, 'maxFlashPrice' | 'stock'>,
  price: number | null,
  stock: number | null,
): FlashIssue[] {
  const out: FlashIssue[] = [];
  if (!price || price <= 0 || price > p.maxFlashPrice) out.push('price');
  if (!stock || stock <= 0 || stock > p.stock) out.push('stock');
  return out;
}

export type VoucherAction = 'EDIT' | 'DUPLICATE' | 'DISABLE';
export function voucherActions(s: VoucherStatus): VoucherAction[] {
  if (s === 'ACTIVE') return ['EDIT', 'DUPLICATE', 'DISABLE'];
  if (s === 'SCHEDULED') return ['EDIT', 'DUPLICATE'];
  return ['DUPLICATE'];
}

export const usagePercent = (v: Pick<ShopVoucher, 'usage'>): number =>
  v.usage.limit > 0 ? Math.min(100, (v.usage.used / v.usage.limit) * 100) : 0;

export const V_TONE: Record<VoucherStatus, string> = {
  ACTIVE: 'text-mint-600 bg-mint-50',
  SCHEDULED: 'text-blue-600 bg-blue-50',
  ENDED: 'text-muted bg-surface-sunken',
  DISABLED: 'text-muted bg-surface-sunken',
};
