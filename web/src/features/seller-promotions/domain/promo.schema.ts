// Form "Tạo / Chỉnh sửa voucher" (zod) — mã 6–12 chữ HOA + số, giá trị > 0 (% ≤ 100), số lượng > 0,
// ngày kết thúc sau ngày bắt đầu, phạm vi danh mục / sản phẩm phải chọn ít nhất 1.
import { z } from 'zod';
import { CODE_RE } from './promo.rules';

const n = z.number().int().nonnegative().nullable();

export const voucherSchema = z
  .object({
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
  })
  .refine((v) => v.discountType === 'FREESHIP' || (v.discountValue ?? 0) > 0, {
    path: ['discountValue'],
    message: 'seller-promotions.form.err.value',
  })
  .refine((v) => v.discountType !== 'PERCENTAGE' || (v.discountValue ?? 0) <= 100, {
    path: ['discountValue'],
    message: 'seller-promotions.form.err.percent',
  })
  .refine((v) => v.usageLimit !== null, {
    path: ['usageLimit'],
    message: 'seller-promotions.form.err.total',
  })
  .refine((v) => !v.validFrom || !v.validTo || v.validTo > v.validFrom, {
    path: ['validTo'],
    message: 'seller-promotions.form.err.end',
  })
  .refine((v) => v.scope !== 'CATEGORY' || v.categoryIds.length > 0, {
    path: ['categoryIds'],
    message: 'seller-promotions.form.err.category',
  })
  .refine((v) => v.scope !== 'PRODUCT' || v.productIds.length > 0, {
    path: ['productIds'],
    message: 'seller-promotions.form.err.product',
  });
export type VoucherValues = z.infer<typeof voucherSchema>;
