// Voucher 3 tầng: sàn / shop / freeship (prototype data.jsx VOUCHERS). Quy tắc tính giảm ở G10.09.
export type VoucherKind = 'platform' | 'shop' | 'freeship';
export type VoucherType = 'FIXED' | 'PERCENT' | 'FREESHIP';
export type VoucherStatus = 'active' | 'upcoming' | 'used' | 'expired';

export interface Voucher {
  id: string;
  code: string;
  kind: VoucherKind;
  shopId: number | null;
  amountLabel: string;
  label: string;
  conditionLabel: string;
  type: VoucherType;
  value: number;
  cap: number | null;
  minOrder: number;
  categories: string[] | null;
  excluded: string[] | null;
  validFrom: string;
  expiresAt: string;
  scopeLabel: string;
  sponsorLabel: string;
  content: string;
  saved: boolean;
  status: VoucherStatus;
  usedCount: number;
  usageLimit: number;
}
