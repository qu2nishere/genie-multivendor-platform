// Form "Tạo / Chỉnh sửa voucher" (zod) — mã 6–12 chữ HOA + số, giá trị > 0 (% ≤ 100), số lượng > 0,
// ngày kết thúc sau ngày bắt đầu, phạm vi danh mục / sản phẩm phải chọn ít nhất 1.
// Cùng form dùng cho voucher sàn ở Admin (plan G12.33, API 11.14) với chế độ `platform`: thêm tên
// chương trình (bắt buộc), nhà tài trợ (PLATFORM / CO_SPONSOR + tỷ lệ sàn tài trợ), phạm vi chỉ
// Toàn sàn / Danh mục, ngày bắt đầu không ở quá khứ khi tạo mới.
import { z } from 'zod';
import { CODE_RE } from './promo.rules';

const n = z.number().int().nonnegative().nullable();

const fields = z.object({
  code: z.string().regex(CODE_RE, 'seller-promotions.form.err.code'),
  discountType: z.enum(['PERCENTAGE', 'FIXED', 'FREESHIP']),
  discountValue: n,
  maxDiscount: n,
  minOrderValue: n,
  usageLimit: z
    .number({ invalid_type_error: 'seller-promotions.form.err.total' })
    .int()
    .positive('seller-promotions.form.err.total')
    .nullable(),
  usagePerUser: n,
  budget: n,
  validFrom: z.string().min(1, 'seller-promotions.form.err.from'),
  validTo: z.string().min(1, 'seller-promotions.form.err.to'),
  scope: z.enum(['ALL', 'CATEGORY', 'PRODUCT']),
  categoryIds: z.array(z.number()),
  productIds: z.array(z.number()),
  excludedCategoryIds: z.array(z.number()),
  // Chỉ voucher sàn dùng (form shop điền sẵn '', PLATFORM, null và bỏ qua khi gửi)
  title: z.string().trim().max(100, 'seller-promotions.form.err.title_long'),
  fundedBy: z.enum(['PLATFORM', 'CO_SPONSOR']),
  platformSharePercent: n,
});

type Core = z.infer<typeof fields>;

function shopChecks(v: Core, ctx: z.RefinementCtx): void {
  const add = (path: keyof Core, message: string) =>
    ctx.addIssue({ code: z.ZodIssueCode.custom, path: [path], message });
  if (v.discountType !== 'FREESHIP' && !((v.discountValue ?? 0) > 0))
    add('discountValue', 'seller-promotions.form.err.value');
  if (v.discountType === 'PERCENTAGE' && (v.discountValue ?? 0) > 100)
    add('discountValue', 'seller-promotions.form.err.percent');
  if (v.usageLimit === null) add('usageLimit', 'seller-promotions.form.err.total');
  if (v.validFrom && v.validTo && !(v.validTo > v.validFrom))
    add('validTo', 'seller-promotions.form.err.end');
  if (v.scope === 'CATEGORY' && v.categoryIds.length === 0)
    add('categoryIds', 'seller-promotions.form.err.category');
  if (v.scope === 'PRODUCT' && v.productIds.length === 0)
    add('productIds', 'seller-promotions.form.err.product');
}

export const voucherSchema = fields.superRefine(shopChecks);
export type VoucherValues = z.infer<typeof voucherSchema>;

export type VoucherFormMode = 'shop' | 'platform';

/**
 * Lược đồ theo chế độ. `today` ("YYYY-MM-DD", giờ VN) chỉ dùng khi tạo mới voucher sàn:
 * ngày bắt đầu không ở quá khứ (API 11.14).
 */
export function voucherSchemaFor(mode: VoucherFormMode, opts: { isNew: boolean; today: string }) {
  if (mode === 'shop') return voucherSchema;
  return fields.superRefine((v, ctx) => {
    shopChecks(v, ctx);
    const add = (path: keyof Core, message: string) =>
      ctx.addIssue({ code: z.ZodIssueCode.custom, path: [path], message });
    if (!v.title) add('title', 'seller-promotions.form.err.title');
    if (v.scope === 'PRODUCT') add('scope', 'seller-promotions.form.err.scope_platform');
    if (v.fundedBy === 'CO_SPONSOR') {
      const s = v.platformSharePercent;
      if (s === null || s < 1 || s > 100)
        add('platformSharePercent', 'seller-promotions.form.err.share');
    }
    if (opts.isNew && v.validFrom && v.validFrom < opts.today)
      add('validFrom', 'seller-promotions.form.err.past');
  });
}
