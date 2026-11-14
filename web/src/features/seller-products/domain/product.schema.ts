// Lược đồ zod cho các trường nhập tay của form đăng bán (khối Thông tin cơ bản, Vận chuyển) và form
// "Đề xuất danh mục mới". Ảnh / phân loại / thông số kiểm bằng `publishIssues` vì là cấu trúc lồng.
import { z } from 'zod';
import { PRE_ORDER_MAX, PRE_ORDER_MIN } from './product.rules';

const optNum = z.number().int().nonnegative().nullable();

export const basicsSchema = z
  .object({
    name: z
      .string()
      .trim()
      .min(5, 'seller-products.err.name')
      .max(120, 'seller-products.err.name_long'),
    origin: z.string().trim().max(60, 'seller-products.err.origin'),
    condition: z.enum(['NEW', 'USED', 'REFURBISHED']),
    weightGrams: z
      .number({ invalid_type_error: 'seller-products.err.weight' })
      .int()
      .positive('seller-products.err.weight')
      .max(50_000, 'seller-products.err.weight_max')
      .nullable(),
    length: optNum,
    width: optNum,
    height: optNum,
    shippingPayer: z.enum(['BUYER', 'SELLER']),
    freeShippingThreshold: optNum,
    isPreOrder: z.boolean(),
    preOrderDays: z.number().int().nullable(),
  })
  .refine(
    (v) =>
      !v.isPreOrder ||
      (v.preOrderDays !== null &&
        v.preOrderDays >= PRE_ORDER_MIN &&
        v.preOrderDays <= PRE_ORDER_MAX),
    {
      path: ['preOrderDays'],
      message: 'seller-products.err.pre_order',
    },
  )
  .refine((v) => v.shippingPayer === 'BUYER' || (v.freeShippingThreshold ?? 0) > 0, {
    path: ['freeShippingThreshold'],
    message: 'seller-products.err.free_ship',
  });
export type BasicsValues = z.infer<typeof basicsSchema>;

export const proposalSchema = z.object({
  name: z
    .string()
    .trim()
    .min(2, 'seller-products.catalog.err.name')
    .max(60, 'seller-products.catalog.err.name_long'),
  parentId: z.number().int().nullable(),
  reason: z.string().trim().min(5, 'seller-products.catalog.err.reason'),
});
export type ProposalValues = z.infer<typeof proposalSchema>;
