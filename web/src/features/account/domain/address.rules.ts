// Quy tắc sổ địa chỉ — TS thuần (API_SPEC 3.8).
import type { Address } from './address.types';

/** SĐT Việt Nam: 0 + 9–10 chữ số (bỏ khoảng trắng). */
export const PHONE_RE = /^0\d{9,10}$/;
export const normalizePhone = (s: string): string => s.replace(/[\s.-]/g, '');

/** Địa chỉ đầy đủ một dòng ($AddressSnapshot.full_address). */
export const fullAddress = (a: Pick<Address, 'line' | 'ward' | 'province'>): string =>
  [a.line, a.ward.name, a.province.name].filter(Boolean).join(', ');

/**
 * Chuẩn hóa địa chỉ cụ thể để so trùng — giống `AddressLines.normalize` ở server (plan AUTH A12.07, D26):
 * chữ thường, bỏ dấu ("đ" → "d"), dấu câu thành khoảng trắng, gộp khoảng trắng.
 */
export const normalizeLine = (s: string): string =>
  s
    .toLowerCase()
    .replace(/đ/g, 'd')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]+/g, ' ')
    .trim();

/** Hai địa chỉ trùng: cùng tỉnh + phường + SĐT + địa chỉ cụ thể (không so tên người nhận, loại địa chỉ). */
export const isSameAddress = (
  a: { provinceId: number; wardCode: string; phone: string; line: string },
  b: { provinceId: number; wardCode: string; phone: string; line: string },
): boolean =>
  a.provinceId === b.provinceId &&
  a.wardCode === b.wardCode &&
  normalizePhone(a.phone) === normalizePhone(b.phone) &&
  normalizeLine(a.line) === normalizeLine(b.line);

/** Mặc định đứng đầu, còn lại giữ thứ tự. */
export const sortAddresses = (list: Address[]): Address[] =>
  [...list].sort((a, b) => Number(b.isDefault) - Number(a.isDefault));

export const defaultAddress = (list: Address[]): Address | null =>
  list.find((a) => a.isDefault) ?? list[0] ?? null;

/** Đặt một địa chỉ làm mặc định — các địa chỉ khác bỏ cờ. */
export const withDefault = (list: Address[], id: number): Address[] =>
  list.map((a) => ({ ...a, isDefault: a.id === id }));

/** SĐT hiển thị theo nhóm: 0901234567 → "0901 234 567"; 11 số → "0901 2345 678". */
export function formatPhone(p: string): string {
  const d = normalizePhone(p);
  if (d.length === 10) return `${d.slice(0, 4)} ${d.slice(4, 7)} ${d.slice(7)}`;
  if (d.length === 11) return `${d.slice(0, 4)} ${d.slice(4, 8)} ${d.slice(8)}`;
  return p;
}
