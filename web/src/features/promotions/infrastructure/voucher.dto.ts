// Voucher (GET /me/vouchers, /public/vouchers/{code}) — snake_case.
export interface VoucherDto {
  id: string;
  code: string;
  /** Tầng: sàn / shop / freeship. */
  kind: 'platform' | 'shop' | 'freeship';
  shop_id: number | null;
  /** Nhãn mức giảm đã định dạng ("-30k", "-15%", "0₫"). */
  amount_label: string;
  label: string;
  condition_label: string;
  type: 'FIXED' | 'PERCENT' | 'FREESHIP';
  value: number;
  /** Trần giảm (PERCENT, FREESHIP). */
  cap: number | null;
  min_order: number;
  /** Chỉ áp cho / loại trừ ngành (tên danh mục gốc). */
  categories: string[] | null;
  excluded: string[] | null;
  valid_from: string;
  expires_at: string;
  scope_label: string;
  sponsor_label: string;
  content: string;
  saved: boolean;
  status: 'active' | 'upcoming' | 'used' | 'expired';
  used_count: number;
  usage_limit: number;
}
