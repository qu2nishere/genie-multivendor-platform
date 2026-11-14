// Lược đồ form địa chỉ (zod) — thông điệp là khóa i18n.
import { z } from 'zod';
import { normalizePhone, PHONE_RE } from './address.rules';

export const addressSchema = z.object({
  recipient: z
    .string()
    .trim()
    .min(1, 'account.address.err.recipient')
    .max(100, 'account.address.err.recipient_long'),
  phone: z
    .string()
    .transform(normalizePhone)
    .refine((p) => PHONE_RE.test(p), 'account.address.err.phone'),
  provinceId: z
    .number({ invalid_type_error: 'account.address.err.region' })
    .int()
    .positive('account.address.err.region'),
  districtId: z
    .number({ invalid_type_error: 'account.address.err.region' })
    .int()
    .positive('account.address.err.region'),
  wardCode: z.string().min(1, 'account.address.err.region'),
  line: z
    .string()
    .trim()
    .min(1, 'account.address.err.line')
    .max(255, 'account.address.err.line_long'),
  label: z.enum(['HOME', 'OFFICE']),
  isDefault: z.boolean(),
});

export type AddressFormValues = z.infer<typeof addressSchema>;
