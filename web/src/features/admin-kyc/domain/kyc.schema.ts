// Form từ chối / yêu cầu bổ sung KYC (zod; API 11.3).
import { z } from 'zod';
import { REJECT_REASONS } from './kyc.rules';

export const rejectSchema = z
  .object({
    reason: z.enum(REJECT_REASONS),
    note: z.string().trim().max(500, 'admin-kyc.reject.err.note_long'),
  })
  .refine((v) => v.reason !== 'OTHER' || v.note.length > 0, {
    path: ['note'],
    message: 'admin-kyc.reject.err.note',
  });
export type RejectValues = z.infer<typeof rejectSchema>;

const DOC = z.enum([
  'ID_FRONT',
  'ID_BACK',
  'BUSINESS_LICENSE',
  'BUSINESS_REGISTRATION',
  'SELFIE',
  'TAX_NOTICE',
]);

export const supplementSchema = z.object({
  documents: z.array(DOC).min(1, 'admin-kyc.supplement.err.docs'),
  note: z.string().trim().max(500, 'admin-kyc.supplement.err.note_long'),
});
export type SupplementValues = z.infer<typeof supplementSchema>;
