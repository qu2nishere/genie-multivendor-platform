// Form phân xử + phong tỏa ký quỹ (zod; API 11.4: verdict / reason bắt buộc).
import { z } from 'zod';

export const resolveSchema = z.object({
  outcome: z.enum(['REFUND_NOW', 'RETURN_THEN_REFUND', 'DENY', 'CARRIER_FAULT']),
  verdict: z
    .string()
    .trim()
    .min(1, 'admin-disputes.resolve.err.verdict')
    .max(2000, 'admin-disputes.resolve.err.long'),
});
export type ResolveValues = z.infer<typeof resolveSchema>;

export const freezeSchema = z.object({
  reason: z
    .string()
    .trim()
    .min(1, 'admin-disputes.freeze.err.reason')
    .max(500, 'admin-disputes.freeze.err.long'),
});
export type FreezeValues = z.infer<typeof freezeSchema>;
