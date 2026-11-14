import type { Voucher } from '../domain/voucher.types';
import type { VoucherDto } from './voucher.dto';

export const toVoucher = (d: VoucherDto): Voucher => ({
  id: d.id,
  code: d.code,
  kind: d.kind,
  shopId: d.shop_id,
  amountLabel: d.amount_label,
  label: d.label,
  conditionLabel: d.condition_label,
  type: d.type,
  value: d.value,
  cap: d.cap,
  minOrder: d.min_order,
  categories: d.categories,
  excluded: d.excluded,
  validFrom: d.valid_from,
  expiresAt: d.expires_at,
  scopeLabel: d.scope_label,
  sponsorLabel: d.sponsor_label,
  content: d.content,
  saved: d.saved,
  status: d.status,
  usedCount: d.used_count,
  usageLimit: d.usage_limit,
});
