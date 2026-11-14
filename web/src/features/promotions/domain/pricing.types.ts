// Kiểu cho bộ tính giá giỏ / thanh toán (plan G10.09–G10.10; prototype data.jsx priceCart).
import type { Voucher } from './voucher.types';

/** Dòng hàng tối thiểu để tính voucher (giỏ và trang thanh toán đều dựng được). */
export interface PricingLine {
  key: string;
  shopId: number;
  /** Tên danh mục gốc — voucher chỉ áp / loại trừ theo ngành. */
  category: string | null;
  price: number;
  qty: number;
}

/** Phí ship của một shop với các dòng đang tính (do checkout/domain/shipping.rules cung cấp). */
export type ShipFn = (shopId: number, lines: PricingLine[]) => number;

/** Voucher đang chọn: một sàn + một freeship + mỗi shop một voucher (giá trị = id voucher). */
export interface VoucherSelection {
  platform: string | null;
  freeship: string | null;
  shop: Record<number, string>;
}

export interface PricingContext {
  /** Kho voucher của người dùng (đã lưu) + mã vừa nhập. */
  vouchers: Voucher[];
  ship: ShipFn;
}

export type IneligibleReason = 'no_items' | 'min_order' | 'not_active';

export type VoucherCheck =
  | { ok: true; base: number }
  | { ok: false; base: number; reason: IneligibleReason; minOrder: number };

export interface PriceBreakdown {
  subtotal: number;
  shipping: number;
  platformDiscount: number;
  freeshipDiscount: number;
  shopDiscount: number;
  /** Giảm voucher shop theo shop. */
  shopByShop: Record<number, number>;
  /** Giảm phí ship (freeship) phân bổ theo shop. */
  shipCutByShop: Record<number, number>;
  /** Giảm voucher sàn phân bổ theo dòng (phần dư dồn dòng cuối). */
  perLine: Record<string, number>;
  /** Số Điểm tối đa được dùng (50% tiền hàng còn lại sau voucher). */
  pointsCap: number;
  pointsDiscount: number;
  discountTotal: number;
  total: number;
  platformVoucher: Voucher | null;
  freeshipVoucher: Voucher | null;
}
