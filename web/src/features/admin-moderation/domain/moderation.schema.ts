// Thanh quyết định duyệt sản phẩm (zod; API 11.12 reject: reason_code + note).
import { z } from 'zod';
import { PRODUCT_REJECT_REASONS } from './moderation.rules';

export const productRejectSchema = z.object({
  reason: z.enum(PRODUCT_REJECT_REASONS),
  note: z.string().trim().max(500, 'admin-moderation.product.err.note_long'),
});
export type ProductRejectValues = z.infer<typeof productRejectSchema>;
