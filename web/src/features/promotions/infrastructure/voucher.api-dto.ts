// `$Voucher` của API (mục 1; 3.14, 5.13 — plan 2026-11-26 voucher V08) — snake_case đúng hợp đồng.
export interface ApiVoucherDto {
  id: number;
  code: string;
  title: string;
  type: 'PLATFORM' | 'SHOP' | 'FREESHIP';
  discount_type: 'PERCENTAGE' | 'FIXED_AMOUNT' | 'FREESHIP';
  /** % (15, 7.5) hoặc số tiền; `null` với FREESHIP. */
  discount_value: number | null;
  max_discount: number | null;
  min_order_value: number;
  /** `id` = shop_id (vendor_profiles.id); `null` với voucher sàn. */
  shop: { id: number; name: string } | null;
  scope: 'ALL' | 'CATEGORY' | 'PRODUCT';
  included_categories: string[];
  excluded_categories: string[];
  valid_from: string;
  valid_to: string;
  state: 'AVAILABLE' | 'UPCOMING' | 'USED' | 'EXPIRED';
  usage: { used: number; limit: number | null };
  funded_by: 'PLATFORM' | 'VENDOR' | 'CO_SPONSOR';
  description: string | null;
  is_saved: boolean | null;
  used_order_code: string | null;
}
