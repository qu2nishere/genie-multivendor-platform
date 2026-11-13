// Form "Từ chối đơn" (zod) — ghi chú bắt buộc khi chọn "Lý do khác" (API 10.3 reject).
import { z } from 'zod';
import { REJECT_REASONS } from './fulfillment.rules';

export const rejectSchema = z
  .object({
    reason: z.enum(REJECT_REASONS),
    note: z.string().trim().max(500, 'seller-orders.reject.err.note_long'),
  })
  .refine((v) => v.reason !== 'OTHER' || v.note.length > 0, {
    path: ['note'],
    message: 'seller-orders.reject.err.note',
  });
export type RejectValues = z.infer<typeof rejectSchema>;
