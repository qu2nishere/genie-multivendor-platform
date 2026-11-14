// Quy tắc Cài đặt Shop: phải bật ít nhất 1 hãng, tự trả lời cần nội dung, tên chủ tài khoản khớp CCCD
// (so không dấu, viết hoa), chế độ nghỉ tối đa 30 ngày.
import type { CarrierCode, ShopSettings } from './settings.types';

export const VACATION_MAX_DAYS = 30;

/** "Đặng Quốc Huy" → "DANG QUOC HUY". */
export const normalizeHolder = (s: string): string =>
  s
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/đ/gi, 'D')
    .toUpperCase()
    .replace(/\s+/g, ' ')
    .trim();

export const holderMatches = (holder: string, kycName: string): boolean =>
  normalizeHolder(holder) === normalizeHolder(kycName);

/** Tắt hãng cuối cùng → không cho (422 GNE-VND-4221). */
export function toggleCarrier(
  c: Record<CarrierCode, boolean>,
  code: CarrierCode,
): Record<CarrierCode, boolean> | null {
  const next = { ...c, [code]: !c[code] };
  return Object.values(next).some(Boolean) ? next : null;
}

export const autoReplyValid = (a: ShopSettings['autoReply']): boolean =>
  !a.enabled || !!a.greeting.trim() || !!a.away.trim();

/** "YYYY-MM-DD" của hôm nay và ngày xa nhất được chọn khi bật chế độ nghỉ (theo giờ Việt Nam). */
export function vacationRange(now: number): { min: string; max: string } {
  const vn = (ms: number) => new Date(ms + 7 * 3_600_000).toISOString().slice(0, 10);
  return { min: vn(now), max: vn(now + VACATION_MAX_DAYS * 86_400_000) };
}

export const canRename = (nextAllowedAt: string | null, now: number): boolean =>
  !nextAllowedAt || Date.parse(nextAllowedAt) <= now;
