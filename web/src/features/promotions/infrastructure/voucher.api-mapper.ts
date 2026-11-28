// `$Voucher` (API) → `Voucher` (UI) — dựng các nhãn hiển thị mà bản mock có sẵn (plan 2026-11-26 voucher V08.01).
import { formatVND, isoDateVN } from '@/shared/lib/format';
import type { Voucher, VoucherKind, VoucherStatus, VoucherType } from '../domain/voucher.types';
import type { ApiVoucherDto } from './voucher.api-dto';

const STATE: Record<ApiVoucherDto['state'], VoucherStatus> = {
  AVAILABLE: 'active',
  UPCOMING: 'upcoming',
  USED: 'used',
  EXPIRED: 'expired',
};

/** Voucher shop miễn phí ship vẫn là `SHOP` ở API nhưng hiện ở nhóm Freeship (3.14). */
export const kindOf = (d: Pick<ApiVoucherDto, 'type' | 'discount_type'>): VoucherKind =>
  d.type === 'FREESHIP' || d.discount_type === 'FREESHIP'
    ? 'freeship'
    : d.type === 'SHOP'
      ? 'shop'
      : 'platform';

const typeOf = (t: ApiVoucherDto['discount_type']): VoucherType =>
  t === 'PERCENTAGE' ? 'PERCENT' : t === 'FIXED_AMOUNT' ? 'FIXED' : 'FREESHIP';

/** 30000 → "30k", 1500000 → "1,5tr". */
export function shortMoney(n: number): string {
  if (n >= 1_000_000) return `${String(Math.round(n / 100_000) / 10).replace('.', ',')}tr`;
  if (n >= 1000) return `${String(Math.round(n / 100) / 10).replace('.', ',')}k`;
  return `${n}₫`;
}

export function amountLabelOf(d: ApiVoucherDto): string {
  if (d.discount_type === 'PERCENTAGE')
    return `-${String(d.discount_value ?? 0).replace('.', ',')}%`;
  if (d.discount_type === 'FIXED_AMOUNT') return `-${shortMoney(d.discount_value ?? 0)}`;
  return d.max_discount ? `-${shortMoney(d.max_discount)}` : 'Freeship';
}

const vnd = (n: number) => formatVND(n, { locale: 'vi' });

export function sponsorLabelOf(d: ApiVoucherDto): string {
  if (d.funded_by === 'VENDOR') return d.shop ? d.shop.name : 'Shop tài trợ';
  if (d.funded_by === 'CO_SPONSOR') return 'Genie & Shop đồng tài trợ';
  return 'Genie tài trợ';
}

export function fromApiVoucher(d: ApiVoucherDto): Voucher {
  const type = typeOf(d.discount_type);
  return {
    id: String(d.id),
    code: d.code,
    kind: kindOf(d),
    shopId: d.shop?.id ?? null,
    amountLabel: amountLabelOf(d),
    label: d.title,
    conditionLabel: d.min_order_value > 0 ? `Đơn từ ${vnd(d.min_order_value)}` : 'Mọi đơn hàng',
    type,
    value: d.discount_value ?? d.max_discount ?? 0,
    cap: d.max_discount,
    minOrder: d.min_order_value,
    categories: d.included_categories.length ? d.included_categories : null,
    excluded: d.excluded_categories.length ? d.excluded_categories : null,
    validFrom: isoDateVN(new Date(d.valid_from)),
    expiresAt: isoDateVN(new Date(d.valid_to)),
    scopeLabel: d.shop ? `Shop ${d.shop.name}` : 'Toàn sàn',
    sponsorLabel: sponsorLabelOf(d),
    content: d.description ?? d.title,
    saved: d.is_saved ?? false,
    status: STATE[d.state],
    usedCount: d.usage.used,
    usageLimit: d.usage.limit ?? 0,
    usedOrderCode: d.used_order_code,
  };
}
