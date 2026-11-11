import type { Shop } from './shop.types';

export const shopHref = (id: number): string => `/shop/${id}`;

/** Tìm trong shop: chuyển sang trang tìm kiếm phạm vi shop (prototype onSearch(q, vendorId)). */
export const shopSearchHref = (id: number, q: string): string =>
  `/search?q=${encodeURIComponent(q.trim())}&scope=shop&shop=${id}`;

export const isOnVacation = (s: Pick<Shop, 'vacation'>): boolean => s.vacation !== null;

/** Shop đang nghỉ thì không đặt mua được (prototype: nút Thêm vào giỏ / Mua ngay bị khóa). */
export const canOrderFrom = (s: Pick<Shop, 'vacation'>): boolean => !isOnVacation(s);

/** Phí ship hiển thị của một shop theo tạm tính (shop tự miễn phí khi đạt ngưỡng). */
export function shopShipFee(s: Pick<Shop, 'shipFee' | 'freeShipFrom'>, subtotal: number): number {
  return s.freeShipFrom != null && subtotal >= s.freeShipFrom ? 0 : s.shipFee;
}
